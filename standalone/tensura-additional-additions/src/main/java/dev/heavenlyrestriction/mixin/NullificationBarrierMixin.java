package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.Nullification;
import io.github.manasmods.tensura.entity.magic.barrier.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BarrierEntity.class,RangedBarrierEntity.class,BossBarrierEntity.class})
public abstract class NullificationBarrierMixin {
    @Inject(method="hurt",at=@At("HEAD"),cancellable=true)
    private void myths$shatter(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){
        var barrier=(BarrierEntity)(Object)this;
        if(!barrier.level().isClientSide() && amount>0 && Nullification.breakBarrier(barrier,source))result.setReturnValue(true);
    }
}
