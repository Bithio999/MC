package dev.wildhunt.mixin;

import dev.wildhunt.ChainedState;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.LivingEntity;

@Mixin(TensuraSkill.class)
public abstract class ChainedLearningMixin {
    @Inject(method="addLearnPoint(Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;Lnet/minecraft/world/entity/LivingEntity;ID)Z",at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$blockedLearning(ManasSkillInstance i,LivingEntity owner,int mode,double point,CallbackInfoReturnable<Boolean> out){if(i.getMastery()<0 && !ChainedState.canLearn(owner,i))out.setReturnValue(false);}
    @Inject(method="getDefaultLearningIncrement",at=@At("RETURN"),cancellable=true,remap=false)
    private void myths$learning(ManasSkillInstance i,LivingEntity owner,int mode,CallbackInfoReturnable<Double> out){if((Object)this instanceof Battlewill && ChainedState.active(owner))out.setReturnValue(out.getReturnValue()+9);}
    @Inject(method="getDefaultMasteryIncrement",at=@At("RETURN"),cancellable=true,remap=false)
    private void myths$mastery(ManasSkillInstance i,LivingEntity owner,int mode,CallbackInfoReturnable<Double> out){if((Object)this instanceof Battlewill && ChainedState.active(owner))out.setReturnValue(out.getReturnValue()+9);}
}
