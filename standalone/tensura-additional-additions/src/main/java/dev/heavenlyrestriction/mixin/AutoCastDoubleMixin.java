package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.splitmind.AutoCastCombat;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native elemental/magic paired hits must reset the same window used by their owning spell. */
@Mixin(value = TensuraDamageHelper.class, remap = false)
public abstract class AutoCastDoubleMixin {
  @WrapOperation(
      method = {"hurtDouble", "hurtSplit"},
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/world/entity/Entity;invulnerableTime:I",
              opcode = Opcodes.GETFIELD,
              remap = true))
  private static int additions$read(
      Entity target,
      Operation<Integer> original,
      @Local(argsOnly = true, ordinal = 0) DamageSource source) {
    var window = AutoCastCombat.window(target, source);
    return window == null ? original.call(target) : window.remaining((LivingEntity) target);
  }

  @WrapOperation(
      method = {"hurtDouble", "hurtSplit"},
      at =
          @At(
              value = "FIELD",
              target = "Lnet/minecraft/world/entity/Entity;invulnerableTime:I",
              opcode = Opcodes.PUTFIELD,
              remap = true))
  private static void additions$write(
      Entity target,
      int value,
      Operation<Void> original,
      @Local(argsOnly = true, ordinal = 0) DamageSource source) {
    var window = AutoCastCombat.window(target, source);
    if (window == null) original.call(target, value);
    else window.duration((LivingEntity) target, value);
  }
}
