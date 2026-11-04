package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.ChainedGameTests.near;
import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.fusion.Fusion;
import dev.heavenlyrestriction.loans.*;
import dev.heavenlyrestriction.splitmind.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.magic.AspectualMagics;
import io.github.manasmods.tensura.registry.skill.CommonSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class UniqueMasteryGameTests {
  @GameTest(template = "empty", timeoutTicks = 120)
  public static void splitMindMasteryDiscountsActualCastsAndAffordabilityButNotManualMagic(
      GameTestHelper h) {
    var p = player(h);
    var mind = SplitMindGameTests.mind(p, true);
    var fire = SplitMindGameTests.learn(p, AspectualMagics.FIRE.get(), true);
    SplitMindGameTests.enable(p, mind, fire);
    var target = target(h, p.position().add(10, 0, 0));
    var shots = new java.util.concurrent.atomic.AtomicInteger();
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> watch =
        e -> {
          if (e.getEntity()
                  instanceof
                  io.github.manasmods.tensura.entity.projectile.magic.FireBoltProjectile shot
              && shot.getOwner() == p) shots.incrementAndGet();
        };
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(watch);
    afterTracking(
        h,
        List.of(target),
        () -> {
          try {
            var pool = TensuraStorages.getExistenceFrom(p);
            double base = ((Magic) fire.getSkill()).getMagiculeCost(p, fire, 0);
            double full = base * p.getAttributeValue(TensuraAttributes.MAGIC_COST_MULTIPLIER);
            mind.setMastery(0);
            pool.setMagicule(full * .75);
            SplitMindGameTests.step(p, 5);
            h.assertTrue(
                SkillAPI.getSkillsFrom(p).getHeldSkills().isEmpty(),
                "Unmastered auto cast received discount");
            pool.setMagicule(full);
            SplitMindGameTests.step(p, 10);
            SplitMindGameTests.step(p, 11);
            near(h, pool.getMagicule(), 0, "Unmastered auto bill changed");
            fire.setCoolDowns(0);
            mind.setMastery(mind.getMaxMastery());
            pool.setMagicule(full * .75);
            SplitMindGameTests.step(p, 15);
            SplitMindGameTests.step(p, 16);
            near(h, pool.getMagicule(), 0, "Mastered affordable cast did not pay exactly 75%");
            h.assertTrue(
                shots.get() == 6, "Both native auto casts did not release their three bolts");
            var rows = SplitMindNetwork.snapshot(p, "", 0, 0).getList("rows", Tag.TAG_COMPOUND);
            near(
                h,
                rows.getCompound(0).getDouble("mp"),
                base * .75,
                "Assignment cost does not match mastery");
            mind.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, false);
            SplitMindCasting.cancel(p);
            fire.setCoolDowns(0);
            TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, fire.getSkill(), 0);
            pool.setMagicule(full);
            var skills = SkillAPI.getSkillsFrom(p);
            h.assertTrue(skills.startHoldSkill(fire, 0, 0), "Manual cast rejected");
            for (var held : List.copyOf(skills.getHeldSkills())) held.tick(skills, p);
            skills.handleSkillRelease(fire, 0, 0, false);
            near(h, pool.getMagicule(), 0, "Mastery discounted manual casting");
            h.succeed();
          } finally {
            SplitMindCasting.cancel(p);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(watch);
          }
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void loanSharkMasteryClaimsAtEightMinutesButNeverEarlyOrWhileActive(
      GameTestHelper h) {
    for (int mode = 0; mode < 3; mode++) {
      var p = LoanSharkGameTests.lender(h);
      var skill =
          SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.LOAN_SHARK.get()).orElseThrow();
      near(h, LoanShark.claimTicks(p), 12000, "Unmastered claim threshold changed");
      skill.setMastery(skill.getMaxMastery());
      near(h, LoanShark.claimTicks(p), 9600, "Mastered threshold is not eight minutes");
      var e = LoanSharkGameTests.subordinate(h, p);
      var loan = LoanShark.lend(p, e, HeavenlyRestriction.SCHEMA.get().getRegistryName());
      h.assertTrue(loan != null, "Mastery test loan failed");
      RestrictionData.learnReward(e, CommonSkills.SELF_REGENERATION.get());
      var reward =
          SkillAPI.getSkillsFrom(e).getSkill(CommonSkills.SELF_REGENERATION.get()).orElseThrow();
      reward.setMastery(333);
      if (mode == 2) LoanSharkGameTests.gift(e).setToggled(true);
      LoanLedger.get(p.server).clock = loan.lastUsed + (mode == 0 ? 9599 : 9600);
      LoanShark.claim(p, loan.id);
      var gained = SkillAPI.getSkillsFrom(p).getSkill(CommonSkills.SELF_REGENERATION.get());
      h.assertTrue(
          gained.isPresent() == (mode == 1),
          "Eight-minute boundary or active-use check failed: " + mode);
      if (gained.isPresent())
        near(h, gained.get().getMastery(), 333, "Mastered Claim lost skill mastery");
      h.assertTrue(
          SkillAPI.getSkillsFrom(e).getSkill(HeavenlyRestriction.SCHEMA.get()).isEmpty(),
          "Claim did not recall gift");
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void masteredFusionUsesFourAndHalfMinutesAndOnePointSevenFiveEp(GameTestHelper h) {
    var p = player(h);
    RestrictionData.learnReward(p, HeavenlyRestriction.FUSION.get());
    var skill = SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.FUSION.get()).orElseThrow();
    skill.setMastery(skill.getMaxMastery());
    var host = target(h, p.position().add(3, 0, 0));
    TensuraStorages.getExistenceFrom(host).setPermanentOwner(p.getUUID());
    EnergyHelper.setBaseMaxEP(host, 1000000);
    afterTracking(
        h,
        List.of(host),
        () -> {
          double own = EnergyHelper.getMaxEP(p), other = EnergyHelper.getMaxEP(host);
          float hp = p.getMaxHealth() + host.getMaxHealth();
          double shp =
              p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)
                  + host.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);
          var a = TensuraStorages.getExistenceFrom(p);
          var b = TensuraStorages.getExistenceFrom(host);
          double ap = a.getAura() + b.getAura(), mp = a.getMagicule() + b.getMagicule();
          try {
            h.assertTrue(Fusion.fuseSubordinate(p, host), "Mastered fusion failed");
            var session = Fusion.session(p);
            near(h, session.until - p.level().getGameTime(), 5400, "Mastered duration is not 4:30");
            near(h, EnergyHelper.getMaxEP(p), (own + other) * 1.75, "Mastered fused EP");
            near(h, EnergyHelper.getMaxEP(host), (own + other) * 1.75, "Host mastered EP differs");
            near(
                h,
                TensuraStorages.getExistenceFrom(p).getAura(),
                ap * 1.75,
                "Current aura uses old multiplier");
            near(
                h,
                TensuraStorages.getExistenceFrom(p).getMagicule(),
                mp * 1.75,
                "Current magicules use old multiplier");
            near(h, p.getMaxHealth(), hp, "Mastery incorrectly multiplied HP");
            near(
                h,
                p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),
                shp,
                "Mastery incorrectly multiplied SHP");
            Fusion.expire(p, session.until - 1800);
            h.assertTrue(
                Fusion.session(p) != null, "Mastered fusion still expired at three minutes");
            Fusion.expire(p, session.until - 1);
            h.assertTrue(Fusion.session(p) != null, "Mastered fusion expired early");
            Fusion.expire(p, session.until);
            h.assertTrue(Fusion.session(p) == null, "Mastered fusion exceeded duration");
            near(h, EnergyHelper.getMaxEP(p), own, "Mastery leaked permanent caster EP");
            near(h, EnergyHelper.getMaxEP(host), other, "Mastery leaked permanent host EP");
            h.succeed();
          } finally {
            Fusion.end(p, "Mastery test cleanup");
          }
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void prospectorMasteryRaisesNativeFindRateAndRareMaterialWeights(GameTestHelper h) {
    var p = player(h);
    RestrictionData.learnReward(p, HeavenlyRestriction.PROSPECTOR.get());
    var skill =
        SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PROSPECTOR.get()).orElseThrow();
    skill.setMastery(0);
    skill.onToggleOn(p);
    var params =
        new LootParams.Builder(h.getLevel())
            .withParameter(LootContextParams.ORIGIN, p.position())
            .withParameter(LootContextParams.BLOCK_STATE, Blocks.STONE.defaultBlockState())
            .withParameter(LootContextParams.TOOL, new ItemStack(Items.DIAMOND_PICKAXE))
            .withParameter(LootContextParams.THIS_ENTITY, p)
            .create(LootContextParamSets.BLOCK);
    int[] counts = new int[2], rares = new int[2];
    var shard = BuiltInRegistries.ITEM.get(ResourceLocation.parse("tensura:magic_ore_shard"));
    for (int mode = 0; mode < 2; mode++) {
      if (mode == 1) skill.setMastery(skill.getMaxMastery());
      var key = ProspectorMining.findsTable(p);
      h.assertTrue(
          key.equals(mode == 0 ? ProspectorMining.FINDS : ProspectorMining.MASTERED_FINDS),
          "Wrong native finds table selected");
      var table = p.server.reloadableRegistries().getLootTable(key);
      var random = RandomSource.create(84021);
      for (int n = 0; n < 50000; n++)
        for (var stack : table.getRandomItems(params, random)) {
          counts[mode]++;
          if (stack.is(Items.DIAMOND) || stack.is(Items.EMERALD) || stack.is(shard)) rares[mode]++;
          h.assertTrue(stack.getCount() == 1, "Mastery duplicated a find");
        }
    }
    h.assertTrue(counts[0] >= 375 && counts[0] <= 625, "Normal rate not 1%: " + counts[0]);
    h.assertTrue(counts[1] >= 600 && counts[1] <= 900, "Mastered rate not 1.5%: " + counts[1]);
    h.assertTrue(
        (double) rares[0] / counts[0] < .10 && (double) rares[1] / counts[1] > .13,
        "Mastery did not shift rare weights: "
            + rares[0]
            + "/"
            + counts[0]
            + " to "
            + rares[1]
            + "/"
            + counts[1]);
    skill.onToggleOff(p);
    h.assertTrue(ProspectorMining.active(p).isEmpty(), "Mastery bypassed passive toggle");
    h.succeed();
  }
}
