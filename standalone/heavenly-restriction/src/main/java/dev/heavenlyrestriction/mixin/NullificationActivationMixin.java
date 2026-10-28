package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.Nullification;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.TensuraSkill;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native AI/casts that call canActivateSkill directly honor the same visible seal. */
@Mixin(TensuraSkill.class)
public abstract class NullificationActivationMixin {
    @Inject(method="canActivateSkill",at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$sealed(ManasSkillInstance instance,LivingEntity owner,int mode,CallbackInfoReturnable<Boolean> result){if(Nullification.sealed(owner))result.setReturnValue(false);}
}
