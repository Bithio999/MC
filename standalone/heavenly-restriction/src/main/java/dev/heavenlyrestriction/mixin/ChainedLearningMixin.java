package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.ChainedState;
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
    /** Native rollback retains the unfinished entry; intercept before acquisition callbacks bill capacity. */
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="addLearnPoint(Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;Lnet/minecraft/world/entity/LivingEntity;ID)Z",at=@At(value="INVOKE",target="Lio/github/manasmods/manascore/skill/api/SkillEvents$UnlockSkillEvent;unlockSkill(Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/network/api/util/Changeable;)Ldev/architectury/event/EventResult;"),remap=false)
    private dev.architectury.event.EventResult restriction$complete(io.github.manasmods.manascore.skill.api.SkillEvents.UnlockSkillEvent event,ManasSkillInstance i,LivingEntity owner,io.github.manasmods.manascore.network.api.util.Changeable<net.minecraft.network.chat.MutableComponent> message,com.llamalad7.mixinextras.injector.wrapoperation.Operation<dev.architectury.event.EventResult> original){
        if(!ChainedState.canLearn(owner,i)){ChainedState.notifyBlocked(owner,i);return dev.architectury.event.EventResult.interruptFalse();}
        return original.call(event,i,owner,message);
    }
    @Inject(method="getDefaultLearningIncrement",at=@At("RETURN"),cancellable=true,remap=false)
    private void restriction$learning(ManasSkillInstance i,LivingEntity owner,int mode,CallbackInfoReturnable<Double> out){if((Object)this instanceof Battlewill && ChainedState.active(owner))out.setReturnValue(out.getReturnValue()+9);}
    @Inject(method="getDefaultMasteryIncrement",at=@At("RETURN"),cancellable=true,remap=false)
    private void restriction$mastery(ManasSkillInstance i,LivingEntity owner,int mode,CallbackInfoReturnable<Double> out){if((Object)this instanceof Battlewill && ChainedState.active(owner))out.setReturnValue(out.getReturnValue()+9);}
}
