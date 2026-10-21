package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Twenty independently damaging, continuously homing singularities; one synchronized formation. */
public final class SingularityBarrage {
    private SingularityBarrage(){}
    public static void begin(ServerLevel level,ManasSkillInstance i,LivingEntity caster,LivingEntity target,boolean empowered){
        clear(level,i);
        var tag=i.getOrCreateTag();
        Vec3 center=target.getBoundingBox().getCenter();
        var plan=new CompoundTag();plan.putLong("start",level.getGameTime());plan.putInt("seed",level.random.nextInt());plan.putBoolean("empowered",empowered);
        plan.putUUID("target",target.getUUID());plan.putInt("target_id",target.getId());
        ApexArts.point(plan,"aim",Vec3.ZERO);
        tag.put("singularity_plan",plan);tag.putBoolean("verdict_empowered",empowered);tag.putInt("verdict_pulse",0);
        tag.putLong("verdict_until",level.getGameTime()+SingularityFlight.LIFE);ApexArts.point(tag,"verdict",center);
        var visual=ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_VERDICT,center,new Vec3(24,0,0),SingularityFlight.LIFE);visual.setCaster(caster);visual.barrage(plan);tag.putUUID("verdict_visual",visual.getUUID());
        level.playSound(null,net.minecraft.core.BlockPos.containing(center),SoundEvents.PORTAL_TRIGGER,SoundSource.PLAYERS,.65f,1.45f);
    }
    public static void tick(GodlySkill skill,ServerLevel level,ManasSkillInstance i,LivingEntity caster){
        var tag=i.getOrCreateTag();if(!tag.contains("singularity_plan"))return;var plan=tag.getCompound("singularity_plan");
        long age=level.getGameTime()-plan.getLong("start");if(age<0 || age>=SingularityFlight.LIFE-1){clear(level,i);return;}Vec3 center=ApexArts.point(tag,"verdict"),aim=ApexArts.point(plan,"aim");
        var visual=tag.hasUUID("verdict_visual") && level.getEntity(tag.getUUID("verdict_visual")) instanceof ArtVisualEntity v?v:null;
        if(visual==null && age>3){clear(level,i);return;}
        if(plan.hasUUID("target") && level.getEntity(plan.getUUID("target")) instanceof LivingEntity target && target.isAlive() && HunterData.canHarm(caster,target))aim=target.getBoundingBox().getCenter().subtract(center);
        ApexArts.point(plan,"aim",aim);
        int node=tag.getInt("verdict_pulse");boolean changed=false;
        // Each collision has its own native source and exact frozen detonation point. Flight never locks.
        // The loop also safely catches up if tickActive was skipped briefly, without repeating any hit.
        while(node<SingularityFlight.COUNT && age>=SingularityFlight.impact(node)){
            Vec3 impact=center.add(aim).add(SingularityFlight.impactOffset(plan.getInt("seed"),node));
            boolean empowered=plan.getBoolean("empowered");double radius=empowered?4.5:3.75;
            for(var target:level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(impact,impact).inflate(radius),
                    e->(e instanceof net.minecraft.world.entity.Mob || e instanceof net.minecraft.world.entity.player.Player) && HunterData.canHarm(caster,e) && e.getBoundingBox().distanceToSqr(impact)<=radius*radius).stream()
                    .sorted(Comparator.comparingDouble(e->e.distanceToSqr(impact))).limit(8).toList())
                skill.hit(level,i,caster,target,empowered?600:480,3);
            ApexArts.point(plan,"impact_"+node,impact.subtract(center));
            plan.putInt("impacted",plan.getInt("impacted")|(1<<node));
            // Do not stack twenty full-volume sounds; the collision optics remain independent.
            level.playSound(null,net.minecraft.core.BlockPos.containing(impact),SoundEvents.WARDEN_SONIC_BOOM,SoundSource.PLAYERS,node%4==0?.7f:.25f,.8f+node*.018f);
            if(node%4==0)level.playSound(null,net.minecraft.core.BlockPos.containing(impact),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,.9f,.65f+node*.025f);
            node++;changed=true;
        }
        tag.putInt("verdict_pulse",node);
        if(visual!=null && (changed || age%2==0))visual.barrage(plan);
    }

    public static void clear(ServerLevel level,ManasSkillInstance i){
        var tag=i.getOrCreateTag();if(tag.hasUUID("verdict_visual") && MythicArts.entityAcrossLevels(level,tag.getUUID("verdict_visual")) instanceof ArtVisualEntity visual)visual.discard();
        for(var key:List.of("verdict_visual","verdict_circle")){
            if(tag.hasUUID(key) && MythicArts.entityAcrossLevels(level,tag.getUUID(key)) instanceof net.minecraft.world.entity.Entity e)e.discard();tag.remove(key);
        }
        tag.remove("singularity_plan");tag.remove("verdict_until");tag.remove("verdict_next");
    }
}
