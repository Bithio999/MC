package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.exponentiation.ExponentiationContext;
import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.magic.AspectualMagics;
import io.github.manasmods.tensura.registry.skill.CommonSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("laplacereview")
@PrefixGameTestTemplate(false)
public final class LaplacePaymentGameTests {
  private static void near(GameTestHelper h, double a, double b, String m) {
    h.assertTrue(Math.abs(a - b) < .02, m + ": " + a + " / " + b);
  }

  private static ManasSkillInstance learn(ServerPlayer p, ManasSkill skill) {
    return SplitMindGameTests.learn(p, skill, true);
  }

  private static ServerPlayer setup(GameTestHelper h) {
    var p = player(h);
    learn(p, HeavenlyRestriction.LAPLACE.get()).onToggleOn(p);
    p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
    p.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(10000);
    p.getAttribute(TensuraAttributes.MAGIC_COST_MULTIPLIER).setBaseValue(1);
    reset(p, 10000, 10000, 10000, 10000);
    return p;
  }

  private static void reset(ServerPlayer p, double mp, double au, double hp, double sp) {
    try (var raw = ExponentiationContext.raw(p, ExponentiationContext.RESTORE)) {
      var e = TensuraStorages.getExistenceFrom(p);
      e.setMagicule(mp);
      e.setAura(au);
      p.setHealth((float) hp);
      e.setSpiritualHealth(sp);
    }
  }

  private static LaplaceActions.Action save(
      GameTestHelper h, ServerPlayer p, ManasSkillInstance i, int mode, PaymentPlan plan) {
    var a = LaplaceActions.find(p, i, mode);
    Laplace.owned(p).orElseThrow().getOrCreateTag().remove("EditUntil");
    var answer = Laplace.save(p, a.key(), a.model().defaults(), true, plan);
    h.assertTrue(answer.startsWith("Saved."), answer);
    return LaplaceActions.find(p, i, mode);
  }

  private static double cost(ServerPlayer p, ManasSkillInstance i, int mode) {
    return ((io.github.manasmods.tensura.ability.TensuraSkill) i.getSkill())
        .getMagiculeCost(p, i, mode);
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void auraPaysNativeFireWithZeroMagicules(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.FIRE.get());
    var a = save(h, p, i, 0, PaymentPlan.AURA);
    double bill = cost(p, i, 0);
    reset(p, 0, 10000, 10000, 10000);
    i.getSkill().onRelease(i, p, 1000, 0, 0);
    var e = TensuraStorages.getExistenceFrom(p);
    near(h, e.getMagicule(), 0, "MP changed");
    near(h, e.getAura(), 10000 - bill, "Aura not charged exactly once");
    h.assertTrue(Laplace.payment(p, a.key()).equals(PaymentPlan.AURA), "Payment did not persist");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void mixedPaymentFailsAtomicallyAndNeverLeaksScope(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.WIND_CUTTER.get());
    save(h, p, i, 0, new PaymentPlan(.5, .5, 0, 0));
    double bill = cost(p, i, 0);
    reset(p, bill, 0, 10000, 10000);
    h.assertTrue(EnergyHelper.isOutOfEnergy(p, i, 0), "Unfunded mix succeeded");
    var e = TensuraStorages.getExistenceFrom(p);
    near(h, e.getMagicule(), bill, "Failed payment consumed MP");
    near(h, e.getAura(), 0, "Failed payment changed aura");
    reset(p, bill, bill, 10000, 10000);
    h.assertTrue(!EnergyHelper.isOutOfEnergy(p, i, 0), "Funded mix failed");
    near(h, e.getMagicule(), bill / 2, "MP mix");
    near(h, e.getAura(), bill / 2, "Aura mix");
    h.assertTrue(!PaymentRouting.converted(p), "Payment scope leaked");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void vitalPaymentBypassesDamageAndRespectsReserves(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.FIRE.get());
    save(h, p, i, 0, new PaymentPlan(0, 0, .5, .5));
    double bill = cost(p, i, 0), hp = 10000, sp = 10000;
    reset(p, 0, 0, hp, sp);
    p.invulnerableTime = 100;
    h.assertTrue(!EnergyHelper.isOutOfEnergy(p, i, 0), "Vital payment failed");
    var e = TensuraStorages.getExistenceFrom(p);
    near(h, p.getHealth(), hp - bill / 20, "HP payment");
    near(h, e.getSpiritualHealth(), sp - bill / 20, "SHP payment");
    h.assertTrue(p.hurtTime == 0, "Payment became a damage event");
    reset(p, 0, 0, 1, sp);
    h.assertTrue(EnergyHelper.isOutOfEnergy(p, i, 0), "Killed caster with HP cost");
    near(h, e.getSpiritualHealth(), sp, "Failed HP check charged SHP");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void nativeMagicCostMultiplierAppliesBeforeConversion(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.FIRE.get());
    save(h, p, i, 0, PaymentPlan.AURA);
    double bill = cost(p, i, 0);
    p.getAttribute(TensuraAttributes.MAGIC_COST_MULTIPLIER).setBaseValue(.5);
    reset(p, 0, 10000, 10000, 10000);
    h.assertTrue(!EnergyHelper.isOutOfEnergy(p, i, 0), "Discounted cast failed");
    near(
        h,
        TensuraStorages.getExistenceFrom(p).getAura(),
        10000 - bill * .5,
        "Native cost modifier lost");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void healingUsesAuraAndCannotConsumeVitals(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.HEALING.get());
    var a = save(h, p, i, 0, PaymentPlan.AURA);
    reset(p, 0, 10000, p.getMaxHealth() - 1, 10000);
    i.getSkill().onRelease(i, p, 1000, 0, 0);
    var e = TensuraStorages.getExistenceFrom(p);
    near(h, p.getHealth(), p.getMaxHealth(), "Aura-funded healing failed");
    near(h, e.getMagicule(), 0, "Healing consumed MP");
    h.assertTrue(e.getAura() < 10000, "Healing was free");
    Laplace.owned(p).orElseThrow().getOrCreateTag().remove("EditUntil");
    boolean rejected = false;
    try {
      Laplace.save(p, a.key(), a.model().defaults(), true, PaymentPlan.HP);
    } catch (IllegalArgumentException expected) {
      rejected = true;
    }
    h.assertTrue(rejected, "HP-funded healing accepted");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void mixedHealingCannotPartiallySpendOrHeal(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.HEALING.get());
    save(h, p, i, 0, new PaymentPlan(.5, .5, 0, 0));
    reset(p, 10000, 0, 1, 10000);
    i.getSkill().onRelease(i, p, 1000, 0, 0);
    var e = TensuraStorages.getExistenceFrom(p);
    near(h, e.getMagicule(), 10000, "Unfunded healing consumed MP");
    near(h, p.getHealth(), 1, "Unfunded healing healed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void regenerationPaysAuraUpkeepAndStopsBeforeEditing(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, CommonSkills.SELF_REGENERATION.get());
    var a = save(h, p, i, 0, PaymentPlan.AURA);
    double bill = cost(p, i, 0);
    reset(p, 0, 10000, 1, 10000);
    i.setToggled(true);
    i.getSkill().onTick(i, p);
    near(h, TensuraStorages.getExistenceFrom(p).getAura(), 10000 - bill, "Upkeep payment");
    Laplace.owned(p).orElseThrow().getOrCreateTag().remove("EditUntil");
    h.assertTrue(
        !Laplace.save(p, a.key(), a.model().defaults(), true, PaymentPlan.MAGICULES)
            .startsWith("Saved."),
        "Active upkeep edit accepted");
    h.assertTrue(
        Laplace.save(p, a.key(), a.model().defaults(), false).startsWith("Saved."),
        "Active upkeep could not be disabled");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void paymentMetadataRoundTripsAndDisableIgnoresPacketPlan(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.FIRE.get());
    var a = save(h, p, i, 0, new PaymentPlan(.25, .25, .25, .25));
    var decoded = LaplaceNetwork.decode(LaplaceNetwork.encode(a.model()));
    h.assertTrue(decoded.payment().equals(a.model().payment()), "Model payment roundtrip");
    var copy = Laplace.owned(p).orElseThrow().copy();
    h.assertTrue(
        Laplace.readPlan(copy.getOrCreateTag().getCompound(Laplace.PROFILES).getCompound(a.key()))
            .equals(a.model().payment().plan()),
        "Skill copy lost plan");
    LaplaceNetwork.open(p);
    Laplace.owned(p).orElseThrow().getOrCreateTag().remove("EditUntil");
    var session = LaplaceNetwork.snapshot(p).getUUID("session");
    LaplaceNetwork.handle(
        p,
        new LaplaceNetwork.Request(session, "disable", a.key(), false, Map.of(), PaymentPlan.AURA));
    h.assertTrue(
        Laplace.payment(p, a.key()).equals(a.model().payment().plan()),
        "Disable trusted packet payment");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void invalidPaymentMetadataFallsBackToNative(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.FIRE.get());
    var a = save(h, p, i, 0, PaymentPlan.AURA);
    var row =
        Laplace.owned(p)
            .orElseThrow()
            .getOrCreateTag()
            .getCompound(Laplace.PROFILES)
            .getCompound(a.key());
    row.getCompound("payment").putDouble("aura", Double.NaN);
    h.assertTrue(
        !Laplace.effectiveEnabled(p, Laplace.owned(p).orElseThrow(), a.key()),
        "Invalid metadata remained active");
    double before = TensuraStorages.getExistenceFrom(p).getMagicule();
    h.assertTrue(!EnergyHelper.isOutOfEnergy(p, i, 0), "Native fallback failed");
    h.assertTrue(
        TensuraStorages.getExistenceFrom(p).getMagicule() < before,
        "Invalid payment produced free cast");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void repeatedWaterCastsPaySelectedResourceEachTime(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.WATER_CUTTER.get());
    save(h, p, i, 1, PaymentPlan.AURA);
    double bill = cost(p, i, 1);
    reset(p, 0, 10000, 10000, 10000);
    for (int n = 0; n < 2; n++)
      h.assertTrue(!EnergyHelper.isOutOfEnergy(p, i, 1), "Repeat payment failed");
    near(
        h,
        TensuraStorages.getExistenceFrom(p).getAura(),
        10000 - 2 * bill,
        "Repeat double charged or skipped payment");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void convertedCastingRetainsAutomaticDiscountExactlyOnce(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.FIRE.get());
    learn(p, HeavenlyRestriction.METIS.get());
    save(h, p, i, 0, PaymentPlan.AURA);
    double bill = cost(p, i, 0);
    reset(p, 0, 10000, 10000, 10000);
    boolean failed =
        dev.heavenlyrestriction.splitmind.SplitMindCasting.autoInvoke(
            p, i, 0, p, () -> EnergyHelper.isOutOfEnergy(p, i, 0));
    h.assertTrue(!failed, "Automatic converted cast failed");
    near(
        h,
        TensuraStorages.getExistenceFrom(p).getAura(),
        10000 - bill * .6,
        "Metis discount applied twice or lost");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void staffFundingIsPreservedForMPAndExcludedForConvertedPayments(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.FIRE.get());
    var item =
        net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
            .filter(
                v ->
                    v.builtInRegistryHolder()
                        .is(io.github.manasmods.tensura.data.TensuraItemTags.SPELL_CAST_WEAPONS))
            .findFirst()
            .orElseThrow();
    var stack = new net.minecraft.world.item.ItemStack(item);
    var ep =
        io.github.manasmods.tensura.registry.item.misc.TensuraDataComponents.EP_DURABILITY.get();
    stack.set(ep, 10000d);
    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
    p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
    h.assertTrue(p.getUseItem() == stack, "Fixture staff not active");
    save(h, p, i, 0, PaymentPlan.MAGICULES);
    double bill = cost(p, i, 0);
    reset(p, 0, 0, 10000, 10000);
    h.assertTrue(!EnergyHelper.isOutOfEnergy(p, i, 0), "Native staff cast failed");
    near(h, stack.get(ep), 10000 - bill, "Native staff EP payment lost");
    save(h, p, i, 0, PaymentPlan.AURA);
    double before = stack.get(ep);
    h.assertTrue(EnergyHelper.isOutOfEnergy(p, i, 0), "Staff funded an aura payment");
    near(h, stack.get(ep), before, "Failed converted cast consumed staff EP");
    reset(p, 0, 10000, 10000, 10000);
    h.assertTrue(!EnergyHelper.isOutOfEnergy(p, i, 0), "Aura-funded staff cast failed");
    near(h, stack.get(ep), before, "Converted cast consumed staff EP");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void recoveryWithNativeCostModifiersUsesItsFinalBill(GameTestHelper h) {
    var p = setup(h);
    var i = learn(p, AspectualMagics.HEALING.get());
    learn(p, HeavenlyRestriction.METIS.get());
    save(h, p, i, 0, PaymentPlan.AURA);
    var cfg =
        io.github.manasmods.manascore.config.ConfigRegistry.getConfig(
                io.github.manasmods.tensura.config.ability.magic.AspectualMagicConfig.class)
            .Healing;
    double bill = Math.ceil(Math.max(cfg.minCost, cfg.magiculeCost));
    reset(p, 0, 10000, p.getMaxHealth() - 1, 10000);
    dev.heavenlyrestriction.splitmind.SplitMindCasting.autoInvoke(
        p,
        i,
        0,
        p,
        () -> {
          i.getSkill().onRelease(i, p, 1000, 0, 0);
          return null;
        });
    near(
        h,
        TensuraStorages.getExistenceFrom(p).getAura(),
        10000 - bill * .6,
        "Healing discount changed after resource conversion");
    near(h, p.getHealth(), p.getMaxHealth(), "Discounted healing failed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void requestCodecCarriesValidatedResourceShares(GameTestHelper h) {
    var p = setup(h);
    var buf =
        new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), p.registryAccess());
    try {
      var plan = new PaymentPlan(.1, .2, .3, .4);
      var r =
          new LaplaceNetwork.Request(
              UUID.randomUUID(), "apply", "test#0", true, Map.of("healing", "1"), plan);
      LaplaceNetwork.Request.CODEC.encode(buf, r);
      h.assertTrue(
          LaplaceNetwork.Request.CODEC.decode(buf).equals(r), "Request resource roundtrip");
      buf.clear();
      buf.writeUUID(UUID.randomUUID());
      buf.writeUtf("apply");
      buf.writeUtf("test#0");
      buf.writeBoolean(true);
      for (int n = 0; n < 4; n++) buf.writeDouble(.1);
      boolean rejected = false;
      try {
        LaplaceNetwork.Request.CODEC.decode(buf);
      } catch (IllegalArgumentException expected) {
        rejected = true;
      }
      h.assertTrue(rejected, "Invalid packet shares accepted");
    } finally {
      buf.release();
    }
    h.succeed();
  }
}
