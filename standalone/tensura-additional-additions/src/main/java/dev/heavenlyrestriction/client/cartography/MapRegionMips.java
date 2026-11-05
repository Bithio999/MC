package dev.heavenlyrestriction.client.cartography;

import com.mojang.blaze3d.platform.NativeImage;
import dev.heavenlyrestriction.cartography.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Small, asynchronous recorded-region textures for distant zooms, with a fixed byte budget. */
public final class MapRegionMips {
  private record Key(int x, int z, int step) {}

  private static final LinkedHashMap<Key, Tile> TILES = new LinkedHashMap<>(32, .75f, true);
  private static long bytes;
  private static final long MAX_BYTES = 32L * 1024 * 1024;

  private static final class Tile {
    final Key key;
    final NativeImage image;
    final BitSet fresh = new BitSet(256);
    DynamicTexture texture;
    ResourceLocation id;
    boolean loading, readComplete, upload;
    volatile boolean closed;

    Tile(Key key) {
      this.key = key;
      image = new NativeImage(256 / key.step, 256 / key.step, true);
    }

    void close() {
      closed = true;
      if (texture != null) {
        Minecraft.getInstance().getTextureManager().release(id);
        texture.close();
      } else image.close();
    }
  }

  public static void clear() {
    TILES.values().forEach(Tile::close);
    TILES.clear();
    bytes = 0;
  }

  private static Tile tile(int x, int z, int step, Path folder, NativeImage live, BitSet fresh) {
    var key = new Key(x, z, step);
    var tile = TILES.get(key);
    if (tile == null) {
      tile = new Tile(key);
      TILES.put(key, tile);
      bytes += (long) tile.image.getWidth() * tile.image.getHeight() * 4;
      if (live != null) copyFresh(tile, live, fresh);
      while (bytes > MAX_BYTES || TILES.size() > 8192) {
        var it = TILES.values().iterator();
        var old = it.next();
        it.remove();
        bytes -= (long) old.image.getWidth() * old.image.getHeight() * 4;
        old.close();
      }
    }
    if (!tile.loading && !tile.readComplete) load(tile, folder);
    return tile;
  }

  private static void load(Tile tile, Path folder) {
    tile.loading = true;
    MapAtlas.submit(
        () -> {
          if (tile.closed) return;
          NativeImage result = null;
          var source = folder.resolve("r_" + tile.key.x + "_" + tile.key.z + ".png");
          var cache =
              folder.resolve("mip_" + tile.key.step + "_" + tile.key.x + "_" + tile.key.z + ".png");
          try {
            if (Files.isRegularFile(source)) {
              var timestamp = Files.getLastModifiedTime(source);
              if (Files.isRegularFile(cache)
                  && Files.getLastModifiedTime(cache).equals(timestamp)) {
                try {
                  result = NativeImage.read(Files.readAllBytes(cache));
                } catch (Exception ignored) {
                }
                if (result != null
                    && (result.getWidth() != 256 / tile.key.step
                        || result.getHeight() != 256 / tile.key.step)) {
                  result.close();
                  result = null;
                }
              }
              if (result == null)
                try (var original = NativeImage.read(Files.readAllBytes(source))) {
                  if (original.getWidth() == 256 && original.getHeight() == 256) {
                    result = new NativeImage(256 / tile.key.step, 256 / tile.key.step, true);
                    var pixels = (java.util.function.IntBinaryOperator) original::getPixelRGBA;
                    for (int z = 0; z < result.getHeight(); z++)
                      for (int x = 0; x < result.getWidth(); x++)
                        result.setPixelRGBA(
                            x,
                            z,
                            MapColorMip.sample(
                                pixels, x * tile.key.step, z * tile.key.step, tile.key.step));
                    Path temporary = cache.resolveSibling(cache.getFileName() + ".tmp");
                    try {
                      result.writeToFile(temporary);
                      try {
                        Files.move(
                            temporary,
                            cache,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                      } catch (AtomicMoveNotSupportedException ex) {
                        Files.move(temporary, cache, StandardCopyOption.REPLACE_EXISTING);
                      }
                      Files.setLastModifiedTime(cache, timestamp);
                    } catch (Exception ignored) {
                      Files.deleteIfExists(temporary);
                    }
                  }
                }
            }
          } catch (Exception ignored) {
          }
          NativeImage completed = result;
          Minecraft.getInstance()
              .execute(
                  () -> {
                    if (tile.closed) {
                      if (completed != null) completed.close();
                      return;
                    }
                    tile.loading = false;
                    tile.readComplete = true;
                    if (completed != null) {
                      int step = tile.key.step;
                      for (int z = 0; z < completed.getHeight(); z++)
                        for (int x = 0; x < completed.getWidth(); x++)
                          if (!tile.fresh.get(((z * step) >> 4) * 16 + ((x * step) >> 4)))
                            tile.image.setPixelRGBA(x, z, completed.getPixelRGBA(x, z));
                      completed.close();
                      tile.upload = true;
                    }
                  });
        },
        () -> tile.loading = false);
  }

  private static void copyFresh(Tile tile, NativeImage live, BitSet chunks) {
    for (int index = chunks.nextSetBit(0); index >= 0; index = chunks.nextSetBit(index + 1))
      copyChunk(tile, live, index % 16 * 16, index / 16 * 16);
  }

  private static void copyChunk(Tile tile, NativeImage live, int bx, int bz) {
    int step = tile.key.step;
    var pixels = (java.util.function.IntBinaryOperator) live::getPixelRGBA;
    for (int z = bz; z < bz + 16; z += step)
      for (int x = bx; x < bx + 16; x += step)
        tile.image.setPixelRGBA(x / step, z / step, MapColorMip.sample(pixels, x, z, step));
    tile.fresh.set((bz >> 4) * 16 + (bx >> 4));
    tile.upload = true;
  }

  public static void changed(int x, int z, NativeImage live, int bx, int bz) {
    for (int step : new int[] {2, 4, 8}) {
      var tile = TILES.get(new Key(x, z, step));
      if (tile != null) copyChunk(tile, live, bx, bz);
    }
  }

  public static void draw(
      GuiGraphics g,
      MapView view,
      int x,
      int z,
      int step,
      Path folder,
      NativeImage live,
      BitSet fresh,
      int left,
      int top,
      int width,
      int height) {
    var tile = tile(x, z, step, folder, live, fresh);
    if (tile.texture == null) {
      tile.texture = new DynamicTexture(tile.image);
      tile.texture.setFilter(false, false);
      tile.id =
          Minecraft.getInstance()
              .getTextureManager()
              .register("marauders_map_region_mip", tile.texture);
      tile.upload = false;
    } else if (tile.upload) {
      tile.texture.upload();
      tile.upload = false;
    }
    int sx = (int) Math.floor(view.screenX(x * 256d, left, width)),
        sy = (int) Math.floor(view.screenZ(z * 256d, top, height));
    int ex = (int) Math.ceil(view.screenX((x + 1d) * 256, left, width)),
        ey = (int) Math.ceil(view.screenZ((z + 1d) * 256, top, height));
    int size = 256 / step;
    g.blit(tile.id, sx, sy, ex - sx, ey - sy, 0f, 0f, size, size, size, size);
  }

  private MapRegionMips() {}
}
