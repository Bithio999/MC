package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.laplace.LaplaceRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
    targets = {
      "io.github.manasmods.tensura.ability.magic.aspectual.enhancement.StrengthMagic",
      "io.github.manasmods.tensura.ability.magic.aspectual.enhancement.ProtectionMagic",
      "io.github.manasmods.tensura.ability.magic.aspectual.enhancement.ReinforcementMagic"
    },
    remap = false)
public abstract class LaplaceDurationMixin {
  @org.spongepowered.asm.mixin.injection.ModifyArgs(
      method = "onRelease",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/effect/MobEffectInstance;<init>(Lnet/minecraft/core/Holder;IIZZZ)V",
              remap = true))
  private void laplace$duration(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
    args.set(1, LaplaceRuntime.duration(args.get(1)));
  }
}
