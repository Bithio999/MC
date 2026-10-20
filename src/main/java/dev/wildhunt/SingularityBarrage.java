package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Twenty visual singularities; four blast contributions combine into one native hit per salvo. */
public final class SingularityBarrage {
    private SingularityBarrage(){}
    public static void begin(ServerLevel level,ManasSkillInstance i,LivingEntity caster,boolean empowered){
        clear(level,i);
        var tag=i.getOrCreateTag();var target=ApexArts.target(level,caster,64);
        Vec3 center=target!=null?target.getBoundingBox().getCenter():empowered?ApexArts.point(tag,"horizon"):ApexArts.cursorPlacement(level,caster,64).add(0,1,0);
        var plan=new CompoundTag();plan.putLong("start",level.getGameTime());plan.putInt("seed",level.random.nextInt());plan.putBoolean("empowered",empowered);
        if(target!=null){plan.putUUID("target",target.getUUID());plan.putInt("target_id",target.getId());}
        ApexArts.point(plan,"aim",Vec3.ZERO);for(int wave=0;wave<SingularityFlight.WAVES;wave++)ApexArts.point(plan,"aim_"+wave,Vec3.ZERO);
        tag.put("singularity_plan",plan);tag.putBoolean("verdict_empowered",empowered);tag.putInt("verdict_pulse",0);
        tag.putLong("verdict_until",level.getGameTime()+SingularityFlight.LIFE);ApexArts.point(tag,"verdict",center);
        var visual=ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_VERDICT,center,new Vec3(24,0,0),SingularityFlight.LIFE);visual.setCaster(caster);visual.barrage(plan);tag.putUUID("verdict_visual",visual.getUUID());
        level.playSound(null,net.minecraft.core.BlockPos.containing(center),SoundEvents.PORTAL_TRIGGER,SoundSource.PLAYERS,.65f,1.45f);
    }
    public static void tick(GodlySkill skill,ServerLevel level,ManasSkillInstance i,LivingEntity caster){
        var tag=i.getOrCreateTag();if(!tag.contains("singularity_plan"))return;var plan=tag.getCompound("singularity_plan");
        long age=level.getGameTime()-plan.getLong("start");if(age<0 || age>=SingularityFlight.LIFE-1){clear(level,i);return;}Vec3 center=ApexArts.point(tag,"verdict"),aim=ApexArts.point(plan,"aim");
        if(plan.hasUUID("target") && level.getEntity(plan.getUUID("target")) instanceof LivingEntity target && target.isAlive() && HunterData.canHarm(caster,target) && target.getBoundingBox().getCenter().distanceToSqr(center)<=24*24)aim=target.getBoundingBox().getCenter().subtract(center);
        ApexArts.point(plan,"aim",aim);boolean changed=false;int mask=plan.getInt("locked");
        for(int wave=0;wave<SingularityFlight.WAVES;wave++)if((mask&(1<<wave))==0){
            ApexArts.point(plan,"aim_"+wave,aim);
            if(age>=SingularityFlight.impact(wave)-SingularityFlight.LOCK_LEAD){mask|=1<<wave;changed=true;}
        }
        plan.putInt("locked",mask);
        if(tag.hasUUID("verdict_visual") && level.getEntity(tag.getUUID("verdict_visual")) instanceof ArtVisualEntity visual){
            if(changed || age%3==0)visual.barrage(plan);
        }else if(age>3){clear(level,i);return;}
        int wave=tag.getInt("verdict_pulse");
        if(wave<SingularityFlight.WAVES && age>=SingularityFlight.impact(wave)){
            Vec3 impact=center.add(ApexArts.point(plan,"aim_"+wave));boolean empowered=plan.getBoolean("empowered");double radius=empowered?4.5:3.75;
            for(var target:ApexArts.enemies(level,caster,impact,radius+2).stream().limit(8).toList()){
                float damage=0;
                for(int n=wave*4;n<wave*4+4;n++)if(target.getBoundingBox().distanceToSqr(impact.add(SingularityFlight.impactOffset(plan.getInt("seed"),n)))<=radius*radius)damage+=empowered?600:480;
                if(damage>0)skill.hit(level,i,caster,target,damage,3);
            }
            tag.putInt("verdict_pulse",wave+1);
            level.playSound(null,net.minecraft.core.BlockPos.containing(impact),SoundEvents.WARDEN_SONIC_BOOM,SoundSource.PLAYERS,.65f,.85f+wave*.07f);
            level.playSound(null,net.minecraft.core.BlockPos.containing(impact),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,1.1f,.65f+wave*.14f);
        }
    }
    public static void clear(ServerLevel level,ManasSkillInstance i){
        var tag=i.getOrCreateTag();if(tag.hasUUID("verdict_visual") && MythicArts.entityAcrossLevels(level,tag.getUUID("verdict_visual")) instanceof ArtVisualEntity visual)visual.discard();
        for(var key:List.of("verdict_visual","verdict_circle")){
            if(tag.hasUUID(key) && MythicArts.entityAcrossLevels(level,tag.getUUID(key)) instanceof net.minecraft.world.entity.Entity e)e.discard();tag.remove(key);
        }
        tag.remove("singularity_plan");tag.remove("verdict_until");tag.remove("verdict_next");
    }
}
