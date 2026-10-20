package dev.wildhunt.mixin;

import dev.wildhunt.GodlyPassives;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Godly ownership bypasses native damage defenses only for the owner's own hits. */
@Mixin(ResistSkill.class)
public abstract class GodlyResistanceMixin {
    @Inject(method={"isResistanceBypass","isNullificationBypass"},at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$degrade(DamageSource source,CallbackInfoReturnable<Boolean> ci) {
        if(GodlyPassives.bypassesDefenses(source))ci.setReturnValue(true);
    }
}
