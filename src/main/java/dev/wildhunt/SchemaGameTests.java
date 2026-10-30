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
    public static void nativeActivationLearnsAuraSlashBeforeSchema(GameTestHelper h){
        var p=player(h);SchemaArt.discover(p);var schema=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.SCHEMA.get()).orElseThrow();
        var slash=(io.github.manasmods.tensura.ability.TensuraSkill)SkillAPI.getSkillRegistry().get(net.minecraft.resources.ResourceLocation.parse("tensura:aura_slash"));
        var auraSlash=slash.createLearningInstance(p);io.github.manasmods.tensura.ability.SkillHelper.learnSkill(p,auraSlash);
        p.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));TensuraStorages.getExistenceFrom(p).setAura(100000);
        var abilities=TensuraStorages.getAbilityFrom(p);abilities.setAbilitySlot(0,schema.getSkill(),0);abilities.setAbilitySlot(1,slash,0);
        var config=io.github.manasmods.tensura.ability.TensuraSkill.BASE_CONFIG.Learning;int min=config.minBonus,max=config.maxBonus;double chance=config.failingPenaltyChance;
        try{
            config.minBonus=0;config.maxBonus=0;config.failingPenaltyChance=0;p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN).setBaseValue(1);
            for(int n=0;n<1000;n++){
                schema.setCoolDown(0,0);io.github.manasmods.manascore.skill.api.SkillEvents.ACTIVATE_SKILL.invoker().activateSkill(io.github.manasmods.manascore.network.api.util.Changeable.of(schema),p,0,0);
                if(n<100){auraSlash.setCoolDown(0,0);io.github.manasmods.manascore.skill.api.SkillEvents.ACTIVATE_SKILL.invoker().activateSkill(io.github.manasmods.manascore.network.api.util.Changeable.of(auraSlash),p,1,0);}
                if(n==99){near(h,auraSlash.getMastery(),0,"Aura Slash no longer learns in its native 100 points");near(h,schema.getMastery(),-900,"Schema learned as quickly as Aura Slash");}
                if(n==998)near(h,schema.getMastery(),-1,"Schema completed before 1000 points");
            }
            near(h,schema.getMastery(),0,"Schema did not finish at exactly 1000 points");
        }finally{config.minBonus=min;config.maxBonus=max;config.failingPenaltyChance=chance;}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeFailedLearningCannotSkipSchemasThousandPoints(GameTestHelper h){
        var p=player(h);SchemaArt.discover(p);var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.SCHEMA.get()).orElseThrow();
        var config=io.github.manasmods.tensura.ability.TensuraSkill.BASE_CONFIG.Learning;
        int min=config.minBonus,max=config.maxBonus,penaltyMin=config.failingPenaltyMin,penaltyMax=config.failingPenaltyMax,level=config.failingPenaltyLevel;
        double chance=config.failingPenaltyChance;
        try{
            config.minBonus=0;config.maxBonus=0;config.failingPenaltyChance=1;config.failingPenaltyMin=1;config.failingPenaltyMax=1;config.failingPenaltyLevel=1;
            p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN).setBaseValue(1);p.getRandom().setSeed(123);
            TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,i.getSkill(),0);
            for(int n=0;n<20;n++){
                i.setCoolDown(0,0);p.removeAllEffects();
                io.github.manasmods.manascore.skill.api.SkillEvents.ACTIVATE_SKILL.invoker().activateSkill(io.github.manasmods.manascore.network.api.util.Changeable.of(i),p,0,0);
                h.assertTrue(i.getMastery()<=-1000+n+1,"Native failure skipped learning points: "+i.getMastery());
                h.assertTrue(i.getMastery()>=-1000,"Failure lost more than the initial learning requirement");
            }
            h.assertTrue(i.getMastery()<-980,"Fixture never applied a learning failure");
        }finally{config.minBonus=min;config.maxBonus=max;config.failingPenaltyChance=chance;config.failingPenaltyMin=penaltyMin;config.failingPenaltyMax=penaltyMax;config.failingPenaltyLevel=level;}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void masteryCompletesAtTwoThousandAndNeverEarlier(GameTestHelper h){
        var p=player(h);SchemaArt.discover(p);var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.SCHEMA.get()).orElseThrow();
        WildHunt.SCHEMA.get().addLearnPoint(i,p,0,1000);i.setMastery(1999);
        h.assertTrue(!i.isMastered(p),"Schema mastered before 2000 points");
        WildHunt.SCHEMA.get().addMasteryPoint(i,p,1);near(h,i.getMastery(),2000,"Mastery did not finish at 2000");
        h.assertTrue(i.isMastered(p),"2000 points did not master Schema");WildHunt.SCHEMA.get().addMasteryPoint(i,p,20);near(h,i.getMastery(),2000,"Mastery exceeded its cap");h.succeed();
    }
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
        h.assertTrue(!SchemaArt.save(p,new int[7],0) && !SchemaArt.save(p,new int[]{4,0,0,0,0,0},0),"Invalid build accepted");
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

    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeWeightsShareAllSlotsAndPersistTheNewEffect(GameTestHelper h){
        int[] six={3,3,3,3,3,3},mixed={3,1,2,3,1,2};near(h,SchemaArt.healingBonus(six,0,1),.39375,"Initial healing weights");near(h,SchemaArt.healingBonus(six,2000,1),1.2,"Mastered healing weights");near(h,SchemaArt.costPenalty(six),.3,"Cost drawback diminished");
        near(h,SchemaArt.healingBonus(mixed,0,1),.225,"Mixed healing order");near(h,SchemaArt.bonus(mixed,0,1),2.8125,"Mixed Mito order");near(h,SchemaArt.strengthBonus(mixed,0,1),1.40625,"Mixed Strength order");near(h,SchemaArt.costPenalty(mixed),.1,"Mixed costs");
        var p=player(h);var i=learnTree(p,mixed,true);h.assertTrue(Arrays.equals(SchemaArt.build(ManasSkillInstance.fromNBT(i.toNBT())),mixed),"Third effect lost on save");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeBoostsHealAndDirectHpRestoresOnceButHonorsCancellation(GameTestHelper h){
        var p=player(h);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);p.setHealth(40);learnTree(p,new int[]{3,0,0,0,0,0},true);
        p.heal(10);near(h,p.getHealth(),52,"heal() was not boosted exactly once");p.setHealth(62);near(h,p.getHealth(),64,"Direct HP restore not boosted");p.setHealth(40);near(h,p.getHealth(),40,"HP loss was altered");
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingHealEvent> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{p.heal(10);near(h,p.getHealth(),40,"Canceled healing bypassed suppression");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        p.setHealth(p.getMaxHealth());p.setHealth(p.getMaxHealth()-1);p.heal(10);near(h,p.getHealth(),p.getMaxHealth(),"Healing bonus exceeded max HP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeBoostsNativeSpiritualRegenAndDirectRestoresWithACap(GameTestHelper h){
        var p=player(h);p.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(200);p.getAttribute(TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION).setBaseValue(2);var e=TensuraStorages.getExistenceFrom(p);e.setSpiritualHealth(50);learnTree(p,new int[]{3,0,0,0,0,0},true);
        e.setSpiritualHealth(60);near(h,e.getSpiritualHealth(),62,"Direct SHP restore");
        try{var m=ExistenceStorage.class.getDeclaredMethod("handleSpiritualHealthRegen",net.minecraft.server.MinecraftServer.class,LivingEntity.class,IExistence.class);m.setAccessible(true);m.invoke(null,h.getLevel().getServer(),p,e);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        near(h,e.getSpiritualHealth(),64.4,"Native SHP regeneration");e.setSpiritualHealth(40);near(h,e.getSpiritualHealth(),40,"SHP damage was altered");double max=p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);e.setSpiritualHealth(max);e.setSpiritualHealth(max-1);e.setSpiritualHealth(max);near(h,e.getSpiritualHealth(),max,"SHP bonus exceeded native cap");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeCostsAreAtomicAndTheConsumingOverloadsChargeOnce(GameTestHelper h){
        var p=player(h);learnTree(p,new int[]{3,0,0,0,0,0},true);var e=TensuraStorages.getExistenceFrom(p);e.setAura(1000);e.setMagicule(1000);
        h.assertTrue(!EnergyHelper.isOutOfEnergy(p,50,80),"Funded native payment rejected");near(h,e.getAura(),947.5,"Aura charged incorrectly");near(h,e.getMagicule(),916,"Magicules charged incorrectly");
        near(h,EnergyHelper.isOutOfAuraConsuming(p,100),0,"Funded aura consumer");near(h,e.getAura(),842.5,"Delegating aura overload double-taxed");near(h,EnergyHelper.isOutOfMagiculeConsuming(p,100,10),0,"Funded MP consumer");near(h,e.getMagicule(),811,"MP consumer surcharge");
        e.setAura(100);e.setMagicule(1000);h.assertTrue(EnergyHelper.isOutOfEnergy(p,100,10),"Surcharge affordability skipped");near(h,e.getAura(),100,"Failed cast spent aura");near(h,e.getMagicule(),1000,"Failed cast spent magicules");
        near(h,EnergyHelper.isOutOfAuraConsuming(p,100,10),15/1.05,"Partial-consumption shortfall");near(h,e.getAura(),10,"Consumer ignored reserved minimum");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeDoesNotTaxFreeActionsHostileDrainsOrCreativeCosts(GameTestHelper h){
        var p=player(h);learnTree(p,new int[]{3,0,0,0,0,0},true);var e=TensuraStorages.getExistenceFrom(p);e.setAura(1000);e.setMagicule(1000);
        h.assertTrue(!EnergyHelper.isOutOfEnergy(p,0,0),"Free action blocked");near(h,e.getAura(),1000,"Free action cost aura");
        h.assertTrue(EnergyHelper.drainEnergy(p,p,100,false,EnergyHelper.DrainType.AURA,EnergyHelper.GainType.NONE),"Drain fixture failed");near(h,e.getAura(),900,"A hostile drain was treated as a cost");
        e.setAura(910);e.setMagicule(1010);near(h,e.getAura(),910,"Resource gain altered");near(h,e.getMagicule(),1010,"Magicule gain altered");
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);h.assertTrue(!EnergyHelper.isOutOfEnergy(p,100,100),"Creative cost blocked");near(h,e.getAura(),910,"Creative paid aura");near(h,e.getMagicule(),1010,"Creative paid MP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedBoostsRegenerativeHealingButNeverItsResourcePenalty(GameTestHelper h){
        var p=player(h);ChainedGameTests.vow(p,true);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);p.setHealth(40);learnTree(p,new int[]{3,0,0,0,0,0},true);
        near(h,SchemaArt.healingFactor(p),1.6,"Chained healing multiplier");near(h,SchemaArt.costFactor(p),1.05,"Chained magnified the drawback");p.heal(10);near(h,p.getHealth(),56,"Chained healing double-boosted");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeSurchargeAlsoAppliesToManualDashAndCounterPayments(GameTestHelper h){
        var p=player(h);var dash=ChainedGameTests.vow(p,false);learnTree(p,new int[]{3,0,0,0,0,0},true);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);p.setOnGround(false);p.setYRot(-90);p.setXRot(-15);WildHunt.CHAINED.get().onPressed(dash,p,0,0);near(h,energy.getAura(),979,"Manual dash did not pay 2.1 percent");
        var q=player(h);HunterData.learnReward(q,WildHunt.UNBOUND.get());var counter=SkillAPI.getSkillsFrom(q).getSkill(WildHunt.UNBOUND.get()).orElseThrow();counter.setMastery(counter.getMaxMastery());learnTree(q,new int[]{3,0,0,0,0,0},true);var qe=TensuraStorages.getExistenceFrom(q);qe.setAura(1000);UnboundCounter.press(counter,q);h.assertTrue(UnboundCounter.active(q),"Counter did not enter stance");near(h,qe.getAura(),895,"Manual counter did not pay 10.5 percent");UnboundCounter.press(counter,q);near(h,qe.getAura(),895,"Cancel charged twice");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void actualHealingTrainsRegenerativeAndToggleForgetRemoveItsEffects(GameTestHelper h){
        var p=player(h);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);p.setHealth(40);var i=learnTree(p,new int[]{3,0,0,0,0,0},false);double gain=p.getAttributeValue(TensuraAttributes.ABILITY_MASTERY_GAIN);
        p.heal(10);near(h,i.getMastery(),gain,"Actual healing did not train");p.heal(10);near(h,i.getMastery(),gain,"Same-tick healing trained twice");i.onToggleOff(p);p.setHealth(40);p.heal(10);near(h,p.getHealth(),50,"Toggle left healing bonus");near(h,SchemaArt.costFactor(p),1,"Toggle left cost surcharge");
        i.onToggleOn(p);SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());p.setHealth(40);p.heal(10);near(h,p.getHealth(),50,"Forget left healing bonus");near(h,SchemaArt.costFactor(p),1,"Forget left cost surcharge");h.assertTrue(!SchemaArt.healingActive(p),"Forget left constructor fast-path flag");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativePartialBillsPreserveTheOriginalRecoveryUnits(GameTestHelper h){
        var p=player(h);learnTree(p,new int[]{3,0,0,0,0,0},true);var e=TensuraStorages.getExistenceFrom(p);
        e.setMagicule(0);near(h,EnergyHelper.isOutOfMagiculeConsuming(p,100),100,"An empty reserve returned an excessive base-unit shortfall");near(h,e.getMagicule(),0,"Empty reserve went negative");
        e.setMagicule(50);double missing=EnergyHelper.isOutOfMagiculeConsuming(p,100);near(h,100-missing,50/1.05,"Underfunded recovery did not match purchased base units");near(h,e.getMagicule(),0,"Partial MP payment did not exhaust only the available amount");
        e.setAura(25);missing=EnergyHelper.isOutOfAuraConsuming(p,100);near(h,100-missing,25/1.05,"Partial aura recovery units");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void masterySharesOneThirtySecondIntervalAcrossEveryBuild(GameTestHelper h){
        var p=player(h);var i=learnTree(p,new int[]{3,1,2,0,0,0},false);double gain=p.getAttributeValue(TensuraAttributes.ABILITY_MASTERY_GAIN);long now=p.level().getGameTime();
        SchemaArt.train(p,1);near(h,i.getMastery(),gain,"First qualifying recovery did not train");
        i.getOrCreateTag().putLong("SchemaLastTraining",now-599);SchemaArt.train(p,1);SchemaArt.trainHealing(p,1);SchemaArt.trainStrength(p);near(h,i.getMastery(),gain,"Training occurred before 30 seconds");
        i.getOrCreateTag().putLong("SchemaLastTraining",now-600);SchemaArt.trainHealing(p,1);near(h,i.getMastery(),2*gain,"Training did not resume at 600 ticks");SchemaArt.trainStrength(p);SchemaArt.train(p,1);near(h,i.getMastery(),2*gain,"Different effects bypassed the shared interval");
        SchemaArt.save(p,new int[]{2,3,1,0,0,0},1);i.onToggleOff(p);i.onToggleOn(p);SchemaArt.trainStrength(p);near(h,i.getMastery(),2*gain,"Editing/toggling reset the timer");
        var restored=ManasSkillInstance.fromNBT(i.toNBT());h.assertTrue(restored.getOrCreateTag().getLong("SchemaLastTraining")==now,"Serialization lost the last training time");h.succeed();
    }

}
