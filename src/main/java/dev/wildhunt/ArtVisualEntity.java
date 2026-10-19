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
    public static final int LASH = 0, CRYSTAL = 1, FALLING_ICE = 2, TIDAL_WAVE = 3, WHIRLPOOL = 4, CAUSAL_SPHERE = 5, CAUSAL_RAY = 6, CAUSAL_DOMAIN = 7, CAUSAL_ANCHOR = 8, CAUSAL_VERDICT = 9, CAUSAL_IMPACT = 10;
    public static final int THREAD_LINE=11,RUNE_MINE=12,LIVING_GROVE=13,MIRROR_WARD=14,PRISM_RAY=15,PLASMA_LANCE=16,
        THUNDERHEAD=17,CATACLYSM_FRONT=18,WINTER_ORCHARD=19,CRYSTAL_BULWARK=20,CRYSTAL_BLOOM=21,TWIN_GATE=22,
        PARALLAX=23,CROSSROADS=24,LEXICON=25,SPELLBREAK=26,MANA_CRUCIBLE=27,ASTRAL_LIBRARY=28,SPELL_RAY=29,
        BRIAR=30,RUNE_BURST=31,MIRROR_SHARD=32;
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(WildHunt.ID, Registries.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<ArtVisualEntity>> TYPE = TYPES.register("art_visual", () ->
        EntityType.Builder.<ArtVisualEntity>of(ArtVisualEntity::new, MobCategory.MISC).sized(0.1f, 0.1f)
            .clientTrackingRange(8).updateInterval(1).noSave().noSummon().build("wildhunt:art_visual"));
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<java.util.Optional<java.util.UUID>> CASTER = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.OPTIONAL_UUID);
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
        builder.define(CASTER,java.util.Optional.empty());builder.define(KIND, LASH); builder.define(LIFETIME, 16);
        builder.define(END_X, 0f); builder.define(END_Y, 0f); builder.define(END_Z, 0f);
    }
    public void updateVector(Vec3 start,Vec3 delta) {setPos(start);entityData.set(END_X,(float)delta.x);entityData.set(END_Y,(float)delta.y);entityData.set(END_Z,(float)delta.z);}
    public void setCaster(Entity caster) {entityData.set(CASTER,java.util.Optional.of(caster.getUUID()));}
    public boolean ownedBy(Entity caster) {return caster!=null && entityData.get(CASTER).filter(caster.getUUID()::equals).isPresent();}
    public int kind() { return entityData.get(KIND); }
    public int lifetime() { return entityData.get(LIFETIME); }
    public Vec3 end() { return new Vec3(entityData.get(END_X), entityData.get(END_Y), entityData.get(END_Z)); }
    @Override public void tick() { super.tick(); if (!level().isClientSide() && tickCount >= lifetime()) discard(); }
    @Override public AABB getBoundingBoxForCulling() { return new AABB(position(), position().add(end())).inflate((kind() == WHIRLPOOL || kind() == CAUSAL_SPHERE || kind()>=CAUSAL_DOMAIN) ? Math.abs(end().x) + 12 : kind() == TIDAL_WAVE ? end().y + 3 : 4); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128 * 128; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
