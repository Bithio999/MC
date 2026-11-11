package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stop automatic cast gestures before they change the arm state or broadcast animation packets. */
@Mixin(LivingEntity.class)
public abstract class AutoCastSwingMixin {
  @Inject(
      method = "swing(Lnet/minecraft/world/InteractionHand;Z)V",
      at = @At("HEAD"),
      cancellable = true)
  private void additions$backgroundSwing(InteractionHand hand, boolean broadcast, CallbackInfo ci) {
    if (SplitMindCasting.automatic((LivingEntity) (Object) this)) ci.cancel();
  }
}
