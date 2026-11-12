package dev.heavenlyrestriction.critical;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CriticalPayoutTest {
  @Test
  void failureKeepsGuaranteedBonusAndStopsImmediately() {
    var calls = new AtomicInteger();
    int successes =
        CriticalPayout.successes(
            () -> {
              calls.incrementAndGet();
              return false;
            });
    assertEquals(0, successes);
    assertEquals(1.5, CriticalPayout.multiplier(successes));
    assertEquals(1, calls.get());
  }

  @Test
  void successfulRollsCompoundAtTheReducedRate() {
    var calls = new AtomicInteger();
    int successes = CriticalPayout.successes(() -> calls.getAndIncrement() < 2);
    assertEquals(2, successes);
    assertEquals(2.34375, CriticalPayout.multiplier(successes));
    assertEquals(3, calls.get());
  }

  @Test
  void perfectRunKeepsOneHundredSuccessesWithoutAPayoutCap() {
    var calls = new AtomicInteger();
    int successes =
        CriticalPayout.successes(
            () -> {
              calls.incrementAndGet();
              return true;
            });
    assertEquals(100, successes);
    assertEquals(100, calls.get());
    assertEquals(1.5 * Math.pow(1.25, 100), CriticalPayout.multiplier(successes));
    assertTrue(CriticalPayout.multiplier(successes) > 100);
    assertEquals(CriticalPayout.multiplier(100), CriticalPayout.multiplier(101));
  }

  @Test
  void maximumRunIsFiniteAndOversizedDamageSaturatesAtTransportLimit() {
    assertTrue(Float.isFinite(CriticalPayout.multiply(1000, CriticalPayout.multiplier(100))));
    assertEquals(
        Float.MAX_VALUE, CriticalPayout.multiply(Float.MAX_VALUE, CriticalPayout.multiplier(100)));
    assertEquals(0, CriticalPayout.multiply(0, 1.5));
    assertEquals(0, CriticalPayout.multiply(Float.NaN, 1.5));
  }

  @Test
  void reportShowsActualDecimalPayoutAndCompactsVeryLargeValues() {
    assertEquals("1", CriticalPayout.label(1));
    assertEquals("1.5", CriticalPayout.label(CriticalPayout.multiplier(0)));
    assertEquals("1.88", CriticalPayout.label(CriticalPayout.multiplier(1)));
    assertEquals("2.34", CriticalPayout.label(CriticalPayout.multiplier(2)));
    assertEquals("1.00e+04", CriticalPayout.label(10000));
  }

  @Test
  void invalidMitigationNeverTurnsIntoDamage() {
    assertEquals(0, CriticalPayout.finite(Float.NaN));
    assertEquals(0, CriticalPayout.finite(Float.NEGATIVE_INFINITY));
    assertEquals(Float.MAX_VALUE, CriticalPayout.finite(Float.POSITIVE_INFINITY));
    assertEquals(12, CriticalPayout.finite(12));
  }
}
