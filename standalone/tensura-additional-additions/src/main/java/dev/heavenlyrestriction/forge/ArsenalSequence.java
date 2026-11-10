package dev.heavenlyrestriction.forge;

import java.util.Arrays;

/** One 24-tick round shared by all weapons. No per-blade cooldown drift or catch-up volleys. */
public final class ArsenalSequence {
  public static final int CYCLE = 24;
  private int[] roster = {};
  private int next;
  private long due = Long.MIN_VALUE;
  private int step;

  public boolean launch(int blade, long tick, int[] live) {
    if (!Arrays.equals(roster, live)) {
      roster = live.clone();
      next = step = 0;
      due = tick + 4;
    }
    if (roster.length == 0 || roster[next] != blade || tick < due) return false;
    int interval = (step + 1) * CYCLE / roster.length - step * CYCLE / roster.length;
    step = (step + 1) % roster.length;
    next = (next + 1) % roster.length;
    due = tick + interval;
    return true;
  }
}
