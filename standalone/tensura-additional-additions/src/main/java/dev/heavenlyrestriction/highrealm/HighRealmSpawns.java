package dev.heavenlyrestriction.highrealm;

import net.minecraft.core.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Realm-local surface rules, preserving native limits everywhere else and real collision checks.
 */
public final class HighRealmSpawns {
  private static boolean scoped(
      EntityType<?> type, ServerLevelAccessor level, MobSpawnType reason) {
    return level.getLevel().dimension().equals(HighRealm.LEVEL)
        && type.is(HighRealmElites.TYPES)
        && (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION);
  }

  private static boolean ground(EntityType<?> type, ServerLevelAccessor level, BlockPos at) {
    var floor = at.below();
    return level.getLevel().getWorldBorder().isWithinBounds(at)
        && level.getBlockState(floor).isValidSpawn(level, floor, type)
        && level.getFluidState(at).isEmpty()
        && level.getFluidState(at.above()).isEmpty()
        && level.getBlockState(at).getCollisionShape(level, at).isEmpty()
        && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty();
  }

  public static void placement(MobSpawnEvent.SpawnPlacementCheck e) {
    if (!scoped(e.getEntityType(), e.getLevel(), e.getSpawnType())
        || e.getResult() != MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT) return;
    boolean valid =
        HighRealmConfig.ELITES.get()
            && e.getLevel().getDifficulty() != Difficulty.PEACEFUL
            && ground(e.getEntityType(), e.getLevel(), e.getPos())
            && Monster.isDarkEnoughToSpawn(e.getLevel(), e.getPos(), e.getRandom());
    e.setResult(
        valid
            ? MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED
            : MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
  }

  public static void position(MobSpawnEvent.PositionCheck e) {
    if (!scoped(e.getEntity().getType(), e.getLevel(), e.getSpawnType())
        || e.getResult() != MobSpawnEvent.PositionCheck.Result.DEFAULT) return;
    var mob = e.getEntity();
    boolean valid =
        HighRealmConfig.ELITES.get()
            && ground(mob.getType(), e.getLevel(), mob.blockPosition())
            && mob.checkSpawnObstruction(e.getLevel());
    e.setResult(
        valid
            ? MobSpawnEvent.PositionCheck.Result.SUCCEED
            : MobSpawnEvent.PositionCheck.Result.FAIL);
  }

  /** Once per finalized spawn, never on reloads, imports, commands or summons. */
  public static boolean allowNatural(Mob mob) {
    if (mob.getRandom().nextDouble() >= HighRealmConfig.SPAWN_CHANCE.get()) return false;
    return mob.level()
            .getEntitiesOfClass(
                Mob.class,
                mob.getBoundingBox().inflate(64),
                other ->
                    other != mob
                        && other.isAlive()
                        && HighRealmElites.elite(other)
                        && HighRealmElites.unowned(other)
                        && mob.distanceToSqr(other) <= 64 * 64)
            .size()
        < HighRealmConfig.LOCAL_CAP.get();
  }

  private HighRealmSpawns() {}
}
