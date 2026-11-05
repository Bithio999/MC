package dev.heavenlyrestriction.cartography;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class MapColorMipTest {
  @Test
  void partialExplorationDoesNotFadeIntoBlack() {
    assertEquals(
        0xff3377bb, MapColorMip.sample((x, z) -> x == 0 && z == 0 ? 0xff3377bb : 0, 0, 0, 8));
    assertEquals(0, MapColorMip.sample((x, z) -> 0, 0, 0, 8));
  }

  @Test
  void distantPixelsRetainSubchunkFeaturesInsteadOfOneColorPerChunk() {
    java.util.function.IntBinaryOperator road = (x, z) -> x < 8 ? 0xffd0d0d0 : 0xff207020;
    assertEquals(0xffd0d0d0, MapColorMip.sample(road, 0, 0, 8));
    assertEquals(0xff207020, MapColorMip.sample(road, 8, 0, 8));
    assertNotEquals(MapColorMip.sample(road, 0, 0, 8), MapColorMip.sample(road, 0, 0, 16));
  }

  @Test
  void areaFilteringPreservesChannelsAndUsesAlphaWeights() {
    assertEquals(
        0xff7f007f, MapColorMip.sample((x, z) -> x % 2 == 0 ? 0xff0000ff : 0xffff0000, 0, 0, 2));
    assertEquals(0xff123456, MapColorMip.sample((x, z) -> 0x80123456, 0, 0, 4));
  }
}
