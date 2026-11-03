package dev.heavenlyrestriction.splitmind;

import net.minecraft.world.entity.LivingEntity;

/** Target binding for native delayed projectiles; no post-launch homing is added. */
public interface SplitMindProjectile {
  void additions$bindSplitMindTarget(LivingEntity target);
}
