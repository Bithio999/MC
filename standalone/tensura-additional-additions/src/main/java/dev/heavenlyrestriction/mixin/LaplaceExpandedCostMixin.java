package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.heavenlyrestriction.laplace.*;
import dev.heavenlyrestriction.laplace.expand.*;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.TensuraSkill;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TensuraSkill.class, remap = false)
public abstract class LaplaceExpandedCostMixin {
  @WrapOperation(
      method =
          "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;IF)Z",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lio/github/manasmods/tensura/ability/TensuraSkill;isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;DD)Z"))
  private boolean laplace$budget(
      TensuraSkill skill,
      LivingEntity owner,
      ManasSkillInstance i,
      double aura,
      double mp,
      Operation<Boolean> original,
      @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) int mode,
      @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) float multiplier) {
    double nativeBudget = mp + aura * LaplaceConfig.rates().magiculesPerAura();
    double bill =
        multiplier > 0
            ? ExpandedRuntime.budget(owner, i, mode, nativeBudget / multiplier) * multiplier
            : Double.NaN;
    return bill < 0
        ? true
        : Double.isNaN(bill)
            ? original.call(skill, owner, i, aura, mp)
            : original.call(skill, owner, i, 0d, bill);
  }
}
