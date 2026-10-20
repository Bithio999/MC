package dev.wildhunt.mixin;

import dev.wildhunt.LughArts;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.TensuraSkill;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native activation/channel checks for one explicitly sealed technique, without mutating ownership. */
@Mixin(TensuraSkill.class)
public abstract class LughCountermandMixin {
    @Inject(method="canActivateSkill",at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$countermand(ManasSkillInstance instance,LivingEntity owner,int mode,CallbackInfoReturnable<Boolean> ci){
        if(LughArts.sealed(owner,instance.getSkill()))ci.setReturnValue(false);
    }
}
