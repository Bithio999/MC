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
  @Inject(method = "addHeldAttributeModifiers", at = @At("HEAD"), cancellable = true, remap = false)
  private void additions$backgroundChant(
      ManasSkillInstance instance, LivingEntity owner, int mode, CallbackInfo ci) {
    if (SplitMindCasting.automatic(owner, instance)) ci.cancel();
  }
}
