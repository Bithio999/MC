package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.highrealm.*;
import dev.heavenlyrestriction.highrealm.boss.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class KaelVeyrGameTests {
  private static KaelVeyrEntity boss(GameTestHelper h) {
    var p = player(h);
    var e = WayfarerContent.KAEL.get().create(h.getLevel());
    e.setPos(p.position().add(0, 0, 6));
    e.setNoGravity(true);
    e.setNoAi(true);
    h.getLevel().addFreshEntity(e);
    return e;
  }

  private static void near(GameTestHelper h, double actual, double expected, String message) {
    h.assertTrue(Math.abs(actual - expected) < .01, message + ": " + actual + " vs " + expected);
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void kaelUsesNativeOtherworlderSaintAndFifteenMillionActualEP(GameTestHelper h) {
    var e = boss(h);
    h.assertTrue(e.initialized(), "Join did not initialize boss");
    h.assertTrue(
        RaceAPI.getRaceFrom(e).getRace().orElseThrow().getRace() == TensuraRaces.HUMAN_SAINT.get(),
        "Race is not native Human Saint");
    near(h, EnergyHelper.getMaxEP(e), 15_000_000, "Actual EP incorrect");
    System.out.println(
        "Kael stats: HP="
            + e.getMaxHealth()
            + " SHP="
            + e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)
            + " AP="
            + EnergyHelper.getMaxAura(e)
            + " MP="
            + EnergyHelper.getMaxMagicule(e));
    h.assertTrue(
        e.getMaxHealth() >= 8000
            && e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH) >= 24000,
        "Boss health package missing");
    h.assertTrue(TensuraStorages.getExistenceFrom(e).isTrueHero(), "True Hero status missing");
    h.assertTrue(
        !HighRealmElites.candidate(e) && !HighRealmElites.elite(e),
        "Boss received elite conversion/EP halving");
    h.assertTrue(!e.getMainHandItem().isEmpty(), "Native Hihiirokane weapon missing");
    h.assertTrue(
        e.getType().is(io.github.manasmods.tensura.data.TensuraEntityTags.HERO_BOSS),
        "Boss missing native hero-progression tag");
    e.discard();
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void kaelOwnsMasteredNativeSkillsAndUsesAllThreePresetSlots(GameTestHelper h) {
    var e = boss(h);
    var storage = SkillAPI.getSkillsFrom(e);
    for (var list :
        java.util.List.of(
            WayfarerLoadout.uniques(), WayfarerLoadout.support(), WayfarerLoadout.magic()))
      for (var s : list)
        h.assertTrue(
            storage.getSkill(s).map(i -> i.getMastery() >= i.getMaxMastery()).orElse(false),
            "Missing/mastery: " + s.getRegistryName());
    for (int slot = 0; slot < 3; slot++)
      h.assertTrue(
          TensuraStorages.getAbilityFrom(e).getAbilitySlot(0, slot).getSkill()
              == WayfarerLoadout.uniques().get(slot),
          "Inactive unique preset");
    e.discard();
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void kaelPersistenceDoesNotRefillPoolsOrResurrectAnInterruptedCast(
      GameTestHelper h) {
    var e = boss(h);
    var pool = TensuraStorages.getExistenceFrom(e);
    e.setHealth(1234);
    pool.setAura(456);
    pool.setMagicule(789);
    pool.setSpiritualHealth(123);
    var t = new CompoundTag();
    e.saveWithoutId(t);
    var copy = WayfarerContent.KAEL.get().create(h.getLevel());
    copy.load(t);
    copy.initialize();
    near(h, copy.getHealth(), 1234, "Reload refilled HP");
    near(h, TensuraStorages.getExistenceFrom(copy).getAura(), 456, "Reload refilled aura");
    near(h, TensuraStorages.getExistenceFrom(copy).getMagicule(), 789, "Reload refilled MP");
    near(
        h, TensuraStorages.getExistenceFrom(copy).getSpiritualHealth(), 123, "Reload refilled SHP");
    h.assertTrue(copy.pattern() == 0, "Reload resumed an unsafe cast");
    e.discard();
    copy.discard();
    h.succeed();
  }

  private static FinalizeSpawnEvent finalize(KaelVeyrEntity e, MobSpawnType reason) {
    var level = (ServerLevel) e.level();
    var event =
        new FinalizeSpawnEvent(
            e,
            level,
            e.getX(),
            e.getY(),
            e.getZ(),
            level.getCurrentDifficultyAt(e.blockPosition()),
            reason,
            null,
            null);
    event.setSpawnCancelled(false);
    NeoForge.EVENT_BUS.post(event);
    return event;
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void kaelNaturalRarityDimensionSanctumAndCooldownAreEnforced(GameTestHelper h) {
    var level = HighRealmGameTests.realm(h);
    var at = new BlockPos(3088, 250, 3088);
    level.getChunkAt(at);
    level.setBlock(at.below(), Blocks.STONE.defaultBlockState(), 2);
    level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
    level.setBlock(at.above(), Blocks.AIR.defaultBlockState(), 2);
    var e = WayfarerContent.KAEL.get().create(level);
    e.setPos(Vec3.atBottomCenterOf(at));
    e.setNoAi(true);
    e.setNoGravity(true);
    var d = RealmData.get(level.getServer());
    long oldTime = d.nextWayfarerSpawn;
    var oldSanctuary = d.sanctuary;
    double chance = HighRealmConfig.WAYFARER_CHANCE.get();
    boolean enabled = HighRealmConfig.WAYFARER_SPAWNS.get();
    try {
      d.sanctuary = null;
      d.nextWayfarerSpawn = 0;
      HighRealmConfig.WAYFARER_SPAWNS.set(true);
      HighRealmConfig.WAYFARER_CHANCE.set(0d);
      h.assertTrue(
          finalize(e, MobSpawnType.NATURAL).isSpawnCancelled(),
          "Zero rarity chance allowed a spawn");
      HighRealmConfig.WAYFARER_CHANCE.set(1d);
      h.assertTrue(
          !finalize(e, MobSpawnType.NATURAL).isSpawnCancelled(),
          "Boss blocked by elite whitelist or valid surface rejected");
      d.sanctuary = at;
      h.assertTrue(
          finalize(e, MobSpawnType.NATURAL).isSpawnCancelled(), "Boss spawned in return sanctuary");
      d.sanctuary = null;
      d.nextWayfarerSpawn = level.getServer().overworld().getGameTime() + 500;
      h.assertTrue(finalize(e, MobSpawnType.NATURAL).isSpawnCancelled(), "Cooldown ignored");
      d.nextWayfarerSpawn = 0;
      h.assertTrue(
          !finalize(e, MobSpawnType.NATURAL).isSpawnCancelled(), "Post-cooldown spawn rejected");
      h.assertTrue(level.addFreshEntity(e), "Natural boss was not added");
      h.assertTrue(
          d.nextWayfarerSpawn > level.getServer().overworld().getGameTime(),
          "Successful spawn did not persist cooldown");
      var saved =
          RealmData.load(d.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
      near(h, saved.nextWayfarerSpawn, d.nextWayfarerSpawn, "Cooldown not saved");
      var outside = WayfarerContent.KAEL.get().create(h.getLevel());
      outside.setPos(h.absoluteVec(Vec3.ZERO));
      h.assertTrue(
          finalize(outside, MobSpawnType.NATURAL).isSpawnCancelled(),
          "Natural boss spawned outside High Realm");
      h.assertTrue(
          !finalize(outside, MobSpawnType.COMMAND).isSpawnCancelled(),
          "Manual debug spawn blocked");
      outside.discard();
    } finally {
      e.discard();
      d.nextWayfarerSpawn = oldTime;
      d.sanctuary = oldSanctuary;
      HighRealmConfig.WAYFARER_CHANCE.set(chance);
      HighRealmConfig.WAYFARER_SPAWNS.set(enabled);
      d.setDirty();
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 150)
  public static void kaelWarnedConstellationHitsAndResourceSealCancelsIt(GameTestHelper h) {
    var e = boss(h);
    var target = target(h, e.position().add(0, 0, 7));
    target.setTarget(e);
    e.setTarget(target);
    e.setNoAi(false);
    double before = TensuraStorages.getExistenceFrom(e).getMagicule();
    h.assertTrue(e.beginPattern(KaelVeyrEntity.CONSTELLATION), "Cast failed");
    near(
        h,
        TensuraStorages.getExistenceFrom(e).getMagicule(),
        before - 18000,
        "Custom mana cost incorrect");
    h.runAfterDelay(
        40,
        () -> {
          h.assertTrue(
              target.getHealth() < target.getMaxHealth(),
              "Warning never became damaging native light hit");
          e.cancelPattern();
          e.addEffect(
              new net.minecraft.world.effect.MobEffectInstance(HeavenlyArsenal.SKILL_SEAL, 60));
          h.assertTrue(!e.beginPattern(KaelVeyrEntity.CONSTELLATION), "Skill seal ignored");
          e.discard();
          target.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void kaelNativeTempestCreatesActualMagicAndSpendsMagicules(GameTestHelper h) {
    var e = boss(h);
    var target = target(h, e.position().add(0, 0, 14));
    target.setTarget(e);
    e.setTarget(target);
    e.setNoAi(false);
    double before = TensuraStorages.getExistenceFrom(e).getMagicule();
    h.assertTrue(e.beginPattern(KaelVeyrEntity.TEMPEST), "Native spell could not begin");
    h.runAfterDelay(
        40,
        () -> {
          h.assertTrue(
              TensuraStorages.getExistenceFrom(e).getMagicule() < before,
              "Native magic did not spend resources");
          boolean projectile = false;
          for (var entity : h.getLevel().getAllEntities())
            if (entity
                    instanceof
                    io.github.manasmods.tensura.entity.projectile.magic.WindTornadoProjectile wind
                && wind.getOwner() == e) projectile = true;
          h.assertTrue(
              projectile, "Native Tempest did not create its owned Wind Tornado projectile");
          e.cancelPattern();
          target.setPos(e.position().add(2, 0, 0));
          e.setTarget(target);
          h.assertTrue(
              e.beginPattern(KaelVeyrEntity.TEMPEST), "Close-range wind follow-up did not begin");
          h.assertTrue(
              e.activeMagic()
                  == io.github.manasmods.tensura.registry.magic.SpiritualMagics.AERIAL_BLADE.get(),
              "Native wind spell preference did not alternate in range");
          double mana = TensuraStorages.getExistenceFrom(e).getMagicule();
          WayfarerCombat.tick(e, e.duration() - 12);
          h.assertTrue(
              TensuraStorages.getExistenceFrom(e).getMagicule() < mana,
              "Close-range native Aerial Blade did not spend mana");
          e.discard();
          target.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void kaelBlinkFindsSolidSafeLandingAndDealsNativeAuraSlash(GameTestHelper h) {
    var e = boss(h);
    var root = e.blockPosition();
    for (int x = -10; x <= 10; x++)
      for (int z = -10; z <= 14; z++)
        h.getLevel().setBlock(root.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
    var dummy = target(h, e.position().add(0, 0, 7));
    afterTracking(
        h,
        java.util.List.of(dummy),
        () -> {
          dummy.setTarget(e);
          e.setTarget(dummy);
          e.setNoAi(false);
          Vec3 before = e.position();
          h.assertTrue(e.beginPattern(KaelVeyrEntity.RIFT_DRAW), "Rift Draw did not begin");
          WayfarerCombat.tick(e, WayfarerCombat.DRAW_WINDUP);
          h.assertTrue(e.position().distanceToSqr(before) > 4, "Rift Draw did not teleport");
          h.assertTrue(
              h.getLevel().noCollision(e, e.getBoundingBox())
                  && h.getLevel().getBlockState(e.blockPosition().below()).isSolid(),
              "Blink landed in collision or above unsafe air");
          h.assertTrue(
              dummy.getHealth() < dummy.getMaxHealth(), "Blink slash did not deal actual damage");
          e.discard();
          dummy.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void kaelWaltzMovingFrontDamagesTargetsAndDoesNotTargetCreativePlayers(
      GameTestHelper h) {
    var e = boss(h);
    var dummy = target(h, e.position().add(0, 0, 7));
    afterTracking(
        h,
        java.util.List.of(dummy),
        () -> {
          dummy.setTarget(e);
          e.setTarget(dummy);
          e.setNoAi(false);
          h.assertTrue(e.beginPattern(KaelVeyrEntity.WALTZ), "Waltz did not begin");
          WayfarerCombat.tick(e, WayfarerCombat.WALTZ_WINDUP + 14 + 2);
          h.assertTrue(
              dummy.getHealth() < dummy.getMaxHealth(),
              "Waltz never damaged a target in its moving front");
          var p = player(h);
          p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
          h.assertTrue(!e.eligible(p), "Boss targets creative players");
          e.discard();
          dummy.discard();
          h.succeed();
        });
  }
}
