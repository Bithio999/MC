package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.unique.MartialMasterSkill;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import static dev.heavenlyrestriction.TestFixtures.*;
import static dev.heavenlyrestriction.ChainedGameTests.*;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class ChainedDashGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void downwardBlockAimMatchesAnActualNativeMartialMasterCast(GameTestHelper h){
        var anchor=player(h);var p=target(h,anchor.position().add(30,0,0));var start=p.position();
        for(int x=-2;x<=20;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(x,-1,z),Blocks.STONE.defaultBlockState());
        EnergyHelper.setBaseMaxEP(p,100000);var energy=TensuraStorages.getExistenceFrom(p);var nativeSkill=UniqueSkills.MARTIAL_MASTER.get();var reference=nativeSkill.createDefaultInstance();
        double original=MartialMasterSkill.CONFIG.ultraDistance;MartialMasterSkill.CONFIG.ultraDistance=ChainedDash.REACH;
        try{
            for(float pitch:new float[]{8,35}){
                p.setPos(start);p.setOnGround(true);p.setYRot(-90);p.setXRot(pitch);energy.setAura(10000);energy.setMagicule(10000);
                nativeSkill.onPressed(reference,p,0,0);var expected=p.position();h.assertTrue(expected.distanceToSqr(start)>.25,"Native reference did not move");
                p.setPos(start);p.setOnGround(true);energy.setAura(10000);var chained=vow(p,false);chained.setCoolDown(0,0);HeavenlyRestriction.CHAINED.get().onPressed(chained,p,0,0);
                near(h,p.getX(),expected.x,"Landing X differs from native Martial Master at pitch "+pitch);near(h,p.getY(),expected.y,"Landing Y differs from native Martial Master at pitch "+pitch);near(h,p.getZ(),expected.z,"Landing Z differs from native Martial Master at pitch "+pitch);
                near(h,energy.getAura(),9800,"Native-targeted cast did not pay once");h.assertTrue(chained.getCoolDown(0)==1,"Native-targeted cast cooldown");SkillAPI.getSkillsFrom(p).forgetSkill(HeavenlyRestriction.CHAINED.get());
            }
        }finally{MartialMasterSkill.CONFIG.ultraDistance=original;}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void airborneActivationIsFreeAndWaterDoesNotStopTheAimRay(GameTestHelper h){
        var anchor=player(h);var p=target(h,anchor.position().add(30,0,0));var start=p.position();EnergyHelper.setBaseMaxEP(p,100000);var i=vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10000);p.setYRot(-90);p.setXRot(0);p.setOnGround(false);
        HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);h.assertTrue(p.position().equals(start) && energy.getAura()==10000 && !i.onCoolDown(0),"Airborne activation moved or charged despite native movement gate");
        for(int x=3;x<=5;x++)for(int y=0;y<4;y++)for(int z=-1;z<=1;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(x,y,z),Blocks.WATER.defaultBlockState());
        p.setOnGround(true);HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);h.assertTrue(p.getX()-start.x>12,"Water stopped the fluid-ignoring native aim ray");h.succeed();
    }

    private static void runway(GameTestHelper h,net.minecraft.world.phys.Vec3 start){
        for(int x=-2;x<=20;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(x,-1,z),Blocks.STONE.defaultBlockState());
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void levelAndUpwardAirAimCastAtFullReachAndPayOnce(GameTestHelper h){
        var anchor=player(h);var start=anchor.position().add(0,30,0);var p=target(h,start);runway(h,start);EnergyHelper.setBaseMaxEP(p,100000);var i=vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);
        for(float pitch:new float[]{0,-1,-3,-5,-15,-45,-80}){
            p.setPos(start);p.setYRot(-90);p.setXRot(pitch);p.setOnGround(true);i.setCoolDown(0,0);energy.setAura(10000);var expected=start.add(p.getLookAngle().scale(ChainedDash.REACH));
            HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);near(h,p.position().distanceTo(expected),0,"Air aim failed at pitch "+pitch);near(h,energy.getAura(),9800,"Air cast payment at pitch "+pitch);h.assertTrue(i.getCoolDown(0)==1,"Air cast cooldown");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void enemyAimStopsBeforeNearestBodyAndStillDealsArrivalDamage(GameTestHelper h){
        var anchor=player(h);var start=anchor.position().add(0,30,0);var p=target(h,start);runway(h,start);EnergyHelper.setBaseMaxEP(p,100000);var i=vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10000);var nearTarget=target(h,start.add(8,0,0));var farTarget=target(h,start.add(14,0,0));
        afterTracking(h,java.util.List.of(p,nearTarget,farTarget),()->{
            p.setYRot(-90);p.setXRot(0);p.setOnGround(true);energy.setAura(10000);HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);
            h.assertTrue(p.getX()>start.x+6 && p.getX()<nearTarget.getBoundingBox().minX,"Dash passed the aimed enemy or stopped too early");h.assertTrue(!p.getBoundingBox().intersects(nearTarget.getBoundingBox()),"Caster overlaps aimed enemy");h.assertTrue(nearTarget.getHealth()<1024 && farTarget.getHealth()==1024,"Dash did not stop its damage route at the first aimed enemy");near(h,energy.getAura(),9800,"Enemy-targeted cast paid incorrectly");h.assertTrue(i.getCoolDown(0)==1,"Enemy-targeted cooldown");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void downwardEnemyAimLandsAtItsFeetInsteadOfInsideTheFloor(GameTestHelper h){
        var anchor=player(h);var start=anchor.position().add(0,30,0);var p=target(h,start);runway(h,start);var victim=target(h,start.add(8,0,0));p.setOnGround(true);p.setYRot(-90);p.setXRot(8);
        afterTracking(h,java.util.List.of(p,victim),()->{p.setOnGround(true);var end=ChainedDash.destination(p).orElseThrow();h.assertTrue(end.x>start.x+6 && end.x<victim.getX(),"Downward enemy aim missed target stop: "+end+" from "+start);near(h,end.y,start.y,"Enemy aim lowered the feet into the floor");h.assertTrue(h.getLevel().noCollision(p,p.getBoundingBox().move(end.subtract(start))),"Enemy landing collides");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void alliesAndOffAxisEnemiesDoNotCaptureAimAndWallsOccludeTargets(GameTestHelper h){
        var anchor=player(h);var start=anchor.position().add(0,30,0);var p=target(h,start);runway(h,start);var ally=target(h,start.add(5,0,0));var offAxis=target(h,start.add(7,0,2));var enemy=target(h,start.add(12,0,0));
        afterTracking(h,java.util.List.of(p,ally,offAxis,enemy),()->{
        var scoreboard=h.getLevel().getScoreboard();var team=scoreboard.addPlayerTeam("dash_"+java.util.UUID.randomUUID().toString().substring(0,8));scoreboard.addPlayerToTeam(p.getScoreboardName(),team);scoreboard.addPlayerToTeam(ally.getScoreboardName(),team);
        try{
            p.setYRot(-90);p.setXRot(0);p.setOnGround(true);var end=ChainedDash.destination(p).orElseThrow();h.assertTrue(end.x>start.x+10 && end.x<enemy.getX(),"Ally/off-axis enemy captured the aim: "+end+" from "+start);
            for(int y=0;y<4;y++)for(int z=-1;z<=1;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(8,y,z),Blocks.STONE.defaultBlockState());
            end=ChainedDash.destination(p).orElseThrow();h.assertTrue(end.x<start.x+8,"Enemy behind wall captured the aim");
        }finally{scoreboard.removePlayerTeam(team);}
        h.succeed();});
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void slightUpwardAimUnderTwoBlockCeilingFindsAForwardLanding(GameTestHelper h){
        var anchor=player(h);var start=anchor.position().add(0,30,0);var p=target(h,start);runway(h,start);EnergyHelper.setBaseMaxEP(p,100000);var i=vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);
        for(int x=-2;x<=20;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(x,2,z),Blocks.STONE.defaultBlockState());
        p.setOnGround(true);p.setYRot(-90);p.setXRot(-1);energy.setAura(10000);HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);
        h.assertTrue(p.getX()>start.x+10,"Slight upward aim rejected a traversable two-block corridor");near(h,p.getY(),start.y,"Dash left the corridor's standing height");near(h,energy.getAura(),9800,"Corridor dash payment");h.assertTrue(i.getCoolDown(0)==1,"Corridor dash cooldown");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void airAimCanLandOnSlabAndOneBlockRiseInsteadOfFailing(GameTestHelper h){
        var anchor=player(h);var start=anchor.position().add(0,30,0);var p=target(h,start);runway(h,start);EnergyHelper.setBaseMaxEP(p,100000);var i=vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);
        for(var block:new net.minecraft.world.level.block.Block[]{Blocks.STONE_SLAB,Blocks.STONE}){
            for(int x=10;x<=20;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(x,0,z),block.defaultBlockState());
            p.setPos(start);p.setOnGround(true);p.setYRot(-90);p.setXRot(0);i.setCoolDown(0,0);energy.setAura(10000);HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);
            h.assertTrue(p.getX()>start.x+10,"Air aim rejected an unobstructed landing on "+block);near(h,p.getY(),start.y+(block==Blocks.STONE_SLAB?.5:1),"Dash did not use actual support height");near(h,energy.getAura(),9800,"Raised landing payment");h.assertTrue(i.getCoolDown(0)==1,"Raised landing cooldown");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void closeEnemyStillAllowsAShortSafeDash(GameTestHelper h){
        var anchor=player(h);var start=anchor.position().add(0,30,0);var p=target(h,start);runway(h,start);EnergyHelper.setBaseMaxEP(p,100000);var i=vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);var victim=target(h,start.add(1,0,0));
        afterTracking(h,java.util.List.of(p,victim),()->{
            p.setOnGround(true);p.setYRot(-90);p.setXRot(0);energy.setAura(10000);HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);
            h.assertTrue(p.getX()>start.x+.01 && p.getX()<victim.getBoundingBox().minX,"Close enemy rejected a short safe dash");h.assertTrue(!p.getBoundingBox().intersects(victim.getBoundingBox()),"Close enemy landing overlaps its body");h.assertTrue(victim.getHealth()<1024,"Close enemy did not receive the dash hit");near(h,energy.getAura(),9800,"Close enemy payment");h.assertTrue(i.getCoolDown(0)==1,"Close enemy cooldown");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void levelAimUnderBirchCanopyFindsStandingRoomWithoutADummy(GameTestHelper h){
        var anchor=player(h);var start=anchor.position().add(0,30,0);var p=target(h,start);runway(h,start);EnergyHelper.setBaseMaxEP(p,100000);var i=vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);
        var leaves=Blocks.BIRCH_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true);
        for(int x=6;x<=18;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(x,2,z),leaves);
        for(int y=0;y<=4;y++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(12,y,0),Blocks.BIRCH_LOG.defaultBlockState());
        p.setOnGround(true);p.setYRot(-90);p.setXRot(0);energy.setAura(10000);HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);
        h.assertTrue(p.getX()>start.x+8 && p.getX()<start.x+12,"Level forest aim rejected standing room before a trunk under leaves");near(h,p.getY(),start.y,"Forest landing put the head in the canopy");near(h,energy.getAura(),9800,"Forest dash payment");h.assertTrue(i.getCoolDown(0)==1,"Forest dash cooldown");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void closeWallUsesExactContactRatherThanASnappedPointBehindCaster(GameTestHelper h){
        var anchor=player(h);var start=anchor.position().add(.6,30,.5);var p=target(h,start);runway(h,start);EnergyHelper.setBaseMaxEP(p,100000);var i=vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);
        for(int y=0;y<4;y++)for(int z=-1;z<=1;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(1,y,z),Blocks.STONE.defaultBlockState());
        p.setOnGround(true);p.setYRot(-90);p.setXRot(0);energy.setAura(10000);HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);
        h.assertTrue(p.getX()>start.x+.01 && p.getX()<BlockPos.containing(start).getX()+.71,"Close wall cast failed or moved backward");near(h,p.getY(),start.y,"Close wall cast moved vertically instead of into the safe gap");h.assertTrue(h.getLevel().noCollision(p,p.getBoundingBox()),"Close wall landing intersects stone");near(h,energy.getAura(),9800,"Close wall payment");h.assertTrue(i.getCoolDown(0)==1,"Close wall cooldown");h.succeed();
    }
}
