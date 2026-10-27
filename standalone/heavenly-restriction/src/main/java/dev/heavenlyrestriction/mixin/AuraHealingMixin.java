package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.AuraRegenerationSkill;
import io.github.manasmods.tensura.ability.SkillUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The native heal event calls this gate too, not just regeneration's own tick. */
@Mixin(SkillUtils.class)
public abstract class AuraHealingMixin {
    @Inject(method = "shouldCancelHealing", at = @At("HEAD"), cancellable = true, remap = false)
    private static void restriction$infiniteAuraRecovery(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (AuraRegenerationSkill.allowsInfiniteRecovery(entity)) cir.setReturnValue(false);
    }
}
