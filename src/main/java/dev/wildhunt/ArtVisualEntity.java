package dev.wildhunt;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Synced visual geometry only: cannot collide, hurt entities or edit blocks. */
public final class ArtVisualEntity extends Entity {
    public static final int LASH = 0, CRYSTAL = 1, FALLING_ICE = 2;
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(WildHunt.ID, Registries.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<ArtVisualEntity>> TYPE = TYPES.register("art_visual", () ->
        EntityType.Builder.<ArtVisualEntity>of(ArtVisualEntity::new, MobCategory.MISC).sized(0.1f, 0.1f)
            .clientTrackingRange(8).updateInterval(1).noSave().noSummon().build("wildhunt:art_visual"));
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.FLOAT);
    public static void register() { TYPES.register(); }
    public ArtVisualEntity(EntityType<? extends ArtVisualEntity> type, Level level) {
        super(type, level); noPhysics = true; setNoGravity(true);
    }
    public static ArtVisualEntity spawn(Level level, int kind, Vec3 start, Vec3 delta, int ticks) {
        var entity = new ArtVisualEntity(TYPE.get(), level); entity.setPos(start);
        entity.entityData.set(KIND, kind); entity.entityData.set(LIFETIME, ticks);
        entity.entityData.set(END_X, (float)delta.x); entity.entityData.set(END_Y, (float)delta.y); entity.entityData.set(END_Z, (float)delta.z);
        level.addFreshEntity(entity); return entity;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, LASH); builder.define(LIFETIME, 16);
        builder.define(END_X, 0f); builder.define(END_Y, 0f); builder.define(END_Z, 0f);
    }
    public int kind() { return entityData.get(KIND); }
    public int lifetime() { return entityData.get(LIFETIME); }
    public Vec3 end() { return new Vec3(entityData.get(END_X), entityData.get(END_Y), entityData.get(END_Z)); }
    @Override public void tick() { super.tick(); if (!level().isClientSide() && tickCount >= lifetime()) discard(); }
    @Override public AABB getBoundingBoxForCulling() { return new AABB(position(), position().add(end())).inflate(4); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128 * 128; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
