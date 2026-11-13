package dev.heavenlyrestriction.laplace;

import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.manascore.attribute.api.ManasCoreAttributes;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.extra.ThoughtAccelerationSkill;
import io.github.manasmods.tensura.config.ability.skill.UniqueSkillConfig;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class LaplaceIntellect {
  public static final net.minecraft.resources.ResourceLocation ID =
      HeavenlyRestriction.id("laplace");

  private static List<Holder<Attribute>> attributes() {
    return List.of(
        Attributes.MOVEMENT_SPEED,
        Attributes.ATTACK_SPEED,
        TensuraAttributes.CHANT_SPEED,
        TensuraAttributes.RESISTANCE_DEGRADATION,
        TensuraAttributes.AUTO_MELEE_DODGE_CHANCE,
        TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE,
        TensuraAttributes.DODGE_NEGATE_CHANCE,
        ManasCoreAttributes.CRITICAL_ATTACK_CHANCE,
        TensuraAttributes.ABILITY_LEARNING_GAIN,
        TensuraAttributes.ABILITY_MASTERY_GAIN,
        TensuraAttributes.ANALYSIS_LEVEL,
        TensuraAttributes.ANALYSIS_DISTANCE);
  }

  public static void remove(LivingEntity e) {
    if (!e.level().isClientSide())
      for (var a : attributes()) {
        var attr = e.getAttribute(a);
        if (attr != null) attr.removeModifier(ID);
      }
  }

  private static void add(LivingEntity e, Holder<Attribute> a, double v) {
    var attr = e.getAttribute(a);
    if (attr != null && (attr.getModifier(ID) == null || attr.getModifier(ID).amount() != v))
      attr.addOrReplacePermanentModifier(
          new AttributeModifier(ID, v, AttributeModifier.Operation.ADD_VALUE));
  }

  // Native appraisal/chant utilities choose their strongest provider rather than adding copies.
  private static void maximum(LivingEntity e, Holder<Attribute> a, double amount) {
    var attr = e.getAttribute(a);
    if (attr == null) return;
    double others =
        attr.getModifiers().stream()
            .filter(
                m -> !m.id().equals(ID) && m.operation() == AttributeModifier.Operation.ADD_VALUE)
            .mapToDouble(m -> Math.max(0, m.amount()))
            .sum();
    add(e, a, Math.max(0, amount - others));
  }

  public static void refresh(LivingEntity e) {
    if (e.level().isClientSide()) return;
    var own = Laplace.active(e);
    if (own.isEmpty()
        || SkillAPI.getSkillsFrom(e)
            .getSkill(UniqueSkills.MATHEMATICIAN.get())
            .filter(i -> i.getMastery() >= 0 && i.isToggled() && i.canInteractSkill(e))
            .isPresent()) {
      remove(e);
      return;
    }
    var i = own.get();
    var c = ConfigRegistry.getConfig(UniqueSkillConfig.class).Mathematician;
    ThoughtAccelerationSkill.onToggle(i, e, ID, true);
    maximum(e, TensuraAttributes.CHANT_SPEED, c.chantSpeed);
    add(e, TensuraAttributes.RESISTANCE_DEGRADATION, 1);
    add(e, TensuraAttributes.AUTO_MELEE_DODGE_CHANCE, c.meleeDodge);
    add(e, TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE, c.projectileDodge);
    add(e, TensuraAttributes.DODGE_NEGATE_CHANCE, c.dodgeNegation);
    add(e, ManasCoreAttributes.CRITICAL_ATTACK_CHANCE, c.critChance);
    add(e, TensuraAttributes.ABILITY_LEARNING_GAIN, c.learningPoint);
    add(e, TensuraAttributes.ABILITY_MASTERY_GAIN, c.masteryPoint);
    maximum(
        e,
        TensuraAttributes.ANALYSIS_LEVEL,
        i.isMastered(e) ? c.analysisLevelMastered : c.analysisLevel);
    maximum(
        e,
        TensuraAttributes.ANALYSIS_DISTANCE,
        i.isMastered(e) ? c.analysisRadiusMastered : c.analysisRadius);
  }

  public static void tick(EntityTickEvent.Post event) {
    if (event.getEntity() instanceof LivingEntity e
        && !e.level().isClientSide()
        && e.tickCount % 20 == 0) {
      if (e instanceof ServerPlayer p) LaplaceSynthesis.discover(p);
      refresh(e);
    }
  }

  private LaplaceIntellect() {}
}
