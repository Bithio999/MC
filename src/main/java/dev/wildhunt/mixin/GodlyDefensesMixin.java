package dev.wildhunt.mixin;

import dev.wildhunt.GodlySkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suppress native resistance queries (including effect immunity) without changing the saved toggle. */
@Mixin(ManasSkillInstance.class)
public abstract class GodlyDefensesMixin {
    @Inject(method="isToggled",at=@At("RETURN"),cancellable=true,remap=false)
    private void myths$suppressed(CallbackInfoReturnable<Boolean> ci) {
        var instance=(ManasSkillInstance)(Object)this;
        if(ci.getReturnValue() && instance.getSkill() instanceof ResistSkill && instance.hasOwningStorage() && GodlySkill.degraded(instance.getOwningStorage().getOwner()))ci.setReturnValue(false);
    }
}
