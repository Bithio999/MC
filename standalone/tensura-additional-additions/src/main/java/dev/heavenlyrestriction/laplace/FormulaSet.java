package dev.heavenlyrestriction.laplace;

import java.util.*;

/** Bounded arithmetic expressions and an acyclic graph of named equations; no scripting runtime. */
public final class FormulaSet {
  public static final int MAX_EQUATIONS = 12, MAX_LENGTH = 160, MAX_NODES = 64;
  public static final double LIMIT = 1e150;

  public record Range(double low, double high) {
    public Range {
      if (!Double.isFinite(low)
          || !Double.isFinite(high)
          || low > high
          || Math.max(Math.abs(low), Math.abs(high)) > LIMIT)
        throw new IllegalArgumentException(
            "The formula produces an invalid or excessively large value.");
    }

    public static Range fixed(double n) {
      return new Range(n, n);
    }
  }

  private interface Lookup<T> {
    T get(String name);
  }

  private interface Node {
    double value(Lookup<Double> variables);

    Range bounds(Lookup<Range> variables);
  }

  private record Constant(double n) implements Node {
    public double value(Lookup<Double> v) {
      return n;
    }

    public Range bounds(Lookup<Range> v) {
      return Range.fixed(n);
    }
  }

  private record Variable(String name) implements Node {
    public double value(Lookup<Double> v) {
      return v.get(name);
    }

    public Range bounds(Lookup<Range> v) {
      return v.get(name);
    }
  }

  private record Unary(Node a) implements Node {
    public double value(Lookup<Double> v) {
      return -a.value(v);
    }

    public Range bounds(Lookup<Range> v) {
      var r = a.bounds(v);
      return new Range(-r.high, -r.low);
    }
  }

  private record Binary(char op, Node a, Node b) implements Node {
    public double value(Lookup<Double> v) {
      double x = a.value(v), y = b.value(v);
      return finite(
          switch (op) {
            case '+' -> x + y;
            case '-' -> x - y;
            case '*' -> x * y;
            case '/' -> {
              if (y == 0) throw new IllegalArgumentException("Division by zero.");
              yield x / y;
            }
            case '^' -> Math.pow(x, y);
            default -> throw new IllegalStateException();
          });
    }

    public Range bounds(Lookup<Range> v) {
      var x = a.bounds(v);
      var y = b.bounds(v);
      return switch (op) {
        case '+' -> new Range(x.low + y.low, x.high + y.high);
        case '-' -> new Range(x.low - y.high, x.high - y.low);
        case '*' -> extrema(x.low * y.low, x.low * y.high, x.high * y.low, x.high * y.high);
        case '/' -> {
          if (y.low <= 0 && y.high >= 0)
            throw new IllegalArgumentException("A divisor can reach zero. Use max(value, 0.01).");
          yield extrema(x.low / y.low, x.low / y.high, x.high / y.low, x.high / y.high);
        }
        case '^' -> {
          if (y.low != y.high || y.low < 0 || y.high > 3 || x.low < 0)
            throw new IllegalArgumentException(
                "Powers need a nonnegative base and a constant exponent from 0 to 3.");
          yield new Range(Math.pow(x.low, y.low), Math.pow(x.high, y.high));
        }
        default -> throw new IllegalStateException();
      };
    }
  }

  private record Function(String name, List<Node> args) implements Node {
    public double value(Lookup<Double> v) {
      double a = args.get(0).value(v);
      return finite(
          switch (name) {
            case "abs" -> Math.abs(a);
            case "min" -> Math.min(a, args.get(1).value(v));
            case "max" -> Math.max(a, args.get(1).value(v));
            case "clamp" -> Math.clamp(a, args.get(1).value(v), args.get(2).value(v));
            default -> throw new IllegalStateException();
          });
    }

    public Range bounds(Lookup<Range> v) {
      var a = args.get(0).bounds(v);
      if (name.equals("abs"))
        return new Range(
            a.low <= 0 && a.high >= 0 ? 0 : Math.min(Math.abs(a.low), Math.abs(a.high)),
            Math.max(Math.abs(a.low), Math.abs(a.high)));
      var b = args.get(1).bounds(v);
      if (name.equals("min")) return new Range(Math.min(a.low, b.low), Math.min(a.high, b.high));
      if (name.equals("max")) return new Range(Math.max(a.low, b.low), Math.max(a.high, b.high));
      var c = args.get(2).bounds(v);
      if (b.high > c.low)
        throw new IllegalArgumentException("Clamp's lower bound must not exceed its upper bound.");
      return new Range(Math.clamp(a.low, b.low, c.low), Math.clamp(a.high, b.high, c.high));
    }
  }

  private static Range extrema(double... values) {
    return new Range(
        Arrays.stream(values).min().orElseThrow(), Arrays.stream(values).max().orElseThrow());
  }

  private static double finite(double value) {
    if (!Double.isFinite(value) || Math.abs(value) > LIMIT)
      throw new IllegalArgumentException(
          "The formula produces an invalid or excessively large value.");
    return value;
  }

  public static boolean identifier(String name) {
    return name != null
        && name.matches("[a-z][a-z0-9_]{0,31}")
        && !Set.of("min", "max", "clamp", "abs").contains(name);
  }

  private final Map<String, Node> nodes;
  private final Map<String, Range> ranges;

  public FormulaSet(Map<String, String> equations, Map<String, Range> inputs) {
    if (equations.size() > MAX_EQUATIONS)
      throw new IllegalArgumentException("Use at most 12 equations.");
    nodes = new LinkedHashMap<>();
    ranges = new HashMap<>(inputs);
    for (var entry : equations.entrySet()) {
      if (!identifier(entry.getKey()))
        throw new IllegalArgumentException("Use a short variable name, such as wounded_bonus.");
      if (inputs.containsKey(entry.getKey()))
        throw new IllegalArgumentException(entry.getKey() + " is a read-only input.");
      nodes.put(entry.getKey(), new Parser(entry.getValue()).parse());
    }
    var visiting = new HashSet<String>();
    for (var name : nodes.keySet()) resolveRange(name, visiting);
  }

  private Range resolveRange(String name, Set<String> visiting) {
    if (ranges.containsKey(name)) return ranges.get(name);
    var node = nodes.get(name);
    if (node == null)
      throw new IllegalArgumentException(
          "Unknown variable: " + name + ". Choose a variable from the palette.");
    if (!visiting.add(name))
      throw new IllegalArgumentException("Circular dependency involving " + name + ".");
    var range = node.bounds(n -> resolveRange(n, visiting));
    visiting.remove(name);
    ranges.put(name, range);
    return range;
  }

  public Range range(String output) {
    return Objects.requireNonNull(ranges.get(output));
  }

  public Map<String, Double> evaluate(Map<String, Double> inputs) {
    return evaluate(inputs, Map.of());
  }

  public Map<String, Double> evaluate(Map<String, Double> inputs, Map<String, Double> overrides) {
    var values = new HashMap<>(inputs);
    values.putAll(overrides);
    for (var name : nodes.keySet()) resolveValue(name, values);
    return Collections.unmodifiableMap(values);
  }

  private double resolveValue(String name, Map<String, Double> values) {
    if (values.containsKey(name)) return finite(values.get(name));
    var node = nodes.get(name);
    if (node == null) throw new IllegalArgumentException("Missing input: " + name);
    double result = finite(node.value(n -> resolveValue(n, values)));
    values.put(name, result);
    return result;
  }

  private static final class Parser {
    private final String text;
    private int cursor, nodes, depth;

    Parser(String text) {
      if (text == null || text.isBlank() || text.length() > MAX_LENGTH)
        throw new IllegalArgumentException("Enter a formula of 1–160 characters.");
      this.text = text;
    }

    Node parse() {
      var n = add();
      space();
      if (cursor != text.length()) throw error("Unexpected symbol");
      return n;
    }

    private Node counted(Node n) {
      if (++nodes > MAX_NODES) throw error("Formula is too complex");
      return n;
    }

    private void space() {
      while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) cursor++;
    }

    private boolean take(char c) {
      space();
      if (cursor < text.length() && text.charAt(cursor) == c) {
        cursor++;
        return true;
      }
      return false;
    }

    private Node add() {
      Node n = multiply();
      while (true) {
        if (take('+')) n = counted(new Binary('+', n, multiply()));
        else if (take('-')) n = counted(new Binary('-', n, multiply()));
        else return n;
      }
    }

    private Node multiply() {
      Node n = unary();
      while (true) {
        if (take('*')) n = counted(new Binary('*', n, unary()));
        else if (take('/')) n = counted(new Binary('/', n, unary()));
        else return n;
      }
    }

    private Node unary() {
      if (++depth > 16) throw error("Formula nesting is too deep");
      try {
        if (take('-')) return counted(new Unary(unary()));
        if (take('+')) return unary();
        return power();
      } finally {
        depth--;
      }
    }

    private Node power() {
      var n = primary();
      return take('^') ? counted(new Binary('^', n, unary())) : n;
    }

    private Node primary() {
      space();
      if (take('(')) {
        var n = add();
        if (!take(')')) throw error("Missing closing parenthesis");
        return n;
      }
      int start = cursor;
      if (cursor < text.length()
          && (Character.isDigit(text.charAt(cursor)) || text.charAt(cursor) == '.')) {
        while (cursor < text.length()
            && (Character.isDigit(text.charAt(cursor)) || text.charAt(cursor) == '.')) cursor++;
        try {
          return counted(new Constant(finite(Double.parseDouble(text.substring(start, cursor)))));
        } catch (NumberFormatException ex) {
          throw error("Invalid number");
        }
      }
      while (cursor < text.length()
          && (text.charAt(cursor) >= 'a' && text.charAt(cursor) <= 'z'
              || text.charAt(cursor) >= '0' && text.charAt(cursor) <= '9'
              || text.charAt(cursor) == '_')) cursor++;
      if (start == cursor) throw error("Expected a number, variable or parenthesis");
      String name = text.substring(start, cursor);
      if (!take('(')) {
        if (!identifier(name)) throw error("Invalid variable name");
        return counted(new Variable(name));
      }
      int required =
          switch (name) {
            case "min", "max" -> 2;
            case "clamp" -> 3;
            case "abs" -> 1;
            default -> throw error("Available functions: min, max, clamp, abs");
          };
      var args = new ArrayList<Node>();
      for (int i = 0; i < required; i++) {
        if (i > 0 && !take(',')) throw error("Separate function arguments with a comma");
        args.add(add());
      }
      if (!take(')')) throw error("Missing closing parenthesis or too many arguments");
      return counted(new Function(name, List.copyOf(args)));
    }

    private IllegalArgumentException error(String message) {
      return new IllegalArgumentException(message + " near position " + (cursor + 1) + ".");
    }
  }
}
