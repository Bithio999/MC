package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.laplace.*;
import dev.heavenlyrestriction.laplace.expand.*;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "io.github.manasmods.tensura.ability.magic.Magic", remap = false)
public abstract class LaplaceExpandedDirectCostMixin {
  @WrapMethod(
      method =
          "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;DD)Z")
  private boolean laplace$direct(
      LivingEntity owner,
      ManasSkillInstance skill,
      double aura,
      double mp,
      Operation<Boolean> original) {
    var frame = ExpandedRuntime.current();
    if (frame == null
        || frame.owner() != owner
        || frame.skill() != skill
        || PaymentRouting.scoped(owner)) return original.call(owner, skill, aura, mp);
    double bill =
        ExpandedRuntime.budget(
            owner, skill, frame.mode(), mp + aura * LaplaceConfig.rates().magiculesPerAura());
    if (bill < 0) return true;
    if (Double.isNaN(bill)) return original.call(owner, skill, aura, mp);
    try (var scope = PaymentRouting.open(owner, skill, frame.mode())) {
      return original.call(owner, skill, 0d, bill) || scope.failed();
    }
  }
}
