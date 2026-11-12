package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.tyche.TycheSynthesis;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.unique.TunerSkill;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TunerSkill.class)
public abstract class TunerProgressMixin {
  @Inject(method = "onDeath", at = @At("RETURN"), remap = false)
  private void tyche$survived(
      ManasSkillInstance i,
      LivingEntity e,
      DamageSource source,
      CallbackInfoReturnable<Boolean> out) {
    if (!out.getReturnValue() && !i.isTemporarySkill())
      TycheSynthesis.survived(e, source.getMsgId());
  }
}
