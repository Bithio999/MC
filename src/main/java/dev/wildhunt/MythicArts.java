package dev.wildhunt;

import dev.wildhunt.myths.MythsTiers;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.entity.variant.MagicCircleVariant;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Bounded world effects. No terrain edits, passive stacks or EP damage multipliers. */
public final class MythicArts {
    private MythicArts(){}
    private static final List<String> VISUALS=List.of("tether_visual","mine_visual","grove_visual","aegis_visual","thunder_visual","orchard_visual","gate_a_visual","gate_b_visual","library_visual");
    private static long now(LivingEntity e){return e.level().getGameTime();}
    private static boolean live(CompoundTag t,String key,long time){return t.getLong(key+"_until")>time;}
    static Vec3 location(ServerLevel l,LivingEntity e,double range){var target=ApexArts.target(l,e,range);return target==null?ApexArts.cursorPlacement(l,e,range):target.position();}
    static boolean canCast(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e,int mode){
        var t=i.getOrCreateTag();long time=now(e);
        return switch(s.kind){
            case THREADWEAVER -> mode==0?ApexArts.target(l,e,28)!=null:t.hasUUID("tether_target") && live(t,"tether",time) && l.getEntity(t.getUUID("tether_target")) instanceof LivingEntity target && HunterData.canHarm(e,target) && e.distanceToSqr(target)<=40*40 && e.hasLineOfSight(target);
            case RUNE_SMITH -> mode==0 || live(t,"mine",time) && e.distanceToSqr(ApexArts.point(t,"mine"))<=48*48;
            case JANUS -> mode!=0 || (!live(t,"gates",time)?safeCursor(l,e,48).distanceToSqr(e.position())>=4:Math.min(e.distanceToSqr(ApexArts.point(t,"gate_a")),e.distanceToSqr(ApexArts.point(t,"gate_b")))<=16 && safe(l,e,gateDestination(t,e)));
            case THOTH -> mode!=2 || ManaCrucible.canConvert(e);
            case LUGH -> LughArts.canCast(l,i,e,mode);
            case ANANSI,AEGIR -> SovereignArts.canCast(s,l,i,e,mode);
            default -> true;
        };
    }
    static void cast(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e,int mode){
        var t=i.getOrCreateTag();long time=now(e);t.putString("dimension",l.dimension().location().toString());
        switch(s.kind){
            case THREADWEAVER -> {
                if(mode==0){
                    var target=ApexArts.target(l,e,28);t.putUUID("tether_target",target.getUUID());ApexArts.point(t,"tether_anchor",target.position());t.putLong("tether_until",time+120);
                    visual(l,t,"tether_visual",ArtVisualEntity.THREAD_LINE,e.getEyePosition(),target.getEyePosition().subtract(e.getEyePosition()),120);
                    hit(s,i,e,target,320,0,TensuraDamageTypes.SPACE_ELEMENTAL);
                } else {
                    var target=(LivingEntity)l.getEntity(t.getUUID("tether_target"));Vec3 center=target.position();
                    area(s,l,i,e,center,4,550,1,TensuraDamageTypes.SPACE_ELEMENTAL,6);endVisual(l,t,"tether_visual");t.remove("tether_until");t.remove("tether_target");
                    ArtVisualEntity.spawn(l,ArtVisualEntity.PARALLAX,center,new Vec3(4,0,0),18).style(1);
                }
            }
            case RUNE_SMITH -> {
                if(mode==0){Vec3 p=location(l,e,28);ApexArts.point(t,"mine",p);t.putLong("mine_until",time+300);t.putLong("mine_armed",time+20);visual(l,t,"mine_visual",ArtVisualEntity.RUNE_MINE,p.add(0,.1,0),new Vec3(3,0,0),300);}
                else detonate(s,l,i,e,true);
            }
            case VERDANT_KEEPER -> {
                if(mode==0){Vec3 p=location(l,e,24);field(l,t,"grove",p,160,ArtVisualEntity.LIVING_GROVE,7);}
                else {
                    for(var target:ApexArts.enemies(l,e,e.position(),12).stream().filter(v->v.getEyePosition().subtract(e.getEyePosition()).normalize().dot(e.getLookAngle())>.4).limit(6).toList()){
                        if(hit(s,i,e,target,300,1,TensuraDamageTypes.EARTH_ELEMENTAL))effect(s,i,e,target,TensuraMobEffects.WEBBED,70,0,1);
                        ArtVisualEntity.spawn(l,ArtVisualEntity.BRIAR,target.position(),new Vec3(1.4,2,0),28);
                    }
                }
            }
            case MIRRORBOUND -> {
                if(mode==0){
                    var first=ApexArts.target(l,e,32);Vec3 end=first==null?ApexArts.cursorPlacement(l,e,32):first.getEyePosition();
                    ArtVisualEntity.spawn(l,ArtVisualEntity.PRISM_RAY,e.getEyePosition(),end.subtract(e.getEyePosition()),12);
                    if(first!=null){hit(s,i,e,first,320,0,TensuraDamageTypes.LIGHT_ELEMENTAL);
                        for(var target:ApexArts.enemies(l,e,first.position(),8).stream().filter(v->v!=first && first.hasLineOfSight(v)).limit(2).toList()){
                            ArtVisualEntity.spawn(l,ArtVisualEntity.PRISM_RAY,first.getEyePosition(),target.getEyePosition().subtract(first.getEyePosition()),12);hit(s,i,e,target,180,0,TensuraDamageTypes.LIGHT_ELEMENTAL);
                        }
                    }
                }else {t.putLong("aegis_until",time+120);visual(l,t,"aegis_visual",ArtVisualEntity.MIRROR_WARD,e.position().add(0,1,0),new Vec3(3.5,0,0),120);if(l.getEntity(t.getUUID("aegis_visual")) instanceof ArtVisualEntity v)v.setCaster(e);}
            }
            case VRITRA -> {
                if(mode==0){
                    Vec3 from=e.getEyePosition(),end=rayEnd(l,e,64);var struck=new HashSet<UUID>();
                    ArtVisualEntity.spawn(l,ArtVisualEntity.PLASMA_LANCE,from,end.subtract(from),18);
                    var targets=new ArrayList<>(lineTargets(l,e,from,end,1.1,8));
                    if(live(t,"thunder",time)){
                        Vec3 origin=ApexArts.point(t,"thunder").add(0,5,0);
                        if(origin.distanceToSqr(e.position())<=64*64){
                            Vec3 routed=l.clip(new net.minecraft.world.level.ClipContext(origin,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,e)).getLocation();
                            ArtVisualEntity.spawn(l,ArtVisualEntity.PLASMA_LANCE,origin,routed.subtract(origin),18);
                            targets.addAll(lineTargets(l,e,origin,routed,1.1,8));
                        }
                    }
                    for(var target:targets)if(struck.size()<8 && struck.add(target.getUUID())){
                        hit(s,i,e,target,1500,0,TensuraDamageTypes.LIGHTNING_ELEMENTAL);
                        schedule(t,target.position(),time+16,3.5,850,0,"fire");
                        ArtVisualEntity.spawn(l,ArtVisualEntity.RUNE_MINE,target.position(),new Vec3(3.5,1,0),16);
                    }
                }else if(mode==1){Vec3 p=location(l,e,48);field(l,t,"thunder",p,160,ArtVisualEntity.THUNDERHEAD,11);}
                else {
                    boolean empowered=live(t,"thunder",time);Vec3 from=empowered?ApexArts.point(t,"thunder"):e.position(),forward=ApexArts.forward(e);
                    if(empowered){t.remove("thunder_until");endVisual(l,t,"thunder_visual");}
                    for(int n=1;n<=7;n++){
                        Vec3 p=from.add(forward.scale(n*4));if(!clearPath(l,e,from.add(0,1,0),p.add(0,1,0)))break;
                        long strike=time+12+n*4;schedule(t,p,strike,empowered?6:4.5,empowered?2400:1800,2,"fire");
                        ArtVisualEntity.spawn(l,ArtVisualEntity.CATACLYSM_FRONT,p,new Vec3(empowered?6:4.5,0,0),(int)(strike-time+12));
                    }
                }
            }
            case DEMETER -> {
                if(mode==0){Vec3 p=location(l,e,32);field(l,t,"orchard",p,240,ArtVisualEntity.WINTER_ORCHARD,12);}
                else if(mode==1){SovereignArts.plantSeeds(l,i,e,allies(l,e,e.position(),16,8));}
                else {
                    Vec3 p=live(t,"orchard",time)?ApexArts.point(t,"orchard"):location(l,e,32);boolean empowered=live(t,"orchard",time);
                    t.remove("orchard_until");endVisual(l,t,"orchard_visual");
                    if(empowered)for(var ally:allies(l,e,p,12,8))heal(ally,.2,.2);
                    var targets=ApexArts.enemies(l,e,p,12);int count=Math.min(6,targets.size());
                    for(int n=0;n<Math.max(1,count);n++){
                        Vec3 mark=count==0?p:targets.get(n).position();schedule(t,mark,time+24,3,empowered?2200:1400,2,"ice");
                        ArtVisualEntity.spawn(l,ArtVisualEntity.CRYSTAL_BLOOM,mark,new Vec3(3,0,0),38);
                    }
                }
            }
            case JANUS -> {
                if(mode==0){
                    if(live(t,"gates",time)){Vec3 destination=gateDestination(t,e);ArtVisualEntity.spawn(l,ArtVisualEntity.GATE_TRANSIT,e.position().add(0,1,0),destination.subtract(e.position()),14);teleport(e,destination);}
                    else {
                        Vec3 a=e.position(),b=safeCursor(l,e,48);ApexArts.point(t,"gate_a",a);ApexArts.point(t,"gate_b",b);t.putLong("gates_until",time+600);
                        visual(l,t,"gate_a_visual",ArtVisualEntity.TWIN_GATE,a,new Vec3(1.5,0,-Math.toRadians(e.getYRot())),600);visual(l,t,"gate_b_visual",ArtVisualEntity.TWIN_GATE,b,new Vec3(1.5,1,-Math.toRadians(e.getYRot())),600);
                    }
                }else if(mode==1){
                    Vec3 p=location(l,e,48);schedule(t,p,time+16,7,1900,1,"space");ArtVisualEntity.spawn(l,ArtVisualEntity.PARALLAX,p,new Vec3(7,0,0),28);
                    if(live(t,"gates",time) && Math.min(e.distanceToSqr(ApexArts.point(t,"gate_a")),e.distanceToSqr(ApexArts.point(t,"gate_b")))<=16){
                        Vec3 other=gateDestination(t,e);if(other.distanceToSqr(p)>14*14){schedule(t,other,time+16,7,1900,1,"space");ArtVisualEntity.spawn(l,ArtVisualEntity.PARALLAX,other,new Vec3(7,0,0),28);}
                    }
                }
                else {
                    Vec3 p=location(l,e,32);ArtVisualEntity.spawn(l,ArtVisualEntity.CROSSROADS,p,new Vec3(10,0,0),40);
                    for(var target:ApexArts.enemies(l,e,p,10).stream().limit(6).toList()){
                        Vec3 destination=p.scale(2).subtract(target.position());destination=new Vec3(destination.x,target.getY(),destination.z);
                        // Blocked destinations leave the target in place; damage never depends on an unsafe teleport.
                        if(safe(l,target,destination)){ArtVisualEntity.spawn(l,ArtVisualEntity.GATE_TRANSIT,target.position().add(0,1,0),destination.subtract(target.position()),14);teleport(target,destination);}
                        schedule(t,target.position(),time+12,2.5,1700,2,"space");
                        effect(s,i,e,target,TensuraMobEffects.MOVEMENT_INTERFERENCE,35,0,2);
                        ArtVisualEntity.spawn(l,ArtVisualEntity.PARALLAX,target.position(),new Vec3(2.5,0,0),24);
                    }
                }
            }
            case ANANSI,AEGIR -> SovereignArts.cast(s,l,i,e,mode);
            case LUGH -> LughArts.cast(s,l,i,e,mode);
            case THOTH -> {
                if(mode==0){
                    Vec3 p=location(l,e,64);ArtVisualEntity.spawn(l,ArtVisualEntity.LEXICON,p,new Vec3(6,0,0),ThothElement.LEXICON_LIFETIME);
                    for(int chapter=0;chapter<ThothElement.COUNT;chapter++)schedule(t,p,time+ThothElement.lexiconTick(chapter),6,ThothElement.LEXICON_DAMAGE,0,ThothElement.at(chapter).id);
                    GodlyVisuals.circle(l,e,p.add(0,.1,0),6,ThothElement.LEXICON_LIFETIME,MagicCircleVariant.MISC);
                }else if(mode==1){
                    Vec3 p=location(l,e,36);double recovered=0;ArtVisualEntity.spawn(l,ArtVisualEntity.SPELLBREAK,p,new Vec3(12,0,0),30);
                    for(var target:ApexArts.enemies(l,e,p,12).stream().limit(6).toList()){
                        hit(s,i,e,target,900,1,TensuraDamageTypes.MAGIC_GENERIC);
                        if(SkillAPI.getSkillsFrom(target).getLearnedSkills().stream().anyMatch(v->MythsTiers.isGodly(v.getSkill())))continue;
                        effect(s,i,e,target,TensuraMobEffects.SILENCE,60,0,1);effect(s,i,e,target,TensuraMobEffects.MAGIC_INTERFERENCE,60,1,1);
                        var storage=TensuraStorages.getExistenceFrom(target);double drained=Math.min(1500,Math.max(0,storage.getMagicule()));storage.setMagicule(storage.getMagicule()-drained);storage.markDirty();recovered+=drained*.5;
                    }
                    var own=TensuraStorages.getExistenceFrom(e);own.setMagicule(Math.min(EnergyHelper.getMaxMagicule(e),own.getMagicule()+Math.min(2000,recovered)));own.markDirty();
                }else if(mode==2){
                    ManaCrucible.begin(i,e);
                }else {t.putInt("library_chapter",0);t.putLong("library_until",time+ThothElement.LIBRARY_DURATION);t.putLong("library_next",time+ThothElement.LIBRARY_FIRST);visual(l,t,"library_visual",ArtVisualEntity.ASTRAL_LIBRARY,e.position().add(0,2.5,0),new Vec3(3.5,0,0),ThothElement.LIBRARY_DURATION);if(l.getEntity(t.getUUID("library_visual")) instanceof ArtVisualEntity v)v.setCaster(e);}
            }
        }
        l.playSound(null,e.blockPosition(),switch(s.kind){case THREADWEAVER,JANUS->SoundEvents.ENDERMAN_TELEPORT;case VERDANT_KEEPER,DEMETER->SoundEvents.AMETHYST_BLOCK_CHIME;case VRITRA->SoundEvents.BLAZE_SHOOT;default->SoundEvents.ENCHANTMENT_TABLE_USE;},SoundSource.PLAYERS,.85f,s.kind.godly()?.7f:1.1f);
    }
    private static Vec3 rayEnd(ServerLevel l,LivingEntity e,double range){Vec3 eye=e.getEyePosition();return l.clip(new net.minecraft.world.level.ClipContext(eye,eye.add(e.getLookAngle().scale(range)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,e)).getLocation();}
    static List<LivingEntity> lineTargets(ServerLevel l,LivingEntity e,Vec3 from,Vec3 end,double width,int cap){return l.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(width),v->HunterData.canHarm(e,v) && e.hasLineOfSight(v) && v.getBoundingBox().inflate(width).clip(from,end).isPresent()).stream().sorted(Comparator.comparingDouble(v->v.distanceToSqr(from))).limit(cap).toList();}
    static boolean clearPath(ServerLevel l,LivingEntity e,Vec3 a,Vec3 b){return l.clip(new net.minecraft.world.level.ClipContext(a,b,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,e)).getType()==HitResult.Type.MISS;}
    static boolean safe(ServerLevel l,LivingEntity e,Vec3 p){var box=e.getBoundingBox().move(p.subtract(e.position()));return Double.isFinite(p.x+p.y+p.z) && box.minY>=l.getMinBuildHeight() && box.maxY<=l.getMaxBuildHeight() && l.getWorldBorder().isWithinBounds(box) && l.noCollision(e,box);}
    static Vec3 safeCursor(ServerLevel l,LivingEntity e,double range){
        Vec3 start=e.position(),desired=location(l,e,range),direction=desired.subtract(start).normalize(),last=start;
        double length=Math.min(range,desired.distanceTo(start));
        for(double d=.5;d<=length;d+=.5){Vec3 p=start.add(direction.scale(d));if(!safe(l,e,p))break;last=p;}return last;
    }
    private static Vec3 gateDestination(CompoundTag t,LivingEntity e){Vec3 a=ApexArts.point(t,"gate_a"),b=ApexArts.point(t,"gate_b");return e.distanceToSqr(a)<=e.distanceToSqr(b)?b:a;}
    static void teleport(LivingEntity e,Vec3 p){
        Vec3 velocity=e.getDeltaMovement();e.teleportTo(p.x,p.y,p.z);e.fallDistance=0;e.setDeltaMovement(velocity);e.hasImpulse=true;e.hurtMarked=true;
        if(e instanceof ServerPlayer player)player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(e));
    }
    static void visual(ServerLevel l,CompoundTag t,String key,int kind,Vec3 p,Vec3 data,int ticks){endVisual(l,t,key);t.putUUID(key,ArtVisualEntity.spawn(l,kind,p,data,ticks).getUUID());}
    static Entity entityAcrossLevels(ServerLevel level,UUID id){
        var entity=level.getEntity(id);if(entity!=null)return entity;
        for(var other:level.getServer().getAllLevels())if(other!=level){entity=other.getEntity(id);if(entity!=null)return entity;}return null;
    }
    static void endVisual(ServerLevel l,CompoundTag t,String key){if(t.hasUUID(key)){var entity=entityAcrossLevels(l,t.getUUID(key));if(entity!=null)entity.discard();t.remove(key);}}
    static void field(ServerLevel l,CompoundTag t,String key,Vec3 p,int ticks,int kind,double radius){ApexArts.point(t,key,p);t.putLong(key+"_until",l.getGameTime()+ticks);t.putLong(key+"_next",l.getGameTime());visual(l,t,key+"_visual",kind,p,new Vec3(radius,0,0),ticks);}
    static void schedule(CompoundTag t,Vec3 p,long time,double radius,float damage,int mode,String type){var list=t.getList("bursts",Tag.TAG_COMPOUND);if(list.size()>=32)return;var b=new CompoundTag();ApexArts.point(b,"p",p);b.putLong("at",time);b.putDouble("radius",radius);b.putFloat("damage",damage);b.putInt("mode",mode);b.putString("type",type);list.add(b);t.put("bursts",list);}
    private static ResourceKey<DamageType> damageType(String type){if(type.equals("ice"))return TensuraDamageTypes.ICE_ELEMENTAL;var element=ThothElement.named(type);return element==null?TensuraDamageTypes.SPACE_ELEMENTAL:element.damage;}
    static boolean hit(MythicSkill s,ManasSkillInstance i,LivingEntity e,LivingEntity target,float damage,int mode,ResourceKey<DamageType> type){
        if(!HunterData.canHarm(e,target))return false;float power=(float)(1+.5*Math.clamp(i.getMastery()/s.getMaxMastery(),0,1));var source=s.createSource(i,e,type,mode);
        if(!s.kind.unique() && !s.kind.godly())((io.github.manasmods.tensura.damage.TensuraDamageSource)source).tensura$setResistanceBypassLevel(1);
        return target.hurt(source,damage*power);
    }
    static void area(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e,Vec3 p,double radius,float damage,int mode,ResourceKey<DamageType> type,int cap){for(var target:ApexArts.enemies(l,e,p,radius).stream().limit(cap).toList())hit(s,i,e,target,damage,mode,type);}
    static void effect(MythicSkill s,ManasSkillInstance i,LivingEntity e,LivingEntity target,dev.architectury.registry.registries.RegistrySupplier<MobEffect> effect,int ticks,int amplifier,int mode){s.addEffect(target,i,mode,e,TensuraMobEffects.getReference(effect),ticks,amplifier);}
    static boolean friendly(LivingEntity e,LivingEntity target){return target==e || e.isAlliedTo(target) || target instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isOwnedBy(e);}
    static List<LivingEntity> allies(ServerLevel l,LivingEntity e,Vec3 p,double radius,int cap){var list=new ArrayList<LivingEntity>();if(e.distanceToSqr(p)<=radius*radius)list.add(e);l.getEntitiesOfClass(LivingEntity.class,new AABB(p,p).inflate(radius),v->v!=e && v.isAlive() && friendly(e,v) && v.distanceToSqr(p)<=radius*radius && e.hasLineOfSight(v)).stream().sorted(Comparator.comparingDouble(v->v.distanceToSqr(p))).limit(Math.max(0,cap-list.size())).forEach(list::add);return list;}
    static void heal(LivingEntity e,double hp,double shp){e.heal((float)(e.getMaxHealth()*hp));var energy=TensuraStorages.getExistenceFrom(e);energy.setSpiritualHealth(Math.min(e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),energy.getSpiritualHealth()+e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*shp));energy.markDirty();}
    private static void detonate(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e,boolean commanded){var t=i.getOrCreateTag();Vec3 p=ApexArts.point(t,"mine");area(s,l,i,e,p,commanded?6:3,commanded?450:300,commanded?1:0,TensuraDamageTypes.EARTH_ELEMENTAL,8);ArtVisualEntity.spawn(l,ArtVisualEntity.RUNE_BURST,p,new Vec3(commanded?6:3,0,0),22);t.remove("mine_until");endVisual(l,t,"mine_visual");}
    static boolean tick(MythicSkill s,ServerLevel l,ManasSkillInstance i,LivingEntity e){
        var t=i.getOrCreateTag();long time=now(e);
        if(live(t,"tether",time) && t.hasUUID("tether_target")){
            var entity=l.getEntity(t.getUUID("tether_target"));
            if(!(entity instanceof LivingEntity target) || !HunterData.canHarm(e,target) || e.distanceToSqr(target)>40*40 || !e.hasLineOfSight(target)){t.remove("tether_until");endVisual(l,t,"tether_visual");}
            else {Vec3 anchor=ApexArts.point(t,"tether_anchor");if(target.distanceToSqr(anchor)>9)ApexArts.push(target,anchor.subtract(target.position()).normalize().scale(.38));moveVisual(l,t,"tether_visual",e.getEyePosition(),target.getEyePosition().subtract(e.getEyePosition()));}
        }
        if(live(t,"mine",time) && time>=t.getLong("mine_armed") && !ApexArts.enemies(l,e,ApexArts.point(t,"mine"),3).isEmpty())detonate(s,l,i,e,false);
        for(String key:List.of("grove","orchard"))if(live(t,key,time) && time>=t.getLong(key+"_next")){
            boolean orchard=key.equals("orchard");Vec3 p=ApexArts.point(t,key);
            if(orchard)for(var ally:allies(l,e,p,12,8))heal(ally,.1,.1);
            else GroveHealing.pulse(i,e,allies(l,e,p,7,8));
            if(orchard)for(var target:ApexArts.enemies(l,e,p,12).stream().limit(8).toList()){hit(s,i,e,target,500,0,TensuraDamageTypes.ICE_ELEMENTAL);effect(s,i,e,target,TensuraMobEffects.CHILL,40,1,0);}
            t.putLong(key+"_next",time+20);
        }
        if(live(t,"aegis",time)){moveVisual(l,t,"aegis_visual",e.position().add(0,1,0),new Vec3(3.5,0,0));reflect(l,e,3.5);}
        if(live(t,"thunder",time) && time>=t.getLong("thunder_next")){
            Vec3 p=ApexArts.point(t,"thunder");for(var target:ApexArts.enemies(l,e,p,11).stream().limit(6).toList()){
                hit(s,i,e,target,900,1,TensuraDamageTypes.LIGHTNING_ELEMENTAL);schedule(t,target.position(),time+12,3,500,1,"fire");
                ArtVisualEntity.spawn(l,ArtVisualEntity.PLASMA_LANCE,target.position().add(0,8,0),new Vec3(0,-8,0),12);
            }t.putLong("thunder_next",time+20);
        }
        if(live(t,"library",time)){
            moveVisual(l,t,"library_visual",e.position().add(0,2.5,0),new Vec3(3.5,0,0));
            if(time>=t.getLong("library_next")){
                int chapter=t.getInt("library_chapter");var element=ThothElement.at(chapter);int book=ThothElement.libraryBook(chapter);
                if(t.hasUUID("library_visual") && l.getEntity(t.getUUID("library_visual")) instanceof ArtVisualEntity formation)formation.style(chapter);
                for(var target:ApexArts.enemies(l,e,e.position(),28).stream().limit(3).toList()){
                    double phase=(ThothElement.LIBRARY_DURATION-(t.getLong("library_until")-time))*.025+book*Math.PI*2/3;
                    Vec3 from=e.position().add(Math.cos(phase)*3.5,2.5+Math.sin((ThothElement.LIBRARY_DURATION-(t.getLong("library_until")-time))*.04+book)*.35,Math.sin(phase)*3.5);
                    ArtVisualEntity.spawn(l,ArtVisualEntity.SPELL_RAY,from,target.getEyePosition().subtract(from),10).style(element.ordinal());hit(s,i,e,target,ThothElement.LIBRARY_DAMAGE,3,element.damage);
                }t.putLong("library_next",time+ThothElement.LIBRARY_INTERVAL);t.putInt("library_chapter",chapter+1);
            }
        }
        var pending=t.getList("bursts",Tag.TAG_COMPOUND);var keep=new ListTag();
        for(var value:pending){var b=(CompoundTag)value;if(time<b.getLong("at")){keep.add(b);continue;}
            Vec3 p=ApexArts.point(b,"p");area(s,l,i,e,p,b.getDouble("radius"),b.getFloat("damage"),b.getInt("mode"),damageType(b.getString("type")),8);
            l.sendParticles(s.kind==MythicKind.DEMETER?ParticleTypes.SNOWFLAKE:ParticleTypes.ENCHANT,p.x,p.y+1,p.z,25,1.2,1.5,1.2,.04);
            if(s.kind==MythicKind.VRITRA)ArtVisualEntity.spawn(l,ArtVisualEntity.ELEMENT_IMPACT,p.add(0,.2,0),new Vec3(Math.min(3,b.getDouble("radius")),0,0),18).style(2);
            if(s.kind==MythicKind.THOTH){var element=ThothElement.named(b.getString("type"));GodlyVisuals.circle(l,e,p.add(0,.2,0),6,10,element==null?MagicCircleVariant.MISC:element.circle);}
        }t.put("bursts",keep);
        boolean active=!keep.isEmpty() | SovereignArts.tick(s,l,i,e) | (s.kind==MythicKind.LUGH && LughArts.tick(l,i,e));for(String key:List.of("tether","mine","grove","aegis","thunder","orchard","gates","library")){
            if(live(t,key,time))active=true;else if(t.contains(key+"_until")){t.remove(key+"_until");if(key.equals("gates")){endVisual(l,t,"gate_a_visual");endVisual(l,t,"gate_b_visual");}else endVisual(l,t,key+"_visual");}
        }return active;
    }
    static void reflect(ServerLevel l,LivingEntity e,double radius){
        for(var p:l.getEntitiesOfClass(Projectile.class,e.getBoundingBox().inflate(radius),v->v.getOwner()!=e && (!(v.getOwner() instanceof LivingEntity owner) || HunterData.canHarm(e,owner)) && v.distanceToSqr(e)<=radius*radius)){
            if(p.getDeltaMovement().dot(e.getEyePosition().subtract(p.position()))<=0)continue;
            Vec3 destination=p.getOwner() instanceof LivingEntity owner?owner.getEyePosition():p.position().add(p.getDeltaMovement().scale(-10));
            Vec3 velocity=destination.subtract(p.position()).normalize().scale(Math.max(.7,p.getDeltaMovement().length()));
            p.setOwner(e);p.setDeltaMovement(velocity);// Owner transfer prevents damage attribution to the original shooter.
            p.hurtMarked=true;ArtVisualEntity.spawn(l,ArtVisualEntity.MIRROR_SHARD,p.position(),new Vec3(.7,0,0),12);
        }
    }
    static void moveVisual(ServerLevel l,CompoundTag t,String key,Vec3 p,Vec3 data){if(t.hasUUID(key) && l.getEntity(t.getUUID(key)) instanceof ArtVisualEntity visual)visual.updateVector(p,data);}
    static void clear(ManasSkillInstance i,LivingEntity e){SovereignArts.clear(i,e);LughArts.clear(i,e);if(i.getSkill() instanceof MythicSkill s && s.kind==MythicKind.THOTH)ManaCrucible.stop(i,e);var t=i.getOrCreateTag();if(e.level() instanceof ServerLevel l)for(String key:VISUALS)endVisual(l,t,key);for(String key:List.of("tether","mine","grove","aegis","thunder","orchard","gates","library"))t.remove(key+"_until");t.remove("bursts");t.remove("tether_target");i.markDirty();}
    private static ResourceLocation wardId(UUID owner){return WildHunt.id("crystal_bulwark/"+owner);}
    static void lendBarrier(ServerLevel l,LivingEntity owner,LivingEntity ally,double shield,double strength,int ticks){
        var id=wardId(owner.getUUID());for(var h:List.of(TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MAGIC_BARRIER,TensuraAttributes.MULTILAYER_BARRIER)){
            var value=ally.getAttribute(h);if(value!=null)value.addOrUpdateTransientModifier(new AttributeModifier(id,h==TensuraAttributes.MULTILAYER_BARRIER?shield:strength,AttributeModifier.Operation.ADD_VALUE));
        }
        var lease=new CompoundTag();lease.putUUID("owner",owner.getUUID());lease.putLong("until",l.getGameTime()+ticks);
        var data=HunterData.of(ally);var leases=data.getList("crystal_wards",Tag.TAG_COMPOUND);leases.removeIf(v->((CompoundTag)v).getUUID("owner").equals(owner.getUUID()));leases.add(lease);data.put("crystal_wards",leases);
        ArtVisualEntity.spawn(l,ArtVisualEntity.CRYSTAL_BULWARK,ally.position(),new Vec3(1.4,0,0),ticks).setCaster(ally);
    }
    public static void tickTarget(LivingEntity e){
        SovereignArts.tickTarget(e);LughArts.tickSeal(e);
        var data=e.getPersistentData().getCompound("wildhunt");var wards=data.getList("crystal_wards",Tag.TAG_COMPOUND);var keep=new ListTag();
        for(var value:wards){var lease=(CompoundTag)value;if(e.isAlive() && lease.getLong("until")>now(e)){keep.add(lease);continue;}
            var id=wardId(lease.getUUID("owner"));for(var h:List.of(TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MAGIC_BARRIER,TensuraAttributes.MULTILAYER_BARRIER)){var a=e.getAttribute(h);if(a!=null)a.removeModifier(id);}
        }if(keep.isEmpty())data.remove("crystal_wards");else data.put("crystal_wards",keep);
    }
    public static void clearTransient(LivingEntity e){
        for(var i:SkillAPI.getSkillsFrom(e).getLearnedSkills())if(i.getSkill() instanceof MythicSkill s){
            s.clearLegacyDefense(e);clear(i,e);i.getOrCreateTag().putString("dimension",e.level().dimension().location().toString());
            if(i.isToggled()){i.getOrCreateTag().putLong("upkeep",0);HunterData.of(e).putBoolean("active_skills",true);}
        }
    }
}
