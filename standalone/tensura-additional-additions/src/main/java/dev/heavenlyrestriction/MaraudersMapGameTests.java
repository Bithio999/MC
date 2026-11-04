package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.cartography.*;
import dev.heavenlyrestriction.mixin.MapChunkAccess;
import io.github.manasmods.manascore.skill.api.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class MaraudersMapGameTests {
  private static ServerPlayer reader(GameTestHelper h) {
    var p = player(h);
    RestrictionData.learnReward(p, HeavenlyRestriction.MARAUDERS_MAP.get());
    SkillAPI.getSkillsFrom(p)
        .getSkill(HeavenlyRestriction.MARAUDERS_MAP.get())
        .orElseThrow()
        .setMastery(0);
    return p;
  }

  private static CompoundTag pin(ServerPlayer p, String name) {
    var t = new CompoundTag();
    t.putString("name", name);
    t.putInt("x", p.getBlockX());
    t.putInt("y", p.getBlockY());
    t.putInt("z", p.getBlockZ());
    t.putInt("color", 3);
    t.putInt("icon", 1);
    return t;
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void mapIsNativeUniqueFreeAndRequiresTheLearnedUsableSkill(GameTestHelper h) {
    var p = player(h);
    h.assertTrue(!MapService.open(p), "Missing skill opened map");
    var skill = HeavenlyRestriction.MARAUDERS_MAP.get();
    RestrictionData.learnReward(p, skill);
    var i = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();
    i.setMastery(-1);
    h.assertTrue(!MapService.open(p), "Unlearned map opened");
    i.setMastery(0);
    h.assertTrue(MapService.open(p), "Learned map did not open");
    h.assertTrue(
        io.github.manasmods.tensura.menu.ReincarnationMenu.getSkillPool().contains(skill),
        "Map missing from native reincarnation pool");
    h.assertTrue(
        !ChainedReincarnation.blocked(skill, p), "Free map incorrectly blocked for Fettered Human");
    h.assertTrue(
        skill.getMagiculeCost(p, i, 0) == 0 && skill.getAuraCost(p, i, 0) == 0,
        "Map charged resources");
    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(HeavenlyArsenal.SKILL_SEAL, 40));
    h.assertTrue(!MapService.open(p), "Sealed map opened");
    MapService.close(p);
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void pinsArePrivateValidatedEditableAndPersistentWithAHardLimit(GameTestHelper h) {
    var p = reader(h);
    var other = reader(h);
    var d = MapData.get(p.server);
    h.assertTrue(MapService.pin(p, pin(p, "Northern camp")), "Pin failed");
    var created = d.pins(p.getUUID()).getFirst();
    var edit = created.tag();
    edit.putString("name", "Home base");
    edit.putInt("color", 7);
    h.assertTrue(MapService.pin(p, edit), "Owned edit failed");
    h.assertTrue(!MapService.pin(other, edit), "Foreign pin edited");
    var invalid = pin(p, " ");
    h.assertTrue(!MapService.pin(p, invalid), "Blank label accepted");
    invalid = pin(p, "Bad\nlabel");
    h.assertTrue(!MapService.pin(p, invalid), "Control characters accepted");
    invalid = pin(p, "Beyond border");
    invalid.putInt("x", Integer.MAX_VALUE);
    h.assertTrue(!MapService.pin(p, invalid), "Out-of-world pin accepted");
    invalid = pin(p, "Wrong icon");
    invalid.putInt("icon", 100);
    h.assertTrue(!MapService.pin(p, invalid), "Invalid icon accepted");
    h.assertTrue(MapService.open(p) && MapService.open(other), "Open failed");
    var request = pin(p, "Forged");
    MapService.request(other, MapService.session(p).token, "pin", request);
    h.assertTrue(d.pins(other.getUUID()).isEmpty(), "Foreign session accepted");
    h.assertTrue(
        MapService.snapshot(other, MapService.session(other))
            .getList("pins", Tag.TAG_COMPOUND)
            .isEmpty(),
        "Private pins leaked");
    for (int n = 1; n < MapData.MAX_PINS; n++)
      h.assertTrue(MapService.pin(p, pin(p, "Pin " + n)), "Pin capacity too small");
    h.assertTrue(!MapService.pin(p, pin(p, "Overflow")), "Pin cap bypassed");
    var loaded = MapData.load(d.save(new CompoundTag(), p.registryAccess()), p.registryAccess());
    h.assertTrue(
        loaded.world.equals(d.world) && loaded.pins(p.getUUID()).size() == 256,
        "Pin save/load lost records");
    h.assertTrue(
        loaded.pins(p.getUUID()).getFirst().name().equals("Home base"), "Edit not persisted");
    h.assertTrue(!loaded.removePin(other.getUUID(), created.id()), "Foreign deletion succeeded");
    h.assertTrue(loaded.removePin(p.getUUID(), created.id()), "Owner deletion failed");
    MapService.close(p);
    MapService.close(other);
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void realStructureStartsAreMarkedAndNewSeedsKeepSeparateAtlases(GameTestHelper h) {
    var p = reader(h);
    var level = h.getLevel();
    var at = h.absolutePos(BlockPos.ZERO).offset(5, 3, 5);
    var chunk = level.getChunkAt(at);
    var structure =
        level
            .registryAccess()
            .registryOrThrow(Registries.STRUCTURE)
            .getOrThrow(BuiltinStructures.BURIED_TREASURE);
    var old = chunk.getStartForStructure(structure);
    var start =
        new StructureStart(
            structure,
            chunk.getPos(),
            0,
            new PiecesContainer(List.of(new BuriedTreasurePieces.BuriedTreasurePiece(at))));
    try {
      chunk.setStartForStructure(structure, start);
      MapService.capture(level, chunk);
      var d = MapData.get(p.server);
      var marker =
          d.structures.values().stream()
              .filter(
                  m ->
                      m.type().equals("minecraft:buried_treasure")
                          && m.key().endsWith("/" + chunk.getPos().toLong()))
              .findFirst()
              .orElseThrow();
      h.assertTrue(
          marker.x() == start.getBoundingBox().getCenter().getX()
              && marker.z() == start.getBoundingBox().getCenter().getZ()
              && marker.y() == start.getBoundingBox().minY(),
          "Marker did not use real bounds");
      MapService.capture(level, chunk);
      long count = d.structures.values().stream().filter(m -> m.key().equals(marker.key())).count();
      h.assertTrue(count == 1, "Structure marker duplicated");
      var isolated =
          MapData.load(d.save(new CompoundTag(), p.registryAccess()), p.registryAccess());
      var before = isolated.world;
      isolated.ensureSeed(level.getServer().overworld().getSeed() + 1);
      h.assertTrue(
          !isolated.world.equals(before) && isolated.structures.isEmpty(),
          "World Hop reused another seed's atlas");
    } finally {
      chunk.setStartForStructure(structure, old == null ? StructureStart.INVALID_START : old);
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void structureRequestsUseNativeAsyncStartsAndSessionsCanBeCancelled(
      GameTestHelper h) {
    var p = reader(h);
    MapService.open(p);
    var s = MapService.session(p);
    var t = new CompoundTag();
    t.putString("type", "minecraft:buried_treasure");
    MapService.request(p, s.token, "locate", t);
    if (s.search != null && !s.search.done) {
      MapService.advance(p, s);
      h.assertTrue(
          s.search.pending == null || !s.search.pending.isCompletedExceptionally(),
          "Native search scheduling failed");
    }
    s.nextRequest = 0;
    MapService.request(p, s.token, "cancel", new CompoundTag());
    h.assertTrue(s.search == null, "Search not cancelled");
    var at = h.absolutePos(BlockPos.ZERO);
    var pos = new ChunkPos(at);
    var future =
        ((MapChunkAccess) h.getLevel().getChunkSource())
            .additions$mapFuture(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS, true);
    h.assertTrue(future != null, "Native async scheduling failed");
    h.startSequence()
        .thenWaitUntil(() -> h.assertTrue(future.isDone(), "Structure start task not finished"))
        .thenExecute(
            () -> {
              h.assertTrue(future.getNow(null).isSuccess(), "Native structure start task failed");
              MapService.close(p);
              h.succeed();
            });
  }

  @GameTest(template = "empty", timeoutTicks = 300)
  public static void unloadedTerrainPreviewsUseNativeGeneratorWithoutLoadingChunks(
      GameTestHelper h) {
    var p = reader(h);
    MapService.open(p);
    var session = MapService.session(p);
    session.cx = 1000000;
    session.cz = -1000000;
    session.radius = 2048;
    h.assertTrue(
        MapTerrain.valid(
            session, Math.floorDiv(session.cx, 128), Math.floorDiv(session.cz, 128), 4, false),
        "Valid distant tile denied");
    h.assertTrue(!MapTerrain.valid(session, Integer.MAX_VALUE, 0, 1, false), "Overflow accepted");
    h.assertTrue(!MapTerrain.valid(session, 0, 0, 3, false), "Nonpower spacing accepted");
    h.assertTrue(!MapTerrain.valid(session, 0, 0, 1, false), "Off-view tile accepted");
    int cx = session.cx >> 4, cz = session.cz >> 4;
    h.assertTrue(
        h.getLevel().getChunkSource().getChunkNow(cx, cz) == null,
        "Test coordinate was already loaded");
    var request = new CompoundTag();
    request.putInt("tx", Math.floorDiv(session.cx, 128));
    request.putInt("tz", Math.floorDiv(session.cz, 128));
    request.putInt("step", 4);
    MapService.request(p, session.token, "terrain", request);
    h.assertTrue(!session.terrain.isEmpty(), "Terrain job never queued");
    h.startSequence()
        .thenWaitUntil(() -> h.assertTrue(session.terrain.isEmpty(), "Preview still running"))
        .thenExecute(
            () -> {
              h.assertTrue(
                  h.getLevel().getChunkSource().getChunkNow(cx, cz) == null,
                  "Preview loaded distant chunk");
              MapService.close(p);
              h.succeed();
            });
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void nativeSurfaceDetailUsesTheTopBlockAndDepthTint(GameTestHelper h) {
    var p = reader(h);
    var at = h.absolutePos(BlockPos.ZERO).offset(3, 10, 3);
    h.getLevel()
        .setBlockAndUpdate(
            at, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState());
    var chunk = h.getLevel().getChunkAt(at);
    var tile = MapTerrain.detail(p.serverLevel(), chunk, Integer.MAX_VALUE);
    int n = Math.floorMod(at.getZ(), 16) * 16 + Math.floorMod(at.getX(), 16);
    h.assertTrue(
        tile.getIntArray("heights")[n] == at.getY(), "Surface map has a one-block height error");
    h.assertTrue(
        tile.getIntArray("blocks")[n]
            == net.minecraft.world.level.block.Block.getId(
                net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState()),
        "Top material was not preserved");
    h.assertTrue(tile.getIntArray("tints").length == 256, "Missing native biome tints");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void playerLocationsIncludeDistantPlayersAndOtherDimensions(GameTestHelper h) {
    var p = reader(h);
    var far =
        net.neoforged.neoforge.common.util.FakePlayerFactory.get(
            h.getLevel(),
            new com.mojang.authlib.GameProfile(UUID.randomUUID(), "DistantCartographer"));
    far.setPos(1000000.5, 123, -1000000.25);
    far.setYRot(73);
    var players = reviewRoster(p);
    var nether = p.server.getLevel(net.minecraft.world.level.Level.NETHER);
    h.assertTrue(nether != null, "Native Nether missing");
    var traveler =
        net.neoforged.neoforge.common.util.FakePlayerFactory.get(
            nether, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "RealmWalker"));
    traveler.setPos(-40.5, 70, 120.5);
    players.add(far);
    players.add(traveler);
    try {
      var list = MapService.players(p);
      h.assertTrue(
          list.stream()
              .map(t -> (CompoundTag) t)
              .anyMatch(
                  t ->
                      t.getUUID("uuid").equals(traveler.getUUID())
                          && t.getString("dimension").equals("minecraft:the_nether")),
          "Other-dimension player omitted");
      var marker =
          list.stream()
              .map(t -> (CompoundTag) t)
              .filter(t -> t.getUUID("uuid").equals(far.getUUID()))
              .findFirst()
              .orElseThrow();
      h.assertTrue(
          marker.getDouble("px") == far.getX() && marker.getInt("y") == 123,
          "Distant player location was rounded/lost");
      h.assertTrue(
          marker.getString("dimension").equals(h.getLevel().dimension().location().toString()),
          "Player dimension missing");
      far.setPos(1000100.5, 127, -1000001.25);
      var updated =
          (CompoundTag)
              MapService.players(p).stream()
                  .filter(t -> ((CompoundTag) t).getUUID("uuid").equals(far.getUUID()))
                  .findFirst()
                  .orElseThrow();
      h.assertTrue(updated.getInt("x") == 1000100, "Location not live");
    } finally {
      players.remove(far);
      players.remove(traveler);
    }
    h.assertTrue(
        MapService.players(p).stream()
            .noneMatch(t -> ((CompoundTag) t).getUUID("uuid").equals(far.getUUID())),
        "Disconnected player retained");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void locateUsesAHundredThousandBlockRadiusAndRejectsOutsideResults(
      GameTestHelper h) {
    var p = reader(h);
    MapService.open(p);
    var session = MapService.session(p);
    session.cx = 22000000;
    session.cz = -22000000;
    var d = MapData.get(p.server);
    String dim = p.level().dimension().location().toString();
    var inside =
        new MapData.Marker(
            "range/" + p.getUUID(),
            "minecraft:stronghold",
            dim,
            session.cx + 90000,
            40,
            session.cz);
    var outside =
        new MapData.Marker(
            "outside/" + p.getUUID(),
            "minecraft:stronghold",
            dim,
            session.cx + 100001,
            40,
            session.cz);
    d.add(inside);
    d.add(outside);
    var request = new CompoundTag();
    request.putString("type", "minecraft:stronghold");
    MapService.request(p, session.token, "locate", request);
    h.assertTrue(
        MapService.SEARCH_RANGE == 100000 && session.found == inside,
        "Distant in-range structure not found");
    d.structures.remove(inside.key());
    session.nextLocate = 0;
    MapService.request(p, session.token, "locate", request);
    h.assertTrue(session.found == null, "Out-of-range structure returned");
    if (session.search != null) {
      var job = session.search;
      MapService.request(p, session.token, "cancel", new CompoundTag());
      h.assertTrue(job.cancelled, "Cancelled background search kept running");
    }
    d.structures.remove(outside.key());
    MapService.close(p);
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void travelOnlyOffersOwnedLearnedUsableCoordinateSkills(GameTestHelper h) {
    var p = reader(h);
    h.assertTrue(MapTravel.skills(p).isEmpty(), "Unowned spatial skills offered");
    var skill = io.github.manasmods.tensura.registry.skill.ExtraSkills.SPATIAL_MOTION.get();
    RestrictionData.learnReward(p, skill);
    var i = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();
    i.setMastery(-1);
    h.assertTrue(MapTravel.skills(p).isEmpty(), "Unlearned spatial skill offered");
    i.setMastery(0);
    h.assertTrue(
        MapTravel.skills(p).stream().anyMatch(a -> a == i), "Learned Spatial Motion missing");
    int mode =
        ((io.github.manasmods.tensura.ability.subclass.ISpatialMovement) skill)
            .getSpatialMovementModeIndex();
    i.setCoolDown(40, mode);
    h.assertTrue(!MapTravel.eligible(p, i), "Cooldown bypassed");
    i.setCoolDown(0, mode);
    RestrictionData.learnReward(p, HeavenlyRestriction.INSTANT_TRANSMISSION.get());
    h.assertTrue(MapTravel.skills(p).size() == 2, "Aura-based Instant Transmission not compatible");
    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(HeavenlyArsenal.SKILL_SEAL, 40));
    h.assertTrue(MapTravel.skills(p).isEmpty(), "Sealed skills offered");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void travelPreparesSafeNativeCoordinatesWithoutWarpingOrCharging(GameTestHelper h) {
    var p = reader(h);
    RestrictionData.learnReward(p, HeavenlyRestriction.INSTANT_TRANSMISSION.get());
    MapService.open(p);
    var session = MapService.session(p);
    var marker =
        new MapData.Marker(
            "travel/" + p.getUUID(),
            "minecraft:village_plains",
            session.dimension,
            p.getBlockX(),
            p.getBlockY(),
            p.getBlockZ());
    MapData.get(p.server).add(marker);
    var energy = io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(p);
    double aura = energy.getAura(), magicules = energy.getMagicule();
    var before = p.position();
    h.assertTrue(
        !MapTravel.prepare(p, session, marker.key(), "tensura:spatial_motion"),
        "Unowned skill authorized");
    h.assertTrue(
        !MapTravel.prepare(
            p,
            session,
            "forged-target",
            HeavenlyRestriction.INSTANT_TRANSMISSION.get().getRegistryName().toString()),
        "Forged destination authorized");
    h.assertTrue(
        MapTravel.prepare(
            p,
            session,
            marker.key(),
            HeavenlyRestriction.INSTANT_TRANSMISSION.get().getRegistryName().toString()),
        "Owned native spatial skill rejected");
    var pending = session.travel;
    h.assertTrue(
        !MapTravel.prepare(
            p,
            session,
            marker.key(),
            HeavenlyRestriction.INSTANT_TRANSMISSION.get().getRegistryName().toString()),
        "Duplicate arrival job accepted");
    h.startSequence()
        .thenWaitUntil(() -> h.assertTrue(pending.future().isDone(), "Arrival chunk not ready"))
        .thenExecute(
            () -> {
              var chunk = pending.future().getNow(null).orElse(null);
              var landing = MapTravel.landing(p, chunk, marker.x(), marker.z());
              h.assertTrue(
                  landing != null
                      && chunk.getBlockState(landing).getCollisionShape(chunk, landing).isEmpty()
                      && chunk
                          .getBlockState(landing.above())
                          .getCollisionShape(chunk, landing.above())
                          .isEmpty(),
                  "Arrival has no body clearance");
              MapTravel.advance(p, session);
              h.assertTrue(
                  MapService.session(p) == null, "Native menu handoff did not close atlas session");
              h.assertTrue(p.position().equals(before), "Preparing menu teleported player");
              h.assertTrue(
                  energy.getAura() == aura && energy.getMagicule() == magicules,
                  "Preparing menu charged resources");
              MapData.get(p.server).structures.remove(marker.key());
              h.succeed();
            });
  }

  @GameTest(template = "empty", timeoutTicks = 200)
  public static void forgottenTeleportSkillsCancelTheHandoff(GameTestHelper h) {
    var p = reader(h);
    RestrictionData.learnReward(p, HeavenlyRestriction.INSTANT_TRANSMISSION.get());
    MapService.open(p);
    var session = MapService.session(p);
    var marker =
        new MapData.Marker(
            "forgotten/" + p.getUUID(),
            "minecraft:village_plains",
            session.dimension,
            p.getBlockX(),
            p.getBlockY(),
            p.getBlockZ());
    MapData.get(p.server).add(marker);
    h.assertTrue(
        MapTravel.prepare(
            p,
            session,
            marker.key(),
            HeavenlyRestriction.INSTANT_TRANSMISSION.get().getRegistryName().toString()),
        "Prepare failed");
    var pending = session.travel;
    SkillAPI.getSkillsFrom(p)
        .forgetSkill(
            SkillAPI.getSkillsFrom(p)
                .getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get())
                .orElseThrow());
    h.startSequence()
        .thenWaitUntil(() -> h.assertTrue(pending.future().isDone(), "Arrival not ready"))
        .thenExecute(
            () -> {
              MapTravel.advance(p, session);
              h.assertTrue(
                  MapService.session(p) == session && session.travel == null,
                  "Forgotten skill still opened native menu");
              MapService.close(p);
              MapData.get(p.server).structures.remove(marker.key());
              h.succeed();
            });
  }
}
