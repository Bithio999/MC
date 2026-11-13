package dev.heavenlyrestriction.laplace.expand;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class ParameterRuntimeTest {
  @Test
  void absentProfilesPreserveNativeValuesExactly() {
    assertEquals(-1, ParameterRuntime.integer(-1, "p"));
    assertEquals(Long.MAX_VALUE, ParameterRuntime.longValue(Long.MAX_VALUE, "p"));
    assertEquals(Float.MAX_VALUE, ParameterRuntime.floating(Float.MAX_VALUE, "p"));
    assertTrue(Double.isNaN(ParameterRuntime.decimal(Double.NaN, "p")));
  }

  @Test
  void nestedCallbacksRestoreAndMaskTheirOwners() {
    try (var outer = new ParameterRuntime.Scope(Map.of("p", 7d))) {
      assertEquals(7, ParameterRuntime.decimal(2, "p"));
      try (var masked = new ParameterRuntime.Scope(null)) {
        assertEquals(2, ParameterRuntime.decimal(2, "p"));
      }
      assertEquals(7, ParameterRuntime.decimal(2, "p"));
      try (var other = new ParameterRuntime.Scope(Map.of("p", 9d))) {
        assertEquals(9, ParameterRuntime.integer(2, "p"));
      }
    }
    assertFalse(ParameterRuntime.active());
    assertEquals(2, ParameterRuntime.decimal(2, "p"));
  }

  @Test
  void exceptionsCannotLeakEditedParameters() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          try (var scope = new ParameterRuntime.Scope(Map.of("p", 9d))) {
            throw new IllegalStateException();
          }
        });
    assertFalse(ParameterRuntime.active());
  }

  @Test
  void stableReadableFieldKeysFitTheEquationLanguage() {
    String a = ParameterKey.key("a.Config.extraordinarilyLongDamageMultiplierMastered"),
        b = ParameterKey.key("b.Config.extraordinarilyLongDamageMultiplierMastered");
    assertEquals(a, ParameterKey.key("a.Config.extraordinarilyLongDamageMultiplierMastered"));
    assertNotEquals(a, b);
    assertTrue(a.startsWith("p_extraordinarily"));
    assertTrue(("base_" + a).length() <= 32);
  }

  @Test
  void progressionTransactionsAndCostFieldsAreProtected() {
    String owner = "io/github/manasmods/tensura/config/ability/skill/UniqueSkillConfig$Creator";
    for (String name :
        List.of(
            "acquiringMagiculeCost",
            "learningPoint",
            "engravingLevel",
            "epGain",
            "inventorySlots",
            "castTime",
            "cooldown",
            "criticalChance")) assertFalse(ParameterKey.allowed(owner, name, "F"), name);
    assertTrue(ParameterKey.allowed(owner, "fireDamageCharged", "F"));
    assertTrue(ParameterKey.allowed(owner, "range", "D"));
  }

  @Test
  void wholeParametersRoundOnlyWhenEdited() {
    try (var scope = new ParameterRuntime.Scope(Map.of("p", 1.6))) {
      assertEquals(2, ParameterRuntime.integer(1, "p"));
      assertEquals(-1, ParameterRuntime.integer(-1, "untouched"));
    }
  }
}
