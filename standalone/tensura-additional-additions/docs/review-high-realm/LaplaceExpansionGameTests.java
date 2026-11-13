package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.laplace.*;
import dev.heavenlyrestriction.laplace.expand.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.magic.AspectualMagics;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("laplacereview")
@PrefixGameTestTemplate(false)
public class LaplaceExpansionGameTests {
  @GameTest(template = "empty", timeoutTicks = 200)
  public static void fireBallUsesItsRealConfigAndOwnerOnlyEdits(GameTestHelper h) {
    var p = player(h);
    var own = SplitMindGameTests.learn(p, HeavenlyRestriction.LAPLACE.get(), true);
    own.onToggleOn(p);
    var i = SplitMindGameTests.learn(p, AspectualMagics.FIRE_BALL.get(), true);
    var a = LaplaceActions.find(p, i, 0);
    h.assertTrue(a != null, "Missing Fire Ball board");
    System.out.println(
        "LAPLACE EXPANDED " + a.title() + " " + a.model().defaults() + " " + a.model().labels());
    var refs = ParameterInspection.fields(i.getSkill().getClass(), 0, true);
    var damage =
        refs.stream().filter(f -> f.name().equals("fireDamageCharged")).findFirst().orElseThrow();
    double nativeDamage = NativeParameters.read(damage);
    h.assertTrue(
        Double.isFinite(nativeDamage) && nativeDamage > 0, "Cannot read live native config");
    var eq = new LinkedHashMap<>(a.model().defaults());
    eq.put(damage.key(), "base_" + damage.key() + " * 2");
    h.assertTrue(
        Laplace.save(p, a.key(), eq, true).startsWith("Saved."), "Cannot save expanded action");
    var result =
        LaplaceRuntime.withBase(
                a.model(),
                "base_cost",
                io.github.manasmods.manascore.config.ConfigRegistry.getConfig(
                        io.github.manasmods.tensura.config.ability.magic.AspectualMagicConfig.class)
                    .FireBall
                    .magiculeCostCharged)
            .evaluate(eq);
    double mp = TensuraStorages.getExistenceFrom(p).getMagicule();
    var shots =
        new ArrayList<io.github.manasmods.tensura.entity.projectile.TensuraFlyingProjectile>();
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> listener =
        e -> {
          if (e.getEntity()
                  instanceof
                  io.github.manasmods.tensura.entity.projectile.TensuraFlyingProjectile shot
              && shot.getOwner() == p) shots.add(shot);
        };
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
    try {
      i.onRelease(p, 1000, 0, 0);
    } finally {
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
    }
    h.assertTrue(!shots.isEmpty(), "No actual native projectile");
    h.assertTrue(
        Math.abs(shots.getFirst().getDamage() - nativeDamage * 2) < .01,
        "Native parameter not applied: " + shots.getFirst().getDamage());
    h.assertTrue(
        Math.abs(
                mp
                    - TensuraStorages.getExistenceFrom(p).getMagicule()
                    - result.values().get("magicule_cost"))
            < .02,
        "Expanded cost not paid");
    h.assertTrue(NativeParameters.read(damage) == nativeDamage, "Shared config was mutated");
    h.assertTrue(ParameterRuntime.decimal(7, damage.key()) == 7, "Parameter scope leaked");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 1200)
  public static void allSupportedRegistrySkillsHaveAuditedBoards(GameTestHelper h) {
    var p = player(h);
    SplitMindGameTests.learn(p, HeavenlyRestriction.LAPLACE.get(), true).onToggleOn(p);
    int skills = 0, boards = 0, fields = 0, empty = 0;
    var rows = new ArrayList<String>();
    rows.add("Skill\tMode\tEditable effects\tCost budget\tDetails");
    for (var skill : SkillAPI.getSkillRegistry()) {
      var i = skill.createDefaultInstance();
      if (!ExpandedActions.namespace(i)) continue;
      i.setMastery(i.getMaxMastery());
      skills++;
      var modes =
          LaplaceActions.legacy(i)
              ? (i.getSkill() == AspectualMagics.WATER_CUTTER.get() && i.isMastered(p)
                  ? Set.of(0, 1)
                  : Set.of(0))
              : ExpandedActions.modes(p, i);
      for (int mode : modes) {
        var a = LaplaceActions.find(p, i, mode);
        if (a == null) continue;
        var model = a.model();
        model.evaluate(model.defaults());
        h.assertTrue(
            model.defaults().size() + 4 <= FormulaSet.MAX_EQUATIONS,
            "Too many native outputs: " + i.getSkillId());
        if (mode >= 0
            && model.inputs().getOrDefault("base_cost", 0d) > 0
            && !ParameterInspection.paying(i.getSkill().getClass(), mode, true)
            && !LaplaceActions.legacy(i))
          System.out.println("LAPLACE CUSTOM PAYMENT " + i.getSkillId() + "#" + mode);
        boards++;
        fields += model.powerOutputs().size();
        if (model.powerOutputs().isEmpty()) empty++;
        rows.add(
            i.getSkillId()
                + "\t"
                + mode
                + "\t"
                + model.powerOutputs().size()
                + "\t"
                + model.inputs().get("base_cost")
                + "\t"
                + a.detail());
      }
    }
    try {
      java.nio.file.Files.write(
          java.nio.file.Path.of(
              "/tmp/laplace-expansion/coverage-"
                  + (io.github.manasmods.tensura.ability.magic.Magic.class
                              .getPackage()
                              .getImplementationVersion()
                          == null
                      ? "native"
                      : "api")
                  + ".tsv"),
          rows);
    } catch (java.io.IOException ex) {
      throw new IllegalStateException(ex);
    }
    System.out.println(
        "LAPLACE COVERAGE skills="
            + skills
            + " boards="
            + boards
            + " fields="
            + fields
            + " fixedOrCostOnly="
            + empty);
    h.assertTrue(skills > 250 && fields > 250, "Registry catalogue incomplete");
    h.succeed();
  }

  private static ManasSkillInstance own(
      net.minecraft.server.level.ServerPlayer p, ManasSkill skill) {
    return SplitMindGameTests.learn(p, skill, true);
  }

  private static LaplaceActions.Action board(
      GameTestHelper h,
      net.minecraft.server.level.ServerPlayer p,
      ManasSkillInstance i,
      int mode,
      String field,
      double factor,
      PaymentPlan plan) {
    var a = LaplaceActions.find(p, i, mode);
    h.assertTrue(a != null, "Missing board " + i.getSkillId() + "#" + mode);
    String key =
        a.model().powerOutputs().stream()
            .filter(
                k ->
                    a.model()
                        .labels()
                        .get(k)
                        .toLowerCase(java.util.Locale.ROOT)
                        .contains(field.toLowerCase(java.util.Locale.ROOT)))
            .findFirst()
            .orElseThrow();
    var eq = new LinkedHashMap<>(a.model().defaults());
    eq.put(key, "base_" + key + " * " + factor);
    Laplace.owned(p).orElseThrow().getOrCreateTag().remove("EditUntil");
    var saved =
        Laplace.save(p, a.key(), eq, true, plan == null ? a.model().payment().plan() : plan);
    h.assertTrue(saved.startsWith("Saved."), saved);
    return LaplaceActions.find(p, i, mode);
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void auraBattlewillKeepsAuraCurrencyAndEditsActualProjectile(GameTestHelper h) {
    var p = player(h);
    own(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    var i =
        own(
            p,
            SkillAPI.getSkillRegistry()
                .get(net.minecraft.resources.ResourceLocation.parse("tensura:aura_slash")));
    p.setItemInHand(
        net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
    var a = board(h, p, i, 0, "attack multiplier", 2, null);
    h.assertTrue(
        a.model().payment().plan().equals(PaymentPlan.AURA),
        "Aura action became a magicule action");
    double aura = TensuraStorages.getExistenceFrom(p).getAura(),
        mp = TensuraStorages.getExistenceFrom(p).getMagicule();
    i.onPressed(p, 0, 0);
    double bill =
        a.model()
            .evaluate(Laplace.equations(Laplace.owned(p).orElseThrow(), a))
            .values()
            .get("magicule_cost");
    h.assertTrue(
        Math.abs(aura - TensuraStorages.getExistenceFrom(p).getAura() - bill) < .05,
        "Battlewill fee incorrect");
    h.assertTrue(TensuraStorages.getExistenceFrom(p).getMagicule() == mp, "Battlewill consumed MP");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void prospectorPassiveChangesItsRealMiningEvent(GameTestHelper h) {
    var p = player(h);
    own(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    var i = own(p, HeavenlyRestriction.PROSPECTOR.get());
    board(h, p, i, -1, "mining speed bonus", 2, null);
    i.onToggleOn(p);
    var event =
        new net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed(
            p,
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
            1,
            net.minecraft.core.BlockPos.ZERO);
    ProspectorMining.speed(event);
    h.assertTrue(Math.abs(event.getNewSpeed() - 2) < .01, "Prospector bonus not doubled");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void editedAgilityUsesRealNativeEffectDuration(GameTestHelper h) {
    var p = player(h);
    own(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    var i = own(p, AspectualMagics.AGILITY.get());
    var a = board(h, p, i, 0, "speed duration", 2, null);
    i.onRelease(p, 1000, 0, 0);
    var effect = p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
    h.assertTrue(effect != null, "No Agility effect");
    var spec =
        a.model().powerOutputs().stream()
            .filter(k -> a.model().labels().get(k).toLowerCase().contains("speed duration"))
            .findFirst()
            .orElseThrow();
    double desired =
        a.model().evaluate(Laplace.equations(Laplace.owned(p).orElseThrow(), a)).values().get(spec);
    h.assertTrue(Math.abs(effect.getDuration() - desired) < 1, "Native duration unchanged");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void expandedPaymentAllowsZeroMagiculeFireball(GameTestHelper h) {
    var p = player(h);
    own(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    var i = own(p, AspectualMagics.FIRE_BALL.get());
    board(h, p, i, 0, "fire damage charged", 2, PaymentPlan.AURA);
    TensuraStorages.getExistenceFrom(p).setMagicule(0);
    double aura = TensuraStorages.getExistenceFrom(p).getAura();
    h.assertTrue(
        dev.heavenlyrestriction.splitmind.SplitMindCasting.funded(p, i, 0),
        "Automatic preparation checked the wrong pool");
    i.onRelease(p, 1000, 0, 0);
    h.assertTrue(TensuraStorages.getExistenceFrom(p).getAura() < aura, "Charged variant was free");
    h.assertTrue(
        TensuraStorages.getExistenceFrom(p).getMagicule() == 0, "Charged variant consumed MP");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void effectSnapshotsSurviveCopyAndNativeSaveReload(GameTestHelper h) {
    var p = player(h);
    own(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    var i = own(p, AspectualMagics.AGILITY.get());
    board(h, p, i, 0, "speed duration", 2, null);
    i.onRelease(p, 1000, 0, 0);
    var effect = p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
    var values = ((EffectParameters) (Object) effect).additions$parameters();
    h.assertTrue(values != null, "No frozen effect snapshot");
    var copy = new net.minecraft.world.effect.MobEffectInstance(effect);
    h.assertTrue(
        values.equals(((EffectParameters) (Object) copy).additions$parameters()),
        "Copy lost snapshot");
    var loaded =
        net.minecraft.world.effect.MobEffectInstance.load(
            (net.minecraft.nbt.CompoundTag) effect.save());
    h.assertTrue(
        values.equals(((EffectParameters) (Object) loaded).additions$parameters()),
        "Native save/load lost snapshot");
    var stronger =
        new net.minecraft.world.effect.MobEffectInstance(
            effect.getEffect(), 1, effect.getAmplifier() + 1);
    copy.update(stronger);
    var hidden = ((EffectParameters) (Object) copy).additions$hidden();
    h.assertTrue(hidden != null, "Native effect did not retain the weaker effect");
    var withHidden =
        net.minecraft.world.effect.MobEffectInstance.load(
            (net.minecraft.nbt.CompoundTag) copy.save());
    h.assertTrue(
        values.equals(
            ((EffectParameters)
                    (Object) ((EffectParameters) (Object) withHidden).additions$hidden())
                .additions$parameters()),
        "Hidden effect save/load lost frozen parameters");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void largeCatalogueSendsOnlyTheSelectedBoard(GameTestHelper h) {
    var p = player(h);
    own(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    for (var skill :
        List.of(
            AspectualMagics.FIRE_BALL.get(),
            AspectualMagics.FIRE_LANCE.get(),
            AspectualMagics.AGILITY.get())) own(p, skill);
    LaplaceNetwork.open(p);
    var first = LaplaceNetwork.snapshot(p);
    h.assertTrue(
        first.getList("rows", 10).stream()
            .map(t -> (net.minecraft.nbt.CompoundTag) t)
            .noneMatch(t -> t.contains("model")),
        "Unselected new boards were eagerly sent");
    var row = (net.minecraft.nbt.CompoundTag) first.getList("rows", 10).getFirst();
    LaplaceNetwork.handle(
        p,
        new LaplaceNetwork.Request(
            first.getUUID("session"), "select", row.getString("key"), false, Map.of()));
    var second = LaplaceNetwork.snapshot(p);
    h.assertTrue(
        second.getList("rows", 10).stream()
                .map(t -> (net.minecraft.nbt.CompoundTag) t)
                .filter(t -> t.contains("model"))
                .count()
            == 1,
        "Selection did not load exactly one board");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void slotAttributeBonusRefreshesAndRestoresWithLaplace(GameTestHelper h) {
    var p = player(h);
    var lp = own(p, HeavenlyRestriction.LAPLACE.get());
    lp.onToggleOn(p);
    var stat = net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;
    var candidate =
        java.util.stream.StreamSupport.stream(SkillAPI.getSkillRegistry().spliterator(), false)
            .filter(
                skill ->
                    skill.getRegistryName().getNamespace().equals("tensura")
                        && skill instanceof io.github.manasmods.tensura.ability.skill.Skill)
            .filter(
                skill ->
                    ((AttributeParameters) skill).additions$attributeTemplates().containsKey(stat))
            .findFirst()
            .orElseThrow();
    var i = own(p, candidate);
    var template =
        (ManasSkill.AttributeTemplate)
            ((AttributeParameters) i.getSkill()).additions$attributeTemplates().get(stat);
    double nativeValue = i.getAttributeModifierAmplifier(p, stat, template, 0);
    board(h, p, i, -1, "Native attribute bonus multiplier", 2, null);
    double edited = i.getAttributeModifierAmplifier(p, stat, template, 0);
    h.assertTrue(
        Math.abs(edited - 2 * nativeValue) < .01 && nativeValue != 0,
        "Native template did not scale: " + i.getSkillId() + "/" + nativeValue + "/" + edited);
    lp.onToggleOff(p);
    h.assertTrue(
        Math.abs(i.getAttributeModifierAmplifier(p, stat, template, 0) - nativeValue) < .01,
        "Attribute bonus persisted after Laplace stopped");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void consumingHelpersRetainReservesAndFailWithoutPartialDebit(GameTestHelper h) {
    var p = player(h);
    own(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    var i = own(p, AspectualMagics.FIRE_BALL.get());
    board(h, p, i, 0, "fire damage charged", 1, PaymentPlan.AURA);
    var energy = TensuraStorages.getExistenceFrom(p);
    energy.setMagicule(0);
    energy.setAura(110);
    try (var frame = ExpandedRuntime.open(p, i, 0)) {
      double missing =
          io.github.manasmods.tensura.util.EnergyHelper.isOutOfMagiculeConsuming(p, 100, 10);
      h.assertTrue(
          missing == 0 && Math.abs(energy.getAura() - 10) < .01,
          "Converted MP consuming helper lost reserve or charged incorrectly");
    }
    try (var raw =
        dev.heavenlyrestriction.exponentiation.ExponentiationContext.raw(
            p, dev.heavenlyrestriction.exponentiation.ExponentiationContext.RESTORE)) {
      energy.setAura(109);
    }
    try (var frame = ExpandedRuntime.open(p, i, 0)) {
      double missing =
          io.github.manasmods.tensura.util.EnergyHelper.isOutOfMagiculeConsuming(p, 100, 10);
      h.assertTrue(
          missing > 0 && energy.getAura() == 109,
          "Unfunded reserve consumed resources: shortfall="
              + missing
              + " aura="
              + energy.getAura());
    }
    h.assertTrue(!ParameterRuntime.active() && !PaymentRouting.scoped(p), "Consuming scope leaked");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void frozenEntityRetainsItsEquationAndCurrencyAfterLaplaceStops(GameTestHelper h) {
    var p = player(h);
    var lp = own(p, HeavenlyRestriction.LAPLACE.get());
    lp.onToggleOn(p);
    var i = own(p, AspectualMagics.FIRE_BALL.get());
    var a = board(h, p, i, 0, "fire damage charged", 2, PaymentPlan.AURA);
    var entity = new net.minecraft.world.entity.projectile.Snowball(p.level(), p);
    try (var frame = ExpandedRuntime.open(p, i, 0)) {
      ParameterSnapshots.spawned(
          new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(entity, p.level()));
    }
    lp.onToggleOff(p);
    var energy = TensuraStorages.getExistenceFrom(p);
    double before = energy.getAura();
    try (var frozen = ParameterSnapshots.entity(entity)) {
      var current = ExpandedRuntime.current();
      h.assertTrue(current != null && current.owner() == p, "Frozen caster frame not restored");
      h.assertTrue(
          !io.github.manasmods.tensura.util.EnergyHelper.isOutOfEnergy(p, 0, 100),
          "Frozen payment failed");
    }
    h.assertTrue(
        Math.abs(before - energy.getAura() - 300) < .02, "Frozen equation or payment changed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void coordinateConfirmationChargesChosenPoolForActualDistance(GameTestHelper h) {
    var p = player(h);
    own(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    var i = own(p, HeavenlyRestriction.INSTANT_TRANSMISSION.get());
    i.setMastery(0);
    var a = LaplaceActions.find(p, i, 0);
    h.assertTrue(
        a != null && a.model().inputs().containsKey("__deferred_cost"), "Missing coordinate board");
    h.assertTrue(
        Laplace.save(p, a.key(), a.model().defaults(), true, PaymentPlan.MAGICULES)
            .startsWith("Saved."),
        "Could not save coordinate payment");
    var energy = TensuraStorages.getExistenceFrom(p);
    double mp = energy.getMagicule(), aura = energy.getAura();
    var at = p.position().add(16, 0, 0);
    boolean accepted =
        HeavenlyRestriction.INSTANT_TRANSMISSION
            .get()
            .requestWarp(p, i, at.x, at.y, at.z, p.level().dimension(), false);
    h.assertTrue(accepted && InstantTransmissionArt.charging(p), "Coordinate request failed");
    h.assertTrue(
        Math.abs(mp - energy.getMagicule() - 160) < .02 && energy.getAura() == aura,
        "Coordinate bill used wrong distance or resource");
    InstantTransmissionArt.cancel(p);
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void nativeSpatialMotionUsesItsWarpModeRatherThanBlink(GameTestHelper h) {
    var p = player(h);
    own(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    var i = own(p, io.github.manasmods.tensura.registry.skill.ExtraSkills.SPATIAL_MOTION.get());
    i.setMastery(0);
    var spatial = (io.github.manasmods.tensura.ability.subclass.ISpatialMovement) i.getSkill();
    int mode = spatial.getSpatialMovementModeIndex();
    h.assertTrue(mode == 1, "Native coordinate mode fixture changed");
    var a = LaplaceActions.find(p, i, mode);
    h.assertTrue(
        Laplace.save(p, a.key(), a.model().defaults(), true, PaymentPlan.AURA).startsWith("Saved."),
        "Could not save native coordinate payment");
    var energy = TensuraStorages.getExistenceFrom(p);
    energy.setMagicule(0);
    double aura = energy.getAura(), bill = spatial.getWarpCost(16);
    var at = p.position().add(16, 0, 0);
    io.github.manasmods.tensura.network.c2s.RequestSpatialActionPacket.handleWarp(
        p, i, spatial, at.x, at.y, at.z, p.level().dimension(), false);
    h.assertTrue(
        Math.abs(aura - energy.getAura() - bill) < .02 && energy.getMagicule() == 0,
        "Native warp billed Blink or the wrong resource: "
            + (aura - energy.getAura())
            + "/"
            + bill);
    h.succeed();
  }
}
