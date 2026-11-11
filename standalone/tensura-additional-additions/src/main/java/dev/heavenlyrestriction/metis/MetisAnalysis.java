package dev.heavenlyrestriction.metis;

import dev.heavenlyrestriction.ChainedContext;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.*;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.ability.skill.unique.GreatSageSkill;
import io.github.manasmods.tensura.data.TensuraSkillTags;
import io.github.manasmods.tensura.entity.TensuraProjectile;
import io.github.manasmods.tensura.event.TensuraSkillEvents;
import io.github.manasmods.tensura.registry.attribute.*;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.registry.sound.TensuraSoundEvents;
import io.github.manasmods.tensura.util.*;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Guaranteed eligible analysis, retaining native copy events, acquisition rules and skill tiers.
 */
public final class MetisAnalysis {
  public static int level(LivingEntity owner, ManasSkillInstance i) {
    return i.isMastered(owner) ? MetisConfig.APPRAISAL_MASTERED.get() : MetisConfig.APPRAISAL.get();
  }

  public static int cooldown(LivingEntity owner, ManasSkillInstance i) {
    return i.isMastered(owner)
        ? MetisConfig.ANALYSIS_COOLDOWN_MASTERED.get()
        : MetisConfig.ANALYSIS_COOLDOWN.get();
  }

  public static void refreshAppraisal(ServerPlayer p, ManasSkillInstance i) {
    var attribute = p.getAttribute(TensuraAttributes.ANALYSIS_LEVEL);
    if (i.getMastery() < 0
        || !i.getOrCreateTag().getBoolean("Activated")
        || attribute == null
        || !attribute.hasModifier(TensuraGlobalAttributeIds.ANALYSIS)) return;
    AttributeHelper.addAnalysisAttributes(
        p,
        level(p, i),
        i.isMastered(p)
            ? GreatSageSkill.CONFIG.analysisRadiusMastered
            : GreatSageSkill.CONFIG.analysisRadius);
  }

  public static boolean copyable(ManasSkillInstance i) {
    if (i.getMastery() < 0 || i.isTemporarySkill() || i.is(TensuraSkillTags.NO_PLUNDERING))
      return false;
    return i.getSkill() instanceof ResistSkill
        || ((GreatSageSkill) UniqueSkills.GREAT_SAGE.get()).canCopy(i);
  }

  private static boolean known(ServerPlayer p, ManasSkill skill) {
    return SkillAPI.getSkillsFrom(p)
        .getSkill(skill)
        .filter(i -> i.getMastery() >= 0 && i.getRemoveTime() != -3)
        .isPresent();
  }

  public static List<ManasSkillInstance> candidates(ServerPlayer p, LivingEntity target) {
    return SkillAPI.getSkillsFrom(target).getLearnedSkills().stream()
        .filter(MetisAnalysis::copyable)
        .filter(i -> !known(p, i.getSkill()))
        .sorted(Comparator.comparing(i -> i.getSkillId().toString()))
        .toList();
  }

  public static boolean copyFrom(ServerPlayer p, ManasSkillInstance mind, LivingEntity target) {
    if (target == p
        || !target.isAlive()
        || p.distanceToSqr(target)
            > GreatSageSkill.CONFIG.copyRange * GreatSageSkill.CONFIG.copyRange
        || !p.hasLineOfSight(target)
        || target instanceof Player player && player.getAbilities().invulnerable) return false;
    var eligible = candidates(p, target);
    if (eligible.isEmpty()) return false;
    return learn(p, mind, target, eligible.get(p.getRandom().nextInt(eligible.size())).getSkill());
  }

  public static boolean copyProjectile(
      ServerPlayer p, ManasSkillInstance mind, TensuraProjectile projectile) {
    var spell = projectile.getSkill();
    return projectile.isAlive()
        && spell != null
        && spell.getMastery() >= 0
        && spell.is(TensuraSkillTags.COPIABLE_MAGIC)
        && !known(p, spell.getSkill())
        && p.distanceToSqr(projectile)
            <= GreatSageSkill.CONFIG.copyRangeMagic * GreatSageSkill.CONFIG.copyRangeMagic
        && p.hasLineOfSight(projectile)
        && learn(p, mind, projectile.getOwner(), spell.getSkill());
  }

  private static boolean learn(
      ServerPlayer p,
      ManasSkillInstance mind,
      net.minecraft.world.entity.Entity source,
      ManasSkill proposed) {
    if (mind.getMastery() < 0
        || !mind.canInteractSkill(p)
        || !mind.canActivateSkill(p, 3)
        || mind.onCoolDown(3) && !mind.canIgnoreCoolDown(p, 3)) return false;
    var skill = Changeable.of(proposed);
    if (TensuraSkillEvents.SKILL_PLUNDER.invoker().plunder(source, p, false, skill).isFalse()
        || skill.get() == null) return false;
    try (var provenance = ChainedContext.ability(p, mind.getSkill())) {
      if (!SkillHelper.learnSkill(p, skill.get(), mind.getRemoveTime())) return false;
    }
    mind.addMasteryPoint(p);
    mind.setCoolDown(cooldown(p, mind), 3);
    mind.markDirty();
    p.displayClientMessage(
        Component.translatable("heavenlyrestriction.metis.analyzed", skill.get().getName()), true);
    return true;
  }

  public static void press(ServerPlayer p, ManasSkillInstance mind) {
    if (mind.onCoolDown(3) && !mind.canIgnoreCoolDown(p, 3)) return;
    var projectile =
        ObjectSelectionHelper.getTargetingEntity(
            TensuraProjectile.class,
            p,
            GreatSageSkill.CONFIG.copyRangeMagic,
            .5,
            true,
            true,
            false);
    boolean success =
        projectile != null && projectile.isAlive()
            ? copyProjectile(p, mind, projectile)
            : Optional.ofNullable(
                    ObjectSelectionHelper.getTargetingEntity(
                        p, GreatSageSkill.CONFIG.copyRange, false))
                .map(t -> copyFrom(p, mind, t))
                .orElse(false);
    if (!success) {
      mind.setCoolDown(1, 3);
      p.displayClientMessage(
          Component.translatable("heavenlyrestriction.metis.analysis_unavailable"), true);
    }
    p.swing(InteractionHand.MAIN_HAND, true);
    p.playNotifySound(
        (SoundEvent)
            (success ? TensuraSoundEvents.GENERIC_CAST : TensuraSoundEvents.GENERIC_CAST_FAIL)
                .get(),
        TensuraSkill.ABILITY_SOUND,
        1,
        1);
  }

  private MetisAnalysis() {}
}
