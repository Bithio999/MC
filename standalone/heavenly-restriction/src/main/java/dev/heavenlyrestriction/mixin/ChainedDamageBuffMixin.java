package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.heavenlyrestriction.ChainedBuffs;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.effect.template.DamageAction;
import io.github.manasmods.tensura.handler.DamagingHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DamagingHandler.class)
public abstract class ChainedDamageBuffMixin {
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/effect/template/DamageAction;onDamagingEntity(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;Lio/github/manasmods/manascore/network/api/util/Changeable;)Z"),remap=false)
    private static boolean restriction$offensiveBuff(DamageAction action,LivingEntity owner,LivingEntity target,DamageSource source,Changeable<Float> amount,Operation<Boolean> original){
        float before=amount.get();boolean result=original.call(action,owner,target,source,amount);if(ChainedBuffs.damageBuff(owner,action))amount.set(Math.max(0,before+(amount.get()-before)*3));return result;
    }
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/effect/template/DamageAction;onBeingDamaged(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;Lio/github/manasmods/manascore/network/api/util/Changeable;)Z"),remap=false)
    private static boolean restriction$defensiveBuff(DamageAction action,LivingEntity owner,DamageSource source,Changeable<Float> amount,Operation<Boolean> original){
        float before=amount.get();boolean result=original.call(action,owner,source,amount);if(ChainedBuffs.damageBuff(owner,action))amount.set(Math.max(0,before+(amount.get()-before)*3));return result;
    }
}
