package dev.wildhunt;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;
import static dev.wildhunt.ChainedGameTests.near;
@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class SchemaGameTests {
    private static ManasSkillInstance learn(LivingEntity p,int count,boolean mastered){
        int[] slots=new int[6];Arrays.fill(slots,0,count,1);return learnTree(p,slots,mastered);
    }
    private static ManasSkillInstance learnTree(LivingEntity p,int[] slots,boolean mastered){
        HunterData.learnReward(p,WildHunt.SCHEMA.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.SCHEMA.get()).orElseThrow();i.setMastery(mastered?2000:0);
        if(p instanceof net.minecraft.server.level.ServerPlayer sp){if(!SchemaArt.save(sp,slots,0))throw new IllegalStateException("Unable to save fixture build");}else i.getOrCreateTag().putIntArray("SchemaTree",slots);
        i.onToggleOn(p);return i;
    }
    private static void pulse(String name,LivingEntity p,double arg){try{
        var method=ExistenceStorage.class.getDeclaredMethod(name,IExistence.class,LivingEntity.class,double.class);method.setAccessible(true);method.invoke(null,TensuraStorages.getExistenceFrom(p),p,arg);
    }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
    @GameTest(template="empty",timeoutTicks=100)
    public static void learningRequiresOneThousandAndMasteryTwoThousand(GameTestHelper h){
        var p=player(h);SchemaArt.discover(p);var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.SCHEMA.get()).orElseThrow();
        near(h,i.getMastery(),-1000,"Initial learning points");h.assertTrue(i.getMaxMastery()==2000,"Mastery requirement");
        WildHunt.SCHEMA.get().addLearnPoint(i,p,0,999);near(h,i.getMastery(),-1,"Completed learning too soon");
        WildHunt.SCHEMA.get().addLearnPoint(i,p,0,1);h.assertTrue(i.getMastery()>=0,"Did not finish native learning at 1000 points");
        SchemaArt.discover(p);h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(i.getSkill()).orElseThrow()==i,"Discovery reset the learned entry");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void allMasteryMilestonesRetainFullDrawbacks(GameTestHelper h){
        int[] six={1,1,1,1,1,1};for(int milestone=0;milestone<=5;milestone++){
            double mastery=milestone*400;h.assertTrue(SchemaArt.fullSlots(mastery)==milestone+1,"Full-slot milestone");near(h,SchemaArt.weight(milestone,mastery),1,"New full-strength slot");near(h,SchemaArt.penalty(six),.3,"Mastery diminished a drawback");
        }
        near(h,SchemaArt.bonus(six,0,1),9.84375,"Initial geometric sum");near(h,SchemaArt.bonus(six,2000,1),30,"Mastered six-stack bonus");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void serverValidatesBuildsAndPersistsThemThroughSkillSerialization(GameTestHelper h){
        var p=player(h);SchemaArt.discover(p);int[] one={1,0,0,0,0,0};h.assertTrue(!SchemaArt.save(p,one,0),"Unlearned entry configured a build");
        var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.SCHEMA.get()).orElseThrow();i.setMastery(0);
        h.assertTrue(!SchemaArt.save(p,new int[7],0) && !SchemaArt.save(p,new int[]{3,0,0,0,0,0},0),"Invalid build accepted");
        h.assertTrue(SchemaArt.save(p,one,0) && !SchemaArt.save(p,new int[6],0),"Revision guard failed");one[0]=0;
        var restored=ManasSkillInstance.fromNBT(i.toNBT());h.assertTrue(SchemaArt.count(SchemaArt.build(restored))==1,"Build alias or serialization lost the saved effect");
        SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());h.assertTrue(!SchemaArt.save(p,new int[6],1),"Forged missing-skill save accepted");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void toggleAndMasteryChangeActualRateWithoutStackingRefreshes(GameTestHelper h){
        var p=player(h);p.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER).setBaseValue(1);var i=learn(p,6,false);
        for(int n=0;n<20;n++)SchemaArt.refresh(p);near(h,p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),10.84375,"Repeated refresh compounded regen");near(h,SchemaArt.damageFactor(p),.7,"Six-stack damage penalty");
        i.setMastery(2000);SchemaArt.refresh(p);near(h,p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),31,"Full mastery bonus");
        i.onToggleOff(p);near(h,p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),1,"Toggle off left regen");near(h,SchemaArt.damageFactor(p),1,"Toggle off left damage penalty");
        i.onToggleOn(p);SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());near(h,p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),1,"Forgetting left regen");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeAuraAndSleepRecoveryAreBothBoostedAndCapped(GameTestHelper h){
        var p=player(h);p.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER).setBaseValue(1);learn(p,1,true);var energy=TensuraStorages.getExistenceFrom(p);double maximum=EnergyHelper.getMaxAura(p);
        energy.setSleepModeTime(-1);energy.setAura(0);pulse("handleAuraRegen",p,maximum);near(h,energy.getAura(),EnergyHelper.CONFIG.baseAuraRegen*6,"Natural aura recovery");
        energy.setSleepModeTime(100);energy.setAura(0);pulse("handleAuraRegen",p,maximum);near(h,energy.getAura(),Math.min(maximum,(EnergyHelper.CONFIG.baseAuraRegen+maximum*EnergyHelper.CONFIG.sleepModeAura)*6),"Sleep aura recovery");
        energy.setAura(maximum-.01);pulse("handleAuraRegen",p,maximum);near(h,energy.getAura(),maximum,"Regeneration exceeded the cap");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedBuffAppliesOnceAndNeverMagnifiesThePenalty(GameTestHelper h){
        var p=player(h);ChainedGameTests.vow(p,true);p.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER).setBaseValue(1);var i=learn(p,1,true);
        SchemaArt.refresh(p);near(h,SchemaArt.regenerationFactor(p),16,"Chained positive buff");near(h,p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),16,"Chained double boosted the modifier");near(h,SchemaArt.damageFactor(p),.95,"Chained magnified the drawback");
        var pact=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.CHAINED.get()).orElseThrow();pact.onToggleOff(p);SchemaArt.refresh(p);near(h,p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),6,"Pact toggle did not remove only its boost");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void convertedMagiculeRegenIsBoostedButRetainedMagiculesRemainFlat(GameTestHelper h){
        var p=player(h);ChainedGameTests.vow(p,false);learn(p,1,true);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(0);energy.setMagicule(10);energy.setSleepModeTime(-1);p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER).setBaseValue(1);
        double area=200;try{var method=ExistenceStorage.class.getDeclaredMethod("handleMagiculeRegen",IExistence.class,LivingEntity.class,double.class,double.class);method.setAccessible(true);method.invoke(null,energy,p,area,EnergyHelper.getMaxMagicule(p));}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        near(h,energy.getAura(),Math.max(0,area*EnergyHelper.CONFIG.areaMagiculeRegen-.5)*6,"Converted aura recovery");near(h,energy.getMagicule(),10.5,"Schema multiplied the retained MP rate");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void meleeProjectilesAndDirectSoulDamagePayThePenaltyExactlyOnce(GameTestHelper h){
        var p=player(h);learn(p,1,true);var melee=target(h,p.position().add(3,0,0));var ranged=target(h,p.position().add(6,0,0));var soul=player(h);soul.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(2000);TensuraStorages.getExistenceFrom(soul).setSpiritualHealth(1000);
        for(var e:List.of(melee,ranged))e.getAttribute(Attributes.ARMOR).setBaseValue(0);
        afterTracking(h,List.of(melee,ranged),()->{
            float before=melee.getHealth();h.assertTrue(melee.hurt(p.damageSources().playerAttack(p),100),"Melee rejected");near(h,before-melee.getHealth(),95,"Melee penalty");
            before=ranged.getHealth();h.assertTrue(ranged.hurt(p.damageSources().arrow(new Arrow(EntityType.ARROW,h.getLevel()),p),100),"Projectile rejected");near(h,before-ranged.getHealth(),95,"Projectile penalty");
            h.assertTrue(TensuraDamageHelper.directSpiritualHurt(soul,p,null,100,0),"Soul hit rejected");near(h,TensuraStorages.getExistenceFrom(soul).getSpiritualHealth(),905,"Direct soul penalty");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void masteryRequiresActualRecoveryAndCannotBeFarmedBySavingOrDuplicatePulses(GameTestHelper h){
        var p=player(h);var i=learn(p,1,false);SchemaArt.train(p,0);near(h,i.getMastery(),0,"No recovery granted mastery");
        double increment=p.getAttributeValue(TensuraAttributes.ABILITY_MASTERY_GAIN);SchemaArt.train(p,1);SchemaArt.train(p,1);near(h,i.getMastery(),increment,"Same-tick recovery granted mastery twice");
        SchemaArt.save(p,new int[]{1,0,0,0,0,0},1);near(h,i.getMastery(),increment,"Saving granted mastery");i.onToggleOff(p);SchemaArt.train(p,100);near(h,i.getMastery(),increment,"Inactive build trained");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void outgoingPenaltyAlsoScalesFlatBonusesAddedByNativeEffects(GameTestHelper h){
        var p=player(h);learn(p,1,true);p.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        var auraSword=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.AURA_SWORD);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(auraSword,100));var victim=target(h,p.position().add(3,0,0));victim.getAttribute(Attributes.ARMOR).setBaseValue(0);
        afterTracking(h,List.of(victim),()->{
            var source=p.damageSources().playerAttack(p);var baseline=io.github.manasmods.manascore.network.api.util.Changeable.of(100f);
            ((io.github.manasmods.tensura.effect.template.DamageAction)auraSword.value()).onDamagingEntity(p,victim,source,baseline);
            h.assertTrue(baseline.get()>100,"Native Aura Sword did not add its configured flat damage");
            float before=victim.getHealth();h.assertTrue(victim.hurt(source,100),"Flat-bonus hit rejected");h.assertTrue(Math.abs((before-victim.getHealth())-baseline.get()*.95)<.001,"Schema failed to reduce the complete Aura Sword-augmented hit");h.succeed();
        });
    }


    @GameTest(template="empty",timeoutTicks=100)
    public static void mixedEffectsShareTheirOrderedWeightsAndPersistBothIds(GameTestHelper h){
        int[] mixed={2,1,2,1,2,1};near(h,SchemaArt.strengthBonus(mixed,0,1),6.5625,"Strength ignored mixed slot order");near(h,SchemaArt.bonus(mixed,0,1),3.28125,"Mito ignored Strength slots");
        near(h,SchemaArt.penalty(mixed),.15,"Mito penalty counted Strength");near(h,SchemaArt.vulnerability(mixed),.06,"Strength drawback diminished");
        near(h,SchemaArt.strengthBonus(mixed,2000,1),15,"Mastered mixed Strength");near(h,SchemaArt.bonus(mixed,2000,1),15,"Mastered mixed recovery");
        int[] six={2,2,2,2,2,2};near(h,SchemaArt.strengthBonus(six,0,1),9.84375,"Initial six Strength bonus");near(h,SchemaArt.strengthBonus(six,2000,1),30,"Mastered six Strength bonus");near(h,SchemaArt.vulnerability(six),.12,"Six full drawbacks");
        var p=player(h);var i=learnTree(p,mixed,false);var restored=ManasSkillInstance.fromNBT(i.toNBT());h.assertTrue(Arrays.equals(SchemaArt.build(restored),mixed),"Mixed build IDs/order did not survive serialization");
        h.assertTrue(SchemaArt.save(p,new int[]{1,2,0,0,0,0},1),"Valid Strength ID was rejected");near(h,SchemaArt.strengthBonus(SchemaArt.build(i),0,1),2.5,"Reordered slot did not attenuate Strength");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void strengthAddsToWeaponBaseAndCleansOnlyItsOwnModifier(GameTestHelper h){
        var p=player(h);p.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        var attack=p.getAttribute(Attributes.ATTACK_DAMAGE);var foreign=WildHunt.id("schema_foreign_strength_test");attack.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(foreign,2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));double before=attack.getValue();
        var i=learnTree(p,new int[]{2,2,2,2,2,2},false);for(int n=0;n<20;n++)SchemaArt.refresh(p);near(h,attack.getValue(),before+9.84375,"Flat Strength changed weapon stats or compounded");
        i.setMastery(2000);SchemaArt.refresh(p);near(h,attack.getValue(),before+30,"Six mastered Strength buffs");near(h,SchemaArt.damageFactor(p),1,"Strength reduced outgoing damage");near(h,SchemaArt.damageTakenFactor(p),1.12,"Six Strength vulnerability");
        i.onToggleOff(p);near(h,attack.getValue(),before,"Toggle left Strength");near(h,SchemaArt.damageTakenFactor(p),1,"Toggle left vulnerability");i.onToggleOn(p);SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());near(h,attack.getValue(),before,"Forget left Strength");h.assertTrue(attack.getModifier(foreign)!=null,"Forget removed foreign modifier");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedStrengthBoostsOnlyPositiveDamageOnce(GameTestHelper h){
        var p=player(h);ChainedGameTests.vow(p,true);double before=p.getAttributeValue(Attributes.ATTACK_DAMAGE);learnTree(p,new int[]{2,0,0,0,0,0},true);
        for(int n=0;n<20;n++)SchemaArt.refresh(p);near(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),before+15,"Chained compounded flat Strength");near(h,SchemaArt.damageTakenFactor(p),1.02,"Chained boosted vulnerability");
        SkillAPI.getSkillsFrom(p).getSkill(WildHunt.CHAINED.get()).orElseThrow().onToggleOff(p);SchemaArt.refresh(p);near(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),before+5,"Chained off lost base Strength or kept boost");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void strengthVulnerabilityAppliesToMeleeMagicProjectilesEnvironmentAndSoul(GameTestHelper h){
        var p=player(h);learn(p,1,true);var victim=target(h,p.position().add(3,0,0));victim.getAttribute(Attributes.ARMOR).setBaseValue(0);learnTree(victim,new int[]{2,0,0,0,0,0},true);
        victim.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(2000);TensuraStorages.getExistenceFrom(victim).setSpiritualHealth(1000);
        afterTracking(h,List.of(victim),()->{
            for(var source:List.of(p.damageSources().playerAttack(p),p.damageSources().arrow(new Arrow(EntityType.ARROW,h.getLevel()),p),p.damageSources().indirectMagic(p,p))){
                victim.invulnerableTime=0;float before=victim.getHealth();h.assertTrue(victim.hurt(source,100),"Mixed hit rejected: "+source);h.assertTrue(Math.abs((before-victim.getHealth())-96.9)<.001,"Outgoing reduction/incoming vulnerability double-counted: "+source+" delta="+(before-victim.getHealth()));
            }
            victim.invulnerableTime=0;float before=victim.getHealth();h.assertTrue(victim.hurt(victim.damageSources().fall(),100),"Ownerless fall rejected");h.assertTrue(Math.abs((before-victim.getHealth())-102)<.001,"Ownerless damage skipped vulnerability");
            h.assertTrue(TensuraDamageHelper.directSpiritualHurt(victim,p,null,100,0),"Soul hit rejected");h.assertTrue(Math.abs(TensuraStorages.getExistenceFrom(victim).getSpiritualHealth()-903.1)<.001,"Soul vulnerability/penalty applied incorrectly");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void strengthOnlyBuildTrainsOnLandedHitsWithFullAura(GameTestHelper h){
        var p=player(h);var i=learnTree(p,new int[]{2,0,0,0,0,0},false);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(EnergyHelper.getMaxAura(p));energy.setSleepModeTime(-1);var victim=target(h,p.position().add(3,0,0));victim.getAttribute(Attributes.ARMOR).setBaseValue(0);
        afterTracking(h,List.of(victim),()->{
            near(h,i.getMastery(),0,"Full aura trained an idle Strength build");double increment=p.getAttributeValue(TensuraAttributes.ABILITY_MASTERY_GAIN);
            h.assertTrue(victim.hurt(p.damageSources().playerAttack(p),100),"Training hit rejected");near(h,i.getMastery(),increment,"Landed Strength hit did not train");SchemaArt.trainStrength(p);near(h,i.getMastery(),increment,"Same tick trained twice");h.succeed();
        });
    }
}
