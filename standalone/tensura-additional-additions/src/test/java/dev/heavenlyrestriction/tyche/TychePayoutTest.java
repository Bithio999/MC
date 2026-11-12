package dev.heavenlyrestriction.tyche;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TychePayoutTest {
  @Test
  void failureGetsOneRerollButCannotLoop() {
    var n = new AtomicInteger();
    assertEquals(
        2,
        TychePayout.chain(
            () -> {
              n.incrementAndGet();
              return false;
            }));
    assertEquals(2, n.get());
  }

  @Test
  void aSavedFailureCanContinueButTheNextFailureStops() {
    boolean[] rolls = {false, true, true, false};
    var n = new AtomicInteger();
    assertEquals(4, TychePayout.chain(() -> rolls[n.getAndIncrement()]));
    assertEquals(4, n.get());
  }

  @Test
  void perfectChainHasOnlyOneHundredAdditionalDoublings() {
    var n = new AtomicInteger();
    assertEquals(
        102,
        TychePayout.chain(
            () -> {
              n.incrementAndGet();
              return true;
            }));
    assertEquals(100, n.get());
  }

  @Test
  void dividendSelectsTheLargerChainInsteadOfCombiningThem() {
    boolean[] rolls = {false, false, true, true, false, false};
    var n = new AtomicInteger();
    assertEquals(4, TychePayout.roll(() -> rolls[n.getAndIncrement()], true));
  }

  @Test
  void adversityIsBoundedAndFallsAsHealthRecovers() {
    assertEquals(0, TychePayout.adversity(20, 20));
    assertEquals(0, TychePayout.adversity(10, 20));
    assertEquals(.5, TychePayout.adversity(5, 20));
    assertEquals(1, TychePayout.adversity(-1, 20));
    assertEquals(0, TychePayout.adversity(0, 0));
  }
}
