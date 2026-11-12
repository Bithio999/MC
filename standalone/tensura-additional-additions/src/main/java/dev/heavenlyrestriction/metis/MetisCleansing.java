package dev.heavenlyrestriction.metis;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.skill.extra.LawManipulationSkill;
import io.github.manasmods.tensura.ability.skill.unique.CookSkill;
import io.github.manasmods.tensura.data.TensuraTags;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Read-only, mode-specific native cleansing rules. Never invokes an ability to probe it. */
public final class MetisCleansing {
  private enum Rule {
    NONE,
    ANTIDOTE,
    HEALER,
    CHEF,
    INVIGORATE,
    ISOLATION,
    LAW,
    SPATIAL,
    REVERSE
  }

  private static Rule rule(ManasSkillInstance i, int mode) {
    return switch (i.getSkillId().toString()) {
      case "tensura:antidote" -> mode == 0 ? Rule.ANTIDOTE : Rule.NONE;
      case "tensura:healer" -> mode == 0 ? Rule.HEALER : Rule.NONE;
      case "tensura:chef" -> mode == 0 ? Rule.CHEF : Rule.NONE;
      case "tensura:lust" -> mode == 1 ? Rule.INVIGORATE : Rule.NONE;
      case "tensura:predator" -> mode == 4 ? Rule.ISOLATION : Rule.NONE;
      case "tensura:gluttony" -> mode == 3 ? Rule.ISOLATION : Rule.NONE;
      case "tensura:law_manipulation" -> mode == 0 ? Rule.LAW : Rule.NONE;
      case "tensura:spatial_domination" -> mode == 1 ? Rule.SPATIAL : Rule.NONE;
      case "tensura:reverser" -> mode == 1 ? Rule.REVERSE : Rule.NONE;
      default -> Rule.NONE;
    };
  }

  public static boolean known(ManasSkillInstance i, int mode) {
    return rule(i, mode) != Rule.NONE;
  }

  public static MetisMagic.Role role(ManasSkillInstance i, int mode) {
    return switch (rule(i, mode)) {
      case HEALER, CHEF, INVIGORATE -> MetisMagic.Role.HEALING;
      default -> MetisMagic.Role.CLEANSING;
    };
  }

  public static boolean needed(ServerPlayer p, ManasSkillInstance i, int mode) {
    return switch (rule(i, mode)) {
      case ANTIDOTE ->
          p.hasEffect(MobEffects.CONFUSION)
              || p.hasEffect(MobEffects.POISON)
              || p.hasEffect(MobEffects.WEAKNESS)
              || i.isMastered(p)
                  && p.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.FATAL_POISON));
      case HEALER ->
          lowHealth(p)
              || i.isMastered(p)
                  && TensuraStorages.getExistenceFrom(p).getSpiritualHealth()
                      < p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH) * .9
              || p.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.INFECTION));
      case CHEF ->
          lowHealth(p)
              || severed(p)
              || removable(
                  p, i, mode, h -> harmful(h) && !h.is(TensuraTags.MobEffects.SKILL_EFFECT), true);
      case INVIGORATE ->
          lowHealth(p)
              || removable(
                  p, i, mode, h -> harmful(h) && !h.is(TensuraTags.MobEffects.SKILL_EFFECT), true);
      // Native Isolation edits the held item instead of cleansing when the hand is occupied.
      case ISOLATION ->
          p.getMainHandItem().isEmpty()
              && (severed(p) || removable(p, i, mode, MetisCleansing::harmful, true));
      case LAW ->
          severed(p)
              || p.getAttribute(Attributes.MAX_HEALTH) != null
                  && p.getAttribute(Attributes.MAX_HEALTH).hasModifier(CookSkill.COOK)
              || removable(p, i, mode, LawManipulationSkill.getCleansingEffect(), false);
      case SPATIAL -> severed(p);
      // Mastered Buff Reverse can transform held potions/engravings before touching the
      // player.
      case REVERSE ->
          (!i.isMastered(p) || p.getMainHandItem().isEmpty())
              && removable(
                  p,
                  i,
                  mode,
                  h ->
                      harmful(h)
                          && !h.is(TensuraTags.MobEffects.SKILL_DEBUFF)
                          && !h.equals(
                              TensuraMobEffects.getReference(TensuraMobEffects.MAGICULE_POISON)),
                  false);
      case NONE -> false;
    };
  }

  private static boolean lowHealth(ServerPlayer p) {
    return p.getHealth() < p.getMaxHealth() * .9;
  }

  private static boolean severed(ServerPlayer p) {
    return TensuraStorages.getEffectFrom(p).getSeveranceAmount() > 0;
  }

  private static boolean harmful(Holder<MobEffect> h) {
    return h.value().getCategory() == MobEffectCategory.HARMFUL;
  }

  private static boolean removable(
      ServerPlayer p,
      ManasSkillInstance i,
      int mode,
      Predicate<Holder<MobEffect>> predicate,
      boolean perLevelCost) {
    double mp = TensuraStorages.getExistenceFrom(p).getMagicule();
    double cost = perLevelCost ? ((TensuraSkill) i.getSkill()).getMagiculeCost(p, i, mode) : 0;
    return p.getActiveEffects().stream()
        .anyMatch(
            e ->
                predicate.test(e.getEffect())
                    && (p.hasInfiniteMaterials()
                        || mp
                            >= dev.heavenlyrestriction.SchemaArt.cost(
                                p, cost * (e.getAmplifier() + 1.0))));
  }

  private MetisCleansing() {}
}
