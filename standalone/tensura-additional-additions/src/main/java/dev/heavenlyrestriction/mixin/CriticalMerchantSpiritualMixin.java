package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.critical.CriticalMerchant;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TensuraDamageHelper.class)
public abstract class CriticalMerchantSpiritualMixin {
  @WrapMethod(
      method =
          "directSpiritualHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;FF)Z",
      remap = false)
  private static boolean merchant$spiritual(
      LivingEntity target,
      Entity owner,
      DamageSource source,
      float amount,
      float resistance,
      Operation<Boolean> original) {
    return CriticalMerchant.observeSpiritual(
        target,
        source,
        () ->
            original.call(
                target,
                owner,
                source,
                CriticalMerchant.spiritual(target, source, amount),
                resistance));
  }
}
