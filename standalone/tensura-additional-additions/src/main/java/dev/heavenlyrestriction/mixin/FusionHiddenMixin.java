package dev.heavenlyrestriction.mixin;
import dev.heavenlyrestriction.fusion.Fusion;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class FusionHiddenMixin {
    @Inject(method="isPickable",at=@At("HEAD"),cancellable=true)
    private void fusion$embeddedBody(CallbackInfoReturnable<Boolean> result){if(Fusion.hidden((LivingEntity)(Object)this))result.setReturnValue(false);}
}
