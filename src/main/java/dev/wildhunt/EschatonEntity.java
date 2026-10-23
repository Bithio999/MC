package dev.wildhunt;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import io.github.manasmods.tensura.entity.template.subclass.IArenaBoss;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.boss.template.BossFightInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Final Myth: native billion-EP existence, finite fuel, actual Godly skills and bounded choreography. */
public final class EschatonEntity extends Monster implements MythEncounter,IArenaBoss,GeoEntity {
    public static final double EP=1_000_000_000d,HP=1_000_000d,SHP=4_000_000d;
    private static final EntityDataAccessor<Integer> FORM=SynchedEntityData.defineId(EschatonEntity.class,EntityDataSerializers.INT),ART=SynchedEntityData.defineId(EschatonEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> FORM_START=SynchedEntityData.defineId(EschatonEntity.class,EntityDataSerializers.LONG),HIT_UNTIL=SynchedEntityData.defineId(EschatonEntity.class,EntityDataSerializers.LONG),BROKEN_UNTIL=SynchedEntityData.defineId(EschatonEntity.class,EntityDataSerializers.LONG);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent hp=bar("Eschaton · 1,000,000,000 EP · I","hp",BossEvent.BossBarColor.BLUE),
        soul=bar("Origin Sovereign · Spiritual Body","shp",BossEvent.BossBarColor.PURPLE),
        magic=bar("Genesis · Magicules","mp",BossEvent.BossBarColor.WHITE),aura=bar("Sovereignty · Aura","ap",BossEvent.BossBarColor.YELLOW);
    private final Set<ServerPlayer> watchers=new HashSet<>();
    private boolean initialized,reloading,transitioning,operatorKill,observed;
    private float previousHP;private double previousSHP;
    private BlockPos anchor;private String arena="";
    private int idle,decision=120,melee,sequence;
    private long stepReady;
    CompoundTag plan=new CompoundTag();
    public EschatonEntity(EntityType<? extends EschatonEntity> type,Level level){super(type,level);setPersistenceRequired();setNoGravity(true);moveControl=new FlyingMoveControl(this,10,true);xpReward=12_000;}
    private static ServerBossEvent bar(String title,String id,BossEvent.BossBarColor color){return new ServerBossEvent(Component.literal(title).withStyle(s->s.withInsertion("wildhunt:eschaton_"+id)),color,BossEvent.BossBarOverlay.PROGRESS);}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){
        c.add(new AnimationController<>(this,"sovereign",8,s->s.setAndContinue(RawAnimation.begin().thenLoop(isStaggered()?"animation.eschaton.stagger":getDeltaMovement().lengthSqr()>.006?"animation.eschaton.advance":"animation.myths.idle"))).triggerableAnim("unfold",RawAnimation.begin().thenPlay("animation.eschaton.unfold")));
        c.add(new AnimationController<>(this,"authority",6,s->{if(art()==0)return PlayState.STOP;return s.setAndContinue(RawAnimation.begin().thenLoop(art()==1?"animation.eschaton.edge":art()==7?"animation.eschaton.thronefall":art()==8?"animation.eschaton.apocalypse":"animation.eschaton.conjure"));}));
    }
    public static AttributeSupplier.Builder attributes(){return TensuraTamableEntity.setAttributes().add(Attributes.MAX_HEALTH,HP).add(Attributes.ATTACK_DAMAGE,2600)
        .add(Attributes.ARMOR,100).add(Attributes.ARMOR_TOUGHNESS,60).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.MOVEMENT_SPEED,.36).add(Attributes.FLYING_SPEED,.48)
        .add(Attributes.FOLLOW_RANGE,112).add(Attributes.STEP_HEIGHT,3).add(TensuraAttributes.MAX_SPIRITUAL_HEALTH,SHP).add(TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION,1000)
        // The billion EP is real, finite fuel. HP/SHP still use native Infinite Regeneration.
        .add(TensuraAttributes.AURA_REGENERATION_MULTIPLIER,0).add(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER,0).add(TensuraAttributes.PRESENCE_SENSE,10);}
    @Override public double getAttributeValue(Holder<Attribute> a){return a.equals(Attributes.MAX_HEALTH) || a.equals(TensuraAttributes.MAX_SPIRITUAL_HEALTH)?MythAttributes.value(this,a):super.getAttributeValue(a);}
    @Override protected PathNavigation createNavigation(Level l){var n=new FlyingPathNavigation(this,l);n.setCanFloat(true);return n;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(FORM,1);b.define(ART,0);b.define(FORM_START,0L);b.define(HIT_UNTIL,0L);b.define(BROKEN_UNTIL,0L);}
    @Override protected void registerGoals(){goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,64));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<Player>(this,Player.class,10,true,false,this::eligible));}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,DifficultyInstance d,MobSpawnType reason,SpawnGroupData data){var result=super.finalizeSpawn(l,d,reason,data);initializeOrigin();return result;}
    public void initializeOrigin(){
        if(initialized || level().isClientSide())return;anchor=blockPosition();RaceAPI.getRaceFrom(this).setRace(EschatonContent.RACE.get(),false);EnergyHelper.setBaseMaxEP(this,EP);
        var e=TensuraStorages.getExistenceFrom(this);e.setTrueDemonLord(true);
        for(var s:List.of(EschatonContent.DEMIURGE.get(),WildHunt.GODLY.get(),WildHunt.MYTHIC.get(MythicKind.THOTH).get()))HunterData.learnReward(this,s);
        for(String id:MythBosses.NATIVE_SKILLS){var s=SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id));if(s==null)throw new IllegalStateException("Missing Eschaton support "+id);HunterData.learnReward(this,s);}
        for(var i:SkillAPI.getSkillsFrom(this).getLearnedSkills())if(i.getMastery()>=0)i.setMastery(i.getMaxMastery());
        initialized=true;refill();activateSupport();refill();snapshot();entityData.set(FORM_START,level().getGameTime());
    }
    private void activateSupport(){
        var storage=SkillAPI.getSkillsFrom(this);
        for(String id:MythBosses.NATIVE_SKILLS){var s=SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id));if(storage.getSkill(s).isEmpty())HunterData.learnReward(this,s);var i=storage.getSkill(s).orElseThrow();i.setMastery(i.getMaxMastery());if(i.canBeToggled(this) && !i.isToggled()){i.setToggled(true);i.onToggleOn(this);i.markDirty();}}
        // Thoth supplies its real passive; Aion supplies active arts and ownership, without another barrier toggle.
        storage.getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).ifPresent(i->{if(!i.isToggled())i.onToggleOn(this);});
    }
    public int form(){return entityData.get(FORM);}
    public int art(){return entityData.get(ART);}
    public long formStarted(){return entityData.get(FORM_START);}
    void setArt(int mode){entityData.set(ART,mode);}
    public boolean isCasting(){return !plan.isEmpty();}
    public boolean isStaggered(){return entityData.get(BROKEN_UNTIL)>level().getGameTime();}
    public boolean hasDamageFeedback(){return entityData.get(HIT_UNTIL)>level().getGameTime();}
    public BlockPos anchor(){return anchor==null?blockPosition():anchor;}
    public boolean eligible(LivingEntity target){return HunterData.canHarm(this,target);}
    @Override public boolean inEncounter(LivingEntity target){
        if(target==this || target instanceof MythEncounter || target instanceof GenesisSealEntity || target.distanceToSqr(Vec3.atCenterOf(anchor()))>112*112)return false;
        if(!arena.isEmpty() && target instanceof ServerPlayer p){var fight=TensuraStorages.getBossFightHolder(level().getServer().overworld()).getBossFight(arena);return fight!=null && fight.getJoinedPlayers().contains(p.getUUID());}
        return true;
    }
    @Override public void tick(){
        if(!level().isClientSide() && observed && (getHealth()<previousHP || TensuraStorages.getExistenceFrom(this).getSpiritualHealth()<previousSHP))hitFlash();
        super.tick();if(!(level() instanceof ServerLevel level))return;initializeOrigin();
        if(reloading){EschatonRites.clear(level,this);GodlySkill.clearTransient(this);MythicArts.clearTransient(this);activateSupport();reloading=false;}
        snapshot();if(!isAlive()){EschatonRites.clear(level,this);return;}updateBars();EschatonRites.tick(level,this);if(isNoAi())return;
        var target=getTarget();if(target==null || !eligible(target)){
            setTarget(null);if(isCasting())EschatonRites.clear(level,this);if(++idle==400)reset();
            if(distanceToSqr(Vec3.atCenterOf(anchor()))>16)getNavigation().moveTo(anchor().getX()+.5,anchor().getY()+1,anchor().getZ()+.5,1);return;
        }
        idle=0;getLookControl().setLookAt(target,45,45);lookAt(target,45,45);
        if(isCasting() || isStaggered()){getNavigation().stop();return;}
        if(distanceToSqr(target)>10*10)getNavigation().moveTo(target,1.2);else getNavigation().stop();
        if(melee>0)melee--;if(distanceToSqr(target)<5.5*5.5 && melee==0){doHurtTarget(target);swing(InteractionHand.MAIN_HAND);melee=form()==3?16:24;}
        if(decision>0){decision--;return;}
        if(level.getGameTime()>=stepReady && distanceToSqr(target)>40*40 && reposition(target)){stepReady=level.getGameTime()+180;decision=16;return;}
        var skill=SkillAPI.getSkillsFrom(this).getSkill(EschatonContent.DEMIURGE.get()).orElseThrow();
        int selected=chooseArt(skill,target);if(selected<0){decision=12;return;}
        skill.onPressed(this,0,selected);decision=form()==3?18:form()==2?26:36;
        if(isCasting() && selected!=7 && sequence%3==0)auxiliary();
    }
    int chooseArt(ManasSkillInstance skill,LivingEntity target){
        var e=TensuraStorages.getExistenceFrom(this);
        int turn=sequence++;
        int preferred=e.getMagicule()<EnergyHelper.getMaxMagicule(this)*.15 && e.getAura()>=20_000_000?5:turn>0 && turn%5==0?7:new int[]{0,2,1,6,4,3,2,0}[turn%8];
        for(int mode:new int[]{preferred,7,2,1,6,0,4,3,5}){
            if((mode==3 || mode==4) && form()<2)continue;
            if(!skill.onCoolDown(mode) && e.getAura()>=DemiurgeSkill.AURA[mode] && e.getMagicule()>=DemiurgeSkill.MAGICULES[mode] && EschatonRites.canBegin(this,mode))return mode;
        }return -1;
    }
    private void auxiliary(){
        var storage=SkillAPI.getSkillsFrom(this);
        if(form()>=2){var t=storage.getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).orElseThrow();if(!t.onCoolDown(3))t.onPressed(this,0,3);else if(!t.onCoolDown(1))t.onPressed(this,0,1);}
        if(form()==3){var a=storage.getSkill(WildHunt.GODLY.get()).orElseThrow();if(!a.onCoolDown(3))a.onPressed(this,0,3);else if(!a.onCoolDown(1))a.onPressed(this,0,1);}
    }
    private boolean reposition(LivingEntity target){
        var level=(ServerLevel)level();var e=TensuraStorages.getExistenceFrom(this);if(e.getAura()<300_000)return false;
        Vec3 away=position().subtract(target.position()).multiply(1,0,1).normalize();if(away.lengthSqr()<.01)away=new Vec3(1,0,0);
        for(double angle:new double[]{0,1,-1,2,-2}){Vec3 at=target.position().add(away.yRot((float)angle).scale(16));if(!safePosition(at))continue;
            EschatonRites.flare(level,this,position(),4);teleportTo(at.x,at.y,at.z);setDeltaMovement(Vec3.ZERO);getNavigation().stop();EschatonRites.flare(level,this,at,4);e.setAura(e.getAura()-300_000);e.markDirty();return true;}
        return false;
    }
    boolean safePosition(Vec3 at){var bounds=getBoundingBox().move(at.subtract(position()));return at.distanceToSqr(Vec3.atCenterOf(anchor()))<=96*96 && level().hasChunkAt(BlockPos.containing(at)) && level().getWorldBorder().isWithinBounds(bounds) && bounds.minY>=level().getMinBuildHeight() && bounds.maxY<level().getMaxBuildHeight() && level().noCollision(this,bounds) && !level().containsAnyLiquid(bounds);}
    public void brokenSeal(int slot){
        if(!(level() instanceof ServerLevel level) || plan.isEmpty() || plan.getInt("mode")!=7 || slot<0 || slot>2 || (plan.getInt("broken")&(1<<slot))!=0)return;
        plan.putInt("broken",plan.getInt("broken")|(1<<slot));var e=TensuraStorages.getExistenceFrom(this);
        e.setMagicule(Math.max(0,e.getMagicule()-50_000_000));e.setAura(Math.max(0,e.getAura()-25_000_000));e.markDirty();
        if(plan.getInt("broken")==7){plan.putBoolean("interrupted",true);entityData.set(BROKEN_UNTIL,level.getGameTime()+160);decision=160;announce("The creation seals shatter. Eschaton's crown is broken!",0x9DF7ED);}
        EschatonRites.sync(level,this);
    }
    private void nextForm(){
        transitioning=true;try{
            var level=(ServerLevel)level();EschatonRites.clear(level,this);GodlySkill.clearTransient(this);MythicArts.clearTransient(this);getNavigation().stop();setDeltaMovement(Vec3.ZERO);
            entityData.set(FORM,form()+1);entityData.set(FORM_START,level.getGameTime());entityData.set(BROKEN_UNTIL,0L);dead=false;deathTime=0;setPose(Pose.STANDING);decision=80;melee=30;sequence=0;
            refill();activateSupport();refill();snapshot();hitFlash();triggerAnim("sovereign","unfold");
            ArtVisualEntity.spawn(level,ArtVisualEntity.ESCHATON_ASCENSION,position(),new Vec3(20,form(),0),80).setCaster(this);
            level.playSound(null,blockPosition(),SoundEvents.END_PORTAL_SPAWN,SoundSource.HOSTILE,1.8f,form()==2?.8f:.5f);
            announce(form()==2?"Eschaton: I gave the stars their names. I can take them back.":"Eschaton: Then witness the end of all myths.",form()==2?0x9DF7ED:0xFF9B99);updateBars();
        }finally{transitioning=false;}
    }
    private void refill(){setHealth(getMaxHealth());var e=TensuraStorages.getExistenceFrom(this);e.setAura(EnergyHelper.getMaxAura(this));e.setMagicule(EnergyHelper.getMaxMagicule(this));e.setSpiritualHealth(getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));e.markDirty();}
    private void reset(){EschatonRites.clear((ServerLevel)level(),this);GodlySkill.clearTransient(this);MythicArts.clearTransient(this);entityData.set(FORM,1);entityData.set(FORM_START,level().getGameTime());entityData.set(BROKEN_UNTIL,0L);decision=120;sequence=0;refill();activateSupport();snapshot();}
    private void snapshot(){previousHP=getHealth();previousSHP=TensuraStorages.getExistenceFrom(this).getSpiritualHealth();observed=true;}
    private void hitFlash(){entityData.set(HIT_UNTIL,level().getGameTime()+10);}
    void announce(String message,int color){for(var p:hp.getPlayers())p.displayClientMessage(Component.literal(message).withColor(color),false);}
    private void updateBars(){
        var e=TensuraStorages.getExistenceFrom(this);hp.setProgress((float)Math.clamp(getHealth()/getMaxHealth(),0,1));soul.setProgress((float)Math.clamp(e.getSpiritualHealth()/Math.max(1,getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)),0,1));magic.setProgress((float)Math.clamp(e.getMagicule()/Math.max(1,EnergyHelper.getMaxMagicule(this)),0,1));aura.setProgress((float)Math.clamp(e.getAura()/Math.max(1,EnergyHelper.getMaxAura(this)),0,1));
        if(tickCount%5!=0)return;String formName=switch(form()){case 1->"The First Sovereign";case 2->"World-Eater";default->"The Last Myth";};
        hp.setName(Component.literal("Eschaton · 1B EP · "+formName).withStyle(s->s.withInsertion("wildhunt:eschaton_hp")));
        String caption=isCasting()?DemiurgeSkill.NAMES[plan.getInt("mode")]:isStaggered()?"Broken Crown · vulnerable":"Sovereignty · Aura";
        if(isCasting() && plan.getInt("mode")==7)caption="End of All Myths · "+(3-Integer.bitCount(plan.getInt("broken")))+" seals · "+String.format(java.util.Locale.ROOT,"%.1fs",Math.max(0,200-(level().getGameTime()-plan.getLong("start")))/20d);
        aura.setName(Component.literal(caption).withStyle(s->s.withInsertion("wildhunt:eschaton_ap")));
        for(var p:watchers)for(var bar:bars())if(p.distanceToSqr(this)<=144*144 && (arena.isEmpty() || inEncounter(p)))bar.addPlayer(p);else bar.removePlayer(p);
    }
    private List<ServerBossEvent> bars(){return List.of(hp,soul,magic,aura);}
    @Override public boolean hurt(DamageSource source,float amount){
        if(source.getEntity() instanceof LivingEntity attacker && !inEncounter(attacker))return false;
        float before=getHealth();double beforeSoul=TensuraStorages.getExistenceFrom(this).getSpiritualHealth();int form=form();
        boolean damaged=super.hurt(source,isStaggered()?amount*1.25f:amount);
        if(!level().isClientSide()){if(getHealth()<before || TensuraStorages.getExistenceFrom(this).getSpiritualHealth()<beforeSoul || form()!=form)hitFlash();snapshot();}
        if(damaged && source.getEntity() instanceof LivingEntity attacker && eligible(attacker))setTarget(attacker);return damaged;
    }
    @Override public void die(DamageSource source){
        if(level() instanceof ServerLevel level){
            if(transitioning)return;
            if(!operatorKill && TensuraStorages.getExistenceFrom(this).getMagicule()>10 && io.github.manasmods.tensura.effect.ability.InstantRegenerationEffect.canStopDeath(source,this)){super.die(source);return;}
            if(!operatorKill && !isRemoved() && !dead && form()<3){nextForm();return;}
            var effect=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.INSTANT_REGENERATION);boolean recovering=hasEffect(effect);removeEffect(effect);super.die(source);
            if(!isAlive()){
                EschatonRites.clear(level,this);GodlySkill.clearTransient(this);MythicArts.clearTransient(this);announce("Eschaton: Let the last myth be yours.",0xE1F8FF);bars().forEach(ServerBossEvent::removeAllPlayers);
                ArtVisualEntity.spawn(level,ArtVisualEntity.ESCHATON_SHATTER,position().add(0,3,0),new Vec3(18,0,0),90);
            }else if(recovering)SkillAPI.getSkillsFrom(this).getSkill(ResourceLocation.parse("tensura:infinite_regeneration")).filter(ManasSkillInstance::isToggled).ifPresent(i->i.onToggleOn(this));
        }else super.die(source);
    }
    @Override public void kill(){operatorKill=true;try{super.kill();}finally{operatorKill=false;}}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);watchers.add(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);watchers.remove(p);bars().forEach(b->b.removePlayer(p));}
    @Override public void remove(RemovalReason reason){if(level() instanceof ServerLevel level){EschatonItems.released(level,getUUID());EschatonRites.clear(level,this);GodlySkill.clearTransient(this);MythicArts.clearTransient(this);bars().forEach(ServerBossEvent::removeAllPlayers);}super.remove(reason);}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putBoolean("origin_initialized",initialized);t.putInt("origin_form",form());t.putLong("origin_anchor",anchor().asLong());t.putString("origin_arena",arena);t.putInt("origin_idle",idle);t.putInt("origin_sequence",sequence);t.putLong("origin_step_remaining",Math.clamp(stepReady-level().getGameTime(),0,180));}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);initialized=t.getBoolean("origin_initialized");entityData.set(FORM,Math.clamp(t.getInt("origin_form"),1,3));entityData.set(FORM_START,level().getGameTime()-80);anchor=t.contains("origin_anchor")?BlockPos.of(t.getLong("origin_anchor")):blockPosition();arena=t.getString("origin_arena");idle=t.getInt("origin_idle");sequence=t.getInt("origin_sequence");stepReady=level().getGameTime()+Math.clamp(t.getLong("origin_step_remaining"),0,180);entityData.set(ART,0);entityData.set(HIT_UNTIL,0L);entityData.set(BROKEN_UNTIL,0L);plan=new CompoundTag();observed=false;reloading=true;decision=80;}
    @Override public String getBossFightId(){return arena;}
    @Override public void setBossFightId(String id){arena=id==null?"":id;}
    @Override public void onSpawned(ServerLevel level,String id,BossFightInstance fight){setBossFightId(id);anchor=fight.getCenter();}
    @Override public AABB getBoundingBoxForCulling(){return super.getBoundingBoxForCulling().inflate(9);}
}
