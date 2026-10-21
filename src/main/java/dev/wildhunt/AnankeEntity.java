package dev.wildhunt;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.entity.template.TensuraTamableEntity;
import io.github.manasmods.tensura.entity.template.subclass.IArenaBoss;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.boss.template.BossFightInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.BlockPos;
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

/** Thirty-million-EP myth. Native resources/skills; readable server-authoritative encounter choreography. */
public final class AnankeEntity extends Monster implements IArenaBoss {
    public static final double EP=30_000_000,HP=48_000,SHP=120_000;
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(AnankeEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RITE=SynchedEntityData.defineId(AnankeEntity.class,EntityDataSerializers.INT);
    private final ServerBossEvent healthBar=new ServerBossEvent(Component.literal("Ananke · 30M EP · I").withStyle(s->s.withInsertion("wildhunt:ananke_hp")),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.NOTCHED_20);
    private final ServerBossEvent soulBar=new ServerBossEvent(Component.literal("Ananke · Spiritual Body").withStyle(s->s.withInsertion("wildhunt:ananke_shp")),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    private final Set<ServerPlayer> watchers=new HashSet<>();
    private boolean initialized,loadCleanup;
    private int supportVersion;
    private long shiftReadyAt;
    private float previousCastPose,castPose;
    private BlockPos anchor;
    private String bossFightId="";
    private int idleTicks,decisionDelay=90,meleeDelay,sequence;
    CompoundTag rite=new CompoundTag();
    public AnankeEntity(EntityType<? extends AnankeEntity> type,Level level){super(type,level);setPersistenceRequired();setNoGravity(true);moveControl=new FlyingMoveControl(this,12,true);xpReward=1500;}
    public static AttributeSupplier.Builder attributes(){return TensuraTamableEntity.setAttributes().add(Attributes.MAX_HEALTH,HP).add(Attributes.ATTACK_DAMAGE,180)
        .add(Attributes.ARMOR,40).add(Attributes.ARMOR_TOUGHNESS,20).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FLYING_SPEED,.35)
        .add(Attributes.FOLLOW_RANGE,64).add(Attributes.KNOCKBACK_RESISTANCE,.9).add(Attributes.STEP_HEIGHT,2)
        .add(TensuraAttributes.MAX_SPIRITUAL_HEALTH,SHP).add(TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION,40)
        .add(TensuraAttributes.AURA_REGENERATION_MULTIPLIER,4).add(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER,4).add(TensuraAttributes.PRESENCE_SENSE,7);}
    /** Keep the encounter's real HP above the native 10K range without changing any global attribute bound.
     * Native getMaxHealth(), healing, saves and client bossbars use this entity-local accessor.
     * Preserve vanilla modifier order, including external health debuffs. */
    @Override public double getAttributeValue(net.minecraft.core.Holder<Attribute> attribute){
        if(!attribute.equals(Attributes.MAX_HEALTH))return super.getAttributeValue(attribute);
        var instance=getAttribute(attribute);if(instance==null)return super.getAttributeValue(attribute);
        double base=instance.getBaseValue();
        for(var modifier:instance.getModifiers())if(modifier.operation()==AttributeModifier.Operation.ADD_VALUE)base+=modifier.amount();
        double value=base;
        for(var modifier:instance.getModifiers())if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)value+=base*modifier.amount();
        for(var modifier:instance.getModifiers())if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)value*=1+modifier.amount();
        return Double.isNaN(value)?1:Math.clamp(value,1,1_000_000);
    }
    @Override protected PathNavigation createNavigation(Level level){var nav=new FlyingPathNavigation(this,level);nav.setCanFloat(true);return nav;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(PHASE,1);b.define(RITE,0);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,32));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<Player>(this,Player.class,10,true,false,e->eligible(e)));
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,DifficultyInstance d,MobSpawnType reason,SpawnGroupData data){var result=super.finalizeSpawn(l,d,reason,data);initializeMyth();return result;}
    public void initializeMyth(){
        if(initialized || level().isClientSide())return;anchor=blockPosition();
        RaceAPI.getRaceFrom(this).setRace(MythBosses.RACE.get(),false);
        EnergyHelper.setBaseMaxEP(this,EP);
        var energy=TensuraStorages.getExistenceFrom(this);energy.setTrueDemonLord(true);energy.setAura(EnergyHelper.getMaxAura(this));energy.setMagicule(EnergyHelper.getMaxMagicule(this));energy.setSpiritualHealth(SHP);energy.markDirty();
        HunterData.learnReward(this,MythBosses.MOIRA.get());HunterData.learnReward(this,WildHunt.MYTHIC.get(MythicKind.THOTH).get());
        for(var id:MythBosses.NATIVE_SKILLS){var skill=SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id));if(skill==null)throw new IllegalStateException("Missing Ananke skill "+id);HunterData.learnReward(this,skill);}
        var owned=SkillAPI.getSkillsFrom(this);
        owned.getSkill(MythBosses.MOIRA.get()).ifPresent(i->i.setMastery(i.getMaxMastery()));
        owned.getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).ifPresent(i->i.setMastery(i.getMaxMastery()));
        for(var id:MythBosses.NATIVE_SKILLS)owned.getSkill(ResourceLocation.parse(id)).ifPresent(i->i.setMastery(i.getMaxMastery()));
        initialized=true;setHealth(getMaxHealth());enableSupport();supportVersion=2;
    }
    /** The existence profile supplies stats only. Always toggle the actual stored instance. */
    private void enableSupport(){
        var skills=SkillAPI.getSkillsFrom(this);
        for(var id:MythBosses.NATIVE_SKILLS){
            var skill=SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id));
            if(skill==null)throw new IllegalStateException("Missing Ananke support "+id);
            if(skills.getSkill(skill).isEmpty())HunterData.learnReward(this,skill);
            var i=skills.getSkill(skill).orElseThrow();i.setMastery(i.getMaxMastery());
            if(!i.isToggled() && i.canBeToggled(this)){i.setToggled(true);i.onToggleOn(this);i.markDirty();}
        }
        skills.getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).ifPresent(i->{if(!i.isToggled())i.onToggleOn(this);});
    }
    private void restoreSupport(){
        if(supportVersion<1){
            // 0.25's profile toggled unstored instances. Remove their native, keyed
            // side effects even where the stored flag is false, then activate once.
            for(var id:MythBosses.NATIVE_SKILLS)SkillAPI.getSkillsFrom(this).getSkill(ResourceLocation.parse(id)).ifPresent(i->{i.onToggleOff(this);i.setToggled(false);});
        }
        if(supportVersion<2){
            // Replace the former gradual regeneration on existing bosses without
            // refilling native resources or reviving a removed recovery channel.
            SkillAPI.getSkillsFrom(this).forgetSkill(ResourceLocation.parse("tensura:self_regeneration"),null);
            supportVersion=2;
        }
        enableSupport();
    }
    public float castPose(float partial){return net.minecraft.util.Mth.lerp(partial,previousCastPose,castPose);}
    public int phase(){return entityData.get(PHASE);}
    public int riteMode(){return entityData.get(RITE);}
    public boolean isCasting(){return !rite.isEmpty();}
    public void setRite(int value){entityData.set(RITE,value);}
    public BlockPos anchor(){return anchor==null?blockPosition():anchor;}
    public boolean eligible(LivingEntity target){return HunterData.canHarm(this,target);}
    /** Shared by every Myths art, including Thoth; arena bystanders must be safe from auxiliary spells too. */
    public boolean inEncounter(LivingEntity target){
        if(target==this || target instanceof AnankeEntity || target.distanceToSqr(Vec3.atCenterOf(anchor()))>72*72)return false;
        if(!bossFightId.isEmpty() && target instanceof ServerPlayer p){var fight=TensuraStorages.getBossFightHolder(level().getServer().overworld()).getBossFight(bossFightId);return fight!=null && fight.getJoinedPlayers().contains(p.getUUID());}
        return true;
    }
    @Override public void tick(){
        if(!level().isClientSide() && !isNoAi() && isAlive())advancePhase();
        super.tick();if(level().isClientSide()){previousCastPose=castPose;castPose=net.minecraft.util.Mth.approach(castPose,riteMode()==0?0:1,.125f);return;}var level=(ServerLevel)level();initializeMyth();
        if(loadCleanup){MythicArts.clearTransient(this);loadCleanup=false;restoreSupport();}
        if(!isAlive()){AnankeRites.clear(level,this);return;}
        updateBars();AnankeRites.tick(level,this);
        if(isNoAi())return;
        var target=getTarget();
        if(target==null || !eligible(target)){
            setTarget(null);if(isCasting()){AnankeRites.clear(level,this);}
            if(++idleTicks==200)resetEncounter();
            if(distanceToSqr(Vec3.atCenterOf(anchor()))>9)getNavigation().moveTo(anchor().getX()+.5,anchor().getY()+.2,anchor().getZ()+.5,1);
            return;
        }
        idleTicks=0;getLookControl().setLookAt(target,40,40);lookAt(target,30,30);
        if(isCasting()){getNavigation().stop();return;}
        if(distanceToSqr(target)>7*7)getNavigation().moveTo(target,1.15);else getNavigation().stop();
        if(meleeDelay>0)meleeDelay--;
        if(distanceToSqr(target)<3.5*3.5 && meleeDelay==0){doHurtTarget(target);swing(InteractionHand.MAIN_HAND);meleeDelay=30;}
        if(decisionDelay>0){decisionDelay--;return;}
        var moira=SkillAPI.getSkillsFrom(this).getSkill(MythBosses.MOIRA.get()).orElseThrow();
        if(level.getGameTime()>=shiftReadyAt && AnankeTactics.reposition(this,target)){
            shiftReadyAt=level.getGameTime()+200;decisionDelay=20;return;
        }
        int preferred=phase()==3 && !moira.onCoolDown(3)?3:new int[]{0,1,0,2,1,0}[sequence++%6];
        int mode=AnankeTactics.chooseRite(this,moira,target,preferred);
        if(mode<0){decisionDelay=10;return;}
        moira.onPressed(this,0,mode);decisionDelay=phase()==3?18:phase()==2?28:38;
        var thoth=SkillAPI.getSkillsFrom(this).getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).orElseThrow();
        if(phase()>=2 && !thoth.onCoolDown(3))thoth.onPressed(this,0,3);
        else if(phase()>=2 && sequence%3==0 && !thoth.onCoolDown(1))thoth.onPressed(this,0,1);
    }
    /** Observe wounds before native regeneration can heal them in super.tick(). */
    private void advancePhase(){
        var level=(ServerLevel)level();
        int reached=getHealth()<=getMaxHealth()*.3?3:getHealth()<=getMaxHealth()*.65?2:1;
        if(reached>phase()){
            entityData.set(PHASE,reached);decisionDelay=Math.max(decisionDelay,45);
            ArtVisualEntity.spawn(level,ArtVisualEntity.ANANKE_ASCENSION,position(),new Vec3(9,reached,0),45);
            level.playSound(null,blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.HOSTILE,2,.6f+reached*.15f);
            for(var p:healthBar.getPlayers())p.displayClientMessage(Component.literal(reached==2?"Ananke: The unwritten crown opens.":"Ananke: The Last Dawn approaches. Find refuge in the gold ring.").withColor(0xFFD386),false);
        }
    }
    private void resetEncounter(){
        AnankeRites.clear((ServerLevel)level(),this);MythicArts.clearTransient(this);entityData.set(PHASE,1);sequence=0;decisionDelay=90;
        setHealth(getMaxHealth());var e=TensuraStorages.getExistenceFrom(this);e.setAura(EnergyHelper.getMaxAura(this));e.setMagicule(EnergyHelper.getMaxMagicule(this));e.setSpiritualHealth(getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));e.markDirty();enableSupport();
    }
    private void updateBars(){
        healthBar.setProgress(Math.clamp(getHealth()/getMaxHealth(),0,1));var energy=TensuraStorages.getExistenceFrom(this);
        soulBar.setProgress((float)Math.clamp(energy.getSpiritualHealth()/Math.max(1,getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)),0,1));
        if(tickCount%20==0){
            healthBar.setName(Component.literal(riteMode()==4?"Last Dawn: refuge 7–12m":"Ananke · 30M EP · "+switch(phase()){case 1->"I";case 2->"II";default->"III";}).withStyle(s->s.withInsertion("wildhunt:ananke_hp")));
            for(var p:watchers)if(p.distanceToSqr(this)<=80*80){healthBar.addPlayer(p);soulBar.addPlayer(p);}else{healthBar.removePlayer(p);soulBar.removePlayer(p);}
        }
    }
    @Override public boolean hurt(DamageSource source,float amount){boolean hit=super.hurt(source,amount);if(hit && source.getEntity() instanceof LivingEntity attacker && eligible(attacker))setTarget(attacker);return hit;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);watchers.add(p);if(p.distanceToSqr(this)<=80*80){healthBar.addPlayer(p);soulBar.addPlayer(p);}}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);watchers.remove(p);healthBar.removePlayer(p);soulBar.removePlayer(p);}
    @Override public void die(DamageSource source){if(level() instanceof ServerLevel level){AnankeRites.clear(level,this);MythicArts.clearTransient(this);healthBar.removeAllPlayers();soulBar.removeAllPlayers();}super.die(source);}
    @Override public void remove(RemovalReason reason){if(!level().isClientSide()){AnankeRites.clear((ServerLevel)level(),this);MythicArts.clearTransient(this);healthBar.removeAllPlayers();soulBar.removeAllPlayers();}super.remove(reason);}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putBoolean("myth_initialized",initialized);t.putInt("myth_support_version",supportVersion);t.putLong("myth_shift_remaining",Math.clamp(shiftReadyAt-level().getGameTime(),0,200));t.putInt("myth_phase",phase());t.putLong("myth_anchor",anchor().asLong());t.putInt("myth_idle",idleTicks);t.putString("myth_arena",bossFightId);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);initialized=t.getBoolean("myth_initialized");supportVersion=t.getInt("myth_support_version");shiftReadyAt=level().getGameTime()+Math.clamp(t.getLong("myth_shift_remaining"),0,200);entityData.set(PHASE,Math.clamp(t.getInt("myth_phase"),1,3));anchor=t.contains("myth_anchor")?BlockPos.of(t.getLong("myth_anchor")):blockPosition();idleTicks=t.getInt("myth_idle");bossFightId=t.getString("myth_arena");rite=new CompoundTag();setRite(0);loadCleanup=true;}
    @Override public String getBossFightId(){return bossFightId;}
    @Override public void setBossFightId(String id){bossFightId=id==null?"":id;}
    @Override public void onSpawned(ServerLevel level,String id,BossFightInstance fight){setBossFightId(id);anchor=fight.getCenter();}
    @Override public AABB getBoundingBoxForCulling(){return super.getBoundingBoxForCulling().inflate(4);}
}
