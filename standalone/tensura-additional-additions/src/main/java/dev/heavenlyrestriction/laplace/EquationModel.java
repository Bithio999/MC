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
    double powerLimit,
    PaymentPlan.Policy payment) {
  public EquationModel(
      Map<String, Double> inputs,
      Map<String, FormulaSet.Range> bounds,
      LinkedHashMap<String, String> defaults,
      Map<String, String> labels,
      Map<String, String> units,
      List<String> powerOutputs,
      List<String> costOutputs,
      double powerLimit) {
    this(
        inputs,
        bounds,
        defaults,
        labels,
        units,
        powerOutputs,
        costOutputs,
        powerLimit,
        PaymentPlan.Policy.DEFAULT);
  }

  public EquationModel withPayment(PaymentPlan plan) {
    return new EquationModel(
        inputs,
        bounds,
        defaults,
        labels,
        units,
        powerOutputs,
        costOutputs,
        powerLimit,
        payment.withPlan(plan));
  }

  public PaymentPlan.Bill bill(Result result) {
    return payment.bill(result.values().get("magicule_cost"));
  }

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
    if (parameterBoard()) {
      for (String name : powerOutputs) {
        var range = program.range(name);
        if (units.getOrDefault(name, "").equals("whole units") && rounded(name, range.low()) < 1)
          throw new IllegalArgumentException(
              labels.get(name) + " must retain at least one whole unit.");
      }
      for (String minimum : powerOutputs) {
        String label = labels.getOrDefault(minimum, "");
        String stem = label.replaceFirst("(?i)^(minimum|min) ", "");
        if (stem.equals(label)) continue;
        for (String maximum : powerOutputs) {
          String other = labels.getOrDefault(maximum, "");
          if (!other.matches("(?i)^(maximum|max) " + java.util.regex.Pattern.quote(stem))) continue;
          if (original.range(minimum).high() <= original.range(maximum).low()
              && program.range(minimum).high() > program.range(maximum).low())
            throw new IllegalArgumentException(label + " must not exceed " + other + ".");
        }
      }
    }
    if ((parameterBoard()
        ? parameterMaximum(program, original) > powerLimit + 1e-6
        : total > 0 && edited > total * powerLimit + 1e-6))
      throw new IllegalArgumentException(
          "Output can exceed "
              + pretty(powerLimit)
              + "× its native value. Use min(..., base_value * "
              + pretty(powerLimit)
              + ").");
    for (String name : costOutputs)
      if (program.range(name).low() < 0)
        throw new IllegalArgumentException("Resource costs cannot be negative. Use max(0, ...).");
    if (parameterBoard()
        && inputs.getOrDefault("base_cost", 0d) == 0
        && program.range("magicule_cost").high() != 0)
      throw new IllegalArgumentException(
          "This native action is free; its cost budget must stay zero.");
    return program;
  }

  public boolean parameterBoard() {
    return inputs.containsKey("__parameter_board");
  }

  private double rounded(String output, double value) {
    return parameterBoard() && units.getOrDefault(output, "").equals("whole units")
        ? Math.max(0, Math.min(Integer.MAX_VALUE - 1d, Math.round(value)))
        : value;
  }

  private double parameterMaximum(FormulaSet program, FormulaSet original) {
    double product = 1;
    for (String name : powerOutputs) {
      double before = original.range(name).high();
      if (before > 0) product *= Math.max(1, rounded(name, program.range(name).high()) / before);
    }
    return product;
  }

  public Result evaluate(Map<String, String> equations) {
    return evaluate(equations, Map.of(), Map.of());
  }

  public Result evaluate(
      Map<String, String> equations,
      Map<String, Double> outputOverrides,
      Map<String, Double> nativeOverrides) {
    payment.validate();
    var program = compile(equations);
    var nativeProgram = new FormulaSet(defaults, bounds);
    var values = new LinkedHashMap<>(program.evaluate(inputs, outputOverrides));
    var originals = nativeProgram.evaluate(inputs, nativeOverrides);
    if (parameterBoard())
      for (String name : powerOutputs) values.computeIfPresent(name, (k, v) -> rounded(k, v));
    double before = 0, after = 0;
    for (String output : powerOutputs) {
      before += originals.get(output);
      after += values.get(output);
    }
    double ratio = before > 0 ? after / before : after > 0 ? Double.POSITIVE_INFINITY : 1;
    if (parameterBoard()) {
      ratio = 1;
      for (String name : powerOutputs)
        if (originals.get(name) > 0) ratio *= Math.max(1, values.get(name) / originals.get(name));
    }
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
      double charge = Math.max(values.get(output), floor);
      if (payment.integerBill() && payment.plan().converted()) charge = Math.ceil(charge);
      payment.bill(
          charge); // Validate converted amounts before the preview or runtime can use them.
      values.put(output, charge);
    }
    return new Result(Collections.unmodifiableMap(values), originals, minimum, ratio);
  }

  public double maximumRatio(Map<String, String> equations) {
    var p = compile(equations);
    var n = new FormulaSet(defaults, bounds);
    if (parameterBoard()) return parameterMaximum(p, n);
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
    double ratio = parameterBoard() ? maximumRatio(equations) : total > 0 ? edited / total : 1;
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
