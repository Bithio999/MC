package dev.wildhunt.mixin;

import dev.wildhunt.myths.DuoSystem;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forward native inherited racial reactions without replacing the primary body/storage. */
@Mixin(ManasRaceInstance.class)
public abstract class DuoRaceMixin {
    @Inject(method="onHurt",at=@At("RETURN"),cancellable=true,remap=false)
    private void myths$hurt(LivingEntity owner,DamageSource source,Changeable<Float> amount,CallbackInfoReturnable<Boolean> cir) {
        if(owner.level().isClientSide() || !cir.getReturnValue() || DuoSystem.primary(owner).orElse(null)!=(Object)this)return;
        for(var instance:DuoSystem.inherited(owner)) {
            boolean proceed=instance.getRace().onHurt(instance,owner,source,amount); DuoSystem.saveInherited(owner,instance);
            if(!proceed) {cir.setReturnValue(false);break;}
        }
    }
    @Inject(method="onEffectAdded",at=@At("RETURN"),cancellable=true,remap=false)
    private void myths$effect(LivingEntity owner,Entity source,Changeable<MobEffectInstance> effect,CallbackInfoReturnable<Boolean> cir) {
        if(owner.level().isClientSide() || !cir.getReturnValue() || DuoSystem.primary(owner).orElse(null)!=(Object)this)return;
        for(var instance:DuoSystem.inherited(owner)) {
            boolean proceed=instance.getRace().onEffectAdded(instance,owner,source,effect); DuoSystem.saveInherited(owner,instance);
            if(!proceed) {cir.setReturnValue(false);break;}
        }
    }
    @Inject(method="onAttackEntity",at=@At("RETURN"),cancellable=true,remap=false)
    private void myths$attack(LivingEntity owner,LivingEntity target,DamageSource source,Changeable<Float> amount,CallbackInfoReturnable<Boolean> cir) {
        if(owner.level().isClientSide() || !cir.getReturnValue() || DuoSystem.primary(owner).orElse(null)!=(Object)this)return;
        for(var instance:DuoSystem.inherited(owner)) {
            boolean proceed=instance.getRace().onAttackEntity(instance,owner,target,source,amount); DuoSystem.saveInherited(owner,instance);
            if(!proceed) {cir.setReturnValue(false);break;}
        }
    }
}
