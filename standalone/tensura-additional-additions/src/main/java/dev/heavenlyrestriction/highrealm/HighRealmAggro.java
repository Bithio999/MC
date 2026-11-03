package dev.heavenlyrestriction.highrealm;

import dev.heavenlyrestriction.RestrictionData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.tslat.smartbrainlib.util.BrainUtils;

/**
 * Alert elite wildlife without replacing native movement, attack animations or spell behaviours.
 */
public final class HighRealmAggro {
  public static void tick(EntityTickEvent.Post e) {
    if (e.getEntity() instanceof Mob mob && mob.tickCount % 20 == 0) update(mob);
  }

  public static void update(Mob mob) {
    if (!(mob.level() instanceof ServerLevel level)
        || !level.dimension().equals(HighRealm.LEVEL)
        || !HighRealmElites.elite(mob)
        || !HighRealmElites.unowned(mob)
        || !mob.isAlive()
        || mob.isNoAi()
        || mob.isVehicle()) return;
    var old = mob.getTarget();
    if (old != null && RestrictionData.canHarm(mob, old) && mob.distanceToSqr(old) <= 1600) return;
    var target =
        level.players().stream()
            .filter(
                p ->
                    RestrictionData.canHarm(mob, p)
                        && mob.distanceToSqr(p) <= 1024
                        && mob.getSensing().hasLineOfSight(p))
            .min(java.util.Comparator.comparingDouble(mob::distanceToSqr))
            .orElse(null);
    if (target == null && old != null) {
      mob.setTarget(null);
      BrainUtils.clearMemory(
          mob, net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
    }
    if (target != null) {
      mob.setTarget(target);
      BrainUtils.setTargetOfEntity(mob, target);
    }
  }

  private HighRealmAggro() {}
}
