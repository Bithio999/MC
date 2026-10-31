package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.ChainedContext;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Covers nested grants and Battlewill callbacks with exception-safe provenance scopes. */
@Mixin(ManasSkillInstance.class)
public abstract class ChainedAbilityContextMixin {
    @WrapMethod(method={"onLearnSkill","onSkillMastered","onTick","onToggleOn","onToggleOff","onForgetSkill"},remap=false)
    private void restriction$callback(LivingEntity owner,Operation<Void> original){try(var scope=ChainedContext.ability(owner,((ManasSkillInstance)(Object)this).getSkill())){original.call(owner);}}
    @WrapMethod(method="onPressed",remap=false)
    private void restriction$pressed(LivingEntity owner,int slot,int mode,Operation<Void> original){try(var scope=ChainedContext.ability(owner,((ManasSkillInstance)(Object)this).getSkill())){original.call(owner,slot,mode);}}
    @WrapMethod(method="onHeld",remap=false)
    private boolean restriction$held(LivingEntity owner,int ticks,int mode,Operation<Boolean> original){try(var scope=ChainedContext.ability(owner,((ManasSkillInstance)(Object)this).getSkill())){return original.call(owner,ticks,mode);}}
    @WrapMethod(method="onRelease",remap=false)
    private void restriction$released(LivingEntity owner,int ticks,int slot,int mode,Operation<Void> original){try(var scope=ChainedContext.ability(owner,((ManasSkillInstance)(Object)this).getSkill())){original.call(owner,ticks,slot,mode);}}
    @WrapMethod(method={"onDamageEntity","onTouchEntity"},remap=false)
    private boolean restriction$attack(LivingEntity owner,LivingEntity target,net.minecraft.world.damagesource.DamageSource source,io.github.manasmods.manascore.network.api.util.Changeable<Float> amount,Operation<Boolean> original){try(var scope=ChainedContext.ability(owner,((ManasSkillInstance)(Object)this).getSkill())){return original.call(owner,target,source,amount);}}
    @WrapMethod(method="onTakenDamage",remap=false)
    private boolean restriction$damage(LivingEntity owner,net.minecraft.world.damagesource.DamageSource source,io.github.manasmods.manascore.network.api.util.Changeable<Float> amount,Operation<Boolean> original){try(var scope=ChainedContext.ability(owner,((ManasSkillInstance)(Object)this).getSkill())){return original.call(owner,source,amount);}}
    @WrapMethod(method="onDeath",remap=false)
    private boolean restriction$death(LivingEntity owner,net.minecraft.world.damagesource.DamageSource source,Operation<Boolean> original){try(var scope=ChainedContext.ability(owner,((ManasSkillInstance)(Object)this).getSkill())){return original.call(owner,source);}}
}
