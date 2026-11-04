package dev.heavenlyrestriction.highrealm.boss;

import com.mojang.datafixers.util.Pair;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.*;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.ability.skill.unique.MartialMasterSkill;
import io.github.manasmods.tensura.config.ability.magic.SpiritualMagicConfig;
import io.github.manasmods.tensura.entity.magic.MagicCircle;
import io.github.manasmods.tensura.entity.variant.MagicCircleVariant;
import io.github.manasmods.tensura.registry.battlewill.MeleeArts;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.magic.*;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.Direction;
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
    FUTURE_VISION(() -> UniqueSkills.SEER.get(), 0, 32, 40);

    private final Supplier<ManasSkill> skill;
    final double min, max;
    final int interval;

    Action(Supplier<ManasSkill> skill, double min, double max, int interval) {
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
        || i.onCoolDown(0) && !i.canIgnoreCoolDown(boss, 0)
        || !i.canInteractSkill(boss)
        || !i.canActivateSkill(boss, 0)) return false;
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
          return pool.getAura() >= skill.getAuraCost(boss, i, 0)
              && pool.getMagicule() >= skill.getMagiculeCost(boss, i, 0);
        });
  }

  private void aim(Action action) {
    var enemy = boss.getTarget();
    boss.lookAt(
        Anchor.EYES,
        action == Action.MARTIAL_DASH ? enemy.position().add(0, -.2, 0) : enemy.getEyePosition());
    boss.setYBodyRot(boss.getYRot());
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
                0,
                Pair.of(0d, magic.getMagiculeCost(boss, i, 0)));
      boss.setCasting(true);
      hold();
    } else {
      withStance(
          () -> {
            action.skill().onPressed(i, boss, 0, 0);
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
    if (i == null || !withStance(() -> action.skill().onHeld(i, boss, held, 0))) {
      cancel();
      return;
    }
    if (held >= duration) {
      withStance(
          () -> {
            action.skill().onRelease(i, boss, held, 0, 0);
            return true;
          });
      i.markDirty();
      cancel();
    }
  }

  public void tick() {
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
          || !i.canActivateSkill(boss, 0)) {
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
      if (ready(Action.FUTURE_VISION)) begin(Action.FUTURE_VISION);
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
