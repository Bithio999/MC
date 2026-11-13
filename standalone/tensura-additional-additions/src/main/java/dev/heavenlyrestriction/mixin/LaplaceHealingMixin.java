package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.heavenlyrestriction.laplace.LaplaceRuntime;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
    targets = "io.github.manasmods.tensura.ability.magic.aspectual.recovery.HealingMagic",
    remap = false)
public abstract class LaplaceHealingMixin {
  @WrapMethod(method = "onRelease")
  private void laplace$cast(
      ManasSkillInstance i, LivingEntity e, int held, int key, int mode, Operation<Void> original) {
    LaplaceRuntime.clearHealing();
    try (var scope = LaplaceRuntime.cast(e, i, mode)) {
      original.call(i, e, held, key, mode);
    } finally {
      LaplaceRuntime.clearHealing();
    }
  }

  @WrapOperation(
      method = "onRelease",
      at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F"))
  private float laplace$amount(float missing, float nativeLimit, Operation<Float> original) {
    return LaplaceRuntime.healingLimit(missing, nativeLimit);
  }

  @WrapOperation(
      method = "onRelease",
      at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D"))
  private double laplace$bill(double a, double b, Operation<Double> original) {
    return LaplaceRuntime.healingCost(a, b);
  }

  @WrapOperation(
      method = "onRelease",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lio/github/manasmods/tensura/util/EnergyHelper;isOutOfMagiculeConsuming(Lnet/minecraft/world/entity/LivingEntity;D)D"))
  private double laplace$paid(LivingEntity e, double bill, Operation<Double> original) {
    try (var scope = dev.heavenlyrestriction.laplace.PaymentRouting.healing(e)) {
      double shortfall = original.call(e, bill);
      return LaplaceRuntime.healingShortfall(scope.failed() ? bill : shortfall, bill);
    }
  }

  @WrapOperation(
      method = "onRelease",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V",
              remap = true))
  private void laplace$heal(LivingEntity e, float amount, Operation<Void> original) {
    original.call(e, LaplaceRuntime.paidHealing(amount));
  }
}
