package dev.heavenlyrestriction.tyche;

import dev.architectury.event.EventResult;
import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.critical.CriticalMerchant;
import io.github.manasmods.manascore.attribute.api.AttributeEvents;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class Tyche {
  public static final String HOUSE_TICKS = "TycheHouseTicks";
  public static final double RADIUS = 16;
  private static final ResourceLocation BONUS = HeavenlyRestriction.id("tyche_adversity");

  private record Defense(Defense previous, LivingEntity target, DamageSource source) {}

  private static final ThreadLocal<Defense> DEFENSE = new ThreadLocal<>();

  public static Optional<ManasSkillInstance> owned(LivingEntity e) {
    return e == null
        ? Optional.empty()
        : SkillAPI.getSkillsFrom(e)
            .getSkill(HeavenlyRestriction.TYCHE.get())
            .filter(i -> i.getMastery() >= 0);
  }

  public static boolean house(LivingEntity e) {
    return owned(e)
        .filter(i -> i.canInteractSkill(e) && i.getOrCreateTag().getInt(HOUSE_TICKS) > 0)
        .isPresent();
  }

  public static boolean protectedFrom(LivingEntity owner, LivingEntity target) {
    return owner != null
        && owner != target
        && house(target)
        && owner.level() == target.level()
        && owner.distanceToSqr(target) <= RADIUS * RADIUS
        && RestrictionData.canHarm(owner, target);
  }

  public static boolean guard(LivingEntity target, DamageSource source, BooleanSupplier action) {
    if (source == null) return action.getAsBoolean();
    var owner = RestrictionData.attacker(source);
    if (target.level().isClientSide() || !protectedFrom(owner, target))
      return action.getAsBoolean();
    for (var f = DEFENSE.get(); f != null; f = f.previous)
      if (f.target == target && f.source == source) return action.getAsBoolean();
    if (!((TensuraDamageSource) source).tensura$isDodgeBypass()
        && target.getRandom().nextFloat() < .25f) {
      visual(target, ArtVisualEntity.TYCHE_DIVIDEND, 1, 16);
      return false;
    }
    var f = new Defense(DEFENSE.get(), target, source);
    DEFENSE.set(f);
    try {
      return action.getAsBoolean();
    } finally {
      if (f.previous == null) DEFENSE.remove();
      else DEFENSE.set(f.previous);
    }
  }

  public static void register() {
    NeoForge.EVENT_BUS.addListener(Tyche::tick);
    NeoForge.EVENT_BUS.addListener(
        EventPriority.LOWEST,
        (CriticalHitEvent e) -> {
          if (e.getTarget() instanceof LivingEntity target
              && protectedFrom(e.getEntity(), target)) {
            e.setCriticalHit(false);
            e.setDamageMultiplier(1);
          }
        });
    AttributeEvents.CRITICAL_ATTACK_CHANCE_EVENT.register(
        (owner, target, base, mult, chance) -> {
          if (target instanceof LivingEntity t && protectedFrom(owner, t)) {
            chance.set(-1d);
            return EventResult.interruptFalse();
          }
          return EventResult.pass();
        });
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerLoggedOutEvent e) ->
            owned(e.getEntity()).ifPresent(i -> clear(i, e.getEntity())));
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerChangedDimensionEvent e) ->
            owned(e.getEntity()).ifPresent(i -> clear(i, e.getEntity())));
  }

  public static void tick(EntityTickEvent.Post event) {
    if (!(event.getEntity() instanceof LivingEntity e) || e.level().isClientSide()) return;
    owned(e)
        .ifPresent(
            i -> {
              FateInsurance.expire(i);
              int left = i.getOrCreateTag().getInt(HOUSE_TICKS);
              if (left > 0) {
                i.getOrCreateTag().putInt(HOUSE_TICKS, left - 1);
                if (left % 20 == 0) i.markDirty();
              }
            });
    if (e.tickCount % 5 == 0) refresh(e);
    if (e instanceof ServerPlayer p && e.tickCount % 20 == 0) TycheSynthesis.discover(p);
  }

  public static void refresh(LivingEntity e) {
    if (e.level().isClientSide()) return;
    double severity =
        owned(e).filter(i -> i.isToggled() && i.canInteractSkill(e)).isPresent()
            ? TychePayout.adversity(e.getHealth(), e.getMaxHealth())
            : 0;
    modifier(
        e,
        Attributes.ATTACK_DAMAGE,
        severity * .5,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    modifier(
        e,
        TensuraAttributes.AUTO_MELEE_DODGE_CHANCE,
        severity * 25,
        AttributeModifier.Operation.ADD_VALUE);
    modifier(
        e,
        TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE,
        severity * 25,
        AttributeModifier.Operation.ADD_VALUE);
  }

  private static void modifier(
      LivingEntity e, Holder<Attribute> type, double amount, AttributeModifier.Operation op) {
    var a = e.getAttribute(type);
    if (a == null) return;
    var old = a.getModifier(BONUS);
    if (old != null && old.amount() == amount) return;
    a.removeModifier(BONUS);
    if (amount > 0) a.addTransientModifier(new AttributeModifier(BONUS, amount, op));
  }

  public static boolean housePress(ManasSkillInstance i, LivingEntity e) {
    if (e.level().isClientSide()
        || i.getMastery() < 0
        || i.onCoolDown(1)
        || !i.canInteractSkill(e)
        || !i.canActivateSkill(e, 1)) return false;
    var energy = TensuraStorages.getExistenceFrom(e);
    double cost = SchemaArt.cost(e, TycheConfig.HOUSE_COST.get());
    if (!Double.isFinite(cost) || energy.getMagicule() < cost) return false;
    energy.setMagicule(energy.getMagicule() - cost);
    energy.markDirty();
    i.getOrCreateTag().putInt(HOUSE_TICKS, 300);
    i.setCoolDown(90, 1);
    i.addMasteryPoint(e);
    i.markDirty();
    visual(e, ArtVisualEntity.TYCHE_HOUSE, RADIUS, 300);
    return true;
  }

  public static void clear(ManasSkillInstance i, LivingEntity e) {
    i.getOrCreateTag().putInt(HOUSE_TICKS, 0);
    CriticalMerchant.clear(i);
    modifier(e, Attributes.ATTACK_DAMAGE, 0, AttributeModifier.Operation.ADD_VALUE);
    modifier(
        e, TensuraAttributes.AUTO_MELEE_DODGE_CHANCE, 0, AttributeModifier.Operation.ADD_VALUE);
    modifier(
        e,
        TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE,
        0,
        AttributeModifier.Operation.ADD_VALUE);
    i.markDirty();
  }

  public static void visual(LivingEntity e, int kind, double radius, int ticks) {
    if (!(e.level() instanceof ServerLevel level)) return;
    var v = ArtVisualEntity.spawn(level, kind, e.position(), new Vec3(radius, 0, 0), ticks);
    v.setCaster(e);
    level.playSound(
        null,
        e.blockPosition(),
        net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
        net.minecraft.sounds.SoundSource.PLAYERS,
        .8f,
        kind == ArtVisualEntity.TYCHE_REBIRTH ? .65f : 1.4f);
  }

  private Tyche() {}
}
