package dev.heavenlyrestriction.cartography;

import dev.heavenlyrestriction.mixin.MapChunkAccess;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.*;

/**
 * Viewport-driven terrain: pure generator previews plus bounded native feature-stage refinement.
 */
public final class MapTerrain {
  public static final int SIZE = 32, MAX_PENDING = 8;
  private static final AtomicLong EPOCH = new AtomicLong();
  private static final ThreadPoolExecutor WORKER =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(16),
          r -> {
            var t = new Thread(r, "MaraudersMap-Terrain");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());

  private record CacheKey(
      UUID world, String dimension, int x, int z, int step, int plane, boolean quick) {}

  private static final Map<CacheKey, CompoundTag> CACHE = new LinkedHashMap<>(32, .75f, true);
  private static final net.minecraft.server.level.TicketType<UUID> TICKET =
      net.minecraft.server.level.TicketType.create("additions_cartography", UUID::compareTo, 200);

  public record Pending(
      net.minecraft.server.level.ServerLevel level,
      UUID token,
      int x,
      int z,
      int plane,
      CompletableFuture<net.minecraft.server.level.ChunkResult<ChunkAccess>> future) {}

  private static long ticks, nextDetail;
  private static final List<CompletableFuture<?>> ACTIVE = new ArrayList<>();

  public static boolean valid(MapService.Session s, int x, int z, int step, boolean detail) {
    if (step < 1 || step > 512 || (step & (step - 1)) != 0 || (detail && step != 1)) return false;
    long span = detail ? 16 : (long) SIZE * step;
    long wx = (long) x * span, wz = (long) z * span;
    return wx >= -MapView.LIMIT - span
        && wx <= MapView.LIMIT
        && wz >= -MapView.LIMIT - span
        && wz <= MapView.LIMIT
        && Math.abs(wx + span / 2 - s.cx) <= s.radius + span
        && Math.abs(wz + span / 2 - s.cz) <= s.radius + span;
  }

  public static void request(ServerPlayer p, MapService.Session s, CompoundTag request) {
    boolean detail = request.getBoolean("detail"), quick = request.getBoolean("quick");
    int x = request.getInt("tx"), z = request.getInt("tz"), step = request.getInt("step");
    if (!valid(s, x, z, step, detail)) return;
    String id = (detail ? "d/" : quick ? "q/" : "p/") + x + "/" + z + "/" + step;
    if (s.terrain.size() >= MAX_PENDING || !s.terrain.add(id)) return;
    var level = p.serverLevel();
    int plane =
        level.dimensionType().hasCeiling()
            ? Math.floorDiv(p.getBlockY(), 16) * 16
            : Integer.MAX_VALUE;
    if (detail) {
      if (ticks < nextDetail
          || ACTIVE.size() >= 2
          || s.details.size() >= 2
          || !level.getWorldBorder().isWithinBounds(new BlockPos(x * 16 + 8, 64, z * 16 + 8))) {
        s.terrain.remove(id);
        reject(p, s, id);
        return;
      }
      nextDetail = ticks + 4;
      ((MapChunkAccess) level.getChunkSource())
          .additions$mapTickets()
          .addTicket(
              TICKET,
              new ChunkPos(x, z),
              net.minecraft.server.level.ChunkLevel.byStatus(ChunkStatus.FEATURES),
              s.token);
      var future =
          ((MapChunkAccess) level.getChunkSource())
              .additions$mapFuture(x, z, ChunkStatus.FEATURES, true);
      ACTIVE.add(future);
      s.details.put(id, new Pending(level, s.token, x, z, plane, future));
      return;
    }
    var key = new CacheKey(MapData.get(p.server).world, s.dimension, x, z, step, plane, quick);
    var cached = CACHE.get(key);
    if (cached != null) {
      deliver(p, s, id, cached);
      return;
    }
    // Capture immutable world-generation inputs. The worker never accesses the live level/chunks.
    var generator = level.getChunkSource().getGenerator();
    var random = level.getChunkSource().randomState();
    var height = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
    long epoch = EPOCH.get();
    try {
      WORKER.execute(
          () -> {
            CompoundTag tile = null;
            try {
              tile =
                  preview(
                      generator,
                      random,
                      height,
                      x,
                      z,
                      step,
                      plane,
                      epoch,
                      quick,
                      () -> !s.terrainClosed && valid(s, x, z, step, false));
            } catch (RuntimeException ignored) {
            }
            CompoundTag result = tile;
            p.server.execute(
                () -> {
                  if (epoch != EPOCH.get()) return;
                  if (result != null) {
                    CACHE.put(key, result);
                    while (CACHE.size() > 512) CACHE.remove(CACHE.keySet().iterator().next());
                  }
                  if (MapService.session(p) != s || !MapService.owns(p)) return;
                  if (result == null) {
                    s.terrain.remove(id);
                    reject(p, s, id);
                  } else deliver(p, s, id, result);
                });
          });
    } catch (RejectedExecutionException ex) {
      s.terrain.remove(id);
      reject(p, s, id);
    }
  }

  /** Real native base terrain; no chunk requests, world writes, tickets or seed disclosure. */
  public static CompoundTag preview(
      ChunkGenerator generator,
      RandomState random,
      LevelHeightAccessor height,
      int tx,
      int tz,
      int step,
      int plane,
      long epoch,
      boolean quick,
      java.util.function.BooleanSupplier active) {
    int resolution = quick ? 8 : SIZE, stride = SIZE / resolution;
    int n = resolution + 2;
    int[] heights = new int[n * n], colors = new int[n * n], depths = new int[n * n];
    boolean below = plane != Integer.MAX_VALUE;
    var sampleHeight =
        below
            ? LevelHeightAccessor.create(
                height.getMinBuildHeight(),
                Math.max(8, Math.min(height.getHeight(), plane - height.getMinBuildHeight())))
            : height;
    var fluid =
        generator instanceof NoiseBasedChunkGenerator noise
            ? noise.generatorSettings().value().defaultFluid().getFluidState()
            : null;
    for (int z = 0; z < n; z++) {
      if (epoch != EPOCH.get() || !active.getAsBoolean()) return null;
      for (int x = 0; x < n; x++) {
        int wx = tx * SIZE * step + (x - 1) * step * stride;
        int wz = tz * SIZE * step + (z - 1) * step * stride;
        int y;
        boolean water = false, lava = false;
        y =
            generator.getFirstOccupiedHeight(
                wx, wz, Heightmap.Types.WORLD_SURFACE_WG, sampleHeight, random);
        if (y < generator.getSeaLevel()) {
          if (fluid != null) {
            water = fluid.is(FluidTags.WATER);
            lava = fluid.is(FluidTags.LAVA);
          } else if (!(generator instanceof NoiseBasedChunkGenerator)) {
            var state = generator.getBaseColumn(wx, wz, sampleHeight, random).getBlock(y);
            water = state.getFluidState().is(FluidTags.WATER);
            lava = state.getFluidState().is(FluidTags.LAVA);
          }
        }
        var biome =
            generator.getBiomeSource().getNoiseBiome(wx >> 2, y >> 2, wz >> 2, random.sampler());
        int at = z * n + x;
        heights[at] = y;
        depths[at] =
            water
                ? Math.max(
                    0,
                    y
                        - generator.getFirstOccupiedHeight(
                            wx, wz, Heightmap.Types.OCEAN_FLOOR_WG, sampleHeight, random))
                : 0;
        colors[at] = terrainColor(biome, wx, y, wz, water, lava, below);
      }
    }
    int[] raster = new int[resolution * resolution];
    for (int z = 0; z < resolution; z++)
      for (int x = 0; x < resolution; x++) {
        int at = (z + 1) * n + x + 1;
        raster[z * resolution + x] =
            shade(
                colors[at],
                heights[at],
                heights[at - 1],
                heights[at + 1],
                heights[at - n],
                heights[at + n],
                depths[at],
                step * stride);
      }
    int[] pixels = new int[SIZE * SIZE];
    for (int z = 0; z < SIZE; z++)
      for (int x = 0; x < SIZE; x++) {
        double rx = x / (double) stride, rz = z / (double) stride;
        int x0 = (int) rx,
            z0 = (int) rz,
            x1 = Math.min(resolution - 1, x0 + 1),
            z1 = Math.min(resolution - 1, z0 + 1);
        int upper = mix(raster[z0 * resolution + x0], raster[z0 * resolution + x1], rx - x0);
        int lower = mix(raster[z1 * resolution + x0], raster[z1 * resolution + x1], rx - x0);
        pixels[z * SIZE + x] = 0xff000000 | mix(upper, lower, rz - z0);
      }
    var t = new CompoundTag();
    t.putInt("tx", tx);
    t.putInt("tz", tz);
    t.putInt("step", step);
    t.putIntArray("pixels", pixels);
    t.putBoolean("quick", quick);
    return t;
  }

  private static int terrainColor(
      Holder<Biome> holder, int x, int y, int z, boolean water, boolean lava, boolean below) {
    var biome = holder.value();
    String name = holder.unwrapKey().map(k -> k.location().getPath()).orElse("");
    if (water) return biome.getWaterColor();
    if (lava) return 0xe97824;
    if (below) {
      if (name.contains("soul")) return 0x655448;
      if (name.contains("basalt")) return 0x55525c;
      if (name.contains("warped")) return 0x307a78;
      if (name.contains("crimson")) return 0x843b48;
      return 0x96524b;
    }
    if (name.contains("end")) return y <= -60 ? 0x141524 : 0xc6c997;
    if (name.contains("desert") || name.contains("beach")) return 0xd4c08b;
    if (name.contains("badlands")) return (y / 5 & 1) == 0 ? 0xb66d4c : 0xc68757;
    if (name.contains("snow")
        || name.contains("frozen")
        || name.contains("ice")
        || (y > 150 && biome.getBaseTemperature() < .3f)) return 0xdce8e9;
    if (name.contains("stony") || name.contains("rocky")) return 0x969794;
    if (name.contains("mushroom")) return 0x8c7b88;
    int grass = biome.getGrassColor(x, z);
    if (name.contains("forest") || name.contains("jungle") || name.contains("taiga")) {
      int foliage = biome.getFoliageColor();
      return mix(grass, foliage, .60);
    }
    return grass;
  }

  public static int mix(int a, int b, double factor) {
    int r = (int) (((a >> 16) & 255) * (1 - factor) + ((b >> 16) & 255) * factor);
    int g = (int) (((a >> 8) & 255) * (1 - factor) + ((b >> 8) & 255) * factor);
    int bl = (int) ((a & 255) * (1 - factor) + (b & 255) * factor);
    return (r << 16) | (g << 8) | bl;
  }

  /** Northwest light, depth-tinted water and fine elevation contours. Shared by both atlases. */
  public static int shade(
      int color, int y, int west, int east, int north, int south, int depth, int step) {
    double dx = (east - west) / (2d * step), dz = (south - north) / (2d * step);
    double light = Math.clamp(.94 + (dx + dz) * .085 / Math.sqrt(1 + dx * dx + dz * dz), .62, 1.10);
    if (depth > 0) {
      color = mix(color, 0x102e54, Math.min(.62, depth / 45d));
      light = .96;
    } else if (y > 0 && Math.floorDiv(y, 8) != Math.floorDiv(north, 8) && Math.abs(y - north) > 0)
      light *= .89;
    int r = (int) Math.min(255, ((color >> 16) & 255) * light);
    int g = (int) Math.min(255, ((color >> 8) & 255) * light);
    int b = (int) Math.min(255, (color & 255) * light);
    return 0xff000000 | (r << 16) | (g << 8) | b;
  }

  private static void deliver(ServerPlayer p, MapService.Session s, String id, CompoundTag tile) {
    s.terrain.remove(id);
    var t = tile.copy();
    t.putUUID("session", s.token);
    t.putBoolean("terrain", true);
    t.putString("tile", id);
    MapNetwork.send(p, t);
  }

  private static void reject(ServerPlayer p, MapService.Session s, String id) {
    var t = new CompoundTag();
    t.putUUID("session", s.token);
    t.putBoolean("terrain", true);
    t.putBoolean("retry", true);
    t.putString("tile", id);
    MapNetwork.send(p, t);
  }

  public static CompoundTag detail(Level level, ChunkAccess chunk, int plane) {
    int[] blocks = new int[256],
        heights = new int[256],
        tints = new int[256],
        depths = new int[256];
    var pos = new BlockPos.MutableBlockPos();
    var cp = chunk.getPos();
    for (int z = 0; z < 16; z++)
      for (int x = 0; x < 16; x++) {
        int wx = cp.getMinBlockX() + x, wz = cp.getMinBlockZ() + z;
        int y =
            plane == Integer.MAX_VALUE
                ? chunk.getHeight(
                    chunk instanceof LevelChunk
                        ? Heightmap.Types.WORLD_SURFACE
                        : Heightmap.Types.WORLD_SURFACE_WG,
                    x,
                    z)
                : Math.min(plane, level.getMaxBuildHeight() - 1);
        y = Math.max(level.getMinBuildHeight(), y);
        pos.set(wx, y, wz);
        var state = chunk.getBlockState(pos);
        while (y > level.getMinBuildHeight()
            && (state.isAir() || state.getMapColor(level, pos).col == 0)) {
          pos.setY(--y);
          state = chunk.getBlockState(pos);
        }
        int at = z * 16 + x;
        blocks[at] = Block.getId(state);
        heights[at] = y;
        var biome = chunk.getNoiseBiome(wx >> 2, y >> 2, wz >> 2).value();
        tints[at] = -1;
        if (state.getFluidState().is(FluidTags.WATER)) {
          tints[at] = biome.getWaterColor();
          int bottom = y;
          while (bottom > level.getMinBuildHeight()
              && y - bottom < 64
              && chunk.getBlockState(pos.setY(bottom)).getFluidState().is(FluidTags.WATER))
            bottom--;
          depths[at] = y - bottom;
        } else if (state.is(Blocks.GRASS_BLOCK)
            || state.is(Blocks.SHORT_GRASS)
            || state.is(Blocks.FERN)) tints[at] = biome.getGrassColor(wx, wz);
        else if (state.is(Blocks.BIRCH_LEAVES)) tints[at] = 0x80a755;
        else if (state.is(Blocks.SPRUCE_LEAVES)) tints[at] = 0x619961;
        else if (state.is(BlockTags.LEAVES) && !state.is(Blocks.CHERRY_LEAVES))
          tints[at] = biome.getFoliageColor();
      }
    var t = new CompoundTag();
    t.putInt("tx", cp.x);
    t.putInt("tz", cp.z);
    t.putInt("step", 1);
    t.putBoolean("detail", true);
    t.putIntArray("blocks", blocks);
    t.putIntArray("heights", heights);
    t.putIntArray("tints", tints);
    t.putIntArray("depths", depths);
    return t;
  }

  public static void tick() {
    ACTIVE.removeIf(CompletableFuture::isDone);
    ticks++;
  }

  public static void advance(ServerPlayer p, MapService.Session s) {
    var iterator = s.details.entrySet().iterator();
    while (iterator.hasNext()) {
      var e = iterator.next();
      var pending = e.getValue();
      if (!pending.future.isDone()) {
        ((MapChunkAccess) pending.level.getChunkSource())
            .additions$mapTickets()
            .addTicket(
                TICKET,
                new ChunkPos(pending.x, pending.z),
                net.minecraft.server.level.ChunkLevel.byStatus(ChunkStatus.FEATURES),
                pending.token);
        continue;
      }
      iterator.remove();
      release(pending);
      try {
        var result = pending.future.getNow(null);
        if (result != null && result.isSuccess()) {
          var chunk = result.orElse(null);
          MapService.capture(p.serverLevel(), chunk);
          deliver(p, s, e.getKey(), detail(p.serverLevel(), chunk, pending.plane));
          continue;
        }
      } catch (RuntimeException ignored) {
      }
      s.terrain.remove(e.getKey());
      reject(p, s, e.getKey());
    }
  }

  private static void release(Pending pending) {
    ((MapChunkAccess) pending.level.getChunkSource())
        .additions$mapTickets()
        .removeTicket(
            TICKET,
            new ChunkPos(pending.x, pending.z),
            net.minecraft.server.level.ChunkLevel.byStatus(ChunkStatus.FEATURES),
            pending.token);
  }

  public static void cancel(MapService.Session session) {
    session.details.values().forEach(MapTerrain::release);
    session.details.clear();
    session.terrain.clear();
  }

  public static void clear() {
    nextDetail = ticks;
    EPOCH.incrementAndGet();
    CACHE.clear();
    ACTIVE.clear();
    WORKER.getQueue().clear();
  }

  private MapTerrain() {}
}
