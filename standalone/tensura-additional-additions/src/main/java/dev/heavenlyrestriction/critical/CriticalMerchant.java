package dev.heavenlyrestriction.critical;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.tyche.*;
import io.github.manasmods.manascore.attribute.api.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.entity.human.golem.TrainingDummyEntity;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import org.joml.Vector3f;

/** One payout and one reserved guarantee per accepted damage transaction. */
public final class CriticalMerchant {
  public static final int HITS = 10, COOLDOWN = 20;
  public static final double COST = 1000;
  private static final String REMAINING = "CriticalMerchantHits", BATCH = "CriticalMerchantBatch";
  private static final ThreadLocal<Hit> CURRENT = new ThreadLocal<>();

  public static void register() {
    // Native ManasCore crit resolution runs at HIGH; Unbound's melee callback registers first.
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CriticalMerchant::melee);
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CriticalMerchant::incoming);
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CriticalMerchant::finiteDamage);
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CriticalMerchant::landed);
    NeoForge.EVENT_BUS.addListener(
        EventPriority.LOWEST,
        (LivingDeathEvent e) -> instance(e.getEntity()).ifPresent(CriticalMerchant::clear));
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.Clone e) -> {
          if (e.isWasDeath()) instance(e.getEntity()).ifPresent(CriticalMerchant::clear);
        });
  }

  public static Optional<ManasSkillInstance> instance(LivingEntity e) {
    if (e == null) return Optional.empty();
    var ultimate = Tyche.owned(e);
    return ultimate.isPresent()
        ? ultimate
        : SkillAPI.getSkillsFrom(e)
            .getSkill(HeavenlyRestriction.CRITICAL_MERCHANT.get())
            .filter(i -> i.getMastery() >= 0);
  }

  public static boolean tycheUndodge(LivingEntity e, DamageSource source) {
    if (!Tyche.house(e)) return false;
    for (var h = CURRENT.get(); h != null; h = h.previous)
      if (h.owner == e
          && h.source == source
          && e.distanceToSqr(h.target) <= Tyche.RADIUS * Tyche.RADIUS) return true;
    return false;
  }

  public static int remaining(ManasSkillInstance i) {
    return i.getTag() == null ? 0 : Math.clamp(i.getTag().getInt(REMAINING), 0, HITS);
  }

  public static void clear(ManasSkillInstance i) {
    i.getOrCreateTag().putInt(REMAINING, 0);
    i.getOrCreateTag().putLong(BATCH, i.getOrCreateTag().getLong(BATCH) + 1);
    i.markDirty();
  }

  public static void press(ManasSkillInstance i, LivingEntity e) {
    if (!(e.level() instanceof ServerLevel level)
        || i.getMastery() < 0
        || i.onCoolDown(0)
        || !i.canInteractSkill(e)
        || !i.canActivateSkill(e, 0)) return;
    var energy = TensuraStorages.getExistenceFrom(e);
    boolean tyche = i.getSkill() == HeavenlyRestriction.TYCHE.get();
    double cost = SchemaArt.cost(e, tyche ? TycheConfig.DIVIDEND_COST.get() : COST);
    if (!Double.isFinite(cost) || energy.getMagicule() < cost) return;
    energy.setMagicule(energy.getMagicule() - cost);
    energy.markDirty();
    clear(i);
    i.getOrCreateTag().putInt(REMAINING, HITS);
    i.setCoolDown(COOLDOWN, 0);
    i.addMasteryPoint(e);
    if (tyche) Tyche.visual(e, ArtVisualEntity.TYCHE_DIVIDEND, 1.4, 40);
    SkillAPI.getSkillsFrom(e).checkAndMarkDirty(i);
    level.sendParticles(
        ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY() + 1, e.getZ(), 18, .4, .6, .4, .05);
    level.playSound(
        null, e.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .6f, 1.25f);
  }

  private static void melee(CriticalHitEvent event) {
    var owner = event.getEntity();
    var i = instance(owner).filter(s -> s.canInteractSkill(owner));
    if (i.isEmpty()
        || !(event.getTarget() instanceof LivingEntity target)
        || !RestrictionData.canHarm(owner, target)
        || Tyche.protectedFrom(owner, target)) return;
    if (remaining(i.get()) > 0 && !event.isCriticalHit()) {
      event.setCriticalHit(true);
      event.setDamageMultiplier(
          Math.max(
              event.getDamageMultiplier(),
              (float)
                  Math.min(
                      Float.MAX_VALUE,
                      owner.getAttributeValue(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER))));
    }
    if (event.isCriticalHit()) UnboundCritical.noteCritical(owner, event.getTarget());
  }

  private static final class Hit {
    final Hit previous;
    final LivingEntity owner, target;
    final DamageSource source;
    final ManasSkillInstance skill;
    final float hp, absorption;
    final double shp;
    final long dummy, batch;
    final boolean reserved;
    boolean critical, boosted, landed;
    int exponent;

    Hit(
        Hit previous,
        LivingEntity owner,
        LivingEntity target,
        DamageSource source,
        ManasSkillInstance skill,
        boolean nativeHit) {
      this.previous = previous;
      this.owner = owner;
      this.target = target;
      this.source = source;
      this.skill = skill;
      hp = target.getHealth();
      absorption = target.getAbsorptionAmount();
      shp = TensuraStorages.getExistenceFrom(target).getSpiritualHealth();
      dummy = stamp(target);
      batch = skill.getOrCreateTag().getLong(BATCH);
      reserved = remaining(skill) > 0;
      if (reserved) skill.getOrCreateTag().putInt(REMAINING, remaining(skill) - 1);
      // Unbound also crits ability/projectile damage later in the native calculation.
      // Knowing that guarantee here prevents adding a second base multiplier first.
      critical =
          UnboundCritical.meleeCritical(owner, target, source)
              || nativeHit && UnboundCritical.enabled(owner);
    }

    boolean accepted() {
      return landed
          || target.getHealth() < hp
          || target.getAbsorptionAmount() < absorption
          || TensuraStorages.getExistenceFrom(target).getSpiritualHealth() < shp
          || target instanceof TrainingDummyEntity d
              && d.lastDamageTaken > 0
              && stamp(target) != dummy;
    }
  }

  private static long stamp(LivingEntity e) {
    return e instanceof TrainingDummyEntity d
        ? ((long) d.lastDamageTick << 32) | Integer.toUnsignedLong(d.damageNumber)
        : 0;
  }

  private static Hit find(LivingEntity owner, LivingEntity target, DamageSource source) {
    for (var hit = CURRENT.get(); hit != null; hit = hit.previous)
      if (hit.owner == owner && hit.target == target && hit.source == source) return hit;
    return null;
  }

  public static boolean criticalOrGuaranteed(
      LivingEntity owner, LivingEntity target, DamageSource source) {
    var hit = find(owner, target, source);
    return hit != null && (hit.critical || hit.reserved);
  }

  public static boolean observe(LivingEntity target, DamageSource source, BooleanSupplier damage) {
    return Tyche.guard(target, source, () -> observe(target, source, damage, true));
  }

  public static boolean observeSpiritual(
      LivingEntity target, DamageSource source, BooleanSupplier damage) {
    return Tyche.guard(target, source, () -> observe(target, source, damage, false));
  }

  private static boolean observe(
      LivingEntity target, DamageSource source, BooleanSupplier damage, boolean nativeHit) {
    if (source == null) return damage.getAsBoolean();
    var owner = RestrictionData.attacker(source);
    if (target.level().isClientSide()
        || owner == null
        || !RestrictionData.canHarm(owner, target)
        || Tyche.protectedFrom(owner, target)) return damage.getAsBoolean();
    var skill = instance(owner).filter(i -> i.canInteractSkill(owner));
    if (skill.isEmpty()
        || !skill.get().isToggled() && remaining(skill.get()) == 0 && !Tyche.house(owner)
        || find(owner, target, source) != null) return damage.getAsBoolean();
    var hit = new Hit(CURRENT.get(), owner, target, source, skill.get(), nativeHit);
    CURRENT.set(hit);
    boolean finished = false;
    try {
      boolean result = damage.getAsBoolean();
      finished = hit.critical && hit.boosted && hit.accepted();
      if (finished) {
        hit.skill.addMasteryPoint(owner);
        report(hit);
        if (hit.skill.getSkill() == HeavenlyRestriction.CRITICAL_MERCHANT.get()
            && !hit.skill.isTemporarySkill()) TycheSynthesis.critical(owner);
      }
      return result;
    } finally {
      if (!finished && hit.reserved && hit.skill.getOrCreateTag().getLong(BATCH) == hit.batch)
        hit.skill.getOrCreateTag().putInt(REMAINING, Math.min(HITS, remaining(hit.skill) + 1));
      if (hit.reserved || finished) {
        hit.skill.markDirty();
        SkillAPI.getSkillsFrom(owner).checkAndMarkDirty(hit.skill);
      }
      if (hit.previous == null) CURRENT.remove();
      else CURRENT.set(hit.previous);
    }
  }

  /** Called by the native crit visual helper, after native chance resolution succeeds. */
  public static void nativeCritical(Entity target, Entity owner) {
    for (var hit = CURRENT.get(); hit != null; hit = hit.previous)
      if (hit.owner == owner && hit.target == target) {
        hit.critical = true;
        return;
      }
  }

  private static float apply(Hit hit, float amount) {
    if (hit == null || hit.boosted || !(amount > 0)) return amount;
    if (!hit.critical && hit.reserved) {
      amount =
          CriticalPayout.multiply(
              amount,
              Math.max(
                  1, hit.owner.getAttributeValue(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER)));
      hit.critical = true;
      ManasCoreAttributeUtils.triggerCriticalAttackEffect(hit.target, hit.owner);
    }
    if (!hit.critical) return amount;
    hit.boosted = true;
    boolean tyche = hit.skill.getSkill() == HeavenlyRestriction.TYCHE.get();
    if (hit.skill.isToggled() || tyche && hit.reserved) {
      hit.exponent =
          tyche
              ? TychePayout.roll(hit.owner.getRandom()::nextBoolean, hit.reserved)
              : CriticalPayout.doublings(hit.owner.getRandom()::nextBoolean);
      amount = CriticalPayout.multiply(amount, Math.scalb(1d, hit.exponent));
    }
    return amount;
  }

  private static void incoming(LivingIncomingDamageEvent event) {
    var owner = RestrictionData.attacker(event.getSource());
    event.setAmount(apply(find(owner, event.getEntity(), event.getSource()), event.getAmount()));
  }

  public static float spiritual(LivingEntity target, DamageSource source, float amount) {
    return source == null
        ? amount
        : apply(find(RestrictionData.attacker(source), target, source), amount);
  }

  private static void finiteDamage(LivingDamageEvent.Pre event) {
    var hit =
        find(RestrictionData.attacker(event.getSource()), event.getEntity(), event.getSource());
    if (hit != null && hit.boosted && !Float.isFinite(event.getNewDamage()))
      event.setNewDamage(CriticalPayout.finite(event.getNewDamage()));
  }

  private static void landed(LivingDamageEvent.Post event) {
    var hit =
        find(RestrictionData.attacker(event.getSource()), event.getEntity(), event.getSource());
    if (hit != null && event.getNewDamage() > 0) hit.landed = true;
  }

  private static void report(Hit hit) {
    if (!(hit.owner.level() instanceof ServerLevel level)) return;
    var at = hit.target.getBoundingBox().getCenter();
    int count = Math.min(24, 5 + hit.exponent * 2);
    level.sendParticles(
        new DustParticleOptions(new Vector3f(1, .72f, .16f), 1.1f),
        at.x,
        at.y,
        at.z,
        count,
        .35,
        .4,
        .35,
        .02);
    if (hit.owner instanceof ServerPlayer player)
      player.displayClientMessage(
          Component.translatable(
              hit.skill.getSkill() == HeavenlyRestriction.TYCHE.get()
                  ? "heavenlyrestriction.tyche.payout"
                  : "heavenlyrestriction.critical_merchant.payout",
              hit.exponent == 0
                  ? "1"
                  : hit.exponent <= 10 ? Long.toString(1L << hit.exponent) : "2^" + hit.exponent,
              remaining(hit.skill)),
          true);
  }

  private CriticalMerchant() {}
}
