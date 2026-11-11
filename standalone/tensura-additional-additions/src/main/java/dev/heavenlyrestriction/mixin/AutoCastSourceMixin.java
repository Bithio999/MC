package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.splitmind.AutoCastCombat;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.TensuraSkill;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TensuraSkill.class, remap = false)
public abstract class AutoCastSourceMixin {
  @Inject(method = "createSource", at = @At("RETURN"))
  private void additions$source(
      ManasSkillInstance spell,
      LivingEntity owner,
      ResourceKey<DamageType> type,
      int mode,
      CallbackInfoReturnable<DamageSource> ci) {
    AutoCastCombat.created(ci.getReturnValue(), owner, spell, mode);
  }
}
