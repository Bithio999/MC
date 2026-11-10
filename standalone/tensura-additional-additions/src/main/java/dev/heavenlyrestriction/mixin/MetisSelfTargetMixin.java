package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Self-directed background healing/support must never select an enemy behind the camera. */
@Mixin(value = ObjectSelectionHelper.class, remap = false)
public abstract class MetisSelfTargetMixin {
  @Inject(
      method =
          "getTargetingEntity(Lnet/minecraft/world/entity/LivingEntity;DZ)Lnet/minecraft/world/entity/LivingEntity;",
      at = @At("HEAD"),
      cancellable = true)
  private static void metis$self(
      LivingEntity owner,
      double range,
      boolean throughBlocks,
      CallbackInfoReturnable<LivingEntity> ci) {
    if (SplitMindCasting.selfTarget(owner)) ci.setReturnValue(owner);
  }
}
