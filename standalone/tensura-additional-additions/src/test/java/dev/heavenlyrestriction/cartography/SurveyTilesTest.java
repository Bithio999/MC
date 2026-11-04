package dev.heavenlyrestriction.cartography;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

final class SurveyTilesTest {
  @Test
  void coversNegativeViewportAndItsStructureGutter() {
    var tiles = SurveyTiles.plan(-900, -300, 700, 1200);
    assertFalse(tiles.isEmpty());
    for (int z = -812; z <= 1712; z += 13)
      for (int x = -1412; x <= 1212; x += 13) {
        final int px = x, pz = z;
        assertTrue(
            tiles.stream()
                .anyMatch(
                    t ->
                        Math.floorDiv(px, t.chunks() * 16) == t.x()
                            && Math.floorDiv(pz, t.chunks() * 16) == t.z()));
      }
    assertEquals(tiles.size(), new HashSet<>(tiles).size());
  }

  @Test
  void hugeViewportsStayBoundedWithoutLeavingHoles() {
    var tiles = SurveyTiles.plan(-65536, -65536, 65536, 65536);
    assertTrue(tiles.size() <= SurveyTiles.MAX_TILES);
    assertTrue(tiles.stream().allMatch(t -> t.lod() > 0));
    for (int x : new int[] {-65536, 0, 65536})
      for (int z : new int[] {-65536, 0, 65536})
        assertTrue(
            tiles.stream()
                .anyMatch(
                    t ->
                        Math.floorDiv(x, t.chunks() * 16) == t.x()
                            && Math.floorDiv(z, t.chunks() * 16) == t.z()));
  }

  @Test
  void reversedBoundsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> SurveyTiles.plan(5, 0, 2, 10));
  }

  @Test
  void packetsAggregateAllSitesInsteadOfDroppingFarawayMarkers() {
    var points = new ArrayList<StructureMarkerClusters.Point<Integer>>();
    for (int i = 0; i < 10000; i++)
      points.add(new StructureMarkerClusters.Point<>(i, i * 3, i % 97, "village"));
    var groups = StructureMarkerClusters.group(points, 128);
    assertTrue(groups.size() <= 128);
    assertEquals(10000, groups.stream().mapToInt(StructureMarkerClusters.Group::count).sum());
  }

  @Test
  void categoriesSurviveNormalClusteringAndSparsePointsStayExact() {
    var points =
        List.of(
            new StructureMarkerClusters.Point<>("a", -20, 3, "village"),
            new StructureMarkerClusters.Point<>("b", -20, 3, "temple"));
    var groups = StructureMarkerClusters.group(points, 2);
    assertEquals(
        List.of("a", "b"),
        groups.stream().map(StructureMarkerClusters.Group::representative).toList());
    assertThrows(IllegalArgumentException.class, () -> StructureMarkerClusters.group(points, 0));
  }
}
