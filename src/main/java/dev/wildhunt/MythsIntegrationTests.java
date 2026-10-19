package dev.wildhunt;

import dev.wildhunt.myths.*;
import io.github.manasmods.manascore.race.api.*;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class MythsIntegrationTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MythsTest"));
        p.setPos(h.absolutePos(new net.minecraft.core.BlockPos(1,1,1)).getCenter());
        RaceAPI.getRaceFrom(p).setRace(WildHunt.HUNTERS.get(2).get(),false);
        EnergyHelper.setBaseMaxEP(p,20_000_000);TensuraStorages.getExistenceFrom(p).setTrueHero(true);
        TensuraStorages.getExistenceFrom(p).setAura(10_000_000);
        return p;
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void allNativePairsRegisterAsDifferentRaces(GameTestHelper h) {
        h.assertTrue(FusionCatalog.RACES.size()==105,"Expected all 105 ancestral pairs");
        var names=new java.util.HashSet<String>();var gains=new java.util.HashSet<Double>();
        for(var profile:FusionCatalog.PROFILES.values()) {
            var race=FusionCatalog.RACES.get(profile.key()).get();
            h.assertTrue(RaceAPI.getRaceRegistry().get(race.getRegistryName())==race,"Fusion not in native registry");
            h.assertTrue(names.add(race.getName().getString()),"Fusion name reused");
            h.assertTrue(!race.getAttributeModifiers().isEmpty(),"Fusion stats are missing from native race metadata");
            h.assertTrue(gains.add(profile.aura()),"Fusion growth profile reused");
            h.assertTrue(FusionCatalog.profile(profile.first(),profile.second())==FusionCatalog.profile(profile.second(),profile.first()),"Reverse pair differs");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void secondRaceUsesNativeEvolutionAndHalfBaseGain(GameTestHelper h) {
        var p=player(h);DuoSystem.select(p,RaceFamily.SLIME);
        h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.SLIME.get(),"Secondary did not start at base Slime");
        var gains=DuoSystem.gains(p,2,1);h.assertTrue(gains.aura()==2.5 && gains.magicules()==2,"Human-Slime weights differ from requested rule");
        var message=DuoSystem.evolve(p,TensuraRaces.DEMON_SLIME.getId());
        h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.DEMON_SLIME.get(),"Native awakened evolution failed: "+message);
        DuoSystem.evolve(p,TensuraRaces.GOD_SLIME.getId());
        h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.GOD_SLIME.get(),"Secondary never reaches final evolution");
        h.assertTrue(DuoSystem.gains(p,2,1).equals(gains),"Secondary stage incorrectly changes base ancestry gain");
        var serialized=DuoSystem.secondary(p).orElseThrow().toNBT();var restored=ManasRaceInstance.fromNBT(serialized);
        h.assertTrue(restored.getRace()==TensuraRaces.GOD_SLIME.get(),"Secondary native state did not roundtrip");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeKillAwardUsesDuoWeightsBeforeCaps(GameTestHelper h) {
        var p=player(h);DuoSystem.select(p,RaceFamily.SLIME);
        var target=h.spawnWithNoFreeWill(EntityType.ZOMBIE,3,1,1);
        double a=EnergyHelper.getBaseMaxAura(p),m=EnergyHelper.getBaseMaxMagicule(p);
        io.github.manasmods.tensura.handler.DeathHandler.gainEPEntity(p,target,100);
        double aa=EnergyHelper.getBaseMaxAura(p)-a,mm=EnergyHelper.getBaseMaxMagicule(p)-m;
        double attrA=p.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.AURA_GAIN);
        double attrM=p.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAGICULE_GAIN);
        h.assertTrue(Math.abs(aa-attrA*2.5)<.0001 && Math.abs(mm-attrM*2)<.0001,"Native award did not apply the secondary base weights exactly once");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void convergenceRequiresBothTerminalAndIsOneTime(GameTestHelper h) {
        var p=player(h);DuoSystem.select(p,RaceFamily.SLIME);
        DuoSystem.convergence(p);h.assertTrue(!(RaceAPI.getRaceFrom(p).getRace().orElseThrow().getRace() instanceof FusionRace),"Base secondary was allowed to fuse");
        DuoSystem.evolve(p,TensuraRaces.DEMON_SLIME.getId());DuoSystem.evolve(p,TensuraRaces.GOD_SLIME.getId());
        double before=EnergyHelper.getBaseMaxEP(p);var result=DuoSystem.convergence(p);
        h.assertTrue(DuoSystem.converged(p),"Convergence rejected: "+result);
        h.assertTrue(RaceAPI.getRaceFrom(p).getRace().orElseThrow().getRace()==FusionCatalog.RACES.get("human_slime").get(),"Wrong unique fusion registered");
        h.assertTrue(Math.abs(EnergyHelper.getBaseMaxEP(p)-before*1.25)<1,"Awakening gain did not apply once");
        double after=EnergyHelper.getBaseMaxEP(p);DuoSystem.convergence(p);
        h.assertTrue(EnergyHelper.getBaseMaxEP(p)==after,"Awakening can be farmed");
        var restored=ManasRaceInstance.fromNBT(RaceAPI.getRaceFrom(p).getRace().orElseThrow().toNBT());
        h.assertTrue(restored.getRace() instanceof FusionRace,"Fused race fails native saving");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void selectionRejectsWrongGatesAndCannotBeSwapped(GameTestHelper h) {
        var p=player(h);TensuraStorages.getExistenceFrom(p).setTrueHero(false);
        DuoSystem.select(p,RaceFamily.SLIME);h.assertTrue(DuoSystem.secondary(p).isEmpty(),"Unawakened soul bypassed selection");
        TensuraStorages.getExistenceFrom(p).setTrueHero(true);EnergyHelper.setBaseMaxEP(p,4_999_999);
        DuoSystem.select(p,RaceFamily.SLIME);h.assertTrue(DuoSystem.secondary(p).isEmpty(),"EP gate bypassed");
        EnergyHelper.setBaseMaxEP(p,20_000_000);DuoSystem.select(p,RaceFamily.HUMAN);h.assertTrue(DuoSystem.secondary(p).isEmpty(),"Same-family combination allowed");
        DuoSystem.select(p,RaceFamily.SLIME);DuoSystem.select(p,RaceFamily.DAEMON);
        h.assertTrue(DuoSystem.secondaryFamily(p).orElseThrow()==RaceFamily.SLIME,"Secondary choice can be swapped");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlyAscensionRetainsUltimateCompatibilityAndPresets(GameTestHelper h) {
        var p=player(h);var skill=WildHunt.ULTIMATE.get(Apex.REY_DAU).get();HunterData.learnReward(p,skill);
        var original=SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();original.setMastery(skill.getMaxMastery());original.setCoolDown(19,0);
        var abilities=TensuraStorages.getAbilityFrom(p);abilities.setAbilitySlot(8,2,skill,1);
        var result=MythsProgression.ascend(p,skill.getRegistryName().toString());
        var evolved=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get());
        h.assertTrue(evolved.isPresent(),"Godly evolution rejected: "+result);
        var delegate=SkillAPI.getSkillRegistry().delegate(WildHunt.GODLY.get().getRegistryName());
        h.assertTrue(delegate.is(MythsTiers.GODLY) && delegate.is(io.github.manasmods.tensura.data.TensuraSkillTags.ULTIMATE_SKILLS),"Godly or native Ultimate tag missing");
        h.assertTrue(WildHunt.GODLY.get().getType()==io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE,"Godly breaks native Ultimate compatibility");
        h.assertTrue(abilities.getAbilitySlot(8,2).getSkill()==WildHunt.GODLY.get(),"Ninth preset was lost");
        h.assertTrue(evolved.orElseThrow().getCoolDown(0)==19,"Ascension reset source cooldown");
        h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(skill).isEmpty(),"Source Ultimate was duplicated");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlyReversalRecoversPositionWithoutResettingEnergy(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var start=p.position();WildHunt.GODLY.get().onPressed(i,p,0,2);
        h.assertTrue(i.getOrCreateTag().contains("anchor_until"),"Causal checkpoint was not set");
        i.setCoolDown(0,2);p.teleportTo(start.x+2,start.y,start.z);p.setHealth(Math.max(1,p.getHealth()-20));
        double aura=TensuraStorages.getExistenceFrom(p).getAura();WildHunt.GODLY.get().onPressed(i,p,0,2);
        h.assertTrue(p.position().distanceTo(start)<.1,"Reversal did not return to safe anchor");
        h.assertTrue(TensuraStorages.getExistenceFrom(p).getAura()<aura,"Reversal restored or did not pay energy");
        h.assertTrue(i.getCoolDown(2)==25,"Reversal bypasses its cooldown");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void secondarySurvivesDeathAndRetainsPrimaryAlignment(GameTestHelper h) {
        var p=player(h);RaceAPI.getRaceFrom(p).setRace(TensuraRaces.DIVINE_HUMAN.get(),false);
        EnergyHelper.setBaseMaxEP(p,20_000_000);DuoSystem.select(p,RaceFamily.SLIME);
        var g=DuoSystem.gains(p,1,2);
        h.assertTrue(g.aura()==1.5 && g.magicules()==3,"Duo overwrote the current Holy primary's growth weights");
        var clone=player(h);HunterEvents.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone,p,true));
        h.assertTrue(DuoSystem.secondary(clone).orElseThrow().getRace()==TensuraRaces.SLIME.get(),"Death lost secondary ancestry");
        DuoSystem.data(clone).putString("secondary_family","daemon");
        h.assertTrue(DuoSystem.secondaryFamily(p).orElseThrow()==RaceFamily.SLIME,"Death clone shares mutable Duo state");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlySeverPiercesEightTargetsAndChargesAura(GameTestHelper h) {
        var p=player(h);p.setPos(p.position().add(896,80,896));p.setYRot(-90);p.setXRot(0);
        var targets=new java.util.ArrayList<net.minecraft.world.entity.monster.Zombie>();
        for(int n=0;n<9;n++) {
            var pos=p.position().add(3+n*3,0,0);h.getLevel().getChunk(net.minecraft.core.BlockPos.containing(pos));
            var z=h.spawnWithNoFreeWill(EntityType.ZOMBIE,2,1,2);z.setPos(pos);z.setNoGravity(true);
            z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1024);z.setHealth(1024);targets.add(z);
        }
        HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        double aura=TensuraStorages.getExistenceFrom(p).getAura();WildHunt.GODLY.get().onPressed(i,p,0,0);
        for(int n=0;n<8;n++)h.assertTrue(targets.get(n).getHealth()<1024,"Spatial ray missed pierced target "+n);
        h.assertTrue(targets.get(8).getHealth()==1024,"Spatial ray exceeded its eight-target cap");
        h.assertTrue(Math.abs(aura-TensuraStorages.getExistenceFrom(p).getAura()-3200)<.01,"Spatial ray charged the wrong pool or cost");
        h.assertTrue(i.getCoolDown(0)==4,"Spatial ray cooldown differs");
        h.assertTrue(i.getOrCreateTag().getList("sever_echoes",10).size()==8,"Sever did not record exactly eight echo victims");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlyVerdictCanCastAloneAndConsumesItsFieldForEmpowerment(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        double aura=TensuraStorages.getExistenceFrom(p).getAura();WildHunt.GODLY.get().onPressed(i,p,0,3);
        h.assertTrue(TensuraStorages.getExistenceFrom(p).getAura()==aura-15000 && i.onCoolDown(3) && !i.getOrCreateTag().getBoolean("verdict_empowered"),"Standalone Verdict failed or received unearned empowerment");
        i.setCoolDown(0,3);
        WildHunt.GODLY.get().onPressed(i,p,0,1);h.assertTrue(i.getOrCreateTag().contains("horizon_until"),"Horizon did not persist its center/lifetime");
        WildHunt.GODLY.get().onPressed(i,p,0,3);
        h.assertTrue(!i.getOrCreateTag().contains("horizon_until") && i.getOrCreateTag().contains("verdict_until"),"Verdict did not consume its own field");
        h.assertTrue(i.getOrCreateTag().getBoolean("verdict_empowered"),"Consuming a Horizon did not empower Verdict");
        GodlySkill.clearTransient(p);h.assertTrue(!i.getOrCreateTag().contains("verdict_until"),"Dimension/relog cleanup left delayed damage active");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlyBarrierRecoversOnlyAfterQuietAndOwnsItsModifiers(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var barrier=p.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MULTILAYER_BARRIER);
        var own=WildHunt.id("aion_continuum");var foreign=WildHunt.id("test_foreign_godly");
        barrier.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(foreign,11,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        WildHunt.GODLY.get().onToggleOn(i,p);h.assertTrue(barrier.getModifier(own)!=null,"Godly passive has no native HUD barrier");barrier.removeModifier(own);
        i.getOrCreateTag().putLong("barrier_ready",p.level().getGameTime()+200);i.getOrCreateTag().putLong("upkeep",0);WildHunt.GODLY.get().tickActive(i,p);
        h.assertTrue(barrier.getModifier(own)==null,"Passive instantly repaired a broken barrier");
        i.getOrCreateTag().putLong("barrier_ready",p.level().getGameTime()-1);i.getOrCreateTag().putLong("upkeep",0);WildHunt.GODLY.get().tickActive(i,p);
        h.assertTrue(barrier.getModifier(own)!=null,"Passive did not recover after quiet time");WildHunt.GODLY.get().onToggleOff(i,p);
        h.assertTrue(barrier.getModifier(own)==null && barrier.getModifier(foreign)!=null,"Toggle cleanup removed another barrier's capacity");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void secondarySlimeActionHonorsNativeHoldAndRelease(GameTestHelper h) {
        var p=player(h);DuoSystem.select(p,RaceFamily.SLIME);p.setOnGround(true);p.setYRot(-90);p.setXRot(0);
        SecondaryAbility.input(p,"race_press");h.assertTrue(DuoSystem.data(p).contains("race_start"),"Secondary action did not start");
        h.runAfterDelay(2,()->{
            SecondaryAbility.tick(p);SecondaryAbility.tick(p);
            h.assertTrue(DuoSystem.secondary(p).orElseThrow().getOrCreateTag().getInt("chargeTick")==2,"Native Slime hold was not forwarded");
            SecondaryAbility.input(p,"race_release");
            h.assertTrue(!DuoSystem.data(p).contains("race_start") && p.getDeltaMovement().x>0,"Native Slime release did not launch the player or clear hold state");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void reversalRestoresRecordedSpiritualHealthAndRejectsBlockedReturns(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var energy=TensuraStorages.getExistenceFrom(p);energy.setSpiritualHealth(500);
        WildHunt.GODLY.get().onPressed(i,p,0,2);var anchor=p.blockPosition();i.setCoolDown(0,2);p.setPos(p.position().add(3,0,0));energy.setSpiritualHealth(100);
        h.getLevel().setBlockAndUpdate(anchor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        double aura=energy.getAura();WildHunt.GODLY.get().onPressed(i,p,0,2);
        h.assertTrue(energy.getAura()==aura && i.getOrCreateTag().contains("anchor_until"),"Blocked return charged energy or discarded a valid pending checkpoint");
        h.getLevel().setBlockAndUpdate(anchor,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());WildHunt.GODLY.get().onPressed(i,p,0,2);
        h.assertTrue(energy.getSpiritualHealth()==500 && energy.getAura()==aura-2400,"Reversal failed to restore recorded SHP or refunded energy");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void horizonBendsHostileProjectilesButPreservesFriendlyShots(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var center=p.position().add(0,2,0);var tag=i.getOrCreateTag();ApexArts.point(tag,"horizon",center);tag.putLong("horizon_until",h.getLevel().getGameTime()+200);tag.putString("dimension",h.getLevel().dimension().location().toString());
        var own=new net.minecraft.world.entity.projectile.Snowball(h.getLevel(),p);own.setPos(center);own.setDeltaMovement(1,0,0);h.getLevel().addFreshEntity(own);
        var hostile=new net.minecraft.world.entity.projectile.Snowball(EntityType.SNOWBALL,h.getLevel());hostile.setPos(center.add(1,0,0));hostile.setDeltaMovement(1,0,0);h.getLevel().addFreshEntity(hostile);
        WildHunt.GODLY.get().tickActive(i,p);
        h.assertTrue(own.getDeltaMovement().x==1 && hostile.getDeltaMovement().x<.7,"Projectile velocities own="+own.getDeltaMovement()+" hostile="+hostile.getDeltaMovement()+" own owner="+own.getOwner()+" field="+tag.getLong("horizon_until")+" now="+h.getLevel().getGameTime());h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void consumedHorizonVisualsAndForgottenEffectsAreRemoved(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        WildHunt.GODLY.get().onPressed(i,p,0,1);var tag=i.getOrCreateTag();var id=tag.getUUID("horizon_visual");var circle=tag.getUUID("horizon_circle");
        WildHunt.GODLY.get().onPressed(i,p,0,3);
        h.assertTrue((h.getLevel().getEntity(id)==null || h.getLevel().getEntity(id).isRemoved()) && (h.getLevel().getEntity(circle)==null || h.getLevel().getEntity(circle).isRemoved()),"Consumed Horizon left its old geometry or native circle active");
        WildHunt.GODLY.get().onToggleOn(i,p);var verdict=tag.getUUID("verdict_visual");WildHunt.GODLY.get().onForgetSkill(i,p);
        h.assertTrue(!i.isToggled() && !tag.contains("verdict_until") && (h.getLevel().getEntity(verdict)==null || h.getLevel().getEntity(verdict).isRemoved()),"Forgetting Aion left its passive or delayed visuals active");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void severEchoesStrikeTwiceWithoutChargingAgain(GameTestHelper h) {
        var p=player(h);p.setPos(p.position().add(0,100,0));p.setYRot(-90);p.setXRot(0);
        var pos=p.position().add(3,0,0);h.getLevel().getChunk(net.minecraft.core.BlockPos.containing(pos));
        var target=h.spawnWithNoFreeWill(EntityType.ZOMBIE,2,1,2);target.setPos(pos);target.setNoGravity(true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1024);target.setHealth(1024);target.setInvulnerable(true);
        HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();WildHunt.GODLY.get().onPressed(i,p,0,0);target.setInvulnerable(false);
        double aura=TensuraStorages.getExistenceFrom(p).getAura();
        h.runAfterDelay(13,()->{WildHunt.GODLY.get().tickActive(i,p);h.assertTrue(target.getHealth()<1024 && i.getOrCreateTag().getInt("echo_count")==1,"First echo: hp="+target.getHealth()+" count="+i.getOrCreateTag().getInt("echo_count")+" due="+i.getOrCreateTag().getLong("echo_next")+" now="+h.getLevel().getGameTime()+" victims="+i.getOrCreateTag().getList("sever_echoes",10).size()+" exists="+(h.getLevel().getEntity(target.getUUID())!=null)+" los="+p.hasLineOfSight(target)+" alive="+target.isAlive()+" invul="+target.isInvulnerable()+" hurt="+target.invulnerableTime+" shp="+TensuraStorages.getExistenceFrom(target).getSpiritualHealth());float after=target.getHealth();
            h.runAfterDelay(13,()->{WildHunt.GODLY.get().tickActive(i,p);h.assertTrue(target.getHealth()<after && !i.getOrCreateTag().contains("sever_echoes"),"Second causal echo did not strike or kept its victims active");h.assertTrue(TensuraStorages.getExistenceFrom(p).getAura()==aura,"Echoes charged aura again");h.succeed();});
        });
    }
}
