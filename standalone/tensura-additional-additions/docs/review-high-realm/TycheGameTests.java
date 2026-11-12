package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.critical.*;
import dev.heavenlyrestriction.tyche.*;
import io.github.manasmods.manascore.attribute.api.ManasCoreAttributes;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.unique.TunerSkill;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("tychereview")
@PrefixGameTestTemplate(false)
public final class TycheGameTests {
  static ManasSkillInstance learn(ServerPlayer p, ManasSkill s, boolean master) {
    return SplitMindGameTests.learn(p, s, master);
  }

  static ManasSkillInstance tyche(ServerPlayer p) {
    var i = learn(p, HeavenlyRestriction.TYCHE.get(), false);
    p.setHealth(p.getMaxHealth());
    TensuraStorages.getExistenceFrom(p)
        .setSpiritualHealth(p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));
    i.onToggleOn(p);
    return i;
  }

  static void near(GameTestHelper h, double a, double b, String label) {
    h.assertTrue(Math.abs(a - b) < .01, label + ": " + a + " / " + b);
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void insuranceFullyRestoresAndClearsOtherCooldownsButPreservesOwnClock(
      GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    var other = learn(p, UniqueSkills.GREAT_SAGE.get(), false);
    other.setCoolDowns(99);
    i.setCoolDown(20, 0);
    i.setCoolDown(90, 1);
    var energy = TensuraStorages.getExistenceFrom(p);
    energy.setAura(1);
    energy.setMagicule(1);
    energy.setSpiritualHealth(1);
    p.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
    p.setHealth(0);
    h.assertTrue(FateInsurance.revive(i, p, p.damageSources().generic()), "Revival rejected");
    near(h, p.getHealth(), p.getMaxHealth(), "HP");
    near(h, energy.getAura(), EnergyHelper.getMaxAura(p), "Aura");
    near(h, energy.getMagicule(), EnergyHelper.getMaxMagicule(p), "Magicules");
    near(
        h,
        energy.getSpiritualHealth(),
        p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),
        "SHP");
    h.assertTrue(!p.hasEffect(MobEffects.POISON), "Harmful effect retained");
    h.assertTrue(
        other.getCooldownList().stream().allMatch(n -> n == 0), "Other cooldowns retained");
    near(h, i.getCoolDown(0), 0, "Dividend cooldown");
    near(h, i.getCoolDown(1), 0, "Against All Odds cooldown");
    near(h, i.getCoolDown(2), 300, "Insurance clock");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void insuranceAllowsNewTypesWithoutExtendingTimerAndRejectsRepeats(
      GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    p.setHealth(0);
    h.assertTrue(FateInsurance.revive(i, p, p.damageSources().generic()), "First revival failed");
    i.setCoolDown(123, 2);
    p.setHealth(0);
    h.assertTrue(!FateInsurance.revive(i, p, p.damageSources().generic()), "Repeated type revived");
    h.assertTrue(
        FateInsurance.revive(i, p, p.damageSources().fall()), "New type rejected during window");
    near(h, i.getCoolDown(2), 123, "Later revival extended or reset timer");
    h.assertTrue(
        i.getOrCreateTag().getCompound(FateInsurance.HISTORY).size() == 2, "Wrong history");
    i.setCoolDown(0, 2);
    p.setHealth(0);
    h.assertTrue(
        FateInsurance.revive(i, p, p.damageSources().generic()), "Expired type remained locked");
    near(h, i.getCoolDown(2), 300, "Fresh window");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void insuranceHistorySurvivesInstanceSaveLoadAndIgnoresVoidAndKill(
      GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    p.setHealth(0);
    h.assertTrue(FateInsurance.revive(i, p, p.damageSources().generic()), "First failed");
    var saved = i.serialize(new CompoundTag());
    var copy = HeavenlyRestriction.TYCHE.get().createDefaultInstance();
    copy.deserialize(saved);
    h.assertTrue(copy.getCoolDown(2) == 300, "Native serialization lost insurance clock");
    p.setHealth(0);
    h.assertTrue(
        !FateInsurance.revive(copy, p, p.damageSources().generic()), "Reload lost lockout");
    h.assertTrue(
        !FateInsurance.revive(copy, p, p.damageSources().fellOutOfWorld()), "Void revived");
    h.assertTrue(!FateInsurance.revive(copy, p, p.damageSources().genericKill()), "Kill revived");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void actualFatalHitDispatchesNativeUltimateRevival(GameTestHelper h) {
    var observer = player(h);
    var p = target(h, observer.position().add(4, 0, 0));
    RestrictionData.learnReward(p, HeavenlyRestriction.TYCHE.get());
    var i = SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.TYCHE.get()).orElseThrow();
    i.setMastery(0);
    afterTracking(
        h,
        List.of(p),
        () -> {
          p.setHealth(1);
          p.invulnerableTime = 0;
          boolean hit = p.hurt(p.damageSources().generic(), 100000000);
          h.assertTrue(
              p.isAlive() && p.getHealth() == p.getMaxHealth(),
              "Native fatal event: hp="
                  + p.getHealth()
                  + " max="
                  + p.getMaxHealth()
                  + " hit="
                  + hit
                  + " timer="
                  + i.getCoolDown(2));
          h.assertTrue(i.onCoolDown(2), "Native death callback did not start insurance");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void dividendPaysMagiculesOnceRefreshesTenAndRefundsBlockedHit(GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    var energy = TensuraStorages.getExistenceFrom(p);
    double before = energy.getMagicule();
    CriticalMerchant.press(i, p);
    near(h, before - energy.getMagicule(), TycheConfig.DIVIDEND_COST.get(), "Dividend cost");
    h.assertTrue(CriticalMerchant.remaining(i) == 10, "Wrong guarantee count");
    var t = target(h, p.position().add(0, 0, 2));
    CriticalMerchant.observe(t, p.damageSources().playerAttack(p), () -> false);
    h.assertTrue(CriticalMerchant.remaining(i) == 10, "Failed hit consumed guarantee");
    h.assertTrue(i.getCoolDown(0) == 20, "Wrong dividend cooldown");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void oddsAtHalfHpGrantsFullNonDuplicatedBuffsAndPersistsThroughHealing(
      GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
    p.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4);
    p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.1);
    p.getAttribute(ManasCoreAttributes.SWIM_SPEED_MULTIPLIER).setBaseValue(1);
    p.getAttribute(TensuraAttributes.AUTO_MELEE_DODGE_CHANCE).setBaseValue(0);
    p.getAttribute(TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE).setBaseValue(0);
    p.setHealth(p.getMaxHealth() * .5f);
    Tyche.checkOdds(p);
    near(
        h,
        p.getAttributeValue(Attributes.ATTACK_DAMAGE),
        15,
        "Full damage without duplicate flat bonus");
    near(
        h,
        p.getAttributeValue(TensuraAttributes.AUTO_MELEE_DODGE_CHANCE),
        25,
        "Melee dodge duplicated");
    near(
        h,
        p.getAttributeValue(TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE),
        25,
        "Projectile dodge duplicated");
    near(
        h,
        p.getAttributeValue(Attributes.ATTACK_SPEED),
        4 + TunerSkill.CONFIG.bonusAttackSpeed * 3,
        "Attack speed stronger than mastered Tuner");
    near(
        h,
        p.getAttributeValue(Attributes.MOVEMENT_SPEED),
        .1 + TunerSkill.CONFIG.bonusSpeed * 2.2,
        "Slight movement increase");
    near(
        h,
        p.getAttributeValue(ManasCoreAttributes.SWIM_SPEED_MULTIPLIER),
        1 + TunerSkill.CONFIG.bonusSwim * 3,
        "Swim speed");
    h.assertTrue(
        Tyche.oddsTicks(i) == TycheConfig.ODDS_DURATION.get() * 20
            && i.getCoolDown(1) == TycheConfig.ODDS_COOLDOWN.get(),
        "Wrong duration/cooldown");
    p.setHealth(p.getMaxHealth());
    Tyche.refresh(p);
    Tyche.refresh(p);
    near(
        h,
        p.getAttributeValue(Attributes.ATTACK_DAMAGE),
        15,
        "Healing canceled timed buff or refresh stacked it");
    Tyche.advanceOdds(i, p);
    Tyche.checkOdds(p);
    h.assertTrue(
        Tyche.oddsTicks(i) == TycheConfig.ODDS_DURATION.get() * 20 - 1, "Check extended duration");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void oddsAtHalfShpTriggersWithFullPhysicalHealth(GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    var energy = TensuraStorages.getExistenceFrom(p);
    var max = p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);
    h.assertTrue(max > 0, "Fixture lacks SHP capacity");
    energy.setSpiritualHealth(max * .5001);
    Tyche.checkOdds(p);
    h.assertTrue(Tyche.oddsTicks(i) == 0, "Above half triggered");
    energy.setSpiritualHealth(max * .5);
    Tyche.checkOdds(p);
    h.assertTrue(
        Tyche.oddsTicks(i) > 0 && p.getHealth() == p.getMaxHealth(), "SHP-only trigger failed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void oddsExpiresAndCannotBypassCooldownByToggling(GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    double damage = p.getAttributeValue(Attributes.ATTACK_DAMAGE),
        speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
    p.setHealth(p.getMaxHealth() * .5f);
    Tyche.checkOdds(p);
    for (int n = 0; n < TycheConfig.ODDS_DURATION.get() * 20; n++) Tyche.advanceOdds(i, p);
    h.assertTrue(Tyche.oddsTicks(i) == 0, "Buff failed to expire");
    near(h, p.getAttributeValue(Attributes.ATTACK_DAMAGE), damage, "Damage persisted after expiry");
    near(
        h,
        p.getAttributeValue(Attributes.MOVEMENT_SPEED),
        speed,
        "Movement persisted after expiry");
    i.onToggleOff(p);
    i.onToggleOn(p);
    h.assertTrue(Tyche.oddsTicks(i) == 0 && i.onCoolDown(1), "Toggle bypassed cooldown");
    i.setCoolDown(0, 1);
    Tyche.checkOdds(p);
    h.assertTrue(Tyche.oddsTicks(i) > 0, "Low pools failed to trigger after cooldown");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void synthesisRequiresMasteryAwakeningAchievementsAndChargesAfterSettlement(
      GameTestHelper h) {
    var p = player(h);
    learn(p, HeavenlyRestriction.CRITICAL_MERCHANT.get(), true);
    learn(p, UniqueSkills.TUNER.get(), true);
    TycheSynthesis.discover(p);
    h.assertTrue(!TycheSynthesis.ready(p), "Achievements bypassed");
    RestrictionData.of(p).putInt(TycheSynthesis.CRITS, TycheConfig.CRITICALS.get());
    for (int n = 0; n < TycheConfig.SURVIVALS.get(); n++) TycheSynthesis.survived(p, "type" + n);
    h.assertTrue(TycheSynthesis.ready(p), "Qualified synthesis blocked");
    var merchant =
        SkillAPI.getSkillsFrom(p)
            .getSkill(HeavenlyRestriction.CRITICAL_MERCHANT.get())
            .orElseThrow();
    CriticalMerchant.press(merchant, p);
    merchant.setCoolDown(13, 0);
    var tuner = SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.TUNER.get()).orElseThrow();
    var history = new ListTag();
    var row = new CompoundTag();
    row.putBoolean("generic", true);
    history.add(row);
    tuner.getOrCreateTag().put("deaths", history);
    tuner.setCoolDown(143, 0);
    TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, merchant.getSkill(), 0);
    double before = p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
    h.assertTrue(TycheSynthesis.synthesize(p), "Synthesis failed");
    var result = Tyche.owned(p).orElseThrow();
    h.assertTrue(
        CriticalMerchant.remaining(result) == 10 && result.getCoolDown(0) == 13,
        "Pending guarantees or their cooldown were lost");
    h.assertTrue(
        result.getOrCreateTag().getCompound(FateInsurance.HISTORY).contains("generic")
            && result.getCoolDown(2) == 143,
        "Existing Tuner lockout was reset by synthesis");
    h.assertTrue(
        TensuraStorages.getAbilityFrom(p).getAbilitySlot(0, 0).getSkill() == result.getSkill(),
        "Native preset still references consumed parent");
    near(
        h,
        before - p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue(),
        TycheConfig.ACQUISITION.get(),
        "Wrong cost");
    h.assertTrue(
        Tyche.owned(p).isPresent()
            && SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.TUNER.get()).isEmpty()
            && SkillAPI.getSkillsFrom(p)
                .getSkill(HeavenlyRestriction.CRITICAL_MERCHANT.get())
                .isEmpty(),
        "Parents not replaced");
    h.assertTrue(!TycheSynthesis.synthesize(p), "Repeated synthesis");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void removedHouseMigratesWithoutTouchingInsuranceAndCannotBeCast(GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    i.getOrCreateTag().putInt("TycheHouseTicks", 250);
    i.setCoolDown(90, 1);
    i.setCoolDown(13, 0);
    i.setCoolDown(123, 2);
    var history = new CompoundTag();
    history.putBoolean("generic", true);
    i.getOrCreateTag().put(FateInsurance.HISTORY, history);
    Tyche.migrate(i);
    Tyche.refresh(p);
    h.assertTrue(
        !i.getOrCreateTag().contains("TycheHouseTicks") && i.getCoolDown(1) == 0,
        "Legacy field/cooldown retained");
    h.assertTrue(
        i.getCoolDown(0) == 13
            && i.getCoolDown(2) == 123
            && i.getOrCreateTag().getCompound(FateInsurance.HISTORY).contains("generic"),
        "Unrelated cooldown/history modified");
    var energy = TensuraStorages.getExistenceFrom(p);
    double mp = energy.getMagicule();
    i.onPressed(p, 0, 1);
    near(h, energy.getMagicule(), mp, "Removed active charged magicules");
    h.assertTrue(Tyche.oddsTicks(i) == 0, "Press manually triggered passive");
    h.assertTrue(
        HeavenlyRestriction.TYCHE.get().nextMode(p, i, 0, false) == 0
            && HeavenlyRestriction.TYCHE.get().nextMode(p, i, 0, true) == 0,
        "Removed active still cycles");
    var t = target(h, p.position().add(2, 0, 0));
    var source = p.damageSources().playerAttack(p);
    CriticalMerchant.observe(
        t,
        source,
        () -> {
          h.assertTrue(
              !((TensuraDamageSource) source).tensura$isDodgeBypass(),
              "Removed dodge bypass remains");
          return false;
        });
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void oddsSerializationAndToggleCleanupRetainCooldown(GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    p.setHealth(p.getMaxHealth() * .5f);
    Tyche.checkOdds(p);
    var copy = HeavenlyRestriction.TYCHE.get().createDefaultInstance();
    copy.deserialize(i.serialize(new CompoundTag()));
    h.assertTrue(
        Tyche.oddsTicks(copy) == Tyche.oddsTicks(i) && copy.getCoolDown(1) == i.getCoolDown(1),
        "Native save lost passive timers");
    i.onToggleOff(p);
    h.assertTrue(Tyche.oddsTicks(i) == 0 && i.onCoolDown(1), "Toggle cleanup erased cooldown");
    h.assertTrue(
        p.getAttribute(Attributes.ATTACK_DAMAGE)
                    .getModifier(HeavenlyRestriction.id("tyche_adversity"))
                == null
            && p.getAttribute(Attributes.ATTACK_SPEED)
                    .getModifier(HeavenlyRestriction.id("tyche_adversity"))
                == null,
        "Toggle retained modifiers");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void physicalAndSpiritualDamageTransactionsCheckThresholdImmediately(
      GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    CriticalMerchant.observe(
        p,
        p.damageSources().generic(),
        () -> {
          p.setHealth(p.getMaxHealth() * .5f);
          return true;
        });
    h.assertTrue(Tyche.oddsTicks(i) > 0, "Physical damage path missed threshold");
    Tyche.clearOdds(i, p);
    i.setCoolDown(0, 1);
    p.setHealth(p.getMaxHealth());
    CriticalMerchant.observeSpiritual(
        p,
        p.damageSources().generic(),
        () -> {
          TensuraStorages.getExistenceFrom(p)
              .setSpiritualHealth(p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH) * .5);
          return true;
        });
    h.assertTrue(Tyche.oddsTicks(i) > 0, "Spiritual damage path missed threshold");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void learningClickCannotBypassSynthesisAndInsuranceIsNotAnActiveMode(
      GameTestHelper h) {
    var p = player(h);
    learn(p, UniqueSkills.TUNER.get(), false);
    TycheSynthesis.discover(p);
    var entry = SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.TYCHE.get()).orElseThrow();
    h.assertTrue(entry.getMastery() < 0, "Discovery granted learned Ultimate");
    h.assertTrue(
        !HeavenlyRestriction.TYCHE.get().addLearnPoint(entry, p, 0, 1000000),
        "Learning bypassed synthesis");
    h.assertTrue(
        HeavenlyRestriction.TYCHE.get().nextMode(p, entry, 1, false) == 0,
        "Insurance exposed as active mode");
    h.succeed();
  }
}
