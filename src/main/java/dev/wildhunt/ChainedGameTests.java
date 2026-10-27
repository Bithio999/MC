package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.ability.*;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.registry.battlewill.*;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class ChainedGameTests {
    static ManasSkillInstance vow(net.minecraft.world.entity.LivingEntity p,boolean on){
        HunterData.learnReward(p,WildHunt.CHAINED.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.CHAINED.get()).orElseThrow();if(on)i.onToggleOn(p);return i;
    }
    static void near(GameTestHelper h,double actual,double expected,String text){h.assertTrue(Math.abs(actual-expected)<Math.max(.00001,Math.abs(expected)*1e-7),text+": "+actual+" != "+expected);}
    static boolean reincarnated(ServerPlayer p,ManasSkill skill){
        try{var m=ReincarnationMenu.class.getDeclaredMethod("grantedUniqueSkill",net.minecraft.world.entity.player.Player.class,net.minecraft.server.level.ServerLevel.class,ManasSkill.class,boolean.class);m.setAccessible(true);return (boolean)m.invoke(null,p,p.serverLevel(),skill,true);}
        catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void startingRaceHasFourNativeEvolutionsAndGuaranteedUnique(GameTestHelper h){
        var p=player(h);var menu=new ReincarnationMenu(0,p.getInventory(),p,false,0,0,0);
        h.assertTrue(Collections.frequency(menu.getRacePool(),WildHunt.RESTRICTED.getFirst().get())==1,"Starting race missing/duplicated");
        h.assertTrue(!ReincarnationMenu.getSkillPool().contains(WildHunt.CHAINED.get()),"Race-exclusive Chained entered ordinary Unique rolls");
        h.assertTrue(WildHunt.CHAINED.get().getColoredName().getString().equals("Chained"),"Colored name exposed a translation key");
        ReincarnationMenu.setRace(p,WildHunt.RESTRICTED.getFirst().get(),true,false);
        h.assertTrue(ChainedState.owned(p),"Race did not grant Chained");near(h,EnergyHelper.getMaxMagicule(p),500,"Initial native magicule pool was blocked before initialization");
        near(h,EnergyHelper.getMaxAura(p),2_000_000,"Starting aura capacity was not multiplied");
        for(int n=0;n<5;n++){var r=WildHunt.RESTRICTED.get(n).get();var i=r.createDefaultInstance();h.assertTrue(r.stage==n && (n==4?i.getNextEvolutions(p).isEmpty():i.getNextEvolutions(p).equals(List.of(WildHunt.RESTRICTED.get(n+1).get()))),"Broken lineage at "+n);h.assertTrue(dev.wildhunt.myths.RaceFamily.from(r).orElseThrow()==dev.wildhunt.myths.RaceFamily.HUMAN,"Human ancestry missing");}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void truePassiveCoversBaseModifierAndHelperGainsWhileOffAndUnslotted(GameTestHelper h){
        var p=player(h);EnergyHelper.setBaseMaxEP(p,2000);var i=vow(p,false);near(h,EnergyHelper.getMaxAura(p),100000,"Ownership multiplier");
        var ap=p.getAttribute(TensuraAttributes.MAX_AURA);ap.setBaseValue(1025);ap.addOrReplacePermanentModifier(new AttributeModifier(WildHunt.id("chained_test_external_ap"),30,AttributeModifier.Operation.ADD_VALUE));near(h,ap.getValue(),105500,"Modifier gain escaped 100x");
        var mp=p.getAttribute(TensuraAttributes.MAX_MAGICULE);double base=mp.getBaseValue();EnergyHelper.setMaxMagicule(p,10000);near(h,mp.getBaseValue(),base,"Helper raised raw max magicules");mp.setBaseValue(base+25);near(h,mp.getBaseValue(),base,"Direct setter raised magicules");
        mp.addOrReplacePermanentModifier(new AttributeModifier(WildHunt.id("chained_test_external_mp"),99999,AttributeModifier.Operation.ADD_VALUE));near(h,mp.getValue(),1000,"Modifier escaped magicule ceiling");
        h.assertTrue(!i.isToggled() && !WildHunt.CHAINED.get().isInSlot(p,i),"Fixture unexpectedly toggled/slotted");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeReincarnationAllowsMartialMasterAndHeavenlyEyeButBlocksUnrelatedGifts(GameTestHelper h){
        var p=player(h);vow(p,false);double before=EnergyHelper.getBaseMaxMagicule(p);
        h.assertTrue(reincarnated(p,UniqueSkills.MARTIAL_MASTER.get()),"Native reincarnation root rejected");
        h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(ExtraSkills.HEAVENLY_EYE.get()).isPresent() && ChainedState.inherited(p,ExtraSkills.HEAVENLY_EYE.get()),"Martial Master's real nested Heavenly Eye grant was blocked/untracked");
        h.assertTrue(!HunterData.learnReward(p,UniqueSkills.MUSICIAN.get()),"Unrelated Unique gift bypassed the vow");near(h,EnergyHelper.getBaseMaxMagicule(p),before,"Rejected gift consumed permanent magicules");
        var blocked=UniqueSkills.MUSICIAN.get().createDefaultInstance();SkillAPI.getSkillsFrom(p).updateSkill(blocked,true);h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.MUSICIAN.get()).isEmpty(),"Update API bypassed acquisition restriction");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void reincarnationBeforeChoosingRacePreservesItsDescendantProvenance(GameTestHelper h){
        var p=player(h);h.assertTrue(reincarnated(p,UniqueSkills.MARTIAL_MASTER.get()),"Root setup");vow(p,false);
        SkillAPI.getSkillsFrom(p).forgetSkill(ExtraSkills.HEAVENLY_EYE.get());h.assertTrue(HunterData.learnReward(p,ExtraSkills.HEAVENLY_EYE.get()),"Previously inherited derived skill cannot be relearned");
        var saved=HunterData.of(p).copy();HunterData.of(p).remove("chained_inheritance");HunterData.of(p).merge(saved);h.assertTrue(ChainedState.inherited(p,UniqueSkills.MARTIAL_MASTER.get()),"Saved ancestry lost");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void resistanceAndBattlewillRemainLearnableButOrdinaryExtraDoesNot(GameTestHelper h){
        var p=player(h);vow(p,false);
        h.assertTrue(HunterData.learnReward(p,UtilityArts.DIAMOND_PATH.get()),"Battlewill blocked");h.assertTrue(HunterData.learnReward(p,ResistanceSkills.POISON_RESISTANCE.get()),"Resistance blocked");h.assertTrue(HunterData.learnReward(p,ResistanceSkills.POISON_NULLIFICATION.get()),"Nullification blocked");h.assertTrue(!HunterData.learnReward(p,ExtraSkills.MAGIC_SENSE.get()),"Ordinary Extra acquired");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void learningBonusesAreSelectiveAndNativeMasteryAddsNine(GameTestHelper h){
        var p=player(h);var i=vow(p,false);var art=UtilityArts.DIAMOND_PATH.get();HunterData.learnReward(p,art);var a=SkillAPI.getSkillsFrom(p).getSkill(art).orElseThrow();p.getRandom().setSeed(123);double learning=art.getDefaultLearningIncrement(a,p,0);double mastery=p.getAttributeValue(TensuraAttributes.ABILITY_MASTERY_GAIN);
        i.onToggleOn(p);p.getRandom().setSeed(123);near(h,art.getDefaultLearningIncrement(a,p,0),learning+9,"Battlewill learning bonus");a.setMastery(0);a.addMasteryPoint(p);near(h,a.getMastery(),mastery+9,"Native Battlewill mastery bonus");
        var resist=ResistanceSkills.POISON_RESISTANCE.get();var r=resist.createDefaultInstance();r.setMastery(-100);resist.addLearnPoint(r,p,0,1);near(h,r.getMastery(),-80,"Native direct resistance learning did not receive +19");
        i.onToggleOff(p);r.setMastery(-100);resist.addLearnPoint(r,p,0,1);near(h,r.getMastery(),-99,"Resistance bonus leaked with toggle off");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void naturalMagiculeRegenAddsToAuraEvenWhenMagiculesAreFull(GameTestHelper h){
        var p=player(h);vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER).setBaseValue(3);energy.setAura(0);energy.setMagicule(EnergyHelper.getMaxMagicule(p));
        try{var method=io.github.manasmods.tensura.storage.ep.ExistenceStorage.class.getDeclaredMethod("handleMagiculeRegen",io.github.manasmods.tensura.storage.ep.IExistence.class,net.minecraft.world.entity.LivingEntity.class,double.class,double.class);method.setAccessible(true);method.invoke(null,energy,p,200d,EnergyHelper.getMaxMagicule(p));near(h,energy.getAura(),200*EnergyHelper.CONFIG.areaMagiculeRegen*3,"Full MP prevented converted regen");double mp=energy.getMagicule()/2;energy.setMagicule(mp);method.invoke(null,energy,p,200d,EnergyHelper.getMaxMagicule(p));near(h,energy.getMagicule(),mp,"Natural magicule regen was not converted");}
        catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void battlewillBuffTriplesOnceAndToggleCleanupPreservesItsNativeValue(GameTestHelper h){
        var p=player(h);var i=vow(p,false);var art=UtilityArts.DIAMOND_PATH.get();HunterData.learnReward(p,art);var a=SkillAPI.getSkillsFrom(p).getSkill(art).orElseThrow();double armor=p.getArmorValue();a.onPressed(p,0,0);double nativeBonus=p.getArmorValue()-armor;h.assertTrue(nativeBonus>0,"Native Diamond Path did not apply");
        i.onToggleOn(p);near(h,p.getArmorValue()-armor,nativeBonus*3,"Battlewill buff not tripled");ChainedState.refresh(p);near(h,p.getArmorValue()-armor,nativeBonus*3,"Refresh compounded buff");i.onToggleOff(p);near(h,p.getArmorValue()-armor,nativeBonus,"Toggle cleanup removed native/foreign armor");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void masteredToggleLowersNullificationOneTierAndBypassesDodgeOnlyWhileActive(GameTestHelper h){
        var p=player(h);var i=vow(p,true);i.setMastery(i.getMaxMastery());ChainedState.refresh(p);var source=WildHunt.CHAINED.get().createSource(i,p,TensuraDamageTypes.FIRE_ELEMENTAL,0);
        var resist=(ResistSkill)ResistanceSkills.HEAT_RESISTANCE.get();var nul=(ResistSkill)ResistanceSkills.HEAT_NULLIFICATION.get();h.assertTrue(resist.isResistanceBypass(source),"Resistance retained its tier");h.assertTrue(!nul.isNullificationBypass(source),"Chained erased nullification instead of lowering one tier");h.assertTrue(((TensuraDamageSource)source).tensura$isDodgeBypass(),"Live mastered source does not bypass native dodge");
        i.onToggleOff(p);h.assertTrue(!resist.isResistanceBypass(source) && !((TensuraDamageSource)source).tensura$isDodgeBypass(),"Mastered traits leaked after toggle off");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void slottedDodgeGuaranteesProjectilesAndLeavesOperatorDamageAvailable(GameTestHelper h){
        var p=player(h);var i=vow(p,false);TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,WildHunt.CHAINED.get(),0);var shooter=target(h,p.position().add(8,0,0));var arrow=new Arrow(h.getLevel(),shooter,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),null);var source=p.damageSources().arrow(arrow,shooter);
        h.assertTrue(!WildHunt.CHAINED.get().onBeingDamaged(i,p,source,10),"Slotted projectile dodge failed");h.assertTrue(WildHunt.CHAINED.get().onBeingDamaged(i,p,p.damageSources().genericKill(),10),"Operator damage was dodged");TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,null,0);h.assertTrue(WildHunt.CHAINED.get().onBeingDamaged(i,p,source,10),"Unslotted dodge remained");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void dashPaysOnceTravelsSixteenAndAddsArrivalDamageInsideNativeHurtCooldown(GameTestHelper h){
        // FakePlayer connections deliberately discard teleports; use a living native caster.
        var anchor=player(h);var p=target(h,anchor.position());EnergyHelper.setBaseMaxEP(p,2000);TensuraStorages.getExistenceFrom(p).setMagicule(1000);var i=vow(p,false);p.setYRot(-90);p.setXRot(0);Vec3 start=p.position();var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);var path=target(h,start.add(8,0,0));var arrival=target(h,start.add(16,0,0));var outside=target(h,start.add(8,0,4));
        afterTracking(h,List.of(p,path,arrival,outside),()->{double before=energy.getAura();WildHunt.CHAINED.get().onPressed(i,p,0,0);near(h,p.getX()-start.x,16,"Dash length");near(h,energy.getAura(),before*.98,"Dash charged more/less than 2% current aura");h.assertTrue(path.getHealth()<1024 && arrival.getHealth()<path.getHealth() && outside.getHealth()==1024,"Path/arrival double hit or radius incorrect");h.assertTrue(TensuraDamageHelper.isBattlewill(WildHunt.CHAINED.get().createSource(i,p,TensuraDamageTypes.AURA_SLASH,0),p),"Dash is not native Battlewill damage");double aura=energy.getAura();WildHunt.CHAINED.get().onPressed(i,p,0,0);near(h,energy.getAura(),aura,"Cooldown allowed another payment");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void dashStopsBeforeSolidWallsAndBlockedDashIsFree(GameTestHelper h){
        var anchor=player(h);var p=target(h,anchor.position());EnergyHelper.setBaseMaxEP(p,2000);TensuraStorages.getExistenceFrom(p).setMagicule(1000);var i=vow(p,false);p.setYRot(-90);p.setXRot(0);var start=p.position();var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);var wall=BlockPos.containing(start.add(5,0,0));for(int y=-1;y<4;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(wall.offset(0,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        WildHunt.CHAINED.get().onPressed(i,p,0,0);h.assertTrue(p.getX()>start.x && p.getX()<wall.getX(),"Dash crossed a solid wall");i.setCoolDown(0,0);double aura=energy.getAura();WildHunt.CHAINED.get().onPressed(i,p,0,0);near(h,energy.getAura(),aura,"Fully blocked dash spent aura");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void toggledBattlewillDamageAndAttackBonusesDoNotMultiplyUnrelatedSkills(GameTestHelper h){
        var p=player(h);var i=vow(p,false);double attack=p.getAttributeValue(Attributes.ATTACK_DAMAGE);i.onToggleOn(p);near(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),attack*3,"Attack attribute not tripled");var victim=target(h,p.position().add(3,0,0));var damage=Changeable.of(10f);ChainedState.damage(SkillAPI.getSkillsFrom(victim),victim,WildHunt.CHAINED.get().createSource(i,p,TensuraDamageTypes.AURA_SLASH,0),damage);near(h,damage.get(),80,"Dash not amplified as Battlewill");var ordinary=Changeable.of(10f);ChainedState.damage(SkillAPI.getSkillsFrom(victim),victim,p.damageSources().mobAttack(p),ordinary);near(h,ordinary.get(),10,"Ordinary melee received the separate Battlewill multiplier");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void forgettingRemovesOnlyChainedModifiersAndDoesNotCompoundOnReload(GameTestHelper h){
        var p=player(h);double aura=p.getAttributeValue(TensuraAttributes.MAX_AURA),attack=p.getAttributeValue(Attributes.ATTACK_DAMAGE);var i=vow(p,true);var restored=ManasSkillInstance.fromNBT(i.toNBT());SkillAPI.getSkillsFrom(p).updateSkill(restored,true);ChainedState.initialize(p);near(h,p.getAttributeValue(TensuraAttributes.MAX_AURA),aura*100,"Reload compounded capacity");SkillAPI.getSkillsFrom(p).forgetSkill(WildHunt.CHAINED.get());near(h,p.getAttributeValue(TensuraAttributes.MAX_AURA),aura,"Forgotten aura modifier persisted");near(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),attack,"Forgotten combat modifier persisted");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void evolutionMenuUsesFixedBaseEPAndActualMasteredBattlewills(GameTestHelper h){
        var p=player(h);EnergyHelper.setBaseMaxEP(p,2000);var i=vow(p,false);i.setMastery(i.getMaxMastery());var first=WildHunt.RESTRICTED.getFirst().get();var current=first.createDefaultInstance();var next=WildHunt.RESTRICTED.get(1).get();
        p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(98999);h.assertTrue(EnergyHelper.getMaxAura(p)>8_000_000 && first.getEvolutionProgress(current,p,next)<100,"Multiplied capacity bypassed fixed base-EP or training requirements");
        HunterData.learnReward(p,UtilityArts.DIAMOND_PATH.get());var art=SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.DIAMOND_PATH.get()).orElseThrow();art.setMastery(art.getMaxMastery());h.assertTrue(first.getEvolutionProgress(current,p,next)<100,"Sub-threshold base EP passed");p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(99000);near(h,first.getEvolutionProgress(current,p,next),100,"Native evolution requirements did not complete at the fixed threshold");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void auraSwordDamageBonusIsTripledWithoutChangingItsDuration(GameTestHelper h){
        var p=player(h);var i=vow(p,true);var art=MeleeArts.AURA_SWORD.get();HunterData.learnReward(p,art);var a=SkillAPI.getSkillsFrom(p).getSkill(art).orElseThrow();a.onPressed(p,0,0);var effect=p.getEffect(TensuraMobEffects.getReference(TensuraMobEffects.AURA_SWORD));h.assertTrue(effect!=null && ChainedBuffs.source(effect)==art,"Native Aura Sword source was not recorded");
        var victim=target(h,p.position().add(3,0,0));var source=p.damageSources().mobAttack(p);double weapon=io.github.manasmods.manascore.attribute.api.ManasCoreAttributeUtils.getWeaponDamage(p,victim,source);var config=io.github.manasmods.manascore.config.ConfigRegistry.getConfig(io.github.manasmods.tensura.config.ability.BattlewillConfig.class);
        double expected=(30+weapon*config.AuraSword.attackMultiplier*3)*8;var amount=Changeable.of(30f);SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.invoker().calculate(SkillAPI.getSkillsFrom(victim),victim,source,amount);near(h,amount.get(),expected,"Aura Sword incremental damage was not tripled before the Battlewill multiplier");h.assertTrue(effect.getAmplifier()==0,"Buff level/duration was changed instead of numeric effectiveness");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ordinaryResistanceAndNullificationAcquisitionSpendAuraWithoutReducingMagiculeCapacity(GameTestHelper h){
        var p=player(h);EnergyHelper.setBaseMaxEP(p,1000);vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10000);double mp=EnergyHelper.getBaseMaxMagicule(p),current=energy.getMagicule();
        double multiplier=p.level().getGameRules().getInt(io.github.manasmods.tensura.world.TensuraGameRules.MP_SKILL_COST)/100d;
        var resistance=ResistanceSkills.POISON_RESISTANCE.get();var nul=ResistanceSkills.POISON_NULLIFICATION.get();double cost=(resistance.getAcquiringMagiculeCost(resistance.createDefaultInstance())+nul.getAcquiringMagiculeCost(nul.createDefaultInstance()))*multiplier;
        h.assertTrue(SkillAPI.getSkillsFrom(p).learnSkill(resistance) && SkillAPI.getSkillsFrom(p).learnSkill(nul),"Normal acquisition could not use aura with only 500 max magicules");near(h,energy.getAura(),10000-cost,"Configured aura acquisition bill");near(h,EnergyHelper.getBaseMaxMagicule(p),mp,"Acquisition burned permanent magicules");near(h,energy.getMagicule(),current,"Acquisition spent current magicules");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void latentResistanceCompletionPaysAuraOnceAndRejectsAnUnfundedCompletion(GameTestHelper h){
        var p=player(h);vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);var skill=ResistanceSkills.POISON_RESISTANCE.get();var i=skill.createDefaultInstance();i.setMastery(-1);h.assertTrue(SkillAPI.getSkillsFrom(p).learnSkill(i),"Latent resistance setup");energy.setAura(0);
        double cost=skill.getDefaultAcquiringMagiculeCost()*p.level().getGameRules().getInt(io.github.manasmods.tensura.world.TensuraGameRules.MP_SKILL_COST)/100d;
        if(cost>0){skill.addLearnPoint(i,p,0,1);near(h,i.getMastery(),-1,"Unfunded completion was not rolled back");}
        energy.setAura(cost+100);double mp=EnergyHelper.getBaseMaxMagicule(p);skill.addLearnPoint(i,p,0,1);near(h,i.getMastery(),0,"Funded latent skill did not complete");near(h,energy.getAura(),100,"Learning completion did not charge exactly once");near(h,EnergyHelper.getBaseMaxMagicule(p),mp,"Learning completion spent permanent magicules");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void masteredStatusDegradationMatchesLowerTierAndDoesNotAffectAnotherCaster(GameTestHelper h){
        var p=player(h);var i=vow(p,false);i.setMastery(i.getMaxMastery());var recipient=target(h,p.position().add(4,0,0));var other=target(h,p.position().add(8,0,0));var nul=ResistanceSkills.POISON_NULLIFICATION.get();var n=nul.createDefaultInstance();n.setToggled(true);var effect=Changeable.of(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100));
        h.assertTrue(!nul.onEffectAdded(n,recipient,p,effect),"Native poison immunity fixture");i.onToggleOn(p);var lower=ResistanceSkills.POISON_RESISTANCE.get();boolean expected=!((ChainedResistanceView)lower).myths$immuneEffects(lower.createDefaultInstance(),recipient).contains(effect.get().getEffect());h.assertTrue(nul.onEffectAdded(n,recipient,p,effect)==expected,"Status immunity did not become its actual lower tier");h.assertTrue(!nul.onEffectAdded(n,recipient,other,effect),"Degradation leaked to another caster");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void existingSpiritualCapacityLimitCannotBeRemovedToGrowMagiculesAfterTheVow(GameTestHelper h){
        var p=player(h);p.getAttribute(TensuraAttributes.LIMITED_SPIRITUAL_MAX_MAGICULE).setBaseValue(100);vow(p,false);near(h,EnergyHelper.getMaxMagicule(p),100,"Effective ceiling setup");p.getAttribute(TensuraAttributes.LIMITED_SPIRITUAL_MAX_MAGICULE).setBaseValue(0);near(h,EnergyHelper.getMaxMagicule(p),100,"Removing a limit raised capacity beyond the original effective maximum");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeRaceChangesPreserveTheVowAndReplaceAllFourEvolutionBonuses(GameTestHelper h){
        var p=player(h);ReincarnationMenu.setRace(p,WildHunt.RESTRICTED.getFirst().get(),true,false);var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.CHAINED.get()).orElseThrow();i.setMastery(i.getMaxMastery());double mp=EnergyHelper.getMaxMagicule(p);double[] hp={0,100,300,600,1000};
        for(int n=1;n<5;n++){RaceAPI.getRaceFrom(p).setRace(WildHunt.RESTRICTED.get(n).get(),false);h.assertTrue(ChainedState.owned(p) && ChainedState.mastery(p)==1,"Evolution lost Chained or mastery");near(h,EnergyHelper.getMaxMagicule(p),mp,"Evolution raised the forbidden pool");near(h,p.getAttribute(Attributes.MAX_HEALTH).getModifier(WildHunt.id("restricted_vitality")).amount(),hp[n],"Racial health modifiers accumulated");}
        h.assertTrue(RaceAPI.getRaceFrom(p).getRace().orElseThrow().getNextEvolutions(p).isEmpty(),"Final race still has an evolution");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void inheritedUniquesCanSynthesizeAndTheirUltimateCanAscendThroughNativeSystems(GameTestHelper h){
        var p=player(h);vow(p,false);var first=WildHunt.UNIQUE.get(Apex.REY_DAU).get();var second=WildHunt.UNIQUE.get(Apex.NU_UDRA).get();h.assertTrue(reincarnated(p,first) && reincarnated(p,second),"Reincarnation ingredients rejected");
        var storage=SkillAPI.getSkillsFrom(p);for(var skill:List.of(first,second)){var i=storage.getSkill(skill).orElseThrow();i.setMastery(i.getMaxMastery());}
        MythicEvolution.synthesize(p,"vritra");var ultimate=WildHunt.MYTHIC.get(MythicKind.VRITRA).get();var result=storage.getSkill(ultimate).orElseThrow();h.assertTrue(ChainedState.inherited(p,ultimate) && storage.getSkill(first).isEmpty() && storage.getSkill(second).isEmpty(),"Synthesis lost inherited provenance or ingredients");result.setMastery(result.getMaxMastery());
        dev.wildhunt.myths.MythsProgression.ascend(p,"wildhunt:vritra","aion");h.assertTrue(storage.getSkill(WildHunt.GODLY.get()).isPresent() && ChainedState.inherited(p,WildHunt.GODLY.get()) && storage.getSkill(ultimate).isEmpty(),"Derived Godly ascension was blocked by the vow");h.succeed();
    }
}
