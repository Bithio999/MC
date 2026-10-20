package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class SingularityGameTests {
    private static ManasSkillInstance aion(LivingEntity owner){HunterData.learnReward(owner,WildHunt.GODLY.get());return SkillAPI.getSkillsFrom(owner).getSkill(WildHunt.GODLY.get()).orElseThrow();}
    private static final class Practice extends Husk {
        int attempts,accepted;float raw;final List<Long> hits=new ArrayList<>();
        Practice(GameTestHelper h,Vec3 at){super(EntityType.HUSK,h.getLevel());setPos(at);setNoAi(true);setNoGravity(true);getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);setHealth(1024);addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,400,3));h.getLevel().addFreshEntity(this);}
        @Override public boolean hurt(DamageSource source,float amount){
            attempts++;raw+=amount;boolean ok=super.hurt(source,amount);if(ok){accepted++;hits.add(level().getGameTime());}if(isAlive())setHealth(getMaxHealth());return ok;
        }
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void twentySeededCurvesAreContinuousDistinctAndConserveSquashVolume(GameTestHelper h){
        var origins=new HashSet<Vec3>();Vec3 aim=new Vec3(4,1,-3);int curved=0;
        for(int node=0;node<20;node++){
            Vec3 origin=SingularityFlight.origin(7182,node);h.assertTrue(origin.y>=3.4 && origin.y<=11.6 && origins.add(origin),"Singularity origins overlap or are not airborne");
            h.assertTrue(origin.equals(SingularityFlight.origin(7182,node)) && !origin.equals(SingularityFlight.origin(7183,node)),"Shared seeds are unstable or identical");
            int launch=SingularityFlight.launch(7182,node);Vec3 left=SingularityFlight.position(7182,node,launch-.0001,aim),right=SingularityFlight.position(7182,node,launch+.0001,aim);
            h.assertTrue(left.distanceTo(right)<.001,"Launch position pops between anticipation and flight");
            Vec3 end=aim.add(SingularityFlight.impactOffset(7182,node));h.assertTrue(SingularityFlight.position(7182,node,SingularityFlight.impact(node/4),aim).distanceTo(end)<1e-8,"Visual endpoint does not match its blast");
            Vec3 start=SingularityFlight.position(7182,node,launch,aim),mid=SingularityFlight.position(7182,node,(launch+SingularityFlight.impact(node/4))*.5,aim);
            if(mid.distanceTo(start.lerp(end,.5))>1)curved++;
            for(double age=launch;age<SingularityFlight.impact(node/4);age+=.5){Vec3 velocity=SingularityFlight.velocity(7182,node,age,aim);h.assertTrue(Double.isFinite(velocity.length()) && velocity.length()<15,"Motion has a non-finite or unbounded velocity");}
        }
        for(double speed:new double[]{0,.1,1,5,100}){double stretch=SingularityFlight.stretch(speed),cross=SingularityFlight.transverse(stretch);h.assertTrue(Math.abs(stretch*cross*cross-1)<1e-9,"Squash/stretch changes volume");}
        h.assertTrue(curved==20,"Some singularities follow straight or duplicate paths");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void barrageTracksTargetThenLocksBeforeEachImpactAndSyncsItsClock(GameTestHelper h){
        var p=player(h);var target=target(h,p.position().add(8,0,0));var i=aion(p);
        afterTracking(h,List.of(target),()->{
            WildHunt.GODLY.get().onPressed(i,p,0,3);var tag=i.getOrCreateTag();var plan=tag.getCompound("singularity_plan");
            h.assertTrue(plan.hasUUID("target") && plan.getUUID("target").equals(target.getUUID()),"Barrage did not acquire aimed target");
            target.setPos(target.position().add(0,0,3));WildHunt.GODLY.get().tickActive(i,p);Vec3 tracked=ApexArts.point(plan,"aim_0");h.assertTrue(tracked.z>2.9,"Target tracking did not move the unfrozen aim");
            plan.putLong("start",h.getLevel().getGameTime()-(SingularityFlight.FIRST_IMPACT-SingularityFlight.LOCK_LEAD));WildHunt.GODLY.get().tickActive(i,p);
            Vec3 locked=ApexArts.point(plan,"aim_0");target.setPos(target.position().add(0,0,3));WildHunt.GODLY.get().tickActive(i,p);
            h.assertTrue(ApexArts.point(plan,"aim_0").equals(locked) && ApexArts.point(plan,"aim_1").z>locked.z,"Lock did not preserve the dodge window or froze future salvos");
            var visual=(ArtVisualEntity)h.getLevel().getEntity(tag.getUUID("verdict_visual"));h.assertTrue(visual.barrageAge(0)==44 && visual.tickCount<44 && visual.barrage().getInt("seed")==plan.getInt("seed"),"Clients rely on spawn age instead of the synchronized cast clock");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void barrageRetainsLastAimWhenTargetLeavesItsTether(GameTestHelper h){
        var p=player(h);var target=target(h,p.position().add(8,0,0));var i=aion(p);
        afterTracking(h,List.of(target),()->{
            WildHunt.GODLY.get().onPressed(i,p,0,3);var plan=i.getOrCreateTag().getCompound("singularity_plan");target.setPos(target.position().add(0,0,4));WildHunt.GODLY.get().tickActive(i,p);Vec3 before=ApexArts.point(plan,"aim");
            target.setPos(target.position().add(0,0,80));WildHunt.GODLY.get().tickActive(i,p);h.assertTrue(ApexArts.point(plan,"aim").equals(before),"Barrage followed a target beyond its 24-block tether");h.succeed();
        });
    }
    private static void combat(GameTestHelper h,boolean empowered){
        var p=player(h);var z=new Practice(h,p.position().add(8,0,0));var i=aion(p);i.setMastery(i.getMaxMastery());
        afterTracking(h,List.of(z),()->{
            if(empowered){WildHunt.GODLY.get().onPressed(i,p,0,1);}
            var energy=TensuraStorages.getExistenceFrom(p);double mp=energy.getMagicule(),aura=energy.getAura();WildHunt.GODLY.get().onPressed(i,p,0,3);
            h.assertTrue(energy.getMagicule()==mp-28000 && energy.getAura()==aura && i.getCoolDown(3)==45,"Barrage changed its MP-only cost or cooldown");
            for(int tick=0;tick<=123;tick++){final int age=tick;h.runAfterDelay(age,()->{
                WildHunt.GODLY.get().tickActive(i,p);
                if(age<50)h.assertTrue(z.attempts==0,"Barrage damaged before its visual warning/flight");
                if(age==123){
                    h.assertTrue(z.attempts==5 && z.accepted==5,"Twenty explosions did not resolve as five native salvos: "+z.attempts+" / "+z.accepted);
                    float expected=(empowered?12000:9600)*1.5f;h.assertTrue(Math.abs(z.raw-expected)<.1,"Barrage multiplied its intended budget: "+z.raw+" expected "+expected);
                    for(int n=1;n<z.hits.size();n++)h.assertTrue(z.hits.get(n)-z.hits.get(n-1)>=12,"Salvos reset/bypassed native hurt immunity");
                    h.assertTrue(!i.getOrCreateTag().contains("singularity_plan") && !i.getOrCreateTag().hasUUID("verdict_visual"),"Completed barrage retained its attack plan or visual");h.succeed();
                }
            });}
        });
    }
    @GameTest(template="empty",timeoutTicks=180) public static void standaloneBarrageHasFiveAcceptedNativeHitsAndItsExistingBudget(GameTestHelper h){combat(h,false);}
    @GameTest(template="empty",timeoutTicks=180) public static void horizonBarrageHasFiveAcceptedNativeHitsAndItsExistingBudget(GameTestHelper h){combat(h,true);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void barrageCancellationRemovesBothDamageAndVisualsAcrossDimensions(GameTestHelper h){
        var p=player(h);var i=aion(p);WildHunt.GODLY.get().onPressed(i,p,0,3);var tag=i.getOrCreateTag();var id=tag.getUUID("verdict_visual");
        h.runAfterDelay(2,()->{
            var other=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);var visual=h.getLevel().getEntity(id);h.assertTrue(visual!=null,"Barrage visual did not enter entity tracking");visual.discard();
            other.getChunk(new net.minecraft.core.BlockPos(9,180,9));other.setChunkForced(0,0,true);var replacement=ArtVisualEntity.spawn(other,ArtVisualEntity.CAUSAL_VERDICT,new Vec3(9,180,9),new Vec3(24,0,0),122);tag.putUUID("verdict_visual",replacement.getUUID());
            h.runAfterDelay(2,()->{
                GodlySkill.clearTransient(p);h.assertTrue(replacement.isRemoved() && !tag.contains("singularity_plan") && !tag.contains("verdict_until"),"Lifecycle left a remote barrage running");
                i.setCoolDown(0,3);WildHunt.GODLY.get().onPressed(i,p,0,3);var last=tag.getUUID("verdict_visual");h.runAfterDelay(2,()->{
                    SkillAPI.getSkillsFrom(p).forgetSkill(i);var abandoned=h.getLevel().getEntity(last);h.assertTrue((abandoned==null || abandoned.isRemoved()) && !tag.contains("singularity_plan"),"Forgetting Aion left a running barrage");h.succeed();
                });
            });
        });
    }
}
