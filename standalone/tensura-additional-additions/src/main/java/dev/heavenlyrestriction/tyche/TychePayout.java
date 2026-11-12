package dev.heavenlyrestriction.tyche;

import dev.heavenlyrestriction.critical.CriticalPayout;
import java.util.function.BooleanSupplier;

/** A stronger geometric payout, up to 100 successes, and one failed-roll reroll per chain. */
public final class TychePayout {
  public static final double BASE = 2, PER_SUCCESS = 1.35;

  public static int chain(BooleanSupplier coin) {
    int successes = 0;
    boolean rerolled = false;
    while (successes < CriticalPayout.MAX_SUCCESSES) {
      if (coin.getAsBoolean()) {
        successes++;
        continue;
      }
      if (rerolled) break;
      rerolled = true;
      if (!coin.getAsBoolean()) break;
      successes++;
    }
    return successes;
  }

  public static double multiplier(int successes) {
    return BASE * Math.pow(PER_SUCCESS, Math.clamp(successes, 0, CriticalPayout.MAX_SUCCESSES));
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
