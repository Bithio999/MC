package dev.heavenlyrestriction.laplace.expand;

import static org.junit.jupiter.api.Assertions.*;

import dev.heavenlyrestriction.laplace.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ParameterModelTest {
  private EquationModel model() {
    var in =
        Map.of(
            "base_damage", 1d, "base_duration", 1000d, "base_cost", 50d, "__parameter_board", 1d);
    var bounds = new HashMap<String, FormulaSet.Range>();
    in.forEach((k, v) -> bounds.put(k, FormulaSet.Range.fixed(v)));
    var eq = new LinkedHashMap<String, String>();
    eq.put("damage", "base_damage");
    eq.put("duration", "base_duration");
    eq.put("magicule_cost", "base_cost");
    return new EquationModel(
        in,
        bounds,
        eq,
        Map.of("damage", "Damage", "duration", "Duration", "magicule_cost", "Budget"),
        Map.of(),
        List.of("damage", "duration"),
        List.of("magicule_cost"),
        3);
  }

  @Test
  void differentUnitsCannotHideAnExcessiveDamageIncrease() {
    var m = model();
    var eq = new LinkedHashMap<>(m.defaults());
    eq.put("damage", "base_damage * 4");
    assertThrows(IllegalArgumentException.class, () -> m.evaluate(eq));
  }

  @Test
  void combinedParameterIncreasesShareOnePowerBudget() {
    var m = model();
    var eq = new LinkedHashMap<>(m.defaults());
    eq.put("damage", "base_damage * 2");
    eq.put("duration", "base_duration * 2");
    assertThrows(IllegalArgumentException.class, () -> m.evaluate(eq));
    eq.put("damage", "base_damage * 1.5");
    var r = m.evaluate(eq);
    assertEquals(3, r.power());
    assertEquals(450, r.values().get("magicule_cost"));
  }

  @Test
  void decreasingOneParameterDoesNotSubsidizeAnother() {
    var m = model();
    var eq = new LinkedHashMap<>(m.defaults());
    eq.put("damage", "base_damage * 3");
    eq.put("duration", "base_duration / 100");
    assertEquals(3, m.evaluate(eq).power());
  }

  @Test
  void boundedExpandedEquationCountRejectsOversizedGraphs() {
    var eq = new HashMap<String, String>();
    for (int n = 0; n <= FormulaSet.MAX_EQUATIONS; n++) eq.put("p" + n, "1");
    assertThrows(IllegalArgumentException.class, () -> new FormulaSet(eq, Map.of()));
  }

  @Test
  void integerParametersCannotRoundToAnUnsafeZero() {
    var m = model();
    m =
        new EquationModel(
            m.inputs(),
            m.bounds(),
            m.defaults(),
            m.labels(),
            Map.of("damage", "whole units"),
            m.powerOutputs(),
            m.costOutputs(),
            m.powerLimit());
    var eq = new LinkedHashMap<>(m.defaults());
    eq.put("damage", "base_damage * 0.1");
    final var bounded = m;
    assertThrows(IllegalArgumentException.class, () -> bounded.evaluate(eq));
  }

  @Test
  void nativeMinimumAndMaximumCannotBeInverted() {
    var m = model();
    m =
        new EquationModel(
            m.inputs(),
            m.bounds(),
            m.defaults(),
            Map.of(
                "damage", "Minimum count", "duration", "Maximum count", "magicule_cost", "Budget"),
            m.units(),
            m.powerOutputs(),
            m.costOutputs(),
            m.powerLimit());
    var eq = new LinkedHashMap<>(m.defaults());
    eq.put("duration", "1");
    eq.put("damage", "3");
    final var bounded = m;
    assertThrows(IllegalArgumentException.class, () -> bounded.evaluate(eq));
  }
}
