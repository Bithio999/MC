package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.splitmind.AutoCastCombat;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TensuraDamageHelper.class, remap = false)
public abstract class AutoCastSourceCopyMixin {
  @Inject(method = "copy", at = @At("RETURN"))
  private static void additions$copy(
      DamageSource original, CallbackInfoReturnable<DamageSource> ci) {
    AutoCastCombat.copy(original, ci.getReturnValue());
  }
}
