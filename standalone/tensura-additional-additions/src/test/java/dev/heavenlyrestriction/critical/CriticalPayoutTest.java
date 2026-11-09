package dev.heavenlyrestriction.critical;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CriticalPayoutTest {
  @Test
  void firstDoublingIsGuaranteedAndFailureStopsImmediately() {
    var calls = new AtomicInteger();
    assertEquals(
        1,
        CriticalPayout.doublings(
            () -> {
              calls.incrementAndGet();
              return false;
            }));
    assertEquals(1, calls.get());
  }

  @Test
  void eachSuccessDoublesAgainUntilFirstFailure() {
    var calls = new AtomicInteger();
    assertEquals(3, CriticalPayout.doublings(() -> calls.getAndIncrement() < 2));
    assertEquals(3, calls.get());
  }

  @Test
  void aPerfectRunStopsAtOneHundredAdditionalRolls() {
    var calls = new AtomicInteger();
    assertEquals(
        101,
        CriticalPayout.doublings(
            () -> {
              calls.incrementAndGet();
              return true;
            }));
    assertEquals(100, calls.get());
    assertEquals(Math.scalb(1d, 101), Math.pow(2, 101));
  }

  @Test
  void maximumRunIsFiniteAndOversizedDamageSaturatesAtTransportLimit() {
    assertTrue(Float.isFinite(CriticalPayout.multiply(1000, Math.scalb(1d, 101))));
    assertEquals(Float.MAX_VALUE, CriticalPayout.multiply(Float.MAX_VALUE, Math.scalb(1d, 101)));
    assertEquals(0, CriticalPayout.multiply(0, 2));
    assertEquals(0, CriticalPayout.multiply(Float.NaN, 2));
  }

  @Test
  void invalidMitigationNeverTurnsIntoDamage() {
    assertEquals(0, CriticalPayout.finite(Float.NaN));
    assertEquals(0, CriticalPayout.finite(Float.NEGATIVE_INFINITY));
    assertEquals(Float.MAX_VALUE, CriticalPayout.finite(Float.POSITIVE_INFINITY));
    assertEquals(12, CriticalPayout.finite(12));
  }
}
