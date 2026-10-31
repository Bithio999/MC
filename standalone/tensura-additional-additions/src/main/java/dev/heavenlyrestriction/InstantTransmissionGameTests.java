package dev.heavenlyrestriction;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.data.TensuraSkillTags;
import io.github.manasmods.tensura.entity.magic.misc.WarpPortalEntity;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import io.github.manasmods.tensura.network.c2s.RequestSpatialActionPacket;
import io.github.manasmods.tensura.registry.battlewill.UtilityArts;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
import static dev.heavenlyrestriction.TestFixtures.*;
import static dev.heavenlyrestriction.ChainedGameTests.near;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class InstantTransmissionGameTests {
    /** FakePlayer's packet listener ignores teleport packets; model the real connection's position update. */
    private static ServerPlayer traveler(GameTestHelper h){
        var initialized=player(h);
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),initialized.getGameProfile()){
            @Override public void teleportTo(double x,double y,double z){setPos(x,y,z);}
        };
        p.setPos(initialized.position());p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        io.github.manasmods.manascore.race.api.RaceAPI.getRaceFrom(p).setRace(io.github.manasmods.tensura.registry.race.TensuraRaces.HUMAN.get(),false);
        io.github.manasmods.tensura.util.EnergyHelper.setBaseMaxEP(p,20_000_000);
        var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1_000_000);energy.setMagicule(1_000_000);return p;
    }
    private static ManasSkillInstance learn(ServerPlayer p,boolean mastered){
        RestrictionData.learnReward(p,HeavenlyRestriction.INSTANT_TRANSMISSION.get());
        var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get()).orElseThrow();
        if(mastered)i.setMastery(i.getMaxMastery());
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,i.getSkill(),0);return i;
    }
    private static void nativeWarp(ServerPlayer p,ManasSkillInstance i,Vec3 at){RequestSpatialActionPacket.handleWarp(p,i,HeavenlyRestriction.INSTANT_TRANSMISSION.get(),at.x,at.y,at.z,p.level().dimension(),false);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void battlewillCategoryAndSelfOnlyCapabilitiesAreNative(GameTestHelper h){
        var p=traveler(h);var i=learn(p,false);var art=HeavenlyRestriction.INSTANT_TRANSMISSION.get();
        h.assertTrue(art instanceof Battlewill && i.is(TensuraSkillTags.BATTLEWILL) && i.getMaxMastery()==400,"Not a native Battlewill");
        h.assertTrue(art.getColoredName().getString().equals("Instant Transmission") && art.getSkillIcon().equals(UtilityArts.INSTANT_MOVE.get().getSkillIcon()),"Name/icon missing");
        h.assertTrue(!art.canPortal(i,p) && !art.canDimensionTravel(i,p) && art.getSpatialMovementModeIndex()==0,"Unexpected portal/dimension mode");
        near(h,art.getMagiculeCost(p,i,0),0,"Requests magicules");near(h,art.getAuraCost(p,i,0),10,"Ordinary per-block cost");
        i.setMastery(i.getMaxMastery());near(h,art.getAuraCost(p,i,0),5,"Mastered per-block cost");
        h.assertTrue(art.getWarpChargeTick(i,p)==0,"Mastered charge remains");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeInstantMoveMasteryUnlocksOneTrainableEvolution(GameTestHelper h){
        var p=traveler(h);var storage=SkillAPI.getSkillsFrom(p);RestrictionData.learnReward(p,UtilityArts.INSTANT_MOVE.get());var parent=storage.getSkill(UtilityArts.INSTANT_MOVE.get()).orElseThrow();
        parent.setMastery(parent.getMaxMastery()-1);h.assertTrue(storage.getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get()).isEmpty(),"Evolution unlocked early");
        parent.addMasteryPoint(p,1);var evolution=storage.getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get()).orElseThrow();
        h.assertTrue(evolution.getMastery()<0 && parent.isMastered(p) && storage.getSkill(parent.getSkill()).isPresent(),"Parent mastery did not unlock a learning entry");
        evolution.setMastery(50);InstantTransmissionArt.login(new PlayerEvent.PlayerLoggedInEvent(p));
        h.assertTrue(storage.getSkill(evolution.getSkill()).orElseThrow()==evolution && evolution.getMastery()==50,"Login reset existing learning/mastery");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void existingMasteredParentsBackfillButBorrowedCopiesDoNot(GameTestHelper h){
        var p=traveler(h);var detached=UtilityArts.INSTANT_MOVE.get().createDefaultInstance();detached.setMastery(detached.getMaxMastery());InstantTransmissionArt.unlockEvolution(p,detached);
        detached.setRemoveTime(60);SkillAPI.getSkillsFrom(p).learnSkill(detached);var parent=SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.INSTANT_MOVE.get()).orElseThrow();
        InstantTransmissionArt.unlockEvolution(p,parent);h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get()).isEmpty(),"Temporary/detached mastery unlocked a permanent art");
        parent.setRemoveTime(-1);InstantTransmissionArt.login(new PlayerEvent.PlayerLoggedInEvent(p));
        h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get()).orElseThrow().getMastery()<0,"Existing permanent mastery did not backfill");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeCoordinateRequestBecomesInstantAtMasteryAndPaysOnlyAura(GameTestHelper h){
        var p=traveler(h);var i=learn(p,true);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);energy.setMagicule(700);var to=p.position().add(12,0,16);
        nativeWarp(p,i,to);h.assertTrue(p.position().distanceToSqr(to)<1e-8 && !InstantTransmissionArt.charging(p),"Mastered native request did not teleport immediately");
        near(h,energy.getAura(),900,"20-block mastered request should cost 100 aura");near(h,energy.getMagicule(),700,"Native packet charged MP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=140)
    public static void unmasteredRequestWaitsExactlyOneHundredTicksAndChargesOnce(GameTestHelper h){
        var p=traveler(h);var i=learn(p,false);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);energy.setMagicule(700);var from=p.position();var to=from.add(12,0,16);long start=h.getLevel().getGameTime();
        nativeWarp(p,i,to);h.assertTrue(InstantTransmissionArt.charging(p) && p.position().equals(from),"Unmastered request skipped charge");near(h,energy.getAura(),800,"20-block ordinary request should cost 200 aura");
        nativeWarp(p,i,from.add(40,0,0));near(h,energy.getAura(),800,"Repeated packet charged again during windup");
        h.runAfterDelay(99,()->{h.assertTrue(h.getLevel().getGameTime()-start==99,"Charge fixture tick timing changed");InstantTransmissionArt.tick(p);h.assertTrue(p.position().equals(from) && InstantTransmissionArt.charging(p),"Teleported before tick 100");});
        h.runAfterDelay(100,()->{InstantTransmissionArt.tick(p);h.assertTrue(p.position().distanceToSqr(to)<1e-8 && !InstantTransmissionArt.charging(p),"Did not teleport on tick 100");near(h,energy.getAura(),800,"Completion charged aura twice");near(h,energy.getMagicule(),700,"Windup spent magicules");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void malformedUnlearnedUnfundedAndCrossDimensionRequestsDoNotTeleport(GameTestHelper h){
        var p=traveler(h);var i=learn(p,true);var art=HeavenlyRestriction.INSTANT_TRANSMISSION.get();var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10);energy.setMagicule(1_000_000);var at=p.position();
        h.assertTrue(!art.requestWarp(p,i,at.x+20,at.y,at.z,p.level().dimension(),false),"Magicules funded an aura shortfall");
        energy.setAura(1000);
        for(double x:new double[]{Double.NaN,Double.POSITIVE_INFINITY,30_000_001})h.assertTrue(!art.requestWarp(p,i,x,at.y,at.z,p.level().dimension(),false),"Invalid coordinate accepted");
        h.assertTrue(!art.requestWarp(p,i,at.x+20,at.y,at.z,Level.NETHER,false),"Cross-dimension request accepted");
        h.assertTrue(!art.requestWarp(p,art.createDefaultInstance(),at.x+20,at.y,at.z,p.level().dimension(),false),"Detached skill authorized teleport");
        i.setMastery(-1);nativeWarp(p,i,at.add(20,0,0));
        h.assertTrue(p.position().equals(at) && !InstantTransmissionArt.charging(p),"Learning entry teleported");near(h,energy.getAura(),1000,"Invalid request charged aura");near(h,energy.getMagicule(),1_000_000,"Invalid request charged magicules");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativePortalRequestCannotCreateAPortalOrChargeResources(GameTestHelper h){
        var p=traveler(h);var i=learn(p,true);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);energy.setMagicule(700);var at=p.position().add(20,0,0);
        int before=h.getLevel().getEntitiesOfClass(WarpPortalEntity.class,p.getBoundingBox().inflate(64)).size();
        RequestSpatialActionPacket.handlePortal(p,i,HeavenlyRestriction.INSTANT_TRANSMISSION.get(),at.x,at.y,at.z,p.level().dimension(),false);
        h.assertTrue(h.getLevel().getEntitiesOfClass(WarpPortalEntity.class,p.getBoundingBox().inflate(64)).size()==before && !InstantTransmissionArt.charging(p),"Portal action was accepted");near(h,energy.getAura(),1000,"Portal request spent aura");near(h,energy.getMagicule(),700,"Portal request spent MP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void spatialBlockadeAndLosingTheSkillCancelPendingTransmission(GameTestHelper h){
        var p=traveler(h);var i=learn(p,false);var from=p.position();nativeWarp(p,i,from.add(20,0,0));
        p.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.SPATIAL_BLOCKADE),200));InstantTransmissionArt.tick(p);
        h.assertTrue(!InstantTransmissionArt.charging(p) && p.position().equals(from),"Spatial Blockade failed to cancel windup");
        p.removeEffect(TensuraMobEffects.getReference(TensuraMobEffects.SPATIAL_BLOCKADE));nativeWarp(p,i,from.add(20,0,0));h.assertTrue(InstantTransmissionArt.charging(p),"Cast did not recover after blockade");
        SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());h.assertTrue(!InstantTransmissionArt.charging(p),"Forgetting the art left a pending cast");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeTeleportEventCanVetoAnInstantTransmission(GameTestHelper h){
        var p=traveler(h);var i=learn(p,true);var from=p.position();var id=p.getUUID();
        TensuraEntityEvents.INSTANT_TRANSMISSION_EVENT.register((target,warper,position,type)->target.getUUID().equals(id)?EventResult.interruptFalse():EventResult.pass());
        nativeWarp(p,i,from.add(20,0,0));h.assertTrue(p.position().equals(from),"Battlewill bypassed native spatial-event veto");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedCanLearnTheEvolutionAndReceivesBattlewillMasteryBonus(GameTestHelper h){
        var p=player(h);ChainedGameTests.vow(p,true);RestrictionData.learnReward(p,UtilityArts.INSTANT_MOVE.get());var parent=SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.INSTANT_MOVE.get()).orElseThrow();parent.setMastery(parent.getMaxMastery());parent.onSkillMastered(p);
        var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get()).orElseThrow();h.assertTrue(i.getMastery()<0,"Chained did not receive the normal learning entry");i.setMastery(0);TensuraStorages.getExistenceFrom(p).setMagicule(0);
        double increment=p.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.ABILITY_MASTERY_GAIN)+9;
        i.addMasteryPoint(p);near(h,i.getMastery(),increment,"Evolution missed Chained's Battlewill mastery bonus");near(h,TensuraStorages.getExistenceFrom(p).getMagicule(),0,"Evolution billed MP");h.succeed();
    }

}
