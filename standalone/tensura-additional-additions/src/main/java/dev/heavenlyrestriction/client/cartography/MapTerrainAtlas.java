package dev.heavenlyrestriction.client.cartography;

import com.mojang.blaze3d.platform.NativeImage;
import dev.heavenlyrestriction.cartography.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;

/**
 * Adaptive preview tiles and 4x4 texel-per-block native surface detail; all GL work stays on
 * client.
 */
public final class MapTerrainAtlas {
  private record Key(int x, int z, int step, boolean detail, boolean quick) {
    String id() {
      return (detail ? "d/" : quick ? "q/" : "p/") + x + "/" + z + "/" + step;
    }

    int span() {
      return detail ? 16 : MapTerrain.SIZE * step;
    }
  }

  private static final class Tile {
    final DynamicTexture texture;
    final ResourceLocation id;
    DynamicTexture explored;
    ResourceLocation exploredId;
    final long received = age;
    boolean shared;

    Tile(NativeImage image) {
      texture = new DynamicTexture(image);
      id = Minecraft.getInstance().getTextureManager().register("marauders_map_terrain", texture);
    }

    void close() {
      Minecraft.getInstance().getTextureManager().release(id);
      texture.close();
      if (explored != null) {
        Minecraft.getInstance().getTextureManager().release(exploredId);
        explored.close();
      }
    }
  }

  private static final Map<Key, Tile> TILES = new LinkedHashMap<>(32, .75f, true);
  private static final Map<String, Long> WAITING = new HashMap<>();
  private static final Map<Integer, int[]> PALETTE = new HashMap<>();
  private static List<Key> desired = List.of();
  private static long age;
  private static UUID session;
  private static int visible, ready;

  public static void session(UUID value) {
    if (!Objects.equals(session, value)) {
      session = value;
      WAITING.clear();
      desired = List.of();
    }
  }

  public static void clear() {
    TILES.values().forEach(Tile::close);
    TILES.clear();
    WAITING.clear();
    PALETTE.clear();
    desired = List.of();
    session = null;
    ready = visible = 0;
  }

  public static String progress() {
    return visible > ready ? "Mapping terrain · " + ready + "/" + visible + " tiles" : "";
  }

  public static void tick() {
    age++;
    if (session == null
        || !(Minecraft.getInstance().screen instanceof MaraudersMapScreen map)
        || !map.terrainViewReady()) return;
    WAITING.entrySet().removeIf(e -> e.getValue() < age);
    int pending = (int) WAITING.values().stream().filter(expiry -> expiry - age > 60).count();
    if (pending >= 6) return;
    for (var key : desired) {
      if ((TILES.containsKey(key) && age - TILES.get(key).received < 600)
          || WAITING.containsKey(key.id())) continue;
      var t = new CompoundTag();
      t.putInt("tx", key.x);
      t.putInt("tz", key.z);
      t.putInt("step", key.step);
      t.putBoolean("detail", key.detail);
      t.putBoolean("quick", key.quick);
      MapNetwork.send(session, "terrain", t);
      WAITING.put(key.id(), age + 600);
      if (++pending >= 6) break;
    }
  }

  public static void local(net.minecraft.world.level.chunk.LevelChunk chunk, int plane) {
    if (session == null) return;
    var t = MapTerrain.detail(Minecraft.getInstance().level, chunk, plane);
    var key = new Key(chunk.getPos().x, chunk.getPos().z, 1, true, false);
    t.putUUID("session", session);
    t.putString("tile", key.id());
    receive(t);
  }

  public static void receive(CompoundTag t) {
    if (session == null || !t.hasUUID("session") || !session.equals(t.getUUID("session"))) return;
    String id = t.getString("tile");
    if (t.getBoolean("retry")) {
      WAITING.put(id, age + 40);
      return;
    }
    WAITING.remove(id);
    boolean detail = t.getBoolean("detail");
    int step = t.getInt("step");
    if (step < 1 || step > 512 || (step & (step - 1)) != 0) return;
    var key = new Key(t.getInt("tx"), t.getInt("tz"), step, detail, t.getBoolean("quick"));
    NativeImage image;
    if (detail) {
      int[] blocks = t.getIntArray("blocks"),
          heights = t.getIntArray("heights"),
          tints = t.getIntArray("tints"),
          depths = t.getIntArray("depths");
      if (blocks.length != 256
          || heights.length != 256
          || tints.length != 256
          || depths.length != 256) return;
      image = new NativeImage(64, 64, true);
      for (int z = 0; z < 16; z++)
        for (int x = 0; x < 16; x++) {
          int at = z * 16 + x;
          int[] pixels = palette(blocks[at]);
          for (int py = 0; py < 4; py++)
            for (int px = 0; px < 4; px++) {
              int color = pixels[py * 4 + px];
              if (tints[at] != -1) color = multiply(color, tints[at]);
              color =
                  MapTerrain.shade(
                      color,
                      heights[at],
                      heights[z * 16 + Math.max(0, x - 1)],
                      heights[z * 16 + Math.min(15, x + 1)],
                      heights[Math.max(0, z - 1) * 16 + x],
                      heights[Math.min(15, z + 1) * 16 + x],
                      depths[at],
                      1);
              image.setPixelRGBA(x * 4 + px, z * 4 + py, abgr(color));
            }
        }
    } else {
      int[] pixels = t.getIntArray("pixels");
      if (pixels.length != 1024) return;
      image = new NativeImage(32, 32, true);
      for (int z = 0; z < 32; z++)
        for (int x = 0; x < 32; x++) image.setPixelRGBA(x, z, abgr(pixels[z * 32 + x]));
    }
    var tile = new Tile(image);
    tile.shared = t.getBoolean("shared");
    int[] recorded = t.getIntArray("explored");
    if (!detail
        && recorded.length == 1024
        && java.util.Arrays.stream(recorded).anyMatch(c -> c != 0)) {
      var overlay = new NativeImage(32, 32, true);
      for (int z = 0; z < 32; z++)
        for (int x = 0; x < 32; x++) overlay.setPixelRGBA(x, z, abgr(recorded[z * 32 + x]));
      tile.explored = new DynamicTexture(overlay);
      tile.exploredId =
          Minecraft.getInstance()
              .getTextureManager()
              .register("marauders_map_shared", tile.explored);
    }
    var previous = TILES.put(key, tile);
    if (previous != null) previous.close();
    while (TILES.size() > 768) {
      var it = TILES.entrySet().iterator();
      var old = it.next();
      it.remove();
      old.getValue().close();
    }
  }

  /**
   * Actual resource-pack top-face texture, averaged into sixteen texels, never a solid map swatch.
   */
  private static int[] palette(int blockId) {
    return PALETTE.computeIfAbsent(
        blockId,
        id -> {
          var state = Block.stateById(id);
          var mc = Minecraft.getInstance();
          var model = mc.getBlockRenderer().getBlockModel(state);
          var quads = model.getQuads(state, Direction.UP, RandomSource.create(42));
          if (quads.isEmpty()) quads = model.getQuads(state, null, RandomSource.create(42));
          var sprite = quads.isEmpty() ? model.getParticleIcon() : quads.getFirst().getSprite();
          // Fluid block models are invisible; choose their actual still-fluid atlas sprite.
          if (!state.getFluidState().isEmpty()) {
            var fluid =
                net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(
                    state.getFluidState());
            var still = fluid.getStillTexture();
            if (still != null)
              sprite =
                  mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                      .apply(still);
          }
          var contents = sprite.contents();
          int w = contents.width(), h = contents.height();
          int[] out = new int[16];
          for (int gy = 0; gy < 4; gy++)
            for (int gx = 0; gx < 4; gx++) {
              long red = 0, green = 0, blue = 0, weight = 0;
              int x0 = gx * w / 4, x1 = Math.max(x0 + 1, (gx + 1) * w / 4);
              int y0 = gy * h / 4, y1 = Math.max(y0 + 1, (gy + 1) * h / 4);
              for (int y = y0; y < y1; y++)
                for (int x = x0; x < x1; x++) {
                  int color = sprite.getPixelRGBA(0, Math.min(w - 1, x), Math.min(h - 1, y));
                  int a = color >>> 24;
                  red += (color & 255) * a;
                  green += ((color >> 8) & 255) * a;
                  blue += ((color >> 16) & 255) * a;
                  weight += a;
                }
              out[gy * 4 + gx] =
                  weight == 0
                      ? 0x777777
                      : ((int) (red / weight) << 16)
                          | ((int) (green / weight) << 8)
                          | (int) (blue / weight);
            }
          return out;
        });
  }

  public static int surfaceColor(int block, int tint) {
    int[] pixels = palette(block);
    long r = 0, g = 0, b = 0;
    for (int color : pixels) {
      r += (color >> 16) & 255;
      g += (color >> 8) & 255;
      b += color & 255;
    }
    int color = ((int) (r / 16) << 16) | ((int) (g / 16) << 8) | (int) (b / 16);
    return tint == -1 ? color : multiply(color, tint);
  }

  private static int multiply(int color, int tint) {
    return (((color >> 16) & 255) * ((tint >> 16) & 255) / 255 << 16)
        | (((color >> 8) & 255) * ((tint >> 8) & 255) / 255 << 8)
        | ((color & 255) * (tint & 255) / 255);
  }

  private static int abgr(int c) {
    return (c & 0xff00ff00) | ((c >> 16) & 255) | ((c & 255) << 16);
  }

  private static List<Key> keys(
      MapView v, int left, int top, int width, int height, int step, boolean detail) {
    return keys(v, left, top, width, height, step, detail, false);
  }

  private static List<Key> keys(
      MapView v,
      int left,
      int top,
      int width,
      int height,
      int step,
      boolean detail,
      boolean quick) {
    int span = detail ? 16 : 32 * step;
    int minX = Math.floorDiv((int) Math.floor(v.worldX(left, left, width)), span),
        maxX = Math.floorDiv((int) Math.floor(v.worldX(left + width, left, width)), span),
        minZ = Math.floorDiv((int) Math.floor(v.worldZ(top, top, height)), span),
        maxZ = Math.floorDiv((int) Math.floor(v.worldZ(top + height, top, height)), span);
    var out = new ArrayList<Key>();
    if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > 4096) return out;
    for (int z = minZ; z <= maxZ; z++)
      for (int x = minX; x <= maxX; x++)
        if (Math.abs((long) x * span) <= MapView.LIMIT
            && Math.abs((long) z * span) <= MapView.LIMIT)
          out.add(new Key(x, z, step, detail, quick));
    out.sort(
        Comparator.comparingDouble(
            k -> Math.hypot((k.x + .5) * span - v.x, (k.z + .5) * span - v.z)));
    if (detail && out.size() > 384) return out.subList(0, 384);
    return out;
  }

  public static void render(
      GuiGraphics g, MapView v, int left, int top, int width, int height, boolean detailLayer) {
    int coarse = 1;
    while (coarse < Math.max(width, height) / v.scale / 128) coarse *= 2;
    int fine = Math.max(1, coarse / 2);
    var quick = keys(v, left, top, width, height, coarse, false, true);
    var fineKeys = keys(v, left, top, width, height, fine, false);
    var details = v.scale >= 1 ? keys(v, left, top, width, height, 1, true) : List.<Key>of();
    if (!detailLayer) {
      com.mojang.blaze3d.systems.RenderSystem.enableBlend();
      var candidates = new ArrayList<Key>();
      candidates.addAll(quick.subList(0, Math.min(4, quick.size())));
      candidates.addAll(details.subList(0, Math.min(4, details.size())));
      candidates.addAll(quick.subList(Math.min(4, quick.size()), quick.size()));
      candidates.addAll(details.subList(Math.min(4, details.size()), details.size()));
      candidates.addAll(fineKeys);
      desired = List.copyOf(candidates);
      visible = desired.size();
      ready = (int) desired.stream().filter(TILES::containsKey).count();
      // Cached coarser tiles provide continuity while the new zoom level fills.
      for (int step = Math.min(512, coarse * 4); step >= fine; step /= 2) {
        for (var k : keys(v, left, top, width, height, step, false, true))
          draw(g, TILES.get(k), k, v, left, top, width, height);
        for (var k : keys(v, left, top, width, height, step, false))
          draw(g, TILES.get(k), k, v, left, top, width, height);
        if (step == 1) break;
      }
    } else for (var k : details) draw(g, TILES.get(k), k, v, left, top, width, height);
  }

  private static void draw(
      GuiGraphics g, Tile tile, Key k, MapView v, int left, int top, int w, int h) {
    if (tile == null) return;
    int span = k.span();
    int x = (int) Math.floor(v.screenX(k.x * (double) span, left, w)),
        y = (int) Math.floor(v.screenZ(k.z * (double) span, top, h)),
        ex = (int) Math.ceil(v.screenX((k.x + 1d) * span, left, w)),
        ey = (int) Math.ceil(v.screenZ((k.z + 1d) * span, top, h));
    int size = k.detail ? 64 : 32;
    g.blit(tile.id, x, y, ex - x, ey - y, 0f, 0f, size, size, size, size);
    if (tile.exploredId != null)
      g.blit(tile.exploredId, x, y, ex - x, ey - y, 0f, 0f, 32, 32, 32, 32);
  }

  public static boolean hasPreviewAt(int x, int z) {
    return TILES.keySet().stream()
        .anyMatch(
            k ->
                !k.detail
                    && Math.floorDiv(x, k.span()) == k.x
                    && Math.floorDiv(z, k.span()) == k.z);
  }

  public static boolean hasDetailAt(int x, int z) {
    return TILES.containsKey(new Key(x >> 4, z >> 4, 1, true, false));
  }

  public static boolean hasSharedDetailAt(int x, int z) {
    var tile = TILES.get(new Key(x >> 4, z >> 4, 1, true, false));
    return tile != null && tile.shared;
  }

  public static int tileCount() {
    return TILES.size();
  }

  private MapTerrainAtlas() {}
}
