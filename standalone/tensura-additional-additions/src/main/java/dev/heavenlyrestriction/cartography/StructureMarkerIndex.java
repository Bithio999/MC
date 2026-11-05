package dev.heavenlyrestriction.cartography;

import java.util.*;

/** Dimension-separated spatial buckets for the bounded, saved structure registry. */
public final class StructureMarkerIndex<T> {
  private record Cell(String dimension, int x, int z) {}

  private record Entry<T>(Cell cell, int x, int z, T value) {}

  private final Map<String, Entry<T>> entries = new HashMap<>();
  private final Map<String, Map<Cell, Map<String, Entry<T>>>> dimensions = new HashMap<>();

  public void put(String id, String dimension, int x, int z, T value) {
    remove(id);
    var cell = new Cell(dimension, Math.floorDiv(x, 512), Math.floorDiv(z, 512));
    var entry = new Entry<>(cell, x, z, value);
    entries.put(id, entry);
    dimensions
        .computeIfAbsent(dimension, k -> new HashMap<>())
        .computeIfAbsent(cell, k -> new HashMap<>())
        .put(id, entry);
  }

  public void remove(String id) {
    var entry = entries.remove(id);
    if (entry == null) return;
    var cells = dimensions.get(entry.cell.dimension);
    var bucket = cells.get(entry.cell);
    bucket.remove(id);
    if (bucket.isEmpty()) cells.remove(entry.cell);
    if (cells.isEmpty()) dimensions.remove(entry.cell.dimension);
  }

  public void clear() {
    entries.clear();
    dimensions.clear();
  }

  public List<T> query(String dimension, int minX, int minZ, int maxX, int maxZ) {
    if (minX > maxX || minZ > maxZ) return List.of();
    var cells = dimensions.get(dimension);
    if (cells == null) return List.of();
    int left = Math.floorDiv(minX, 512), right = Math.floorDiv(maxX, 512);
    int top = Math.floorDiv(minZ, 512), bottom = Math.floorDiv(maxZ, 512);
    var found = new ArrayList<T>();
    java.util.function.Consumer<Map<String, Entry<T>>> collect =
        bucket -> {
          if (bucket == null) return;
          for (var entry : bucket.values())
            if (entry.x >= minX && entry.x <= maxX && entry.z >= minZ && entry.z <= maxZ)
              found.add(entry.value);
        };
    // Sparse worlds and very wide viewports should not iterate millions of empty cells.
    if ((long) (right - left + 1) * (bottom - top + 1) > cells.size())
      cells.values().forEach(collect);
    else
      for (int z = top; z <= bottom; z++)
        for (int x = left; x <= right; x++) collect.accept(cells.get(new Cell(dimension, x, z)));
    return found;
  }
}
