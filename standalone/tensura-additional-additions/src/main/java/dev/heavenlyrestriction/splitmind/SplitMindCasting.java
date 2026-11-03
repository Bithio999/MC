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
  public static final String ASSIGNED = "SplitMindMagic", AUTO = "SplitMindAuto";
  private static final int AUTO_KEY = 3;
  private static final Map<UUID, Cast> CASTS = new HashMap<>();

  private record Invocation(ServerPlayer owner, ManasSkillInstance spell) {}

  private static final ThreadLocal<Invocation> INVOCATION = new ThreadLocal<>();

  public static boolean virtualSlot(
      io.github.manasmods.tensura.storage.ability.IAbility storage,
      int key,
      ManasSkill skill,
      int mode) {
    var context = INVOCATION.get();
    return context != null
        && key == AUTO_KEY
        && mode == 0
        && context.spell().getSkill() == skill
        && TensuraStorages.getAbilityFrom(context.owner()) == storage
        && SkillAPI.getSkillsFrom(context.owner()).getSkill(skill).orElse(null) == context.spell()
        && eligible(context.spell());
  }

  private static <T> T autoInvoke(
      ServerPlayer p, ManasSkillInstance spell, LivingEntity target, Supplier<T> work) {
    var before = INVOCATION.get();
    INVOCATION.set(new Invocation(p, spell));
    try {
      return aim(p, target, work);
    } finally {
      if (before == null) INVOCATION.remove();
      else INVOCATION.set(before);
    }
  }

  private record Cast(ManasSkillInstance spell, TickingSkill held, UUID target) {}

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
        : SkillAPI.getSkillsFrom(e).getSkill(id).filter(SplitMindCasting::eligible);
  }

  public static boolean assign(ServerPlayer p, String id) {
    var mind =
        SkillAPI.getSkillsFrom(p)
            .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
            .filter(i -> i.getMastery() >= 0 && i.canInteractSkill(p) && i.canActivateSkill(p, 0));
    if (mind.isEmpty()) return false;
    if (!id.isEmpty()) {
      var key = ResourceLocation.tryParse(id);
      if (key == null
          || SkillAPI.getSkillsFrom(p).getSkill(key).filter(SplitMindCasting::eligible).isEmpty())
        return false;
    }
    cancel(p);
    mind.get().getOrCreateTag().putString(ASSIGNED, id);
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

  private static boolean funded(ServerPlayer p, ManasSkillInstance i) {
    if (p.hasInfiniteMaterials()) return true;
    var magic = (Magic) i.getSkill();
    var pool = TensuraStorages.getExistenceFrom(p);
    return pool.getAura() >= magic.getAuraCost(p, i, 0)
        && pool.getMagicule()
            >= magic.getMagiculeCost(p, i, 0)
                * p.getAttributeValue(TensuraAttributes.MAGIC_COST_MULTIPLIER);
  }

  public static dev.architectury.event.EventResult manualActivation(
      io.github.manasmods.manascore.network.api.util.Changeable<ManasSkillInstance> skill,
      LivingEntity e,
      int key,
      int mode) {
    if (key >= 0 && key < 3) cancel(e);
    return dev.architectury.event.EventResult.pass();
  }

  public static void tick(PlayerTickEvent.Post event) {
    if (event.getEntity() instanceof ServerPlayer p) update(p);
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
    var storage = SkillAPI.getSkillsFrom(p);
    var current = CASTS.get(p.getUUID());
    if (current != null) {
      var enemy = target(p, current);
      if (current.spell() != spell.get()
          || enemy == null
          || !storage.getHeldSkills().contains(current.held())
          || !spell.get().canActivateSkill(p, 0)
          || !funded(p, spell.get())) {
        cancel(p);
        return;
      }
      if (current.held().getDuration()
          >= ((Magic) spell.get().getSkill()).getCastingTime(spell.get(), p)) {
        try {
          var pool = TensuraStorages.getExistenceFrom(p);
          double ap = pool.getAura(), mp = pool.getMagicule(), mastery = spell.get().getMastery();
          int cooldown = spell.get().getCoolDown(0);
          autoInvoke(
              p,
              spell.get(),
              enemy,
              () -> {
                storage.handleSkillRelease(spell.get(), AUTO_KEY, 0, false);
                return true;
              });
          if (pool.getAura() < ap
              || pool.getMagicule() < mp
              || spell.get().getMastery() > mastery
              || spell.get().getCoolDown(0) > cooldown) mind.get().addMasteryPoint(p);
        } finally {
          cancel(p);
        }
      }
      return;
    }
    if (p.tickCount % 5 != 0
        || p.isUsingItem()
        || !storage.getHeldSkills().isEmpty()
        || !funded(p, spell.get())
        || !spell.get().canInteractSkill(p)
        || !spell.get().canActivateSkill(p, 0)
        || spell.get().onCoolDown(0) && !spell.get().canIgnoreCoolDown(p, 0)) return;
    var enemy = nearest(p);
    if (enemy == null) return;
    if (autoInvoke(p, spell.get(), enemy, () -> storage.startHoldSkill(spell.get(), AUTO_KEY, 0))) {
      storage.getHeldSkills().stream()
          .filter(
              t ->
                  t.getSkill() == spell.get().getSkill()
                      && t.getMode() == 0
                      && t.getKeyNumber() == AUTO_KEY)
          .findFirst()
          .ifPresent(t -> CASTS.put(p.getUUID(), new Cast(spell.get(), t, enemy.getUUID())));
    }
  }

  /** Only the automation's native hold callback receives the scoped server aim. */
  public static boolean tickAimed(TickingSkill held, LivingEntity e, Supplier<Boolean> original) {
    var cast = CASTS.get(e.getUUID());
    if (cast == null || cast.held() != held || !(e instanceof ServerPlayer p))
      return original.get();
    var enemy = target(p, cast);
    if (enemy == null) return false;
    return autoInvoke(p, cast.spell(), enemy, original);
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

  public static void cancel(LivingEntity e) {
    var cast = CASTS.remove(e.getUUID());
    if (cast == null) return;
    SkillAPI.getSkillsFrom(e).getHeldSkills().remove(cast.held());
    cast.spell().removeAttributeModifiers(e, 0);
  }

  private SplitMindCasting() {}
}
