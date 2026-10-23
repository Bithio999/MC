package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.effect.template.TensuraMobEffectInstance;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import java.util.*;

/** Paid casts, locked warnings, bounded native hits and an interruptible creation-seal finale. */
public final class EschatonRites {
    private EschatonRites(){}
    public static boolean canBegin(EschatonEntity b,int mode){
        if(mode<0 || mode>7 || b.getTarget()==null || !b.eligible(b.getTarget()) || b.distanceToSqr(b.getTarget())>96*96)return false;
        Vec3 delta=b.getTarget().getBoundingBox().getCenter().subtract(b.position());
        if(mode==0)return Math.abs(delta.y)<=9 && delta.horizontalDistanceSqr()<=46*46;
        if(mode==1)return Math.abs(delta.y)<=24 && delta.horizontalDistanceSqr()<=46*46;
        if(mode==4)return Math.abs(delta.y)<=8 && delta.horizontalDistanceSqr()<=56*56;
        if(mode==5){var e=TensuraStorages.getExistenceFrom(b);return e.getMagicule()<EnergyHelper.getMaxMagicule(b)-8_000_000 && e.getAura()>=20_000_000;}
        if(mode==6)return delta.lengthSqr()<=64*64 && landing(b,b.getTarget().position())!=null;
        if(mode==7)return !sealPositions(b).isEmpty();
        return true;
    }
    public static void begin(ServerLevel level,EschatonEntity b,int mode){
        var p=new CompoundTag();p.putLong("start",level.getGameTime());p.putInt("mode",mode);p.putInt("phase",b.form());p.putInt("seed",level.random.nextInt());p.putInt("step",0);p.putInt("warned",1);p.putInt("last_pulse",-100);
        Vec3 root=b.position(),aim=b.getTarget().getBoundingBox().getCenter();ApexArts.point(p,"root",root);ApexArts.point(p,"aim",aim.subtract(root));
        Vec3 forward=aim.subtract(root).multiply(1,0,1).normalize();if(forward.lengthSqr()<.01)forward=new Vec3(0,0,1);ApexArts.point(p,"forward",forward);
        ApexArts.point(p,"meteor_0",aim.subtract(root));
        if(mode==6){ApexArts.point(p,"landing",landing(b,b.getTarget().position()).subtract(root));ApexArts.point(p,"apex",root.add(0,6,0));}
        if(mode==7){
            var positions=sealPositions(b);int broken=0;
            for(int n=0;n<3;n++){
                if(n>=positions.size()){broken|=1<<n;ApexArts.point(p,"seal_"+n,new Vec3(0,-100,0));continue;}
                Vec3 at=positions.get(n);var seal=EschatonContent.SEAL.get().create(level);if(seal==null)throw new IllegalStateException("Genesis seal factory missing");seal.setPos(at);seal.bind(b,n,p.getLong("start"));level.addFreshEntity(seal);ApexArts.point(p,"seal_"+n,at.subtract(root));p.putUUID("seal_entity_"+n,seal.getUUID());
            }p.putInt("broken",broken);b.announce("Eschaton: Break my creation seals, or be unmade. A broken seal leaves a golden refuge.",0xFFA9A3);
        }
        b.plan=p;b.setArt(mode+1);
        var visual=ArtVisualEntity.spawn(level,ArtVisualEntity.ESCHATON_EDGE+mode,root,new Vec3(mode==7?56:48,b.form(),0),EschatonPattern.LIFE[mode]);visual.setCaster(b);visual.barrage(p);p.putUUID("visual",visual.getUUID());
        level.playSound(null,b.blockPosition(),mode==7?SoundEvents.END_PORTAL_SPAWN:SoundEvents.BEACON_POWER_SELECT,SoundSource.HOSTILE,mode==7?1.6f:1.2f,mode==7?.5f:.7f+mode*.06f);
    }
    private static Vec3 landing(EschatonEntity b,Vec3 target){
        Vec3 away=b.position().subtract(target).multiply(1,0,1).normalize();if(away.lengthSqr()<.01)away=new Vec3(1,0,0);
        for(double angle:new double[]{0,1.2,-1.2,2.4,-2.4}){Vec3 at=target.add(away.yRot((float)angle).scale(4));if(b.safePosition(at))return at;}return null;
    }
    private static List<Vec3> sealPositions(EschatonEntity b){
        List<Vec3> chosen=new ArrayList<>();var l=b.level();for(int n=0;n<24 && chosen.size()<3;n++){
            int candidate=n%12;double angle=((candidate%3)*4+candidate/3)*Math.TAU/12,r=n<12?18:25;Vec3 at=b.position().add(Math.cos(angle)*r,1,Math.sin(angle)*r);var bounds=new AABB(at.x-1.1,at.y,at.z-1.1,at.x+1.1,at.y+3.6,at.z+1.1);
            if(at.distanceToSqr(Vec3.atCenterOf(b.anchor()))>96*96 || !l.hasChunkAt(BlockPos.containing(at)) || !l.getWorldBorder().isWithinBounds(bounds) || bounds.minY<l.getMinBuildHeight() || bounds.maxY>=l.getMaxBuildHeight() || !l.noCollision(null,bounds) || l.containsAnyLiquid(bounds) || chosen.stream().anyMatch(v->v.distanceToSqr(at)<16*16))continue;
            chosen.add(at);
        }return chosen;
    }
    private static List<LivingEntity> targets(ServerLevel l,EschatonEntity b,Vec3 center,double radius){return l.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),e->b.eligible(e) && e.getBoundingBox().distanceToSqr(center)<=radius*radius).stream().limit(64).toList();}
    public static void tick(ServerLevel level,EschatonEntity b){
        var p=b.plan;if(p.isEmpty())return;int mode=p.getInt("mode");if(mode<0 || mode>7){clear(level,b);return;}
        long age=level.getGameTime()-p.getLong("start");if(age<0 || age>=EschatonPattern.LIFE[mode]){clear(level,b);return;}
        var i=SkillAPI.getSkillsFrom(b).getSkill(EschatonContent.DEMIURGE.get()).orElse(null);
        if(i==null || !i.getSkill().canInteractSkill(i,b) || !i.getSkill().canActivateSkill(i,b,mode) || !b.isAlive() || p.hasUUID("visual") && level.getEntity(p.getUUID("visual"))==null && age>3){clear(level,b);return;}
        Vec3 root=ApexArts.point(p,"root");b.getNavigation().stop();b.setDeltaMovement(b.getDeltaMovement().scale(.3));var skill=EschatonContent.DEMIURGE.get();
        switch(mode){
            case 0->{
                int wave=p.getInt("step");while(wave<3 && age>=EschatonPattern.edgeTick(wave)){
                    for(var t:targets(level,b,root,48))if(EschatonPattern.edge(t.getBoundingBox().getCenter().subtract(root),p,wave))skill.hit(b,t,0,wave);
                    p.putInt("step",++wave);strike(level,root,.8f);
                }
            }
            case 1->{
                if(age>=32 && age<112 && age-p.getInt("last_pulse")>=10){
                    Vec3 a=root.add(EschatonPattern.loomStart(p));
                    for(var t:targets(level,b,root,48))for(int n=0;n<EschatonPattern.count(p);n++)if(t.getBoundingBox().inflate(1.1).clip(a,root.add(EschatonPattern.loomEnd(p,age,n))).isPresent()){skill.hit(b,t,1,n);break;}
                    p.putInt("last_pulse",(int)age);
                }
            }
            case 2->{
                int warned=p.getInt("warned");while(warned<7 && age>=warned*14){
                    Vec3 aim=b.getTarget()!=null && b.eligible(b.getTarget())?b.getTarget().getBoundingBox().getCenter().subtract(root):ApexArts.point(p,"aim");ApexArts.point(p,"meteor_"+warned,aim);p.putInt("warned",++warned);sync(level,b);
                }
                int n=p.getInt("step");while(n<7 && age>=EschatonPattern.meteorTick(n)){
                    Vec3 at=root.add(EschatonPattern.meteor(p,n));for(var t:targets(level,b,at,4.5))skill.hit(b,t,2,n);strike(level,at,1.1f);p.putInt("step",++n);
                }
            }
            case 3->{
                if(age>=38 && !p.getBoolean("struck")){
                    for(var t:targets(level,b,root,96))for(int n=0;n<3;n++)if(t.getBoundingBox().inflate(2.4,8,2.4).clip(root.add(EschatonPattern.cutEnd(p,n,-1)),root.add(EschatonPattern.cutEnd(p,n,1))).isPresent()){skill.hit(b,t,3,n);break;}
                    p.putBoolean("struck",true);strike(level,root,.5f);
                }
            }
            case 4->{
                for(int n=0;n<3;n++){
                    if(age<EschatonPattern.waveTick(n) || age>EschatonPattern.waveTick(n)+44)continue;
                    var hits=p.getList("hits_"+n,Tag.TAG_INT_ARRAY);
                    for(var t:targets(level,b,root,58))if(EschatonPattern.wave(t.getBoundingBox().getCenter().subtract(root),p,n,age)){
                        var uuid=NbtUtils.createUUID(t.getUUID());if(!hits.contains(uuid)){skill.hit(b,t,4,n);hits.add(uuid);}
                    }p.put("hits_"+n,hits);
                }
            }
            case 5->{
                if(age>=48 && !p.getBoolean("struck")){var e=TensuraStorages.getExistenceFrom(b);e.setMagicule(Math.min(EnergyHelper.getMaxMagicule(b),e.getMagicule()+8_000_000));e.markDirty();p.putBoolean("struck",true);strike(level,b.position(),1.4f);}
            }
            case 6->{
                if(age<24){Vec3 apex=ApexArts.point(p,"apex");if(b.safePosition(apex)){Vec3 at=root.lerp(apex,Math.clamp(age/24d,0,1));b.teleportTo(at.x,at.y,at.z);}}
                if(age>=36 && !p.getBoolean("struck")){
                    Vec3 at=root.add(ApexArts.point(p,"landing"));if(b.safePosition(at)){b.teleportTo(at.x,at.y,at.z);b.setDeltaMovement(Vec3.ZERO);for(var t:targets(level,b,at,9))skill.hit(b,t,6,0);strike(level,at,.5f);}p.putBoolean("struck",true);
                }
            }
            case 7->{
                if(p.getBoolean("interrupted")){if(age>=p.getLong("interrupted_at")+24 && p.contains("interrupted_at")){clear(level,b);return;}if(!p.contains("interrupted_at"))p.putLong("interrupted_at",age);}
                else if(age>=200 && !p.getBoolean("struck")){
                    for(var t:targets(level,b,root,56))if(!EschatonPattern.refuge(t.getBoundingBox().getCenter().subtract(root),p))skill.hit(b,t,7,0);
                    p.putBoolean("struck",true);strike(level,root,.35f);for(var seal:level.getEntitiesOfClass(GenesisSealEntity.class,b.getBoundingBox().inflate(144),s->s.ownedBy(b)))seal.discard();
                }
            }
        }
        if(age>=releaseAge(p)){finish(b);}
    }
    static int releaseAge(CompoundTag p){return switch(p.getInt("mode")){case 0->64;case 1->120;case 2->124;case 3->48;case 4->120;case 5->56;case 6->48;default->220;};}
    private static void finish(EschatonEntity b){if(b.plan.getInt("mode")==7)for(var seal:b.level().getEntitiesOfClass(GenesisSealEntity.class,b.getBoundingBox().inflate(144),s->s.ownedBy(b)))seal.discard();b.plan=new CompoundTag();b.setArt(0);}
    public static void sync(ServerLevel l,EschatonEntity b){if(b.plan.hasUUID("visual") && l.getEntity(b.plan.getUUID("visual")) instanceof ArtVisualEntity v)v.barrage(b.plan);}
    static void silence(EschatonEntity b,LivingEntity t,ManasSkillInstance skill){var effect=new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.MAGIC_INTERFERENCE),60,1);((TensuraMobEffectInstance)effect).tensura$setSource(b.getUUID());((TensuraMobEffectInstance)effect).tensura$setSourceAbility(skill.getSkill(),3);t.addEffect(effect,b);}
    public static void flare(ServerLevel l,EschatonEntity b,Vec3 at,double r){ArtVisualEntity.spawn(l,ArtVisualEntity.ESCHATON_SHATTER,at.add(0,2,0),new Vec3(r,b.form(),0),35).setCaster(b);l.playSound(null,BlockPos.containing(at),SoundEvents.ENDERMAN_TELEPORT,SoundSource.HOSTILE,1.1f,.6f);}
    private static void strike(ServerLevel l,Vec3 at,float pitch){l.playSound(null,BlockPos.containing(at),SoundEvents.WARDEN_SONIC_BOOM,SoundSource.HOSTILE,1.4f,pitch);l.playSound(null,BlockPos.containing(at),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.HOSTILE,1.2f,pitch);}
    public static void clear(ServerLevel l,EschatonEntity b){
        for(var v:l.getEntitiesOfClass(ArtVisualEntity.class,b.getBoundingBox().inflate(192),v->v.ownedBy(b) && v.kind()>=ArtVisualEntity.ESCHATON_EDGE && v.kind()<=ArtVisualEntity.ESCHATON_SHATTER))v.discard();
        for(var seal:l.getEntitiesOfClass(GenesisSealEntity.class,b.getBoundingBox().inflate(160),s->s.ownedBy(b)))seal.discard();b.plan=new CompoundTag();b.setArt(0);
    }
}
