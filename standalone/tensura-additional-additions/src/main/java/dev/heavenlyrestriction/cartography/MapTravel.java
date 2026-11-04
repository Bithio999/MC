package dev.heavenlyrestriction.cartography;

import dev.heavenlyrestriction.mixin.MapChunkAccess;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.subclass.ISpatialMovement;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Offers owned native spatial skills and prepares coordinates; the native menu performs the warp.
 */
public final class MapTravel {
  private static final TicketType<UUID> TICKET =
      TicketType.create("additions_map_arrival", UUID::compareTo, 200);
  private static final Set<Pending> ACTIVE = new HashSet<>();

  public record Pending(
      ServerLevel level,
      UUID ticket,
      ManasSkill skill,
      MapData.Marker marker,
      CompletableFuture<ChunkResult<ChunkAccess>> future) {}

  public static boolean eligible(ServerPlayer p, ManasSkillInstance instance) {
    if (!(instance.getSkill() instanceof ISpatialMovement spatial) || instance.getMastery() < 0)
      return false;
    int mode = spatial.getSpatialMovementModeIndex();
    return mode >= 0
        && (!instance.onCoolDown(mode) || instance.canIgnoreCoolDown(p, mode))
        && instance.canInteractSkill(p)
        && instance.canActivateSkill(p, mode)
        && spatial.canWarp(instance, p)
        && !spatial.getBlacklistDimensions().contains(p.level().dimension().location().toString())
        && !SkillUtils.shouldCancelInteraction(p)
        && !SkillUtils.shouldCancelTeleportation(p);
  }

  public static List<ManasSkillInstance> skills(ServerPlayer p) {
    return SkillAPI.getSkillsFrom(p).getLearnedSkills().stream()
        .filter(i -> eligible(p, i))
        .sorted(Comparator.comparing(i -> i.getSkill().getName().getString()))
        .toList();
  }

  private static MapData.Marker marker(ServerPlayer p, MapService.Session s, String id) {
    var m = MapData.get(p.server).structures.get(id);
    if (m == null && s.found != null && s.found.key().equals(id)) m = s.found;
    return m != null && m.dimension().equals(s.dimension) ? m : null;
  }

  public static boolean options(ServerPlayer p, MapService.Session s, String id) {
    var m = marker(p, s, id);
    if (m == null) return false;
    var options = new ListTag();
    for (var i : skills(p)) {
      var tag = new CompoundTag();
      tag.putString("id", i.getSkill().getRegistryName().toString());
      tag.putString("name", i.getSkill().getName().getString());
      options.add(tag);
    }
    if (options.isEmpty()) return false;
    var t = new CompoundTag();
    t.putUUID("session", s.token);
    t.put("travelOptions", options);
    var target = m.tag();
    target.putString("name", MapService.name(m.type()));
    t.put("target", target);
    MapNetwork.send(p, t);
    return true;
  }

  public static boolean prepare(ServerPlayer p, MapService.Session s, String id, String skillId) {
    if (s.travel != null || ACTIVE.size() >= 2) return false;
    var m = marker(p, s, id);
    var key = ResourceLocation.tryParse(skillId);
    if (m == null || key == null) return false;
    var i = SkillAPI.getSkillsFrom(p).getSkill(key).orElse(null);
    if (i == null || !eligible(p, i)) return false;
    var level = p.serverLevel();
    var pos = new ChunkPos(m.x() >> 4, m.z() >> 4);
    if (!level.getWorldBorder().isWithinBounds(new BlockPos(m.x(), m.y(), m.z()))) return false;
    UUID ticket = UUID.randomUUID();
    var tickets = ((MapChunkAccess) level.getChunkSource()).additions$mapTickets();
    tickets.addTicket(TICKET, pos, ChunkLevel.byStatus(ChunkStatus.FEATURES), ticket);
    try {
      var future =
          ((MapChunkAccess) level.getChunkSource())
              .additions$mapFuture(pos.x, pos.z, ChunkStatus.FEATURES, true);
      s.travel = new Pending(level, ticket, i.getSkill(), m, future);
      ACTIVE.add(s.travel);
      s.message = "Preparing arrival coordinates near " + MapService.name(m.type()) + "…";
      return true;
    } catch (RuntimeException ex) {
      tickets.removeTicket(TICKET, pos, ChunkLevel.byStatus(ChunkStatus.FEATURES), ticket);
      return false;
    }
  }

  public static void advance(ServerPlayer p, MapService.Session s) {
    var job = s.travel;
    if (job == null) return;
    var pos = new ChunkPos(job.marker.x() >> 4, job.marker.z() >> 4);
    if (!job.future.isDone()) {
      ((MapChunkAccess) job.level.getChunkSource())
          .additions$mapTickets()
          .addTicket(TICKET, pos, ChunkLevel.byStatus(ChunkStatus.FEATURES), job.ticket);
      return;
    }
    cancel(s);
    var owned = SkillAPI.getSkillsFrom(p).getSkill(job.skill).orElse(null);
    if (owned == null || !eligible(p, owned)) {
      s.message = "That teleport skill is no longer available.";
      return;
    }
    try {
      var result = job.future.getNow(null);
      var arrival =
          result != null && result.isSuccess()
              ? landing(p, result.orElse(null), job.marker.x(), job.marker.z())
              : null;
      if (arrival == null) {
        s.message =
            "No safe surface arrival found in that structure's chunk. Choose another structure.";
        return;
      }
      var t = new CompoundTag();
      t.putUUID("session", s.token);
      t.putBoolean("openTravel", true);
      t.putString("skill", job.skill.getRegistryName().toString());
      t.putString("dimension", s.dimension);
      t.putString("name", MapService.name(job.marker.type()));
      t.putDouble("x", arrival.getX() + .5);
      t.putDouble("y", arrival.getY());
      t.putDouble("z", arrival.getZ() + .5);
      var dimensions = new ListTag();
      dimensions.add(StringTag.valueOf(s.dimension));
      t.put("dimensions", dimensions);
      MapService.close(p);
      MapNetwork.send(p, t);
    } catch (RuntimeException ex) {
      s.message = "Could not prepare those coordinates. Try another structure.";
    }
  }

  public static BlockPos landing(ServerPlayer p, ChunkAccess chunk, int x, int z) {
    var level = p.serverLevel();
    var cp = chunk.getPos();
    var columns = new ArrayList<BlockPos>();
    for (int dz = 0; dz < 16; dz++)
      for (int dx = 0; dx < 16; dx++)
        columns.add(new BlockPos(cp.getMinBlockX() + dx, 0, cp.getMinBlockZ() + dz));
    columns.sort(Comparator.comparingDouble(at -> Math.hypot(at.getX() - x, at.getZ() - z)));
    for (var at : columns) {
      int top =
          level.dimensionType().hasCeiling()
              ? Math.min(p.getBlockY() + 16, level.getMaxBuildHeight() - 2)
              : Math.min(
                  chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ())
                      + 1,
                  level.getMaxBuildHeight() - 2);
      int bottom =
          level.dimensionType().hasCeiling()
              ? level.getMinBuildHeight() + 1
              : Math.max(level.getMinBuildHeight() + 1, top - 8);
      for (int y = top; y >= bottom; y--) {
        var feet = new BlockPos(at.getX(), y, at.getZ());
        if (!level.getWorldBorder().isWithinBounds(feet)) continue;
        var floor = chunk.getBlockState(feet.below());
        var foot = chunk.getBlockState(feet);
        var head = chunk.getBlockState(feet.above());
        if (!floor.getFluidState().isEmpty()
            || floor.is(Blocks.MAGMA_BLOCK)
            || floor.is(Blocks.CACTUS)) continue;
        if (!floor.isFaceSturdy(chunk, feet.below(), Direction.UP)) continue;
        if (!safe(foot, chunk, feet) || !safe(head, chunk, feet.above())) continue;
        return feet;
      }
    }
    return null;
  }

  private static boolean safe(
      net.minecraft.world.level.block.state.BlockState state,
      net.minecraft.world.level.BlockGetter level,
      BlockPos pos) {
    return state.getFluidState().isEmpty()
        && !state.is(net.minecraft.tags.BlockTags.FIRE)
        && !state.is(Blocks.POWDER_SNOW)
        && !state.is(Blocks.SWEET_BERRY_BUSH)
        && !state.is(Blocks.WITHER_ROSE)
        && state.getCollisionShape(level, pos).isEmpty();
  }

  public static void cancel(MapService.Session s) {
    var job = s.travel;
    if (job == null) return;
    s.travel = null;
    ACTIVE.remove(job);
    ((MapChunkAccess) job.level.getChunkSource())
        .additions$mapTickets()
        .removeTicket(
            TICKET,
            new ChunkPos(job.marker.x() >> 4, job.marker.z() >> 4),
            ChunkLevel.byStatus(ChunkStatus.FEATURES),
            job.ticket);
  }

  private MapTravel() {}
}
