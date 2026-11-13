package dev.heavenlyrestriction.laplace;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class FormulaSetTest {
  private EquationModel model() {
    return new EquationModel(
        Map.of("base_healing", 4d, "base_cost", 20d, "missing_hp_fraction", .5),
        Map.of(
            "base_healing",
            FormulaSet.Range.fixed(4),
            "base_cost",
            FormulaSet.Range.fixed(20),
            "missing_hp_fraction",
            new FormulaSet.Range(0, 1)),
        new LinkedHashMap<>(Map.of("healing", "base_healing", "magicule_cost", "base_cost")),
        Map.of("healing", "Healing", "magicule_cost", "Cost"),
        Map.of(),
        List.of("healing"),
        List.of("magicule_cost"),
        3);
  }

  @Test
  void realDependenciesRecomputeInAnyOrder() {
    var p =
        new FormulaSet(
            Map.of(
                "magicule_cost",
                "3 * healing",
                "healing",
                "base * multiplier",
                "multiplier",
                "1 + missing"),
            Map.of("base", FormulaSet.Range.fixed(4), "missing", new FormulaSet.Range(0, 1)));
    var values = p.evaluate(Map.of("base", 4d, "missing", .5));
    assertEquals(6, values.get("healing"));
    assertEquals(18, values.get("magicule_cost"));
    assertEquals(8, p.range("healing").high());
    assertEquals(
        9,
        p.evaluate(Map.of("base", 4d, "missing", .5), Map.of("healing", 3d)).get("magicule_cost"));
  }

  @Test
  void arithmeticAndPowerPrecedence() {
    assertEquals(
        10, new FormulaSet(Map.of("a", "2 + 2 * 2 ^ 2"), Map.of()).evaluate(Map.of()).get("a"));
    assertEquals(-4, new FormulaSet(Map.of("a", "-2 ^ 2"), Map.of()).evaluate(Map.of()).get("a"));
  }

  @Test
  void helpersAreBoundedAndFunctionRangesCorrect() {
    var p =
        new FormulaSet(
            Map.of("a", "clamp(abs(x), 2, 4)", "b", "max(0, min(x, 3))"),
            Map.of("x", new FormulaSet.Range(-10, 10)));
    assertEquals(new FormulaSet.Range(2, 4), p.range("a"));
    assertEquals(new FormulaSet.Range(0, 3), p.range("b"));
  }

  @Test
  void cyclesAndUnknownsFailEarly() {
    for (var eq :
        List.of(Map.of("a", "a + 1"), Map.of("a", "b", "b", "a"), Map.of("a", "missing + 1")))
      assertThrows(IllegalArgumentException.class, () -> new FormulaSet(eq, Map.of()));
  }

  @Test
  void divisorsCannotBecomeZero() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FormulaSet(Map.of("a", "1 / x"), Map.of("x", new FormulaSet.Range(0, 1))));
    assertDoesNotThrow(
        () ->
            new FormulaSet(
                Map.of("a", "1 / max(x, .01)"), Map.of("x", new FormulaSet.Range(0, 1))));
  }

  @Test
  void invalidPowersAndNonfiniteAreRejected() {
    for (String s :
        List.of(
            "(-2) ^ .5", "4 ^ 4", "4 ^ -1", "0 / 0", "clamp(1, 3, 2)", "NaN", "1.0.2", "sqrt(4)"))
      assertThrows(
          IllegalArgumentException.class, () -> new FormulaSet(Map.of("a", s), Map.of()), s);
    assertThrows(IllegalArgumentException.class, () -> new FormulaSet.Range(Double.NaN, 1));
  }

  @Test
  void nestingSizeAndInputWritesAreRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FormulaSet(Map.of("a", "(".repeat(20) + "1" + ")".repeat(20)), Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FormulaSet(Map.of("a", "1".repeat(161)), Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FormulaSet(Map.of("x", "1"), Map.of("x", FormulaSet.Range.fixed(2))));
  }

  @Test
  void outputCostsHaveVisibleConvexMinimum() {
    var m = model();
    var eq = new LinkedHashMap<>(m.defaults());
    eq.put("healing", "base_healing * 2");
    eq.put("magicule_cost", "0");
    var r = m.evaluate(eq);
    assertEquals(8, r.values().get("healing"));
    assertEquals(60, r.values().get("magicule_cost"));
    eq.put("healing", "base_healing * 3");
    assertEquals(180, m.evaluate(eq).values().get("magicule_cost"));
    eq.put("healing", "base_healing * 4");
    assertThrows(IllegalArgumentException.class, () -> m.evaluate(eq));
  }

  @Test
  void stateDependentFormulaCannotBeUnderpricedAtFullHealth() {
    var m = model();
    var eq = new LinkedHashMap<>(m.defaults());
    eq.put("healing", "base_healing * (1 + missing_hp_fraction)");
    eq.put("magicule_cost", "0");
    assertEquals(6, m.evaluate(eq).values().get("healing"));
    assertEquals(60, m.evaluate(eq).values().get("magicule_cost"));
  }

  @Test
  void discountsAndBalanceButtonDoNotMakeFreeActions() {
    var m = model();
    var eq = new LinkedHashMap<>(m.defaults());
    eq.put("magicule_cost", "0");
    assertEquals(15, m.evaluate(eq).values().get("magicule_cost"));
    eq.put("healing", "base_healing * 2");
    var balanced = m.balanceCost(eq);
    assertEquals(
        60, new FormulaSet(balanced, m.bounds()).evaluate(m.inputs()).get("magicule_cost"));
  }

  @Test
  void actualHealingClampFlowsIntoPrice() {
    var m = model();
    var defaults = new LinkedHashMap<>(m.defaults());
    defaults.put("magicule_cost", "5 * healing");
    m =
        new EquationModel(
            m.inputs(),
            m.bounds(),
            defaults,
            m.labels(),
            m.units(),
            m.powerOutputs(),
            m.costOutputs(),
            3);
    var eq = new LinkedHashMap<>(defaults);
    eq.put("healing", "base_healing * 2");
    var r = m.evaluate(eq, Map.of("healing", 2d), Map.of("healing", 2d));
    assertEquals(10, r.values().get("magicule_cost"));
  }

  @Test
  void negativeOutputAndExtraHelpersCannotBypassValidation() {
    var m = model();
    var eq = new LinkedHashMap<>(m.defaults());
    eq.put("healing", "-1");
    assertThrows(IllegalArgumentException.class, () -> m.compile(eq));
    eq.put("healing", "base_healing");
    for (int n = 0; n < 5; n++) eq.put("helper_" + n, "1");
    assertThrows(IllegalArgumentException.class, () -> m.compile(eq));
  }
}
