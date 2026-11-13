package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.exponentiation.Exponentiation;
import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.entity.projectile.TensuraFlyingProjectile;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.magic.AspectualMagics;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("laplacereview")
@PrefixGameTestTemplate(false)
public final class LaplaceGameTests {
  private static ManasSkillInstance learn(ServerPlayer p, ManasSkill s, boolean master) {
    return SplitMindGameTests.learn(p, s, master);
  }

  private static ManasSkillInstance laplace(ServerPlayer p, boolean master) {
    var i = learn(p, HeavenlyRestriction.LAPLACE.get(), master);
    i.onToggleOn(p);
    return i;
  }

  private static void near(GameTestHelper h, double actual, double expected, String message) {
    h.assertTrue(Math.abs(actual - expected) < .02, message + ": " + actual + " / " + expected);
  }

  private static LaplaceActions.Action profile(
      GameTestHelper h, ServerPlayer p, ManasSkillInstance skill, String output, String formula) {
    var a = LaplaceActions.find(p, skill, 0);
    h.assertTrue(a != null, "No catalogue entry for " + skill.getSkillId());
    var eq = new LinkedHashMap<>(a.model().defaults());
    eq.put(output, formula);
    Laplace.owned(p).orElseThrow().getOrCreateTag().remove("EditUntil");
    h.assertTrue(Laplace.save(p, a.key(), eq, true).startsWith("Saved."), "Save failed");
    return a;
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void verifiedCatalogueMatchesRegistryAndLiveConstants(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    for (var skill :
        List.of(
            CommonSkills.SELF_REGENERATION.get(),
            AspectualMagics.FIRE.get(),
            AspectualMagics.WIND_CUTTER.get(),
            AspectualMagics.WATER_CUTTER.get(),
            AspectualMagics.HEALING.get(),
            AspectualMagics.STRENGTH.get(),
            AspectualMagics.PROTECTION.get(),
            AspectualMagics.REINFORCEMENT.get())) {
      var i = learn(p, skill, true);
      var a = LaplaceActions.find(p, i, 0);
      h.assertTrue(a != null, "Missing " + i.getSkillId());
      var original = a.model().evaluate(a.model().defaults());
      near(h, original.power(), 1, "Native formula changed output");
      var roundTrip = LaplaceNetwork.decode(LaplaceNetwork.encode(a.model()));
      near(
          h,
          roundTrip.evaluate(roundTrip.defaults()).power(),
          1,
          "Preview differed after wire roundtrip");
    }
    h.assertTrue(LaplaceActions.learned(p).size() == 9, "Mode catalogue mismatch");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void fireNativeReleaseEditsBothDamageAndPaidCost(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    var spell = learn(p, AspectualMagics.FIRE.get(), false);
    var a = profile(h, p, spell, "elemental_damage", "base_elemental_damage * 2");
    var result = a.model().evaluate(Laplace.equations(Laplace.owned(p).orElseThrow(), a));
    var energy = TensuraStorages.getExistenceFrom(p);
    double before = energy.getMagicule();
    spell.getSkill().onRelease(spell, p, 1000, 0, 0);
    near(h, before - energy.getMagicule(), result.values().get("magicule_cost"), "Native payment");
    h.runAfterDelay(
        2,
        () -> {
          var shots =
              p.serverLevel()
                  .getEntitiesOfClass(
                      TensuraFlyingProjectile.class,
                      p.getBoundingBox().inflate(8),
                      e -> e.getOwner() == p);
          h.assertTrue(shots.size() == 1, "Native projectile count changed");
          near(
              h,
              shots.getFirst().getDamage(),
              result.values().get("elemental_damage"),
              "Primary damage");
          near(
              h,
              shots.getFirst().getSecondaryDamage(),
              result.values().get("magic_damage"),
              "Secondary damage");
          h.assertTrue(LaplaceRuntime.current() == null, "Cast scope leaked");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void enhancementEditsDurationWithoutChangingLevel(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    var spell = learn(p, AspectualMagics.STRENGTH.get(), false);
    var a = profile(h, p, spell, "duration", "base_duration * 2");
    var energy = TensuraStorages.getExistenceFrom(p);
    double before = energy.getMagicule();
    spell.getSkill().onRelease(spell, p, 1000, 0, 0);
    var effect = p.getEffect(MobEffects.DAMAGE_BOOST);
    h.assertTrue(effect != null, "No native Strength effect");
    near(h, effect.getDuration(), a.model().inputs().get("base_duration") * 40, "Duration");
    near(
        h,
        before - energy.getMagicule(),
        a.model().inputs().get("base_cost") * 3,
        "Duration price");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void selfRegenerationUsesPulseAndSeparateUpkeep(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    var i = learn(p, CommonSkills.SELF_REGENERATION.get(), true);
    i.setToggled(true);
    var a = profile(h, p, i, "healing", "base_healing * 2");
    double nativeHeal = a.model().inputs().get("base_healing");
    near(
        h,
        LaplaceRuntime.regeneration(p, "healing", (float) nativeHeal),
        nativeHeal * 2,
        "Pulse did not change");
    double expected =
        a.model()
            .evaluate(Laplace.equations(Laplace.owned(p).orElseThrow(), a))
            .values()
            .get("magicule_cost");
    near(
        h,
        ((io.github.manasmods.tensura.ability.TensuraSkill) i.getSkill()).getMagiculeCost(p, i, 0),
        expected,
        "Upkeep not priced");
    p.setHealth(1);
    double before = p.getHealth();
    io.github.manasmods.tensura.registry.effect.TensuraMobEffects.SELF_REGENERATION
        .get()
        .applyEffectTick(p, 1);
    h.assertTrue(p.getHealth() > before, "Native effect did not heal");
    Laplace.owned(p).orElseThrow().setToggled(false);
    near(
        h,
        ((io.github.manasmods.tensura.ability.TensuraSkill) i.getSkill()).getMagiculeCost(p, i, 0),
        a.model().inputs().get("base_cost"),
        "Cost remained edited with passive off");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void healingRespectsMissingHpAndLinkedCost(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    var i = learn(p, AspectualMagics.HEALING.get(), false);
    profile(h, p, i, "healing", "base_healing * 2");
    try (var restore =
        dev.heavenlyrestriction.exponentiation.ExponentiationContext.raw(
            p, dev.heavenlyrestriction.exponentiation.ExponentiationContext.RESTORE)) {
      p.setHealth(p.getMaxHealth() - 1);
    }
    var energy = TensuraStorages.getExistenceFrom(p);
    double before = energy.getMagicule();
    i.getSkill().onRelease(i, p, 1000, 0, 0);
    near(h, p.getHealth(), p.getMaxHealth(), "Actual native heal");
    var cfg =
        io.github.manasmods.manascore.config.ConfigRegistry.getConfig(
                io.github.manasmods.tensura.config.ability.magic.AspectualMagicConfig.class)
            .Healing;
    near(
        h,
        before - energy.getMagicule(),
        Math.max(cfg.minCost, cfg.magiculeCost),
        "Overhealing was charged");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void savingRequiresOwnershipAndRespectsCapacityAndCooldown(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    h.assertTrue(
        !Laplace.save(p, "tensura:invented#0", Map.of("damage", "1"), true).startsWith("Saved."),
        "Forged action accepted");
    for (var skill :
        List.of(
            AspectualMagics.FIRE.get(),
            AspectualMagics.WIND_CUTTER.get(),
            AspectualMagics.STRENGTH.get(),
            AspectualMagics.PROTECTION.get())) {
      var i = learn(p, skill, false);
      var a = LaplaceActions.find(p, i, 0);
      var own = Laplace.owned(p).orElseThrow();
      own.getOrCreateTag().remove("EditUntil");
      String answer = Laplace.save(p, a.key(), a.model().defaults(), true);
      if (skill == AspectualMagics.PROTECTION.get())
        h.assertTrue(!answer.startsWith("Saved."), "Fourth unmastered edit enabled");
      else {
        h.assertTrue(answer.startsWith("Saved."), answer);
        h.assertTrue(
            !Laplace.save(p, a.key(), a.model().defaults(), false).startsWith("Saved."),
            "Save cooldown bypassed");
      }
    }
    var edited = Laplace.owned(p).orElseThrow();
    edited.getOrCreateTag().putLong("EditUntil", p.level().getGameTime() + 1000);
    near(h, Laplace.remaining(p, edited), 0, "Foreign world clock locked editing");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void invalidSavedFormulaFallsBackWithoutFreeOutput(GameTestHelper h) {
    var p = player(h);
    var own = laplace(p, false);
    var i = learn(p, AspectualMagics.FIRE.get(), false);
    var a = profile(h, p, i, "elemental_damage", "base_elemental_damage * 2");
    own.getOrCreateTag()
        .getCompound(Laplace.PROFILES)
        .getCompound(a.key())
        .getCompound("equations")
        .putString("magicule_cost", "1/0");
    near(
        h,
        ((io.github.manasmods.tensura.ability.TensuraSkill) i.getSkill()).getMagiculeCost(p, i, 0),
        a.model().inputs().get("base_cost"),
        "Malformed cost did not fall back");
    try (var scope = LaplaceRuntime.cast(p, i, 0)) {
      h.assertTrue(LaplaceRuntime.current() == null, "Invalid formula partially applied");
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void universalFormulaUsesExistingExclusionsAndNoDoubleLens(GameTestHelper h) {
    var p = player(h);
    var i = laplace(p, false);
    p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
    near(h, p.getAttributeValue(Attributes.ATTACK_DAMAGE), Math.pow(10, 1.2), "Damage lens");
    near(
        h,
        p.getAttribute(TensuraAttributes.MAX_AURA).getValue(),
        Exponentiation.capacity(p, () -> p.getAttribute(TensuraAttributes.MAX_AURA).getValue()),
        "Aura capacity amplified");
    i.onToggleOff(p);
    near(h, p.getAttributeValue(Attributes.ATTACK_DAMAGE), 10, "Lens remained off");
    h.assertTrue(
        p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN).getModifier(LaplaceIntellect.ID)
            == null,
        "Intellect bonus remained");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void synthesisChargesOnceAndReplacesAllPresets(GameTestHelper h) {
    var p = player(h);
    var exp = learn(p, HeavenlyRestriction.EXPONENTIATION.get(), true);
    learn(p, UniqueSkills.MATHEMATICIAN.get(), true);
    for (var s :
        List.of(
            AspectualMagics.FIRE.get(),
            AspectualMagics.WIND_CUTTER.get(),
            AspectualMagics.HEALING.get())) learn(p, s, true);
    var ability = TensuraStorages.getAbilityFrom(p);
    ability.setAbilitySlot(8, 2, exp.getSkill(), 0);
    double before = p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
    LaplaceSynthesis.discover(p);
    h.assertTrue(LaplaceSynthesis.synthesize(p), "Valid synthesis failed");
    near(
        h,
        p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue(),
        before - LaplaceSynthesis.cost(p),
        "Permanent cost");
    h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(exp.getSkill()).isEmpty(), "Parent retained");
    h.assertTrue(
        ability.getAbilitySlot(8, 2).getSkill() == HeavenlyRestriction.LAPLACE.get(),
        "Preset lost");
    h.assertTrue(!LaplaceSynthesis.synthesize(p), "Repeat synthesis accepted");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void unfinishedSynthesisAndForgedSessionCannotSpend(GameTestHelper h) {
    var p = player(h);
    learn(p, HeavenlyRestriction.EXPONENTIATION.get(), true);
    LaplaceSynthesis.discover(p);
    var entry = SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.LAPLACE.get()).orElseThrow();
    h.assertTrue(entry.getMastery() < 0, "Pending entry learned");
    double before = p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
    LaplaceNetwork.handle(
        p, new LaplaceNetwork.Request(UUID.randomUUID(), "synthesize", "", false, Map.of()));
    near(
        h,
        p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue(),
        before,
        "Forged session spent resources");
    h.assertTrue(!LaplaceSynthesis.ready(p), "Missing conditions ignored");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void waterRepeatAndWindUseTheirPaidComponents(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    var wind = learn(p, AspectualMagics.WIND_CUTTER.get(), true);
    var a = profile(h, p, wind, "magic_damage", "base_magic_damage * 2");
    var expected = a.model().evaluate(Laplace.equations(Laplace.owned(p).orElseThrow(), a));
    wind.getSkill().onRelease(wind, p, 1000, 0, 0);
    h.runAfterDelay(
        2,
        () -> {
          var shots =
              p.serverLevel()
                  .getEntitiesOfClass(
                      TensuraFlyingProjectile.class,
                      p.getBoundingBox().inflate(24),
                      e -> e.getOwner() == p);
          h.assertTrue(shots.size() == 3, "Mastered Wind count changed");
          for (var shot : shots)
            near(
                h,
                shot.getSecondaryDamage(),
                expected.values().get("magic_damage"),
                "Wind Magic component");
          shots.forEach(net.minecraft.world.entity.Entity::discard);
          var water = learn(p, AspectualMagics.WATER_CUTTER.get(), true);
          var wa = LaplaceActions.find(p, water, 1);
          var eq = new LinkedHashMap<>(wa.model().defaults());
          eq.put("magic_damage", "base_magic_damage * 2");
          Laplace.owned(p).orElseThrow().getOrCreateTag().remove("EditUntil");
          h.assertTrue(Laplace.save(p, wa.key(), eq, true).startsWith("Saved."), "Repeat save");
          var energy = TensuraStorages.getExistenceFrom(p);
          double before = energy.getMagicule();
          var cfg =
              io.github.manasmods.manascore.config.ConfigRegistry.getConfig(
                      io.github.manasmods.tensura.config.ability.magic.AspectualMagicConfig.class)
                  .WaterCutter;
          water.getSkill().onHeld(water, p, cfg.castTimeRepeat, 1);
          near(h, before - energy.getMagicule(), cfg.magiculeCostRepeat * 3, "Repeat payment");
          h.runAfterDelay(
              2,
              () -> {
                var blades =
                    p.serverLevel()
                        .getEntitiesOfClass(
                            TensuraFlyingProjectile.class,
                            p.getBoundingBox().inflate(32),
                            e -> e.getOwner() == p);
                h.assertTrue(blades.size() == 1, "Repeat blade count");
                near(
                    h,
                    blades.getFirst().getSecondaryDamage(),
                    cfg.magicDamage * 2,
                    "Repeat output");
                h.succeed();
              });
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void protectionAndReinforcementAdaptTheirNativeDurations(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    for (var skill :
        List.of(AspectualMagics.PROTECTION.get(), AspectualMagics.REINFORCEMENT.get())) {
      var i = learn(p, skill, true);
      var a = profile(h, p, i, "duration", "base_duration * 2");
      skill.onRelease(i, p, 1000, 0, 0);
      int duration = (int) Math.round(a.model().inputs().get("base_duration") * 40);
      h.assertTrue(
          p.getActiveEffects().stream().anyMatch(e -> e.getDuration() == duration),
          "Native duration missing for " + i.getSkillId());
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void unfundedEditedHealingCannotHealForFree(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    var i = learn(p, AspectualMagics.HEALING.get(), false);
    profile(h, p, i, "healing", "base_healing * 2");
    p.setHealth(1);
    TensuraStorages.getExistenceFrom(p).setMagicule(0);
    i.getSkill().onRelease(i, p, 1000, 0, 0);
    near(h, p.getHealth(), 1, "Zero MP healed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void nativeMathematicianAndLaplaceDoNotDuplicateCalculatingPassives(
      GameTestHelper h) {
    var p = player(h);
    var lap = laplace(p, false);
    var math = learn(p, UniqueSkills.MATHEMATICIAN.get(), true);
    math.setToggled(true);
    math.onToggleOn(p);
    LaplaceIntellect.refresh(p);
    h.assertTrue(
        p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN).getModifier(LaplaceIntellect.ID)
            == null,
        "Parent and Ultimate duplicated learning");
    math.setToggled(false);
    math.onToggleOff(p);
    LaplaceIntellect.refresh(p);
    h.assertTrue(
        p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN).getModifier(LaplaceIntellect.ID)
            != null,
        "Inherited passive did not resume");
    SkillAPI.getSkillsFrom(p).forgetSkill(lap);
    h.assertTrue(
        p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN).getModifier(LaplaceIntellect.ID)
            == null,
        "Forgetting left modifiers");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void savedEquationsSurviveCopyAndMasteryUnlocksFourthEdit(GameTestHelper h) {
    var p = player(h);
    var lap = laplace(p, true);
    var fire = learn(p, AspectualMagics.FIRE.get(), false);
    var a = profile(h, p, fire, "elemental_damage", "base_elemental_damage * 2");
    var copy = lap.copy();
    h.assertTrue(
        Laplace.equations(copy, a).get("elemental_damage").equals("base_elemental_damage * 2"),
        "Profile lost in serialized skill copy");
    h.assertTrue(Laplace.enabled(copy, a.key()), "Enabled flag lost");
    for (var skill :
        List.of(
            AspectualMagics.STRENGTH.get(),
            AspectualMagics.PROTECTION.get(),
            AspectualMagics.REINFORCEMENT.get())) {
      var i = learn(p, skill, false);
      profile(h, p, i, "duration", "base_duration");
    }
    h.assertTrue(Laplace.capacity(p) == 4, "Mastered capacity");
    lap.setMastery(0);
    h.assertTrue(
        LaplaceActions.learned(p).stream()
                .filter(a2 -> Laplace.effectiveEnabled(p, lap, a2.key()))
                .count()
            == 3,
        "Mastery loss retained four effective edits");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void inheritedChantAndAppraisalPreferStrongerExistingProvider(GameTestHelper h) {
    var p = player(h);
    laplace(p, false);
    io.github.manasmods.tensura.util.AttributeHelper.multiplyChantSpeed(p, 100);
    io.github.manasmods.tensura.util.AttributeHelper.addAnalysisAttributes(p, 100, 100);
    LaplaceIntellect.refresh(p);
    near(
        h,
        p.getAttribute(TensuraAttributes.CHANT_SPEED).getModifier(LaplaceIntellect.ID).amount(),
        0,
        "Chant duplicated stronger provider");
    near(
        h,
        p.getAttribute(TensuraAttributes.ANALYSIS_LEVEL).getModifier(LaplaceIntellect.ID).amount(),
        0,
        "Appraisal duplicated stronger provider");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void applyEnablesTheSavedActionAndUniversalFormulaTogether(GameTestHelper h) {
    var p = player(h);
    var i = laplace(p, false);
    i.onToggleOff(p);
    var spell = learn(p, AspectualMagics.WIND_CUTTER.get(), false);
    var a = LaplaceActions.find(p, spell, 0);
    var eq = new LinkedHashMap<>(a.model().defaults());
    eq.put("elemental_damage", "base_elemental_damage * 2");
    LaplaceNetwork.open(p);
    var session = LaplaceNetwork.snapshot(p).getUUID("session");
    LaplaceNetwork.handle(p, new LaplaceNetwork.Request(session, "apply", a.key(), false, eq));
    h.assertTrue(
        i.isToggled() && Laplace.effectiveEnabled(p, i, a.key()),
        "Apply did not enable both gates");
    h.assertTrue(Laplace.equations(i, a).equals(eq), "Applied equations did not persist");
    i.getOrCreateTag().remove("EditUntil");
    LaplaceNetwork.handle(
        p,
        new LaplaceNetwork.Request(session, "disable", a.key(), true, Map.of("untrusted", "1/0")));
    h.assertTrue(
        i.isToggled() && !Laplace.effectiveEnabled(p, i, a.key()),
        "Disable affected the passive or failed to disable the action");
    h.assertTrue(
        Laplace.equations(i, a).equals(eq),
        "Disable replaced the saved equations with packet input");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void invalidBoardApplyCannotToggleOrReplaceTheLiveState(GameTestHelper h) {
    var p = player(h);
    var i = laplace(p, false);
    i.onToggleOff(p);
    var spell = learn(p, AspectualMagics.FIRE.get(), false);
    var a = LaplaceActions.find(p, spell, 0);
    var eq = new LinkedHashMap<>(a.model().defaults());
    eq.put("elemental_damage", "helper_1");
    eq.put("helper_1", "elemental_damage");
    LaplaceNetwork.open(p);
    var session = LaplaceNetwork.snapshot(p).getUUID("session");
    LaplaceNetwork.handle(p, new LaplaceNetwork.Request(session, "apply", a.key(), true, eq));
    h.assertTrue(
        !i.isToggled() && !Laplace.enabled(i, a.key()), "Invalid equations enabled effects");
    h.assertTrue(!i.getOrCreateTag().contains(Laplace.PROFILES), "Invalid board wrote profiles");
    h.succeed();
  }
}
