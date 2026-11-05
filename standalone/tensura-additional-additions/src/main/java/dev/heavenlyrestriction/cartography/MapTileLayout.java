package dev.heavenlyrestriction.cartography;

/** Screen-density LOD with a bounded working set; independent of Minecraft rendering. */
public final class MapTileLayout {
  public static final int MAX_TILES = 256;

  public static int step(double scale, int width, int height) {
    int step = Integer.highestOneBit(Math.max(1, (int) (1 / scale)));
    while (step < 512 && tileBudget(scale, width, height, step) > MAX_TILES) step *= 2;
    return Math.min(512, step);
  }

  public static long tileBudget(double scale, int width, int height, int step) {
    return ((long) Math.ceil(width / (32d * step * scale)) + 1)
        * ((long) Math.ceil(height / (32d * step * scale)) + 1);
  }

  private MapTileLayout() {}
}
