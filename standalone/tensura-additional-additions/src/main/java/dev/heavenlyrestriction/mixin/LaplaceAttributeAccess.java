package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.laplace.expand.AttributeParameters;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ManasSkill.class, remap = false)
public interface LaplaceAttributeAccess extends AttributeParameters {
  @Accessor("attributeModifiers")
  Map<?, ?> additions$attributeTemplates();
}
