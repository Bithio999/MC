package dev.heavenlyrestriction.laplace;

/** Normalized resource shares; conversion never changes the action's balanced budget. */
public record PaymentPlan(double magicules, double aura, double hp, double shp) {
  public static final PaymentPlan MAGICULES = new PaymentPlan(1, 0, 0, 0),
      AURA = new PaymentPlan(0, 1, 0, 0),
      HP = new PaymentPlan(0, 0, 1, 0),
      SHP = new PaymentPlan(0, 0, 0, 1);

  public PaymentPlan {
    for (double n : new double[] {magicules, aura, hp, shp})
      if (!Double.isFinite(n) || n < 0 || n > 1)
        throw new IllegalArgumentException("Payment shares must be between 0% and 100%.");
    double sum = magicules + aura + hp + shp;
    if (Math.abs(sum - 1) > 1e-6)
      throw new IllegalArgumentException("Payment shares must total 100%.");
    magicules /= sum;
    aura /= sum;
    hp /= sum;
    shp /= sum;
  }

  public boolean converted() {
    return aura > 0 || hp > 0 || shp > 0;
  }

  public record Rates(double magiculesPerAura, double magiculesPerHP, double magiculesPerSHP) {
    public static final Rates DEFAULT = new Rates(1, 10, 10);

    public Rates {
      for (double n : new double[] {magiculesPerAura, magiculesPerHP, magiculesPerSHP})
        if (!Double.isFinite(n) || n < .000001 || n > 1e9)
          throw new IllegalArgumentException("Invalid payment conversion rate.");
    }
  }

  public record Policy(PaymentPlan plan, Rates rates, boolean allowVital, boolean integerBill) {
    public static final Policy DEFAULT = new Policy(MAGICULES, Rates.DEFAULT, true, false);

    public void validate() {
      if (!allowVital && (plan.hp > 0 || plan.shp > 0))
        throw new IllegalArgumentException(
            "Healing and regeneration can be paid with magicules and aura only.");
    }

    public Bill bill(double budget) {
      validate();
      if (!Double.isFinite(budget) || budget < 0 || budget > FormulaSet.LIMIT)
        throw new IllegalArgumentException("Invalid cost budget.");
      return new Bill(
          budget * plan.magicules,
          budget * plan.aura / rates.magiculesPerAura,
          budget * plan.hp / rates.magiculesPerHP,
          budget * plan.shp / rates.magiculesPerSHP);
    }

    public Policy withPlan(PaymentPlan p) {
      return new Policy(p, rates, allowVital, integerBill);
    }
  }

  public record Bill(double magicules, double aura, double hp, double shp) {
    public Bill {
      for (double n : new double[] {magicules, aura, hp, shp})
        if (!Double.isFinite(n) || n < 0 || n > FormulaSet.LIMIT)
          throw new IllegalArgumentException("The resource bill is too large.");
    }

    public boolean empty() {
      return magicules == 0 && aura == 0 && hp == 0 && shp == 0;
    }
  }

  public record Pools(double magicules, double aura, double hp, double shp) {}

  /** A prepared whole transaction. HP and SHP always retain at least one point. */
  public record Debit(Pools after, boolean affordable) {}

  public static Debit prepare(Pools before, Bill bill) {
    double mp = subtract(before.magicules, bill.magicules),
        au = subtract(before.aura, bill.aura),
        sp = subtract(before.shp, bill.shp);
    double health = before.hp;
    if (bill.hp > 0) {
      double desired = health - bill.hp;
      float rounded = (float) desired;
      if (rounded > desired) rounded = Math.nextDown(rounded);
      if (rounded >= health) rounded = Math.nextDown((float) health);
      health = rounded;
    }
    boolean enough =
        Double.isFinite(mp)
            && Double.isFinite(au)
            && Double.isFinite(health)
            && Double.isFinite(sp)
            && mp >= 0
            && au >= 0
            && (bill.hp == 0 || health >= 1)
            && (bill.shp == 0 || sp >= 1);
    return new Debit(new Pools(mp, au, health, sp), enough);
  }

  private static double subtract(double before, double amount) {
    if (amount == 0) return before;
    double after = before - amount;
    return after >= before ? Math.nextDown(before) : after;
  }
}
