package dev.heavenlyrestriction.laplace;

import java.util.*;

/** Shared server/client preview and balance rules for a verified native action. */
public record EquationModel(
    Map<String, Double> inputs,
    Map<String, FormulaSet.Range> bounds,
    LinkedHashMap<String, String> defaults,
    Map<String, String> labels,
    Map<String, String> units,
    List<String> powerOutputs,
    List<String> costOutputs,
    double powerLimit) {
  public record Result(
      Map<String, Double> values,
      Map<String, Double> nativeValues,
      Map<String, Double> minimumCosts,
      double power) {}

  public FormulaSet compile(Map<String, String> equations) {
    if (!equations.keySet().containsAll(defaults.keySet()))
      throw new IllegalArgumentException("Every native aspect needs an equation.");
    if (equations.size() > defaults.size() + 4)
      throw new IllegalArgumentException("Add at most four helper variables per action.");
    var program = new FormulaSet(equations, bounds);
    var original = new FormulaSet(defaults, bounds);
    double total = 0, edited = 0;
    for (String name : powerOutputs) {
      var range = program.range(name);
      if (range.low() < 0)
        throw new IllegalArgumentException(
            labels.get(name) + " can become negative. Use max(0, ...).");
      total += original.range(name).high();
      edited += range.high();
    }
    if (total > 0 && edited > total * powerLimit + 1e-6)
      throw new IllegalArgumentException(
          "Output can exceed "
              + pretty(powerLimit)
              + "× its native value. Use min(..., base_value * "
              + pretty(powerLimit)
              + ").");
    for (String name : costOutputs)
      if (program.range(name).low() < 0)
        throw new IllegalArgumentException("Resource costs cannot be negative. Use max(0, ...).");
    return program;
  }

  public Result evaluate(Map<String, String> equations) {
    return evaluate(equations, Map.of(), Map.of());
  }

  public Result evaluate(
      Map<String, String> equations,
      Map<String, Double> outputOverrides,
      Map<String, Double> nativeOverrides) {
    var program = compile(equations);
    var nativeProgram = new FormulaSet(defaults, bounds);
    var values = new LinkedHashMap<>(program.evaluate(inputs, outputOverrides));
    var originals = nativeProgram.evaluate(inputs, nativeOverrides);
    double before = 0, after = 0;
    for (String output : powerOutputs) {
      before += originals.get(output);
      after += values.get(output);
    }
    double ratio = before > 0 ? after / before : after > 0 ? Double.POSITIVE_INFINITY : 1;
    if (!Double.isFinite(ratio) || ratio > powerLimit + 1e-5)
      throw new IllegalArgumentException(
          "This action's values changed; restore or rebalance its equations.");
    double billedRatio =
        outputOverrides.isEmpty() ? Math.max(ratio, maximumRatio(equations)) : ratio;
    double price = billedRatio > 1 ? 1 + 2 * Math.pow(billedRatio - 1, 2) : .75;
    var minimum = new LinkedHashMap<String, Double>();
    for (String output : costOutputs) {
      double floor = originals.get(output) * price;
      minimum.put(output, floor);
      values.put(output, Math.max(values.get(output), floor));
    }
    return new Result(Collections.unmodifiableMap(values), originals, minimum, ratio);
  }

  public double maximumRatio(Map<String, String> equations) {
    var p = compile(equations);
    var n = new FormulaSet(defaults, bounds);
    double before = 0, after = 0;
    for (String k : powerOutputs) {
      before += n.range(k).high();
      after += p.range(k).high();
    }
    return before > 0 ? after / before : 1;
  }

  public LinkedHashMap<String, String> balanceCost(Map<String, String> equations) {
    var program = compile(equations);
    var original = new FormulaSet(defaults, bounds);
    double total = 0, edited = 0;
    for (String output : powerOutputs) {
      total += original.range(output).high();
      edited += program.range(output).high();
    }
    double ratio = total > 0 ? edited / total : 1;
    double factor = ratio > 1 ? 1 + 2 * Math.pow(ratio - 1, 2) : 1;
    var result = new LinkedHashMap<>(equations);
    for (String cost : costOutputs)
      result.put(
          cost,
          "("
              + defaults.get(cost)
              + ") * "
              + java.math.BigDecimal.valueOf(factor).stripTrailingZeros().toPlainString());
    return result;
  }

  public static String pretty(double n) {
    if (!Double.isFinite(n)) return "—";
    if (Math.abs(n) >= 100000) return String.format(Locale.ROOT, "%.3g", n);
    return java.math.BigDecimal.valueOf(n)
        .setScale(3, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString();
  }
}
