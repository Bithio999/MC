package dev.heavenlyrestriction.laplace.expand;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

class ParameterTransformTest {
  @Test
  void primitiveFieldHooksKeepStackShapeAndProtectNativeCosts() throws Exception {
    var cls = new ClassNode();
    cls.version = Opcodes.V21;
    cls.name = "example/Probe";
    cls.superName = "java/lang/Object";
    String owner = "io/github/manasmods/tensura/config/ability/magic/AspectualMagicConfig$Fire";
    var m =
        new MethodNode(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "probe", "(L" + owner + ";)F", null, null);
    m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
    m.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, owner, "fireDamage", "F"));
    m.instructions.add(new InsnNode(Opcodes.FRETURN));
    m.maxStack = 1;
    m.maxLocals = 1;
    cls.methods.add(m);
    ParameterTransformer.apply(cls);
    assertEquals(5, m.instructions.size());
    var hook = (MethodInsnNode) m.instructions.get(3);
    assertEquals("floating", hook.name);
    new Analyzer<>(new BasicInterpreter()).analyze(cls.name, m);
  }
}
