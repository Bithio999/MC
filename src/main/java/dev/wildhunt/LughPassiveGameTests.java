package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class LughPassiveGameTests {
    static LivingEntity observer(GameTestHelper h,Vec3 at){
        var pos=BlockPos.containing(at);h.getLevel().getChunk(pos);h.getLevel().setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
        var owner=target(h,at);EnergyHelper.setBaseMaxEP(owner,20_000_000);TensuraStorages.getExistenceFrom(owner).setMagicule(1_000_000);return owner;
    }
    static ManasSkillInstance enable(LivingEntity owner){var i=learn(owner,MythicKind.LUGH);((MythicSkill)i.getSkill()).onToggleOn(i,owner);return i;}
    private static void near(GameTestHelper h,double actual,double expected,String why){h.assertTrue(Math.abs(actual-expected)<.0001,why+": "+actual+" != "+expected);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void allSkilledAddsFortyNineNativeLearningAndMasteryPointsAndCleansLegacySpeed(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.LUGH);var s=(MythicSkill)i.getSkill();var mastery=p.getAttribute(TensuraAttributes.ABILITY_MASTERY_GAIN);var learning=p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN);
        mastery.setBaseValue(2);learning.setBaseValue(3);double beforeMastery=mastery.getValue(),beforeLearning=learning.getValue();
        var foreign=WildHunt.id("lugh_test_foreign_speed");var speed=p.getAttribute(Attributes.ATTACK_SPEED);speed.addOrUpdateTransientModifier(new AttributeModifier(foreign,.25,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));double attack=speed.getValue();
        speed.addOrUpdateTransientModifier(new AttributeModifier(WildHunt.id("lugh_passive"),.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        HunterData.learnReward(p,UniqueSkills.MUSICIAN.get());var nativeSkill=SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.MUSICIAN.get()).orElseThrow();
        p.getRandom().setSeed(915);double learningBefore=UniqueSkills.MUSICIAN.get().getDefaultLearningIncrement(nativeSkill,p,0);
        s.onToggleOn(i,p);h.assertTrue(LughArts.active(p),"All-Skilled did not activate");near(h,mastery.getValue(),beforeMastery+49,"Mastery gain is not a flat +49");near(h,learning.getValue(),beforeLearning+49,"Learning gain is not a flat +49");near(h,speed.getValue(),attack,"Legacy attack speed survived or another modifier was removed");
        nativeSkill.setMastery(0);nativeSkill.addMasteryPoint(p);near(h,nativeSkill.getMastery(),beforeMastery+49,"A native skill did not receive the increased mastery gain");
        p.getRandom().setSeed(915);near(h,UniqueSkills.MUSICIAN.get().getDefaultLearningIncrement(nativeSkill,p,0),learningBefore+49,"Native learning did not receive +49");
        s.onToggleOff(i,p);near(h,mastery.getValue(),beforeMastery,"Mastery modifier leaked after toggle off");near(h,learning.getValue(),beforeLearning,"Learning modifier leaked after toggle off");near(h,speed.getValue(),attack,"Cleanup removed a foreign speed modifier");h.succeed();
    }
    private static void nativeCooldownTick(LivingEntity owner){
        try{var tick=SkillStorage.class.getDeclaredMethod("checkSkillTimers",LivingEntity.class,Skills.class);tick.setAccessible(true);tick.invoke(null,owner,SkillAPI.getSkillsFrom(owner));}
        catch(ReflectiveOperationException ex){throw new IllegalStateException("Native cooldown timer fixture failed",ex);}
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeCooldownsRecoverTwiceAsFastExceptLughAndStopBoostingWhenDisabled(GameTestHelper h){
        var p=player(h);var i=enable(p);var other=learn(p,MythicKind.RUNE_SMITH);HunterData.learnReward(p,UniqueSkills.MUSICIAN.get());var nativeSkill=SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.MUSICIAN.get()).orElseThrow();HunterData.learnReward(p,WildHunt.GODLY.get());var aion=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        other.setCoolDown(7,0);nativeSkill.setCoolDown(8,0);aion.setCoolDown(10,3);i.setCoolDown(4,1);i.setCoolDown(18,2);i.setCoolDown(40,3);
        nativeCooldownTick(p);h.assertTrue(other.getCoolDown(0)==5 && nativeSkill.getCoolDown(0)==6 && aion.getCoolDown(3)==8,"Native/addon/Godly recovery was not doubled");h.assertTrue(i.getCoolDown(1)==3 && i.getCoolDown(2)==17 && i.getCoolDown(3)==39,"Lugh accelerated its own cooldowns");
        nativeCooldownTick(p);nativeCooldownTick(p);nativeCooldownTick(p);h.assertTrue(other.getCoolDown(0)==0 && nativeSkill.getCoolDown(0)==0 && aion.getCoolDown(3)==2,"Odd/zero cooldowns did not clamp or finish in half the native updates");
        other.setCoolDown(8,0);((MythicSkill)i.getSkill()).onToggleOff(i,p);nativeCooldownTick(p);h.assertTrue(other.getCoolDown(0)==7,"Cooldown acceleration persisted with toggle off");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void passiveBanksActualNearbyDamageAtSixtyFourBlocksButNotBeyondOrWhileOff(GameTestHelper h){
        var anchor=player(h);Vec3 at=anchor.position().add(8,0,0);var actor=target(h,at);var victim=target(h,at.add(2,0,0));var nearOwner=observer(h,at.add(0,0,64));var farOwner=observer(h,at.add(64.1,0,0));var offOwner=observer(h,at.add(0,0,32));
        var near=enable(nearOwner);var far=enable(farOwner);var off=learn(offOwner,MythicKind.LUGH);var source=learn(actor,MythicKind.RUNE_SMITH);
        afterTracking(h,List.of(actor,victim,nearOwner,farOwner,offOwner),()->{
            victim.hurt(((MythicSkill)source.getSkill()).createSource(source,actor,TensuraDamageTypes.EARTH_ELEMENTAL,0),100);
            h.assertTrue(LughArts.library(near).size()==4 && LughArts.library(far).size()==3 && LughArts.library(off).size()==3,"Automatic bank ignored the 64-block boundary or toggle state");
            var record=LughArts.library(near).getCompound(3);h.assertTrue(record.getString("source").equals(source.getSkillId().toString()) && record.getString("type").equals("tensura:earth_elemental") && record.getFloat("damage")==3500,"Passive lost native source parameters or damage bounds");
            String entry=record.getString("entry");victim.invulnerableTime=0;victim.hurt(((MythicSkill)source.getSkill()).createSource(source,actor,TensuraDamageTypes.EARTH_ELEMENTAL,0),200);
            h.assertTrue(LughArts.library(near).size()==4 && LughArts.library(near).getCompound(3).getString("entry").equals(entry) && LughArts.library(near).getCompound(3).getFloat("observed_damage")==200,"Repeat use duplicated its bank record or did not update parameters");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void unfundedPassiveCannotBankOrAccelerateAndForgettingRemovesBothGainModifiers(GameTestHelper h){
        var p=player(h);var i=enable(p);var other=learn(p,MythicKind.RUNE_SMITH);i.getOrCreateTag().putLong("upkeep",0);TensuraStorages.getExistenceFrom(p).setMagicule(0);h.assertTrue(!LughArts.active(p),"Unfunded expired toggle stayed authorized");
        other.setCoolDown(8,0);nativeCooldownTick(p);h.assertTrue(other.getCoolDown(0)==7,"Unfunded toggle accelerated cooldowns");
        var actor=target(h,p.position().add(8,0,0));var record=new net.minecraft.nbt.CompoundTag();record.putString("name","Test");record.putString("source","wildhunt:rune_smith");record.putString("type","tensura:earth_elemental");record.putFloat("damage",4000);LughArts.collect(p,actor,record);h.assertTrue(LughArts.library(i).size()==3,"Unfunded toggle banked a technique");
        SkillAPI.getSkillsFrom(p).forgetSkill(i);h.assertTrue(!LughArts.active(p) && p.getAttribute(TensuraAttributes.ABILITY_MASTERY_GAIN).getModifier(WildHunt.id("lugh_passive"))==null && p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN).getModifier(WildHunt.id("lugh_passive"))==null,"Forgetting leaked passives");h.succeed();
    }
}
