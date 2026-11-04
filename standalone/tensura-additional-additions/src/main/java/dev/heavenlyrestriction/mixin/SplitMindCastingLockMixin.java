package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.ability.magic.aspectual.earth.MudSpearsMagic;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Ignore only the other-chant lock inside an owned auto-cast; anti-magic checks stay native. */
@Mixin({Magic.class, MudSpearsMagic.class})
public abstract class SplitMindCastingLockMixin {
  @WrapOperation(
      method = "isCastingBlocked",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getModifiers()Ljava/util/Set;",
              remap = true),
      remap = false)
  private Set<AttributeModifier> additions$parallelChant(
      AttributeInstance attribute,
      Operation<Set<AttributeModifier>> original,
      ManasSkillInstance instance,
      LivingEntity owner) {
    return SplitMindCasting.automatic(owner, instance) ? Set.of() : original.call(attribute);
  }
}
