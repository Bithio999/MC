package dev.heavenlyrestriction.client.cartography;

import com.mojang.blaze3d.platform.NativeImage;
import dev.heavenlyrestriction.cartography.MapView;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/** Live recorded-terrain overlay, bounded texture LRU, and asynchronous atomic disk IO. */
public final class MapAtlas {
  private static final ThreadPoolExecutor IO =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(64),
          r -> {
            var t = new Thread(r, "MaraudersMap-IO");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());
  private static final LinkedHashMap<Long, Region> REGIONS = new LinkedHashMap<>(32, .75f, true);
  private static final LinkedHashMap<Long, Integer> OVERVIEW = new LinkedHashMap<>();
  private static final LinkedHashMap<Long, Long> SAMPLED = new LinkedHashMap<>();
  private static UUID world;
  private static String dimension = "", key = "";
  private static Path folder;
  private static long epoch, tick, version;
  private static int scan, ceilingPlane, scanRadius = -1;
  private static List<ChunkPos> offsets = List.of();
  private static boolean overviewDirty;
  public static volatile String notice = "";
  private static DynamicTexture overviewTexture;
  private static ResourceLocation overviewId;
  private static double lastX = Double.NaN, lastZ, lastScale;
  private static int lastW, lastH;
  private static long lastVersion = -1;

  private static final class Region {
    final int x, z;
    final long generation;
    final Path path;
    final BitSet fresh = new BitSet(256);
    NativeImage image = new NativeImage(256, 256, true);
    DynamicTexture texture;
    ResourceLocation id;
    boolean loading, readComplete, dirty, upload, closed;

    Region(int x, int z) {
      this.x = x;
      this.z = z;
      generation = epoch;
      path = folder.resolve("r_" + x + "_" + z + ".png");
    }

    void close() {
      if (closed) return;
      save(this);
      closed = true;
      if (texture != null) {
        Minecraft.getInstance().getTextureManager().release(id);
        texture.close();
      } else image.close();
    }
  }

  public static void context(UUID nextWorld, String nextDimension) {
    if (Objects.equals(world, nextWorld) && dimension.equals(nextDimension)) return;
    clear();
    world = nextWorld;
    dimension = nextDimension;
    configure();
  }

  private static void configure() {
    if (world == null) return;
    var mc = Minecraft.getInstance();
    if (mc.level == null || !dimension.equals(mc.level.dimension().location().toString())) return;
    ceilingPlane = mc.player == null ? 64 : Math.floorDiv(mc.player.getBlockY(), 16) * 16;
    String layer =
        mc.level != null && mc.level.dimensionType().hasCeiling()
            ? "_below_" + ceilingPlane
            : "_surface";
    try {
      String hash =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(dimension.getBytes(StandardCharsets.UTF_8)))
              .substring(0, 16);
      key = world + "/" + hash + layer + "_detail2";
      folder = mc.gameDirectory.toPath().resolve("tensura-additions/maps").resolve(key);
      loadOverview();
    } catch (Exception e) {
      notice = "Terrain cache unavailable; live mapping still works.";
    }
  }

  public static String layer() {
    return Minecraft.getInstance().level != null
            && Minecraft.getInstance().level.dimensionType().hasCeiling()
        ? "Below roof · Y " + ceilingPlane
        : "Surface terrain";
  }

  private static void submit(Runnable task, Runnable rejected) {
    try {
      IO.execute(task);
    } catch (RejectedExecutionException e) {
      rejected.run();
    }
  }

  private static void loadOverview() {
    long generation = epoch;
    Path path = folder.resolve("overview.dat");
    submit(
        () -> {
          var rows = new LinkedHashMap<Long, Integer>();
          try {
            if (Files.isRegularFile(path))
              try (var in = new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
                int count = in.readInt();
                if (count < 0 || count > 262144) throw new IOException("Invalid overview size");
                for (int n = 0; n < count; n++) rows.put(in.readLong(), in.readInt());
              }
          } catch (IOException e) {
            /* A corrupt cache can be safely rebuilt from visible terrain. */
          }
          Minecraft.getInstance()
              .execute(
                  () -> {
                    if (generation != epoch) return;
                    rows.forEach(OVERVIEW::putIfAbsent);
                    version++;
                  });
        },
        () -> {});
  }

  private static Region region(int x, int z) {
    long id = ChunkPos.asLong(x, z);
    var existing = REGIONS.get(id);
    if (existing != null) {
      if (!existing.loading && !existing.readComplete) load(existing);
      return existing;
    }
    var r = new Region(x, z);
    REGIONS.put(id, r);
    load(r);
    while (REGIONS.size() > 128) {
      var first = REGIONS.entrySet().iterator();
      var old = first.next();
      first.remove();
      old.getValue().close();
    }
    return r;
  }

  private static void load(Region r) {
    r.loading = true;
    submit(
        () -> {
          NativeImage loaded = null;
          try {
            if (Files.isRegularFile(r.path)) {
              loaded = NativeImage.read(Files.readAllBytes(r.path));
              if (loaded.getWidth() != 256 || loaded.getHeight() != 256) {
                loaded.close();
                loaded = null;
              }
            }
          } catch (IOException | RuntimeException ignored) {
          }
          NativeImage result = loaded;
          Minecraft.getInstance()
              .execute(
                  () -> {
                    if (r.closed || r.generation != epoch) {
                      if (result != null) result.close();
                      return;
                    }
                    r.loading = false;
                    r.readComplete = true;
                    if (result != null) {
                      for (int y = 0; y < 256; y++)
                        for (int px = 0; px < 256; px++)
                          if (!r.fresh.get((y >> 4) * 16 + (px >> 4)))
                            r.image.setPixelRGBA(px, y, result.getPixelRGBA(px, y));
                      result.close();
                      r.upload = true;
                    }
                    version++;
                  });
        },
        () -> r.loading = false);
  }

  private static int abgr(int color) {
    return (color & 0xff00ff00) | ((color >> 16) & 255) | ((color & 255) << 16);
  }

  public static void tick() {
    var mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) {
      if (world != null) clear();
      return;
    }
    if (world == null || !dimension.equals(mc.level.dimension().location().toString())) return;
    if (folder == null) configure();
    tick++;
    if (tick % 200 == 0) {
      REGIONS.values().forEach(MapAtlas::save);
      saveOverview();
    }
    int radius = Math.min(12, Math.max(2, mc.options.renderDistance().get()));
    if (radius != scanRadius) {
      scanRadius = radius;
      var next = new ArrayList<ChunkPos>();
      for (int x = -radius; x <= radius; x++)
        for (int z = -radius; z <= radius; z++) next.add(new ChunkPos(x, z));
      next.sort(Comparator.comparingInt(pos -> pos.x * pos.x + pos.z * pos.z));
      offsets = List.copyOf(next);
      scan = 0;
    }
    for (int n = 0; n < 4; n++) {
      var offset = offsets.get(Math.floorMod(scan++, offsets.size()));
      int cx = (mc.player.getBlockX() >> 4) + offset.x,
          cz = (mc.player.getBlockZ() >> 4) + offset.z;
      long chunkKey = ChunkPos.asLong(cx, cz);
      if (tick - SAMPLED.getOrDefault(chunkKey, -1000L) < 100) continue;
      var chunk = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
      if (chunk == null || chunk instanceof EmptyLevelChunk) continue;
      capture(chunk);
      SAMPLED.put(chunkKey, tick);
      while (SAMPLED.size() > 8192) SAMPLED.remove(SAMPLED.keySet().iterator().next());
    }
    if (mc.screen instanceof MaraudersMapScreen && tick % 4 == 0)
      REGIONS
          .values()
          .forEach(
              r -> {
                if (r.upload && r.texture != null) {
                  r.texture.upload();
                  r.upload = false;
                }
              });
  }

  private static void capture(LevelChunk chunk) {
    var mc = Minecraft.getInstance();
    var level = mc.level;
    var cp = chunk.getPos();
    var r = region(Math.floorDiv(cp.x, 16), Math.floorDiv(cp.z, 16));
    int bx = Math.floorMod(cp.x, 16) * 16, bz = Math.floorMod(cp.z, 16) * 16;
    boolean below = level.dimensionType().hasCeiling();
    if (mc.screen instanceof MaraudersMapScreen map
        && map.dimension().equals(dimension)
        && map.view.scale >= 1
        && map.visibleChunk(cp.x, cp.z))
      MapTerrainAtlas.local(chunk, below ? ceilingPlane : Integer.MAX_VALUE);
    var pos = new BlockPos.MutableBlockPos();
    long red = 0, green = 0, blue = 0;
    int[] previous = new int[16];
    for (int z = 0; z < 16; z++)
      for (int x = 0; x < 16; x++) {
        int wx = cp.getMinBlockX() + x, wz = cp.getMinBlockZ() + z;
        int y =
            below
                ? Math.min(ceilingPlane, level.getMaxBuildHeight() - 1)
                : chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        y = Math.max(level.getMinBuildHeight(), y);
        pos.set(wx, y, wz);
        var state = chunk.getBlockState(pos);
        int steps = 0;
        while (y > level.getMinBuildHeight()
            && (state.isAir() || state.getMapColor(level, pos) == MapColor.NONE)
            && steps++ < 160) {
          pos.setY(--y);
          state = chunk.getBlockState(pos);
        }
        int tint = mc.getBlockColors().getColor(state, level, pos, 0);
        if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER))
          tint = BiomeColors.getAverageWaterColor(level, pos);
        int color = MapTerrainAtlas.surfaceColor(Block.getId(state), tint);
        int depth = 0;
        if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
          int floor = y;
          while (floor > level.getMinBuildHeight()
              && y - floor < 64
              && chunk
                  .getBlockState(pos.setY(floor))
                  .getFluidState()
                  .is(net.minecraft.tags.FluidTags.WATER)) floor--;
          depth = y - floor;
          pos.setY(y);
        }
        int north = z > 0 ? previous[x] : y;
        previous[x] = y;
        int west = x > 0 ? chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x - 1, z) : y;
        int east = x < 15 ? chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x + 1, z) : y;
        int south = z < 15 ? chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z + 1) : y;
        if (below) west = east = south = y;
        color =
            dev.heavenlyrestriction.cartography.MapTerrain.shade(
                color, y, west, east, north, south, depth, 1);
        int rr = (color >> 16) & 255, gg = (color >> 8) & 255, bb = color & 255;
        r.image.setPixelRGBA(bx + x, bz + z, abgr(0xff000000 | (rr << 16) | (gg << 8) | bb));
        red += rr;
        green += gg;
        blue += bb;
      }
    r.fresh.set(Math.floorMod(cp.z, 16) * 16 + Math.floorMod(cp.x, 16));
    r.dirty = true;
    r.upload = true;
    int average =
        0xff000000 | ((int) (red / 256) << 16) | ((int) (green / 256) << 8) | (int) (blue / 256);
    var old = OVERVIEW.put(cp.toLong(), average);
    if (old == null || old != average) {
      overviewDirty = true;
      version++;
    }
    while (OVERVIEW.size() > 262144) OVERVIEW.remove(OVERVIEW.keySet().iterator().next());
  }

  private static void save(Region r) {
    if (!r.dirty || !r.readComplete || r.loading || r.closed) return;
    var copy = new NativeImage(256, 256, true);
    copy.copyFrom(r.image);
    r.dirty = false;
    submit(
        () -> {
          try {
            Files.createDirectories(r.path.getParent());
            Path temporary = r.path.resolveSibling(r.path.getFileName() + ".tmp");
            copy.writeToFile(temporary);
            move(temporary, r.path);
          } catch (IOException e) {
            notice = "Terrain cache could not be saved.";
          } finally {
            copy.close();
          }
        },
        () -> {
          r.dirty = true;
          copy.close();
        });
  }

  private static void saveOverview() {
    if (!overviewDirty || folder == null) return;
    var copy = new LinkedHashMap<>(OVERVIEW);
    Path path = folder.resolve("overview.dat");
    overviewDirty = false;
    submit(
        () -> {
          try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling("overview.tmp");
            try (var out =
                new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(temporary)))) {
              out.writeInt(copy.size());
              for (var row : copy.entrySet()) {
                out.writeLong(row.getKey());
                out.writeInt(row.getValue());
              }
            }
            move(temporary, path);
          } catch (IOException e) {
            notice = "Terrain overview could not be saved.";
          }
        },
        () -> overviewDirty = true);
  }

  private static void move(Path from, Path to) throws IOException {
    try {
      Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public static void flush() {
    REGIONS.values().forEach(MapAtlas::save);
    saveOverview();
  }

  public static void clear() {
    MapTerrainAtlas.clear();
    flush();
    REGIONS.values().forEach(Region::close);
    REGIONS.clear();
    OVERVIEW.clear();
    SAMPLED.clear();
    if (overviewTexture != null) {
      Minecraft.getInstance().getTextureManager().release(overviewId);
      overviewTexture.close();
      overviewTexture = null;
    }
    world = null;
    dimension = "";
    folder = null;
    epoch++;
    scan = 0;
    version++;
    overviewDirty = false;
    lastX = Double.NaN;
    notice = "";
  }

  public static int knownChunks() {
    return OVERVIEW.size();
  }

  public static void render(GuiGraphics g, MapView view, int left, int top, int width, int height) {
    if (folder == null) return;
    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
    com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    if (view.scale < .5) {
      overview(g, view, left, top, width, height);
      return;
    }
    int minX = Math.floorDiv((int) Math.floor(view.worldX(left, left, width)), 256),
        maxX = Math.floorDiv((int) Math.ceil(view.worldX(left + width, left, width)), 256),
        minZ = Math.floorDiv((int) Math.floor(view.worldZ(top, top, height)), 256),
        maxZ = Math.floorDiv((int) Math.ceil(view.worldZ(top + height, top, height)), 256);
    if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > 96) {
      overview(g, view, left, top, width, height);
      return;
    }
    for (int z = minZ; z <= maxZ; z++)
      for (int x = minX; x <= maxX; x++) {
        var r = region(x, z);
        if (r.texture == null) {
          r.texture = new DynamicTexture(r.image);
          r.id =
              Minecraft.getInstance()
                  .getTextureManager()
                  .register("marauders_map_region", r.texture);
          r.upload = false;
        }
        int sx = (int) Math.floor(view.screenX(x * 256d, left, width)),
            sy = (int) Math.floor(view.screenZ(z * 256d, top, height));
        int ex = (int) Math.ceil(view.screenX(x * 256d + 256, left, width)),
            ey = (int) Math.ceil(view.screenZ(z * 256d + 256, top, height));
        g.blit(r.id, sx, sy, ex - sx, ey - sy, 0f, 0f, 256, 256, 256, 256);
      }
  }

  private static void overview(GuiGraphics g, MapView v, int left, int top, int width, int height) {
    if (overviewTexture == null) {
      overviewTexture = new DynamicTexture(256, 256, true);
      overviewId =
          Minecraft.getInstance()
              .getTextureManager()
              .register("marauders_map_overview", overviewTexture);
    }
    if (v.x != lastX
        || v.z != lastZ
        || v.scale != lastScale
        || width != lastW
        || height != lastH
        || lastVersion != version) {
      var image = overviewTexture.getPixels();
      for (int y = 0; y < 256; y++)
        for (int x = 0; x < 256; x++) {
          int wx = (int) Math.floor(v.worldX(left + (x + .5) * width / 256d, left, width)),
              wz = (int) Math.floor(v.worldZ(top + (y + .5) * height / 256d, top, height));
          int color = OVERVIEW.getOrDefault(ChunkPos.asLong(wx >> 4, wz >> 4), 0);
          image.setPixelRGBA(x, y, abgr(color));
        }
      overviewTexture.upload();
      lastX = v.x;
      lastZ = v.z;
      lastScale = v.scale;
      lastW = width;
      lastH = height;
      lastVersion = version;
    }
    g.blit(overviewId, left, top, width, height, 0f, 0f, 256, 256, 256, 256);
  }

  private MapAtlas() {}
}
