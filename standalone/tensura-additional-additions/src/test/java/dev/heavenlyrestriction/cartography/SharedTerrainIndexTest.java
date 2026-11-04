package dev.heavenlyrestriction.cartography;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SharedTerrainIndexTest {
  @Test
  void exactSamplesRespectNegativeCoordinatesLayersAndUnknownTerrain() {
    var index = new SharedTerrainIndex();
    index.put("overworld", -1, -2, 0xff123456);
    index.put("nether", -1, -2, 0xffad3021);
    assertEquals(0xff123456, index.sample("overworld", -1, -17, 16));
    assertEquals(0xffad3021, index.sample("nether", -1, -17, 16));
    assertEquals(0, index.sample("overworld", 0, -17, 16));
    assertEquals(0, index.sample("missing", -1, -17, 16));
  }

  @Test
  void coarsePixelsAverageExploredChunksAndUpdatesReplaceOldContributions() {
    var index = new SharedTerrainIndex();
    index.put("surface", 0, 0, 0xff204060);
    index.put("surface", 1, 0, 0xff406080);
    assertEquals(0xff305070, index.sample("surface", 0, 0, 32));
    assertTrue(index.put("surface", 0, 0, 0xff6080a0));
    assertEquals(0xff507090, index.sample("surface", 0, 0, 32));
    assertFalse(index.put("surface", 0, 0, 0xff6080a0));
    assertEquals(2, index.size());
  }

  @Test
  void missingDetailCanBeRemovedWithoutContaminatingCoarseLevels() {
    var index = new SharedTerrainIndex();
    index.put("surface", -2, -2, 0xff112233);
    index.put("surface", -1, -1, 0xff334455);
    assertEquals(0xff223344, index.sample("surface", -32, -32, 32));
    assertTrue(index.remove("surface", -1, -1));
    assertEquals(0xff112233, index.sample("surface", -32, -32, 32));
    assertEquals(0, index.sample("surface", -1, -1, 16));
    assertFalse(index.remove("surface", -1, -1));
    assertTrue(index.remove("surface", -2, -2));
    assertEquals(0, index.sample("surface", -32, -32, 512));
  }

  @Test
  void serializedBaseRebuildsTheSamePyramidAndClearIsComplete() {
    var a = new SharedTerrainIndex();
    a.put("surface", 145, 341, 0xffaabbcc);
    a.put("below64", -12, 4, 0xff123456);
    var b = new SharedTerrainIndex();
    a.entries().forEach(e -> b.put(e.layer(), e.x(), e.z(), e.color()));
    assertEquals(a.entries(), b.entries());
    assertEquals(
        a.sample("surface", 145 * 16, 341 * 16, 512), b.sample("surface", 145 * 16, 341 * 16, 512));
    b.clear();
    assertEquals(0, b.size());
    assertEquals(0, b.sample("surface", 145 * 16, 341 * 16, 512));
  }
}
