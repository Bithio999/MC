package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Magic;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Background chanting must not apply the player's casting slow/range/FOV modifiers. */
@Mixin(Magic.class)
public abstract class SplitMindCastingModifiersMixin {
  /** Native Magic pays directly, so discount its bill before its affordability check. */
  @ModifyVariable(
      method =
          "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;DD)Z",
      at = @At("HEAD"),
      argsOnly = true,
      index = 5,
      remap = false)
  private double additions$masteredCost(
      double amount, LivingEntity owner, ManasSkillInstance spell, double aura, double magicules) {
    return SplitMindCasting.automatic(owner, spell)
        ? amount * SplitMindCasting.magiculeFactor(owner)
        : amount;
  }

  @Inject(method = "addHeldAttributeModifiers", at = @At("HEAD"), cancellable = true, remap = false)
  private void additions$backgroundChant(
      ManasSkillInstance instance, LivingEntity owner, int mode, CallbackInfo ci) {
    if (SplitMindCasting.automatic(owner, instance)) ci.cancel();
  }
}
