package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import io.github.manasmods.manascore.skill.impl.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TickingSkill.class)
public abstract class SplitMindAimMixin {
  @WrapMethod(method = "tick", remap = false)
  private boolean additions$autoAim(
      SkillStorage storage, LivingEntity e, Operation<Boolean> original) {
    return SplitMindCasting.tickAimed(
        (TickingSkill) (Object) this, e, () -> original.call(storage, e));
  }
}
