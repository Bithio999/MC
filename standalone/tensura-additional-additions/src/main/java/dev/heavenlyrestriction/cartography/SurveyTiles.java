package dev.heavenlyrestriction.cartography;

import java.util.*;

/** Covers the visible rectangle, including a structure-bounds gutter, at bounded tile counts. */
public final class SurveyTiles {
  public static final int GUTTER = 512, MAX_TILES = 384, LIMIT = 29999984;

  public record Tile(int x, int z, int lod) {
    public int chunks() {
      return 32 << lod;
    }

    public int minChunkX() {
      return x * chunks();
    }

    public int minChunkZ() {
      return z * chunks();
    }
  }

  public static List<Tile> plan(int minX, int minZ, int maxX, int maxZ) {
    if (minX > maxX || minZ > maxZ) throw new IllegalArgumentException("Reversed survey bounds");
    long left = Math.max(-LIMIT, (long) minX - GUTTER),
        right = Math.min(LIMIT, (long) maxX + GUTTER);
    long top = Math.max(-LIMIT, (long) minZ - GUTTER),
        bottom = Math.min(LIMIT, (long) maxZ + GUTTER);
    int lod = 0;
    while (lod < 17) {
      int size = 512 << lod;
      long width = Math.floorDiv(right, size) - Math.floorDiv(left, size) + 1;
      long height = Math.floorDiv(bottom, size) - Math.floorDiv(top, size) + 1;
      if (width * height <= MAX_TILES) break;
      lod++;
    }
    int size = 512 << lod;
    var result = new ArrayList<Tile>();
    for (int z = (int) Math.floorDiv(top, size); z <= Math.floorDiv(bottom, size); z++)
      for (int x = (int) Math.floorDiv(left, size); x <= Math.floorDiv(right, size); x++)
        result.add(new Tile(x, z, lod));
    double cx = ((long) minX + maxX) / 2d, cz = ((long) minZ + maxZ) / 2d;
    result.sort(
        Comparator.comparingDouble(
            t -> Math.hypot((t.x + .5) * size - cx, (t.z + .5) * size - cz)));
    return List.copyOf(result);
  }

  private SurveyTiles() {}
}
