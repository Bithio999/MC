package dev.heavenlyrestriction.cartography;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

final class StructureMarkerIndexTest {
  @Test
  void exactBoundsNegativeCoordinatesAndDimensionIsolation() {
    var index = new StructureMarkerIndex<String>();
    index.put("a", "overworld", -512, -1, "village");
    index.put("b", "overworld", -513, -1, "temple");
    index.put("c", "nether", -512, -1, "fortress");
    assertEquals(List.of("village"), index.query("overworld", -512, -1, 0, 0));
    assertEquals(List.of("fortress"), index.query("nether", -512, -1, -512, -1));
    assertTrue(index.query("missing", -1000, -1000, 1000, 1000).isEmpty());
  }

  @Test
  void replacementRemovalAndClearCannotLeaveGhostMarkers() {
    var index = new StructureMarkerIndex<String>();
    index.put("same", "overworld", 0, 0, "predicted");
    index.put("same", "nether", 10000, 10000, "actual");
    assertTrue(index.query("overworld", -1, -1, 1, 1).isEmpty());
    assertEquals(
        List.of("actual"), index.query("nether", -30000000, -30000000, 30000000, 30000000));
    index.remove("same");
    assertTrue(index.query("nether", 0, 0, 20000, 20000).isEmpty());
    index.put("new", "overworld", 0, 0, "new");
    index.clear();
    assertTrue(index.query("overworld", -1, -1, 1, 1).isEmpty());
  }

  @Test
  void randomizedIndexMatchesFullScanAcrossSparseAndDenseQueries() {
    var index = new StructureMarkerIndex<Integer>();
    var expected = new HashMap<Integer, int[]>();
    var random = new Random(4169);
    for (int i = 0; i < 65536; i++) {
      int x = random.nextInt(200000) - 100000, z = random.nextInt(200000) - 100000;
      index.put("" + i, "world", x, z, i);
      expected.put(i, new int[] {x, z});
    }
    for (int radius : new int[] {0, 256, 1024, 5000, 100000}) {
      int x = -913, z = 111;
      var matches = new HashSet<Integer>();
      expected.forEach(
          (id, p) -> {
            if (Math.abs((long) p[0] - x) <= radius && Math.abs((long) p[1] - z) <= radius)
              matches.add(id);
          });
      assertEquals(
          matches,
          new HashSet<>(index.query("world", x - radius, z - radius, x + radius, z + radius)));
    }
  }
}
