package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.ChainedGameTests.near;

import dev.heavenlyrestriction.highrealm.*;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.ResistanceSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class HighRealmGameTests {
  static void frame(ServerLevel level, BlockPos core, Direction.Axis axis) {
    for (int x = -2; x <= 2; x++)
      for (int y = 0; y <= 5; y++)
        level.setBlock(
            GatePattern.at(core, axis, x, y),
            (Math.abs(x) == 2 || y == 0 || y == 5 ? GatePattern.expected(x, y) : Blocks.AIR)
                .defaultBlockState(),
            2);
  }

  static ServerLevel realm(GameTestHelper h) {
    var l = h.getLevel().getServer().getLevel(HighRealm.LEVEL);
    h.assertTrue(l != null, "High Realm did not load");
    return l;
  }

  static io.github.manasmods.tensura.entity.monster.OrcEntity orc(ServerLevel l) {
    var m =
        new io.github.manasmods.tensura.entity.monster.OrcEntity(
            io.github.manasmods.tensura.registry.entity.MonsterEntityTypes.ORC.get(), l);
    m.setPos(96, 160, 96);
    return m;
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void dimensionAndThreeBiomesLoadWithIndependentTerrain(GameTestHelper h) {
    var l = realm(h);
    h.assertTrue(
        l.dimensionType().hasFixedTime() && !l.dimensionType().bedWorks(),
        "Realm time/bed rules missing");
    var registry = l.registryAccess().registryOrThrow(Registries.BIOME);
    for (String name : new String[] {"ivory_crags", "starwood_grove", "mistglass_basin"}) {
      var biome = registry.get(HeavenlyRestriction.id(name));
      h.assertTrue(biome != null, "Missing biome " + name);
      h.assertTrue(
          biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
              .allMatch(
                  s -> {
                    var m = s.type.create(l);
                    return m instanceof Mob mob && HighRealmElites.candidate(mob);
                  }),
          "Biome can spawn non-whitelisted mobs");
    }
    h.assertTrue(
        l.getChunkSource().getGenerator() != h.getLevel().getChunkSource().getGenerator(),
        "Overworld generator reused");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void gateRequiresExactFrameBothOrientationsAndClearInterior(GameTestHelper h) {
    var level = h.getLevel();
    var core = h.absolutePos(BlockPos.ZERO).offset(3, 2, 3);
    for (var axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
      frame(level, core, axis);
      h.assertTrue(GatePattern.valid(level, core, axis), "Complete frame rejected " + axis);
      level.setBlock(GatePattern.at(core, axis, 0, 3), Blocks.STONE.defaultBlockState(), 2);
      h.assertTrue(!GatePattern.valid(level, core, axis), "Blocked opening accepted");
      level.setBlock(GatePattern.at(core, axis, 0, 3), Blocks.WATER.defaultBlockState(), 2);
      h.assertTrue(!GatePattern.valid(level, core, axis), "Flooded opening accepted");
      frame(level, core, axis);
      level.setBlock(GatePattern.at(core, axis, 2, 5), Blocks.IRON_BLOCK.defaultBlockState(), 2);
      h.assertTrue(!GatePattern.valid(level, core, axis), "Cheap corner accepted");
      frame(level, core, axis);
      for (int x = -2; x <= 2; x++)
        for (int y = 0; y <= 5; y++)
          level.setBlock(GatePattern.at(core, axis, x, y), Blocks.AIR.defaultBlockState(), 2);
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void gateUsesActualEpAndGuardsForeignDimensionTravel(GameTestHelper h) {
    var p = LoanSharkGameTests.actor(h);
    double threshold = HighRealmConfig.ENTRY_EP.get();
    EnergyHelper.setBaseMaxEP(p, threshold - 1);
    h.assertTrue(!HighRealm.enoughEP(p), "EP below threshold accepted");
    var denied =
        new net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent(p, HighRealm.LEVEL);
    NeoForge.EVENT_BUS.post(denied);
    h.assertTrue(denied.isCanceled(), "External teleport bypassed EP");
    var aura = p.getAttribute(TensuraAttributes.MAX_AURA);
    var key = HeavenlyRestriction.id("high_realm_test");
    aura.addTransientModifier(new AttributeModifier(key, 2, AttributeModifier.Operation.ADD_VALUE));
    try {
      h.assertTrue(HighRealm.enoughEP(p), "Actual attribute EP ignored");
      var allowed =
          new net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent(p, HighRealm.LEVEL);
      NeoForge.EVENT_BUS.post(allowed);
      h.assertTrue(!allowed.isCanceled(), "Actual EP rejected");
    } finally {
      aura.removeModifier(key);
    }
    EnergyHelper.setBaseMaxEP(p, threshold);
    h.assertTrue(HighRealm.enoughEP(p), "Exact threshold rejected");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void activationPersistsAndReturnsRemainPerPlayer(GameTestHelper h) {
    var p = LoanSharkGameTests.actor(h);
    var core = h.absolutePos(BlockPos.ZERO).offset(3, 2, 3);
    frame(h.getLevel(), core, Direction.Axis.X);
    h.assertTrue(HighRealm.activate(p, core, Direction.Axis.X), "Gate activation failed");
    var data = RealmData.get(p.server);
    int count = data.gates.size();
    h.assertTrue(
        HighRealm.activate(p, core, Direction.Axis.X) && data.gates.size() == count,
        "Same gate duplicated");
    var second = UUID.randomUUID();
    data.homes.put(p.getUUID(), new RealmData.Home(Level.OVERWORLD, new Vec3(20, 100, 20), 45, 15));
    data.homes.put(second, new RealmData.Home(Level.NETHER, new Vec3(30, 100, 30), 90, 0));
    var restored =
        RealmData.load(data.save(new CompoundTag(), p.registryAccess()), p.registryAccess());
    h.assertTrue(
        restored.gates.size() == count
            && restored.homes.get(second).dimension().equals(Level.NETHER)
            && restored.homes.get(p.getUUID()).pos().x == 20,
        "Gate/individual returns lost after restart");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void returnSearchAvoidsObstructionsWaterAndMagma(GameTestHelper h) {
    var p = LoanSharkGameTests.actor(h);
    var core = h.absolutePos(BlockPos.ZERO).offset(3, 3, 3);
    for (int x = -5; x <= 5; x++)
      for (int z = -5; z <= 5; z++) {
        h.getLevel().setBlock(core.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
        for (int y = 0; y <= 4; y++)
          h.getLevel().setBlock(core.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
    h.getLevel().setBlock(core, Blocks.STONE.defaultBlockState(), 2);
    var v = HighRealm.safeLanding(h.getLevel(), Vec3.atBottomCenterOf(core), p);
    h.assertTrue(v != null && !BlockPos.containing(v).equals(core), "Return landed inside a block");
    h.getLevel().setBlock(core, Blocks.AIR.defaultBlockState(), 2);
    h.getLevel().setBlock(core.above(), Blocks.WATER.defaultBlockState(), 2);
    v = HighRealm.safeLanding(h.getLevel(), Vec3.atBottomCenterOf(core), p);
    h.assertTrue(
        v != null && !BlockPos.containing(v).equals(core), "Return submerged player's head");
    h.getLevel().setBlock(core.above(), Blocks.AIR.defaultBlockState(), 2);
    h.getLevel().setBlock(core.below(), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
    v = HighRealm.safeLanding(h.getLevel(), Vec3.atBottomCenterOf(core), p);
    h.assertTrue(v != null && !BlockPos.containing(v).equals(core), "Return landed on magma");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void eliteUpgradePreservesIdentityGrantsNativeSkillsAndNeverRepeats(
      GameTestHelper h) {
    var l = realm(h);
    var m = orc(l);
    h.assertTrue(HighRealmElites.empower(m), "Elite upgrade failed");
    h.assertTrue(
        m.getType() == io.github.manasmods.tensura.registry.entity.MonsterEntityTypes.ORC.get()
            && m.getMaxHealth() >= 1000,
        "Elite identity/stats incorrect");
    near(h, EnergyHelper.getBaseMaxEP(m), 2500000 * HighRealmConfig.ELITE_POWER.get(), "Elite EP");
    h.assertTrue(
        TensuraStorages.getExistenceFrom(m).getSpiritualHealth() > 4000, "Elite SHP missing");
    h.assertTrue(
        SkillAPI.getSkillsFrom(m)
            .getSkill(ResistanceSkills.PHYSICAL_ATTACK_RESISTANCE.get())
            .isPresent(),
        "Native resistance missing");
    h.assertTrue(
        !m.isPersistenceRequired() && !m.requiresCustomPersistence(),
        "Named elite would bypass despawn caps");
    m.setHealth(37);
    var tag = new CompoundTag();
    m.saveWithoutId(tag);
    var restored = orc(l);
    restored.load(tag);
    h.assertTrue(!HighRealmElites.empower(restored), "Reload upgraded elite twice");
    near(h, restored.getHealth(), 37, "Reload healed elite");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void savedEliteEpHalvesOnceWithoutHealingOrChangingOtherStats(GameTestHelper h) {
    var m = orc(realm(h));
    HighRealmElites.empower(m);
    // Model an old saved elite with unequal aura/magicule pools.
    m.getPersistentData().remove(HighRealmElites.EP_REBALANCED);
    m.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(4000000);
    m.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(1000000);
    m.setHealth(37);
    var energy = TensuraStorages.getExistenceFrom(m);
    // Real saved mobs have completed Tensura's first-join resource initialization.
    energy.setSkippingEPDrop(false);
    energy.setAura(123);
    energy.setMagicule(900000);
    energy.setSpiritualHealth(79);
    double damage = m.getAttributeValue(Attributes.ATTACK_DAMAGE), hp = m.getMaxHealth();
    var saved = new CompoundTag();
    m.saveWithoutId(saved);
    var restored = orc(realm(h));
    restored.load(saved);
    NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(restored, restored.level()));
    near(h, restored.getAttributeBaseValue(TensuraAttributes.MAX_AURA), 2000000, "Aura not halved");
    near(
        h,
        restored.getAttributeBaseValue(TensuraAttributes.MAX_MAGICULE),
        500000,
        "Magicules not halved");
    var pool = TensuraStorages.getExistenceFrom(restored);
    near(h, pool.getAura(), 123, "Migration refilled aura");
    near(
        h, pool.getMagicule(), EnergyHelper.getMaxMagicule(restored), "Magicules exceeded new cap");
    near(h, restored.getHealth(), 37, "Migration healed HP");
    near(h, pool.getSpiritualHealth(), 79, "Migration changed SHP");
    near(h, restored.getMaxHealth(), hp, "Migration changed max HP");
    near(
        h,
        restored.getAttributeValue(Attributes.ATTACK_DAMAGE),
        damage,
        "Migration changed damage");
    saved = new CompoundTag();
    restored.saveWithoutId(saved);
    var again = orc(realm(h));
    again.load(saved);
    NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(again, again.level()));
    near(h, EnergyHelper.getBaseMaxEP(again), 2500000, "Reload halved EP again");
    var owned = orc(realm(h));
    owned.getPersistentData().putBoolean(HighRealmElites.MARKER, true);
    TensuraStorages.getExistenceFrom(owned).setPermanentOwner(UUID.randomUUID());
    EnergyHelper.setBaseMaxEP(owned, 5000000);
    h.assertTrue(!HighRealmElites.rebalanceEP(owned), "Owned elite migrated");
    near(h, EnergyHelper.getBaseMaxEP(owned), 5000000, "Owned elite lost EP");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void elitesExcludeBossesOwnedMobsAndOrdinaryOverworldMobs(GameTestHelper h) {
    var l = realm(h);
    for (var type :
        new EntityType<?>[] {
          EntityType.WITHER, EntityType.ENDER_DRAGON, EntityType.WARDEN, EntityType.COW
        }) {
      var entity = type.create(l);
      h.assertTrue(
          !(entity instanceof Mob mob) || !HighRealmElites.empower(mob), "Forbidden elite " + type);
    }
    var m = orc(l);
    TensuraStorages.getExistenceFrom(m).setPermanentOwner(UUID.randomUUID());
    h.assertTrue(!HighRealmElites.empower(m), "Owned subordinate upgraded");
    var summoned = orc(l);
    TensuraStorages.getExistenceFrom(summoned).setSummoner(UUID.randomUUID());
    h.assertTrue(!HighRealmElites.empower(summoned), "Summon upgraded");
    h.assertTrue(!HighRealmElites.empower(orc(h.getLevel())), "Overworld mob upgraded");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void naturalSpawnsAreMarkedButImportedMobsAreUntouched(GameTestHelper h) {
    var l = realm(h);
    var m = orc(l);
    var event =
        new FinalizeSpawnEvent(
            m,
            l,
            m.getX(),
            m.getY(),
            m.getZ(),
            l.getCurrentDifficultyAt(m.blockPosition()),
            MobSpawnType.NATURAL,
            null,
            null);
    NeoForge.EVENT_BUS.post(event);
    h.assertTrue(
        !event.isSpawnCancelled() && m.getPersistentData().getBoolean(HighRealmElites.CANDIDATE),
        "Natural elite not marked");
    NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(m, l));
    h.assertTrue(HighRealmElites.elite(m), "Natural join not empowered");
    var imported = orc(l);
    NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(imported, l));
    h.assertTrue(!HighRealmElites.elite(imported), "Imported mob silently upgraded");
    var summoned = orc(l);
    NeoForge.EVENT_BUS.post(
        new FinalizeSpawnEvent(
            summoned,
            l,
            96,
            160,
            96,
            l.getCurrentDifficultyAt(summoned.blockPosition()),
            MobSpawnType.MOB_SUMMONED,
            null,
            null));
    h.assertTrue(
        !summoned.getPersistentData().getBoolean(HighRealmElites.CANDIDATE),
        "Summoned spawn marked");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void returnSanctumRepelsNaturalSpawnsAndExplosionDamage(GameTestHelper h) {
    var l = realm(h);
    var d = RealmData.get(l.getServer());
    var saved = d.sanctuary;
    var center = new BlockPos(96, 160, 96);
    d.sanctuary = center;
    try {
      var mob = orc(l);
      var spawn =
          new FinalizeSpawnEvent(
              mob,
              l,
              96,
              160,
              96,
              l.getCurrentDifficultyAt(center),
              MobSpawnType.NATURAL,
              null,
              null);
      NeoForge.EVENT_BUS.post(spawn);
      h.assertTrue(spawn.isSpawnCancelled(), "Sanctum allows natural spawn");
      var outside = center.offset(70, 0, 0);
      var explosion =
          new net.minecraft.world.level.Explosion(
              l,
              null,
              96,
              160,
              96,
              4,
              false,
              net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);
      explosion.getToBlow().add(center);
      explosion.getToBlow().add(outside);
      NeoForge.EVENT_BUS.post(
          new net.neoforged.neoforge.event.level.ExplosionEvent.Detonate(
              l, explosion, new java.util.ArrayList<>()));
      h.assertTrue(
          !explosion.getToBlow().contains(center) && explosion.getToBlow().contains(outside),
          "Sanctum protection leaks or blocks normal terrain explosions");
    } finally {
      d.sanctuary = saved;
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void eliteRewardsUseNativeEpAndArrowsReceiveTheEliteBonus(GameTestHelper h) {
    var l = realm(h);
    var mob = orc(l);
    HighRealmElites.empower(mob);
    var p = LoanSharkGameTests.actor(h);
    double before = EnergyHelper.getMaxEP(p);
    io.github.manasmods.tensura.handler.DeathHandler.gainEPEntity(p, mob, 9);
    h.assertTrue(
        EnergyHelper.getMaxEP(p) > before && Double.isFinite(EnergyHelper.getMaxEP(p)),
        "Native elite kill EP missing or invalid");
    var arrow = new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW, l);
    arrow.setOwner(mob);
    NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(arrow, l));
    h.assertTrue(
        arrow.getBaseDamage() >= 30 * HighRealmConfig.ELITE_POWER.get(),
        "Elite arrow bonus missing");
    var drops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
    NeoForge.EVENT_BUS.post(
        new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(
            mob, mob.damageSources().playerAttack(p), drops, true));
    h.assertTrue(
        drops.stream().anyMatch(e -> e.getItem().is(net.minecraft.world.item.Items.AMETHYST_SHARD)),
        "Elite reward missing");
    h.succeed();
  }
}
