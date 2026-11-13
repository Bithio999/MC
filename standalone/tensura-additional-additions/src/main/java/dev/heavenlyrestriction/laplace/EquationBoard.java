package dev.heavenlyrestriction.laplace;

import java.util.*;

/**
 * Client-independent structural editor. Blocks serialize to the existing bounded formula language.
 */
public final class EquationBoard {
  public enum Kind {
    NUMBER,
    VARIABLE,
    OPERATOR,
    FUNCTION,
    HOLE
  }

  public record Expr(Kind kind, String value, List<Expr> children) {
    public Expr {
      children = List.copyOf(children);
    }

    public static Expr hole() {
      return new Expr(Kind.HOLE, "", List.of());
    }

    public static Expr variable(String name) {
      if (!FormulaSet.identifier(name))
        throw new IllegalArgumentException("Invalid variable name.");
      return new Expr(Kind.VARIABLE, name, List.of());
    }

    public static Expr number(String value) {
      var n = parse(value);
      if (n.kind != Kind.NUMBER
          && !(n.kind == Kind.OPERATOR
              && n.value.equals("neg")
              && n.children.getFirst().kind == Kind.NUMBER))
        throw new IllegalArgumentException("Enter a number, such as 2 or 0.5.");
      return n;
    }

    public static Expr operation(String op) {
      int count =
          switch (op) {
            case "+", "-", "*", "/", "^", "min", "max" -> 2;
            case "clamp" -> 3;
            case "abs", "neg" -> 1;
            default -> throw new IllegalArgumentException("Unknown operation.");
          };
      return new Expr(
          Set.of("min", "max", "clamp", "abs").contains(op) ? Kind.FUNCTION : Kind.OPERATOR,
          op,
          Collections.nCopies(count, hole()));
    }

    public boolean complete() {
      return kind != Kind.HOLE && children.stream().allMatch(Expr::complete);
    }

    public int holes() {
      return (kind == Kind.HOLE ? 1 : 0) + children.stream().mapToInt(Expr::holes).sum();
    }

    public int count() {
      return 1 + children.stream().mapToInt(Expr::count).sum();
    }

    public int depth() {
      return 1 + children.stream().mapToInt(Expr::depth).max().orElse(0);
    }

    private int precedence() {
      return kind != Kind.OPERATOR
          ? 5
          : switch (value) {
            case "+", "-" -> 1;
            case "*", "/" -> 2;
            case "neg" -> 3;
            default -> 4;
          };
    }

    public String formula() {
      if (!complete())
        throw new IllegalArgumentException("Fill the empty input slots on the board.");
      return code();
    }

    private String code() {
      if (kind == Kind.HOLE) return "?";
      if (kind == Kind.NUMBER || kind == Kind.VARIABLE) return value;
      if (kind == Kind.FUNCTION)
        return value + "(" + String.join(",", children.stream().map(Expr::code).toList()) + ")";
      if (value.equals("neg")) {
        var a = children.getFirst();
        return "-" + (a.precedence() < 3 ? "(" + a.code() + ")" : a.code());
      }
      var a = children.get(0);
      var b = children.get(1);
      boolean left =
          a.precedence() < precedence() || value.equals("^") && a.precedence() == precedence();
      boolean right =
          b.precedence() < precedence() || !value.equals("^") && b.precedence() == precedence();
      return (left ? "(" + a.code() + ")" : a.code())
          + value
          + (right ? "(" + b.code() + ")" : b.code());
    }
  }

  public record Address(String equation, List<Integer> path) {
    public Address {
      path = List.copyOf(path);
    }

    public Address child(int n) {
      var p = new ArrayList<>(path);
      p.add(n);
      return new Address(equation, p);
    }

    public boolean contains(Address other) {
      return equation.equals(other.equation)
          && path.size() <= other.path.size()
          && other.path.subList(0, path.size()).equals(path);
    }
  }

  private record Entry(Expr expression, String original) {}

  private LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
  private final Deque<LinkedHashMap<String, Entry>> undo = new ArrayDeque<>(),
      redo = new ArrayDeque<>();

  public EquationBoard(Map<String, String> equations) {
    equations.forEach((k, v) -> entries.put(k, new Entry(parse(v), v)));
  }

  public List<String> names() {
    return List.copyOf(entries.keySet());
  }

  public Expr root(String key) {
    var e = entries.get(key);
    if (e == null) throw new IllegalArgumentException("Select an equation first.");
    return e.expression;
  }

  public Expr at(Address a) {
    var e = root(a.equation);
    for (int n : a.path) {
      if (n < 0 || n >= e.children.size())
        throw new IllegalArgumentException("That input moved. Choose another slot.");
      e = e.children.get(n);
    }
    return e;
  }

  public int holes() {
    return entries.values().stream().mapToInt(e -> e.expression.holes()).sum();
  }

  public LinkedHashMap<String, String> formulas() {
    var result = new LinkedHashMap<String, String>();
    entries.forEach(
        (k, v) -> result.put(k, v.original != null ? v.original : v.expression.formula()));
    return result;
  }

  public boolean canUndo() {
    return !undo.isEmpty();
  }

  public boolean canRedo() {
    return !redo.isEmpty();
  }

  private static Expr replace(Expr root, List<Integer> path, int depth, Expr value) {
    if (depth == path.size()) return value;
    var children = new ArrayList<>(root.children);
    int n = path.get(depth);
    if (n < 0 || n >= children.size()) throw new IllegalArgumentException("That input moved.");
    children.set(n, replace(children.get(n), path, depth + 1, value));
    return new Expr(root.kind, root.value, children);
  }

  private static void check(Map<String, Entry> next) {
    if (next.size() > 12) throw new IllegalArgumentException("Use at most twelve equations.");
    for (var e : next.values()) {
      if (e.expression.count() > 64 || e.expression.depth() > 16)
        throw new IllegalArgumentException("This expression is too large. Undo or simplify it.");
      if (e.expression.complete() && e.expression.formula().length() > 160)
        throw new IllegalArgumentException("Keep each formula within 160 characters.");
    }
  }

  private void commit(LinkedHashMap<String, Entry> next) {
    check(next);
    if (next.equals(entries)) return;
    undo.push(new LinkedHashMap<>(entries));
    while (undo.size() > 40) undo.removeLast();
    redo.clear();
    entries = next;
  }

  private static void put(LinkedHashMap<String, Entry> next, Address a, Expr replacement) {
    var before = next.get(a.equation);
    next.put(a.equation, new Entry(replace(before.expression, a.path, 0, replacement), null));
  }

  /** Operator tools wrap an occupied expression, retaining it as their first input. */
  public void drop(Address target, Expr tool) {
    var old = at(target);
    if (!tool.children.isEmpty()
        && tool.children.stream().allMatch(e -> e.kind == Kind.HOLE)
        && old.kind != Kind.HOLE) {
      var children = new ArrayList<>(tool.children);
      children.set(0, old);
      tool = new Expr(tool.kind, tool.value, children);
    }
    var next = new LinkedHashMap<>(entries);
    put(next, target, tool);
    commit(next);
  }

  /** Board-to-board dragging swaps occupied slots, moves into holes, or copies with Shift. */
  public void transfer(Address source, Address target, boolean copy) {
    if (source.equals(target)) return;
    if (source.contains(target) || target.contains(source))
      throw new IllegalArgumentException(
          "Drop into a different branch; an expression cannot contain itself.");
    var value = at(source);
    var displaced = at(target);
    var next = new LinkedHashMap<>(entries);
    if (!copy) put(next, source, displaced);
    put(next, target, value);
    commit(next);
  }

  public void remove(Address a) {
    var next = new LinkedHashMap<>(entries);
    put(next, a, Expr.hole());
    commit(next);
  }

  public void unwrap(Address a) {
    var e = at(a);
    if (e.children.isEmpty()) throw new IllegalArgumentException("Choose an operation to unwrap.");
    var next = new LinkedHashMap<>(entries);
    put(next, a, e.children.getFirst());
    commit(next);
  }

  public void text(String name, String formula) {
    var next = new LinkedHashMap<>(entries);
    next.put(name, new Entry(parse(formula), formula));
    commit(next);
  }

  public void addHelper(String name) {
    if (!FormulaSet.identifier(name) || entries.containsKey(name))
      throw new IllegalArgumentException("Choose a new helper name.");
    var next = new LinkedHashMap<>(entries);
    next.put(name, new Entry(Expr.number("1"), "1"));
    commit(next);
  }

  public void deleteHelper(String name, Set<String> nativeOutputs) {
    if (nativeOutputs.contains(name))
      throw new IllegalArgumentException("Native outputs cannot be removed.");
    var next = new LinkedHashMap<>(entries);
    next.remove(name);
    commit(next);
  }

  public void reset(Map<String, String> defaults) {
    var next = new LinkedHashMap<String, Entry>();
    defaults.forEach((k, v) -> next.put(k, new Entry(parse(v), v)));
    commit(next);
  }

  public void balance(EquationModel model) {
    var next = new LinkedHashMap<String, Entry>();
    model.balanceCost(formulas()).forEach((k, v) -> next.put(k, new Entry(parse(v), v)));
    commit(next);
  }

  public void undo() {
    if (undo.isEmpty()) return;
    redo.push(new LinkedHashMap<>(entries));
    entries = undo.pop();
  }

  public void redo() {
    if (redo.isEmpty()) return;
    undo.push(new LinkedHashMap<>(entries));
    entries = redo.pop();
  }

  public static Expr parse(String formula) {
    return new Parser(formula).parse();
  }

  private static final class Parser {
    final String text;
    int cursor, nodes, depth;

    Parser(String s) {
      if (s == null || s.isBlank() || s.length() > 160)
        throw new IllegalArgumentException("Enter a formula of 1–160 characters.");
      text = s;
    }

    Expr node(Kind kind, String value, Expr... args) {
      if (++nodes > 64) throw error("Too many blocks");
      return new Expr(kind, value, List.of(args));
    }

    void space() {
      while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) cursor++;
    }

    boolean take(char c) {
      space();
      if (cursor < text.length() && text.charAt(cursor) == c) {
        cursor++;
        return true;
      }
      return false;
    }

    Expr parse() {
      var n = add();
      space();
      if (cursor != text.length()) throw error("Unexpected symbol");
      return n;
    }

    Expr add() {
      var n = multiply();
      while (true) {
        if (take('+')) n = node(Kind.OPERATOR, "+", n, multiply());
        else if (take('-')) n = node(Kind.OPERATOR, "-", n, multiply());
        else return n;
      }
    }

    Expr multiply() {
      var n = unary();
      while (true) {
        if (take('*')) n = node(Kind.OPERATOR, "*", n, unary());
        else if (take('/')) n = node(Kind.OPERATOR, "/", n, unary());
        else return n;
      }
    }

    Expr unary() {
      if (++depth > 16) throw error("Formula nesting is too deep");
      try {
        if (take('-')) return node(Kind.OPERATOR, "neg", unary());
        if (take('+')) return unary();
        return power();
      } finally {
        depth--;
      }
    }

    Expr power() {
      var n = primary();
      return take('^') ? node(Kind.OPERATOR, "^", n, unary()) : n;
    }

    Expr primary() {
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
        String number = text.substring(start, cursor);
        try {
          double n = Double.parseDouble(number);
          if (!Double.isFinite(n) || Math.abs(n) > FormulaSet.LIMIT)
            throw error("Number is too large");
        } catch (NumberFormatException ex) {
          throw error("Invalid number");
        }
        return node(Kind.NUMBER, number);
      }
      while (cursor < text.length()
          && (Character.isLowerCase(text.charAt(cursor))
              || Character.isDigit(text.charAt(cursor))
              || text.charAt(cursor) == '_')) cursor++;
      String name = text.substring(start, cursor);
      if (!FormulaSet.identifier(name) && !Set.of("min", "max", "clamp", "abs").contains(name))
        throw error("Choose a variable from the library");
      if (!take('(')) {
        if (!FormulaSet.identifier(name)) throw error("Functions need parentheses and inputs");
        return node(Kind.VARIABLE, name);
      }
      int n =
          switch (name) {
            case "min", "max" -> 2;
            case "clamp" -> 3;
            case "abs" -> 1;
            default -> throw error("Unknown function");
          };
      var args = new Expr[n];
      for (int j = 0; j < n; j++) {
        if (j > 0 && !take(',')) throw error("Separate inputs with a comma");
        args[j] = add();
      }
      if (!take(')')) throw error("Missing closing parenthesis");
      return node(Kind.FUNCTION, name, args);
    }

    IllegalArgumentException error(String s) {
      return new IllegalArgumentException(s + " near position " + (cursor + 1) + ".");
    }
  }
}
