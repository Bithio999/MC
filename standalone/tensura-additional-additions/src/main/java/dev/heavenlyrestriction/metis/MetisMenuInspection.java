package dev.heavenlyrestriction.metis;

import java.io.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Read-only, bounded callback inspection. Never probes an ability by activating it. */
public final class MetisMenuInspection {
  private static final Map<String, ClassNode> CLASSES = new HashMap<>();
  private static final Map<Key, Boolean> CACHE = new LinkedHashMap<>();

  private record Key(Class<?> type, int mode, boolean mastered) {}

  private record Method(String owner, MethodNode code) {}

  private record Range(double low, double high) {
    static Range exact(double n) {
      return new Range(n, n);
    }
  }

  private enum Ref {
    INSTANCE,
    OWNER
  }

  public static boolean blocks(Class<?> type, int mode, boolean mastered) {
    var key = new Key(type, mode, mastered);
    if (CACHE.size() > 1024) CACHE.clear();
    return CACHE.computeIfAbsent(
        key,
        k -> {
          try {
            return new Query(type.getClassLoader(), mastered)
                .inspect(type.getName().replace('.', '/'), mode);
          } catch (IOException | AnalyzerException | RuntimeException ex) {
            return true;
          }
        });
  }

  private static final class Query {
    final ClassLoader loader;
    final boolean mastered;
    final Set<String> seen = new HashSet<>();
    int visits;

    Query(ClassLoader loader, boolean mastered) {
      this.loader = loader;
      this.mastered = mastered;
    }

    ClassNode node(String owner) throws IOException {
      var cached = CLASSES.get(owner);
      if (cached != null) return cached;
      try (var in = loader.getResourceAsStream(owner + ".class")) {
        if (in == null) throw new IOException("Missing callback class " + owner);
        var n = new ClassNode();
        new ClassReader(in).accept(n, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        CLASSES.put(owner, n);
        return n;
      }
    }

    Method resolve(String owner, String name, String desc, Set<String> path) throws IOException {
      if (owner == null || !path.add(owner) || framework(owner)) return null;
      var n = node(owner);
      for (var m : n.methods)
        if (m.name.equals(name) && (desc == null || m.desc.equals(desc)))
          return new Method(owner, m);
      var parent = resolve(n.superName, name, desc, path);
      if (parent != null) return parent;
      for (var face : n.interfaces) {
        var found = resolve(face, name, desc, path);
        if (found != null) return found;
      }
      return null;
    }

    boolean inspect(String owner, int mode) throws IOException, AnalyzerException {
      for (String name : List.of("onPressed", "onHeld", "onRelease")) {
        var method = resolve(owner, name, null, new HashSet<>());
        if (method == null) continue;
        if (method.code.instructions.size() == 0
            && (method.code.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) != 0) return true;
        var args = Type.getArgumentTypes(method.code.desc);
        var bindings = new HashMap<Integer, Object>();
        bindings.put(1, Ref.INSTANCE);
        bindings.put(2, Ref.OWNER);
        int slot = 1;
        for (int n = 0; n < args.length; n++) {
          if (n == args.length - 1) bindings.put(slot, Range.exact(mode));
          slot += args[n].getSize();
        }
        if (walk(method, bindings, 0)) return true;
      }
      return false;
    }

    boolean walk(Method method, Map<Integer, Object> bindings, int depth)
        throws IOException, AnalyzerException {
      if (method == null || method.code.instructions.size() == 0) return false;
      String key = method.owner + "." + method.code.name + method.code.desc + bindings;
      if (!seen.add(key)) return false;
      if (depth > 12 || ++visits > 192) return true;
      var code = method.code;
      var frames = new Analyzer<>(new SourceInterpreter()).analyze(method.owner, code);
      var todo = new ArrayDeque<Integer>();
      var reached = new HashSet<Integer>();
      todo.add(0);
      for (var handler : code.tryCatchBlocks) todo.add(code.instructions.indexOf(handler.handler));
      while (!todo.isEmpty()) {
        int at = todo.removeFirst();
        if (at < 0 || at >= frames.length || !reached.add(at) || frames[at] == null) continue;
        var insn = code.instructions.get(at);
        int op = insn.getOpcode();
        var f = frames[at];
        if (insn instanceof MethodInsnNode call) {
          if (menu(call.owner, call.name)) return true;
          if (call.owner.startsWith("io/github/manasmods/manascore/skill/")
              && Set.of("onPressed", "onHeld", "onRelease").contains(call.name)) return true;
          if (!framework(call.owner)) {
            var args = Type.getArgumentTypes(call.desc);
            var values = new HashMap<Integer, Object>();
            int local = call.getOpcode() == Opcodes.INVOKESTATIC ? 0 : 1;
            for (int n = 0; n < args.length; n++) {
              Object value =
                  value(
                      f.getStack(f.getStackSize() - args.length + n),
                      frames,
                      code,
                      bindings,
                      new HashSet<>());
              if (value != null) values.put(local, value);
              local += args[n].getSize();
            }
            if (walk(resolve(call.owner, call.name, call.desc, new HashSet<>()), values, depth + 1))
              return true;
          }
        }
        if (insn instanceof TypeInsnNode t && op == Opcodes.NEW && menu(t.desc, "<init>"))
          return true;
        if (insn instanceof InvokeDynamicInsnNode dynamic) {
          // Lambda bodies may open a menu indirectly; inspect them with captured constants.
          for (Object arg : dynamic.bsmArgs)
            if (arg instanceof Handle handle && !framework(handle.getOwner())) {
              var values = new HashMap<Integer, Object>();
              var captured = Type.getArgumentTypes(dynamic.desc);
              int local = handle.getTag() == Opcodes.H_INVOKESTATIC ? 0 : 1;
              for (int n = 0; n < captured.length; n++) {
                Object v =
                    value(
                        f.getStack(f.getStackSize() - captured.length + n),
                        frames,
                        code,
                        bindings,
                        new HashSet<>());
                if (v != null) values.put(local, v);
                local += captured[n].getSize();
              }
              if (walk(
                  resolve(handle.getOwner(), handle.getName(), handle.getDesc(), new HashSet<>()),
                  values,
                  depth + 1)) return true;
            }
        }
        if (insn instanceof JumpInsnNode jump) {
          if (op == Opcodes.GOTO) {
            todo.add(code.instructions.indexOf(jump.label));
            continue;
          }
          Object a =
              f.getStackSize() > 0
                  ? value(f.getStack(f.getStackSize() - 1), frames, code, bindings, new HashSet<>())
                  : null;
          Object b =
              f.getStackSize() > 1
                  ? value(f.getStack(f.getStackSize() - 2), frames, code, bindings, new HashSet<>())
                  : null;
          Boolean take = branch(op, a, b);
          if (!Boolean.FALSE.equals(take)) todo.add(code.instructions.indexOf(jump.label));
          if (!Boolean.TRUE.equals(take)) todo.add(at + 1);
          continue;
        }
        if (insn instanceof TableSwitchInsnNode table) {
          Object v =
              value(f.getStack(f.getStackSize() - 1), frames, code, bindings, new HashSet<>());
          if (v instanceof Range n && n.low == n.high)
            todo.add(
                code.instructions.indexOf(
                    n.low >= table.min && n.low <= table.max
                        ? table.labels.get((int) n.low - table.min)
                        : table.dflt));
          else {
            for (var label : table.labels) todo.add(code.instructions.indexOf(label));
            todo.add(code.instructions.indexOf(table.dflt));
          }
          continue;
        }
        if (insn instanceof LookupSwitchInsnNode lookup) {
          Object v =
              value(f.getStack(f.getStackSize() - 1), frames, code, bindings, new HashSet<>());
          if (v instanceof Range n && n.low == n.high) {
            int k = lookup.keys.indexOf((int) n.low);
            todo.add(code.instructions.indexOf(k < 0 ? lookup.dflt : lookup.labels.get(k)));
          } else {
            for (var label : lookup.labels) todo.add(code.instructions.indexOf(label));
            todo.add(code.instructions.indexOf(lookup.dflt));
          }
          continue;
        }
        if (op < Opcodes.IRETURN || op > Opcodes.RETURN) if (op != Opcodes.ATHROW) todo.add(at + 1);
      }
      return false;
    }

    Object value(
        SourceValue v,
        Frame<SourceValue>[] frames,
        MethodNode code,
        Map<Integer, Object> bindings,
        Set<AbstractInsnNode> path) {
      Object result = null;
      for (var insn : v.insns) {
        if (!path.add(insn)) return null;
        int op = insn.getOpcode(), at = code.instructions.indexOf(insn);
        var f = frames[at];
        Object next = null;
        if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5)
          next = Range.exact(op - Opcodes.ICONST_0);
        else if (op == Opcodes.DCONST_0 || op == Opcodes.DCONST_1)
          next = Range.exact(op - Opcodes.DCONST_0);
        else if (op == Opcodes.FCONST_0 || op == Opcodes.FCONST_1 || op == Opcodes.FCONST_2)
          next = Range.exact(op - Opcodes.FCONST_0);
        else if (insn instanceof IntInsnNode n) next = Range.exact(n.operand);
        else if (insn instanceof LdcInsnNode n && n.cst instanceof Number number)
          next = Range.exact(number.doubleValue());
        else if (insn instanceof VarInsnNode n
            && (op == Opcodes.ILOAD
                || op == Opcodes.ALOAD
                || op == Opcodes.DLOAD
                || op == Opcodes.FLOAD)) {
          var local = f.getLocal(n.var);
          next =
              local.insns.isEmpty()
                  ? bindings.get(n.var)
                  : value(local, frames, code, bindings, path);
        } else if (insn instanceof VarInsnNode n
            && (op == Opcodes.ISTORE
                || op == Opcodes.ASTORE
                || op == Opcodes.DSTORE
                || op == Opcodes.FSTORE))
          next = value(f.getStack(f.getStackSize() - 1), frames, code, bindings, path);
        else if (op == Opcodes.CHECKCAST
            || op == Opcodes.DUP
            || op == Opcodes.I2D
            || op == Opcodes.I2F)
          next = value(f.getStack(f.getStackSize() - 1), frames, code, bindings, path);
        else if (insn instanceof MethodInsnNode call) {
          int n = Type.getArgumentTypes(call.desc).length;
          Object receiver =
              f.getStackSize() > n
                  ? value(f.getStack(f.getStackSize() - n - 1), frames, code, bindings, path)
                  : null;
          if (receiver == Ref.INSTANCE && call.name.equals("getMastery"))
            next = new Range(0, Double.MAX_VALUE);
          else if (receiver == Ref.INSTANCE && call.name.equals("isMastered"))
            next = Range.exact(mastered ? 1 : 0);
          else if (receiver == Ref.OWNER && call.name.equals("isShiftKeyDown"))
            next = Range.exact(0);
        } else if (op >= Opcodes.LCMP && op <= Opcodes.DCMPG) {
          Object a =
              value(f.getStack(f.getStackSize() - 2), frames, code, bindings, new HashSet<>(path));
          Object b =
              value(f.getStack(f.getStackSize() - 1), frames, code, bindings, new HashSet<>(path));
          if (a instanceof Range x && b instanceof Range y)
            next = new Range(Double.compare(x.low, y.high), Double.compare(x.high, y.low));
        }
        path.remove(insn);
        if (next == null || result != null && !result.equals(next)) return null;
        result = next;
      }
      return result;
    }
  }

  private static Boolean branch(int op, Object top, Object below) {
    if (!(top instanceof Range a)) return null;
    if (op >= Opcodes.IFEQ && op <= Opcodes.IFLE)
      return compare(op - Opcodes.IFEQ, a, Range.exact(0));
    if (op >= Opcodes.IF_ICMPEQ && op <= Opcodes.IF_ICMPLE && below instanceof Range b)
      return compare(op - Opcodes.IF_ICMPEQ, b, a);
    return null;
  }

  private static Boolean compare(int op, Range a, Range b) {
    return switch (op) {
      case 0 ->
          a.low == a.high && a.equals(b) ? true : a.high < b.low || a.low > b.high ? false : null;
      case 1 -> {
        var eq = compare(0, a, b);
        yield eq == null ? null : !eq;
      }
      case 2 -> a.high < b.low ? true : a.low >= b.high ? false : null;
      case 3 -> a.low >= b.high ? true : a.high < b.low ? false : null;
      case 4 -> a.low > b.high ? true : a.high <= b.low ? false : null;
      case 5 -> a.high <= b.low ? true : a.low > b.high ? false : null;
      default -> null;
    };
  }

  private static boolean menu(String owner, String name) {
    String o = owner.toLowerCase(Locale.ROOT), n = name.toLowerCase(Locale.ROOT);
    return n.equals("openmenu")
        || n.equals("setscreen")
        || n.equals("openitemgui")
        || (n.startsWith("open") || n.startsWith("show"))
            && (n.contains("menu")
                || n.contains("screen")
                || n.contains("gui")
                || n.contains("inventory")
                || o.contains("network"))
        || name.equals("<init>")
            && (o.contains("/menu/")
                || o.endsWith("menu")
                || o.endsWith("screen")
                || o.contains("openscreen")
                || o.contains("openmenu")
                || o.contains("opengui"));
  }

  private static boolean framework(String owner) {
    return owner.startsWith("java/")
        || owner.startsWith("javax/")
        || owner.startsWith("net/minecraft/")
        || owner.startsWith("com/google/")
        || owner.startsWith("com/mojang/")
        || owner.startsWith("org/objectweb/")
        || owner.startsWith("org/joml/")
        || owner.startsWith("org/slf4j/")
        || owner.startsWith("org/apache/")
        || owner.startsWith("org/lwjgl/")
        || owner.startsWith("net/neoforged/")
        || owner.startsWith("io/github/manasmods/manascore/")
        || owner.startsWith("dev/architectury/");
  }

  private MetisMenuInspection() {}
}
