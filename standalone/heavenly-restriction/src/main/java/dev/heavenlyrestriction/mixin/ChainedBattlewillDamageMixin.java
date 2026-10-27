package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.ChainedState;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TensuraDamageHelper.class)
public abstract class ChainedBattlewillDamageMixin {
    @Inject(method="isBattlewill",at=@At("HEAD"),cancellable=true,remap=false)
    private static void restriction$battlewill(DamageSource source,Entity owner,CallbackInfoReturnable<Boolean> out){if(ChainedState.isDash(source))out.setReturnValue(true);}
    @Inject(method="isPhysicalOrBattlewill",at=@At("HEAD"),cancellable=true,remap=false)
    private static void restriction$physicalBattlewill(DamageSource source,LivingEntity owner,CallbackInfoReturnable<Boolean> out){if(ChainedState.isDash(source))out.setReturnValue(true);}
}
