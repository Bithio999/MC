package dev.heavenlyrestriction.laplace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PaymentPlanTest {
  @Test
  void convertsOneBudgetIntoFourPools() {
    var policy =
        new PaymentPlan.Policy(
            new PaymentPlan(.25, .25, .25, .25), new PaymentPlan.Rates(2, 10, 20), true, false);
    assertEquals(new PaymentPlan.Bill(100, 50, 10, 5), policy.bill(400));
  }

  @Test
  void sharesCannotHideFreeOrInvalidPayments() {
    for (double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1, 1.01})
      assertThrows(IllegalArgumentException.class, () -> new PaymentPlan(value, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new PaymentPlan(.2, .2, .2, .2));
    assertThrows(IllegalArgumentException.class, () -> new PaymentPlan.Rates(0, 10, 10));
  }

  @Test
  void recoveryCannotFundItsOwnHealing() {
    var policy = new PaymentPlan.Policy(PaymentPlan.HP, PaymentPlan.Rates.DEFAULT, false, false);
    assertThrows(IllegalArgumentException.class, () -> policy.bill(100));
    assertEquals(100, policy.withPlan(PaymentPlan.AURA).bill(100).aura());
  }

  @Test
  void mixedPaymentsArePreparedAsOneTransaction() {
    var before = new PaymentPlan.Pools(100, 100, 20, 20);
    var bill = new PaymentPlan.Bill(20, 101, 1, 1);
    assertFalse(PaymentPlan.prepare(before, bill).affordable());
    assertEquals(new PaymentPlan.Pools(100, 100, 20, 20), before);
    var good = PaymentPlan.prepare(before, new PaymentPlan.Bill(20, 30, 4, 5));
    assertTrue(good.affordable());
    assertEquals(new PaymentPlan.Pools(80, 70, 16, 15), good.after());
  }

  @Test
  void vitalPaymentsRetainOnePoint() {
    var before = new PaymentPlan.Pools(100, 100, 20, 20);
    assertTrue(PaymentPlan.prepare(before, new PaymentPlan.Bill(0, 0, 19, 19)).affordable());
    assertFalse(PaymentPlan.prepare(before, new PaymentPlan.Bill(0, 0, 19.01, 19)).affordable());
    assertFalse(PaymentPlan.prepare(before, new PaymentPlan.Bill(0, 0, 19, 19.01)).affordable());
    assertTrue(
        PaymentPlan.prepare(new PaymentPlan.Pools(0, 0, .1, 0), new PaymentPlan.Bill(0, 0, 0, 0))
            .affordable());
  }

  @Test
  void smallFeesCannotRoundToFreeCasts() {
    var before = new PaymentPlan.Pools(1e50, 1e50, 100, 1e50);
    var d = PaymentPlan.prepare(before, new PaymentPlan.Bill(1, 1, .000000000001, 1));
    assertTrue(d.affordable());
    assertTrue(d.after().magicules() < before.magicules());
    assertTrue(d.after().aura() < before.aura());
    assertTrue(d.after().hp() < before.hp());
    assertTrue(d.after().shp() < before.shp());
  }

  @Test
  void nativeModifiersAreNotRoundedAway() {
    var policy = new PaymentPlan.Policy(PaymentPlan.AURA, PaymentPlan.Rates.DEFAULT, false, true);
    assertEquals(1.5, policy.bill(1.5).aura());
  }
}
