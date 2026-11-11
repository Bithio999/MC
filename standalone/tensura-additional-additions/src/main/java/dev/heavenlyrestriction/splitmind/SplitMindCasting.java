package dev.heavenlyrestriction.splitmind;

import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.skill.impl.*;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Uses the owned magic instance and ManasCore's activation/hold/release lifecycle. */
public final class SplitMindCasting {
  public static final String ASSIGNED = "SplitMindMagic",
      MODE = "SplitMindMode",
      AUTO = "SplitMindAuto";
  private static final int AUTO_KEY = 3;
  private static final Map<UUID, Cast> CASTS = new HashMap<>();

  private record Invocation(
      ServerPlayer owner, ManasSkillInstance spell, int mode, LivingEntity target) {}

  private static final ThreadLocal<Invocation> INVOCATION = new ThreadLocal<>();

  public static boolean virtualSlot(
      io.github.manasmods.tensura.storage.ability.IAbility storage,
      int key,
      ManasSkill skill,
      int mode) {
    var context = INVOCATION.get();
    return context != null
        && key == AUTO_KEY
        && mode == context.mode()
        && context.spell().getSkill() == skill
        && TensuraStorages.getAbilityFrom(context.owner()) == storage
        && SkillAPI.getSkillsFrom(context.owner()).getSkill(skill).orElse(null) == context.spell()
        && eligible(context.spell());
  }

  public static boolean virtualSlot(
      io.github.manasmods.tensura.storage.ability.IAbility storage, ManasSkill skill) {
    var context = INVOCATION.get();
    return context != null && virtualSlot(storage, AUTO_KEY, skill, context.mode());
  }

  /** Exact server-owned callback scope; ordinary/manual casting keeps all native rules. */
  public static boolean automatic(LivingEntity owner, ManasSkillInstance spell) {
    var context = INVOCATION.get();
    return context != null && context.owner() == owner && context.spell() == spell;
  }

  public static boolean automatic(LivingEntity owner) {
    var context = INVOCATION.get();
    return context != null && context.owner() == owner;
  }

  public static boolean selfTarget(LivingEntity owner) {
    var context = INVOCATION.get();
    return context != null && context.owner() == owner && context.target() == owner;
  }

  public static double magiculeFactor(LivingEntity owner) {
    if (dev.heavenlyrestriction.metis.MetisMatrix.owned(owner).isPresent())
      return dev.heavenlyrestriction.metis.MetisMatrix.costFactor(owner);
    return SkillAPI.getSkillsFrom(owner)
            .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
            .filter(i -> i.getMastery() >= 0 && i.isMastered(owner))
            .isPresent()
        ? .75
        : 1;
  }

  public static <T> T autoInvoke(
      ServerPlayer p, ManasSkillInstance spell, int mode, LivingEntity target, Supplier<T> work) {
    var before = INVOCATION.get();
    INVOCATION.set(new Invocation(p, spell, mode, target));
    try {
      return aim(p, target, work);
    } finally {
      if (before == null) INVOCATION.remove();
      else INVOCATION.set(before);
    }
  }

  public static void spawned(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
    var context = INVOCATION.get();
    if (!event.getLevel().isClientSide() && context != null)
      AutoCastCombat.spawned(event.getEntity(), context.owner(), context.spell(), context.mode());
    if (!event.getLevel().isClientSide()
        && context != null
        && event.getEntity()
            instanceof io.github.manasmods.tensura.entity.projectile.TensuraFlyingProjectile shot
        && shot.getOwner() == context.owner()
        && shot.getLookDistance() > 0)
      ((SplitMindProjectile) shot).additions$bindSplitMindTarget(context.target());
  }

  private record Cast(ManasSkillInstance spell, TickingSkill held, UUID target, int mode) {}

  public static boolean eligible(ManasSkillInstance i) {
    return i.getMastery() >= 0
        && i.getRemoveTime() != -3
        && i.getSkill() instanceof Magic m
        && (m.getType() == Magic.MagicType.ASPECTUAL || m.getType() == Magic.MagicType.SPIRITUAL);
  }

  public static Optional<ManasSkillInstance> assigned(LivingEntity e, ManasSkillInstance mind) {
    var id = ResourceLocation.tryParse(mind.getOrCreateTag().getString(ASSIGNED));
    return id == null
        ? Optional.empty()
        : SkillAPI.getSkillsFrom(e)
            .getSkill(id)
            .filter(i -> SplitMindModes.allowed(e, i, assignedMode(mind)));
  }

  public static int assignedMode(ManasSkillInstance mind) {
    return mind.getOrCreateTag().getInt(MODE);
  }

  public static boolean assign(ServerPlayer p, String id) {
    return assign(p, id, 0);
  }

  public static boolean assign(ServerPlayer p, String id, int mode) {
    var mind =
        SkillAPI.getSkillsFrom(p)
            .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
            .filter(i -> i.getMastery() >= 0 && i.canInteractSkill(p) && i.canActivateSkill(p, 0));
    if (mind.isEmpty()) return false;
    if (!id.isEmpty()) {
      var key = ResourceLocation.tryParse(id);
      if (key == null
          || SkillAPI.getSkillsFrom(p)
              .getSkill(key)
              .filter(i -> SplitMindModes.allowed(p, i, mode))
              .isEmpty()) return false;
    }
    if (id.isEmpty() && mode != 0) return false;
    cancel(p);
    mind.get().getOrCreateTag().putString(ASSIGNED, id);
    mind.get().getOrCreateTag().putInt(MODE, mode);
    if (id.isEmpty()) mind.get().getOrCreateTag().putBoolean(AUTO, false);
    mind.get().markDirty();
    SkillAPI.getSkillsFrom(p).markDirty();
    return true;
  }

  public static boolean enemy(ServerPlayer p, LivingEntity other) {
    if (!other.isAlive()
        || other == p
        || other.isSpectator()
        || !RestrictionData.canHarm(p, other)
        || p.distanceToSqr(other) > 32 * 32) return false;
    if (other instanceof Player player
        && (!p.canHarmPlayer(player) || player.isCreative() || !p.getServer().isPvpAllowed()))
      return false;
    var owner = TensuraStorages.getExistenceFrom(other).getPermanentOwner();
    if (owner != null && owner.equals(p.getUUID())) return false;
    if (other instanceof TamableAnimal pet
        && pet.getOwnerUUID() != null
        && pet.getOwnerUUID().equals(p.getUUID())) return false;
    boolean combat =
        p.getLastHurtByMob() == other
            || p.getLastHurtMob() == other
            || other.getLastHurtByMob() == p
            || other.getLastHurtMob() == p
            || other instanceof Mob mob && mob.getTarget() == p;
    return p.hasLineOfSight(other)
        && (combat
            || !(other instanceof Player)
                && (other instanceof net.minecraft.world.entity.monster.Enemy
                    || other.getType().getCategory() == MobCategory.MONSTER));
  }

  public static LivingEntity nearest(ServerPlayer p) {
    return p
        .serverLevel()
        .getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(32), e -> enemy(p, e))
        .stream()
        .min(
            Comparator.comparingDouble((LivingEntity e) -> p.distanceToSqr(e))
                .thenComparingInt(Entity::getId))
        .orElse(null);
  }

  private static LivingEntity target(ServerPlayer p, Cast cast) {
    var e = p.serverLevel().getEntity(cast.target());
    return e instanceof LivingEntity living && enemy(p, living) ? living : null;
  }

  public static boolean funded(ServerPlayer p, ManasSkillInstance i, int mode) {
    if (p.hasInfiniteMaterials()) return true;
    var magic = (Magic) i.getSkill();
    var pool = TensuraStorages.getExistenceFrom(p);
    return pool.getAura() >= magic.getAuraCost(p, i, mode)
        && pool.getMagicule()
            >= magic.getMagiculeCost(p, i, mode)
                * magiculeFactor(p)
                * p.getAttributeValue(TensuraAttributes.MAGIC_COST_MULTIPLIER);
  }

  public static dev.architectury.event.EventResult manualActivation(
      io.github.manasmods.manascore.network.api.util.Changeable<ManasSkillInstance> skill,
      LivingEntity e,
      int key,
      int mode) {
    if (skill.get() != null) dev.heavenlyrestriction.metis.MetisCasting.manual(e, skill.get());
    var current = CASTS.get(e.getUUID());
    // One owned spell instance shares its cooldown/tag state. Manual use of that same
    // spell takes priority, while every different spell can run alongside automation.
    if (INVOCATION.get() == null
        && current != null
        && skill.get() != null
        && current.spell().getSkill() == skill.get().getSkill()) cancel(e);
    return dev.architectury.event.EventResult.pass();
  }

  public static void tick(PlayerTickEvent.Post event) {
    if (event.getEntity() instanceof ServerPlayer p) {
      update(p);
      dev.heavenlyrestriction.metis.MetisIntellect.update(p);
      dev.heavenlyrestriction.metis.MetisCasting.update(p);
    }
  }

  public static void update(ServerPlayer p) {
    if (p.tickCount % 20 == 0) SplitMindSkill.refresh(p);
    var mind = SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SPLIT_MIND.get());
    if (!p.isAlive()
        || p.isSpectator()
        || mind.isEmpty()
        || mind.get().getMastery() < 0
        || !mind.get().getOrCreateTag().getBoolean(AUTO)
        || !mind.get().canInteractSkill(p)
        || !mind.get().canActivateSkill(p, 1)) {
      cancel(p);
      return;
    }
    var spell = assigned(p, mind.get());
    if (spell.isEmpty()) {
      cancel(p);
      mind.get().getOrCreateTag().putBoolean(AUTO, false);
      mind.get().markDirty();
      return;
    }
    int mode = assignedMode(mind.get());
    var storage = SkillAPI.getSkillsFrom(p);
    var current = CASTS.get(p.getUUID());
    if (current != null) {
      var enemy = target(p, current);
      if (current.spell() != spell.get()
          || current.mode() != mode
          || enemy == null
          || !storage.getHeldSkills().contains(current.held())
          || !spell.get().canActivateSkill(p, mode)
          || !funded(p, spell.get(), mode)) {
        cancel(p);
        return;
      }
      if (!SplitMindModes.sustained(spell.get(), mode)
          && current.held().getDuration()
              >= ((Magic) spell.get().getSkill()).getCastingTime(spell.get(), p)) {
        try {
          var pool = TensuraStorages.getExistenceFrom(p);
          double ap = pool.getAura(), mp = pool.getMagicule(), mastery = spell.get().getMastery();
          int cooldown = spell.get().getCoolDown(mode);
          autoInvoke(
              p,
              spell.get(),
              mode,
              enemy,
              () -> {
                storage.handleSkillRelease(spell.get(), AUTO_KEY, mode, false);
                return true;
              });
          if (pool.getAura() < ap
              || pool.getMagicule() < mp
              || spell.get().getMastery() > mastery
              || spell.get().getCoolDown(mode) > cooldown) mind.get().addMasteryPoint(p);
        } finally {
          cancel(p);
        }
      }
      return;
    }
    if (p.tickCount % 5 != 0
        || storage.getHeldSkills().stream().anyMatch(t -> t.getSkill() == spell.get().getSkill())
        || !funded(p, spell.get(), mode)
        || !spell.get().canInteractSkill(p)
        || !spell.get().canActivateSkill(p, mode)
        || spell.get().onCoolDown(mode) && !spell.get().canIgnoreCoolDown(p, mode)) return;
    var enemy = nearest(p);
    if (enemy == null) return;
    if (autoInvoke(
        p, spell.get(), mode, enemy, () -> storage.startHoldSkill(spell.get(), AUTO_KEY, mode))) {
      storage.getHeldSkills().stream()
          .filter(
              t ->
                  t.getSkill() == spell.get().getSkill()
                      && t.getMode() == mode
                      && t.getKeyNumber() == AUTO_KEY)
          .findFirst()
          .ifPresent(t -> CASTS.put(p.getUUID(), new Cast(spell.get(), t, enemy.getUUID(), mode)));
    }
  }

  /** Only the automation's native hold callback receives the scoped server aim. */
  public static boolean tickAimed(TickingSkill held, LivingEntity e, Supplier<Boolean> original) {
    var cast = CASTS.get(e.getUUID());
    if (cast == null || cast.held() != held || !(e instanceof ServerPlayer p))
      return e instanceof ServerPlayer player
          ? dev.heavenlyrestriction.metis.MetisCasting.tickAimed(held, player, original)
          : original.get();
    var enemy = target(p, cast);
    if (enemy == null || !SplitMindModes.allowed(p, cast.spell(), cast.mode())) return false;
    var pool = TensuraStorages.getExistenceFrom(p);
    double ap = pool.getAura(), mp = pool.getMagicule();
    boolean keep = autoInvoke(p, cast.spell(), cast.mode(), enemy, original);
    if (pool.getAura() < ap || pool.getMagicule() < mp)
      SkillAPI.getSkillsFrom(p)
          .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
          .ifPresent(i -> i.addMasteryPoint(p));
    if (!keep
        && SplitMindModes.sustained(cast.spell(), cast.mode())
        && held.getDuration() >= ((Magic) cast.spell().getSkill()).getCastingTime(cast.spell(), p))
      autoInvoke(
          p,
          cast.spell(),
          cast.mode(),
          enemy,
          () -> {
            SkillAPI.getSkillsFrom(p)
                .handleSkillRelease(cast.spell(), AUTO_KEY, cast.mode(), false);
            return true;
          });
    return keep;
  }

  private static <T> T aim(ServerPlayer p, LivingEntity target, Supplier<T> work) {
    float yaw = p.getYRot(), pitch = p.getXRot(), head = p.getYHeadRot(), body = p.yBodyRot;
    Vec3 d = target.getBoundingBox().getCenter().subtract(p.getEyePosition());
    p.setYRot((float) (Mth.atan2(d.z, d.x) * 180 / Math.PI) - 90);
    p.setXRot((float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI));
    p.setYHeadRot(p.getYRot());
    try {
      return work.get();
    } finally {
      p.setYRot(yaw);
      p.setXRot(pitch);
      p.setYHeadRot(head);
      p.yBodyRot = body;
    }
  }

  public static void cancelAll(LivingEntity e) {
    cancel(e);
    dev.heavenlyrestriction.metis.MetisCasting.cancel(e);
    if (e instanceof ServerPlayer player)
      dev.heavenlyrestriction.metis.MetisIntellect.clear(player);
  }

  public static void cancel(LivingEntity e) {
    var cast = CASTS.remove(e.getUUID());
    if (cast == null) return;
    SkillAPI.getSkillsFrom(e).getHeldSkills().remove(cast.held());
    cast.spell().removeAttributeModifiers(e, cast.mode());
  }

  private SplitMindCasting() {}
}
