package dev.wildhunt;

import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.UUID;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Destructible, encounter-owned weak point. No loot, natural spawn, saved orphan or boss progression tag. */
public final class GenesisSealEntity extends Mob implements GeoEntity {
    public static final double HP=120_000;
    private static final EntityDataAccessor<Integer> SLOT=SynchedEntityData.defineId(GenesisSealEntity.class,EntityDataSerializers.INT);
    private UUID owner;private long cast;
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    public GenesisSealEntity(EntityType<? extends GenesisSealEntity> type,Level l){super(type,l);setNoGravity(true);setNoAi(true);setPersistenceRequired();xpReward=0;}
    public static AttributeSupplier.Builder attributes(){return TensuraTamableEntity.setAttributes().add(Attributes.MAX_HEALTH,HP).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.MOVEMENT_SPEED,0);}
    @Override public double getAttributeValue(net.minecraft.core.Holder<Attribute> a){return a.equals(Attributes.MAX_HEALTH)?MythAttributes.maximumHealth(this):super.getAttributeValue(a);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SLOT,0);}
    public int slot(){return entityData.get(SLOT);}
    public boolean ownedBy(EschatonEntity boss){return owner!=null && owner.equals(boss.getUUID());}
    public EschatonEntity owner(){return level() instanceof ServerLevel l && owner!=null && l.getEntity(owner) instanceof EschatonEntity b?b:null;}
    void bind(EschatonEntity boss,int slot,long token){owner=boss.getUUID();cast=token;entityData.set(SLOT,slot);EnergyHelper.setBaseMaxEP(this,0);setHealth(getMaxHealth());}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"seal",0,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.myths.idle"))));}
    @Override public void tick(){super.tick();setDeltaMovement(Vec3.ZERO);if(level().isClientSide())return;if(tickCount%5==0){setCustomName(net.minecraft.network.chat.Component.literal("Genesis Seal "+(slot()+1)+" · "+Math.round(getHealth()/getMaxHealth()*100)+"%"));setCustomNameVisible(true);}var b=owner();if(b==null || !b.isAlive() || !b.isCasting() || b.plan.getInt("mode")!=7 || b.plan.getLong("start")!=cast || tickCount>230)discard();}
    @Override public boolean hurt(DamageSource source,float amount){var b=owner();if(b==null)return false;if(source.getEntity() instanceof LivingEntity attacker){if(attacker==b || !b.inEncounter(attacker))return false;}else if(!source.is(DamageTypes.GENERIC_KILL))return false;return super.hurt(source,amount);}
    @Override public void die(DamageSource source){super.die(source);if(!level().isClientSide() && !isAlive()){var b=owner();if(b!=null && b.plan.getLong("start")==cast){b.brokenSeal(slot());ArtVisualEntity.spawn(level(),ArtVisualEntity.ESCHATON_SHATTER,position().add(0,1,0),new Vec3(5,slot(),0),45).setCaster(b);}discard();}}
    @Override protected void dropAllDeathLoot(ServerLevel level,DamageSource source){}
    @Override public boolean causeFallDamage(float d,float m,DamageSource s){return false;}
    @Override public boolean removeWhenFarAway(double d){return false;}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);owner=null;}
    @Override public AABB getBoundingBoxForCulling(){return super.getBoundingBoxForCulling().inflate(2);}
}
