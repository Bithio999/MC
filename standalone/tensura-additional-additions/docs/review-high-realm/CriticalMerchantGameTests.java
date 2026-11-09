package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.critical.*;
import io.github.manasmods.manascore.attribute.api.ManasCoreAttributes;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.entity.human.golem.TrainingDummyEntity;
import io.github.manasmods.tensura.registry.block.TensuraBlocks;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("merchantreview")
@PrefixGameTestTemplate(false)
public final class CriticalMerchantGameTests {
  private static ManasSkillInstance grant(LivingEntity e, boolean toggle) {
    RestrictionData.learnReward(e, HeavenlyRestriction.CRITICAL_MERCHANT.get());
    var i =
        SkillAPI.getSkillsFrom(e)
            .getSkill(HeavenlyRestriction.CRITICAL_MERCHANT.get())
            .orElseThrow();
    i.setMastery(0);
    i.setToggled(toggle);
    return i;
  }

  private static ServerPlayer merchant(GameTestHelper h) {
    var p = player(h);
    p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
    p.getAttribute(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER).setBaseValue(2.5);
    p.getAttribute(ManasCoreAttributes.CRITICAL_ATTACK_CHANCE).setBaseValue(0);
    grant(p, false);
    return p;
  }

  private static TrainingDummyEntity dummy(GameTestHelper h, LivingEntity p) {
    var t =
        (TrainingDummyEntity)
            BuiltInRegistries.ENTITY_TYPE
                .get(ResourceLocation.parse("tensura:training_dummy"))
                .create(h.getLevel());
    t.setPos(p.position().add(0, 0, 3));
    t.setNoAi(true);
    h.getLevel().setBlock(t.blockPosition().below(), Blocks.STONE.defaultBlockState(), 3);
    h.getLevel()
        .setBlock(t.blockPosition(), TensuraBlocks.TRAINING_DUMMY.get().defaultBlockState(), 3);
    h.getLevel().addFreshEntity(t);
    return t;
  }

  private static ManasSkillInstance skill(LivingEntity e) {
    return CriticalMerchant.instance(e).orElseThrow();
  }

  private static void press(LivingEntity e) {
    HeavenlyRestriction.CRITICAL_MERCHANT.get().onPressed(skill(e), e, 0, 0);
  }

  private static void near(GameTestHelper h, double a, double b, String label) {
    h.assertTrue(Math.abs(a - b) < .001, label + ": " + a + " / " + b);
  }

  private static final class Capture implements AutoCloseable {
    final List<Float> values = new ArrayList<>();
    final java.util.function.Consumer<LivingIncomingDamageEvent> listener;

    Capture(LivingEntity target) {
      listener =
          e -> {
            if (e.getEntity() == target) values.add(e.getAmount());
          };
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, listener);
    }

    public void close() {
      NeoForge.EVENT_BUS.unregister(listener);
    }
  }

  private static void payoutRatio(GameTestHelper h, float base, float boosted) {
    double exponent = Math.log(boosted / base) / Math.log(2);
    h.assertTrue(
        base > 0
            && exponent >= .9999
            && exponent <= 101.0001
            && Math.abs(exponent - Math.rint(exponent)) < .001,
        "Native crit must receive one power-of-two payout, without a second base critical: "
            + base
            + " -> "
            + boosted);
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantChargesFlatMagiculesAndRefreshesWithTwentySecondCooldown(
      GameTestHelper h) {
    var p = merchant(h);
    var i = skill(p);
    var energy = TensuraStorages.getExistenceFrom(p);
    energy.setMagicule(2500);
    press(p);
    near(h, energy.getMagicule(), 1500, "Cast bill");
    h.assertTrue(
        CriticalMerchant.remaining(i) == 10 && i.onCoolDown(0), "Ten hits or cooldown missing");
    press(p);
    near(h, energy.getMagicule(), 1500, "Cooldown allowed another bill");
    i.setCoolDown(0, 0);
    press(p);
    near(h, energy.getMagicule(), 500, "Second cast bill");
    h.assertTrue(CriticalMerchant.remaining(i) == 10, "Recast stacked guarantees");
    i.setCoolDown(0, 0);
    press(p);
    near(h, energy.getMagicule(), 500, "Unaffordable cast spent magicules");
    h.assertTrue(
        CriticalMerchant.remaining(i) == 10 && !i.onCoolDown(0),
        "Failed cast changed stock/cooldown");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantGuaranteesExactlyTenLandedHitsWithPassiveOff(GameTestHelper h) {
    var p = merchant(h);
    var t = dummy(h, p);
    h.runAfterDelay(
        5,
        () -> {
          try (var c = new Capture(t)) {
            t.hurt(p.damageSources().playerAttack(p), 8);
            float baseline = c.values.getLast();
            press(p);
            for (int n = 0; n < 10; n++) {
              t.invulnerableTime = 0;
              t.hurt(p.damageSources().playerAttack(p), 8);
              near(h, c.values.getLast(), baseline * 2.5, "Guaranteed crit " + n);
            }
            h.assertTrue(CriticalMerchant.remaining(skill(p)) == 0, "Guarantees did not exhaust");
            t.invulnerableTime = 0;
            t.hurt(p.damageSources().playerAttack(p), 8);
            near(h, c.values.getLast(), baseline, "Eleventh hit was still guaranteed");
          }
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantBlockedHitsAndExceptionsRefundTheirGuarantee(GameTestHelper h) {
    var p = merchant(h);
    var t = dummy(h, p);
    press(p);
    h.runAfterDelay(
        5,
        () -> {
          t.setInvulnerable(true);
          t.hurt(p.damageSources().playerAttack(p), 8);
          h.assertTrue(CriticalMerchant.remaining(skill(p)) == 10, "Immune hit spent guarantee");
          t.setInvulnerable(false);
          try {
            CriticalMerchant.observe(
                t,
                p.damageSources().playerAttack(p),
                () -> {
                  throw new IllegalStateException("expected");
                });
            throw new AssertionError("Missing exception");
          } catch (IllegalStateException expected) {
          }
          h.assertTrue(CriticalMerchant.remaining(skill(p)) == 10, "Exception spent guarantee");
          t.invulnerableTime = 0;
          t.hurt(p.damageSources().playerAttack(p), 8);
          h.assertTrue(
              CriticalMerchant.remaining(skill(p)) == 9,
              "Context leaked or valid hit did not spend guarantee");
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantAmplifiesNativePlayerCriticalsOnce(GameTestHelper h) {
    var p = merchant(h);
    var t = dummy(h, p);
    p.getAttribute(ManasCoreAttributes.CRITICAL_ATTACK_CHANCE).setBaseValue(100);
    h.runAfterDelay(
        5,
        () -> {
          try (var c = new Capture(t)) {
            p.resetAttackStrengthTicker();
            p.attack(t);
            float baseline = c.values.getLast();
            skill(p).setToggled(true);
            t.invulnerableTime = 0;
            p.resetAttackStrengthTicker();
            p.attack(t);
            payoutRatio(h, baseline, c.values.getLast());
            h.assertTrue(CriticalMerchant.remaining(skill(p)) == 0, "Passive created guarantees");
          }
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantDoesNotDoubleUnboundsBaseCritical(GameTestHelper h) {
    var p = merchant(h);
    var t = dummy(h, p);
    RestrictionData.learnReward(p, HeavenlyRestriction.UNBOUND.get());
    var u = SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();
    u.setMastery(u.getMaxMastery());
    u.setToggled(false);
    h.runAfterDelay(
        5,
        () -> {
          try (var c = new Capture(t)) {
            p.resetAttackStrengthTicker();
            p.attack(t);
            float baseline = c.values.getLast();
            skill(p).setToggled(true);
            t.invulnerableTime = 0;
            p.resetAttackStrengthTicker();
            p.attack(t);
            payoutRatio(h, baseline, c.values.getLast());
          }
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantGuaranteesOwnedProjectileCriticals(GameTestHelper h) {
    var p = merchant(h);
    var t = dummy(h, p);
    var arrow = new Arrow(EntityType.ARROW, h.getLevel());
    arrow.setOwner(p);
    var type =
        h.getLevel()
            .registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(DamageTypes.ARROW);
    h.runAfterDelay(
        5,
        () -> {
          try (var c = new Capture(t)) {
            t.hurt(new DamageSource(type, arrow, p), 8);
            float baseline = c.values.getLast();
            press(p);
            t.invulnerableTime = 0;
            t.hurt(new DamageSource(type, arrow, p), 8);
            near(h, c.values.getLast(), baseline * 2.5, "Owned projectile critical");
            h.assertTrue(
                CriticalMerchant.remaining(skill(p)) == 9, "Projectile did not consume one hit");
          }
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantHandlesDirectSpiritualDamageOnce(GameTestHelper h) {
    var p = merchant(h);
    var t = target(h, p.position().add(0, 0, 3));
    RaceAPI.getRaceFrom(t).setRace(TensuraRaces.HUMAN_SAINT.get(), false);
    var energy = TensuraStorages.getExistenceFrom(t);
    h.runAfterDelay(
        5,
        () -> {
          energy.setSpiritualHealth(1000);
          io.github.manasmods.tensura.damage.TensuraDamageHelper.directSpiritualHurt(
              t, p, p.damageSources().playerAttack(p), 10, 0);
          double baseline = 1000 - energy.getSpiritualHealth();
          h.assertTrue(baseline > 0, "SHP fixture accepted no damage");
          press(p);
          energy.setSpiritualHealth(1000);
          io.github.manasmods.tensura.damage.TensuraDamageHelper.directSpiritualHurt(
              t, p, p.damageSources().playerAttack(p), 10, 0);
          near(h, 1000 - energy.getSpiritualHealth(), baseline * 2.5, "Direct SHP critical");
          h.assertTrue(CriticalMerchant.remaining(skill(p)) == 9, "Direct SHP guarantee count");
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantAmplifiesNativeMobCriticalsOnce(GameTestHelper h) {
    var p = player(h);
    var mob = target(h, p.position().add(0, 0, 1));
    var i = grant(mob, false);
    var t = dummy(h, mob);
    mob.getAttribute(ManasCoreAttributes.CRITICAL_ATTACK_CHANCE).setBaseValue(100);
    mob.getAttribute(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER).setBaseValue(2.5);
    h.runAfterDelay(
        5,
        () -> {
          try (var c = new Capture(t)) {
            t.hurt(mob.damageSources().mobAttack(mob), 8);
            float baseline = c.values.getLast();
            i.setToggled(true);
            t.invulnerableTime = 0;
            t.hurt(mob.damageSources().mobAttack(mob), 8);
            payoutRatio(h, baseline, c.values.getLast());
          }
          t.discard();
          mob.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantIsExcludedFromFetteredReincarnationBecauseOfMagiculeCost(
      GameTestHelper h) {
    var p = merchant(h);
    near(
        h,
        HeavenlyRestriction.CRITICAL_MERCHANT.get().getMagiculeCost(p, skill(p), 0),
        1000,
        "Flat native cost getter");
    h.assertTrue(
        ChainedReincarnation.hasMagiculeCost(HeavenlyRestriction.CRITICAL_MERCHANT.get(), p),
        "Automatic Fettered blacklist missed paid active");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantPassiveDoesNotAmplifyOrdinaryHits(GameTestHelper h) {
    var p = merchant(h);
    var t = dummy(h, p);
    h.runAfterDelay(
        5,
        () -> {
          try (var c = new Capture(t)) {
            t.hurt(p.damageSources().playerAttack(p), 8);
            float baseline = c.values.getLast();
            skill(p).setToggled(true);
            t.invulnerableTime = 0;
            t.hurt(p.damageSources().playerAttack(p), 8);
            near(h, c.values.getLast(), baseline, "Passive amplified a noncritical hit");
            near(h, skill(p).getMastery(), 0, "Noncritical hit awarded mastery");
          }
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantForcesNativeMeleeCriticalWithoutApplyingItsBaseTwice(
      GameTestHelper h) {
    var p = merchant(h);
    var t = dummy(h, p);
    h.runAfterDelay(
        5,
        () -> {
          try (var c = new Capture(t)) {
            p.resetAttackStrengthTicker();
            p.attack(t);
            float baseline = c.values.getLast();
            press(p);
            t.invulnerableTime = 0;
            p.resetAttackStrengthTicker();
            p.attack(t);
            near(h, c.values.getLast(), baseline * 2.5, "Native forced melee base critical");
            h.assertTrue(
                CriticalMerchant.remaining(skill(p)) == 9,
                "Native melee spent the wrong number of hits");
          }
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantGuaranteeDoesNotDuplicateUnboundProjectileCriticals(GameTestHelper h) {
    var p = merchant(h);
    var t = dummy(h, p);
    RestrictionData.learnReward(p, HeavenlyRestriction.UNBOUND.get());
    var u = SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();
    u.setMastery(u.getMaxMastery());
    u.setToggled(false);
    var arrow = new Arrow(EntityType.ARROW, h.getLevel());
    arrow.setOwner(p);
    var type =
        h.getLevel()
            .registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(DamageTypes.ARROW);
    h.runAfterDelay(
        5,
        () -> {
          t.hurt(new DamageSource(type, arrow, p), 8);
          float baseline = t.lastDamageTaken;
          h.assertTrue(baseline > 0, "Unbound projectile fixture dealt no damage");
          press(p);
          t.invulnerableTime = 0;
          t.hurt(new DamageSource(type, arrow, p), 8);
          near(
              h,
              t.lastDamageTaken,
              baseline,
              "Guarantee duplicated Unbound projectile critical base");
          h.assertTrue(
              CriticalMerchant.remaining(skill(p)) == 9,
              "Unbound projectile spent wrong charge count");
          skill(p).setToggled(true);
          t.invulnerableTime = 0;
          t.hurt(new DamageSource(type, arrow, p), 8);
          payoutRatio(h, baseline, t.lastDamageTaken);
          t.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void merchantCountsLandedDamageEvenWhenHealedDuringTheHit(GameTestHelper h) {
    var p = merchant(h);
    var t = target(h, p.position().add(0, 0, 3));
    press(p);
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post>
        heal =
            e -> {
              if (e.getEntity() == t) t.setHealth(t.getMaxHealth());
            };
    NeoForge.EVENT_BUS.addListener(heal);
    h.runAfterDelay(
        5,
        () -> {
          t.invulnerableTime = 0;
          t.hurt(p.damageSources().playerAttack(p), 8);
          near(
              h,
              t.getHealth(),
              t.getMaxHealth(),
              "Fixture must restore HP during the damage transaction");
          h.assertTrue(
              CriticalMerchant.remaining(skill(p)) == 9, "Healing refunded a landed critical");
          NeoForge.EVENT_BUS.unregister(heal);
          t.discard();
          h.succeed();
        });
  }
}
