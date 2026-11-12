package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.critical.*;
import dev.heavenlyrestriction.tyche.*;
import io.github.manasmods.manascore.skill.api.*;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("tychereview")
@PrefixGameTestTemplate(false)
public final class TycheGameTests {
  static ManasSkillInstance learn(ServerPlayer p, ManasSkill s, boolean master) {
    return SplitMindGameTests.learn(p, s, master);
  }

  static ManasSkillInstance tyche(ServerPlayer p) {
    var i = learn(p, HeavenlyRestriction.TYCHE.get(), false);
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
    near(h, i.getCoolDown(1), 0, "House cooldown");
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
  public static void adversityTracksCurrentHealthAndDoesNotStackOrRemainAfterToggle(
      GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
    p.setHealth(p.getMaxHealth() * .25f);
    Tyche.refresh(p);
    near(h, p.getAttributeValue(Attributes.ATTACK_DAMAGE), 12.5, "Low HP damage");
    Tyche.refresh(p);
    near(h, p.getAttributeValue(Attributes.ATTACK_DAMAGE), 12.5, "Bonus stacked");
    i.onToggleOff(p);
    near(h, p.getAttributeValue(Attributes.ATTACK_DAMAGE), 10, "Toggle retained bonus");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void housePaysOnceAndCriticalProtectionHasRangeAndOwnership(GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    var energy = TensuraStorages.getExistenceFrom(p);
    double before = energy.getMagicule();
    h.assertTrue(Tyche.housePress(i, p), "House rejected");
    near(h, before - energy.getMagicule(), TycheConfig.HOUSE_COST.get(), "House cost");
    h.assertTrue(!Tyche.housePress(i, p), "Recast ignored cooldown");
    var enemy = target(h, p.position().add(2, 0, 0));
    h.assertTrue(Tyche.protectedFrom(enemy, p), "Nearby enemy not protected against");
    enemy.setPos(p.position().add(30, 0, 0));
    h.assertTrue(!Tyche.protectedFrom(enemy, p), "House affected distant enemy");
    Tyche.clear(i, p);
    h.assertTrue(!Tyche.house(p), "Cleanup retained house");
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
  public static void houseSuppressesNativeMeleeCriticalsEvenAgainstGuaranteedHits(
      GameTestHelper h) {
    var p = player(h);
    var defense = tyche(p);
    h.assertTrue(Tyche.housePress(defense, p), "House rejected");
    var enemy = player(h);
    enemy.setPos(p.position().add(2, 0, 0));
    var offense = learn(enemy, HeavenlyRestriction.CRITICAL_MERCHANT.get(), false);
    CriticalMerchant.press(offense, enemy);
    // NeoForge FakePlayer deliberately forbids harming players. House must honor that.
    h.assertTrue(!Tyche.protectedFrom(enemy, p), "House ignored fake-player PvP protection");
    var defender = target(h, enemy.position().add(2, 0, 0));
    RestrictionData.learnReward(defender, HeavenlyRestriction.TYCHE.get());
    var mobSkill =
        SkillAPI.getSkillsFrom(defender).getSkill(HeavenlyRestriction.TYCHE.get()).orElseThrow();
    mobSkill.setMastery(0);
    defender.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(1000000);
    TensuraStorages.getExistenceFrom(defender).setMagicule(1000000);
    h.assertTrue(Tyche.housePress(mobSkill, defender), "Mob House rejected");
    h.assertTrue(Tyche.protectedFrom(enemy, defender), "Native hostile fixture not protected");
    var event = new CriticalHitEvent(enemy, defender, 2.5f, true);
    NeoForge.EVENT_BUS.post(event);
    h.assertTrue(
        !event.isCriticalHit() && event.getDamageMultiplier() == 1,
        "Native critical survived House");
    h.assertTrue(CriticalMerchant.remaining(offense) == 10, "Suppressed crit consumed a guarantee");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void houseDodgeBypassOnlyExistsInsideNearbyOwnedDamageTransaction(
      GameTestHelper h) {
    var p = player(h);
    var i = tyche(p);
    i.setToggled(false);
    h.assertTrue(Tyche.housePress(i, p), "House rejected");
    var target = target(h, p.position().add(2, 0, 0));
    var source = p.damageSources().playerAttack(p);
    h.assertTrue(!CriticalMerchant.tycheUndodge(p, source), "Global dodge bypass leaked");
    CriticalMerchant.observe(
        target,
        source,
        () -> {
          h.assertTrue(
              CriticalMerchant.tycheUndodge(p, source), "Nearby attack did not bypass dodge");
          return false;
        });
    target.setPos(p.position().add(30, 0, 0));
    CriticalMerchant.observe(
        target,
        source,
        () -> {
          h.assertTrue(!CriticalMerchant.tycheUndodge(p, source), "Distant attack bypassed dodge");
          return false;
        });
    h.assertTrue(!CriticalMerchant.tycheUndodge(p, source), "Transaction cleanup failed");
    h.assertTrue(i.canIgnoreCoolDown(p, 0) == false, "Unshifted active ignores cooldown");
    p.setShiftKeyDown(true);
    h.assertTrue(i.canIgnoreCoolDown(p, 0), "History inaccessible during Dividend cooldown");
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
