package dev.heavenlyrestriction.critical;

import java.util.function.BooleanSupplier;

/** A fresh geometric payout for each critical, with at most 100 successful rolls. */
public final class CriticalPayout {
  public static final int MAX_SUCCESSES = 100;
  public static final double BASE = 1.5, PER_SUCCESS = 1.25;

  public static int successes(BooleanSupplier coin) {
    int successes = 0;
    while (successes < MAX_SUCCESSES && coin.getAsBoolean()) successes++;
    return successes;
  }

  public static double multiplier(int successes) {
    return BASE * Math.pow(PER_SUCCESS, Math.clamp(successes, 0, MAX_SUCCESSES));
  }

  public static String label(double multiplier) {
    if (multiplier >= 10000) return String.format(java.util.Locale.ROOT, "%.3g", multiplier);
    return java.math.BigDecimal.valueOf(multiplier)
        .setScale(2, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString();
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
