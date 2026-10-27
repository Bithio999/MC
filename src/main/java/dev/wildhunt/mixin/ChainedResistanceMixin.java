package dev.wildhunt.mixin;

import dev.wildhunt.*;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Invoker;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.effect.template.TensuraMobEffectInstance;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import java.util.List;
import net.minecraft.world.damagesource.DamageSource;

/** Native nullification already falls back to resistance at bypass level one. */
@Mixin(ResistSkill.class)
public abstract class ChainedResistanceMixin implements ChainedResistanceView {
    @Invoker(value="getNullificationForm",remap=false) public abstract ManasSkill myths$nullificationForm();
    @Invoker(value="getImmuneEffects",remap=false) public abstract List<Holder<MobEffect>> myths$immuneEffects(ManasSkillInstance i,LivingEntity owner);
    @ModifyVariable(method="addLearnPoint",at=@At("HEAD"),argsOnly=true,remap=false)
    private double myths$resistanceLearning(double point,io.github.manasmods.manascore.skill.api.ManasSkillInstance i,net.minecraft.world.entity.LivingEntity owner,int mode,double original){return point>0 && ChainedState.active(owner)?point+19:point;}
    @Inject(method="isResistanceBypass",at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$oneTier(DamageSource source,CallbackInfoReturnable<Boolean> out){if(ChainedState.mastered(GodlyPassives.attacker(source)))out.setReturnValue(true);}
    @Inject(method="onEffectAdded",at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$statusTier(ManasSkillInstance i,LivingEntity owner,Entity caster,Changeable<MobEffectInstance> effect,CallbackInfoReturnable<Boolean> out){
        if(effect.isEmpty() || !i.isToggled())return;
        if(!(caster instanceof LivingEntity)){
            var id=((TensuraMobEffectInstance)effect.get()).tensura$getSource();if(id!=null && owner.level() instanceof net.minecraft.server.level.ServerLevel level)caster=level.getEntity(id);
        }
        if(!(caster instanceof LivingEntity attacker) || !ChainedState.mastered(attacker))return;
        var self=(ResistSkill)(Object)this;
        if(self.getResistType()==ResistSkill.ResistType.RESISTANCE){out.setReturnValue(true);return;}
        for(var skill:SkillAPI.getSkillRegistry())if(skill instanceof ResistSkill resistance && resistance.getResistType()==ResistSkill.ResistType.RESISTANCE && ((ChainedResistanceView)resistance).myths$nullificationForm()==self){
            out.setReturnValue(!((ChainedResistanceView)resistance).myths$immuneEffects(resistance.createDefaultInstance(),owner).contains(effect.get().getEffect()));return;
        }
        out.setReturnValue(true);
    }
}
