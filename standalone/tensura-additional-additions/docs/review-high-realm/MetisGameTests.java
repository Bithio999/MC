package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.metis.*;
import dev.heavenlyrestriction.splitmind.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.magic.*;
import io.github.manasmods.tensura.ability.subclass.*;
import io.github.manasmods.tensura.entity.magic.MagicCircle;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.magic.*;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("metisreview")
@PrefixGameTestTemplate(false)
public final class MetisGameTests {
  private static ManasSkillInstance learn(ServerPlayer p, ManasSkill s, boolean master) {
    return SplitMindGameTests.learn(p, s, master);
  }

  private static ManasSkillInstance metis(ServerPlayer p, boolean master) {
    var i = learn(p, HeavenlyRestriction.METIS.get(), master);
    i.onToggleOn(p);
    return i;
  }

  private static void near(GameTestHelper h, double a, double b, String label) {
    h.assertTrue(Math.abs(a - b) < .01, label + ": " + a + " / " + b);
  }

  private static void step(ServerPlayer p, int tick) {
    p.tickCount = tick;
    for (var held : List.copyOf(SkillAPI.getSkillsFrom(p).getHeldSkills()))
      if (!held.tick(SkillAPI.getSkillsFrom(p), p))
        SkillAPI.getSkillsFrom(p).getHeldSkills().remove(held);
    MetisCasting.update(p);
  }

  private static ServerPlayer candidate(GameTestHelper h) {
    var p = player(h);
    learn(p, HeavenlyRestriction.SPLIT_MIND.get(), true);
    learn(p, UniqueSkills.GREAT_SAGE.get(), true);
    learn(p, AspectualMagics.FIRE.get(), true);
    learn(p, AspectualMagics.WIND_CUTTER.get(), true);
    learn(p, AspectualMagics.HEALING.get(), true);
    return p;
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void nativeUltimateRetainsGreatSageInterfacesAndNoGrandCalculation(
      GameTestHelper h) {
    var s = HeavenlyRestriction.METIS.get();
    h.assertTrue(
        s.getType() == io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE
            && s instanceof IRefining
            && s instanceof IRepeatCrafting
            && s.getModes(s.createDefaultInstance()) == 5,
        "Wrong classification or utilities");
    var p = player(h);
    var i = metis(p, false);
    near(
        h,
        p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN)
            .getModifier(MetisIntellect.BONUS)
            .amount(),
        19,
        "Learning bonus");
    near(
        h,
        p.getAttribute(TensuraAttributes.ABILITY_MASTERY_GAIN)
            .getModifier(MetisIntellect.BONUS)
            .amount(),
        19,
        "Mastery bonus");
    h.assertTrue(MagicUtils.hasChantAnnulment(p), "Missing chant annulment");
    i.getOrCreateTag().putBoolean("Activated", true);
    HeavenlyRestriction.METIS.get().onRespawn(i, p, false);
    h.assertTrue(
        p.getAttributeValue(TensuraAttributes.ANALYSIS_LEVEL) > 0,
        "Inherited appraisal did not restore on respawn");

    MetisIntellect.refresh(p);
    near(
        h,
        p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN)
            .getModifier(MetisIntellect.BONUS)
            .amount(),
        19,
        "Bonus stacked");
    i.onToggleOff(p);
    h.assertTrue(
        !p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN).hasModifier(MetisIntellect.BONUS),
        "Bonus remained after toggle off");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void synthesisPreservesStorageAssignmentsAndPresetsAndChargesOnce(
      GameTestHelper h) {
    var p = candidate(h);
    var skills = SkillAPI.getSkillsFrom(p);
    var sage = skills.getSkill(UniqueSkills.GREAT_SAGE.get()).orElseThrow();
    var mind = skills.getSkill(HeavenlyRestriction.SPLIT_MIND.get()).orElseThrow();
    mind.onToggleOn(p);
    h.assertTrue(
        SplitMindCasting.assign(p, AspectualMagics.FIRE.getId().toString()), "Initial assignment");
    var storage =
        ((IRefining<?>) UniqueSkills.GREAT_SAGE.get()).getSpatialStorage(sage, p.registryAccess());
    storage.setItem(0, new ItemStack(Items.DIAMOND, 7));
    ((IRefining<?>) UniqueSkills.GREAT_SAGE.get())
        .setItemInSpatialStorage(sage, p, new ItemStack(Items.DIAMOND, 7), 0);
    var ability = TensuraStorages.getAbilityFrom(p);
    ability.setAbilitySlot(0, 0, mind.getSkill(), 1);
    ability.setAbilitySlot(0, 1, sage.getSkill(), 2);
    double before = p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
    MetisSynthesis.discover(p);
    h.assertTrue(MetisSynthesis.synthesize(p), "Valid synthesis rejected");
    var i = MetisMatrix.owned(p).orElseThrow();
    h.assertTrue(
        i.getMastery() == 0
            && i.getParentSkill() == null
            && skills.getSkill(mind.getSkill()).isEmpty()
            && skills.getSkill(sage.getSkill()).isEmpty(),
        "Sources not replaced with learned independent Ultimate");
    near(
        h,
        p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue(),
        before - MetisSynthesis.cost(p),
        "Cost charged incorrectly");
    h.assertTrue(!MetisSynthesis.synthesize(p), "Repeat synthesis succeeded");
    h.assertTrue(
        HeavenlyRestriction.METIS
                .get()
                .getSpatialStorage(i, p.registryAccess())
                .getItem(0)
                .getCount()
            == 7,
        "Stored items lost");
    h.assertTrue(
        MetisMatrix.slots(p, i).size() == 1
            && MetisMatrix.slots(p, i).getFirst().id().equals(AspectualMagics.FIRE.getId()),
        "Assignment lost");
    h.assertTrue(
        ability.getAbilitySlot(0, 0).getSkill() == i.getSkill()
            && ability.getAbilitySlot(0, 0).getMode() == 1
            && ability.getAbilitySlot(0, 1).getMode() == 4,
        "Preset utility mode not remapped");
    h.assertTrue(
        !p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN).hasModifier(SplitMindSkill.BONUS)
            && p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN)
                .hasModifier(MetisIntellect.BONUS),
        "Source passive retained or new passive lost");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void unfinishedEntryCannotLearnNormallyAndSynthesisChecksAllRequirements(
      GameTestHelper h) {
    var p = candidate(h);
    MetisSynthesis.discover(p);
    var entry = SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.METIS.get()).orElseThrow();
    near(h, entry.getMastery(), -1, "Pending entry learned early");
    double before = p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
    // No registered UI session: opening is harmless, normal learning must not complete the entry.
    HeavenlyRestriction.METIS.get().addLearnPoint(entry, p, 0, 1000000);
    h.assertTrue(entry.getMastery() < 0, "Normal learning bypassed synthesis");
    TensuraStorages.getExistenceFrom(p).setTrueHero(false);
    h.assertTrue(!MetisSynthesis.ready(p), "Awakening bypassed");
    TensuraStorages.getExistenceFrom(p).setTrueHero(true);
    SkillAPI.getSkillsFrom(p)
        .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
        .orElseThrow()
        .setMastery(0);
    h.assertTrue(!MetisSynthesis.ready(p), "Mastery bypassed");
    near(
        h,
        p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue(),
        before,
        "Failed synthesis charged capacity");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void activeSkillsCanBeAssignedWhileMenuModesAndPassivesAreExcluded(
      GameTestHelper h) {
    var p = player(h);
    metis(p, false);
    var merchant = learn(p, HeavenlyRestriction.CRITICAL_MERCHANT.get(), true);
    var sage = learn(p, UniqueSkills.GREAT_SAGE.get(), true);
    var prospector = learn(p, HeavenlyRestriction.PROSPECTOR.get(), true);
    var fighter = learn(p, UniqueSkills.FIGHTER.get(), true);
    h.assertTrue(
        MetisAbilities.allowed(p, merchant, 0),
        "Critical Merchant excluded: " + MetisAbilities.available(p, merchant));
    h.assertTrue(
        MetisMatrix.assign(p, 0, merchant.getSkillId().toString(), 0, 1, false),
        "Owned active skill rejected");
    h.assertTrue(
        !MetisAbilities.allowed(p, sage, 2)
            && !MetisMatrix.assign(p, 1, sage.getSkillId().toString(), 2, 0, false),
        "Great Sage crafting/refining menu accepted");
    h.assertTrue(
        !MetisAbilities.allowed(p, prospector, 0) && !MetisAbilities.eligible(fighter),
        "Menu-only/passive-only skill eligible");
    h.assertTrue(!MetisAbilities.available(p, sage).isEmpty(), "Non-menu Great Sage modes lost");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void assignedPressSkillUsesNativeCostCooldownAndKeepsMagicDiscountSeparate(
      GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var merchant = learn(p, HeavenlyRestriction.CRITICAL_MERCHANT.get(), false);
    var fire = learn(p, AspectualMagics.FIRE.get(), true);
    MetisMatrix.assign(p, 0, merchant.getSkillId().toString(), 0, 1, false);
    mind.getOrCreateTag().putBoolean(MetisMatrix.AUTO, true);
    var energy = TensuraStorages.getExistenceFrom(p);
    double mp = energy.getMagicule();
    p.tickCount = 5;
    MetisCasting.update(p);
    h.assertTrue(
        dev.heavenlyrestriction.critical.CriticalMerchant.remaining(merchant) == 10
            && merchant.getCoolDown(0) == 20,
        "Press skill did not activate");
    near(h, mp - energy.getMagicule(), 1000, "Nonmagic skill was discounted or charged twice");
    h.assertTrue(
        SkillAPI.getSkillsFrom(p).getHeldSkills().stream()
            .noneMatch(t -> t.getSkill() == merchant.getSkill()),
        "Press skill leaked a held instance");
    near(
        h,
        SplitMindCasting.autoInvoke(p, merchant, 0, p, () -> SplitMindCasting.magiculeFactor(p)),
        1,
        "Skill received magic discount");
    near(
        h,
        SplitMindCasting.autoInvoke(p, fire, 0, p, () -> SplitMindCasting.magiculeFactor(p)),
        .7,
        "Magic discount lost");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void spatialMotionKeepsBlinkButOmitsWarpMenuAndRejectsForgedMenuPackets(
      GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var spatial = learn(p, ExtraSkills.SPATIAL_MOTION.get(), true);
    h.assertTrue(
        MetisAbilities.allowed(p, spatial, 0) && !MetisAbilities.allowed(p, spatial, 1),
        "Blink/warp distinction failed");
    MetisNetwork.open(p);
    var state = MetisNetwork.snapshot(p, "spatial motion", 3, 0);
    var rows = state.getList("rows", Tag.TAG_COMPOUND);
    h.assertTrue(
        rows.size() == 1 && rows.getCompound(0).getList("modes", Tag.TAG_COMPOUND).size() == 1,
        "Menu mode was included in dropdown");
    MetisNetwork.handle(
        p,
        new MetisNetwork.Request(
            state.getUUID("session"),
            "assign",
            spatial.getSkillId().toString(),
            "",
            0,
            3,
            0,
            1,
            2,
            true));
    h.assertTrue(
        MetisMatrix.slots(p, mind).isEmpty(), "Valid session forged menu assignment accepted");
    var saved = new ListTag();
    var row = new CompoundTag();
    row.putInt("slot", 0);
    row.putString("id", spatial.getSkillId().toString());
    row.putInt("mode", 1);
    saved.add(row);
    mind.getOrCreateTag().put(MetisMatrix.SLOTS, saved);
    mind.getOrCreateTag().putBoolean(MetisMatrix.AUTO, true);
    var container = p.containerMenu;
    p.tickCount = 5;
    MetisCasting.update(p);
    h.assertTrue(
        MetisMatrix.slots(p, mind).isEmpty()
            && p.containerMenu == container
            && SkillAPI.getSkillsFrom(p).getHeldSkills().isEmpty(),
        "Invalid saved mode executed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void autoSkillHealingTargetsSelfAndRestoresCrouchState(GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var healer = learn(p, UniqueSkills.HEALER.get(), true);
    h.assertTrue(
        MetisMagic.role(healer, 0) == MetisMagic.Role.HEALING
            && MetisMagic.role(healer, 1) == MetisMagic.Role.OFFENSE,
        "Healer virus treated as self healing");
    h.assertTrue(
        MetisMatrix.assign(p, 0, healer.getSkillId().toString(), 0, 0, false),
        "Native Healer rejected");
    mind.getOrCreateTag().putBoolean(MetisMatrix.AUTO, true);
    p.setHealth(p.getMaxHealth() * .5f);
    p.setShiftKeyDown(true);
    double mp = TensuraStorages.getExistenceFrom(p).getMagicule();
    p.tickCount = 5;
    MetisCasting.update(p);
    h.assertTrue(
        p.getHealth() == p.getMaxHealth() && p.isShiftKeyDown(),
        "Self heal or input restoration failed");
    h.assertTrue(
        TensuraStorages.getExistenceFrom(p).getMagicule() < mp,
        "Skill healing did not pay native cost");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void autoSkillDoesNotUseManualCooldownOverrideToCancelCounter(GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var counter = learn(p, HeavenlyRestriction.UNBOUND.get(), true);
    var enemy = target(h, p.position().add(0, 0, 3));
    h.assertTrue(
        MetisMatrix.assign(p, 0, counter.getSkillId().toString(), 1, 2, false),
        "Counter assignment rejected");
    mind.getOrCreateTag().putBoolean(MetisMatrix.AUTO, true);
    afterTracking(
        h,
        List.of(enemy),
        () -> {
          p.tickCount = 5;
          MetisCasting.update(p);
          h.assertTrue(UnboundCounter.active(p), "Automatic counter did not activate");
          double aura = TensuraStorages.getExistenceFrom(p).getAura();
          p.tickCount = 10;
          MetisCasting.update(p);
          h.assertTrue(
              UnboundCounter.active(p) && TensuraStorages.getExistenceFrom(p).getAura() == aura,
              "Automatic recast canceled or recharged active counter");
          UnboundCounter.clear(p);
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void matrixRejectsUnownedLockedDuplicateAndFourthUnmasteredSlots(GameTestHelper h) {
    var p = player(h);
    var i = metis(p, false);
    var fire = learn(p, AspectualMagics.FIRE.get(), true);
    h.assertTrue(
        MetisMatrix.assign(p, 0, fire.getSkillId().toString(), 0, 2, true),
        "Valid assignment failed");
    h.assertTrue(
        !MetisMatrix.assign(p, 1, fire.getSkillId().toString(), 0, 2, true)
            && !MetisMatrix.assign(p, 3, fire.getSkillId().toString(), 0, 2, true)
            && !MetisMatrix.assign(p, 1, "tensura:wind_cutter", 0, 2, true)
            && !MetisMatrix.assign(p, 0, fire.getSkillId().toString(), 999, 2, true),
        "Forged or conflicting assignment accepted");
    i.setMastery(i.getMaxMastery());
    var wind = learn(p, AspectualMagics.WIND_CUTTER.get(), true);
    h.assertTrue(
        MetisMatrix.assign(p, 3, wind.getSkillId().toString(), 0, 2, false),
        "Mastered fourth slot failed");
    var reload = i.copy();
    h.assertTrue(MetisMatrix.slots(p, reload).size() == 2, "Matrix did not persist");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void requestRequiresOwnerBoundSessionAndCatalogListsOnlyLearnedEligibleAbilities(
      GameTestHelper h) {
    var p = player(h);
    metis(p, false);
    var fire = learn(p, AspectualMagics.FIRE.get(), true);
    var stone = learn(p, AspectualMagics.STONE_SHOT.get(), false);
    stone.setMastery(-1);
    var rows = MetisNetwork.snapshot(p, "", 0, 0).getList("rows", Tag.TAG_COMPOUND);
    h.assertTrue(
        rows.size() == 1
            && rows.getCompound(0).getString("id").equals(fire.getSkillId().toString()),
        "Catalog included unlearned or nonmagic skills");
    MetisNetwork.handle(
        p,
        new MetisNetwork.Request(
            UUID.randomUUID(), "assign", fire.getSkillId().toString(), "", 0, 0, 0, 0, 2, true));
    h.assertTrue(
        MetisMatrix.slots(p, MetisMatrix.owned(p).orElseThrow()).isEmpty(),
        "Unbound packet mutated matrix");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void threeIndependentSpellsCastAlongsideManualWithoutCameraModifiers(
      GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var auto =
        List.of(
            learn(p, AspectualMagics.FIRE.get(), true),
            learn(p, AspectualMagics.WIND_CUTTER.get(), true),
            learn(p, AspectualMagics.STONE_SHOT.get(), true));
    for (int n = 0; n < 3; n++)
      h.assertTrue(
          MetisMatrix.assign(p, n, auto.get(n).getSkillId().toString(), 0, 2, false),
          "Assignment failed");
    mind.getOrCreateTag().putBoolean(MetisMatrix.AUTO, true);
    var manual = learn(p, AspectualMagics.FIRE_BALL.get(), false);
    var storage = SkillAPI.getSkillsFrom(p);
    TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, manual.getSkill(), 0);
    var target = target(h, p.position().add(12, 0, 0));
    afterTracking(
        h,
        List.of(target),
        () -> {
          try {
            float yaw = p.getYRot(), pitch = p.getXRot();
            double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            MetisCasting.update(p);
            p.tickCount = 5;
            MetisCasting.update(p);
            h.assertTrue(
                storage.getHeldSkills().size() == 3, "Three background spells did not coexist");
            near(h, p.getYRot(), yaw, "Yaw changed");
            near(h, p.getXRot(), pitch, "Pitch changed");
            near(
                h,
                p.getAttributeValue(Attributes.MOVEMENT_SPEED),
                speed,
                "Background camera/speed modifier");
            h.assertTrue(
                storage.startHoldSkill(manual, 0, 0) && storage.getHeldSkills().size() == 4,
                "Manual spell could not overlap");
            for (int tick = 6; tick <= 12; tick++) step(p, tick);
            h.assertTrue(
                storage.getHeldSkills().stream().anyMatch(t -> t.getKeyNumber() == 0),
                "Automation cancelled manual spell");
          } finally {
            SplitMindCasting.cancelAll(p);
          }
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void autoCostsDiscountOnceAndManualBillsStayNative(GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var spell = learn(p, AspectualMagics.FIRE.get(), true);
    var energy = TensuraStorages.getExistenceFrom(p);
    energy.setMagicule(10000);
    double before = energy.getMagicule();
    SplitMindCasting.autoInvoke(
        p,
        spell,
        0,
        p,
        () -> {
          io.github.manasmods.tensura.util.EnergyHelper.isOutOfEnergy(p, 0, 100);
          return true;
        });
    near(h, before - energy.getMagicule(), 70, "Direct auto discount");
    before = energy.getMagicule();
    SplitMindCasting.autoInvoke(
        p,
        spell,
        0,
        p,
        () -> {
          ((Magic) spell.getSkill()).isOutOfEnergy(p, spell, 0d, 100d);
          return true;
        });
    near(h, before - energy.getMagicule(), 70, "Native Magic discount doubled");
    mind.setMastery(mind.getMaxMastery());
    before = energy.getMagicule();
    SplitMindCasting.autoInvoke(
        p,
        spell,
        0,
        p,
        () -> {
          io.github.manasmods.tensura.util.EnergyHelper.isOutOfMagiculeConsuming(p, 100);
          return true;
        });
    near(h, before - energy.getMagicule(), 60, "Mastered consuming discount");
    before = energy.getMagicule();
    io.github.manasmods.tensura.util.EnergyHelper.isOutOfEnergy(p, 0, 100);
    near(h, before - energy.getMagicule(), 100, "Manual cost discounted");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void healingTargetsOwnerAndDoesNotRepeatWhenHealthy(GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, true);
    var heal = learn(p, AspectualMagics.HEALING.get(), true);
    p.setHealth(2);
    h.assertTrue(
        MetisMatrix.assign(p, 0, heal.getSkillId().toString(), 0, 0, true),
        "Heal assignment failed");
    mind.getOrCreateTag().putBoolean(MetisMatrix.AUTO, true);
    var target = target(h, p.position().add(0, 0, 4));
    target.setHealth(500);
    afterTracking(
        h,
        List.of(target),
        () -> {
          try {
            double before = target.getHealth();
            for (int tick = 5; tick <= 12; tick++) step(p, tick);
            h.assertTrue(p.getHealth() > 2, "Self-healing never cast");
            near(h, target.getHealth(), before, "Healed hostile target");
            p.setHealth(p.getMaxHealth());
            h.assertTrue(!MetisMagic.needed(p, heal), "Healthy owner still needs healing");
          } finally {
            SplitMindCasting.cancelAll(p);
          }
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void optimalSolutionAvoidsNullificationAndRecordsWithoutChangingDefenses(
      GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var fire = learn(p, SpiritualMagics.FIRE_BOLT.get(), true);
    var wind = learn(p, SpiritualMagics.AERIAL_BLADE.get(), true);
    var target = target(h, p.position().add(4, 0, 0));
    RestrictionData.learnReward(target, ResistanceSkills.FLAME_ATTACK_NULLIFICATION.get());
    var nul =
        SkillAPI.getSkillsFrom(target)
            .getSkill(ResistanceSkills.FLAME_ATTACK_NULLIFICATION.get())
            .orElseThrow();
    nul.setToggled(true);
    h.assertTrue(
        nul.getMastery() >= 0 && nul.canInteractSkill(target),
        "Nullification not active: mastery="
            + nul.getMastery()
            + " interact="
            + nul.canInteractSkill(target));
    h.assertTrue(
        !io.github.manasmods.tensura.ability.skill.resist.ResistSkill.isNullificationDisabled(
            target.level()),
        "Native nullifications are disabled by gamerule");
    near(h, MetisMagic.effectiveness(p, target, fire, 0), 0, "Known nullification not avoided");
    h.assertTrue(MetisMagic.effectiveness(p, target, wind, 0) > 0, "Unresisted spell rejected");
    var aspect = learn(p, AspectualMagics.FIRE.get(), true);
    h.assertTrue(
        MetisMagic.effectiveness(p, target, aspect, 0) > 0,
        "Physical flame nullification incorrectly blocked aspectual magic");
    target.getAttribute(TensuraAttributes.WIND_RESISTANCE).setBaseValue(-.5);
    near(
        h,
        MetisMagic.effectiveness(p, target, wind, 0),
        1.5,
        "Native elemental vulnerability ignored");

    MetisMatrix.assign(p, 0, fire.getSkillId().toString(), 0, 2, true);
    MetisMagic.record(p, target, mind);
    h.assertTrue(
        mind.getOrCreateTag().getList("MetisDossiers", Tag.TAG_COMPOUND).size() == 1
            && nul.isToggled(),
        "Dossier missing or defense changed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void automaticCircleAnalysisCopiesOnlyEligibleVisibleNonownedMagic(
      GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var caster = target(h, p.position().add(4, 0, 0));
    RestrictionData.learnReward(caster, AspectualMagics.FIRE.get());
    var spell = SkillAPI.getSkillsFrom(caster).getSkill(AspectualMagics.FIRE.get()).orElseThrow();
    var circle = new MagicCircle(h.getLevel(), caster);
    circle.setSkill(spell);
    circle.setPos(p.position().add(4, 1, 0));
    h.assertTrue(MetisIntellect.analyze(p, mind, circle), "Valid circle was not learned");
    h.assertTrue(
        SkillAPI.getSkillsFrom(p)
            .getSkill(spell.getSkill())
            .filter(i -> i.getMastery() >= 0)
            .isPresent(),
        "Analysis did not add magic");
    h.assertTrue(
        !MetisIntellect.analyze(p, mind, circle), "Already learned circle repeatedly analyzed");
    var own = new MagicCircle(h.getLevel(), p);
    own.setSkill(learn(p, AspectualMagics.WIND_CUTTER.get(), true));
    own.setPos(p.position().add(1, 1, 0));
    h.assertTrue(!MetisIntellect.analyze(p, mind, own), "Own circle was analyzed");
    mind.onToggleOff(p);
    h.assertTrue(!MetisIntellect.analyze(p, mind, circle), "Disabled intellect analyzed circle");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void expiredMatrixSessionClosesWithoutMutationAndCanBeReopened(GameTestHelper h)
      throws Exception {
    var p = player(h);
    var mind = metis(p, false);
    var fire = learn(p, AspectualMagics.FIRE.get(), true);
    MetisNetwork.open(p);
    var id = MetisNetwork.snapshot(p, "", 0, 0).getUUID("session");
    var field = MetisNetwork.class.getDeclaredField("SESSIONS");
    field.setAccessible(true);
    var sessions = (java.util.Map) field.get(null);
    var old = sessions.get(p);
    var ctor = old.getClass().getDeclaredConstructor(UUID.class, long.class);
    ctor.setAccessible(true);
    sessions.put(p, ctor.newInstance(id, p.level().getGameTime() - 1));
    MetisNetwork.handle(
        p,
        new MetisNetwork.Request(
            id, "assign", fire.getSkillId().toString(), "", 0, 0, 0, 0, 2, true));
    h.assertTrue(
        !sessions.containsKey(p) && MetisMatrix.slots(p, mind).isEmpty(),
        "Expired session mutated the matrix or remained stuck");
    MetisNetwork.open(p);
    var next = MetisNetwork.snapshot(p, "", 0, 0).getUUID("session");
    h.assertTrue(!next.equals(id), "Session nonce reused");
    MetisNetwork.handle(
        p,
        new MetisNetwork.Request(
            next, "assign", fire.getSkillId().toString(), "", 0, 0, 0, 0, 2, true));
    h.assertTrue(MetisMatrix.slots(p, mind).size() == 1, "Reopened matrix could not save");
    h.succeed();
  }

  private static net.minecraft.world.damagesource.DamageSource autoSource(
      ServerPlayer p, ManasSkillInstance spell, LivingEntity target) {
    return SplitMindCasting.autoInvoke(
        p,
        spell,
        0,
        target,
        () ->
            ((Magic) spell.getSkill())
                .createSource(spell, p, net.minecraft.world.damagesource.DamageTypes.GENERIC, 0));
  }

  private static LivingEntity bareTarget(GameTestHelper h, ServerPlayer p) {
    var target = target(h, p.position().add(4, 0, 0));
    target.getAttribute(Attributes.ARMOR).setBaseValue(0);
    return target;
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void backgroundSpellGesturesDoNotSwingOrInterruptManualSwings(GameTestHelper h) {
    var p = player(h);
    var spell = learn(p, AspectualMagics.FIRE.get(), true);
    p.swinging = false;
    SplitMindCasting.autoInvoke(
        p,
        spell,
        0,
        p,
        () -> {
          p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
          return true;
        });
    h.assertTrue(!p.swinging, "Automatic spell started an arm swing");
    p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    h.assertTrue(p.swinging, "Manual swing blocked");
    int swing = p.swingTime;
    SplitMindCasting.autoInvoke(
        p,
        spell,
        0,
        p,
        () -> {
          p.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
          return true;
        });
    h.assertTrue(
        p.swinging
            && p.swingTime == swing
            && p.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND,
        "Automation changed an existing manual swing");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void differentAutomaticSpellsHitIndependentlyAndKeepOwnNativeWindows(
      GameTestHelper h) {
    var p = player(h);
    var fire = learn(p, AspectualMagics.FIRE.get(), true);
    var wind = learn(p, AspectualMagics.WIND_CUTTER.get(), true);
    var t = bareTarget(h, p);
    var a = autoSource(p, fire, t);
    var b = autoSource(p, wind, t);
    float before = t.getHealth();
    h.assertTrue(
        t.hurt(a, 10) && t.hurt(b, 10), "Parallel spell damage was lost to another thread");
    near(h, before - t.getHealth(), 20, "Independent spell totals");
    h.assertTrue(!t.hurt(a, 10), "Same-spell native hurt cooldown was bypassed");
    h.assertTrue(t.hurt(a, 15), "Higher same-spell hit was incorrectly blocked");
    near(h, before - t.getHealth(), 25, "Higher hit should apply only its native difference");
    t.tickCount += 11;
    h.assertTrue(t.hurt(a, 10), "Spell window did not age with target ticks");
    near(h, before - t.getHealth(), 35, "Expired window did not accept full damage");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void autoWindowsDoNotOverwriteManualDamageStateOrGrantImmunityBypass(
      GameTestHelper h) {
    var p = player(h);
    var spell = learn(p, AspectualMagics.FIRE.get(), true);
    var t = bareTarget(h, p);
    var manual = p.damageSources().generic();
    h.assertTrue(t.hurt(manual, 20), "Manual setup failed");
    int timer = t.invulnerableTime;
    var source = autoSource(p, spell, t);
    h.assertTrue(t.hurt(source, 5), "Manual timer blocked automatic spell");
    h.assertTrue(
        t.invulnerableTime == timer && !t.hurt(manual, 20),
        "Auto changed manual timer or last-hit damage");
    float before = t.getHealth();
    h.assertTrue(t.hurt(manual, 25), "Native higher manual hit failed");
    near(h, before - t.getHealth(), 5, "Manual last-hit value was overwritten");
    t.invulnerableTime = 60;
    h.assertTrue(!t.hurt(source, 5), "Long defensive window bypassed");
    t.invulnerableTime = 20;
    h.assertTrue(!t.hurt(source, 5), "Tail of defensive window bypassed");
    var immune = bareTarget(h, p);
    immune.setInvulnerable(true);
    before = immune.getHealth();
    h.assertTrue(!immune.hurt(source, 10), "Entity invulnerability bypassed");
    near(h, immune.getHealth(), before, "Invulnerable entity lost health");
    h.assertTrue(
        !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY),
        "Source gained immunity bypass");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void projectileWindowsSurviveDelayedImpactCopyAndSaveAndIncludeManualMagic(
      GameTestHelper h) {
    var p = player(h);
    var spell = learn(p, AspectualMagics.FIRE.get(), true);
    var t = bareTarget(h, p);
    var shot =
        SplitMindCasting.autoInvoke(
            p,
            spell,
            0,
            t,
            () -> {
              var e =
                  new io.github.manasmods.tensura.entity.projectile.magic.FireBoltProjectile(
                      p.level(), p);
              e.setSkill(p, spell, (Magic) spell.getSkill(), 0);
              e.setPos(p.position().add(0, 3, 0));
              p.level().addFreshEntity(e);
              return e;
            });
    var source = shot.getDamageSource();
    h.assertTrue(AutoCastCombat.key(source) != null, "Delayed native projectile lost auto origin");
    var copy = io.github.manasmods.tensura.damage.TensuraDamageHelper.copy(source);
    h.assertTrue(
        AutoCastCombat.key(copy).equals(AutoCastCombat.key(source)),
        "Native source copy lost origin");
    var tag = new CompoundTag();
    shot.saveWithoutId(tag);
    var reloaded =
        new io.github.manasmods.tensura.entity.projectile.magic.FireBoltProjectile(p.level(), p);
    reloaded.load(tag);
    reloaded.setOwner(p);
    h.assertTrue(
        AutoCastCombat.key(reloaded.getDamageSource()) != null, "Saved projectile lost origin");
    var ordinary =
        new io.github.manasmods.tensura.entity.projectile.magic.FireBoltProjectile(p.level(), p);
    ordinary.setSkill(p, spell, (Magic) spell.getSkill(), 0);
    h.assertTrue(
        AutoCastCombat.key(ordinary.getDamageSource()) != null
            && !AutoCastCombat.key(ordinary.getDamageSource()).equals(AutoCastCombat.key(source)),
        "Separate manual projectile did not receive its own impact window");
    shot.discard();
    h.succeed();
  }

  private static float[] nativePair(
      GameTestHelper h, ServerPlayer p, float magic, float element, boolean automatic)
      throws Exception {
    var spell = learn(p, AspectualMagics.FIRE.get(), true);
    var t = bareTarget(h, p);
    var source =
        automatic
            ? autoSource(p, spell, t)
            : ((Magic) spell.getSkill())
                .createSource(spell, p, net.minecraft.world.damagesource.DamageTypes.GENERIC, 0);
    var other = io.github.manasmods.tensura.damage.TensuraDamageHelper.copy(source);
    float before = t.getHealth();
    io.github.manasmods.tensura.damage.TensuraDamageHelper.hurtDouble(
        t, source, element, other, magic);
    float first = before - t.getHealth();
    before = t.getHealth();
    io.github.manasmods.tensura.damage.TensuraDamageHelper.hurtDouble(
        t, source, element, other, magic);
    return new float[] {first, before - t.getHealth()};
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void nativePairedDamageCadenceIsPreservedInsideEachAutoThread(GameTestHelper h)
      throws Exception {
    var p = player(h);
    for (var pair : new float[][] {{30, 10}, {20, 10}, {40, 0}, {10, 30}}) {
      var nativeHits = nativePair(h, p, pair[0], pair[1], false);
      var autoHits = nativePair(h, p, pair[0], pair[1], true);
      near(h, autoHits[0], nativeHits[0], "First paired hit changed");
      near(h, autoHits[1], nativeHits[1], "Native same-spell repeat behavior changed");
    }
    h.succeed();
  }

  private static float[] actualSpellHits(
      GameTestHelper h, ServerPlayer p, io.github.manasmods.tensura.entity.TensuraProjectile shot)
      throws Exception {
    var t = bareTarget(h, p);
    var method =
        io.github.manasmods.tensura.entity.TensuraProjectile.class.getDeclaredMethod(
            "dealDamage", Entity.class);
    method.setAccessible(true);
    float before = t.getHealth();
    method.invoke(shot, t);
    float first = before - t.getHealth();
    before = t.getHealth();
    method.invoke(shot, t);
    return new float[] {first, before - t.getHealth()};
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void nativeWindFireAndWaterRepeatDamageExplainsTheirDifferentHitBehavior(
      GameTestHelper h) throws Exception {
    var p = player(h);
    var c =
        io.github.manasmods.manascore.config.ConfigRegistry.getConfig(
            io.github.manasmods.tensura.config.ability.magic.AspectualMagicConfig.class);
    var wind =
        new io.github.manasmods.tensura.entity.projectile.magic.WindBladeProjectile(p.level(), p);
    wind.setDamage(c.WindCutter.windDamage);
    wind.setSecondaryDamage(c.WindCutter.magicDamage);
    var fire =
        new io.github.manasmods.tensura.entity.projectile.magic.FireBoltProjectile(p.level(), p);
    fire.setDamage(c.Fire.fireDamage);
    fire.setSecondaryDamage(c.Fire.magicDamage);
    var water =
        new io.github.manasmods.tensura.entity.projectile.magic.WaterBladeProjectile(p.level(), p);
    water.setSecondaryDamage(c.WaterCutter.magicDamage);
    var w = actualSpellHits(h, p, wind);
    var f = actualSpellHits(h, p, fire);
    var a = actualSpellHits(h, p, water);
    System.out.println(
        "Native immediate projectile repeats: Wind="
            + Arrays.toString(w)
            + " Fire="
            + Arrays.toString(f)
            + " Water="
            + Arrays.toString(a));
    h.assertTrue(
        w[0] > 0 && w[1] > 0 && a[0] > 0 && a[1] == 0,
        "Native mixed vs pure-magic behavior did not reproduce");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void metisAppraisalUsesHigherLevelsWithoutStackingAndRestoresOnRespawn(
      GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    mind.getSkill().onPressed(mind, p, 0, 2);
    near(h, p.getAttributeValue(TensuraAttributes.ANALYSIS_LEVEL), 20, "Appraisal level");
    near(
        h,
        p.getAttribute(TensuraAttributes.ANALYSIS_DISTANCE)
            .getModifier(
                io.github.manasmods.tensura.registry.attribute.TensuraGlobalAttributeIds.ANALYSIS)
            .amount(),
        15,
        "Native range");
    MetisAnalysis.refreshAppraisal(p, mind);
    near(h, p.getAttributeValue(TensuraAttributes.ANALYSIS_LEVEL), 20, "Refresh stacked appraisal");
    mind.setMastery(mind.getMaxMastery());
    mind.getSkill().onSkillMastered(mind, p);
    near(h, p.getAttributeValue(TensuraAttributes.ANALYSIS_LEVEL), 40, "Mastered appraisal level");
    near(
        h,
        p.getAttribute(TensuraAttributes.ANALYSIS_DISTANCE)
            .getModifier(
                io.github.manasmods.tensura.registry.attribute.TensuraGlobalAttributeIds.ANALYSIS)
            .amount(),
        25,
        "Mastered native range");
    io.github.manasmods.tensura.util.AttributeHelper.removeAnalysisAttributes(p, true, true, false);
    mind.getSkill().onRespawn(mind, p, false);
    near(h, p.getAttributeValue(TensuraAttributes.ANALYSIS_LEVEL), 40, "Respawn appraisal");
    h.succeed();
  }

  private static LivingEntity analysisTarget(GameTestHelper h, ServerPlayer p, ManasSkill skill) {
    var t = bareTarget(h, p);
    var storage = SkillAPI.getSkillsFrom(t);
    for (var i : List.copyOf(storage.getLearnedSkills())) storage.forgetSkill(i);
    RestrictionData.learnReward(t, skill);
    storage.getSkill(skill).orElseThrow().setMastery(0);
    SkillAPI.getSkillsFrom(p).getSkill(skill).ifPresent(i -> i.setMastery(-1));
    return t;
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void guaranteedAnalysisCopiesResistanceAndNullificationWithShortSeparateCooldowns(
      GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var config = io.github.manasmods.tensura.ability.skill.unique.GreatSageSkill.CONFIG;
    float chance = config.copyChance, masteredChance = config.copyChanceMastered;
    try {
      config.copyChance = 0;
      config.copyChanceMastered = 0;
      var resistance = ResistanceSkills.FLAME_ATTACK_RESISTANCE.get();
      var t = analysisTarget(h, p, resistance);
      h.assertTrue(
          !MetisAnalysis.candidates(p, t).isEmpty(),
          "No eligible resistance: target="
              + SkillAPI.getSkillsFrom(t).getSkill(resistance).orElseThrow().getMastery()
              + " player="
              + SkillAPI.getSkillsFrom(p)
                  .getSkill(resistance)
                  .map(i -> i.getMastery())
                  .orElse(-999d));
      h.assertTrue(
          mind.canInteractSkill(p) && mind.canActivateSkill(p, 3), "Analysis cannot activate");
      h.assertTrue(p.hasLineOfSight(t), "No line of sight");
      h.assertTrue(
          MetisAnalysis.copyFrom(p, mind, t),
          "Guaranteed resistance analysis failed: cost="
              + resistance.getAcquiringMagiculeCost(resistance.createDefaultInstance())
              + " max="
              + p.getAttributeValue(TensuraAttributes.MAX_MAGICULE));
      h.assertTrue(
          SkillAPI.getSkillsFrom(p)
              .getSkill(resistance)
              .filter(i -> i.getMastery() >= 0)
              .isPresent(),
          "Resistance was not learned");
      h.assertTrue(
          SkillAPI.getSkillsFrom(t).getSkill(resistance).isPresent(),
          "Analysis stole target's resistance");
      h.assertTrue(
          mind.getCoolDown(3) == 5 && mind.getCoolDown(1) == 0,
          "Wrong success cooldown or shared auto-cast cooldown");
      MetisAnalysis.press(p, mind);
      h.assertTrue(mind.getCoolDown(3) == 5, "Pressing on cooldown shortened it");
      var nul = ResistanceSkills.FLAME_ATTACK_NULLIFICATION.get();
      var second = analysisTarget(h, p, nul);
      h.assertTrue(!MetisAnalysis.copyFrom(p, mind, second), "Analysis bypassed its cooldown");
      mind.setCoolDown(0, 3);
      mind.setMastery(mind.getMaxMastery());
      h.assertTrue(
          MetisAnalysis.copyFrom(p, mind, second), "Guaranteed nullification analysis failed");
      h.assertTrue(mind.getCoolDown(3) == 2, "Mastered cooldown was not reduced");
      h.assertTrue(
          SkillAPI.getSkillsFrom(p).getSkill(nul).filter(i -> i.getMastery() >= 0).isPresent(),
          "Nullification was not learned");
      mind.setCoolDown(0, 3);
      h.assertTrue(!MetisAnalysis.copyFrom(p, mind, second), "Known skill copied repeatedly");
    } finally {
      config.copyChance = chance;
      config.copyChanceMastered = masteredChance;
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void analysisRetainsTierTemporaryRangeAndProjectileCopyRules(GameTestHelper h) {
    var p = player(h);
    var mind = metis(p, false);
    var t = analysisTarget(h, p, UniqueSkills.GREAT_SAGE.get());
    h.assertTrue(
        MetisAnalysis.candidates(p, t).isEmpty() && !MetisAnalysis.copyFrom(p, mind, t),
        "Analysis copied unique tier");
    var pending = ResistanceSkills.FLAME_ATTACK_RESISTANCE.get().createDefaultInstance();
    pending.setMastery(-1);
    h.assertTrue(!MetisAnalysis.copyable(pending), "Unlearned resistance eligible");
    pending.setMastery(0);
    pending.setRemoveTime(100);
    h.assertTrue(!MetisAnalysis.copyable(pending), "Temporary resistance eligible");
    var ultimate = HeavenlyRestriction.METIS.get().createDefaultInstance();
    ultimate.setMastery(0);
    h.assertTrue(!MetisAnalysis.copyable(ultimate), "Ultimate eligible");
    var protectedSkill = ExtraSkills.LAW_MANIPULATION.get().createDefaultInstance();
    protectedSkill.setMastery(0);
    h.assertTrue(!MetisAnalysis.copyable(protectedSkill), "Protected skill eligible");
    var spell = AspectualMagics.FIRE.get().createDefaultInstance();
    spell.setMastery(0);
    var shot =
        new io.github.manasmods.tensura.entity.projectile.magic.FireBoltProjectile(p.level(), t);
    shot.setSkill(t, spell, (Magic) spell.getSkill(), 0);
    shot.setPos(p.position().add(4, 1, 0));
    h.assertTrue(
        MetisAnalysis.copyProjectile(p, mind, shot),
        "Native copyable magic projectile was not analyzed");
    mind.setCoolDown(0, 3);
    h.assertTrue(
        !MetisAnalysis.copyProjectile(p, mind, shot),
        "Already owned projectile magic copied again");
    var remote = analysisTarget(h, p, ResistanceSkills.FLAME_ATTACK_RESISTANCE.get());
    remote.setPos(p.position().add(31, 0, 0));
    h.assertTrue(!MetisAnalysis.copyFrom(p, mind, remote), "Native target range bypassed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void distinctManualMagicProjectilesDealFullDamageWithoutChangingPersistentCadence(
      GameTestHelper h) throws Exception {
    var p = player(h);
    var method =
        io.github.manasmods.tensura.entity.TensuraProjectile.class.getDeclaredMethod(
            "dealDamage", Entity.class);
    method.setAccessible(true);
    for (int n = 0; n < 3; n++) {
      var t = bareTarget(h, p);
      var magic =
          n == 0
              ? AspectualMagics.WIND_CUTTER.get()
              : n == 1 ? AspectualMagics.FIRE.get() : AspectualMagics.WATER_CUTTER.get();
      var instance = learn(p, magic, true);
      float total = n == 1 ? 30 : 40;
      io.github.manasmods.tensura.entity.TensuraProjectile last = null;
      for (int hit = 0; hit < 2; hit++) {
        var shot =
            n == 0
                ? new io.github.manasmods.tensura.entity.projectile.magic.WindBladeProjectile(
                    p.level(), p)
                : n == 1
                    ? new io.github.manasmods.tensura.entity.projectile.magic.FireBoltProjectile(
                        p.level(), p)
                    : new io.github.manasmods.tensura.entity.projectile.magic.WaterBladeProjectile(
                        p.level(), p);
        shot.setSkill(p, instance, (Magic) magic, 0);
        shot.setDamage(n == 2 ? 0 : 10);
        shot.setSecondaryDamage(n == 0 ? 30 : n == 1 ? 20 : 40);
        float before = t.getHealth();
        method.invoke(shot, t);
        near(h, before - t.getHealth(), total, "Distinct native magic impact " + n + "/" + hit);
        last = shot;
      }
      float before = t.getHealth();
      method.invoke(last, t);
      near(
          h,
          before - t.getHealth(),
          n == 0 ? 30 : n == 1 ? 20 : 0,
          "Persistent projectile native repeat cadence");
    }
    var arrow =
        new net.minecraft.world.entity.projectile.Arrow(
            p.level(), p, new ItemStack(Items.ARROW), null);
    h.assertTrue(
        AutoCastCombat.key(p.damageSources().arrow(arrow, p)) == null,
        "Ordinary arrow got magic windows");
    h.succeed();
  }
}
