package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.event.TensuraSkillEvents;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(TensuraSkill.class)
public abstract class ExponentiationLearningMixin {
    @WrapMethod(method="getDefaultLearningIncrement",remap=false)
    private double exponentiation$learningSource(ManasSkillInstance i,LivingEntity owner,int mode,Operation<Double> original){try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.LEARNING)){return original.call(i,owner,mode);}}
    @WrapMethod(method="getDefaultMasteryIncrement",remap=false)
    private double exponentiation$masterySource(ManasSkillInstance i,LivingEntity owner,int mode,Operation<Double> original){try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.LEARNING)){return original.call(i,owner,mode);}}
    @WrapOperation(method={"addLearnPoint(Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;Lnet/minecraft/world/entity/LivingEntity;ID)Z","learnMode(Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;Lnet/minecraft/world/entity/LivingEntity;IDZ)Z"},at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/event/TensuraSkillEvents$SkillLearningEvent;learn(Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;Lnet/minecraft/world/entity/LivingEntity;IDLio/github/manasmods/manascore/network/api/util/Changeable;)Ldev/architectury/event/EventResult;"),remap=false)
    private dev.architectury.event.EventResult exponentiation$learn(TensuraSkillEvents.SkillLearningEvent event,ManasSkillInstance i,LivingEntity owner,int mode,double requirement,Changeable<Double> proposed,Operation<dev.architectury.event.EventResult> original){double before=requirement==0?i.getMastery():i.getOrCreateTag().getDouble(((TensuraSkill)(Object)this).getModeId(i,mode));var result=original.call(event,i,owner,mode,requirement,proposed);if(!result.isFalse() && Exponentiation.active(owner) && proposed.get()>before)proposed.set(before+Exponentiation.square(proposed.get()-before));return result;}
    @WrapOperation(method="addMasteryPoint(Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;Lnet/minecraft/world/entity/LivingEntity;D)V",at=@At(value="INVOKE",target="Lio/github/manasmods/manascore/skill/api/SkillEvents$SkillMasteryEvent;master(Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/network/api/util/Changeable;)Ldev/architectury/event/EventResult;"),remap=false)
    private dev.architectury.event.EventResult exponentiation$master(SkillEvents.SkillMasteryEvent event,ManasSkillInstance i,LivingEntity owner,Changeable<Double> proposed,Operation<dev.architectury.event.EventResult> original){double before=i.getMastery();var result=original.call(event,i,owner,proposed);if(!result.isFalse() && Exponentiation.active(owner) && proposed.get()>before)proposed.set(before+Exponentiation.square(proposed.get()-before));return result;}
}
