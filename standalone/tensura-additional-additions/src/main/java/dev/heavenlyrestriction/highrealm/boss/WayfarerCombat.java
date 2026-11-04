package dev.heavenlyrestriction.highrealm.boss;

import com.mojang.datafixers.util.Pair;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.*;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.entity.magic.MagicCircle;
import io.github.manasmods.tensura.entity.variant.MagicCircleVariant;
import io.github.manasmods.tensura.registry.magic.*;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/**
 * Bounded encounter geometry. Native magic owns its cost/cooldown; custom attacks use native
 * sources.
 */
public final class WayfarerCombat {
  public static final int DRAW_WINDUP = 22, LANCE_WINDUP = 34, WALTZ_WINDUP = 36;

  public static double auraCost(int mode) {
    return mode == KaelVeyrEntity.RIFT_DRAW ? 4000 : mode == KaelVeyrEntity.WALTZ ? 12000 : 0;
  }

  public static double magicCost(int mode) {
    return mode == KaelVeyrEntity.CONSTELLATION ? 18000 : 0;
  }

  private static ManasSkillInstance martial(KaelVeyrEntity boss) {
    return SkillAPI.getSkillsFrom(boss).getSkill(UniqueSkills.MARTIAL_MASTER.get()).orElse(null);
  }

  public static boolean ready(KaelVeyrEntity boss, int mode) {
    if (mode < 1 || mode > 4 || SkillUtils.shouldCancelInteraction(boss)) return false;
    var instance = martial(boss);
    if (instance == null || !instance.canInteractSkill(boss) || !instance.canActivateSkill(boss, 0))
      return false;
    if (mode == KaelVeyrEntity.RIFT_DRAW && SkillUtils.shouldCancelTeleportation(boss))
      return false;
    var pool = TensuraStorages.getExistenceFrom(boss);
    if (pool.getAura() < auraCost(mode) || pool.getMagicule() < magicCost(mode)) return false;
    return mode != KaelVeyrEntity.TEMPEST || spell(boss) != null;
  }

  static ManasSkillInstance spell(KaelVeyrEntity boss) {
    var spells = List.of(AspectualMagics.TORNADO_BLADE.get(), SpiritualMagics.AERIAL_BLADE.get());
    for (int n = 0; n < spells.size(); n++) {
      var magic =
          boss.activeMagic() != null
              ? boss.activeMagic()
              : spells.get(Math.floorMod(boss.spellPreference() + n, spells.size()));
      if (magic == SpiritualMagics.AERIAL_BLADE.get()
          && boss.activeMagic() == null
          && boss.getTarget() != null) {
        double range =
            io.github.manasmods.manascore.config.ConfigRegistry.getConfig(
                    io.github.manasmods.tensura.config.ability.magic.SpiritualMagicConfig.class)
                .AerialBlade
                .rangeMastered;
        if (boss.distanceToSqr(boss.getTarget()) > range * range) continue;
      }
      var i = SkillAPI.getSkillsFrom(boss).getSkill(magic).orElse(null);
      if (i != null
          && !i.onCoolDown(0)
          && i.canInteractSkill(boss)
          && i.canActivateSkill(boss, 0)
          && TensuraStorages.getExistenceFrom(boss).getMagicule()
              >= ((Magic) magic).getMagiculeCost(boss, i, 0)) return i;
      if (boss.activeMagic() != null) break;
    }
    return null;
  }

  public static int duration(KaelVeyrEntity boss, int mode) {
    return switch (mode) {
      case KaelVeyrEntity.RIFT_DRAW -> DRAW_WINDUP + 12;
      case KaelVeyrEntity.CONSTELLATION -> LANCE_WINDUP + 6 * 5 + 12;
      case KaelVeyrEntity.TEMPEST -> {
        var i = spell(boss);
        yield Math.max(32, i == null ? 32 : ((Magic) i.getSkill()).getCastingTime(i, boss)) + 12;
      }
      default -> WALTZ_WINDUP + 3 * 14 + 10;
    };
  }

  public static void start(KaelVeyrEntity boss, int mode) {
    var pool = TensuraStorages.getExistenceFrom(boss);
    pool.setAura(Math.max(0, pool.getAura() - auraCost(mode)));
    pool.setMagicule(Math.max(0, pool.getMagicule() - magicCost(mode)));
    pool.markDirty();
    boss.playSound(
        SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, mode == KaelVeyrEntity.WALTZ ? .65f : 1.15f);
    if (mode == KaelVeyrEntity.TEMPEST) {
      var i = spell(boss);
      if (i != null)
        MagicCircle.castMagicCircle(
            2.2f,
            boss.duration() - 12,
            MagicCircleVariant.WIND,
            boss,
            .8f,
            Vec3.ZERO,
            i.getSkill(),
            0,
            Pair.of(0d, ((Magic) i.getSkill()).getMagiculeCost(boss, i, 0)));
    }
  }

  public static Vec3 direction(KaelVeyrEntity boss) {
    var diff = boss.patternAim().subtract(boss.patternOrigin());
    return new Vec3(diff.x, 0, diff.z).lengthSqr() < .001
        ? new Vec3(0, 0, 1)
        : new Vec3(diff.x, 0, diff.z).normalize();
  }

  public static Vec3 lance(KaelVeyrEntity boss, int index) {
    var center = boss.patternAim();
    if (index == 0) return center;
    double a = (index - 1) * Math.PI * 2 / 5;
    return center.add(Math.cos(a) * 7.5, 0, Math.sin(a) * 7.5);
  }

  public static void tick(KaelVeyrEntity boss, int age) {
    var level = (ServerLevel) boss.level();
    switch (boss.pattern()) {
      case KaelVeyrEntity.RIFT_DRAW -> {
        if (age == DRAW_WINDUP) draw(level, boss);
      }
      case KaelVeyrEntity.CONSTELLATION -> {
        int index = (age - LANCE_WINDUP) / 5;
        if (age >= LANCE_WINDUP && (age - LANCE_WINDUP) % 5 == 0 && index < 6) {
          Vec3 at = lance(boss, index);
          level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
          level.sendParticles(
              new DustParticleOptions(new org.joml.Vector3f(.6f, .92f, 1), 1.4f),
              at.x,
              at.y,
              at.z,
              18,
              1.3,
              .2,
              1.3,
              .05);
          boss.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.1f, .7f + index * .08f);
          for (var target : targets(level, boss, at, 3.25))
            if (target.getBoundingBox().getCenter().distanceToSqr(at) < 3.25 * 3.25)
              hit(boss, target, TensuraDamageTypes.LIGHT_ELEMENTAL, 260, 2);
        }
      }
      case KaelVeyrEntity.TEMPEST -> {
        var target = boss.castTarget();
        if (target != null) {
          boss.lookAt(Anchor.EYES, target.getEyePosition());
          boss.setYBodyRot(boss.getYRot());
        }
        if (age == boss.duration() - 12) {
          var i = spell(boss);
          if (i != null) {
            var magic = (Magic) i.getSkill();
            // The native release performs its own resource deduction, projectile/field and
            // cooldown.
            if (magic.onHeld(i, boss, 0, 0))
              magic.onRelease(
                  i, boss, Math.max(magic.getCastingTime(i, boss), boss.duration() - 12), 0, 0);
            boss.magicReleased();
            i.markDirty();
          }
        }
      }
      case KaelVeyrEntity.WALTZ -> {
        if (age >= WALTZ_WINDUP) {
          int wave = (age - WALTZ_WINDUP) / 14, part = (age - WALTZ_WINDUP) % 14;
          if (wave < 3 && part <= 8) {
            if (part == 0) {
              boss.waveHits().clear();
              boss.swing(InteractionHand.MAIN_HAND, true);
              boss.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2, .7f + wave * .18f);
            }
            Vec3 root = boss.patternOrigin().add(0, .9, 0),
                axis = direction(boss).yRot((float) Math.toRadians((wave - 1) * 30));
            double outer = (part + 1) * 3, inner = Math.max(0, (part - 1) * 3);
            for (var target : targets(level, boss, root, Math.min(27, outer + 2))) {
              Vec3 delta = target.getBoundingBox().getCenter().subtract(root);
              double distance = delta.horizontalDistance();
              if (distance < inner || distance > outer + 1.2 || Math.abs(delta.y) > 4) continue;
              if (distance > .01 && delta.normalize().dot(axis) < Math.cos(Math.toRadians(38)))
                continue;
              if (!boss.waveHits().add(target.getUUID())
                  || !clear(level, boss, root, target.getEyePosition())) continue;
              hit(boss, target, TensuraDamageTypes.AURA_SLASH, 340, 4);
            }
          }
        }
      }
      default -> {}
    }
  }

  private static List<LivingEntity> targets(
      ServerLevel level, KaelVeyrEntity boss, Vec3 at, double range) {
    return level.getEntitiesOfClass(
        LivingEntity.class, new AABB(at, at).inflate(range), boss::eligible);
  }

  private static boolean clear(ServerLevel level, KaelVeyrEntity boss, Vec3 from, Vec3 to) {
    return level
            .clip(
                new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss))
            .getType()
        == HitResult.Type.MISS;
  }

  private static void draw(ServerLevel level, KaelVeyrEntity boss) {
    var target = boss.castTarget();
    if (target == null || SkillUtils.shouldCancelTeleportation(boss)) return;
    Vec3 facing = boss.position().subtract(target.position());
    facing = new Vec3(facing.x, 0, facing.z).normalize();
    Vec3 desired = target.position().add(facing.yRot((float) Math.toRadians(55)).scale(3));
    BlockPos landing = null;
    for (int dy = 2; dy >= -3; dy--) {
      var pos = BlockPos.containing(desired).offset(0, dy, 0);
      if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
      if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
          || !level.getFluidState(pos).isEmpty()) continue;
      var box = boss.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(boss.position()));
      if (level.noCollision(boss, box)) {
        landing = pos;
        break;
      }
    }
    if (landing == null) return;
    Vec3 from = boss.position(), origin = boss.patternOrigin(), aim = boss.patternAim();
    boss.teleportTo(landing.getX() + .5, landing.getY(), landing.getZ() + .5);
    boss.resetFallDistance();
    boss.preservePatternOrigin(origin, aim);
    level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 24, .25, .6, .25, .12);
    level.sendParticles(
        ParticleTypes.FLASH, boss.getX(), boss.getY() + 1, boss.getZ(), 1, 0, 0, 0, 0);
    boss.playSound(SoundEvents.PLAYER_TELEPORT, 1.2f, .85f);
    boss.swing(InteractionHand.MAIN_HAND, true);
    boss.lookAt(Anchor.EYES, target.getEyePosition());
    boss.setYBodyRot(boss.getYRot());
    for (var enemy : targets(level, boss, boss.position().add(0, .9, 0), 5)) {
      Vec3 delta = enemy.getBoundingBox().getCenter().subtract(boss.position().add(0, .9, 0));
      if (delta.horizontalDistance() <= 4.5
          && delta.normalize().dot(boss.getLookAngle()) > .15
          && clear(level, boss, boss.getEyePosition(), enemy.getEyePosition()))
        hit(boss, enemy, TensuraDamageTypes.AURA_SLASH, 320, 1);
    }
  }

  private static void hit(
      KaelVeyrEntity boss,
      LivingEntity target,
      ResourceKey<DamageType> type,
      float amount,
      int mode) {
    var instance = martial(boss);
    if (instance == null || !boss.eligible(target)) return;
    var source = UniqueSkills.MARTIAL_MASTER.get().createSource(instance, boss, type, 0);
    ((TensuraDamageSource) source).tensura$setAuraCost(auraCost(mode));
    target.hurt(source, amount);
  }

  private WayfarerCombat() {}
}
