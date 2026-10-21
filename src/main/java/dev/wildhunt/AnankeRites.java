package dev.wildhunt;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import java.util.*;

/** Server-owned warnings and exact, bounded hit geometry. Cosmetic light never creates damage. */
public final class AnankeRites {
    private AnankeRites(){}
    public static final int[] WARNING={24,28,30,75};
    public static final int[] LIFE={48,150,86,108};
    public static void begin(ServerLevel level,AnankeEntity boss,int mode){
        var plan=new CompoundTag();plan.putLong("start",level.getGameTime());plan.putInt("mode",mode);plan.putInt("phase",boss.phase());plan.putInt("seed",level.random.nextInt());
        Vec3 root=boss.position(),aim=boss.getTarget().getBoundingBox().getCenter();
        ApexArts.point(plan,"root",root);ApexArts.point(plan,"aim",aim.subtract(root));
        Vec3 forward=aim.subtract(root).multiply(1,0,1).normalize();if(forward.lengthSqr()<.01)forward=new Vec3(0,0,1);ApexArts.point(plan,"forward",forward);
        plan.putInt("count",boss.phase()>=2?8:6);plan.put("hits",new ListTag());ApexArts.point(plan,"star_0",star(plan,0));plan.putInt("warned",1);
        var visual=ArtVisualEntity.spawn(level,ArtVisualEntity.ANANKE_FATEBREAKER+mode,root,new Vec3(mode==3?32:24,0,0),LIFE[mode]);visual.setCaster(boss);visual.barrage(plan);plan.putUUID("visual",visual.getUUID());boss.rite=plan;
        boss.setRite(mode+1);level.playSound(null,boss.blockPosition(),SoundEvents.BEACON_POWER_SELECT,SoundSource.HOSTILE,1.3f,mode==3?.5f:.8f);
    }
    public static boolean fan(Vec3 delta,Vec3 facing){return Math.abs(delta.y)<5 && delta.horizontalDistanceSqr()<=16*16 && (delta.horizontalDistanceSqr()<1 || delta.multiply(1,0,1).normalize().dot(facing)>=Math.cos(Math.toRadians(55)));}
    public static boolean dawnDanger(Vec3 delta){double horizontal=delta.horizontalDistance();return delta.lengthSqr()<=32*32 && !(horizontal>=7 && horizontal<=12);}
    public static Vec3 star(CompoundTag plan,int index){
        if(plan.contains("star_"+index+"_x"))return ApexArts.point(plan,"star_"+index);
        Vec3 aim=ApexArts.point(plan,"aim");double a=index*2.3999632+plan.getInt("seed")*.00001,r=index==0?0:2.5+(index%3)*1.3;
        return aim.add(Math.cos(a)*r,0,Math.sin(a)*r);
    }
    private static List<LivingEntity> targets(ServerLevel level,AnankeEntity boss,Vec3 center,double radius){
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),e->boss.eligible(e) && e.getBoundingBox().distanceToSqr(center)<=radius*radius);
    }
    public static void tick(ServerLevel level,AnankeEntity boss){
        var plan=boss.rite;if(plan.isEmpty())return;int mode=plan.getInt("mode");long age=level.getGameTime()-plan.getLong("start");
        if(age<0 || age>=LIFE[mode]){clear(level,boss);return;}
        var skill=MythBosses.MOIRA.get();var instance=io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(boss).getSkill(skill).orElse(null);
        if(instance==null || !skill.canInteractSkill(instance,boss) || !skill.canActivateSkill(instance,boss,mode)){clear(level,boss);return;}
        if(!plan.hasUUID("visual") || !(level.getEntity(plan.getUUID("visual")) instanceof ArtVisualEntity) && age>3){clear(level,boss);return;}
        Vec3 root=ApexArts.point(plan,"root"),forward=ApexArts.point(plan,"forward");boss.getNavigation().stop();boss.setDeltaMovement(boss.getDeltaMovement().scale(.3));
        if(mode==0 && age>=WARNING[0] && !plan.getBoolean("struck")){
            for(var target:targets(level,boss,root,18))if(fan(target.getBoundingBox().getCenter().subtract(root),forward))skill.hit(boss,target,mode);
            plan.putBoolean("struck",true);strikeSound(level,root,1.1f);
        }else if(mode==1){
            int warning=plan.getInt("warned");
            if(warning<plan.getInt("count") && age>=warning*12){
                if(boss.getTarget()!=null && boss.eligible(boss.getTarget()))ApexArts.point(plan,"aim",boss.getTarget().getBoundingBox().getCenter().subtract(root));
                ApexArts.point(plan,"star_"+warning,star(plan,warning));plan.putInt("warned",warning+1);
                if(level.getEntity(plan.getUUID("visual")) instanceof ArtVisualEntity visual)visual.barrage(plan);
            }
            int index=plan.getInt("star");
            if(index<plan.getInt("count") && age>=WARNING[1]+index*12){
                Vec3 at=root.add(star(plan,index));for(var target:targets(level,boss,at,3))skill.hit(boss,target,mode);
                plan.putInt("star",index+1);strikeSound(level,at,1.4f);
            }
        }else if(mode==2 && age>=WARNING[2] && age<WARNING[2]+40){
            double r=3+(age-WARNING[2])*.55;var hits=plan.getList("hits",Tag.TAG_INT_ARRAY);
            for(var target:targets(level,boss,root,27)){
                Vec3 delta=target.getBoundingBox().getCenter().subtract(root);var id=NbtUtils.createUUID(target.getUUID());
                if(Math.abs(delta.y)<=3 && Math.abs(delta.horizontalDistance()-r)<=1.2 && !hits.contains(id)){
                    skill.hit(boss,target,mode);hits.add(id);
                }
            }if(age==WARNING[2])strikeSound(level,root,.6f);
        }else if(mode==3 && age>=WARNING[3] && !plan.getBoolean("struck")){
            for(var target:targets(level,boss,root,33))if(dawnDanger(target.getBoundingBox().getCenter().subtract(root)))skill.hit(boss,target,mode);
            plan.putBoolean("struck",true);strikeSound(level,root,.45f);
        }
    }
    private static void strikeSound(ServerLevel level,Vec3 at,float pitch){level.playSound(null,net.minecraft.core.BlockPos.containing(at),SoundEvents.WARDEN_SONIC_BOOM,SoundSource.HOSTILE,1.5f,pitch);level.playSound(null,net.minecraft.core.BlockPos.containing(at),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.HOSTILE,1.4f,pitch);}
    public static void clear(ServerLevel level,AnankeEntity boss){if(boss.rite.hasUUID("visual") && MythicArts.entityAcrossLevels(level,boss.rite.getUUID("visual")) instanceof ArtVisualEntity visual)visual.discard();boss.rite=new CompoundTag();boss.setRite(0);}
}
