package dev.wildhunt.mixin;

import dev.wildhunt.ChainedState;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TensuraDamageHelper.class)
public abstract class ChainedBattlewillDamageMixin {
    // Keep Severance Rush purely Physical, even with native Aura Sword/Ogre Guillotine buffs.
    @Inject(method="isBattlewill",at=@At("HEAD"),cancellable=true,remap=false)
    private static void myths$battlewill(DamageSource source,Entity owner,CallbackInfoReturnable<Boolean> out){if(ChainedState.isDash(source))out.setReturnValue(false);}

    /** Native direct SHP attacks skip LivingIncomingDamageEvent and the normal skill damage callbacks. */
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="directSpiritualHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;FF)Z",remap=false)
    private static boolean myths$spiritual(net.minecraft.world.entity.LivingEntity target,Entity attacker,DamageSource source,float amount,float resistance,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
        if(target.level().isClientSide() || TensuraDamageHelper.hasSpiritualDamageImmunity(target.level(),target,attacker))return original.call(target,attacker,source,amount,resistance);
        DamageSource actual=source==null?io.github.manasmods.tensura.damage.TensuraDamageTypes.getEntityDamageSource(target.level(),io.github.manasmods.tensura.damage.TensuraDamageTypes.SOUL_SCATTER,attacker):source;
        if(dev.wildhunt.UnboundCounter.intercept(target,actual,amount))return false;
        var damage=io.github.manasmods.manascore.network.api.util.Changeable.of(amount);
        dev.wildhunt.UnboundCritical.apply(attacker instanceof net.minecraft.world.entity.LivingEntity living?living:null,target,actual,damage);
        return original.call(target,attacker,actual,damage.get(),resistance);
    }
}
