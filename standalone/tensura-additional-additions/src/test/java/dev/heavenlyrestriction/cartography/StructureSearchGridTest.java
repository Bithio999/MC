package dev.heavenlyrestriction.cartography;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

final class StructureSearchGridTest {
  @Test
  void visitsEveryCellExactlyOnceIncludingNegativeCoordinates() {
    for (int radius : new int[] {0, 1, 2, 7}) {
      var grid = new StructureSearchGrid(-12, 19, radius);
      var cells = new HashSet<StructureSearchGrid.Cell>();
      while (grid.hasNext()) {
        var cell = grid.next();
        assertTrue(cells.add(cell));
        assertTrue(Math.abs(cell.x() + 12) <= radius && Math.abs(cell.z() - 19) <= radius);
      }
      assertEquals((2 * radius + 1) * (2 * radius + 1), cells.size());
      assertThrows(NoSuchElementException.class, grid::next);
    }
  }

  @Test
  void visitsCloserRingsBeforeFartherRings() {
    var grid = new StructureSearchGrid(0, 0, 10);
    int ring = -1;
    while (grid.hasNext()) {
      var cell = grid.next();
      int next = Math.max(Math.abs(cell.x()), Math.abs(cell.z()));
      assertTrue(next >= ring);
      ring = next;
    }
    assertEquals(10, ring);
  }

  @Test
  void hundredThousandBlockRegionSearchDoesNotRequireAChunkSizedGrid() {
    int rangeChunks = 100000 / 16, spacing = 32;
    int radius = (rangeChunks + spacing - 1) / spacing + 1;
    long regions = (2L * radius + 1) * (2L * radius + 1);
    long chunks = (2L * rangeChunks + 1) * (2L * rangeChunks + 1);
    assertTrue(regions < chunks / 900);
    var grid = new StructureSearchGrid(-58600, 58600, radius);
    assertEquals(new StructureSearchGrid.Cell(-58600, 58600), grid.next());
  }

  @Test
  void rejectsOverflowAndInvalidBounds() {
    assertThrows(IllegalArgumentException.class, () -> new StructureSearchGrid(0, 0, -1));
    assertThrows(
        IllegalArgumentException.class, () -> new StructureSearchGrid(Integer.MAX_VALUE, 0, 2));
    assertThrows(
        IllegalArgumentException.class, () -> new StructureSearchGrid(0, Integer.MIN_VALUE, 2));
  }
}
