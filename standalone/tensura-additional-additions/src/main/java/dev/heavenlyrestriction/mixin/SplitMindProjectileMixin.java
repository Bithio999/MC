package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.splitmind.SplitMindProjectile;
import io.github.manasmods.tensura.entity.projectile.TensuraFlyingProjectile;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Delayed native shots keep the auto-cast aim; the native delay vector is already synced. */
@Mixin(TensuraFlyingProjectile.class)
public abstract class SplitMindProjectileMixin implements SplitMindProjectile {
  @Unique
  private static final EntityDataAccessor<Integer> ADDITIONS_AUTO_TARGET =
      SynchedEntityData.defineId(TensuraFlyingProjectile.class, EntityDataSerializers.INT);

  @Unique
  private static final String ADDITIONS_TARGET_UUID = "heavenlyrestriction:split_mind_target";

  @Inject(method = "defineSynchedData", at = @At("TAIL"))
  private void additions$targetData(SynchedEntityData.Builder builder, CallbackInfo ci) {
    builder.define(ADDITIONS_AUTO_TARGET, -1);
  }

  @Override
  public void additions$bindSplitMindTarget(LivingEntity target) {
    var shot = (TensuraFlyingProjectile) (Object) this;
    shot.getEntityData().set(ADDITIONS_AUTO_TARGET, target.getId());
    shot.getPersistentData().putUUID(ADDITIONS_TARGET_UUID, target.getUUID());
    shot.updateShootRotation();
  }

  @Inject(method = "updateShootVector", at = @At("HEAD"), cancellable = true, remap = false)
  private void additions$autoAim(CallbackInfo ci) {
    var shot = (TensuraFlyingProjectile) (Object) this;
    int id = shot.getEntityData().get(ADDITIONS_AUTO_TARGET);
    if (id < 0 && !shot.getPersistentData().hasUUID(ADDITIONS_TARGET_UUID)) return;
    Entity target = shot.level().getEntity(id);
    if (shot.level() instanceof ServerLevel level
        && shot.getPersistentData().hasUUID(ADDITIONS_TARGET_UUID)) {
      var uuid = shot.getPersistentData().getUUID(ADDITIONS_TARGET_UUID);
      if (target == null || !target.getUUID().equals(uuid)) target = level.getEntity(uuid);
      if (target != null && target.getId() != id)
        shot.getEntityData().set(ADDITIONS_AUTO_TARGET, target.getId());
    }
    // Keep the last launch direction if the original target disappears.
    if (target instanceof LivingEntity living && living.isAlive()) {
      Vec3 pos = living.getBoundingBox().getCenter();
      var owner = shot.getOwner();
      if (owner != null) {
        Vec3 aim = pos.subtract(owner.getEyePosition());
        float yaw = (float) (Mth.atan2(aim.z, aim.x) * 180 / Math.PI) - 90;
        float pitch = (float) (-Mth.atan2(aim.y, aim.horizontalDistance()) * 180 / Math.PI);
        pos =
            pos.add(
                shot.getTargetOffset().xRot(-pitch * Mth.DEG_TO_RAD).yRot(-yaw * Mth.DEG_TO_RAD));
      }
      shot.setDelayVec(pos.subtract(shot.position()).normalize().scale(shot.getSpeed()));
    }
    ci.cancel();
  }
}
