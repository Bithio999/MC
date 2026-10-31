package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class ChainedEffectMixin {
    @WrapMethod(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean restriction$buffSource(MobEffectInstance effect,Entity source,Operation<Boolean> original){ChainedBuffs.mark((LivingEntity)(Object)this,effect);return original.call(effect,source);}
    @WrapMethod(method="onEffectAdded")
    private void restriction$buffApplied(MobEffectInstance effect,Entity source,Operation<Void> original){
        var skill=ChainedBuffs.source(effect);if(skill==null){original.call(effect,source);return;}
        try(var scope=ChainedContext.ability((LivingEntity)(Object)this,skill)){original.call(effect,source);}
    }
    @WrapMethod(method="onEffectUpdated")
    private void restriction$buffUpdated(MobEffectInstance effect,boolean reapply,Entity source,Operation<Void> original){
        var skill=ChainedBuffs.source(effect);if(skill==null){original.call(effect,reapply,source);return;}
        try(var scope=ChainedContext.ability((LivingEntity)(Object)this,skill)){original.call(effect,reapply,source);}
    }
}
