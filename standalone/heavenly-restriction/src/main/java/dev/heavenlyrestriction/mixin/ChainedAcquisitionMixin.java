package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.architectury.event.EventResult;
import dev.heavenlyrestriction.ChainedState;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.handler.AbilityHandler;
import io.github.manasmods.tensura.world.TensuraGameRules;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Pinned 2.0.1.3 unlock callback: replace its permanent-MP acquisition bill, not upkeep. */
@Mixin(AbilityHandler.class)
public abstract class ChainedAcquisitionMixin {
    @WrapMethod(method="lambda$init$0",remap=false)
    private static EventResult restriction$auraAcquisition(ManasSkillInstance i,LivingEntity owner,Changeable<MutableComponent> message,Operation<EventResult> original){
        if(!(i.getSkill() instanceof ResistSkill skill) || !ChainedState.owned(owner) || i.getMastery()<0 || i.isTemporarySkill() || i.getTag()!=null && i.getTag().getBoolean("NoMagiculeCost"))return original.call(i,owner,message);
        double cost=Math.max(0,skill.getAcquiringMagiculeCost(i)*owner.level().getGameRules().getInt(TensuraGameRules.MP_SKILL_COST)/100d);
        var energy=TensuraStorages.getExistenceFrom(owner);
        if(!Double.isFinite(cost) || energy.getAura()<cost){if(owner instanceof net.minecraft.server.level.ServerPlayer)owner.sendSystemMessage(Component.literal("Chained requires "+cost+" aura to acquire "+skill.getName().getString()+"."));return EventResult.interruptFalse();}
        // Native reward handling skips its MP bill and clears this one-use flag itself.
        i.getOrCreateTag().putBoolean("NoMagiculeCost",true);var result=original.call(i,owner,message);
        if(!result.isFalse() && cost>0){energy.setAura(Math.max(0,energy.getAura()-cost));energy.markDirty();}
        return result;
    }
}
