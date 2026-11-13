package dev.heavenlyrestriction.laplace.expand;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Pure bytecode transformation; safe to verify without starting Minecraft or Mixin. */
public final class ParameterTransformer {
  public static void apply(ClassNode node) {
    for (var method : node.methods) {
      if (method.name.equals("<clinit>")) continue;
      boolean changed = false;
      for (var insn : method.instructions.toArray())
        if (insn instanceof FieldInsnNode f
            && f.getOpcode() == Opcodes.GETFIELD
            && ParameterKey.allowed(f.owner, f.name, f.desc)) {
          String id = f.owner + "." + f.name;
          var hook = new InsnList();
          hook.add(new LdcInsnNode(ParameterKey.key(id)));
          String name =
              switch (f.desc) {
                case "I" -> "integer";
                case "F" -> "floating";
                case "J" -> "longValue";
                default -> "decimal";
              };
          hook.add(
              new MethodInsnNode(
                  Opcodes.INVOKESTATIC,
                  "dev/heavenlyrestriction/laplace/expand/ParameterRuntime",
                  name,
                  "(" + f.desc + "Ljava/lang/String;)" + f.desc,
                  false));
          method.instructions.insert(f, hook);
          changed = true;
        }
      if (changed) method.maxStack += 1;
    }
  }

  private ParameterTransformer() {}
}
