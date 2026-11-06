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

  public static final class Pending {
    final ServerLevel level;
    final UUID ticket = UUID.randomUUID();
    final ManasSkill skill;
    final ChunkPos source;
    MapData.Marker marker;
    ChunkPos requested;
    ChunkStatus status;
    ChunkAccess origin;
    int relocations;
    CompletableFuture<ChunkResult<ChunkAccess>> future;

    Pending(ServerLevel level, ManasSkill skill, MapData.Marker marker, ChunkPos source) {
      this.level = level;
      this.skill = skill;
      this.marker = marker;
      this.source = source;
    }

    public CompletableFuture<ChunkResult<ChunkAccess>> future() {
      return future;
    }

    void request(ChunkPos pos, ChunkStatus status) {
      this.requested = pos;
      this.status = status;
      renew();
      try {
        future =
            ((MapChunkAccess) level.getChunkSource())
                .additions$mapFuture(pos.x, pos.z, status, true);
      } catch (RuntimeException ex) {
        release();
        throw ex;
      }
    }

    void renew() {
      ((MapChunkAccess) level.getChunkSource())
          .additions$mapTickets()
          .addTicket(TICKET, requested, ChunkLevel.byStatus(status), ticket);
    }

    void release() {
      if (requested != null)
        ((MapChunkAccess) level.getChunkSource())
            .additions$mapTickets()
            .removeTicket(TICKET, requested, ChunkLevel.byStatus(status), ticket);
    }
  }

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

  private static boolean allowedDimension(ServerPlayer p, ManasSkillInstance i, String dimension) {
    if (((ISpatialMovement) i.getSkill()).getBlacklistDimensions().contains(dimension))
      return false;
    // Native spatial movement normally supplies a same-dimension destination list. Browsing another
    // dimension must never grant a cross-dimension ability the owned skill does not provide.
    return dimension.equals(p.level().dimension().location().toString());
  }

  private static MapData.Marker marker(ServerPlayer p, MapService.Session s, String id) {
    var m = MapData.get(p.server).structures.get(id);
    return m != null && m.dimension().equals(s.dimension) ? m : null;
  }

  public static boolean options(ServerPlayer p, MapService.Session s, String id) {
    var m = marker(p, s, id);
    if (m == null) return false;
    var options = new ListTag();
    for (var i : skills(p)) {
      if (!allowedDimension(p, i, s.dimension)) continue;
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
    if (i == null || !eligible(p, i) || !allowedDimension(p, i, s.dimension)) return false;
    var level = MapService.level(p, s);
    var source = m.sourceChunk();
    if (source == null
        || Math.abs((long) source.x * 16) > MapView.LIMIT + 16
        || Math.abs((long) source.z * 16) > MapView.LIMIT + 16
        || !level
            .getWorldBorder()
            .isWithinBounds(new BlockPos(source.getMiddleBlockX(), m.y(), source.getMiddleBlockZ()))
        || !level.getWorldBorder().isWithinBounds(new BlockPos(m.x(), m.y(), m.z()))) return false;
    var job = new Pending(level, i.getSkill(), m, source);
    try {
      // The icon is the bounding-box center; only the origin chunk owns the native start.
      job.request(source, ChunkStatus.STRUCTURE_STARTS);
      s.travel = job;
      ACTIVE.add(job);
      s.message = "Checking " + MapService.name(m.type()) + " and preparing arrival coordinates…";
      return true;
    } catch (RuntimeException ex) {
      job.release();
      return false;
    }
  }

  public static void advance(ServerPlayer p, MapService.Session s) {
    var job = s.travel;
    if (job == null) return;
    if (!job.future.isDone()) {
      job.renew();
      return;
    }
    var owned = SkillAPI.getSkillsFrom(p).getSkill(job.skill).orElse(null);
    if (owned == null || !eligible(p, owned) || !allowedDimension(p, owned, s.dimension)) {
      cancel(s);
      s.message = "That teleport skill is no longer available.";
      return;
    }
    try {
      var result = job.future.getNow(null);
      var chunk = result != null && result.isSuccess() ? result.orElse(null) : null;
      if (chunk == null) {
        cancel(s);
        s.message = "Could not read that structure. Try again shortly.";
        return;
      }
      if (job.origin == null) {
        MapService.capture(job.level, chunk);
        var type =
            job.level
                .registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(ResourceLocation.parse(job.marker.type()));
        var start = type == null ? null : chunk.getStartForStructure(type);
        if (start == null || !start.isValid() || !job.source.equals(start.getChunkPos())) {
          cancel(s);
          s.message = "That world-generation location has no structure in the saved terrain.";
          return;
        }
        var actual = MapData.get(p.server).structures.get(job.marker.key());
        if (actual == null) {
          cancel(s);
          s.message = "That structure is no longer available.";
          return;
        }
        job.origin = chunk;
        job.marker = actual;
        job.release();
        job.request(new ChunkPos(actual.x() >> 4, actual.z() >> 4), ChunkStatus.FEATURES);
        // Completed local futures can finish in the same tick; otherwise wait without blocking.
        if (job.future.isDone()) advance(p, s);
        return;
      }
      MapService.capture(job.level, job.origin);
      var current = MapData.get(p.server).structures.get(job.marker.key());
      if (current == null) {
        cancel(s);
        s.message = "That structure is no longer available.";
        return;
      }
      job.marker = current;
      var landingChunk = new ChunkPos(current.x() >> 4, current.z() >> 4);
      if (!landingChunk.equals(job.requested)) {
        if (++job.relocations > 2) {
          cancel(s);
          s.message = "That structure moved while preparing travel. Select it again.";
          return;
        }
        job.release();
        job.request(landingChunk, ChunkStatus.FEATURES);
        return;
      }
      MapService.capture(job.level, chunk);
      var arrival = landing(p, job.level, chunk, job.marker.x(), job.marker.z());
      if (arrival == null) {
        cancel(s);
        s.message =
            "No safe surface arrival found in that structure's chunk. Choose another structure.";
        return;
      }
      cancel(s);
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
      cancel(s);
      s.message = "Could not prepare those coordinates. Try another structure.";
    }
  }

  public static BlockPos landing(ServerPlayer p, ChunkAccess chunk, int x, int z) {
    return landing(p, p.serverLevel(), chunk, x, z);
  }

  private static BlockPos landing(
      ServerPlayer p, ServerLevel level, ChunkAccess chunk, int x, int z) {
    var cp = chunk.getPos();
    var columns = new ArrayList<BlockPos>();
    for (int dz = 0; dz < 16; dz++)
      for (int dx = 0; dx < 16; dx++)
        columns.add(new BlockPos(cp.getMinBlockX() + dx, 0, cp.getMinBlockZ() + dz));
    columns.sort(Comparator.comparingDouble(at -> Math.hypot(at.getX() - x, at.getZ() - z)));
    for (var at : columns) {
      int top =
          level.dimensionType().hasCeiling()
              ? Math.min(
                  (level == p.serverLevel() ? p.getBlockY() : 64) + 16,
                  level.getMaxBuildHeight() - 2)
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
    job.release();
  }

  private MapTravel() {}
}
