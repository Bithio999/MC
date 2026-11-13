package dev.heavenlyrestriction.laplace;

import static org.junit.jupiter.api.Assertions.*;

import dev.heavenlyrestriction.laplace.EquationBoard.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class EquationBoardTest {
  private Address at(String key, int... indices) {
    return new Address(key, Arrays.stream(indices).boxed().toList());
  }

  private double evaluate(String expression) {
    return new FormulaSet(Map.of("result", expression), Map.of()).evaluate(Map.of()).get("result");
  }

  @Test
  void operatorDropRetainsTheOriginalAndCreatesAnExplicitEmptySocket() {
    var b = new EquationBoard(Map.of("damage", "12"));
    b.drop(at("damage"), Expr.operation("*"));
    assertEquals(1, b.holes());
    assertEquals("12", b.at(at("damage", 0)).formula());
    assertThrows(IllegalArgumentException.class, b::formulas);
    b.drop(at("damage", 1), Expr.number("2"));
    assertEquals(24, evaluate(b.formulas().get("damage")));
  }

  @Test
  void nestedFunctionsAndOperatorPrecedenceRoundTripWithoutChangingMeaning() {
    for (String expression :
        List.of(
            "2+3*4",
            "(2+3)*4",
            "2-(3-1)",
            "24/(3/2)",
            "(2^1)^2",
            "2^1^2",
            "-2^2",
            "max(4,min(5,8))",
            "clamp(abs(-3),1,5)",
            "1+(2+3)",
            "(2*3)^2")) {
      String code = EquationBoard.parse(expression).formula();
      assertEquals(evaluate(expression), evaluate(code), expression + " -> " + code);
    }
  }

  @Test
  void existingFormulaTextIsPreservedUntilThatEquationIsEdited() {
    var original = new LinkedHashMap<>(Map.of("damage", " 2 + 3 * 4 ", "cost", "max(2, damage)"));
    var b = new EquationBoard(original);
    assertEquals(original, b.formulas());
    b.drop(at("damage", 0), Expr.number("4"));
    assertEquals("max(2, damage)", b.formulas().get("cost"));
    b.undo();
    assertEquals(original, b.formulas());
  }

  @Test
  void dragBetweenOccupiedSocketsSwapsWithoutLosingEitherSubtree() {
    var b = new EquationBoard(Map.of("damage", "8/2"));
    b.transfer(at("damage", 0), at("damage", 1), false);
    assertEquals(.25, evaluate(b.formulas().get("damage")));
    b.undo();
    assertEquals(4, evaluate(b.formulas().get("damage")));
    b.redo();
    assertEquals(.25, evaluate(b.formulas().get("damage")));
  }

  @Test
  void shiftDragCopiesAndMovingIntoAHoleLeavesAnExplicitHole() {
    var b = new EquationBoard(Map.of("damage", "8+2", "cost", "1"));
    b.transfer(at("damage", 0), at("cost"), true);
    assertEquals("8", b.formulas().get("cost"));
    assertEquals(10, evaluate(b.formulas().get("damage")));
    b.remove(at("cost"));
    b.transfer(at("damage", 1), at("cost"), false);
    assertEquals("2", b.root("cost").formula());
    assertEquals(1, b.holes());
  }

  @Test
  void aSubtreeCannotBeDraggedIntoItself() {
    var b = new EquationBoard(Map.of("damage", "2*(3+4)"));
    assertThrows(
        IllegalArgumentException.class, () -> b.transfer(at("damage"), at("damage", 1, 0), false));
    assertThrows(
        IllegalArgumentException.class, () -> b.transfer(at("damage", 1, 0), at("damage"), true));
    assertFalse(b.canUndo());
    assertEquals(14, evaluate(b.formulas().get("damage")));
  }

  @Test
  void namedOutputsStayAsReferencesAndFeedDependentEquations() {
    var b = new EquationBoard(new LinkedHashMap<>(Map.of("damage", "4", "cost", "damage*5")));
    b.addHelper("helper_1");
    b.text("helper_1", "2");
    b.drop(at("damage"), Expr.operation("*"));
    b.drop(at("damage", 1), Expr.variable("helper_1"));
    var p = new FormulaSet(b.formulas(), Map.of());
    assertEquals(8, p.evaluate(Map.of()).get("damage"));
    assertEquals(40, p.evaluate(Map.of()).get("cost"));
    b.deleteHelper("helper_1", Set.of("damage", "cost"));
    assertThrows(IllegalArgumentException.class, () -> new FormulaSet(b.formulas(), Map.of()));
    b.undo();
    assertEquals(40, new FormulaSet(b.formulas(), Map.of()).evaluate(Map.of()).get("cost"));
    assertThrows(IllegalArgumentException.class, () -> b.deleteHelper("damage", Set.of("damage")));
  }

  @Test
  void unwrapDeleteResetAndUndoAreReversible() {
    var b = new EquationBoard(Map.of("damage", "4*2"));
    b.unwrap(at("damage"));
    assertEquals("4", b.formulas().get("damage"));
    b.remove(at("damage"));
    assertEquals(1, b.holes());
    b.reset(Map.of("damage", "6"));
    assertEquals("6", b.formulas().get("damage"));
    b.undo();
    assertEquals(1, b.holes());
    b.undo();
    assertEquals("4", b.formulas().get("damage"));
  }

  @Test
  void rejectedEditsDoNotChangeTheBoardOrHistory() {
    var b = new EquationBoard(Map.of("damage", "4"));
    assertThrows(
        IllegalArgumentException.class,
        () -> b.text("damage", "(".repeat(20) + "1" + ")".repeat(20)));
    assertThrows(IllegalArgumentException.class, () -> b.text("damage", "1".repeat(161)));
    assertEquals("4", b.formulas().get("damage"));
    assertFalse(b.canUndo());
  }

  @Test
  void badNumbersFunctionsAndVariablesProduceUsefulErrors() {
    for (String text : List.of("NaN", "Infinity", ".", "1.2.3", "2+4", "4/0", "base_cost"))
      assertThrows(IllegalArgumentException.class, () -> Expr.number(text), text);
    for (String text : List.of("min", "min(1)", "clamp(1,2)", "sqrt(4)", "2 garbage"))
      assertThrows(IllegalArgumentException.class, () -> EquationBoard.parse(text), text);
    assertEquals("-2", Expr.number("-2").formula());
  }

  @Test
  void snapshotsAndAddressesAreImmutableAndRedoClearsAfterANewEdit() {
    var path = new ArrayList<>(List.of(0));
    var address = new Address("damage", path);
    path.add(1);
    assertEquals(List.of(0), address.path());
    var b = new EquationBoard(Map.of("damage", "4+2"));
    b.drop(address, Expr.number("8"));
    b.undo();
    assertTrue(b.canRedo());
    b.drop(address, Expr.number("9"));
    assertFalse(b.canRedo());
    assertEquals(11, evaluate(b.formulas().get("damage")));
  }

  @Test
  void helperAndHistoryBoundsPreventAnUnboundedBoard() {
    var b = new EquationBoard(Map.of("a", "1"));
    for (int n = 0; n < 11; n++) b.addHelper("h" + n);
    assertThrows(IllegalArgumentException.class, () -> b.addHelper("overflow"));
    for (int n = 0; n < 50; n++) b.text("a", Integer.toString(n));
    int undos = 0;
    while (b.canUndo()) {
      b.undo();
      undos++;
    }
    assertEquals(40, undos);
  }
}
