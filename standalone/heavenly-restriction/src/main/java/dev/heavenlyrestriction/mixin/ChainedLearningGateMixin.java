package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;

/** Reject before native unlock callbacks can spend EP or trigger advancements. */
@Mixin(SkillStorage.class)
public abstract class ChainedLearningGateMixin {
    @WrapMethod(method="learnSkill",remap=false)
    private boolean restriction$learn(ManasSkillInstance i,MutableComponent message,Operation<Boolean> original){
        var owner=((Skills)(Object)this).getOwner();if(!ChainedState.canLearn(owner,i)){ChainedState.notifyBlocked(owner,i);return false;}
        try(var scope=i.getParentSkill()!=null && ChainedState.inherited(owner,i.getParentSkill())?ChainedContext.reincarnation(owner,i.getSkill()):ChainedContext.ability(owner,i.getSkill())){boolean result=original.call(i,message);if(result && ChainedContext.inherited(owner))ChainedState.remember(owner,i.getSkill());return result;}
    }
    @WrapMethod(method="updateSkill",remap=false)
    private void restriction$update(ManasSkillInstance i,boolean sync,Operation<Void> original){
        var storage=(Skills)(Object)this;var owner=storage.getOwner();if(storage.getSkill(i.getSkillId()).isEmpty() && !ChainedState.canLearn(owner,i)){ChainedState.notifyBlocked(owner,i);return;}
        original.call(i,sync);if(!owner.level().isClientSide() && ChainedContext.inherited(owner))ChainedState.remember(owner,i.getSkill());
    }
}
