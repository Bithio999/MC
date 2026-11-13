package dev.heavenlyrestriction.laplace.expand;

import java.util.*;

/** Primitive hooks have no game-class dependency during Mixin's early bootstrap. */
public final class ParameterRuntime {
  private static final ThreadLocal<Map<String, Double>> CURRENT = new ThreadLocal<>();

  public static boolean active() {
    return CURRENT.get() != null;
  }

  public static final class Scope implements AutoCloseable {
    private final Map<String, Double> before;

    public Scope(Map<String, Double> values) {
      before = CURRENT.get();
      CURRENT.set(values);
    }

    public void close() {
      if (before == null) CURRENT.remove();
      else CURRENT.set(before);
    }
  }

  public static double decimal(double value, String key) {
    var map = CURRENT.get();
    return map == null ? value : map.getOrDefault(key, value);
  }

  public static float floating(float value, String key) {
    var m = CURRENT.get();
    return m == null || !m.containsKey(key) ? value : (float) Math.min(1e30, m.get(key));
  }

  public static int integer(int value, String key) {
    var m = CURRENT.get();
    return m == null || !m.containsKey(key)
        ? value
        : (int) Math.min(Integer.MAX_VALUE - 1, Math.max(0, Math.round(m.get(key))));
  }

  public static long longValue(long value, String key) {
    var m = CURRENT.get();
    return m == null || !m.containsKey(key)
        ? value
        : (long) Math.min(Long.MAX_VALUE - 1d, Math.max(0, Math.round(m.get(key))));
  }

  private ParameterRuntime() {}
}
