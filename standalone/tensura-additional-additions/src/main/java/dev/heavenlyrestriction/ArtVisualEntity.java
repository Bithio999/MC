package dev.heavenlyrestriction;

import dev.architectury.registry.registries.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoEntity;

/** Bounded cosmetic protocols. This entity cannot collide, hurt or edit the world. */
public final class ArtVisualEntity extends Entity implements GeoEntity {
    public static final int HEALING_PULSE=0,CHAINED_RUSH=1,UNBOUND_STANCE=2,UNBOUND_SLASH=3,FUSION=4,TYCHE_HOUSE=5,TYCHE_REBIRTH=6,TYCHE_DIVIDEND=7;
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(HeavenlyRestriction.ID,Registries.ENTITY_TYPE);
    public static final RegistrySupplier<EntityType<ArtVisualEntity>> TYPE=TYPES.register("art_visual",()->EntityType.Builder.<ArtVisualEntity>of(ArtVisualEntity::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).noSave().noSummon().build("heavenlyrestriction:art_visual"));
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(ArtVisualEntity.class,EntityDataSerializers.INT),LIFE=SynchedEntityData.defineId(ArtVisualEntity.class,EntityDataSerializers.INT),STYLE=SynchedEntityData.defineId(ArtVisualEntity.class,EntityDataSerializers.INT),CASTER=SynchedEntityData.defineId(ArtVisualEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> X=SynchedEntityData.defineId(ArtVisualEntity.class,EntityDataSerializers.FLOAT),Y=SynchedEntityData.defineId(ArtVisualEntity.class,EntityDataSerializers.FLOAT),Z=SynchedEntityData.defineId(ArtVisualEntity.class,EntityDataSerializers.FLOAT);
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache cache=software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    public ArtVisualEntity(EntityType<? extends ArtVisualEntity> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    public static void register(){TYPES.register();}
    public static ArtVisualEntity spawn(Level level,int kind,Vec3 start,Vec3 delta,int ticks){var e=new ArtVisualEntity(TYPE.get(),level);e.setPos(start);e.entityData.set(KIND,kind);e.entityData.set(LIFE,ticks);e.entityData.set(X,(float)delta.x);e.entityData.set(Y,(float)delta.y);e.entityData.set(Z,(float)delta.z);level.addFreshEntity(e);return e;}
    public int kind(){return entityData.get(KIND);}public int lifetime(){return entityData.get(LIFE);}public int style(){return entityData.get(STYLE);}
    public ArtVisualEntity style(int style){entityData.set(STYLE,Math.clamp(style,0,2));return this;}
    public Vec3 end(){return new Vec3(entityData.get(X),entityData.get(Y),entityData.get(Z));}
    public void setCaster(Entity e){entityData.set(CASTER,e.getId());}public Entity caster(){return level().getEntity(entityData.get(CASTER));}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(KIND,0);b.define(LIFE,16);b.define(STYLE,0);b.define(CASTER,-1);b.define(X,0f);b.define(Y,0f);b.define(Z,0f);}
    @Override public void tick(){super.tick();if((kind()==FUSION || kind()>=TYCHE_HOUSE) && caster()!=null)setPos(caster().position());if(!level().isClientSide() && (tickCount>=lifetime() || kind()==TYCHE_HOUSE && (!(caster() instanceof LivingEntity owner) || !dev.heavenlyrestriction.tyche.Tyche.house(owner))))discard();}
    @Override public AABB getBoundingBoxForCulling(){return new AABB(position(),position().add(end())).inflate(Math.max(4,Math.abs(end().x)));}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<128*128;}
    @Override protected void readAdditionalSaveData(CompoundTag tag){}@Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar c){c.add(new software.bernie.geckolib.animation.AnimationController<>(this,"rig",0,s->s.setAndContinue(software.bernie.geckolib.animation.RawAnimation.begin().thenPlay("animation.restriction.appear").thenLoop("animation.restriction.idle"))));}
}
