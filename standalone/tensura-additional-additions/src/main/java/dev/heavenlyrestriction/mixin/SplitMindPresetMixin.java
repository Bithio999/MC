package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.storage.ability.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Only the internal, owned auto-cast invocation has a virtual slot; client slot checks stay native.
 */
@Mixin(AbilityStorage.class)
public abstract class SplitMindPresetMixin {
  @Inject(
      method = "isAbilityInActivePreset(ILio/github/manasmods/manascore/skill/api/ManasSkill;I)Z",
      at = @At("HEAD"),
      cancellable = true,
      remap = false)
  private void additions$virtualSlot(
      int key, ManasSkill skill, int mode, CallbackInfoReturnable<Boolean> cir) {
    if (SplitMindCasting.virtualSlot((IAbility) (Object) this, key, skill, mode))
      cir.setReturnValue(true);
  }

  @Inject(
      method = "isAbilityInActivePreset(Lio/github/manasmods/manascore/skill/api/ManasSkill;)Z",
      at = @At("HEAD"),
      cancellable = true,
      remap = false)
  private void additions$virtualHeld(ManasSkill skill, CallbackInfoReturnable<Boolean> cir) {
    if (SplitMindCasting.virtualSlot((IAbility) (Object) this, 3, skill, 0))
      cir.setReturnValue(true);
  }

  @Inject(
      method = "isAbilityInActivePreset(Lio/github/manasmods/manascore/skill/api/ManasSkill;I)Z",
      at = @At("HEAD"),
      cancellable = true,
      remap = false)
  private void additions$virtualMode(
      ManasSkill skill, int mode, CallbackInfoReturnable<Boolean> cir) {
    if (SplitMindCasting.virtualSlot((IAbility) (Object) this, 3, skill, mode))
      cir.setReturnValue(true);
  }
}
