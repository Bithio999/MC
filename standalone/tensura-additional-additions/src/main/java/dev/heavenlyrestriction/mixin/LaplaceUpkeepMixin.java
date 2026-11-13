package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.heavenlyrestriction.laplace.LaplaceRuntime;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
    targets = "io.github.manasmods.tensura.ability.skill.common.SelfRegenerationSkill",
    remap = false)
public abstract class LaplaceUpkeepMixin {
  @ModifyReturnValue(method = "getMagiculeCost", at = @At("RETURN"))
  private double laplace$cost(double original, LivingEntity e, ManasSkillInstance i, int mode) {
    return LaplaceRuntime.cost(original, e, i, mode);
  }
}
