package dev.heavenlyrestriction.tyche;

import static org.junit.jupiter.api.Assertions.*;

import dev.heavenlyrestriction.critical.CriticalPayout;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TychePayoutTest {
  @Test
  void failureGetsOneRerollButCannotLoop() {
    var n = new AtomicInteger();
    int successes =
        TychePayout.chain(
            () -> {
              n.incrementAndGet();
              return false;
            });
    assertEquals(0, successes);
    assertEquals(2, TychePayout.multiplier(successes));
    assertEquals(2, n.get());
  }

  @Test
  void rescuedFailureContinuesAtReducedRateAndNextFailureStops() {
    boolean[] rolls = {false, true, true, false};
    var n = new AtomicInteger();
    int successes = TychePayout.chain(() -> rolls[n.getAndIncrement()]);
    assertEquals(2, successes);
    assertEquals(3.645, TychePayout.multiplier(successes), 1e-12);
    assertEquals(4, n.get());
  }

  @Test
  void perfectChainKeepsOneHundredSuccessesWithoutAPayoutCap() {
    var n = new AtomicInteger();
    int successes =
        TychePayout.chain(
            () -> {
              n.incrementAndGet();
              return true;
            });
    assertEquals(100, successes);
    assertEquals(100, n.get());
    assertEquals(2 * Math.pow(1.35, 100), TychePayout.multiplier(successes));
    assertTrue(TychePayout.multiplier(successes) > 100);
    assertEquals(TychePayout.multiplier(100), TychePayout.multiplier(101));
    assertTrue(Float.isFinite(CriticalPayout.multiply(1000, TychePayout.multiplier(100))));
    assertEquals(
        Float.MAX_VALUE, CriticalPayout.multiply(Float.MAX_VALUE, TychePayout.multiplier(100)));
  }

  @Test
  void savedFailureDoesNotConsumeOneOfTheHundredSuccesses() {
    var n = new AtomicInteger();
    assertEquals(100, TychePayout.chain(() -> n.incrementAndGet() != 1));
    assertEquals(101, n.get());
  }

  @Test
  void dividendSelectsLargerChainInsteadOfCombiningThem() {
    boolean[] rolls = {true, false, false, true, true, false, false};
    var n = new AtomicInteger();
    assertEquals(2, TychePayout.roll(() -> rolls[n.getAndIncrement()], true));
    assertEquals(7, n.get());
  }

  @Test
  void ordinaryPayoutDoesNotConsumeASecondChain() {
    var n = new AtomicInteger();
    assertEquals(
        0,
        TychePayout.roll(
            () -> {
              n.incrementAndGet();
              return false;
            },
            false));
    assertEquals(2, n.get());
  }

  @Test
  void halfThresholdIncludesExactHalfAndZeroButNotAbsentOrInvalidCapacity() {
    assertFalse(TychePayout.atHalf(20, 20));
    assertFalse(TychePayout.atHalf(10.01, 20));
    assertTrue(TychePayout.atHalf(10, 20));
    assertTrue(TychePayout.atHalf(0, 20));
    assertFalse(TychePayout.atHalf(0, 0));
    assertFalse(TychePayout.atHalf(Double.NaN, 20));
    assertFalse(TychePayout.atHalf(1, Double.POSITIVE_INFINITY));
  }
}
