package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.laplace.LaplaceRuntime;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
    targets = {
      "io.github.manasmods.tensura.ability.magic.aspectual.fire.FireMagic",
      "io.github.manasmods.tensura.ability.magic.aspectual.wind.WindCutterMagic",
      "io.github.manasmods.tensura.ability.magic.aspectual.water.WaterCutterMagic",
      "io.github.manasmods.tensura.ability.magic.aspectual.enhancement.StrengthMagic",
      "io.github.manasmods.tensura.ability.magic.aspectual.enhancement.ProtectionMagic",
      "io.github.manasmods.tensura.ability.magic.aspectual.enhancement.ReinforcementMagic"
    },
    remap = false)
public abstract class LaplaceMagicMixin {
  @ModifyReturnValue(method = "getMagiculeCost", at = @At("RETURN"))
  private double laplace$cost(double original, LivingEntity e, ManasSkillInstance i, int mode) {
    return LaplaceRuntime.cost(original, e, i, mode);
  }

  @WrapMethod(method = "onRelease")
  private void laplace$cast(
      ManasSkillInstance i, LivingEntity e, int held, int key, int mode, Operation<Void> original) {
    try (var scope = LaplaceRuntime.cast(e, i, mode)) {
      original.call(i, e, held, key, mode);
    }
  }
}
