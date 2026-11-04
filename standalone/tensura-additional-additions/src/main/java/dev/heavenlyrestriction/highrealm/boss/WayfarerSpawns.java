package dev.heavenlyrestriction.highrealm.boss;

import dev.heavenlyrestriction.highrealm.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Rare native biome spawn, independent from elite acceptance; cooldown is committed only on join.
 */
public final class WayfarerSpawns {
  private static final String NATURAL = "heavenlyrestriction:wayfarer_natural";

  public static boolean natural(MobSpawnType type) {
    return type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION;
  }

  public static boolean validSurface(EntityType<?> type, ServerLevelAccessor world, BlockPos pos) {
    return world.getLevel().dimension().equals(HighRealm.LEVEL)
        && world.getDifficulty() != Difficulty.PEACEFUL
        && world.getLevel().getWorldBorder().isWithinBounds(pos)
        && world.canSeeSky(pos)
        && world.getBlockState(pos.below()).isValidSpawn(world, pos.below(), type)
        && world.getFluidState(pos).isEmpty()
        && world.getFluidState(pos.above()).isEmpty()
        && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
        && world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).isEmpty();
  }

  public static boolean allowed(ServerLevel level, Vec3 pos) {
    var data = RealmData.get(level.getServer());
    return HighRealmConfig.WAYFARER_SPAWNS.get()
        && level.dimension().equals(HighRealm.LEVEL)
        && level.getDifficulty() != Difficulty.PEACEFUL
        && level.getServer().overworld().getGameTime() >= data.nextWayfarerSpawn
        && (data.sanctuary == null
            || pos.distanceToSqr(Vec3.atCenterOf(data.sanctuary)) >= 192 * 192)
        && level
            .getEntitiesOfClass(
                KaelVeyrEntity.class,
                new AABB(pos, pos).inflate(512),
                b -> b.isAlive() && !b.isTame())
            .isEmpty();
  }

  public static void placements(RegisterSpawnPlacementsEvent e) {
    e.register(
        WayfarerContent.KAEL.get(),
        SpawnPlacementTypes.ON_GROUND,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        (type, world, reason, pos, random) -> !natural(reason) || validSurface(type, world, pos),
        RegisterSpawnPlacementsEvent.Operation.REPLACE);
  }

  public static void finalizing(FinalizeSpawnEvent e) {
    if (!(e.getEntity() instanceof KaelVeyrEntity boss) || !natural(e.getSpawnType())) return;
    var level = e.getLevel().getLevel();
    if (boss.getRandom().nextDouble() >= HighRealmConfig.WAYFARER_CHANCE.get()
        || !validSurface(boss.getType(), e.getLevel(), boss.blockPosition())
        || !allowed(level, boss.position())) {
      e.setSpawnCancelled(true);
      return;
    }
    boss.getPersistentData().putBoolean(NATURAL, true);
  }

  public static void joined(EntityJoinLevelEvent e) {
    if (!(e.getLevel() instanceof ServerLevel level)
        || !(e.getEntity() instanceof KaelVeyrEntity boss)) return;
    boss.initialize();
    if (!e.loadedFromDisk() && boss.getPersistentData().getBoolean(NATURAL)) {
      // A second candidate finalized in the same tick must not bypass the first one's cooldown.
      if (!allowed(level, boss.position())) {
        e.setCanceled(true);
        return;
      }
      var data = RealmData.get(level.getServer());
      data.nextWayfarerSpawn =
          level.getServer().overworld().getGameTime() + HighRealmConfig.WAYFARER_COOLDOWN.get();
      data.setDirty();
      boss.getPersistentData().remove(NATURAL);
    }
  }

  private WayfarerSpawns() {}
}
