package dev.wildhunt.mixin;

import dev.wildhunt.GodlySkill;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use native degradation gates for the attack only; learned defenses remain intact. */
@Mixin(ResistSkill.class)
public abstract class GodlyResistanceMixin {
    @Inject(method={"isResistanceBypass","isNullificationBypass"},at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$degrade(DamageSource source,CallbackInfoReturnable<Boolean> ci) {
        if(GodlySkill.passiveActive(source.getEntity()))ci.setReturnValue(true);
    }
}
