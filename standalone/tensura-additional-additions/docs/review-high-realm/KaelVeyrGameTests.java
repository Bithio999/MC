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
    h.assertTrue(
        !copy.casting() && copy.combat().active() == null, "Reload resumed an unsafe cast");
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

  private static net.minecraft.world.entity.monster.Husk enemy(
      GameTestHelper h, KaelVeyrEntity e, double distance) {
    var target = target(h, e.position().add(0, 0, distance));
    target.setTarget(e);
    e.setTarget(target);
    return target;
  }

  private static void release(KaelVeyrEntity e) {
    int limit = 0;
    while (e.combat().active() != null && limit++ < 400) e.combat().tick();
    if (e.combat().active() != null) throw new IllegalStateException("Native cast never completed");
  }

  private static boolean ownsProjectile(GameTestHelper h, KaelVeyrEntity e, String path) {
    for (var entity : h.getLevel().getAllEntities())
      if (entity instanceof net.minecraft.world.entity.projectile.Projectile p
          && !p.isRemoved()
          && p.getOwner() == e
          && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
              .getKey(p.getType())
              .getPath()
              .equals(path)) return true;
    return false;
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void kaelUsesNativeAuraSlashAndWindDominationSphereWithNativeCosts(
      GameTestHelper h) {
    var e = boss(h);
    var target = enemy(h, e, 12);
    afterTracking(
        h,
        java.util.List.of(target),
        () -> {
          e.setNoAi(false);
          var pool = TensuraStorages.getExistenceFrom(e);
          double aura = pool.getAura(), mana = pool.getMagicule();
          h.assertTrue(
              e.combat().begin(WayfarerCombat.Action.AURA_SLASH),
              "Native Aura Slash did not begin");
          h.assertTrue(
              pool.getAura() < aura && ownsProjectile(h, e, "aura_slash"),
              "Native slash projectile/cost missing");
          h.assertTrue(
              !e.combat().begin(WayfarerCombat.Action.AURA_SLASH), "Aura Slash cooldown ignored");
          h.assertTrue(
              e.combat().begin(WayfarerCombat.Action.WIND_SPHERE),
              "Native wind sphere did not begin");
          h.assertTrue(
              pool.getMagicule() < mana && ownsProjectile(h, e, "wind_sphere"),
              "Native wind sphere/cost missing");
          h.assertTrue(!e.isShiftKeyDown(), "AI left the alternate-mode stance enabled");
          e.addEffect(
              new net.minecraft.world.effect.MobEffectInstance(HeavenlyArsenal.SKILL_SEAL, 60));
          h.assertTrue(!e.combat().begin(WayfarerCombat.Action.WIND_CUTTER), "Skill seal ignored");
          e.discard();
          target.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void kaelReleasesOwnedNativeWindCutterTornadoAndCloseRangeAerialBlade(
      GameTestHelper h) {
    var e = boss(h);
    var target = enemy(h, e, 14);
    afterTracking(
        h,
        java.util.List.of(target),
        () -> {
          e.setNoAi(false);
          var pool = TensuraStorages.getExistenceFrom(e);
          double before = pool.getMagicule();
          h.assertTrue(
              e.combat().begin(WayfarerCombat.Action.WIND_CUTTER), "Wind Cutter did not begin");
          release(e);
          h.assertTrue(
              pool.getMagicule() < before && ownsProjectile(h, e, "wind_blade"),
              "Native Wind Cutter release missing");
          before = pool.getMagicule();
          h.assertTrue(
              e.combat().begin(WayfarerCombat.Action.TORNADO_BLADE), "Tornado Blade did not begin");
          release(e);
          h.assertTrue(
              pool.getMagicule() < before && ownsProjectile(h, e, "wind_tornado"),
              "Native Tornado Blade release missing");
          h.assertTrue(
              !e.combat().ready(WayfarerCombat.Action.AERIAL_BLADE),
              "Aerial Blade ignored native range");
          target.setPos(e.position().add(2, 0, 0));
          h.runAfterDelay(
              2,
              () -> {
                double mana = pool.getMagicule();
                h.assertTrue(
                    e.combat().begin(WayfarerCombat.Action.AERIAL_BLADE),
                    "Aerial Blade did not begin in range");
                release(e);
                h.assertTrue(
                    pool.getMagicule() < mana && target.getHealth() < target.getMaxHealth(),
                    "Native Aerial Blade did not spend mana/deal damage");
                e.discard();
                target.discard();
                h.succeed();
              });
        });
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void kaelChannelsNativeWindBreathAndSealsCancelIt(GameTestHelper h) {
    var e = boss(h);
    var target = enemy(h, e, 8);
    afterTracking(
        h,
        java.util.List.of(target),
        () -> {
          e.setNoAi(false);
          double mana = TensuraStorages.getExistenceFrom(e).getMagicule();
          h.assertTrue(
              e.combat().begin(WayfarerCombat.Action.WIND_BREATH), "Native breath did not begin");
          h.assertTrue(
              e.casting() && ownsProjectile(h, e, "wind_breath"),
              "Native held wind breath missing");
          h.assertTrue(
              TensuraStorages.getExistenceFrom(e).getMagicule() < mana,
              "Breath did not use native resource cost");
          e.addEffect(
              new net.minecraft.world.effect.MobEffectInstance(HeavenlyArsenal.SKILL_SEAL, 60));
          e.combat().tick();
          h.assertTrue(
              !e.casting() && e.combat().active() == null, "Sealed breath kept channeling");
          h.assertTrue(
              !ownsProjectile(h, e, "wind_breath"),
              "Sealed breath left an owned projectile behind");
          e.discard();
          target.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void kaelUsesActualMartialMasterDashAndAvoidsUnsafeLandings(GameTestHelper h) {
    var e = boss(h);
    var root = e.blockPosition();
    for (int x = -6; x <= 6; x++)
      for (int z = -2; z <= 35; z++)
        h.getLevel().setBlock(root.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
    var target = enemy(h, e, 9);
    afterTracking(
        h,
        java.util.List.of(target),
        () -> {
          e.setNoAi(false);
          e.setOnGround(true);
          var before = e.position();
          double aura = TensuraStorages.getExistenceFrom(e).getAura();
          h.assertTrue(
              e.combat().begin(WayfarerCombat.Action.MARTIAL_DASH),
              "Native Martial Master dash did not begin");
          h.assertTrue(
              e.position().distanceToSqr(before) > 4
                  && TensuraStorages.getExistenceFrom(e).getAura() < aura,
              "Native dash did not teleport/spend aura");
          h.assertTrue(
              h.getLevel().noCollision(e, e.getBoundingBox())
                  && h.getLevel().getBlockState(e.blockPosition().below()).isSolid(),
              "Native dash landed unsafely");
          e.discard();
          target.discard();
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void kaelUpgradeReplacesOldSkillsWithoutRefillingOrStackingModifiers(
      GameTestHelper h) {
    var e = boss(h);
    var skills = SkillAPI.getSkillsFrom(e);
    double wind = e.getAttributeValue(TensuraAttributes.WIND_BOOST);
    var extras =
        java.util.List.of(
            io.github.manasmods.tensura.registry.skill.ExtraSkills.INFINITE_REGENERATION.get(),
            io.github.manasmods.tensura.registry.skill.ExtraSkills.WIND_DOMINATION.get());
    for (var skill : extras)
      skills
          .getSkill(skill)
          .ifPresent(
              i -> {
                i.setToggled(false);
                i.onToggleOff(e);
                skills.forgetSkill(i);
              });
    for (var skill :
        java.util.List.of(
            io.github.manasmods.tensura.registry.skill.ExtraSkills.ULTRASPEED_REGENERATION.get(),
            io.github.manasmods.tensura.registry.skill.ExtraSkills.WIND_MANIPULATION.get())) {
      RestrictionData.learnReward(e, skill);
      var i = skills.getSkill(skill).orElseThrow();
      i.setMastery(i.getMaxMastery());
      i.setToggled(true);
      i.onToggleOn(e);
    }
    e.getPersistentData().putInt("heavenlyrestriction:wayfarer_loadout", 1);
    TensuraStorages.getAbilityFrom(e)
        .setAbilitySlot(
            8,
            2,
            io.github.manasmods.tensura.registry.skill.ExtraSkills.WIND_MANIPULATION.get(),
            0);
    e.setHealth(1234);
    var pool = TensuraStorages.getExistenceFrom(e);
    pool.setAura(456);
    pool.setMagicule(789);
    pool.setSpiritualHealth(123);
    e.initialize();
    h.assertTrue(
        TensuraStorages.getAbilityFrom(e).getAbilitySlot(8, 2).getSkill() == null,
        "Upgrade left a superseded skill in a saved preset");
    for (var skill : extras)
      h.assertTrue(
          skills.getSkill(skill).map(i -> i.isToggled() && i.isMastered(e)).orElse(false),
          "Upgraded passive missing");
    h.assertTrue(
        skills
                .getSkill(
                    io.github.manasmods.tensura.registry.skill.ExtraSkills.ULTRASPEED_REGENERATION
                        .get())
                .isEmpty()
            && skills
                .getSkill(
                    io.github.manasmods.tensura.registry.skill.ExtraSkills.WIND_MANIPULATION.get())
                .isEmpty(),
        "Old passives retained");
    near(h, e.getHealth(), 1234, "Upgrade refilled HP");
    near(h, pool.getAura(), 456, "Upgrade refilled aura");
    near(h, pool.getMagicule(), 789, "Upgrade refilled mana");
    near(h, pool.getSpiritualHealth(), 123, "Upgrade refilled SHP");
    near(h, EnergyHelper.getMaxEP(e), 15_000_000, "Upgrade changed EP");
    near(h, e.getAttributeValue(TensuraAttributes.WIND_BOOST), wind, "Wind bonus stacked");
    e.initialize();
    near(
        h,
        e.getAttributeValue(TensuraAttributes.WIND_BOOST),
        wind,
        "Repeated upgrade stacked wind bonus");
    e.discard();
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void kaelInfiniteRegenerationHealsHPAndSHPUsingNativeMagiculesAndHasOneBossBar(
      GameTestHelper h) {
    var e = boss(h);
    var pool = TensuraStorages.getExistenceFrom(e);
    e.setHealth(e.getMaxHealth() - 100);
    double max = e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);
    pool.setSpiritualHealth(max - 100);
    var effect =
        io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(
            io.github.manasmods.tensura.registry.effect.TensuraMobEffects.INSTANT_REGENERATION);
    var regeneration = e.getEffect(effect);
    h.assertTrue(
        regeneration != null && regeneration.getAmplifier() == 1,
        "Infinite Regeneration native effect missing");
    double mana = pool.getMagicule();
    effect.value().applyEffectTick(e, 1);
    near(h, e.getHealth(), e.getMaxHealth(), "Infinite Regeneration did not restore HP");
    near(h, pool.getSpiritualHealth(), max, "Infinite Regeneration did not restore SHP");
    h.assertTrue(pool.getMagicule() < mana, "Native regeneration did not spend mana");
    long bars =
        java.util.Arrays.stream(KaelVeyrEntity.class.getDeclaredFields())
            .filter(f -> f.getType() == net.minecraft.server.level.ServerBossEvent.class)
            .count();
    h.assertTrue(bars == 1, "Boss has an extra SHP bar");
    e.discard();
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 150)
  public static void kaelAutonomousAIActuallyUsesOwnedNativeAbilities(GameTestHelper h) {
    var e = boss(h);
    var target = enemy(h, e, 14);
    java.util.concurrent.atomic.AtomicBoolean cast =
        new java.util.concurrent.atomic.AtomicBoolean();
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> listener =
        event -> {
          if (event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile p
              && p.getOwner() == e) cast.set(true);
        };
    NeoForge.EVENT_BUS.addListener(listener);
    afterTracking(
        h,
        java.util.List.of(target),
        () -> {
          e.setNoAi(false);
          e.combat().delay(0);
          h.runAfterDelay(
              45,
              () -> {
                NeoForge.EVENT_BUS.unregister(listener);
                h.assertTrue(cast.get(), "Autonomous AI never released a native projectile");
                e.discard();
                target.discard();
                h.succeed();
              });
        });
  }
}
