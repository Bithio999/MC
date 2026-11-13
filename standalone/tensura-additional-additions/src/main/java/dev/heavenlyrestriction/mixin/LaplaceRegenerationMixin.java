package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.heavenlyrestriction.laplace.LaplaceRuntime;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.github.manasmods.tensura.effect.ability.SelfRegenerationEffect", remap = false)
public abstract class LaplaceRegenerationMixin {
  @WrapOperation(
      method = "applyEffectTick",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V",
              remap = true))
  private void laplace$hp(LivingEntity e, float amount, Operation<Void> original) {
    original.call(e, LaplaceRuntime.regeneration(e, "healing", amount));
  }

  @WrapOperation(
      method = "applyEffectTick",
      at =
          @At(
              value = "INVOKE",
              target = "Lio/github/manasmods/tensura/storage/ep/IExistence;setSpiritualHealth(D)V"))
  private void laplace$shp(
      IExistence energy, double proposed, Operation<Void> original, LivingEntity e, int amplifier) {
    double before = energy.getSpiritualHealth();
    double base =
        io.github.manasmods.tensura.ability.skill.common.SelfRegenerationSkill.CONFIG.regenSHP
            * (amplifier + 1);
    double amount = LaplaceRuntime.regeneration(e, "shp_healing", (float) base);
    original.call(
        energy,
        Math.min(before + amount, e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)));
  }
}
