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
      int before = s.search.checked;
      MapService.advance(p, s);
      h.assertTrue(
          s.search.checked - before <= MapService.SCAN_BUDGET, "Search exceeded tick budget");
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
}
