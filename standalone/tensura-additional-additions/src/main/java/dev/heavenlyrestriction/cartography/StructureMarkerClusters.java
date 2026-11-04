package dev.heavenlyrestriction.cartography;

import java.util.*;

/** Bounded packets without dropping sites: every group retains its full represented count. */
public final class StructureMarkerClusters {
  public record Point<T>(T value, int x, int z, String type) {}

  public record Group<T>(T representative, int count) {}

  public static <T> List<Group<T>> group(List<Point<T>> points, int limit) {
    if (limit < 1) throw new IllegalArgumentException("Invalid marker limit");
    if (points.size() <= limit) return points.stream().map(p -> new Group<>(p.value, 1)).toList();
    long minX = points.stream().mapToInt(Point::x).min().orElse(0),
        minZ = points.stream().mapToInt(Point::z).min().orElse(0);
    for (long cell = 64; ; cell *= 2) {
      record Cell(long x, long z, String type) {}
      var bins = new LinkedHashMap<Cell, Group<T>>();
      for (var p : points) {
        var key =
            new Cell(
                ((long) p.x - minX) / cell,
                ((long) p.z - minZ) / cell,
                cell < 1L << 33 ? p.type : "");
        var old = bins.get(key);
        bins.put(
            key,
            old == null ? new Group<>(p.value, 1) : new Group<>(old.representative, old.count + 1));
      }
      if (bins.size() <= limit) return List.copyOf(bins.values());
    }
  }

  private StructureMarkerClusters() {}
}
