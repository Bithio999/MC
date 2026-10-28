package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.attribute.api.ManasCoreAttributes;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.registry.battlewill.UtilityArts;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.damage.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.heavenlyrestriction.TestFixtures.*;
import static dev.heavenlyrestriction.ChainedGameTests.near;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class UnboundGameTests {
    private static ManasSkillInstance prepare(ServerPlayer p){
        io.github.manasmods.tensura.menu.ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        RaceAPI.getRaceFrom(p).setRace(HeavenlyRestriction.RESTRICTED.getLast().get(),false);
        var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();i.setMastery(i.getMaxMastery());
        TensuraStorages.getExistenceFrom(p).setTrueHero(true);ChainedState.refresh(p);return i;
    }
    private static ManasSkillInstance evolve(GameTestHelper h,ServerPlayer p){
        prepare(p);ChainedEvolution.discover(p);var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();
        h.assertTrue(HeavenlyRestriction.UNBOUND.get().addLearnPoint(i,p,0,1),"Native final learning step did not complete evolution");return i;
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ultimateDashWorksInMidairAndStopsBeforeAnEnemy(GameTestHelper h){
        // Native FakePlayer deliberately ignores connection teleports. Use a tracked living
        // owner for movement assertions; the opt-in client covers a real evolved player.
        var anchor=player(h);var start=anchor.position().add(0,35,0);var p=target(h,start);EnergyHelper.setBaseMaxEP(p,100000);
        RestrictionData.learnReward(p,HeavenlyRestriction.UNBOUND.get());var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();var victim=target(h,start.add(8,0,0));
        afterTracking(h,List.of(p,victim),()->{
            p.setOnGround(false);p.setYRot(-90);p.setXRot(0);TensuraStorages.getExistenceFrom(p).setAura(10000);
            HeavenlyRestriction.UNBOUND.get().onPressed(i,p,0,0);
            h.assertTrue(p.getX()>start.x+6 && p.getX()<victim.getBoundingBox().minX,"Airborne Ultimate dash did not stop before target: start="+start+" end="+p.position()+" destination="+ChainedDash.destination(p)+" mastery="+i.getMastery()+" cooldown="+i.getCoolDown(0)+" interact="+i.canInteractSkill(p)+" activate="+i.canActivateSkill(p,0)+" aura="+TensuraStorages.getExistenceFrom(p).getAura());
            h.assertTrue(!p.getBoundingBox().intersects(victim.getBoundingBox()) && victim.getHealth()<1024,"Airborne dash failed its safe landing/damage");
            near(h,TensuraStorages.getExistenceFrom(p).getAura(),9800,"Ultimate airborne dash cost");h.assertTrue(i.getCoolDown(0)==1,"Ultimate airborne cooldown");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void airWalkMasteryHalvesActivationAndFlyingUpkeep(GameTestHelper h){
        var p=player(h);RestrictionData.learnReward(p,HeavenlyRestriction.AIR_SOVEREIGNTY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get()).orElseThrow();var art=HeavenlyRestriction.AIR_SOVEREIGNTY.get();
        var energy=TensuraStorages.getExistenceFrom(p);double normal=art.getAuraCost(p,i,0);i.setMastery(i.getMaxMastery());near(h,art.getAuraCost(p,i,0),normal*.5,"Mastered cost was not halved");
        energy.setAura(1000);i.onPressed(p,0,0);near(h,energy.getAura(),1000-normal*.5,"Mastered activation cost");
        for(int n=0;n<20;n++)AirSovereigntyArt.tick(p);near(h,energy.getAura(),1000-normal,"Mastered one-second upkeep");
        energy.setAura(0);i.onPressed(p,0,0);h.assertTrue(!p.getAbilities().flying,"Stopping was not free");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void forbiddenSkillsRemainDiscoverableAndTrainableButCannotFinish(GameTestHelper h){
        var p=player(h);prepare(p);var skills=SkillAPI.getSkillsFrom(p);var skill=ExtraSkills.MAGIC_SENSE.get();var latent=skill.createLearningInstance(p);latent.setMastery(-10);
        h.assertTrue(skills.learnSkill(latent),"Chained hid the unfinished skill from the native menu");
        h.assertTrue(skill.addLearnPoint(latent,p,0,1) && latent.getMastery()>-10 && latent.getMastery()<0,"Chained blocked unfinished training");
        latent.setMastery(-1);double mp=EnergyHelper.getBaseMaxMagicule(p);h.assertTrue(!skill.addLearnPoint(latent,p,0,1000),"Forbidden skill completed");
        near(h,latent.getMastery(),-1,"Completion failure did not retain learning progress");near(h,EnergyHelper.getBaseMaxMagicule(p),mp,"Blocked completion spent permanent MP");
        h.assertTrue(skills.getSkill(skill).orElseThrow()==latent,"Blocked skill disappeared from menu");
        skills.updateSkill(skill.createDefaultInstance(),true);h.assertTrue(skills.getSkill(skill).orElseThrow()==latent,"Update promoted an unfinished blocked skill");
        var message=ChainedState.blockedMessage(UniqueSkills.MUSICIAN.get());h.assertTrue(message.getString().equals("Cannot acquire "+UniqueSkills.MUSICIAN.get().getName().getString()+"."),"Notice added a reason");
        var colors=new ArrayList<net.minecraft.network.chat.TextColor>();message.visit((style,text)->{if(text.contains(UniqueSkills.MUSICIAN.get().getName().getString()))colors.add(style.getColor());return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        h.assertTrue(colors.contains(UniqueSkills.MUSICIAN.get().getColoredName().getStyle().getColor()),"Skill name lost native tier color");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ultimateDiscoveryRequiresEveryGateAndPreservesTheOriginalOnFailure(GameTestHelper h){
        var p=player(h);var old=prepare(p);var energy=TensuraStorages.getExistenceFrom(p);energy.setTrueHero(false);energy.setTrueDemonLord(false);
        ChainedEvolution.discover(p);h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.UNBOUND.get()).isEmpty(),"Unlocked without awakening status");
        energy.setTrueDemonLord(true);old.setMastery(old.getMaxMastery()-1);ChainedEvolution.discover(p);h.assertTrue(!ChainedEvolution.ready(p),"Unmastered Chained passed gate");
        old.setMastery(old.getMaxMastery());RaceAPI.getRaceFrom(p).setRace(HeavenlyRestriction.RESTRICTED.get(3).get(),false);ChainedEvolution.discover(p);h.assertTrue(!ChainedEvolution.ready(p),"Earlier race passed gate");
        RaceAPI.getRaceFrom(p).setRace(HeavenlyRestriction.RESTRICTED.getLast().get(),false);ChainedEvolution.discover(p);var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();
        h.assertTrue(i.getMastery()<0 && !ChainedState.ultimate(p),"Discovery granted ultimate powers early");energy.setTrueDemonLord(false);
        h.assertTrue(!HeavenlyRestriction.UNBOUND.get().addLearnPoint(i,p,0,1000) && i.getMastery()<0 && SkillAPI.getSkillsFrom(p).getSkill(old.getSkill()).isPresent(),"Lost gate consumed predecessor");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeMenuEvolutionPreservesResourcesMasteryToggleCooldownAndAllPresets(GameTestHelper h){
        var p=player(h);var old=prepare(p);old.onToggleOn(p);old.setCoolDown(1,0);ChainedState.remember(p,old.getSkill());
        var ability=TensuraStorages.getAbilityFrom(p);for(int preset=0;preset<9;preset++)ability.setAbilitySlot(preset,1,old.getSkill(),0);
        p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(0);
        double aura=EnergyHelper.getMaxAura(p),mp=EnergyHelper.getMaxMagicule(p),starting=old.getOrCreateTag().getDouble("starting_aura");
        ChainedEvolution.discover(p);var skills=SkillAPI.getSkillsFrom(p);var next=skills.getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();ability.setAbilitySlot(0,0,next.getSkill(),0);
        skills.startHoldSkill(next,0,0);
        h.assertTrue(next.getMastery()>=0 && next.isMastered(p) && next.isToggled() && next.getModes()==2,"Native activation failed to inherit completed mastery/toggle");
        h.assertTrue(skills.getSkill(HeavenlyRestriction.CHAINED.get()).isEmpty() && ChainedState.instance(p).orElseThrow()==next,"Evolution retained both vows");
        near(h,EnergyHelper.getMaxAura(p),aura,"Evolution changed aura capacity");near(h,EnergyHelper.getMaxMagicule(p),mp,"Evolution recaptured magicule ceiling");near(h,next.getOrCreateTag().getDouble("starting_aura"),starting,"Lost birth exemption");
        h.assertTrue(next.getCoolDown(0)==1 && ChainedState.inherited(p,next.getSkill()),"Lost cooldown or inheritance");
        for(int preset=0;preset<9;preset++)h.assertTrue(ability.getAbilitySlot(preset,1).getSkill()==next.getSkill(),"Preset was not migrated");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ultimateDamageUsesEightOrFiveAndCritsWithToggleOffToo(GameTestHelper h){
        var p=player(h);var i=evolve(h,p);var target=target(h,p.position().add(4,0,0));i.onToggleOn(p);
        var art=UtilityArts.DIAMOND_PATH.get().createDefaultInstance();var physical=p.damageSources().playerAttack(p);var battle=HeavenlyRestriction.CHAINED.get().createSource(art,p,DamageTypes.MOB_ATTACK,0);
        double crit=p.getAttributeValue(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER);var amount=Changeable.of(10f);ChainedState.damage(SkillAPI.getSkillsFrom(target),target,battle,amount);near(h,amount.get(),80*crit,"Battlewill used stacked/wrong branches");
        amount=Changeable.of(10f);ChainedState.damage(SkillAPI.getSkillsFrom(target),target,physical,amount);near(h,amount.get(),50*crit,"Physical multiplier/crit");
        i.onToggleOff(p);amount=Changeable.of(10f);ChainedState.damage(SkillAPI.getSkillsFrom(target),target,battle,amount);near(h,amount.get(),10*crit,"True critical passive was tied to combat toggle");
        var event=new CriticalHitEvent(p,target,1,false);UnboundCritical.critical(event);h.assertTrue(event.isCriticalHit(),"Native melee did not become critical");near(h,event.getDamageMultiplier(),crit,"Native melee critical multiplier");
        try(var scope=UnboundCritical.melee(p)){UnboundCritical.critical(event);amount=Changeable.of((float)(10*crit));ChainedState.damage(SkillAPI.getSkillsFrom(target),target,physical,amount);near(h,amount.get(),10*crit,"Native melee crit was applied twice");}
        amount=Changeable.of(10f);ChainedState.damage(SkillAPI.getSkillsFrom(target),target,physical,amount);near(h,amount.get(),10*crit,"Melee scope leaked to later hits");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void evolvedNumericBattlewillBuffBecomesFourfoldWithoutCompounding(GameTestHelper h){
        var p=player(h);var old=prepare(p);old.onToggleOff(p);var art=UtilityArts.DIAMOND_PATH.get();RestrictionData.learnReward(p,art);var a=SkillAPI.getSkillsFrom(p).getSkill(art).orElseThrow();double armor=p.getArmorValue();a.onPressed(p,0,0);double nativeBonus=p.getArmorValue()-armor;
        old.onToggleOn(p);near(h,p.getArmorValue()-armor,nativeBonus*3,"Original buff strength");ChainedEvolution.discover(p);var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();HeavenlyRestriction.UNBOUND.get().addLearnPoint(i,p,0,1);
        ChainedState.refresh(p);near(h,p.getArmorValue()-armor,nativeBonus*4,"Ultimate did not upgrade a live native buff");ChainedState.refresh(p);near(h,p.getArmorValue()-armor,nativeBonus*4,"Refresh compounded ultimate buff");i.onToggleOff(p);near(h,p.getArmorValue()-armor,nativeBonus,"Ultimate toggle did not restore native buff");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void unerringBodyReducesDamageOnlyInSlotAndRespectsNativeBypass(GameTestHelper h){
        var p=player(h);var old=prepare(p);var ability=TensuraStorages.getAbilityFrom(p);ability.setAbilitySlot(0,0,old.getSkill(),0);var amount=Changeable.of(100f);old.getSkill().onTakenDamage(old,p,p.damageSources().generic(),amount);near(h,amount.get(),90,"Chained body reduction");
        var i=evolve(h,p);ability.setAbilitySlot(0,0,i.getSkill(),0);amount=Changeable.of(100f);i.getSkill().onTakenDamage(i,p,p.damageSources().generic(),amount);near(h,amount.get(),70,"Ultimate body reduction");
        amount=Changeable.of(100f);i.getSkill().onTakenDamage(i,p,p.damageSources().genericKill(),amount);near(h,amount.get(),100,"Body mitigated operator bypass");
        var source=HeavenlyRestriction.UNBOUND.get().createSource(i,p,DamageTypes.GENERIC,0);((TensuraDamageSource)source).tensura$setBarrierBypassLevel(2);amount=Changeable.of(100f);i.getSkill().onTakenDamage(i,p,source,amount);near(h,amount.get(),100,"Body ignored Strengthen Body bypass rule");
        ability.setAbilitySlot(0,0,null,0);amount=Changeable.of(100f);i.getSkill().onTakenDamage(i,p,p.damageSources().generic(),amount);near(h,amount.get(),100,"Body reduction remained unslotted");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void counterEntryRootsPaysOnceAndEarlyCancellationIsFreeDuringCooldown(GameTestHelper h){
        var p=player(h);var i=evolve(h,p);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);double speed=p.getAttributeValue(Attributes.MOVEMENT_SPEED);UnboundCounter.press(i,p);
        h.assertTrue(UnboundCounter.active(p) && i.getCoolDown(1)==10,"Counter did not start");near(h,energy.getAura(),900,"Counter did not pay 10% current aura");near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),0,"Counter did not root movement");
        h.assertTrue(i.canIgnoreCoolDown(p,1),"Native menu cannot cancel while cooling down");UnboundCounter.press(i,p);h.assertTrue(!UnboundCounter.active(p),"Recast did not cancel");near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),speed,"Cancel did not restore movement");near(h,energy.getAura(),900,"Cancel charged aura");
        UnboundCounter.press(i,p);near(h,energy.getAura(),900,"Cooldown allowed a new stance");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void counterNegatesOneEnemyHitAndCutsThroughTerrainWithPhysicalSource(GameTestHelper h){
        var p=player(h);var i=evolve(h,p);i.onToggleOff(p);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10000);
        var enemy=target(h,p.position().add(10,0,0));var behind=target(h,p.position().add(13,0,0));var outside=target(h,p.position().add(10,0,5));
        for(int y=-2;y<=4;y++)for(int z=-4;z<=4;z++)h.getLevel().setBlockAndUpdate(p.blockPosition().offset(5,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        afterTracking(h,List.of(enemy,behind,outside),()->{
            double hp=p.getHealth(),shp=energy.getSpiritualHealth();UnboundCounter.press(i,p);var seen=new ArrayList<Float>();var source=new DamageSource[1];
            java.util.function.Consumer<LivingIncomingDamageEvent> observer=e->{if(e.getEntity()==enemy){seen.add(e.getAmount());source[0]=e.getSource();}};NeoForge.EVENT_BUS.addListener(observer);
            try{var incoming=new LivingIncomingDamageEvent(p,new DamageContainer(p.damageSources().mobAttack(enemy),20));NeoForge.EVENT_BUS.post(incoming);h.assertTrue(incoming.isCanceled(),"Enemy damage event was not negated");}finally{NeoForge.EVENT_BUS.unregister(observer);}
            near(h,p.getHealth(),hp,"Counter leaked HP damage");near(h,energy.getSpiritualHealth(),shp,"Counter leaked SHP damage");near(h,energy.getAura(),9000,"Counter paid twice");
            h.assertTrue(!UnboundCounter.active(p) && seen.size()==1,"Counter did not consume exactly one hit");near(h,seen.getFirst(),UnboundCounter.damage(20,1000),"Counter base formula");
            h.assertTrue(source[0].is(DamageTypes.MOB_ATTACK) && !TensuraDamageHelper.isBattlewill(source[0],p),"Counter lost purely Physical classification");
            h.assertTrue(enemy.getHealth()<1024 && behind.getHealth()<1024 && outside.getHealth()==1024,"Slash missed terrain-shielded targets or exceeded its radius");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void counterDoesNotTriggerOnEnvironmentSelfOrOperatorDamage(GameTestHelper h){
        var p=player(h);var i=evolve(h,p);TensuraStorages.getExistenceFrom(p).setAura(1000);UnboundCounter.press(i,p);
        for(var source:List.of(p.damageSources().generic(),p.damageSources().playerAttack(p),p.damageSources().genericKill())){
            var event=new LivingIncomingDamageEvent(p,new DamageContainer(source,10));UnboundCounter.incoming(event);h.assertTrue(!event.isCanceled() && UnboundCounter.active(p),"Counter intercepted ineligible damage");
        }
        SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());h.assertTrue(!UnboundCounter.active(p) && p.getAttributeValue(Attributes.MOVEMENT_SPEED)>0,"Forgetting left a rooted stance");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void counterTimesOutAfterThreeSecondsAndRestoresMovement(GameTestHelper h){
        var p=player(h);var i=evolve(h,p);TensuraStorages.getExistenceFrom(p).setAura(1000);UnboundCounter.press(i,p);
        h.runAfterDelay(61,()->{UnboundCounter.tick(p);h.assertTrue(!UnboundCounter.active(p) && p.getAttributeValue(Attributes.MOVEMENT_SPEED)>0,"Stance outlived three seconds");near(h,TensuraStorages.getExistenceFrom(p).getAura(),900,"Timeout refunded or charged aura");h.succeed();});
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void actualPlayerMeleeAndOwnedProjectileCritExactlyOnce(GameTestHelper h){
        var p=player(h);var i=evolve(h,p);i.onToggleOff(p);p.setOnGround(true);p.setSprinting(false);p.fallDistance=0;
        var target=target(h,p.position().add(2,0,0));target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        afterTracking(h,List.of(target),()->{
            try{var timer=LivingEntity.class.getDeclaredField("attackStrengthTicker");timer.setAccessible(true);timer.setInt(p,1000);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            double expected=p.getAttributeValue(Attributes.ATTACK_DAMAGE)*p.getAttributeValue(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER);float before=target.getHealth();p.attack(target);near(h,before-target.getHealth(),expected,"Actual native melee did not critically hit exactly once");
            target.invulnerableTime=0;target.setHealth(1024);var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),null);
            target.hurt(p.damageSources().arrow(arrow,p),10);near(h,1024-target.getHealth(),10*p.getAttributeValue(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER),"Actual owned projectile did not critically hit exactly once");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ultimateBodyDoublesOrdinaryDodgeAndKeepsProjectileAndBypassRules(GameTestHelper h){
        var p=player(h);var i=evolve(h,p);TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,i.getSkill(),0);p.getRandom().setSeed(123);
        int dodged=0;for(int n=0;n<1000;n++)if(!i.getSkill().onBeingDamaged(i,p,p.damageSources().generic(),10))dodged++;
        h.assertTrue(dodged>450 && dodged<550,"Ultimate body does not have 50% ordinary dodge: "+dodged);
        var shooter=target(h,p.position().add(4,0,0));var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),shooter,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),null);var source=p.damageSources().arrow(arrow,shooter);
        h.assertTrue(!i.getSkill().onBeingDamaged(i,p,source,10),"Ultimate lost guaranteed projectile dodge");((TensuraDamageSource)source).tensura$setDodgeBypass();h.assertTrue(i.getSkill().onBeingDamaged(i,p,source,10),"Ultimate ignored explicit dodge bypass");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void directSpiritualAttacksCritAndCounterWithoutLeakingSHPDamage(GameTestHelper h){
        var p=player(h);var i=evolve(h,p);i.onToggleOff(p);var victim=target(h,p.position().add(8,0,0));
        victim.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(1000);var targetEnergy=TensuraStorages.getExistenceFrom(victim);targetEnergy.setSpiritualHealth(1000);
        TensuraDamageHelper.directSpiritualHurt(victim,p,null,10,0);near(h,targetEnergy.getSpiritualHealth(),1000-10*p.getAttributeValue(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER),"Direct SHP attack did not critically hit exactly once");
        var energy=TensuraStorages.getExistenceFrom(p);energy.setSpiritualHealth(1000);energy.setAura(10000);
        afterTracking(h,List.of(victim),()->{UnboundCounter.press(i,p);double hp=p.getHealth(),shp=energy.getSpiritualHealth();boolean hit=TensuraDamageHelper.directSpiritualHurt(p,victim,null,20,0);
            h.assertTrue(!hit && !UnboundCounter.active(p) && victim.getHealth()<1024,"Direct SHP attack bypassed Counter");near(h,p.getHealth(),hp,"Direct-SHP Counter lost HP");near(h,energy.getSpiritualHealth(),shp,"Direct-SHP Counter lost SHP");near(h,energy.getAura(),9000,"Direct-SHP Counter charged twice");h.succeed();});
    }
}
