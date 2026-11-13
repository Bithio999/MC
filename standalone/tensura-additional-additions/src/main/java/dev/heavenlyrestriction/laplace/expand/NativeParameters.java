package dev.heavenlyrestriction.laplace.expand;

import io.github.manasmods.manascore.config.ConfigRegistry;
import java.lang.reflect.*;
import java.util.*;

/** Reads live native config, never invokes an ability and never writes a shared singleton. */
public final class NativeParameters {
  private static final Map<String, Reader> READERS = new HashMap<>();

  private record Reader(Class<?> root, List<Field> path, Field field) {
    Number read() throws ReflectiveOperationException {
      Object object = ConfigRegistry.getConfig((Class) root);
      for (var f : path) object = f.get(object);
      return (Number) field.get(object);
    }
  }

  public static double read(ParameterInspection.FieldRef ref) {
    try {
      var r = READERS.get(ref.identity());
      if (r == null) {
        var type =
            Class.forName(
                ref.owner().replace('/', '.'), true, NativeParameters.class.getClassLoader());
        var root = Class.forName(type.getName().split("\\$")[0], true, type.getClassLoader());
        Object instance = ConfigRegistry.getConfig((Class) root);
        var path = find(instance, type, new IdentityHashMap<>(), 0);
        if (path == null) throw new IllegalArgumentException("No native config section");
        r = new Reader(root, path, type.getField(ref.name()));
        READERS.put(ref.identity(), r);
      }
      return r.read().doubleValue();
    } catch (ReflectiveOperationException | RuntimeException ex) {
      return Double.NaN;
    }
  }

  private static List<Field> find(
      Object at, Class<?> wanted, IdentityHashMap<Object, Boolean> seen, int depth)
      throws ReflectiveOperationException {
    if (at == null || depth > 5 || seen.put(at, true) != null) return null;
    if (wanted.isInstance(at)) return List.of();
    for (var field : at.getClass().getFields())
      if (!Modifier.isStatic(field.getModifiers())
          && field
              .getType()
              .getName()
              .startsWith("io.github.manasmods.tensura.config/".replace('/', '.'))) {
        var path = find(field.get(at), wanted, seen, depth + 1);
        if (path != null) {
          var list = new ArrayList<Field>();
          list.add(field);
          list.addAll(path);
          return List.copyOf(list);
        }
      }
    return null;
  }

  private NativeParameters() {}
}
