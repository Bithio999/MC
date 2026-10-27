package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.data.TensuraSkillTags;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.battlewill.UtilityArts;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
import static dev.heavenlyrestriction.TestFixtures.player;
import static dev.heavenlyrestriction.ChainedGameTests.near;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class AirSovereigntyGameTests {
    private static ManasSkillInstance learn(ServerPlayer p){
        RestrictionData.learnReward(p,HeavenlyRestriction.AIR_SOVEREIGNTY.get());
        var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get()).orElseThrow();
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,HeavenlyRestriction.AIR_SOVEREIGNTY.get(),0);return i;
    }
    private static void press(ServerPlayer p,ManasSkillInstance i){i.onPressed(p,0,0);}
    private static void ticks(ServerPlayer p,int count){for(int n=0;n<count;n++)AirSovereigntyArt.tick(p);}

    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeBattlewillCategoryHasARealNameAndNoMagiculeCost(GameTestHelper h){
        var p=player(h);var i=learn(p);var art=HeavenlyRestriction.AIR_SOVEREIGNTY.get();
        h.assertTrue(art instanceof Battlewill && i.is(TensuraSkillTags.BATTLEWILL),"New art lost native Battlewill classification");
        h.assertTrue(art.getColoredName().getString().equals("Air Sovereignty") && i.getMaxMastery()==400,"Name/mastery does not use native Battlewill behavior");
        near(h,art.getMagiculeCost(p,i,0),0,"Flight requests MP");h.assertTrue(!io.github.manasmods.tensura.menu.ReincarnationMenu.getSkillPool().contains(art),"Battlewill entered Unique rolls");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void actualAirFlightMasteryUnlocksANativeLearningEntryAndRetainsTheParent(GameTestHelper h){
        var p=player(h);var storage=SkillAPI.getSkillsFrom(p);RestrictionData.learnReward(p,UtilityArts.AIR_FLIGHT.get());var parent=storage.getSkill(UtilityArts.AIR_FLIGHT.get()).orElseThrow();
        parent.setMastery(parent.getMaxMastery()-1);h.assertTrue(storage.getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get()).isEmpty(),"Evolution unlocked early");
        parent.addMasteryPoint(p,1);var result=storage.getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get()).orElseThrow();
        h.assertTrue(result.getMastery()<0 && parent.isMastered(p) && storage.getSkill(parent.getSkill()).isPresent(),"Native mastery did not unlock a trainable evolution while retaining Air Flight");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void borrowedSubInstanceAndUnmasteredAirFlightCannotUnlockEvolution(GameTestHelper h){
        var p=player(h);ManasSkillInstance parent=UtilityArts.AIR_FLIGHT.get().createDefaultInstance();
        AirSovereigntyArt.unlockEvolution(p,parent);parent.setMastery(parent.getMaxMastery());AirSovereigntyArt.unlockEvolution(p,parent);
        parent.setRemoveTime(60);SkillAPI.getSkillsFrom(p).learnSkill(parent);parent=SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.AIR_FLIGHT.get()).orElseThrow();AirSovereigntyArt.unlockEvolution(p,parent);
        parent.setRemoveTime(-1);parent.setParentSkill(UtilityArts.DIAMOND_PATH.get());AirSovereigntyArt.unlockEvolution(p,parent);
        h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get()).isEmpty(),"Borrowed/unmastered parent unlocked permanent flight");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void existingMasteredAirFlightGetsOneLearningEntryOnLogin(GameTestHelper h){
        var p=player(h);var storage=SkillAPI.getSkillsFrom(p);RestrictionData.learnReward(p,UtilityArts.AIR_FLIGHT.get());var parent=storage.getSkill(UtilityArts.AIR_FLIGHT.get()).orElseThrow();parent.setMastery(parent.getMaxMastery());
        AirSovereigntyArt.login(new PlayerEvent.PlayerLoggedInEvent(p));var i=storage.getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get()).orElseThrow();i.setMastery(135);
        AirSovereigntyArt.login(new PlayerEvent.PlayerLoggedInEvent(p));AirSovereigntyArt.unlockEvolution(p,parent);
        near(h,storage.getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get()).orElseThrow().getMastery(),135,"Repeated discovery reset earned mastery");
        h.assertTrue(storage.getLearnedSkills().stream().filter(v->v.getSkill()==HeavenlyRestriction.AIR_SOVEREIGNTY.get()).count()==1,"Evolution duplicated");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void latentArtUsesNativeLearningAndCannotEnableFlightBeforeCompletion(GameTestHelper h){
        var p=player(h);var art=HeavenlyRestriction.AIR_SOVEREIGNTY.get();var i=art.createLearningInstance(p);var storage=SkillAPI.getSkillsFrom(p);storage.learnSkill(i);
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,art,0);var energy=TensuraStorages.getExistenceFrom(p);energy.setMagicule(0);double before=energy.getAura();
        press(p,i);h.assertTrue(!p.getAbilities().mayfly,"Latent art granted flight");
        storage.startHoldSkill(i,0,0);h.assertTrue(!p.getAbilities().mayfly && energy.getAura()<before && energy.getMagicule()==0,"Native learning granted flight or billed magicules");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void flightMatchesGravityManipulationAndStoppingIsFree(GameTestHelper h){
        var p=player(h);var reference=player(h);var i=learn(p);var energy=TensuraStorages.getExistenceFrom(p);energy.setMagicule(0);double aura=energy.getAura(),cost=HeavenlyRestriction.AIR_SOVEREIGNTY.get().getAuraCost(p,i,0);
        RestrictionData.learnReward(reference,ExtraSkills.GRAVITY_MANIPULATION.get());var g=SkillAPI.getSkillsFrom(reference).getSkill(ExtraSkills.GRAVITY_MANIPULATION.get()).orElseThrow();g.onPressed(reference,0,0);press(p,i);
        h.assertTrue(p.getAbilities().mayfly && p.getAbilities().flying && p.getAbilities().mayfly==reference.getAbilities().mayfly && p.getAbilities().flying==reference.getAbilities().flying,"Flight differs from native Gravity Manipulation");
        h.assertTrue(SkillUtils.canFlyLegit(p),"Native flight authorization rejects the Battlewill");near(h,energy.getAura(),aura-cost,"Activation payment");near(h,energy.getMagicule(),0,"Activation used magicules");
        i.onRelease(p,1,0,0);h.assertTrue(p.getAbilities().flying,"Releasing the key interrupted press-controlled flight");
        energy.setAura(0);press(p,i);h.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying && !SkillUtils.canFlyLegit(p),"Stopping retained flight permission");near(h,energy.getAura(),0,"Stopping costs aura");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void upkeepChargesActualFlyingTimeAndStopsCleanlyWhenUnfunded(GameTestHelper h){
        var p=player(h);var i=learn(p);var energy=TensuraStorages.getExistenceFrom(p);energy.setMagicule(0);press(p,i);double before=energy.getAura(),cost=HeavenlyRestriction.AIR_SOVEREIGNTY.get().getAuraCost(p,i,0);
        ticks(p,19);near(h,energy.getAura(),before,"Upkeep charged early");ticks(p,1);near(h,energy.getAura(),before-cost,"One-second upkeep did not charge once");
        p.getAbilities().flying=false;double paused=energy.getAura();ticks(p,40);near(h,energy.getAura(),paused,"Landed/idle permission consumed upkeep");
        p.getAbilities().flying=true;energy.setAura(0);ticks(p,20);h.assertTrue(!p.getAbilities().flying && !p.getAbilities().mayfly,"Empty aura retained free flight");near(h,energy.getAura(),0,"Flight overdraws aura");near(h,energy.getMagicule(),0,"Upkeep used MP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void interferenceAndMovementRestrictionRevokeFlightAndBlockActivation(GameTestHelper h){
        var p=player(h);var i=learn(p);press(p,i);var effect=TensuraMobEffects.getReference(TensuraMobEffects.MAGIC_INTERFERENCE);p.addEffect(new MobEffectInstance(effect,100));ticks(p,1);
        h.assertTrue(!p.getAbilities().mayfly,"Magic interference did not revoke flight");double aura=TensuraStorages.getExistenceFrom(p).getAura();press(p,i);near(h,TensuraStorages.getExistenceFrom(p).getAura(),aura,"Blocked activation spent aura");
        p.removeEffect(effect);press(p,i);h.assertTrue(p.getAbilities().flying,"Cannot fly again after interference");p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);ticks(p,1);
        h.assertTrue(!p.getAbilities().mayfly,"Movement restriction retained flight");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void forgettingRevokesOnlyThisFlightSource(GameTestHelper h){
        var p=player(h);var i=learn(p);press(p,i);SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());
        h.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying,"Forgetting left orphaned flight");
        i=learn(p);press(p,i);RestrictionData.learnReward(p,ExtraSkills.GRAVITY_MANIPULATION.get());AirSovereigntyArt.stop(p);
        h.assertTrue(p.getAbilities().mayfly && p.getAbilities().flying && SkillUtils.canFlyLegit(p),"Stopping removed a native Gravity Manipulation owner's legitimate flight");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void creativeFlightIsPreservedAndCannotBeChargedByTheArt(GameTestHelper h){
        var p=player(h);var i=learn(p);press(p,i);p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;ticks(p,1);
        h.assertTrue(p.getAbilities().mayfly && p.getAbilities().flying,"Creative flight was revoked");double aura=TensuraStorages.getExistenceFrom(p).getAura();press(p,i);ticks(p,20);near(h,TensuraStorages.getExistenceFrom(p).getAura(),aura,"Creative flight charged upkeep");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void flightLeaseSurvivesSkillReloadButEndsOnPlayerClone(GameTestHelper h){
        var p=player(h);var i=learn(p);press(p,i);var storage=SkillAPI.getSkillsFrom(p);var reloaded=ManasSkillInstance.fromNBT(i.toNBT());storage.updateSkill(reloaded,true);ticks(p,1);
        h.assertTrue(p.getAbilities().flying && SkillUtils.canFlyLegit(p),"Saved flight state was not recognized");
        RestrictionData.of(p).remove("air_sovereignty_flight");ticks(p,1);h.assertTrue(!p.getAbilities().mayfly && !AirSovereigntyArt.authorizesFlight(p),"A detached skill save granted unbilled flight");press(p,reloaded);
        var clone=player(h);SkillAPI.getSkillsFrom(clone).learnSkill(ManasSkillInstance.fromNBT(reloaded.toNBT()));clone.getAbilities().mayfly=true;clone.getAbilities().flying=true;var event=new PlayerEvent.Clone(clone,p,true);RestrictionEvents.onClone(event);AirSovereigntyArt.clonePlayer(event);
        h.assertTrue(!AirSovereigntyArt.authorizesFlight(clone) && !clone.getAbilities().mayfly && !clone.getAbilities().flying && !RestrictionData.of(clone).getBoolean("air_sovereignty_flight"),"Respawn inherited a stale flight entitlement");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedCanLearnTheEvolutionAndItsBattlewillMasteryBonusApplies(GameTestHelper h){
        var p=player(h);ChainedGameTests.vow(p,true);RestrictionData.learnReward(p,UtilityArts.AIR_FLIGHT.get());var parent=SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.AIR_FLIGHT.get()).orElseThrow();parent.setMastery(parent.getMaxMastery());parent.onSkillMastered(p);
        var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get()).orElseThrow();i.setMastery(0);TensuraStorages.getExistenceFrom(p).setMagicule(0);double increment=p.getAttributeValue(TensuraAttributes.ABILITY_MASTERY_GAIN)+9;
        press(p,i);near(h,i.getMastery(),increment,"New art did not receive Chained's native Battlewill mastery bonus");h.assertTrue(p.getAbilities().flying,"Chained cannot use aura flight");near(h,TensuraStorages.getExistenceFrom(p).getMagicule(),0,"Chained flight consumed MP");h.succeed();
    }
}
