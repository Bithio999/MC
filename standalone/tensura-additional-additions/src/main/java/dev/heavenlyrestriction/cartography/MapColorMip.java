package dev.heavenlyrestriction.cartography;

import java.util.function.IntBinaryOperator;

/** Area filtering at the displayed block footprint, never whole-chunk color interpolation. */
public final class MapColorMip {
  public static int sample(IntBinaryOperator pixels, int x, int z, int step) {
    long r = 0, g = 0, b = 0, weight = 0;
    for (int dz = 0; dz < step; dz++)
      for (int dx = 0; dx < step; dx++) {
        int color = pixels.applyAsInt(x + dx, z + dz), alpha = color >>> 24;
        r += (color & 255) * alpha;
        g += ((color >> 8) & 255) * alpha;
        b += ((color >> 16) & 255) * alpha;
        weight += alpha;
      }
    return weight == 0
        ? 0
        : 0xff000000 | (int) (r / weight) | ((int) (g / weight) << 8) | ((int) (b / weight) << 16);
  }

  private MapColorMip() {}
}
