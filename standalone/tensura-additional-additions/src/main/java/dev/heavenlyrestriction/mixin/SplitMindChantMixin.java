package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.SplitMindSkill;
import io.github.manasmods.tensura.ability.magic.MagicUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MagicUtils.class)
public abstract class SplitMindChantMixin {
  @Inject(method = "hasChantAnnulment", at = @At("HEAD"), cancellable = true, remap = false)
  private static void additions$splitMind(LivingEntity e, CallbackInfoReturnable<Boolean> cir) {
    if (SplitMindSkill.active(e)) cir.setReturnValue(true);
  }
}
