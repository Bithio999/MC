package dev.heavenlyrestriction.cartography;

import java.util.*;

/** Sparse mip pyramid of actual explored chunk colors; queries never scan the explored world. */
public final class SharedTerrainIndex {
  public static final int LIMIT = 262144;

  public record Entry(String layer, int x, int z, int color) {}

  private record Key(String layer, int x, int z) {}

  private static final class Sum {
    long r, g, b;
    int count;

    void add(int color, int sign) {
      r += ((color >> 16) & 255) * sign;
      g += ((color >> 8) & 255) * sign;
      b += (color & 255) * sign;
      count += sign;
    }

    int color() {
      return count == 0
          ? 0
          : 0xff000000 | ((int) (r / count) << 16) | ((int) (g / count) << 8) | (int) (b / count);
    }
  }

  private final LinkedHashMap<Key, Integer> base = new LinkedHashMap<>();
  private final Map<String, List<Map<Long, Sum>>> layers = new HashMap<>();

  private static long pack(int x, int z) {
    return ((long) x << 32) | (z & 0xffffffffL);
  }

  public boolean put(String layer, int x, int z, int color) {
    var key = new Key(layer, x, z);
    Integer old = base.get(key);
    if (old != null && old == color) return false;
    if (old != null) change(key, old, -1);
    base.put(key, color);
    change(key, color, 1);
    while (base.size() > LIMIT) {
      var first = base.entrySet().iterator();
      var entry = first.next();
      change(entry.getKey(), entry.getValue(), -1);
      first.remove();
    }
    return true;
  }

  public boolean remove(String layer, int x, int z) {
    var key = new Key(layer, x, z);
    var old = base.remove(key);
    if (old == null) return false;
    change(key, old, -1);
    return true;
  }

  private void change(Key key, int color, int sign) {
    var pyramid =
        layers.computeIfAbsent(
            key.layer,
            ignored -> {
              var list = new ArrayList<Map<Long, Sum>>();
              for (int n = 0; n <= 16; n++) list.add(new HashMap<>());
              return list;
            });
    for (int n = 0; n <= 16; n++) {
      var map = pyramid.get(n);
      long pos = pack(key.x >> n, key.z >> n);
      var sum = map.computeIfAbsent(pos, ignored -> new Sum());
      sum.add(color, sign);
      if (sum.count == 0) map.remove(pos);
    }
  }

  public int sample(String layer, int blockX, int blockZ, int step) {
    var pyramid = layers.get(layer);
    if (pyramid == null) return 0;
    int level = Math.clamp(31 - Integer.numberOfLeadingZeros(Math.max(16, step)) - 4, 0, 16);
    var sum = pyramid.get(level).get(pack(blockX >> (4 + level), blockZ >> (4 + level)));
    return sum == null ? 0 : sum.color();
  }

  public List<Entry> entries() {
    return base.entrySet().stream()
        .map(e -> new Entry(e.getKey().layer, e.getKey().x, e.getKey().z, e.getValue()))
        .toList();
  }

  public void clear() {
    base.clear();
    layers.clear();
  }

  public int size() {
    return base.size();
  }
}
