package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** After Tensura merges its DamageSource extension, apply the live owner trait to every source. */
@Mixin(value=DamageSource.class,priority=900)
public abstract class ChainedDodgeMixin {
    @Dynamic("Added by Tensura's MixinDamageSource")
    @Inject(method="tensura$isDodgeBypass",at=@At("RETURN"),cancellable=true,remap=false)
    private void restriction$ignoreDodge(CallbackInfoReturnable<Boolean> out){if(ChainedState.mastered(RestrictionData.attacker((DamageSource)(Object)this)) || dev.heavenlyrestriction.critical.CriticalMerchant.tycheUndodge(RestrictionData.attacker((DamageSource)(Object)this),(DamageSource)(Object)this))out.setReturnValue(true);}
    @Dynamic("Added by Tensura's MixinDamageSource")
    @Inject(method="tensura$getResistanceBypassLevel",at=@At("RETURN"),cancellable=true,remap=false)
    private void restriction$degradeSource(CallbackInfoReturnable<Float> out){if(ChainedState.mastered(RestrictionData.attacker((DamageSource)(Object)this)))out.setReturnValue(Math.max(1,out.getReturnValue()));}
}
