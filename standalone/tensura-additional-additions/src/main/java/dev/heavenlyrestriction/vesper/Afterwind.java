package dev.heavenlyrestriction.vesper;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.exponentiation.ExponentiationContext;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

/** A single Wind-to-sword combo, committed only after real HP/SHP/absorption damage. */
public final class Afterwind {
  public static final ResourceKey<Enchantment> ENGRAVING =
      ResourceKey.create(Registries.ENCHANTMENT, HeavenlyRestriction.id("afterwind"));
  public static final ResourceKey<DamageType> DAMAGE =
      ResourceKey.create(Registries.DAMAGE_TYPE, HeavenlyRestriction.id("afterwind"));
  public static final int MARK_TICKS = 100, COOLDOWN_TICKS = 60, DELAY_TICKS = 4;
  private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
  private static final ThreadLocal<Map<LivingEntity, Set<LivingEntity>>> OBSERVING =
      ThreadLocal.withInitial(IdentityHashMap::new);

  /** Native spiritual helpers can run inside hurt; commit only the outer transaction. */
  public static boolean observe(
      LivingEntity target, DamageSource source, java.util.function.BooleanSupplier damage) {
    if (source == null) return damage.getAsBoolean();
    var hit = before(target, source);
    if (hit == null) return damage.getAsBoolean();
    var map = OBSERVING.get();
    var owners =
        map.computeIfAbsent(target, k -> Collections.newSetFromMap(new IdentityHashMap<>()));
    if (!owners.add(hit.owner)) return damage.getAsBoolean();
    try {
      boolean result = damage.getAsBoolean();
      after(target, hit);
      return result;
    } finally {
      owners.remove(hit.owner);
      if (owners.isEmpty()) map.remove(target);
      if (map.isEmpty()) OBSERVING.remove();
    }
  }

  private static final class State {
    final LinkedHashMap<UUID, Long> marks = new LinkedHashMap<>();
    long ready;
    Cut cut;
  }

  private record Cut(LivingEntity target, float damage, long due, ServerLevel level) {}

  public record Hit(
      LivingEntity owner,
      float health,
      double shp,
      float absorption,
      float attack,
      int level,
      boolean wind,
      boolean strike) {}

  public static int level(LivingEntity owner) {
    if (owner == null || !(owner.getMainHandItem().getItem() instanceof VesperItem)) return 0;
    VesperItem.engrave(owner.getMainHandItem(), owner.level().registryAccess());
    var holder =
        owner
            .level()
            .registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(ENGRAVING);
    return EnchantmentHelper.getItemEnchantmentLevel(holder, owner.getMainHandItem());
  }

  public static double coefficient(int level) {
    return level < 1 ? 0 : .15 + .05 * level;
  }

  public static Hit before(LivingEntity target, DamageSource source) {
    if (target.level().isClientSide() || source.is(DAMAGE)) return null;
    var owner = RestrictionData.attacker(source);
    int level = level(owner);
    if (level < 1 || owner == target || !RestrictionData.canHarm(owner, target)) return null;
    var tagged = (TensuraDamageSource) source;
    boolean wind =
        tagged.tensura$getElement() == Element.WIND
            || source.is(
                ResourceKey.create(
                    Registries.DAMAGE_TYPE, ResourceLocation.parse("tensura:wind_elemental")))
            || source.is(
                ResourceKey.create(
                    Registries.DAMAGE_TYPE, ResourceLocation.parse("tensura:wind_breath")))
            || source.is(
                ResourceKey.create(
                    Registries.DAMAGE_TYPE, ResourceLocation.parse("tensura:tornado")));
    var ability = tagged.tensura$getAbilityInstance();
    boolean strike =
        source.getDirectEntity() == owner
            && source.getEntity() == owner
            && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))
            && (ability == null || ability.getSkill() instanceof ChainedSkill);
    if (!wind && !strike) return null;
    float attack;
    try (var raw = ExponentiationContext.raw(owner, ExponentiationContext.DAMAGE)) {
      attack = (float) Math.max(0, owner.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }
    return new Hit(
        owner,
        target.getHealth(),
        TensuraStorages.getExistenceFrom(target).getSpiritualHealth(),
        target.getAbsorptionAmount(),
        attack,
        level,
        wind,
        strike);
  }

  public static void after(LivingEntity target, Hit hit) {
    if (hit == null || !hit.owner.isAlive() || level(hit.owner) < 1 || !target.isAlive()) return;
    boolean damaged =
        target.getHealth() < hit.health - .0001f
            || TensuraStorages.getExistenceFrom(target).getSpiritualHealth() < hit.shp - .0001
            || target.getAbsorptionAmount() < hit.absorption - .0001f;
    if (!damaged) return;
    var owner = hit.owner;
    long now = owner.level().getGameTime();
    var state = STATES.computeIfAbsent(owner, k -> new State());
    state.marks.values().removeIf(expiry -> expiry < now);
    // A wind-transformed melee strike can arm the next strike, but never its own aftercut.
    if (hit.strike
        && state.cut == null
        && now >= state.ready
        && state.marks.getOrDefault(target.getUUID(), -1L) >= now) {
      state.marks.remove(target.getUUID());
      state.ready = now + COOLDOWN_TICKS;
      state.cut =
          new Cut(
              target,
              (float) (hit.attack * coefficient(hit.level)),
              now + DELAY_TICKS,
              (ServerLevel) owner.level());
    }
    if (hit.wind && now >= state.ready) {
      state.marks.put(target.getUUID(), now + MARK_TICKS);
      while (state.marks.size() > 16) state.marks.remove(state.marks.keySet().iterator().next());
    }
  }

  public static DamageSource source(LivingEntity owner) {
    var source =
        new DamageSource(
            owner
                .level()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DAMAGE),
            owner);
    ((TensuraDamageSource) source).tensura$setElement(Element.WIND);
    return source;
  }

  public static void tick(ServerTickEvent.Post event) {
    for (var entry : List.copyOf(STATES.entrySet())) {
      var owner = entry.getKey();
      var state = entry.getValue();
      if (owner == null || owner.isRemoved() || !owner.isAlive() || level(owner) < 1) {
        STATES.remove(owner);
        continue;
      }
      long now = owner.level().getGameTime();
      state.marks.values().removeIf(expiry -> expiry < now);
      var cut = state.cut;
      if (cut != null && now >= cut.due) {
        state.cut = null;
        var target = cut.target;
        if (owner.level() == cut.level
            && target.level() == cut.level
            && target.isAlive()
            && !target.isRemoved()
            && owner.distanceToSqr(target) <= 144
            && RestrictionData.canHarm(owner, target)
            && cut.damage > 0) {
          target.hurt(source(owner), cut.damage);
          visual(cut.level, owner, target);
        }
      }
      if (state.cut == null && state.marks.isEmpty() && now >= state.ready) STATES.remove(owner);
    }
  }

  private static void visual(ServerLevel level, LivingEntity owner, LivingEntity target) {
    var center = target.getBoundingBox().getCenter();
    var forward = center.subtract(owner.getEyePosition()).normalize();
    var across = new Vec3(-forward.z, 0, forward.x).normalize();
    if (across.lengthSqr() < .01) across = new Vec3(1, 0, 0);
    var dust = new DustParticleOptions(new Vector3f(.78f, .9f, .94f), .85f);
    for (int n = 0; n < 13; n++) {
      double t = n / 12d;
      var at =
          center.add(across.scale((t - .5) * 2.8)).add(0, Math.sin(t * Math.PI) * .65 - .25, 0);
      level.sendParticles(dust, at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }
    level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0, 0, 0, 0);
    level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 5, .35, .4, .35, .02);
    level.playSound(
        null,
        target.blockPosition(),
        SoundEvents.PLAYER_ATTACK_SWEEP,
        SoundSource.PLAYERS,
        .7f,
        1.35f);
  }

  public static void clear() {
    STATES.clear();
    OBSERVING.remove();
  }

  private Afterwind() {}
}
