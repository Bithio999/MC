package dev.heavenlyrestriction.tyche;

import java.util.function.BooleanSupplier;

/** Two guaranteed doublings, up to 100 successes, and one failed-roll reroll per chain. */
public final class TychePayout {
  public static int chain(BooleanSupplier coin) {
    int exponent = 2;
    boolean rerolled = false;
    for (int roll = 0; roll < 100; roll++) {
      if (coin.getAsBoolean()) {
        exponent++;
        continue;
      }
      if (rerolled) break;
      rerolled = true;
      if (!coin.getAsBoolean()) break;
      exponent++;
    }
    return exponent;
  }

  public static int roll(BooleanSupplier coin, boolean dividend) {
    int first = chain(coin);
    return dividend ? Math.max(first, chain(coin)) : first;
  }

  public static boolean atHalf(double value, double maximum) {
    return Double.isFinite(value)
        && Double.isFinite(maximum)
        && maximum > 0
        && value <= maximum * .5;
  }

  private TychePayout() {}
}
