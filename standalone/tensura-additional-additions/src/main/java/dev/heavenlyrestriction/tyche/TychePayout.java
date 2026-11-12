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

  public static double adversity(double health, double maximum) {
    return maximum > 0 ? Math.clamp(1 - health / (maximum * .5), 0, 1) : 0;
  }

  private TychePayout() {}
}
