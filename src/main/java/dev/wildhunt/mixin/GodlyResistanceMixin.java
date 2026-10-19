package dev.wildhunt.mixin;

import dev.wildhunt.GodlySkill;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native attack bypass remains available outside the proximity suppression field. */
@Mixin(ResistSkill.class)
public abstract class GodlyResistanceMixin {
    @Inject(method="canInteractSkill",at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$suppressInteraction(io.github.manasmods.manascore.skill.api.ManasSkillInstance instance,net.minecraft.world.entity.LivingEntity target,CallbackInfoReturnable<Boolean> ci) {
        if(GodlySkill.degraded(target))ci.setReturnValue(false);
    }
    @Inject(method={"isResistanceBypass","isNullificationBypass"},at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$degrade(DamageSource source,CallbackInfoReturnable<Boolean> ci) {
        if(GodlySkill.passiveActive(source.getEntity()))ci.setReturnValue(true);
    }
}
