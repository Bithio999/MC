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
    public static void downwardAndLevelAimMatchAnActualNativeMartialMasterCast(GameTestHelper h){
        var anchor=player(h);var p=target(h,anchor.position().add(30,0,0));var start=p.position();
        for(int x=-2;x<=20;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(start).offset(x,-1,z),Blocks.STONE.defaultBlockState());
        EnergyHelper.setBaseMaxEP(p,100000);var energy=TensuraStorages.getExistenceFrom(p);var nativeSkill=UniqueSkills.MARTIAL_MASTER.get();var reference=nativeSkill.createDefaultInstance();
        double original=MartialMasterSkill.CONFIG.ultraDistance;MartialMasterSkill.CONFIG.ultraDistance=ChainedDash.REACH;
        try{
            for(float pitch:new float[]{8,35,0}){
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
}
