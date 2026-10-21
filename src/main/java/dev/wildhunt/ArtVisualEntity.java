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
    private Vec3 interpolationTarget=Vec3.ZERO, previousEnd=Vec3.ZERO, smoothedEnd=Vec3.ZERO;
    private int interpolationSteps;
    public static final int LASH = 0, CRYSTAL = 1, FALLING_ICE = 2, TIDAL_WAVE = 3, WHIRLPOOL = 4, CAUSAL_SPHERE = 5, CAUSAL_RAY = 6, CAUSAL_DOMAIN = 7, CAUSAL_ANCHOR = 8, CAUSAL_VERDICT = 9, CAUSAL_IMPACT = 10;
    public static final int THREAD_LINE=11,RUNE_MINE=12,LIVING_GROVE=13,MIRROR_WARD=14,PRISM_RAY=15,PLASMA_LANCE=16,
        THUNDERHEAD=17,CATACLYSM_FRONT=18,WINTER_ORCHARD=19,CRYSTAL_BULWARK=20,CRYSTAL_BLOOM=21,TWIN_GATE=22,
        PARALLAX=23,CROSSROADS=24,LEXICON=25,SPELLBREAK=26,MANA_CRUCIBLE=27,ASTRAL_LIBRARY=28,SPELL_RAY=29,
        BRIAR=30,RUNE_BURST=31,MIRROR_SHARD=32,RAILBOLT=33,THUNDER_STEP=34,ELEMENT_IMPACT=35,OIL_POOL=36,HEALING_PULSE=37,GATE_TRANSIT=38,
        FATE_ANCHOR=39,FATE_THREAD=40,LOOM_TRANSIT=41,CHECKMATE_EDGE=42,MIRRORSEA=43,REFRACTED_HARPOON=44,SOVEREIGN_BREAKER=45,WATER_REFRACTION=46,
        PERENNIAL_SEED=47,REBIRTH_BLOOM=48,SKILL_APPRAISAL=49,ARTISAN_THREAD=50,COUNTERMAND=51,GRAND_SYNTHESIS=52,RECONSTRUCTED_BLADE=53,RECONSTRUCTED_LANCE=54,RECONSTRUCTED_CROWN=55;
    public static final int ANANKE_FATEBREAKER=56,ANANKE_STARFALL=57,ANANKE_PROCESSION=58,ANANKE_LAST_DAWN=59,ANANKE_ASCENSION=60,ANANKE_SHIFT=62;
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(WildHunt.ID, Registries.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<ArtVisualEntity>> TYPE = TYPES.register("art_visual", () ->
        EntityType.Builder.<ArtVisualEntity>of(ArtVisualEntity::new, MobCategory.MISC).sized(0.1f, 0.1f)
            .clientTrackingRange(8).updateInterval(1).noSave().noSummon().build("wildhunt:art_visual"));
    // Boss warnings can be separated by the full encounter diameter. Keep their
    // network range independent from the ordinary skill visual budget.
    public static final RegistrySupplier<EntityType<ArtVisualEntity>> MYTH_TYPE=TYPES.register("myth_visual",()->
        EntityType.Builder.<ArtVisualEntity>of(ArtVisualEntity::new,MobCategory.MISC).sized(.1f,.1f)
            .clientTrackingRange(12).updateInterval(1).noSave().noSummon().build("wildhunt:myth_visual"));
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<java.util.Optional<java.util.UUID>> CASTER = SynchedEntityData.defineId(ArtVisualEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<CompoundTag> BARRAGE=SynchedEntityData.defineId(ArtVisualEntity.class,EntityDataSerializers.COMPOUND_TAG);
    public void barrage(CompoundTag state){entityData.set(BARRAGE,state.copy());}
    public CompoundTag barrage(){return entityData.get(BARRAGE);}
    public float barrageAge(float partial){return (float)Math.max(0,level().getGameTime()-barrage().getLong("start"))+partial;}
    public static void register() { TYPES.register(); }
    public ArtVisualEntity(EntityType<? extends ArtVisualEntity> type, Level level) {
        super(type, level); noPhysics = true; setNoGravity(true);
    }
    public static ArtVisualEntity spawn(Level level, int kind, Vec3 start, Vec3 delta, int ticks) {
        var entity = new ArtVisualEntity(kind>=ANANKE_FATEBREAKER && kind<=ANANKE_SHIFT?MYTH_TYPE.get():TYPE.get(), level); entity.setPos(start);
        entity.entityData.set(KIND, kind); entity.entityData.set(LIFETIME, ticks);
        entity.entityData.set(END_X, (float)delta.x); entity.entityData.set(END_Y, (float)delta.y); entity.entityData.set(END_Z, (float)delta.z);
        level.addFreshEntity(entity); return entity;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BARRAGE,new CompoundTag());builder.define(CASTER,java.util.Optional.empty());builder.define(KIND, LASH); builder.define(LIFETIME, 16);builder.define(STYLE,0);builder.define(CASTER_ID,-1);
        builder.define(END_X, 0f); builder.define(END_Y, 0f); builder.define(END_Z, 0f);
    }
    public void updateVector(Vec3 start,Vec3 delta) {setPos(start);entityData.set(END_X,(float)delta.x);entityData.set(END_Y,(float)delta.y);entityData.set(END_Z,(float)delta.z);}
    public void setCaster(Entity caster) {entityData.set(CASTER_ID,caster.getId());entityData.set(CASTER,java.util.Optional.of(caster.getUUID()));}
    public boolean ownedBy(Entity caster) {return caster!=null && entityData.get(CASTER).filter(caster.getUUID()::equals).isPresent();}
    public Entity caster() {var e=level().getEntity(entityData.get(CASTER_ID));return e!=null && ownedBy(e)?e:entityData.get(CASTER).map(id->level().getPlayerByUUID(id)).orElse(null);}
    public void remaining(int ticks){entityData.set(LIFETIME,tickCount+Math.max(1,ticks));}
    public int kind() { return entityData.get(KIND); }
    public int lifetime() { return entityData.get(LIFETIME); }
    public int style(){return entityData.get(STYLE);}
    public ArtVisualEntity style(int style){entityData.set(STYLE,Math.clamp(style,0,kind()==GRAND_SYNTHESIS?26:kind()==SPELL_RAY?ThothElement.COUNT-1:kind()==ASTRAL_LIBRARY?ThothElement.LIBRARY_DURATION/ThothElement.LIBRARY_INTERVAL:2));return this;}
    public Vec3 end() { return new Vec3(entityData.get(END_X), entityData.get(END_Y), entityData.get(END_Z)); }
    public Vec3 end(float partial) {return tickCount<=1?end():previousEnd.lerp(smoothedEnd,partial);}
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps) {
        interpolationTarget=new Vec3(x,y,z);interpolationSteps=Math.clamp(steps,1,3);
    }
    @Override public void tick() {
        previousEnd=smoothedEnd;smoothedEnd=tickCount<=1?end():smoothedEnd.lerp(end(),.65);super.tick();
        if(level().isClientSide() && interpolationSteps>0){setPos(position().lerp(interpolationTarget,1d/interpolationSteps));interpolationSteps--;}
        if(!level().isClientSide() && (kind()==PERENNIAL_SEED || kind()==COUNTERMAND) && tickCount>2){
            if(caster() instanceof net.minecraft.world.entity.LivingEntity target){
                var lease=target.getPersistentData().getCompound("wildhunt").getCompound(kind()==PERENNIAL_SEED?"perennial_seed":"lugh_seal");
                if(!lease.hasUUID("visual") || !lease.getUUID("visual").equals(getUUID()) || lease.getLong("until")<=level().getGameTime())discard();
            }else discard();
        }
        if(!level().isClientSide() && kind()==CAUSAL_VERDICT && !barrage().isEmpty()){
            if(barrageAge(0)>=lifetime())discard();
            if(tickCount>3 && caster() instanceof net.minecraft.world.entity.LivingEntity owner){
                var skill=io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(owner).getSkill(WildHunt.GODLY.get());
                if(!owner.isAlive() || skill.isEmpty() || !skill.get().getOrCreateTag().hasUUID("verdict_visual") || !getUUID().equals(skill.get().getOrCreateTag().getUUID("verdict_visual")))discard();
            }
        }else if (!level().isClientSide() && tickCount >= lifetime()) discard();
    }
    @Override public AABB getBoundingBoxForCulling() {
        // Each star warning can be far from the caster. Cull by its actual saved
        // placement and sky lance, not only the encounter's origin.
        if(kind()==ANANKE_STARFALL && !barrage().isEmpty()){
            var box=new AABB(position(),position());var plan=barrage();
            for(int n=0;n<plan.getInt("warned");n++){var at=position().add(AnankeRites.star(plan,n));box=box.minmax(new AABB(at,at.add(0,20,0)).inflate(4));}
            return box;
        }
        if(kind()==CAUSAL_VERDICT && !barrage().isEmpty())return new AABB(position(),position().add(new Vec3(barrage().getDouble("aim_x"),barrage().getDouble("aim_y"),barrage().getDouble("aim_z")))).inflate(24);return new AABB(position(), position().add(end())).inflate((kind() == WHIRLPOOL || kind() == CAUSAL_SPHERE || kind()>=CAUSAL_DOMAIN) ? Math.abs(end().x) + 12 : kind() == TIDAL_WAVE ? end().y + 3 : 4); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < (kind()==ANANKE_STARFALL ? 192 * 192 : 128 * 128); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
