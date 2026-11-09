package dev.heavenlyrestriction.critical;

import java.util.function.BooleanSupplier;

/** A fresh geometric payout for each critical: one guaranteed doubling and at most 100 rolls. */
public final class CriticalPayout {
  public static final int MAX_ROLLS = 100;

  public static int doublings(BooleanSupplier coin) {
    int exponent = 1;
    while (exponent <= MAX_ROLLS && coin.getAsBoolean()) exponent++;
    return exponent;
  }

  public static float multiply(float damage, double multiplier) {
    if (!(damage > 0) || !(multiplier > 0)) return 0;
    return (float) Math.min(Float.MAX_VALUE, (double) damage * multiplier);
  }

  public static float finite(float damage) {
    if (Float.isNaN(damage) || damage <= 0) return 0;
    return Math.min(Float.MAX_VALUE, damage);
  }

  private CriticalPayout() {}
}
