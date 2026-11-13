package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.heavenlyrestriction.laplace.expand.*;
import io.github.manasmods.manascore.skill.api.*;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ManasSkillInstance.class, remap = false)
public abstract class LaplaceAttributePowerMixin {
  @ModifyReturnValue(method = "getAttributeModifierAmplifier", at = @At("RETURN"))
  private double laplace$power(
      double value,
      LivingEntity owner,
      Holder<Attribute> attribute,
      ManasSkill.AttributeTemplate template,
      int mode) {
    var i = (ManasSkillInstance) (Object) this;
    if (!ExpandedActions.attributeEffects(i)) return value;
    return value
        * AddonParameters.value(owner, i.getSkillId().toString(), -1, "attribute_power", 1);
  }
}
