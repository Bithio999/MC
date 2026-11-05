package dev.heavenlyrestriction.highrealm.boss;

import com.mojang.datafixers.util.Pair;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.*;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.ability.skill.MagicElementalTransformSkill;
import io.github.manasmods.tensura.ability.skill.extra.LawManipulationSkill;
import io.github.manasmods.tensura.ability.skill.unique.CookSkill;
import io.github.manasmods.tensura.ability.skill.unique.MartialMasterSkill;
import io.github.manasmods.tensura.config.ability.magic.SpiritualMagicConfig;
import io.github.manasmods.tensura.data.TensuraSkillTags;
import io.github.manasmods.tensura.entity.TensuraProjectile;
import io.github.manasmods.tensura.entity.magic.MagicCircle;
import io.github.manasmods.tensura.entity.variant.MagicCircleVariant;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.battlewill.MeleeArts;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.magic.*;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** Chooses owned native abilities; all attacks, damage, spending and effects belong to Tensura. */
public final class WayfarerCombat {
  public enum Action {
    AURA_SLASH(() -> MeleeArts.AURA_SLASH.get(), 3, 32, 20),
    WIND_CUTTER(() -> AspectualMagics.WIND_CUTTER.get(), 4, 32, 30),
    TORNADO_BLADE(() -> AspectualMagics.TORNADO_BLADE.get(), 5, 32, 50),
    AERIAL_BLADE(() -> SpiritualMagics.AERIAL_BLADE.get(), 0, 4, 40),
    MARTIAL_DASH(() -> UniqueSkills.MARTIAL_MASTER.get(), 6, 24, 120),
    FUTURE_VISION(() -> UniqueSkills.SEER.get(), 0, 32, 40),
    WIND_TRANSFORM(() -> ExtraSkills.MAGIC_WIND_TRANSFORM.get(), 0, 80, 20),
    LAW_CLEANSE(() -> ExtraSkills.LAW_MANIPULATION.get(), 0, 80, 40),
    LAW_TAKEOVER(() -> ExtraSkills.LAW_MANIPULATION.get(), 0, 80, 10, 1);

    private final Supplier<ManasSkill> skill;
    final double min, max;
    final int interval, mode;

    Action(Supplier<ManasSkill> skill, double min, double max, int interval) {
      this(skill, min, max, interval, 0);
    }

    Action(Supplier<ManasSkill> skill, double min, double max, int interval, int mode) {
      this.mode = mode;
      this.skill = skill;
      this.min = min;
      this.max = max;
      this.interval = interval;
    }

    public ManasSkill skill() {
      return skill.get();
    }
  }

  private final KaelVeyrEntity boss;
  private final EnumMap<Action, Long> nextUse = new EnumMap<>(Action.class);
  private Action active;
  private UUID target;
  private int held, duration;
  private long nextDecision;
  private MagicCircle circle;
  private boolean engaged;

  WayfarerCombat(KaelVeyrEntity boss) {
    this.boss = boss;
  }

  public Action active() {
    return active;
  }

  public int duration() {
    return duration;
  }

  public void delay(int ticks) {
    nextDecision = boss.level().getGameTime() + ticks;
  }

  private ManasSkillInstance instance(Action action) {
    return SkillAPI.getSkillsFrom(boss).getSkill(action.skill()).orElse(null);
  }

  private boolean withStance(Supplier<Boolean> call) {
    boolean old = boss.isShiftKeyDown();
    boss.setShiftKeyDown(false);
    try {
      return call.get();
    } finally {
      boss.setShiftKeyDown(old);
    }
  }

  public boolean ready(Action action) {
    var enemy = boss.getTarget();
    if (!boss.initialized()
        || boss.isTame()
        || boss.isNoAi()
        || !boss.eligible(enemy)
        || !boss.hasLineOfSight(enemy)
        || SkillUtils.shouldCancelInteraction(boss)
        || boss.level().getGameTime() < nextUse.getOrDefault(action, 0L)) return false;
    double range =
        action == Action.AERIAL_BLADE
            ? ConfigRegistry.getConfig(SpiritualMagicConfig.class).AerialBlade.rangeMastered
            : action.max;
    double distance = boss.distanceToSqr(enemy);
    if (distance < action.min * action.min || distance > range * range) return false;
    var i = instance(action);
    if (i == null
        || i.getMastery() < 0
        || i.onCoolDown(action.mode) && !i.canIgnoreCoolDown(boss, action.mode)
        || !i.canInteractSkill(boss)
        || !i.canActivateSkill(boss, action.mode)) return false;
    if (action == Action.WIND_TRANSFORM
        && MagicElementalTransformSkill.hasMagicTransformEffect(boss, Element.WIND)) return false;
    if (action == Action.LAW_CLEANSE && !needsCleanse()) return false;
    if (action == Action.LAW_TAKEOVER && takeoverTarget() == null) return false;
    if (action == Action.FUTURE_VISION
        && boss.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.FUTURE_VISION)))
      return false;
    if (action == Action.MARTIAL_DASH) {
      if (!boss.onGround() && !boss.isInWaterOrBubble()
          || SkillUtils.shouldCancelTeleportation(boss)) return false;
      if (!safeNativeDash()) return false;
    }
    return withStance(
        () -> {
          var skill = (TensuraSkill) action.skill();
          var pool = TensuraStorages.getExistenceFrom(boss);
          return pool.getAura() >= skill.getAuraCost(boss, i, action.mode)
              && pool.getMagicule() >= skill.getMagiculeCost(boss, i, action.mode);
        });
  }

  private void aim(Action action) {
    if (action == Action.LAW_TAKEOVER) {
      var projectile = takeoverTarget();
      if (projectile != null) look(projectile.getBoundingBox().getCenter());
      return;
    }
    var enemy = boss.getTarget();
    boss.lookAt(
        Anchor.EYES,
        action == Action.MARTIAL_DASH ? enemy.position().add(0, -.2, 0) : enemy.getEyePosition());
    boss.setYBodyRot(boss.getYRot());
  }

  private void look(Vec3 at) {
    boss.lookAt(Anchor.EYES, at);
    boss.setYBodyRot(boss.getYRot());
  }

  private boolean needsCleanse() {
    var health = boss.getAttribute(Attributes.MAX_HEALTH);
    return TensuraStorages.getEffectFrom(boss).getSeveranceAmount() > 0
        || health != null && health.hasModifier(CookSkill.COOK)
        || boss.getActiveEffects().stream()
            .anyMatch(e -> LawManipulationSkill.getCleansingEffect().test(e.getEffect()));
  }

  /** Aim at an eligible native spell, then verify the same ray selection used by Law's mode. */
  private TensuraProjectile takeoverTarget() {
    double range = LawManipulationSkill.CONFIG.takeoverRange;
    var projectiles =
        boss.level()
            .getEntitiesOfClass(
                TensuraProjectile.class,
                boss.getBoundingBox().inflate(range),
                p ->
                    p.isAlive()
                        && boss.distanceToSqr(p) <= range * range
                        && p.getOwner() instanceof LivingEntity owner
                        && boss.eligible(owner)
                        && p.getSkill() != null
                        && p.getSkill().is(TensuraSkillTags.MAGIC));
    projectiles.sort(Comparator.comparingDouble(boss::distanceToSqr));
    for (var p : projectiles) {
      var owner = (LivingEntity) p.getOwner();
      if (owner.getAttributeValue(TensuraAttributes.LAW_DEGRADATION) > 0
          && EnergyHelper.getMaxEP(owner)
              >= EnergyHelper.getMaxEP(boss) * LawManipulationSkill.CONFIG.takeoverMultiplier)
        continue;
      look(p.getBoundingBox().getCenter());
      if (ObjectSelectionHelper.getTargetingEntity(
              TensuraProjectile.class, boss, range, .5, true, false, false)
          == p) return p;
    }
    return null;
  }

  /** Preflight the exact native dash destination; never substitute custom teleport/damage. */
  private boolean safeNativeDash() {
    aim(Action.MARTIAL_DASH);
    var hit =
        ObjectSelectionHelper.getPlayerPOVHitResult(
            boss.level(),
            boss,
            ClipContext.Fluid.NONE,
            MartialMasterSkill.CONFIG.ultraDistanceMastered);
    var pos = hit.getBlockPos().relative(hit.getDirection());
    if (!boss.level().hasChunkAt(pos)) return false;
    Vec3 at = ObjectSelectionHelper.getFloorPos(pos);
    if (!boss.level().getBlockState(pos).canBeReplaced())
      at = ObjectSelectionHelper.getFloorPos(pos.above());
    var floor = net.minecraft.core.BlockPos.containing(at);
    return boss.level().hasChunkAt(floor)
        && boss.level().getWorldBorder().isWithinBounds(floor)
        && boss.level()
            .getBlockState(floor.below())
            .isFaceSturdy(boss.level(), floor.below(), Direction.UP)
        && boss.level().getFluidState(floor).isEmpty()
        && boss.level().noCollision(boss, boss.getBoundingBox().move(at.subtract(boss.position())))
        && at.distanceToSqr(boss.getTarget().position()) <= 6 * 6;
  }

  public boolean begin(Action action) {
    if (active != null || !ready(action)) return false;
    aim(action);
    var i = instance(action);
    active = action;
    target = boss.getTarget().getUUID();
    held = 0;
    duration =
        action.skill() instanceof Magic magic ? Math.max(0, magic.getCastingTime(i, boss)) : 0;
    nextUse.put(action, boss.level().getGameTime() + Math.max(action.interval, duration + 10));
    if (action.skill() instanceof Magic magic) {
      if (duration > 1)
        circle =
            MagicCircle.castMagicCircle(
                1.6f,
                duration,
                MagicCircleVariant.WIND,
                boss,
                i.getOrCreateTag(),
                .8f,
                Vec3.ZERO,
                i,
                action.mode,
                Pair.of(0d, magic.getMagiculeCost(boss, i, action.mode)));
      boss.setCasting(true);
      hold();
    } else {
      withStance(
          () -> {
            action.skill().onPressed(i, boss, 0, action.mode);
            return true;
          });
      i.markDirty();
      cancel();
    }
    return true;
  }

  private void hold() {
    var i = instance(active);
    Action action = active;
    if (i == null || !withStance(() -> action.skill().onHeld(i, boss, held, action.mode))) {
      cancel();
      return;
    }
    if (held >= duration) {
      withStance(
          () -> {
            action.skill().onRelease(i, boss, held, 0, action.mode);
            return true;
          });
      i.markDirty();
      cancel();
    }
  }

  public void tick() {
    if (!boss.eligible(boss.getTarget())) {
      engaged = false;
      if (active != null) cancel();
      return;
    }
    if (!engaged) {
      engaged = true;
      if (begin(Action.WIND_TRANSFORM)) return;
    }
    if (active != null) {
      var enemy = boss.getTarget();
      var i = instance(active);
      if (!boss.eligible(enemy)
          || target == null
          || !target.equals(enemy.getUUID())
          || boss.isTame()
          || boss.isNoAi()
          || !boss.hasLineOfSight(enemy)
          || SkillUtils.shouldCancelInteraction(boss)
          || i == null
          || !i.canInteractSkill(boss)
          || !i.canActivateSkill(boss, active.mode)) {
        cancel();
        return;
      }
      aim(active);
      boss.getNavigation().stop();
      boss.setDeltaMovement(boss.getDeltaMovement().multiply(0, 1, 0));
      held++;
      hold();
    } else if (boss.level().getGameTime() >= nextDecision) {
      delay(20);
      if (ready(Action.LAW_CLEANSE)) begin(Action.LAW_CLEANSE);
      else if (ready(Action.WIND_TRANSFORM)) begin(Action.WIND_TRANSFORM);
      else if (ready(Action.LAW_TAKEOVER)) begin(Action.LAW_TAKEOVER);
      else if (ready(Action.FUTURE_VISION)) begin(Action.FUTURE_VISION);
      else {
        var choices = Arrays.stream(Action.values()).filter(this::ready).toList();
        if (!choices.isEmpty()) begin(choices.get(boss.getRandom().nextInt(choices.size())));
      }
    }
  }

  public void cancel() {
    if (circle != null) {
      if (active != null) {
        var i = instance(active);
        if (i != null && i.getOrCreateTag().getInt("MagicCircleID") == circle.getId()) {
          i.getOrCreateTag().remove("MagicCircleID");
          i.markDirty();
        }
      }
      circle.discard();
      circle = null;
    }
    active = null;
    target = null;
    boss.setCasting(false);
    delay(15);
  }
}
