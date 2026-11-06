package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.vesper.Afterwind;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TensuraDamageHelper.class)
public abstract class AfterwindSpiritualHitMixin {
  @WrapMethod(
      method =
          "directSpiritualHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;FF)Z",
      remap = false)
  private static boolean vesper$spiritual(
      LivingEntity target,
      Entity attacker,
      DamageSource source,
      float amount,
      float resistance,
      Operation<Boolean> original) {
    return Afterwind.observe(
        target, source, () -> original.call(target, attacker, source, amount, resistance));
  }
}
