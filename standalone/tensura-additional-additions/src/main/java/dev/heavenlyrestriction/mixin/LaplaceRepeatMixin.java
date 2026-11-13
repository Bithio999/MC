package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.laplace.LaplaceRuntime;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
    targets = "io.github.manasmods.tensura.ability.magic.aspectual.water.WaterCutterMagic",
    remap = false)
public abstract class LaplaceRepeatMixin {
  @WrapMethod(method = "onHeld")
  private boolean laplace$repeat(
      ManasSkillInstance i, LivingEntity e, int held, int mode, Operation<Boolean> original) {
    try (var scope = LaplaceRuntime.cast(e, i, mode)) {
      return original.call(i, e, held, mode);
    }
  }
}
