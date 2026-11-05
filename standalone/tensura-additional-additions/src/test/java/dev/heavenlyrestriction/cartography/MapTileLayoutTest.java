package dev.heavenlyrestriction.cartography;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class MapTileLayoutTest {
  @Test
  void allSupportedZoomsAndGuiSizesHaveBoundedCrispTiles() {
    for (int width : new int[] {320, 640, 1280, 1920})
      for (int height : new int[] {180, 360, 720, 1080})
        for (double scale : new double[] {.125, .17, .25, .5, 1, 2, 4, 8}) {
          int step = MapTileLayout.step(scale, width, height);
          assertTrue(step >= 1 && step <= 512 && (step & (step - 1)) == 0);
          assertTrue(
              MapTileLayout.tileBudget(scale, width, height, step) <= MapTileLayout.MAX_TILES);
          if (step > 1)
            assertTrue(
                MapTileLayout.tileBudget(scale, width, height, step / 2) > MapTileLayout.MAX_TILES
                    || step <= 1 / scale,
                "unnecessarily coarse LOD");
        }
  }

  @Test
  void zoomedOutMapRetainsScreenDensityInsteadOfEnlargingEightPixelPreviews() {
    assertEquals(8, MapTileLayout.step(.125, 600, 300));
    assertEquals(1, MapTileLayout.step(1, 600, 300));
    assertEquals(1, MapTileLayout.step(8, 600, 300));
  }
}
