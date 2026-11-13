package dev.heavenlyrestriction.tyche;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.critical.CriticalMerchant;
import io.github.manasmods.manascore.attribute.api.ManasCoreAttributes;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.unique.TunerSkill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class Tyche {
  // Keep existing native cooldown indices: 0 Dividend, 1 Odds, 2 Insurance.
  public static final int ODDS_MODE = 1;
  public static final String ODDS_TICKS = "TycheOddsTicks";
  private static final ResourceLocation BONUS = HeavenlyRestriction.id("tyche_adversity");

  public static Optional<ManasSkillInstance> owned(LivingEntity e) {
    return e == null
        ? Optional.empty()
        : SkillAPI.getSkillsFrom(e)
            .getSkill(HeavenlyRestriction.TYCHE.get())
            .filter(i -> i.getMastery() >= 0);
  }

  /** Drop the removed field and its cooldown without touching Dividend or Insurance. */
  public static void migrate(ManasSkillInstance i) {
    if (i.getOrCreateTag().contains("TycheHouseTicks")) {
      i.getOrCreateTag().remove("TycheHouseTicks");
      i.setCoolDown(0, ODDS_MODE);
      i.markDirty();
    }
  }

  public static int oddsTicks(ManasSkillInstance i) {
    return Math.max(0, i.getOrCreateTag().getInt(ODDS_TICKS));
  }

  public static void register() {
    NeoForge.EVENT_BUS.addListener(Tyche::tick);
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
              advanceOdds(i, e);
            });
    if (e.tickCount % 5 == 0) refresh(e);
    if (e instanceof ServerPlayer p && e.tickCount % 20 == 0) TycheSynthesis.discover(p);
  }

  public static void advanceOdds(ManasSkillInstance i, LivingEntity e) {
    if (e.level().isClientSide()) return;
    migrate(i);
    int left = oddsTicks(i);
    if (left > 0) {
      if (!i.isToggled() || !i.canInteractSkill(e) || !e.isAlive()) left = 1;
      i.getOrCreateTag().putInt(ODDS_TICKS, left - 1);
      if (left % 20 == 0 || left == 1) i.markDirty();
      if (left == 1) refresh(e);
    }
    checkOdds(e);
  }

  /** Called after both physical and direct spiritual damage, with a tick fallback. */
  public static void checkOdds(LivingEntity e) {
    if (e == null || e.level().isClientSide() || !e.isAlive()) return;
    owned(e)
        .ifPresent(
            i -> {
              migrate(i);
              if (!i.isToggled()
                  || !i.canInteractSkill(e)
                  || oddsTicks(i) > 0
                  || i.onCoolDown(ODDS_MODE)) return;
              var energy = TensuraStorages.getExistenceFrom(e);
              if (!TychePayout.atHalf(e.getHealth(), e.getMaxHealth())
                  && !TychePayout.atHalf(
                      energy.getSpiritualHealth(),
                      e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH))) return;
              i.getOrCreateTag().putInt(ODDS_TICKS, TycheConfig.ODDS_DURATION.get() * 20);
              i.setCoolDown(TycheConfig.ODDS_COOLDOWN.get(), ODDS_MODE);
              i.addMasteryPoint(e);
              i.markDirty();
              refresh(e);
              visual(e, ArtVisualEntity.TYCHE_DIVIDEND, 1.4, 40);
              if (e instanceof ServerPlayer p)
                p.displayClientMessage(
                    Component.translatable(
                        "heavenlyrestriction.tyche.odds", TycheConfig.ODDS_DURATION.get()),
                    true);
            });
  }

  public static void refresh(LivingEntity e) {
    var instance=io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(e).getSkill(net.minecraft.resources.ResourceLocation.parse("heavenlyrestriction:tyche"));
    if(instance.isPresent())try(var equations=dev.heavenlyrestriction.laplace.expand.ExpandedRuntime.open(e,instance.get(),-1)){refreshNative(e);return;}
    refreshNative(e);
  }
  private static void refreshNative(LivingEntity e){
    if (e.level().isClientSide()) return;
    boolean active =
        owned(e)
            .filter(i -> oddsTicks(i) > 0 && i.isToggled() && i.canInteractSkill(e))
            .isPresent();
    modifier(
        e,
        Attributes.ATTACK_DAMAGE,
        active ? .5 : 0,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    modifier(
        e,
        TensuraAttributes.AUTO_MELEE_DODGE_CHANCE,
        active ? 25 : 0,
        AttributeModifier.Operation.ADD_VALUE);
    modifier(
        e,
        TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE,
        active ? 25 : 0,
        AttributeModifier.Operation.ADD_VALUE);
    // Native mastered Tuner multiplies its configured bonuses by two. Add missing categories
    // once: attack/swim bonuses are 50% stronger, movement only 10% stronger.
    modifier(
        e,
        Attributes.ATTACK_SPEED,
        active ? TunerSkill.CONFIG.bonusAttackSpeed * 3 : 0,
        AttributeModifier.Operation.ADD_VALUE);
    modifier(
        e,
        Attributes.MOVEMENT_SPEED,
        active ? TunerSkill.CONFIG.bonusSpeed * 2.2 : 0,
        AttributeModifier.Operation.ADD_VALUE);
    modifier(
        e,
        ManasCoreAttributes.SWIM_SPEED_MULTIPLIER,
        active ? TunerSkill.CONFIG.bonusSwim * 3 : 0,
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

  public static void clearOdds(ManasSkillInstance i, LivingEntity e) {
    if (e.level().isClientSide()) return;
    i.getOrCreateTag().putInt(ODDS_TICKS, 0);
    for (var type :
        List.of(
            Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED,
            Attributes.MOVEMENT_SPEED,
            ManasCoreAttributes.SWIM_SPEED_MULTIPLIER,
            TensuraAttributes.AUTO_MELEE_DODGE_CHANCE,
            TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE))
      modifier(e, type, 0, AttributeModifier.Operation.ADD_VALUE);
    i.markDirty();
  }

  public static void clear(ManasSkillInstance i, LivingEntity e) {
    migrate(i);
    clearOdds(i, e);
    CriticalMerchant.clear(i);
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
