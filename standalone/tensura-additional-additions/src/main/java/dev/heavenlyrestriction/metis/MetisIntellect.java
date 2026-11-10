package dev.heavenlyrestriction.metis;

import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.skill.extra.ThoughtAccelerationSkill;
import io.github.manasmods.tensura.ability.skill.unique.GreatSageSkill;
import io.github.manasmods.tensura.data.TensuraSkillTags;
import io.github.manasmods.tensura.entity.magic.MagicCircle;
import io.github.manasmods.tensura.event.TensuraSkillEvents;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

public final class MetisIntellect {
  public static final ResourceLocation BONUS = HeavenlyRestriction.id("metis");
  private static final Map<ServerPlayer, Map<ResourceLocation, Long>> RETRY = new WeakHashMap<>();

  public static void refresh(LivingEntity e) {
    if (e.level().isClientSide()) return;
    var mind = MetisMatrix.owned(e).filter(i -> i.isToggled() && i.canInteractSkill(e));
    if (mind.isEmpty()) {
      remove(e);
      return;
    }
    var i = mind.get();
    ThoughtAccelerationSkill.onToggle(i, e, BONUS, true);
    modifier(e, TensuraAttributes.ABILITY_LEARNING_GAIN, 19);
    modifier(e, TensuraAttributes.ABILITY_MASTERY_GAIN, 19);
    modifier(e, TensuraAttributes.CHANT_SPEED, GreatSageSkill.CONFIG.chantSpeed);
  }

  private static void modifier(LivingEntity e, Holder<Attribute> a, double amount) {
    var attr = e.getAttribute(a);
    if (attr != null
        && (attr.getModifier(BONUS) == null || attr.getModifier(BONUS).amount() != amount))
      attr.addOrReplacePermanentModifier(
          new AttributeModifier(BONUS, amount, AttributeModifier.Operation.ADD_VALUE));
  }

  public static void remove(LivingEntity e) {
    if (e.level().isClientSide()) return;
    for (var a :
        List.of(
            Attributes.MOVEMENT_SPEED,
            Attributes.ATTACK_SPEED,
            TensuraAttributes.CHANT_SPEED,
            TensuraAttributes.ABILITY_LEARNING_GAIN,
            TensuraAttributes.ABILITY_MASTERY_GAIN)) {
      var attr = e.getAttribute(a);
      if (attr != null) attr.removeModifier(BONUS);
    }
  }

  public static void update(ServerPlayer p) {
    if (p.tickCount % 20 == 0) {
      MetisSynthesis.discover(p);
      refresh(p);
    }
    if (p.tickCount % 10 != 0 || !p.isAlive() || p.isSpectator() || !MetisMatrix.active(p)) return;
    var mind = MetisMatrix.owned(p).orElseThrow();
    if (p.tickCount % 20 == 0) {
      var target = p.getLastHurtMob();
      if (target == null) target = p.getLastHurtByMob();
      if (target != null && dev.heavenlyrestriction.splitmind.SplitMindCasting.enemy(p, target))
        MetisMagic.record(p, target, mind);
    }
    for (var circle :
        p
            .serverLevel()
            .getEntitiesOfClass(
                MagicCircle.class,
                p.getBoundingBox().inflate(32),
                c -> c.isAlive() && p.distanceToSqr(c) <= 1024 && p.hasLineOfSight(c))
            .stream()
            .sorted(Comparator.comparingDouble(p::distanceToSqr))
            .limit(16)
            .toList()) analyze(p, mind, circle);
  }

  public static boolean analyze(ServerPlayer p, ManasSkillInstance mind, MagicCircle circle) {
    if (!MetisMatrix.active(p)
        || circle.getOwner() == p
        || !circle.isAlive()
        || p.distanceToSqr(circle) > 1024
        || !p.hasLineOfSight(circle)) return false;
    var original = circle.getSkill();
    if (original == null
        || original.getMastery() < 0
        || !original.is(TensuraSkillTags.COPIABLE_MAGIC)) return false;
    var storage = SkillAPI.getSkillsFrom(p);
    if (storage
        .getSkill(original.getSkill())
        .filter(i -> i.getMastery() >= 0 && i.getRemoveTime() != -3)
        .isPresent()) return false;
    var retry = RETRY.computeIfAbsent(p, k -> new HashMap<>());
    long now = p.serverLevel().getGameTime();
    Long until = retry.get(original.getSkillId());
    if (until != null && until > now && until - now <= 600) return false;
    if (retry.size() >= 128) retry.clear();
    retry.put(original.getSkillId(), now + 600);
    var skill = Changeable.of(original.getSkill());
    if (TensuraSkillEvents.SKILL_PLUNDER
            .invoker()
            .plunder(circle.getOwner(), p, false, skill)
            .isFalse()
        || skill.get() == null) return false;
    boolean learned;
    try (var scope = ChainedContext.ability(p, mind.getSkill())) {
      learned = SkillHelper.learnSkill(p, skill.get(), mind.getRemoveTime());
    }
    if (learned) {
      mind.addMasteryPoint(p);
      p.displayClientMessage(
          Component.translatable("heavenlyrestriction.metis.analyzed", skill.get().getName()),
          true);
    }
    return learned;
  }

  public static void clear(ServerPlayer p) {
    RETRY.remove(p);
  }

  private MetisIntellect() {}
}
