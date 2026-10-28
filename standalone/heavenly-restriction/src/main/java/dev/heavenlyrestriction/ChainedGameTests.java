package dev.heavenlyrestriction;

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
import static dev.heavenlyrestriction.TestFixtures.*;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class ChainedGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void auraGainRateCountsAsBaseEPWhileChainedCapacityBonusCountsAsEffectiveEP(GameTestHelper h){
        // Reproduce Restricted 2.2.1's +2 TOTAL modifier without loading that addon.
        var p=player(h);var victim=target(h,p.position().add(5,0,0));EnergyHelper.setBaseMaxEP(p,2000);
        var gain=p.getAttribute(TensuraAttributes.AURA_GAIN);gain.setBaseValue(1);
        double before=EnergyHelper.getBaseMaxAura(p);
        io.github.manasmods.tensura.handler.DeathHandler.gainEPEntity(p,victim,10);
        double ordinary=EnergyHelper.getBaseMaxAura(p)-before;h.assertTrue(ordinary>0,"Native reference award gave no aura");
        EnergyHelper.setBaseMaxEP(p,2000);gain.addOrReplacePermanentModifier(new AttributeModifier(HeavenlyRestriction.id("restricted_reference_gain"),2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        double base=EnergyHelper.getBaseMaxEP(p),total=EnergyHelper.getMaxEP(p),aura=EnergyHelper.getBaseMaxAura(p),mp=EnergyHelper.getBaseMaxMagicule(p);
        io.github.manasmods.tensura.handler.DeathHandler.gainEPEntity(p,victim,10);
        double auraGain=EnergyHelper.getBaseMaxAura(p)-aura,mpGain=EnergyHelper.getBaseMaxMagicule(p)-mp;
        near(h,auraGain,ordinary*3,"Restricted-style gain multiplier did not enter raw aura capacity");
        near(h,EnergyHelper.getBaseMaxEP(p)-base,auraGain+mpGain,"Boosted gain missing from base EP");
        near(h,EnergyHelper.getMaxEP(p)-total,auraGain+mpGain,"Boosted gain missing from total EP");
        var chained=player(h);ReincarnationMenu.setRace(chained,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        double start=EnergyHelper.getBaseMaxAura(chained),baseBefore=EnergyHelper.getBaseMaxEP(chained),totalBefore=EnergyHelper.getMaxEP(chained);
        EnergyHelper.setMaxAura(chained,start+100);
        near(h,EnergyHelper.getBaseMaxEP(chained)-baseBefore,100,"Chained changed raw EP accounting");
        near(h,EnergyHelper.getMaxEP(chained)-totalBefore,500,"Chained capacity bonus missing from total EP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void restrictedEvolutionsGrantOnlyTheirRecoveryTiersThroughTheVow(GameTestHelper h){
        var p=player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        var skills=SkillAPI.getSkillsFrom(p);double mp=EnergyHelper.getMaxMagicule(p);
        // Keep this fixture free of any pre-existing recovery grants.
        for(var recovery:HeavenlyRestriction.REGENERATION)skills.forgetSkill(recovery.get());
        for(int stage=1;stage<=4;stage++){
            var race=HeavenlyRestriction.RESTRICTED.get(stage).get();RaceAPI.getRaceFrom(p).setRace(race,false);
            race.learnIntrinsicSkills(race.createDefaultInstance(),p);
            for(int tier=1;tier<=3;tier++){
                var recovery=HeavenlyRestriction.REGENERATION.get(tier-1).get();var learned=skills.getSkill(recovery);
                h.assertTrue(learned.isPresent()==(tier<stage),"Wrong recovery tier at evolution "+stage+", tier "+tier);
                if(learned.isPresent())h.assertTrue(learned.get().isToggled(),"New recovery was not enabled");
                h.assertTrue(!ChainedState.inherited(p,recovery),"Racial recovery incorrectly became a reincarnation root");
            }
            near(h,EnergyHelper.getMaxMagicule(p),mp,"Racial recovery spent magicule capacity");
            h.assertTrue(!RestrictionData.learnReward(p,ExtraSkills.MAGIC_SENSE.get()),"Racial exemption leaked to unrelated Extras");
        }
        var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10000);energy.setMagicule(0);
        energy.setSpiritualHealth(p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)-10);p.setHealth(p.getMaxHealth()-20);p.tickCount=20;
        AuraRegenerationSkill.tick(p);
        near(h,p.getHealth(),p.getMaxHealth(),"Restricted Sovereign Rebirth did not heal HP");
        near(h,energy.getSpiritualHealth(),p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),"Restricted Sovereign Rebirth did not heal SHP");
        near(h,energy.getAura(),9300,"Recovery tiers stacked or used the wrong cost");near(h,energy.getMagicule(),0,"Aura recovery used magicules");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void existingRestrictedRecoveryMigrationPreservesMasteryAndDisabledToggles(GameTestHelper h){
        var p=player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        var race=HeavenlyRestriction.RESTRICTED.getLast().get();RaceAPI.getRaceFrom(p).setRace(race,false);
        var storage=SkillAPI.getSkillsFrom(p);
        // Recreate an existing save that reached the last stage before recovery was added.
        for(var skill:HeavenlyRestriction.REGENERATION)storage.forgetSkill(skill.get());
        race.learnIntrinsicSkills(race.createDefaultInstance(),p);
        var existing=storage.getSkill(HeavenlyRestriction.REGENERATION.getFirst().get()).orElseThrow();existing.setMastery(73);existing.onToggleOff(p);
        storage.forgetSkill(HeavenlyRestriction.REGENERATION.get(1).get());storage.forgetSkill(HeavenlyRestriction.REGENERATION.getLast().get());
        RestrictionData.of(p).remove("restricted_recovery_stage");RestrictedRace.migrateRecovery(p);
        for(var skill:HeavenlyRestriction.REGENERATION)h.assertTrue(storage.getSkill(skill.get()).isPresent(),"Migration missed a lower recovery tier");
        var first=storage.getSkill(HeavenlyRestriction.REGENERATION.getFirst().get()).orElseThrow();near(h,first.getMastery(),73,"Migration replaced existing mastery");h.assertTrue(!first.isToggled(),"Migration enabled a previously disabled tier");
        RestrictedRace.migrateRecovery(p);near(h,first.getMastery(),73,"Migration reset mastery");h.assertTrue(!first.isToggled(),"Migration re-enabled an existing disabled tier");
        var second=player(h);ReincarnationMenu.setRace(second,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        for(var recovery:HeavenlyRestriction.REGENERATION)SkillAPI.getSkillsFrom(second).forgetSkill(recovery.get());
        h.assertTrue(!RestrictionData.learnReward(second,HeavenlyRestriction.REGENERATION.getLast().get()),"Recovery exemption escaped its scope or stage");h.succeed();
    }
    private static final class MessagePlayer extends net.neoforged.neoforge.common.util.FakePlayer {
        final java.util.List<net.minecraft.network.chat.Component> messages=new java.util.ArrayList<>();
        MessagePlayer(net.minecraft.server.level.ServerLevel level){super(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ChainedGateTest"));}
        @Override public void sendSystemMessage(net.minecraft.network.chat.Component message){if(messages!=null)messages.add(message);}
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void blockedAcquisitionExplainsChainedWithoutChargingOrSpamming(GameTestHelper h){
        var p=new MessagePlayer(h.getLevel());EnergyHelper.setBaseMaxEP(p,10000);vow(p,false);p.messages.clear();
        var storage=SkillAPI.getSkillsFrom(p);var energy=TensuraStorages.getExistenceFrom(p);double maxMP=EnergyHelper.getBaseMaxMagicule(p),aura=energy.getAura(),magicules=energy.getMagicule();
        var blocked=UniqueSkills.MUSICIAN.get();h.assertTrue(!storage.learnSkill(blocked.createDefaultInstance()),"Blocked Unique was learned");
        h.assertTrue(p.messages.size()==1 && p.messages.getFirst().getString().contains("Chained") && p.messages.getFirst().getString().contains(blocked.getName().getString()),"Missing named Chained rejection message");
        storage.learnSkill(blocked.createDefaultInstance());storage.updateSkill(blocked.createDefaultInstance(),true);h.assertTrue(p.messages.size()==1,"Repeated same-skill failures spammed chat");
        var extra=ExtraSkills.MAGIC_SENSE.get();storage.updateSkill(extra.createDefaultInstance(),true);h.assertTrue(p.messages.size()==2 && p.messages.getLast().getString().contains(extra.getName().getString()),"Update-path rejection had no named explanation");
        var latent=ExtraSkills.HEAVENLY_EYE.get().createDefaultInstance();latent.setMastery(-1);ExtraSkills.HEAVENLY_EYE.get().addLearnPoint(latent,p,0,1);h.assertTrue(p.messages.size()==3 && latent.getMastery()<0,"Latent learning rejection was silent or completed");
        var message=Changeable.of(net.minecraft.network.chat.Component.empty());ChainedState.unlock(blocked.createDefaultInstance(),p,message);h.assertTrue(message.isPresent() && message.get().getString().contains("Chained"),"Unlock event did not provide the custom reason");
        h.assertTrue(storage.getSkill(blocked).isEmpty() && storage.getSkill(extra).isEmpty(),"Blocked skill entered storage");near(h,EnergyHelper.getBaseMaxMagicule(p),maxMP,"Rejected skill spent permanent MP");near(h,energy.getAura(),aura,"Rejected skill spent aura");near(h,energy.getMagicule(),magicules,"Rejected skill spent current MP");
        int messages=p.messages.size();h.assertTrue(RestrictionData.learnReward(p,UtilityArts.DIAMOND_PATH.get()) && RestrictionData.learnReward(p,ResistanceSkills.POISON_RESISTANCE.get()),"Allowed acquisition failed");h.assertTrue(p.messages.subList(messages,p.messages.size()).stream().noneMatch(m->m.getString().startsWith("Chained blocks")),"Allowed acquisition emitted a rejection");h.succeed();
    }
    static ManasSkillInstance vow(net.minecraft.world.entity.LivingEntity p,boolean on){
        RestrictionData.learnReward(p,HeavenlyRestriction.CHAINED.get());var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();if(on)i.onToggleOn(p);return i;
    }
    static void near(GameTestHelper h,double actual,double expected,String text){h.assertTrue(Math.abs(actual-expected)<Math.max(.00001,Math.abs(expected)*1e-7),text+": "+actual+" != "+expected);}
    static boolean reincarnated(ServerPlayer p,ManasSkill skill){
        try{var m=ReincarnationMenu.class.getDeclaredMethod("grantedUniqueSkill",net.minecraft.world.entity.player.Player.class,net.minecraft.server.level.ServerLevel.class,ManasSkill.class,boolean.class);m.setAccessible(true);return (boolean)m.invoke(null,p,p.serverLevel(),skill,true);}
        catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void startingRaceHasFourNativeEvolutionsAndGuaranteedUnique(GameTestHelper h){
        var p=player(h);var menu=new ReincarnationMenu(0,p.getInventory(),p,false,0,0,0);
        h.assertTrue(Collections.frequency(menu.getRacePool(),HeavenlyRestriction.RESTRICTED.getFirst().get())==1,"Starting race missing/duplicated");
        h.assertTrue(!ReincarnationMenu.getSkillPool().contains(HeavenlyRestriction.CHAINED.get()),"Race-exclusive Chained entered ordinary Unique rolls");
        h.assertTrue(HeavenlyRestriction.CHAINED.get().getColoredName().getString().equals("Chained"),"Colored name exposed a translation key");
        ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        h.assertTrue(ChainedState.owned(p),"Race did not grant Chained");
        double aura=EnergyHelper.getBaseMaxAura(p),mp=EnergyHelper.getBaseMaxMagicule(p);
        h.assertTrue(aura>=1500 && aura<=2000 && mp>=50 && mp<=100,"Starting roll outside the new ranges");
        near(h,EnergyHelper.getMaxAura(p),aura,"Chained multiplied the starting aura");near(h,EnergyHelper.getMaxMagicule(p),mp,"Initial native magicule pool was blocked before initialization");
        near(h,TensuraStorages.getExistenceFrom(p).getAura(),aura,"Starting current aura differs from its native roll");
        near(h,EnergyHelper.getMaxEP(p),aura+mp,"Starting total EP includes a phantom multiplier");
        for(int n=0;n<5;n++){var r=HeavenlyRestriction.RESTRICTED.get(n).get();var i=r.createDefaultInstance();h.assertTrue(r.stage==n && (n==4?i.getNextEvolutions(p).isEmpty():i.getNextEvolutions(p).equals(List.of(HeavenlyRestriction.RESTRICTED.get(n+1).get()))),"Broken lineage at "+n);h.assertTrue(i.is(io.github.manasmods.tensura.data.TensuraRaceTags.HUMAN_LIKE),"Native human-like classification missing");}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void startingRollStaysUnmultipliedWhileSubsequentSourcesGetFiveTimes(GameTestHelper h){
        var p=player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        double start=EnergyHelper.getBaseMaxAura(p);var ap=p.getAttribute(TensuraAttributes.MAX_AURA);
        EnergyHelper.setMaxAura(p,start+100);near(h,EnergyHelper.getMaxAura(p),start+500,"Helper gain not multiplied independently of the birth pool");
        var equipment=HeavenlyRestriction.id("chained_post_birth_equipment");ap.addOrReplacePermanentModifier(new AttributeModifier(equipment,50,AttributeModifier.Operation.ADD_VALUE));
        near(h,EnergyHelper.getMaxAura(p),start+750,"Equipment gain not multiplied");
        var multiplier=HeavenlyRestriction.id("chained_foreign_capacity_multiplier");ap.addOrReplacePermanentModifier(new AttributeModifier(multiplier,1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        near(h,EnergyHelper.getMaxAura(p),(start+750)*2,"Foreign multiplier did not preserve the starting exemption");
        ap.removeModifier(multiplier);ap.removeModifier(equipment);near(h,EnergyHelper.getMaxAura(p),start+500,"Removing equipment inflated the baseline");
        var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();
        var restored=ManasSkillInstance.fromNBT(i.toNBT());SkillAPI.getSkillsFrom(p).updateSkill(restored,true);ChainedState.initialize(p);
        near(h,EnergyHelper.getMaxAura(p),start+500,"Reload recaptured earned aura as starting aura");
        i=restored;i.onToggleOff(p);i.onToggleOn(p);ChainedState.refresh(p);near(h,EnergyHelper.getMaxAura(p),start+500,"Toggle/refresh changed the exemption");
        SkillAPI.getSkillsFrom(p).forgetSkill(HeavenlyRestriction.CHAINED.get());near(h,EnergyHelper.getMaxAura(p),start+100,"Forgetting left an exemption modifier");
        vow(p,false);near(h,EnergyHelper.getMaxAura(p),start+500,"Relearning recaptured earned aura or lost the starting exemption");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void repeatedNativeBirthsRollBothNewRangesAndResetOnlyTheirStartingExemption(GameTestHelper h){
        var p=player(h);var race=HeavenlyRestriction.RESTRICTED.getFirst().get();var rolls=new HashSet<Double>();
        near(h,race.getMinBaseAura(),1500,"Aura lower range");near(h,race.getMaxBaseAura(),2000,"Aura upper range");near(h,race.getMinBaseMagicule(),50,"MP lower range");near(h,race.getMaxBaseMagicule(),100,"MP upper range");
        for(int n=0;n<8;n++){
            p.getRandom().setSeed(n*7919+37);ReincarnationMenu.setRace(p,race,true,false);
            double a=EnergyHelper.getBaseMaxAura(p),m=EnergyHelper.getBaseMaxMagicule(p);rolls.add(a);
            h.assertTrue(a>=1500 && a<=2000 && m>=50 && m<=100,"Rebirth roll outside range");
            near(h,EnergyHelper.getMaxAura(p),a,"Rebirth retained a stale or multiplied starting pool");near(h,EnergyHelper.getMaxMagicule(p),m,"Rebirth retained the preceding magicule ceiling");
            EnergyHelper.setMaxAura(p,a+25);near(h,EnergyHelper.getMaxAura(p),a+125,"Post-rebirth aura gain not multiplied");
        }
        h.assertTrue(rolls.size()>1,"Starting aura range never produced different rolls");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void oldLineageOwnerExcludesHistoricalBirthAuraWithoutRerollingEarnedEP(GameTestHelper h){
        var p=player(h);RaceAPI.getRaceFrom(p).setRace(HeavenlyRestriction.RESTRICTED.get(2).get(),false);
        EnergyHelper.setMaxAura(p,25_000);var i=vow(p,false);i.setMastery(700);
        RestrictionData.of(p).remove("chained_starting_aura");i.getOrCreateTag().remove("starting_aura");
        p.getAttribute(TensuraAttributes.MAX_AURA).removeModifier(ChainedState.START);
        ChainedState.initialize(p);near(h,EnergyHelper.getBaseMaxAura(p),25_000,"Migration discarded earned aura");
        near(h,EnergyHelper.getMaxAura(p),20_000+5*5000,"Migration multiplied the legacy birth pool or exempted earned aura");near(h,i.getMastery(),700,"Migration reset mastery");
        ChainedState.refresh(p);near(h,EnergyHelper.getMaxAura(p),45_000,"Repeated refresh compounded migration");
        RaceAPI.getRaceFrom(p).setRace(io.github.manasmods.tensura.registry.race.TensuraRaces.HUMAN.get(),false);
        RestrictionData.of(p).remove("chained_starting_aura");i.getOrCreateTag().remove("starting_aura");ChainedState.initialize(p);
        near(h,EnergyHelper.getMaxAura(p),45_000,"Changing race prevented a legacy Chained owner from migrating");
        RestrictionData.of(p).remove("chained_starting_aura");i.getOrCreateTag().remove("starting_aura");EnergyHelper.setMaxAura(p,10_000);
        near(h,EnergyHelper.getMaxAura(p),10_000,"Legacy loss produced a negative starting exemption");
        EnergyHelper.setMaxAura(p,15_000);near(h,EnergyHelper.getMaxAura(p),35_000,"Legacy capacity gain lost 5x after a permanent loss");
        EnergyHelper.setMaxAura(p,25_000);near(h,EnergyHelper.getMaxAura(p),85_000,"Legacy later growth stopped receiving 5x");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void permanentAuraLossReducesTheExemptionAndLaterGainsRemainFivefold(GameTestHelper h){
        var p=player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        double start=EnergyHelper.getBaseMaxAura(p);
        try(var scope=ChainedContext.ability(p,UtilityArts.DIAMOND_PATH.get())){EnergyHelper.setMaxAura(p,start*.5);}
        near(h,EnergyHelper.getMaxAura(p),start*.5,"Loss/Battlewill callback tripled or left a stale starting exemption");
        EnergyHelper.setMaxAura(p,start);near(h,EnergyHelper.getMaxAura(p),start*3,"Subsequent growth below the original birth roll lost 5x");
        EnergyHelper.setMaxAura(p,start+50);near(h,EnergyHelper.getMaxAura(p),start*3+250,"Later growth did not remain at 5x");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void laterEvolutionGrantsNoChainedAndCannotRegrantAForgottenVow(GameTestHelper h){
        var p=player(h);var storage=SkillAPI.getSkillsFrom(p);
        for(int n=1;n<5;n++){
            var race=HeavenlyRestriction.RESTRICTED.get(n).get();var instance=race.createDefaultInstance();
            h.assertTrue(!race.getIntrinsicSkills(instance,p).contains(HeavenlyRestriction.CHAINED.get()),"Later race advertises Chained: "+n);
            RaceAPI.getRaceFrom(p).setRace(race,false);instance.learnIntrinsicSkills(p);
            h.assertTrue(!ChainedState.owned(p),"Direct later-stage selection granted Chained: "+n);
        }
        ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        h.assertTrue(ChainedState.owned(p),"Fettered Human no longer grants Chained");storage.forgetSkill(HeavenlyRestriction.CHAINED.get());
        for(int n=1;n<5;n++){
            RaceAPI.getRaceFrom(p).setRace(HeavenlyRestriction.RESTRICTED.get(n).get(),false);
            h.assertTrue(!ChainedState.owned(p),"Evolution regranted forgotten Chained: "+n);
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void lineageUsesNinetyTenNativeEPGrowthBeforeVow(GameTestHelper h){
        var p=player(h);var victim=target(h,p.position().add(5,0,0));
        p.getAttribute(TensuraAttributes.AURA_GAIN).setBaseValue(100);p.getAttribute(TensuraAttributes.MAGICULE_GAIN).setBaseValue(100);
        for(int n=0;n<5;n++){
            RaceAPI.getRaceFrom(p).setRace(HeavenlyRestriction.RESTRICTED.get(n).get(),false);SkillAPI.getSkillsFrom(p).forgetSkill(HeavenlyRestriction.CHAINED.get());
            double a=EnergyHelper.getBaseMaxAura(p),m=EnergyHelper.getBaseMaxMagicule(p);
            double auraGain=90*p.getAttributeValue(TensuraAttributes.AURA_GAIN)/100,mpGain=90*p.getAttributeValue(TensuraAttributes.MAGICULE_GAIN)/100;
            io.github.manasmods.tensura.handler.DeathHandler.gainEPEntity(p,victim,90);
            near(h,EnergyHelper.getBaseMaxAura(p)-a,auraGain*2.7,"Native 90% aura growth at stage "+n);
            near(h,EnergyHelper.getBaseMaxMagicule(p)-m,mpGain*.3,"Native 10% MP growth at stage "+n);
        }
        vow(p,false);double a=EnergyHelper.getMaxAura(p),m=EnergyHelper.getMaxMagicule(p);
        double auraGain=90*p.getAttributeValue(TensuraAttributes.AURA_GAIN)/100;
        io.github.manasmods.tensura.handler.DeathHandler.gainEPEntity(p,victim,90);
        near(h,EnergyHelper.getMaxAura(p)-a,auraGain*2.7*5,"Vow did not multiply only the aura share");near(h,EnergyHelper.getMaxMagicule(p),m,"Blocked MP share escaped the vow");
        // An already-saved 0.32 modifier is replaced, rather than compounded.
        p.getAttribute(TensuraAttributes.MAX_AURA).addOrReplacePermanentModifier(new AttributeModifier(ChainedState.VOW,99,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        ChainedState.refresh(p);near(h,p.getAttribute(TensuraAttributes.MAX_AURA).getModifier(ChainedState.VOW).amount(),4,"Old save kept the 100x multiplier");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void dashDamageUsesRebalancedActualAuraPaymentWithoutAFloor(GameTestHelper h){
        near(h,ChainedSkill.pathDamage(0),0,"Empty-aura damage floor");near(h,ChainedSkill.arrivalDamage(0),0,"Empty-aura arrival floor");
        near(h,ChainedSkill.pathDamage(25),7.5,"Rebalanced path formula");near(h,ChainedSkill.arrivalDamage(25),15,"Rebalanced arrival formula");
        near(h,ChainedSkill.pathDamage(100),15,"Paid-aura scaling");near(h,ChainedSkill.arrivalDamage(100),30,"Arrival paid-aura scaling");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void truePassiveCoversBaseModifierAndHelperGainsWhileOffAndUnslotted(GameTestHelper h){
        var p=player(h);EnergyHelper.setBaseMaxEP(p,2000);var i=vow(p,false);near(h,EnergyHelper.getMaxAura(p),1000,"Starting capacity was multiplied");
        var ap=p.getAttribute(TensuraAttributes.MAX_AURA);ap.setBaseValue(1025);ap.addOrReplacePermanentModifier(new AttributeModifier(HeavenlyRestriction.id("chained_test_external_ap"),30,AttributeModifier.Operation.ADD_VALUE));near(h,ap.getValue(),1275,"Modifier gain escaped 5x");
        var mp=p.getAttribute(TensuraAttributes.MAX_MAGICULE);double base=mp.getBaseValue();EnergyHelper.setMaxMagicule(p,10000);near(h,mp.getBaseValue(),base,"Helper raised raw max magicules");mp.setBaseValue(base+25);near(h,mp.getBaseValue(),base,"Direct setter raised magicules");
        mp.addOrReplacePermanentModifier(new AttributeModifier(HeavenlyRestriction.id("chained_test_external_mp"),99999,AttributeModifier.Operation.ADD_VALUE));near(h,mp.getValue(),1000,"Modifier escaped magicule ceiling");
        h.assertTrue(!i.isToggled() && !HeavenlyRestriction.CHAINED.get().isInSlot(p,i),"Fixture unexpectedly toggled/slotted");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeReincarnationAllowsMartialMasterAndHeavenlyEyeButBlocksUnrelatedGifts(GameTestHelper h){
        var p=player(h);vow(p,false);double before=EnergyHelper.getBaseMaxMagicule(p);
        h.assertTrue(reincarnated(p,UniqueSkills.MARTIAL_MASTER.get()),"Native reincarnation root rejected");
        h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(ExtraSkills.HEAVENLY_EYE.get()).isPresent() && ChainedState.inherited(p,ExtraSkills.HEAVENLY_EYE.get()),"Martial Master's real nested Heavenly Eye grant was blocked/untracked");
        h.assertTrue(!RestrictionData.learnReward(p,UniqueSkills.MUSICIAN.get()),"Unrelated Unique gift bypassed the vow");near(h,EnergyHelper.getBaseMaxMagicule(p),before,"Rejected gift consumed permanent magicules");
        var blocked=UniqueSkills.MUSICIAN.get().createDefaultInstance();SkillAPI.getSkillsFrom(p).updateSkill(blocked,true);h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.MUSICIAN.get()).isEmpty(),"Update API bypassed acquisition restriction");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void reincarnationBeforeChoosingRacePreservesItsDescendantProvenance(GameTestHelper h){
        var p=player(h);h.assertTrue(reincarnated(p,UniqueSkills.MARTIAL_MASTER.get()),"Root setup");vow(p,false);
        SkillAPI.getSkillsFrom(p).forgetSkill(ExtraSkills.HEAVENLY_EYE.get());h.assertTrue(RestrictionData.learnReward(p,ExtraSkills.HEAVENLY_EYE.get()),"Previously inherited derived skill cannot be relearned");
        var saved=RestrictionData.of(p).copy();RestrictionData.of(p).remove("chained_inheritance");RestrictionData.of(p).merge(saved);h.assertTrue(ChainedState.inherited(p,UniqueSkills.MARTIAL_MASTER.get()),"Saved ancestry lost");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void resistanceAndBattlewillRemainLearnableButOrdinaryExtraDoesNot(GameTestHelper h){
        var p=player(h);vow(p,false);
        h.assertTrue(RestrictionData.learnReward(p,UtilityArts.DIAMOND_PATH.get()),"Battlewill blocked");h.assertTrue(RestrictionData.learnReward(p,ResistanceSkills.POISON_RESISTANCE.get()),"Resistance blocked");h.assertTrue(RestrictionData.learnReward(p,ResistanceSkills.POISON_NULLIFICATION.get()),"Nullification blocked");h.assertTrue(!RestrictionData.learnReward(p,ExtraSkills.MAGIC_SENSE.get()),"Ordinary Extra acquired");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void learningBonusesAreSelectiveAndNativeMasteryAddsNine(GameTestHelper h){
        var p=player(h);var i=vow(p,false);var art=UtilityArts.DIAMOND_PATH.get();RestrictionData.learnReward(p,art);var a=SkillAPI.getSkillsFrom(p).getSkill(art).orElseThrow();p.getRandom().setSeed(123);double learning=art.getDefaultLearningIncrement(a,p,0);double mastery=p.getAttributeValue(TensuraAttributes.ABILITY_MASTERY_GAIN);
        i.onToggleOn(p);p.getRandom().setSeed(123);near(h,art.getDefaultLearningIncrement(a,p,0),learning+9,"Battlewill learning bonus");a.setMastery(0);a.addMasteryPoint(p);near(h,a.getMastery(),mastery+9,"Native Battlewill mastery bonus");
        var resist=ResistanceSkills.POISON_RESISTANCE.get();var r=resist.createDefaultInstance();r.setMastery(-100);resist.addLearnPoint(r,p,0,1);near(h,r.getMastery(),-80,"Native direct resistance learning did not receive +19");
        i.onToggleOff(p);r.setMastery(-100);resist.addLearnPoint(r,p,0,1);near(h,r.getMastery(),-99,"Resistance bonus leaked with toggle off");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void flatMagiculeRegenerationPreservesLowRatesCapsAndUnrelatedOwners(GameTestHelper h){
        var p=player(h);vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);var rate=p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);double max=EnergyHelper.getMaxMagicule(p),unit=200*EnergyHelper.CONFIG.areaMagiculeRegen;
        try{
            var method=io.github.manasmods.tensura.storage.ep.ExistenceStorage.class.getDeclaredMethod("handleMagiculeRegen",io.github.manasmods.tensura.storage.ep.IExistence.class,net.minecraft.world.entity.LivingEntity.class,double.class,double.class);method.setAccessible(true);
            energy.setSleepModeTime(-1);rate.setBaseValue(3);energy.setAura(0);energy.setMagicule(max);method.invoke(null,energy,p,200d,max);near(h,energy.getAura(),unit*3-.5,"Full MP prevented area/rate conversion");near(h,energy.getMagicule(),max,"Full MP overflowed");
            energy.setAura(0);energy.setMagicule(0);method.invoke(null,energy,p,200d,max);near(h,energy.getAura(),unit*3-.5,"Complete regeneration output was not converted");near(h,energy.getMagicule(),.5,"Retained MP exceeded its half-second budget");
            rate.setBaseValue(.5);energy.setAura(0);energy.setMagicule(0);double poor=.1/EnergyHelper.CONFIG.areaMagiculeRegen;method.invoke(null,energy,p,poor,max);near(h,energy.getAura(),0,"Sub-budget regeneration created excess aura");near(h,energy.getMagicule(),.05,"Low-area regeneration penalty was overwritten");
            energy.setMagicule(max+200);near(h,energy.getMagicule(),max,"Chained reserve writer allowed an overfill");
            var other=player(h);var nativeEnergy=TensuraStorages.getExistenceFrom(other);other.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER).setBaseValue(3);nativeEnergy.setSleepModeTime(-1);nativeEnergy.setAura(0);nativeEnergy.setMagicule(0);double otherMax=EnergyHelper.getMaxMagicule(other);method.invoke(null,nativeEnergy,other,200d,otherMax);near(h,nativeEnergy.getMagicule(),Math.min(otherMax,unit*3),"Unrelated owner's native MP rate changed");near(h,nativeEnergy.getAura(),0,"Unrelated owner's MP was converted");
            nativeEnergy.setMagicule(otherMax+200);method.invoke(null,nativeEnergy,other,200d,otherMax);near(h,nativeEnergy.getMagicule(),Math.max(otherMax,otherMax+200-EnergyHelper.CONFIG.exceedMaxLost),"Unrelated owner's native overflow decay changed");
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void battlewillBuffTriplesOnceAndToggleCleanupPreservesItsNativeValue(GameTestHelper h){
        var p=player(h);var i=vow(p,false);var art=UtilityArts.DIAMOND_PATH.get();RestrictionData.learnReward(p,art);var a=SkillAPI.getSkillsFrom(p).getSkill(art).orElseThrow();double armor=p.getArmorValue();a.onPressed(p,0,0);double nativeBonus=p.getArmorValue()-armor;h.assertTrue(nativeBonus>0,"Native Diamond Path did not apply");
        i.onToggleOn(p);near(h,p.getArmorValue()-armor,nativeBonus*3,"Battlewill buff not tripled");ChainedState.refresh(p);near(h,p.getArmorValue()-armor,nativeBonus*3,"Refresh compounded buff");i.onToggleOff(p);near(h,p.getArmorValue()-armor,nativeBonus,"Toggle cleanup removed native/foreign armor");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void masteredTruePassiveWorksOffAndUnslottedAndEndsOnForget(GameTestHelper h){
        var p=player(h);var i=vow(p,false);i.setMastery(i.getMaxMastery()-1);ChainedState.refresh(p);
        var source=HeavenlyRestriction.CHAINED.get().createSource(i,p,TensuraDamageTypes.FIRE_ELEMENTAL,0);
        var resist=(ResistSkill)ResistanceSkills.HEAT_RESISTANCE.get();var nul=(ResistSkill)ResistanceSkills.HEAT_NULLIFICATION.get();
        h.assertTrue(!resist.isResistanceBypass(source) && !((TensuraDamageSource)source).tensura$isDodgeBypass(),"Unmastered Chained gained Broken Limit");
        i.setMastery(i.getMaxMastery());ChainedState.refresh(p);
        h.assertTrue(!i.isToggled() && !HeavenlyRestriction.CHAINED.get().isInSlot(p,i),"True-passive fixture is toggled or slotted");
        h.assertTrue(resist.isResistanceBypass(source),"Mastered true passive retained resistance while off");
        h.assertTrue(!nul.isNullificationBypass(source),"Broken Limit erased nullification instead of lowering one tier");
        h.assertTrue(((TensuraDamageSource)source).tensura$isDodgeBypass(),"Mastered true passive retained dodge while off");
        near(h,p.getAttribute(TensuraAttributes.DODGE_NEGATE_CHANCE).getModifier(ChainedState.COMBAT).amount(),100,"Off-toggle dodge-negation attribute");
        near(h,p.getAttribute(TensuraAttributes.RESISTANCE_DEGRADATION).getModifier(ChainedState.COMBAT).amount(),1,"Off-toggle degradation attribute");
        i.onToggleOn(p);i.onToggleOff(p);
        h.assertTrue(ChainedState.mastered(p) && resist.isResistanceBypass(source) && ((TensuraDamageSource)source).tensura$isDodgeBypass(),"Turning Martial Supremacy off disabled Broken Limit");
        h.assertTrue(p.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(ChainedState.COMBAT)==null,"True passive kept the toggled attack buff");
        SkillAPI.getSkillsFrom(p).forgetSkill(HeavenlyRestriction.CHAINED.get());
        h.assertTrue(!ChainedState.mastered(p) && !resist.isResistanceBypass(source) && !((TensuraDamageSource)source).tensura$isDodgeBypass(),"Forgotten Chained retained Broken Limit");
        h.assertTrue(p.getAttribute(TensuraAttributes.DODGE_NEGATE_CHANCE).getModifier(ChainedState.COMBAT)==null && p.getAttribute(TensuraAttributes.RESISTANCE_DEGRADATION).getModifier(ChainedState.COMBAT)==null,"Forgetting retained mastered attributes");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void slottedDodgeGuaranteesProjectilesAndLeavesOperatorDamageAvailable(GameTestHelper h){
        var p=player(h);var i=vow(p,false);TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,HeavenlyRestriction.CHAINED.get(),0);var shooter=target(h,p.position().add(8,0,0));var arrow=new Arrow(h.getLevel(),shooter,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),null);var source=p.damageSources().arrow(arrow,shooter);
        h.assertTrue(!HeavenlyRestriction.CHAINED.get().onBeingDamaged(i,p,source,10),"Slotted projectile dodge failed");h.assertTrue(HeavenlyRestriction.CHAINED.get().onBeingDamaged(i,p,p.damageSources().genericKill(),10),"Operator damage was dodged");TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,null,0);h.assertTrue(HeavenlyRestriction.CHAINED.get().onBeingDamaged(i,p,source,10),"Unslotted dodge remained");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void dashPaysOnceUsesNativeLandingAndRespectsOneSecondCooldown(GameTestHelper h){
        // FakePlayer connections deliberately discard teleports; use a living native caster.
        var anchor=player(h);var p=target(h,anchor.position());EnergyHelper.setBaseMaxEP(p,2000);TensuraStorages.getExistenceFrom(p).setMagicule(1000);var i=vow(p,false);p.setOnGround(true);p.setYRot(-90);p.setXRot(0);Vec3 start=p.position();var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);var path=target(h,start.add(8,0,2));var arrival=target(h,start.add(16,0,0));var outside=target(h,start.add(8,0,4));
        afterTracking(h,List.of(p,path,arrival,outside),()->{p.setOnGround(true);double before=energy.getAura();var observed=new DamageSource[1];java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> observer=event->{if(event.getEntity()==path)observed[0]=event.getSource();};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(observer);try{HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(observer);}h.assertTrue(observed[0]!=null && observed[0].is(DamageTypes.MOB_ATTACK) && TensuraDamageHelper.isPhysicalAttack(observed[0]) && !TensuraDamageHelper.isBattlewill(observed[0],p),"Actual dash did not emit purely Physical damage");h.assertTrue(p.getX()-start.x>=14 && p.getX()-start.x<=18,"Native block-snapped landing did not follow the aim reach");near(h,energy.getAura(),before*.98,"Dash charged more/less than 2% current aura");h.assertTrue(path.getHealth()<1024 && arrival.getHealth()<path.getHealth() && outside.getHealth()==1024,"Path/arrival double hit or radius incorrect");h.assertTrue(!TensuraDamageHelper.isBattlewill(HeavenlyRestriction.CHAINED.get().createSource(i,p,DamageTypes.MOB_ATTACK,0),p),"Dash retained Battlewill classification");double aura=energy.getAura();var landed=p.position();ChainedState.refresh(p);h.assertTrue(i.getCoolDown(0)==1,"Dash did not retain its 1-second cooldown");HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);near(h,energy.getAura(),aura,"Cooldown allowed another payment");h.assertTrue(p.position().equals(landed),"Cooldown allowed another teleport");i.decreaseCoolDown(1,0);arrival.discard();path.discard();outside.discard();p.setOnGround(true);HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);near(h,energy.getAura(),aura*.98,"Ready dash did not charge 2% of remaining aura");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void dashStopsBeforeSolidWallsAndBlockedDashIsFree(GameTestHelper h){
        var anchor=player(h);var p=target(h,anchor.position());EnergyHelper.setBaseMaxEP(p,2000);TensuraStorages.getExistenceFrom(p).setMagicule(1000);var i=vow(p,false);p.setOnGround(true);p.setYRot(-90);p.setXRot(0);var start=p.position();var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);var wall=BlockPos.containing(start.add(5,0,0));for(int y=-1;y<4;y++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(wall.offset(0,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);h.assertTrue(p.getX()>start.x && p.getX()<wall.getX(),"Dash crossed a solid wall");i.setCoolDown(0,0);var cell=p.blockPosition();for(int x=-1;x<=1;x++)for(int y=0;y<=4;y++)for(int z=-1;z<=1;z++)if(x!=0 || z!=0 || y>=2)h.getLevel().setBlockAndUpdate(cell.offset(x,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());/* Put the body against the front wall: a centered body still has room for a valid short dash. */p.setPos(cell.getX()+1-p.getBbWidth()*.5,p.getY(),p.getZ());double aura=energy.getAura();p.setOnGround(true);HeavenlyRestriction.CHAINED.get().onPressed(i,p,0,0);near(h,energy.getAura(),aura,"Fully blocked landing spent aura");h.assertTrue(!i.onCoolDown(0),"Fully blocked landing spent cooldown");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void battlewillAndPhysicalDamageBranchesAreExclusiveWithoutChangingAttackStat(GameTestHelper h){
        var p=player(h);var i=vow(p,false);double attack=p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        p.getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(new AttributeModifier(ChainedState.COMBAT,2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        i.onToggleOn(p);near(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),attack,"Retired attack-stat modifier survived migration");
        var victim=target(h,p.position().add(3,0,0));
        var melee=p.damageSources().mobAttack(p);damageCase(h,p,victim,melee,30,"Ordinary Physical melee");
        var arrow=new Arrow(h.getLevel(),p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),null);
        var projectile=p.damageSources().arrow(arrow,p);damageCase(h,p,victim,projectile,30,"Owned Physical projectile");
        var skill=UniqueSkills.MARTIAL_MASTER.get();var physicalSkill=skill.createSource(skill.createDefaultInstance(),p,DamageTypes.MOB_ATTACK,0);damageCase(h,p,victim,physicalSkill,30,"Physical skill");
        var magical=TensuraDamageTypes.getEntityDamageSource(p.level(),TensuraDamageTypes.FIRE_ELEMENTAL,p);damageCase(h,p,victim,magical,10,"Elemental damage was incorrectly tripled");
        var converted=TensuraDamageTypes.getEntityDamageSource(p.level(),TensuraDamageTypes.FIRE_ELEMENTAL,p);((TensuraDamageSource)converted).tensura$setPhysicalConverted();damageCase(h,p,victim,converted,30,"Native physical conversion");
        var dash=HeavenlyRestriction.CHAINED.get().createSource(i,p,DamageTypes.MOB_ATTACK,0);
        h.assertTrue(TensuraDamageHelper.isPhysicalAttack(dash) && !TensuraDamageHelper.isBattlewill(dash,p),"Dash must be Physical without Battlewill classification");damageCase(h,p,victim,dash,30,"Dash received more than the Physical bonus");
        h.assertTrue(TensuraDamageHelper.isPhysicalOrBattlewill(dash,p),"Pure Physical dash lost native Physical handling");
        var physicalBattlewill=MeleeArts.AURA_SWORD.get();var battlewillSource=physicalBattlewill.createSource(physicalBattlewill.createDefaultInstance(),p,DamageTypes.MOB_ATTACK,0);damageCase(h,p,victim,battlewillSource,50,"Physical Battlewill received stacked damage bonuses");((TensuraDamageSource)battlewillSource).tensura$setPhysicalConverted();damageCase(h,p,victim,battlewillSource,50,"Converted Battlewill did not take precedence over Physical");
        for(var buff:java.util.List.of(TensuraMobEffects.AURA_SWORD,TensuraMobEffects.OGRE_GUILLOTINE)){
            var effect=TensuraMobEffects.getReference(buff);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,200));
            h.assertTrue(TensuraDamageHelper.isBattlewill(melee,p),"Native buff no longer classifies ordinary melee as Battlewill");
            h.assertTrue(!TensuraDamageHelper.isBattlewill(dash,p) && !TensuraDamageHelper.isBattlewill(dash,null),"Native buff reclassified dash as Battlewill");
            p.removeEffect(effect);
        }
        var battlewill=MeleeArts.AURA_SWORD.get();var elementalBattlewill=battlewill.createSource(battlewill.createDefaultInstance(),p,TensuraDamageTypes.FIRE_ELEMENTAL,0);damageCase(h,p,victim,elementalBattlewill,50,"Non-Physical Battlewill bonus changed");
        var other=player(h);damageCase(h,p,victim,other.damageSources().mobAttack(other),10,"Another attacker's Physical damage was boosted");
        i.onToggleOff(p);damageCase(h,p,victim,projectile,10,"Physical bonus persisted with toggle off");damageCase(h,p,victim,dash,10,"Dash bonuses persisted with toggle off");damageCase(h,p,victim,battlewillSource,10,"Battlewill bonus persisted with toggle off");near(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),attack,"Physical bonus changed the attack attribute");h.succeed();
    }
    private static void damageCase(GameTestHelper h,net.minecraft.world.entity.LivingEntity owner,net.minecraft.world.entity.LivingEntity victim,DamageSource source,double expected,String label){
        var amount=Changeable.of(10f);SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.invoker().calculate(SkillAPI.getSkillsFrom(victim),victim,source,amount);near(h,amount.get(),expected,label);
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void forgettingRemovesOnlyChainedModifiersAndDoesNotCompoundOnReload(GameTestHelper h){
        var p=player(h);double aura=p.getAttributeValue(TensuraAttributes.MAX_AURA),attack=p.getAttributeValue(Attributes.ATTACK_DAMAGE);var i=vow(p,true);var restored=ManasSkillInstance.fromNBT(i.toNBT());SkillAPI.getSkillsFrom(p).updateSkill(restored,true);ChainedState.initialize(p);near(h,p.getAttributeValue(TensuraAttributes.MAX_AURA),aura,"Reload compounded capacity");SkillAPI.getSkillsFrom(p).forgetSkill(HeavenlyRestriction.CHAINED.get());near(h,p.getAttributeValue(TensuraAttributes.MAX_AURA),aura,"Forgotten aura modifier persisted");near(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),attack,"Forgotten combat modifier persisted");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void evolutionMenuUsesFixedTotalEPAndActualMasteredBattlewills(GameTestHelper h){
        var p=player(h);EnergyHelper.setBaseMaxEP(p,2000);var i=vow(p,false);i.setMastery(i.getMaxMastery());var first=HeavenlyRestriction.RESTRICTED.getFirst().get();var current=first.createDefaultInstance();var next=HeavenlyRestriction.RESTRICTED.get(1).get();
        p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(20599);h.assertTrue(EnergyHelper.getMaxEP(p)<100_000 && first.getEvolutionProgress(current,p,next)<100,"Evolution ignored the fixed total-EP or training requirements");
        RestrictionData.learnReward(p,UtilityArts.DIAMOND_PATH.get());var art=SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.DIAMOND_PATH.get()).orElseThrow();art.setMastery(art.getMaxMastery());h.assertTrue(first.getEvolutionProgress(current,p,next)<100,"Sub-threshold total EP passed");p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(20600);h.assertTrue(EnergyHelper.getBaseMaxEP(p)<100_000,"Fixture no longer distinguishes base and total EP");near(h,first.getEvolutionProgress(current,p,next),100,"Native evolution requirements did not complete at the fixed threshold");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void auraSwordKeepsNativeBonusWithoutChangingOtherBattlewillBuffs(GameTestHelper h){
        var p=player(h);var i=vow(p,true);var art=MeleeArts.AURA_SWORD.get();RestrictionData.learnReward(p,art);var a=SkillAPI.getSkillsFrom(p).getSkill(art).orElseThrow();a.onPressed(p,0,0);var effect=p.getEffect(TensuraMobEffects.getReference(TensuraMobEffects.AURA_SWORD));h.assertTrue(effect!=null && ChainedBuffs.source(effect)==art,"Native Aura Sword source was not recorded");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));p.getMainHandItem().forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND,(attribute,modifier)->{var value=p.getAttribute(attribute);if(value!=null)value.addOrUpdateTransientModifier(modifier);});var victim=target(h,p.position().add(3,0,0));var source=p.damageSources().mobAttack(p);double weapon=io.github.manasmods.manascore.attribute.api.ManasCoreAttributeUtils.getWeaponDamage(p,victim,source);var config=io.github.manasmods.manascore.config.ConfigRegistry.getConfig(io.github.manasmods.tensura.config.ability.BattlewillConfig.class);
        double expected=(30+weapon*config.AuraSword.attackMultiplier)*3;var amount=Changeable.of(30f);SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.invoker().calculate(SkillAPI.getSkillsFrom(victim),victim,source,amount);near(h,amount.get(),expected,"Aura Sword added damage received the retired buff multiplier");h.assertTrue(!ChainedBuffs.damageBuff(p,effect.getEffect().value()),"Aura Sword is still treated as a tripled buff");near(h,weapon,7,"Diamond sword reference damage");h.assertTrue(effect.getAmplifier()==0,"Buff level/duration was changed instead of numeric effectiveness");i.onToggleOff(p);var nativeAmount=Changeable.of(7f);SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.invoker().calculate(SkillAPI.getSkillsFrom(victim),victim,source,nativeAmount);near(h,nativeAmount.get(),7+weapon*config.AuraSword.attackMultiplier,"Toggle-off changed native Aura Sword");i.onToggleOn(p);var boosted=Changeable.of(7f);SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.invoker().calculate(SkillAPI.getSkillsFrom(victim),victim,source,boosted);near(h,boosted.get(),(7+weapon*config.AuraSword.attackMultiplier)*3,"Diamond sword damage reapplied the buff multiplier");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeDiamondSwordAuraSlashGetsOnlyTheBattlewillBonus(GameTestHelper h){
        var p=player(h);var chained=vow(p,true);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        RestrictionData.learnReward(p,MeleeArts.AURA_SWORD.get());var sword=SkillAPI.getSkillsFrom(p).getSkill(MeleeArts.AURA_SWORD.get()).orElseThrow();sword.onPressed(p,0,0);h.assertTrue(p.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.AURA_SWORD)),"Native Aura Sword setup failed");
        var victim=target(h,p.position().add(3,0,0));var art=MeleeArts.AURA_SLASH.get();var shots=new java.util.ArrayList<io.github.manasmods.tensura.entity.projectile.magic.AuraSlashProjectile>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> observer=event->{if(event.getEntity() instanceof io.github.manasmods.tensura.entity.projectile.magic.AuraSlashProjectile shot && shot.getOwner()==p)shots.add(shot);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(observer);
        try{
            for(boolean mastered:new boolean[]{false,true}){
                var instance=art.createDefaultInstance();instance.setMastery(mastered?instance.getMaxMastery():0);TensuraStorages.getExistenceFrom(p).setAura(10000);int before=shots.size();art.onPressed(instance,p,0,0);
                h.assertTrue(shots.size()==before+1,"Actual Aura Slash did not spawn one main-hand projectile");var shot=shots.getLast();var source=shot.getDamageSource();
                h.assertTrue(TensuraDamageHelper.isPhysicalAttack(source) && ((TensuraDamageSource)source).tensura$getAbilityInstance().getSkill() instanceof io.github.manasmods.tensura.ability.battlewill.Battlewill,"Native Aura Slash lost its Physical/Battlewill source");
                near(h,shot.getDamage(),mastered?14:7,"Actual diamond sword Aura Slash base damage");
                var amount=Changeable.of(shot.getDamage());SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.invoker().calculate(SkillAPI.getSkillsFrom(victim),victim,source,amount);near(h,amount.get(),mastered?70:35,"Aura Slash stacked Physical or Aura Sword bonuses");
                chained.onToggleOff(p);var unboosted=Changeable.of(shot.getDamage());SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.invoker().calculate(SkillAPI.getSkillsFrom(victim),victim,source,unboosted);near(h,unboosted.get(),shot.getDamage(),"Toggle-off projectile kept a Chained bonus");chained.onToggleOn(p);
            }
        }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(observer);shots.forEach(net.minecraft.world.entity.Entity::discard);}
        h.succeed();
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
        var p=player(h);var i=vow(p,false);i.setMastery(i.getMaxMastery()-1);var recipient=target(h,p.position().add(4,0,0));var other=target(h,p.position().add(8,0,0));var nul=ResistanceSkills.POISON_NULLIFICATION.get();var n=nul.createDefaultInstance();n.setToggled(true);var effect=Changeable.of(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100));
        h.assertTrue(!nul.onEffectAdded(n,recipient,p,effect),"Native poison immunity fixture");i.setMastery(i.getMaxMastery());ChainedState.refresh(p);h.assertTrue(!i.isToggled(),"Status true-passive fixture is toggled");var lower=ResistanceSkills.POISON_RESISTANCE.get();boolean expected=!((ChainedResistanceView)lower).restriction$immuneEffects(lower.createDefaultInstance(),recipient).contains(effect.get().getEffect());h.assertTrue(nul.onEffectAdded(n,recipient,p,effect)==expected,"Status immunity did not become its actual lower tier");h.assertTrue(!nul.onEffectAdded(n,recipient,other,effect),"Degradation leaked to another caster");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void existingSpiritualCapacityLimitCannotBeRemovedToGrowMagiculesAfterTheVow(GameTestHelper h){
        var p=player(h);p.getAttribute(TensuraAttributes.LIMITED_SPIRITUAL_MAX_MAGICULE).setBaseValue(100);vow(p,false);near(h,EnergyHelper.getMaxMagicule(p),100,"Effective ceiling setup");p.getAttribute(TensuraAttributes.LIMITED_SPIRITUAL_MAX_MAGICULE).setBaseValue(0);near(h,EnergyHelper.getMaxMagicule(p),100,"Removing a limit raised capacity beyond the original effective maximum");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeRaceChangesPreserveTheVowAndReplaceAllFourEvolutionBonuses(GameTestHelper h){
        var p=player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();i.setMastery(i.getMaxMastery());double mp=EnergyHelper.getMaxMagicule(p),start=RestrictionData.of(p).getDouble("chained_starting_aura");double[] hp={0,100,300,600,1000};
        for(int n=1;n<5;n++){RaceAPI.getRaceFrom(p).setRace(HeavenlyRestriction.RESTRICTED.get(n).get(),false);h.assertTrue(ChainedState.owned(p) && ChainedState.mastery(p)==1,"Evolution lost Chained or mastery");near(h,EnergyHelper.getMaxMagicule(p),mp,"Evolution raised the forbidden pool");near(h,RestrictionData.of(p).getDouble("chained_starting_aura"),start,"Evolution reset the starting aura exemption");near(h,p.getAttribute(Attributes.MAX_HEALTH).getModifier(HeavenlyRestriction.id("restricted_vitality")).amount(),hp[n],"Racial health modifiers accumulated");}
        h.assertTrue(RaceAPI.getRaceFrom(p).getRace().orElseThrow().getNextEvolutions(p).isEmpty(),"Final race still has an evolution");h.succeed();
    }
}
