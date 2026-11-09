package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Hover, anticipate, curved high-speed slash and smooth return; no orbiting projectiles. */
public final class ArsenalBlade extends Entity {
  private static final EntityDataAccessor<ItemStack> ITEM =
      SynchedEntityData.defineId(ArsenalBlade.class, EntityDataSerializers.ITEM_STACK);
  private static final EntityDataAccessor<Integer>
      OWNER = SynchedEntityData.defineId(ArsenalBlade.class, EntityDataSerializers.INT),
      INDEX = SynchedEntityData.defineId(ArsenalBlade.class, EntityDataSerializers.INT),
      PHASE = SynchedEntityData.defineId(ArsenalBlade.class, EntityDataSerializers.INT);
  private UUID owner = new UUID(0, 0);
  private ServerPlayer caster;
  private int count = 1, age, nextAttack;
  private LivingEntity target;
  private Vec3 start, control, end;
  private boolean hit;
  public final ArrayDeque<Vec3> trail = new ArrayDeque<>();
  private int lerpTicks;
  private Vec3 lerpTarget;
  private float lerpYaw, lerpPitch;

  public ArsenalBlade(EntityType<? extends ArsenalBlade> type, Level level) {
    super(type, level);
    noPhysics = true;
    setNoGravity(true);
  }

  public UUID ownerId() {
    return owner;
  }

  public int index() {
    return entityData.get(INDEX);
  }

  public int phase() {
    return entityData.get(PHASE);
  }

  public ItemStack item() {
    return entityData.get(ITEM);
  }

  public Entity ownerEntity() {
    return level().getEntity(entityData.get(OWNER));
  }

  public void visual(ItemStack stack) {
    entityData.set(ITEM, stack.copy());
  }

  public static ArsenalBlade spawn(ServerPlayer owner, int index, int count) {
    var b = new ArsenalBlade(ForgeContent.BLADE.get(), owner.level());
    b.owner = owner.getUUID();
    b.caster = owner;
    b.entityData.set(OWNER, owner.getId());
    b.entityData.set(INDEX, index);
    b.count = count;
    b.visual(ForgeArsenal.item(owner, index));
    b.setPos(b.home(owner, 0));
    b.nextAttack = 8 + index * 3;
    ForgeArsenal.bind(owner, index, b);
    owner.level().addFreshEntity(b);
    return b;
  }

  private Vec3 home(ServerPlayer p, float time) {
    double spread = (index() - (count - 1) * .5) * .58;
    Vec3 forward = p.getLookAngle().multiply(1, 0, 1).normalize();
    if (forward.lengthSqr() < .001) forward = new Vec3(0, 0, 1);
    var side = new Vec3(-forward.z, 0, forward.x);
    return p.position()
        .add(0, 1.8 + Math.abs(spread) * .25 + Math.sin((time + index() * 7) * .08) * .10, 0)
        .add(forward.scale(-.75))
        .add(side.scale(spread));
  }

  private LivingEntity target(ServerPlayer p) {
    var focus = p.getLastHurtMob();
    if (focus != null && SplitMindCasting.enemy(p, focus)) return focus;
    return p
        .level()
        .getEntitiesOfClass(
            LivingEntity.class, p.getBoundingBox().inflate(32), e -> SplitMindCasting.enemy(p, e))
        .stream()
        .min(Comparator.comparingDouble(p::distanceToSqr))
        .orElse(null);
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide()) {
      if (lerpTicks > 0) {
        setPos(position().lerp(lerpTarget, 1d / lerpTicks));
        setYRot(net.minecraft.util.Mth.rotLerp(1f / lerpTicks, getYRot(), lerpYaw));
        setXRot(net.minecraft.util.Mth.rotLerp(1f / lerpTicks, getXRot(), lerpPitch));
        lerpTicks--;
      }
      trail.addFirst(position());
      while (trail.size() > 12) trail.removeLast();
      return;
    }
    var p = caster == null ? level().getServer().getPlayerList().getPlayer(owner) : caster;
    if (p == null
        || p.level() != level()
        || !p.isAlive()
        || !ForgeArsenal.active(p)
        || !ForgeArsenal.current(p, index(), this)
        || ForgeArsenal.item(p, index()).isEmpty()) {
      discard();
      return;
    }
    entityData.set(OWNER, p.getId());
    Vec3 before = position(), home = home(p, tickCount);
    if (phase() == 0) {
      setPos(position().lerp(home, .28));
      setYRot(-p.getYRot() + index() * 5);
      setXRot(-12);
      if (tickCount >= nextAttack && (target = target(p)) != null) {
        start = position();
        age = 0;
        hit = false;
        entityData.set(PHASE, 1);
      }
    } else if (phase() == 1) {
      if (target == null || !target.isAlive() || p.distanceToSqr(target) > 40 * 40) {
        entityData.set(PHASE, 2);
        age = 0;
      } else {
        age++;
        double u = Math.clamp(age / 9d, 0, 1), t = u * u * (3 - 2 * u);
        end = target.getBoundingBox().getCenter();
        Vec3 axis = end.subtract(start).normalize(),
            side = new Vec3(-axis.z, .35, axis.x).normalize();
        control =
            start
                .lerp(end, .5)
                .add(side.scale((index() % 2 == 0 ? 1 : -1) * (1.8 + index() * .2)))
                .add(0, 1.2, 0);
        Vec3 next =
            start
                .scale((1 - t) * (1 - t))
                .add(control.scale(2 * t * (1 - t)))
                .add(end.add(axis.scale(1.4)).scale(t * t));
        setPos(next);
        if (!hit
            && (target.getBoundingBox().inflate(.65).contains(next)
                || target.getBoundingBox().inflate(.65).clip(before, next).isPresent())) {
          hit = true;
          ForgeArsenal.hit(p, target, index(), this);
        }
        if (age >= 9) {
          entityData.set(PHASE, 2);
          age = 0;
        }
      }
    } else {
      age++;
      setPos(position().lerp(home, .3));
      if (age >= 9 || distanceToSqr(home) < .06) {
        entityData.set(PHASE, 0);
        nextAttack = tickCount + 5 + index() % 3;
      }
    }
    Vec3 delta = position().subtract(before);
    setDeltaMovement(delta);
    if (phase() != 0 && delta.lengthSqr() > .001) {
      setYRot((float) (Math.toDegrees(Math.atan2(delta.x, delta.z))));
      setXRot(
          (float)
              -Math.toDegrees(
                  Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
    }
  }

  @Override
  public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
    lerpTarget = new Vec3(x, y, z);
    lerpYaw = yaw;
    lerpPitch = pitch;
    lerpTicks = Math.min(3, Math.max(1, steps));
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    b.define(ITEM, ItemStack.EMPTY);
    b.define(OWNER, -1);
    b.define(INDEX, 0);
    b.define(PHASE, 0);
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag t) {}

  @Override
  protected void addAdditionalSaveData(CompoundTag t) {}

  @Override
  public boolean isPickable() {
    return false;
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  @Override
  public AABB getBoundingBoxForCulling() {
    return getBoundingBox().inflate(8);
  }
}
