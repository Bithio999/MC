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
        TensuraStorages.getExistenceFrom(p).setMagicule(10_000_000);
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
    public static void secondaryUsesFixedStageEPAndKeepsPrimarySplit(GameTestHelper h) {
        var p=player(h);DuoSystem.select(p,RaceFamily.SLIME);
        h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.SLIME.get(),"Secondary did not start at base Slime");
        var gains=DuoSystem.gains(p,2,1);h.assertTrue(Math.abs(gains.aura()-2.2)<1e-9 && Math.abs(gains.magicules()-1.1)<1e-9,"Secondary bonus changed the native primary split");
        EnergyHelper.setBaseMaxEP(p,7_499_999);
        var blocked=DuoSystem.evolve(p,TensuraRaces.DEMON_SLIME.getId());
        h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.SLIME.get(),"Below-threshold evolution was allowed: "+blocked);
        h.assertTrue(DuoSystem.secondaryEvolutionEP(p)==7_500_000,"First secondary stage threshold is not fixed");
        var message=evolveSecondary(p,TensuraRaces.DEMON_SLIME.getId());
        h.assertTrue(DuoSystem.secondaryEvolutionEP(p)==10_000_000,"Second stage did not raise the EP gate");
        h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.DEMON_SLIME.get(),"Native awakened evolution failed: "+message);
        evolveSecondary(p,TensuraRaces.GOD_SLIME.getId());
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
        h.assertTrue(Math.abs(aa-attrA*2.2)<.0001 && Math.abs(mm-attrM*1.1)<.0001,"Native award did not apply the ten percent bonus exactly once");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void convergenceRequiresBothTerminalAndIsOneTime(GameTestHelper h) {
        var p=player(h);DuoSystem.select(p,RaceFamily.SLIME);
        DuoSystem.convergence(p);h.assertTrue(!(RaceAPI.getRaceFrom(p).getRace().orElseThrow().getRace() instanceof FusionRace),"Base secondary was allowed to fuse");
        evolveSecondary(p,TensuraRaces.DEMON_SLIME.getId());evolveSecondary(p,TensuraRaces.GOD_SLIME.getId());
        double required=DuoSystem.convergenceEP(p);
        h.assertTrue(required>EnergyHelper.getBaseMaxEP(p),"Awakening did not advance beyond the last secondary evolution gate");
        DuoSystem.convergence(p);h.assertTrue(!DuoSystem.converged(p),"Lineage-dependent awakening EP gate was bypassed");
        h.assertTrue(MythsNetwork.snapshot(p,"").getDouble("convergence_ep")==required,"Menu and server disagree on awakening EP");
        EnergyHelper.setBaseMaxEP(p,required);double before=EnergyHelper.getBaseMaxEP(p);var result=DuoSystem.convergence(p);
        h.assertTrue(DuoSystem.converged(p),"Convergence rejected: "+result);
        h.assertTrue(RaceAPI.getRaceFrom(p).getRace().orElseThrow().getRace()==FusionCatalog.RACES.get("human_slime").get(),"Wrong unique fusion registered");
        h.assertTrue(Math.abs(EnergyHelper.getBaseMaxEP(p)-before*1.25)<1,"Awakening gain did not apply once");
        var profile=FusionCatalog.PROFILES.get("human_slime");var growth=DuoSystem.gains(p,2,1);
        h.assertTrue(Math.abs(growth.aura()-profile.aura()*1.1)<1e-9 && Math.abs(growth.magicules()-profile.magicules()*1.1)<1e-9,"Fusion lost its profile or inherited growth bonus");
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
        var p=player(h);var skill=WildHunt.MYTHIC.get(MythicKind.VRITRA).get();HunterData.learnReward(p,skill);
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
    public static void godlyReversalRestoresReservesAfterCastPaymentAndReturnsForFree(GameTestHelper h) {
        var p=tank(h,h.absolutePos(new net.minecraft.core.BlockPos(1,101,1)).getCenter());EnergyHelper.setBaseMaxEP(p,20_000_000);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(100_000);energy.setMagicule(100_000);
        var start=p.position();WildHunt.GODLY.get().onPressed(i,p,0,2);
        h.assertTrue(energy.getMagicule()==88_000 && i.getOrCreateTag().getDouble("anchor_magicules")==88_000,"Snapshot refunded or preceded the initial cast payment");
        h.assertTrue(i.getOrCreateTag().contains("anchor_until"),"Causal checkpoint was not set");
        i.setCoolDown(0,2);p.setPos(start.add(2,0,0));p.setHealth(Math.max(1,p.getHealth()-20));
        energy.setAura(1);energy.setMagicule(0);
        h.assertTrue(WildHunt.GODLY.get().getMagiculeCost(p,i,2)==0,"Pending Reversal displays a recast cost");
        WildHunt.GODLY.get().onPressed(i,p,0,2);
        h.assertTrue(p.position().distanceTo(start)<.1,"Reversal did not return to safe anchor");
        h.assertTrue(energy.getAura()==100_000 && energy.getMagicule()==88_000,"Free return did not restore recorded reserves after the cast payment");
        h.assertTrue(i.getCoolDown(2)==25,"Reversal bypasses its cooldown");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void secondarySurvivesDeathAndRetainsPrimaryAlignment(GameTestHelper h) {
        var p=player(h);RaceAPI.getRaceFrom(p).setRace(TensuraRaces.DIVINE_HUMAN.get(),false);
        EnergyHelper.setBaseMaxEP(p,20_000_000);DuoSystem.select(p,RaceFamily.SLIME);
        var g=DuoSystem.gains(p,1,2);
        h.assertTrue(Math.abs(g.aura()-1.1)<1e-9 && Math.abs(g.magicules()-2.2)<1e-9,"Duo overwrote the current Holy primary's growth weights");
        var clone=player(h);HunterEvents.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone,p,true));
        h.assertTrue(DuoSystem.secondary(clone).orElseThrow().getRace()==TensuraRaces.SLIME.get(),"Death lost secondary ancestry");
        DuoSystem.data(clone).putString("secondary_family","daemon");
        h.assertTrue(DuoSystem.secondaryFamily(p).orElseThrow()==RaceFamily.SLIME,"Death clone shares mutable Duo state");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlyChannelPiercesEightTargetsAndChargesMagicules(GameTestHelper h) {
        var p=player(h);p.setPos(p.position().add(896,80,896));p.setYRot(-90);p.setXRot(0);
        var targets=new java.util.ArrayList<net.minecraft.world.entity.monster.Zombie>();
        for(int n=0;n<9;n++) {
            var pos=p.position().add(3+n*3,0,0);h.getLevel().getChunk(net.minecraft.core.BlockPos.containing(pos));
            var z=h.spawnWithNoFreeWill(EntityType.HUSK,2,1,2);z.setPos(pos);z.setNoGravity(true);
            z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1024);z.setHealth(1024);targets.add(z);
        }
        HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        double aura=TensuraStorages.getExistenceFrom(p).getMagicule();WildHunt.GODLY.get().onPressed(i,p,0,0);
        for(int n=0;n<8;n++)h.assertTrue(targets.get(n).getHealth()<1024,"Spatial ray missed pierced target "+n);
        h.assertTrue(targets.get(8).getHealth()==1024,"Spatial ray exceeded its eight-target cap");
        h.assertTrue(Math.abs(aura-TensuraStorages.getExistenceFrom(p).getMagicule()-2400)<.01,"Spatial ray charged the wrong pool or cost");
        h.assertTrue(i.getCoolDown(0)==0 && i.getOrCreateTag().contains("channel_until"),"Channel did not start before its release cooldown");
        WildHunt.GODLY.get().onRelease(i,p,1,0,0);h.assertTrue(i.getCoolDown(0)==4 && !i.getOrCreateTag().contains("channel_until"),"Release did not stop the channel and start cooldown");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlyVerdictCanCastAloneAndConsumesItsFieldForEmpowerment(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        double aura=TensuraStorages.getExistenceFrom(p).getMagicule();WildHunt.GODLY.get().onPressed(i,p,0,3);
        h.assertTrue(TensuraStorages.getExistenceFrom(p).getMagicule()==aura-28000 && i.onCoolDown(3) && !i.getOrCreateTag().getBoolean("verdict_empowered"),"Standalone Verdict failed or received unearned empowerment");
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
        var energy=TensuraStorages.getExistenceFrom(p);energy.setSpiritualHealth(500);energy.setMagicule(100_000);
        WildHunt.GODLY.get().onPressed(i,p,0,2);var anchor=p.blockPosition();i.setCoolDown(0,2);p.setPos(p.position().add(3,0,0));energy.setSpiritualHealth(100);
        h.getLevel().setBlockAndUpdate(anchor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        double aura=energy.getMagicule();WildHunt.GODLY.get().onPressed(i,p,0,2);
        h.assertTrue(energy.getMagicule()==aura && i.getOrCreateTag().contains("anchor_until"),"Blocked return charged energy or discarded a valid pending checkpoint");
        h.getLevel().setBlockAndUpdate(anchor,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());WildHunt.GODLY.get().onPressed(i,p,0,2);
        h.assertTrue(energy.getSpiritualHealth()==500 && energy.getMagicule()==aura,"Free Reversal failed to restore recorded SHP or charged for returning");h.succeed();
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
    public static void channelTracksAimBillsPulsesAndStopsOnRelease(GameTestHelper h) {
        var p=player(h);p.setPos(p.position().add(0,260,0));p.setYRot(-90);p.setXRot(0);
        var first=tank(h,p.position().add(3,0,0));var second=tank(h,p.position().add(0,0,3));
        for(var target:java.util.List.of(first,second)){var chunk=new net.minecraft.world.level.ChunkPos(target.blockPosition());h.getLevel().setChunkForced(chunk.x,chunk.z,true);}
        HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var energy=TensuraStorages.getExistenceFrom(p);double aura=energy.getAura();WildHunt.GODLY.get().onPressed(i,p,0,0);
        float after=first.getHealth();h.assertTrue(after<1024 && second.getHealth()==1024,"Channel opener did not follow initial aim");
        double mp=energy.getMagicule();p.setYRot(0);
        h.runAfterDelay(11,()->{
            h.assertTrue(WildHunt.GODLY.get().onHeld(i,p,11,0),"Native held callback did not continue channel");
            h.assertTrue(second.getHealth()<1024 && first.getHealth()==after,"Channel pulse: first="+first.getHealth()+" expected="+after+" second="+second.getHealth()+" aim="+ApexArts.point(i.getOrCreateTag(),"channel_aim"));
            h.assertTrue(energy.getMagicule()==mp-1200 && energy.getAura()==aura,"Channel billed the wrong resource or pulse cost");
            WildHunt.GODLY.get().onRelease(i,p,11,0,0);
            h.assertTrue(!WildHunt.GODLY.get().onHeld(i,p,12,0) && !i.getOrCreateTag().hasUUID("channel_visual"),"Released channel retained a damaging beam");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void channelStopsWhenMagiculesAreExhausted(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        TensuraStorages.getExistenceFrom(p).setMagicule(2400);WildHunt.GODLY.get().onPressed(i,p,0,0);
        i.getOrCreateTag().putLong("channel_next",0);
        h.assertTrue(!WildHunt.GODLY.get().onHeld(i,p,10,0) && !i.getOrCreateTag().contains("channel_until") && i.getCoolDown(0)==4,"Unfunded channel continued");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlyOwnershipDefeatsResistancesWithoutToggleOrStacking(GameTestHelper h) {
        var p=player(h);var target=tank(h,p.position().add(2,100,0));
        var nullification=io.github.manasmods.tensura.registry.skill.ResistanceSkills.SPATIAL_ATTACK_NULLIFICATION.get();
        var resistance=io.github.manasmods.tensura.registry.skill.ResistanceSkills.SPATIAL_ATTACK_RESISTANCE.get();
        HunterData.learnReward(target,nullification);HunterData.learnReward(target,resistance);
        var ni=SkillAPI.getSkillsFrom(target).getSkill(nullification).orElseThrow();ni.setToggled(true);
        var ri=SkillAPI.getSkillsFrom(target).getSkill(resistance).orElseThrow();ri.setToggled(true);
        var i=WildHunt.GODLY.get().createDefaultInstance();
        var source=WildHunt.GODLY.get().createSource(i,p,io.github.manasmods.tensura.damage.TensuraDamageTypes.SPACE_ELEMENTAL,0);
        h.assertTrue(!nullification.onBeingDamaged(ni,target,source,100),"Fixture did not initially reject spatial damage");
        var thoth=WildHunt.MYTHIC.get(MythicKind.THOTH).get();HunterData.learnReward(p,thoth);
        TensuraStorages.getExistenceFrom(p).setMagicule(0);
        h.assertTrue(nullification.onBeingDamaged(ni,target,source,100),"Untoggled, unfunded Thoth owner did not bypass nullification");
        var amount=io.github.manasmods.manascore.network.api.util.Changeable.of(100f);
        h.assertTrue(resistance.onTakenDamage(ri,target,source,amount) && amount.get()==100f,"Resistance retained damage reduction against a Godly owner");
        GodlyPassives.modifyDamage(SkillAPI.getSkillsFrom(target),target,source,amount);
        h.assertTrue(Math.abs(amount.get()-135)<.001,"Classification damage bonus missing");
        HunterData.learnReward(p,WildHunt.GODLY.get());amount.set(100f);GodlyPassives.modifyDamage(SkillAPI.getSkillsFrom(target),target,source,amount);
        h.assertTrue(Math.abs(amount.get()-135)<.001,"Multiple Godly skills stacked the bonus");
        HunterData.learnReward(target,WildHunt.GODLY.get());amount.set(100f);GodlyPassives.modifyDamage(SkillAPI.getSkillsFrom(target),target,source,amount);
        h.assertTrue(amount.get()==100f,"Godly defender incorrectly took the non-Godly damage bonus");
        target.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.SPACE_RESISTANCE).setBaseValue(.9);
        float before=target.getHealth();target.hurt(source,100);
        h.assertTrue(before-target.getHealth()>50,"Native elemental resistance still blocked an untoggled Godly owner's damage");
        SkillAPI.getSkillsFrom(p).forgetSkill(WildHunt.GODLY.get());
        h.assertTrue(nullification.onBeingDamaged(ni,target,source,100),"Forgetting Aion disabled Thoth's classification trait");
        SkillAPI.getSkillsFrom(p).forgetSkill(thoth);target.invulnerableTime=0;
        before=target.getHealth();target.hurt(source,100);
        h.assertTrue(target.getHealth()==before,"Forgetting the final Godly skill left the bypass active");
        h.assertTrue(ni.isToggled() && ri.isToggled() && !nullification.onBeingDamaged(ni,target,source,100),"Godly damage permanently removed the target's defenses");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void proximitySlowIsStrongerNearbyAndExpires(GameTestHelper h) {
        var p=player(h);p.setPos(p.position().add(0,100,0));HunterData.learnReward(p,WildHunt.GODLY.get());
        var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();WildHunt.GODLY.get().onToggleOn(i,p);
        var near=tank(h,p.position().add(2,0,0));var far=tank(h,p.position().add(12,0,0));
        h.runAfterDelay(6,()->{
            // Fake players do not tick, so explicitly run the caster at a five-tick aura boundary.
            h.runAtTickTime(h.getTick()+(5-h.getLevel().getGameTime()%5),()->{
                WildHunt.GODLY.get().tickActive(i,p);
                var id=WildHunt.id("causal_dilation");var a=near.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(id);var b=far.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(id);
                h.assertTrue(a!=null && b!=null && a.amount()<b.amount(),"Proximity dilation did not strengthen toward the caster");
                WildHunt.GODLY.get().onToggleOff(i,p);
                h.runAfterDelay(11,()->{GodlySkill.tickSlow(near);GodlySkill.tickSlow(far);h.assertTrue(near.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(id)==null && far.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(id)==null,"Slow survived its short lease");h.succeed();});
            });
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void horizonPullsOutsideItsDamagingCore(GameTestHelper h) {
        var p=player(h);p.setPos(p.position().add(0,340,0));HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var center=p.position().add(0,1,0);var target=tank(h,center.add(18,0,0));
        ApexArts.point(i.getOrCreateTag(),"horizon",center);i.getOrCreateTag().putString("dimension",p.level().dimension().location().toString());i.getOrCreateTag().putLong("horizon_until",p.level().getGameTime()+200);
        var chunk=new net.minecraft.world.level.ChunkPos(target.blockPosition());h.getLevel().setChunkForced(chunk.x,chunk.z,true);
        h.runAfterDelay(5,()->{
        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);target.setHealth(1024);
        WildHunt.GODLY.get().tickActive(i,p);
        double expected=-.4*(1-Math.clamp(target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE),0,1));
        h.assertTrue(Math.abs(target.getDeltaMovement().x-expected)<.0001 && target.getHealth()==1024,"Outer Horizon pull="+target.getDeltaMovement().x+" expected="+expected+" health="+target.getHealth());h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void reversalTimeoutAutomaticallyReturnsOnceAndHonorsCurrentCaps(GameTestHelper h) {
        var p=tank(h,h.absolutePos(new net.minecraft.core.BlockPos(1,101,1)).getCenter());EnergyHelper.setBaseMaxEP(p,20_000_000);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1_000_000);energy.setMagicule(1_000_000);
        var start=p.position();WildHunt.GODLY.get().onPressed(i,p,0,2);p.setPos(start.add(3,0,0));energy.setAura(0);energy.setMagicule(0);
        EnergyHelper.setMaxAura(p,50_000);EnergyHelper.setMaxMagicule(p,40_000);
        i.getOrCreateTag().putLong("anchor_until",h.getLevel().getGameTime());WildHunt.GODLY.get().tickActive(i,p);
        h.assertTrue(p.position().distanceTo(start)<.1 && energy.getAura()==EnergyHelper.getMaxAura(p) && energy.getMagicule()==EnergyHelper.getMaxMagicule(p),"Automatic return: position="+p.position()+" expected="+start+" aura="+energy.getAura()+"/"+EnergyHelper.getMaxAura(p)+" MP="+energy.getMagicule()+"/"+EnergyHelper.getMaxMagicule(p));
        h.assertTrue(!i.getOrCreateTag().contains("anchor_until") && i.getCoolDown(2)==25,"Timeout failed to consume the checkpoint and start cooldown");
        energy.setMagicule(1);WildHunt.GODLY.get().tickActive(i,p);h.assertTrue(energy.getMagicule()==1,"Timed-out Reversal recast more than once");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void blockedTimeoutRestoresInPlaceAndLifecycleCannotRefund(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var energy=TensuraStorages.getExistenceFrom(p);energy.setMagicule(100_000);WildHunt.GODLY.get().onPressed(i,p,0,2);
        var anchor=p.blockPosition();p.setPos(p.position().add(3,0,0));var before=p.position();energy.setMagicule(0);
        h.getLevel().setBlockAndUpdate(anchor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());i.getOrCreateTag().putLong("anchor_until",h.getLevel().getGameTime());
        WildHunt.GODLY.get().tickActive(i,p);
        h.assertTrue(p.position().equals(before) && energy.getMagicule()==88_000 && !i.getOrCreateTag().contains("anchor_until"),"Blocked timeout stranded a checkpoint or teleported into a wall");
        i.setCoolDown(0,2);WildHunt.GODLY.get().onPressed(i,p,0,2);energy.setMagicule(0);GodlySkill.clearTransient(p);WildHunt.GODLY.get().tickActive(i,p);
        h.assertTrue(energy.getMagicule()==0 && !i.getOrCreateTag().contains("anchor_until"),"Lifecycle cleanup rewound resources across a dimension/relog boundary");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlySuppressionOnlyBenefitsItsOwnersDamage(GameTestHelper h) {
        var p=player(h);p.setPos(p.position().add(0,120,0));HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();
        var target=h.spawnWithNoFreeWill(EntityType.COW,2,1,2);target.setPos(p.position().add(2,0,0));target.setNoGravity(true);
        var poison=io.github.manasmods.tensura.registry.skill.ResistanceSkills.POISON_NULLIFICATION.get();var spatial=io.github.manasmods.tensura.registry.skill.ResistanceSkills.SPATIAL_ATTACK_NULLIFICATION.get();
        HunterData.learnReward(target,poison);HunterData.learnReward(target,spatial);var pi=SkillAPI.getSkillsFrom(target).getSkill(poison).orElseThrow();var si=SkillAPI.getSkillsFrom(target).getSkill(spatial).orElseThrow();pi.setToggled(true);si.setToggled(true);
        WildHunt.GODLY.get().onToggleOn(i,p);
        h.runAtTickTime(h.getTick()+(5-h.getLevel().getGameTime()%5),()->{
            WildHunt.GODLY.get().tickActive(i,p);
            h.assertTrue(!target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,200)),"Aion aura suppressed unrelated status immunity");
            var attacker=player(h);var source=WildHunt.GODLY.get().createSource(i,attacker,io.github.manasmods.tensura.damage.TensuraDamageTypes.SPACE_ELEMENTAL,0);
            h.assertTrue(!spatial.onBeingDamaged(si,target,source,10),"Nearby Godly owner lent suppression to another attacker");
            var ownSource=WildHunt.GODLY.get().createSource(i,p,io.github.manasmods.tensura.damage.TensuraDamageTypes.SPACE_ELEMENTAL,0);
            h.assertTrue(spatial.onBeingDamaged(si,target,ownSource,10),"Owner's own damage was not exempt");
            h.assertTrue(pi.isToggled() && si.isToggled() && !HunterData.of(target).contains("aion_degraded_until"),"Aura changed target defense state");
            WildHunt.GODLY.get().onToggleOff(i,p);h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlyDamageTraitsCoverMeleeProjectilesMagicAndNativeSkills(GameTestHelper h) {
        var p=player(h);var ordinary=player(h);var target=tank(h,p.position().add(0,180,0));
        HunterData.learnReward(p,WildHunt.MYTHIC.get(MythicKind.THOTH).get());
        var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),null);
        var sources=java.util.List.of(p.damageSources().playerAttack(p),p.damageSources().arrow(arrow,p),
            WildHunt.MYTHIC.get(MythicKind.THOTH).get().createSource(WildHunt.MYTHIC.get(MythicKind.THOTH).get().createDefaultInstance(),p,io.github.manasmods.tensura.damage.TensuraDamageTypes.MAGIC_GENERIC,1),
            WildHunt.UNIQUE.get(Apex.REY_DAU).get().createSource(WildHunt.UNIQUE.get(Apex.REY_DAU).get().createDefaultInstance(),p,io.github.manasmods.tensura.damage.TensuraDamageTypes.LIGHTNING_ELEMENTAL,0));
        var resist=io.github.manasmods.tensura.registry.skill.ResistanceSkills.PHYSICAL_ATTACK_NULLIFICATION.get();
        for(var source:sources) {
            h.assertTrue(resist.isResistanceBypass(source) && resist.isNullificationBypass(source),"User damage category did not bypass defenses: "+source);
            var amount=io.github.manasmods.manascore.network.api.util.Changeable.of(100f);GodlyPassives.modifyDamage(SkillAPI.getSkillsFrom(target),target,source,amount);
            h.assertTrue(Math.abs(amount.get()-135)<.001,"User damage category did not get its classification bonus");
        }
        h.assertTrue(!GodlyPassives.bypassesDefenses(p.damageSources().playerAttack(ordinary)) && !GodlyPassives.bypassesDefenses(p.damageSources().fall()),"Unrelated or environmental damage borrowed the bypass");
        var projectileOnly=new net.minecraft.world.damagesource.DamageSource(p.damageSources().arrow(arrow,p).typeHolder(),arrow);
        h.assertTrue(GodlyPassives.bypassesDefenses(projectileOnly),"Projectile shooter fallback was lost");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void godlyInterferenceImmunityUsesOriginSkillNotCastersTier(GameTestHelper h) {
        var target=player(h);var caster=player(h);var thoth=WildHunt.MYTHIC.get(MythicKind.THOTH).get();
        var interference=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.MAGIC_INTERFERENCE);
        var ordinary=WildHunt.UNIQUE.get(Apex.REY_DAU).get();
        h.assertTrue(io.github.manasmods.tensura.effect.template.TensuraMobEffect.addEffect(target,new net.minecraft.world.effect.MobEffectInstance(interference,200),caster,ordinary,0),"Non-Godly fixture unexpectedly immune");
        HunterData.learnReward(target,thoth);GodlyPassives.tick(target);
        h.assertTrue(!target.hasEffect(interference),"Existing ordinary interference survived Godly acquisition");
        HunterData.learnReward(caster,WildHunt.GODLY.get());
        h.assertTrue(!io.github.manasmods.tensura.effect.template.TensuraMobEffect.addEffect(target,new net.minecraft.world.effect.MobEffectInstance(interference,200),caster,ordinary,0),"Godly caster promoted an ordinary skill's interference");
        SkillAPI.getSkillsFrom(caster).forgetSkill(WildHunt.GODLY.get());
        h.assertTrue(io.github.manasmods.tensura.effect.template.TensuraMobEffect.addEffect(target,new net.minecraft.world.effect.MobEffectInstance(interference,200),caster,thoth,1),"Actual Godly-origin interference was rejected");
        GodlyPassives.tick(target);h.assertTrue(target.hasEffect(interference),"Cleanup removed Godly-origin interference");
        var effect=net.minecraft.world.effect.MobEffectInstance.load((CompoundTag)target.getEffect(interference).save());
        h.assertTrue(effect!=null && !GodlyPassives.blocksInterference(target,effect),"Saved effect lost Godly provenance");
        target.removeEffect(interference);
        h.assertTrue(!target.addEffect(new net.minecraft.world.effect.MobEffectInstance(interference,200)),"Unattributed interference gained Godly authority");
        var silence=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.SILENCE);
        h.assertTrue(target.addEffect(new net.minecraft.world.effect.MobEffectInstance(silence,200)),"Magic Interference immunity spread to unrelated Silence");
        SkillAPI.getSkillsFrom(target).forgetSkill(thoth);
        h.assertTrue(target.addEffect(new net.minecraft.world.effect.MobEffectInstance(interference,200)),"Forgetting last Godly skill retained interference immunity");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void projectileDilationScalesByProximityWithoutCompoundingOrSlowingOwnShots(GameTestHelper h) {
        var p=player(h);p.setPos(p.position().add(0,140,0));HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();WildHunt.GODLY.get().onToggleOn(i,p);
        var near=new net.minecraft.world.entity.projectile.Snowball(EntityType.SNOWBALL,h.getLevel());near.setPos(p.position().add(2,0,0));near.setDeltaMovement(1,0,0);h.getLevel().addFreshEntity(near);
        var far=new net.minecraft.world.entity.projectile.Snowball(EntityType.SNOWBALL,h.getLevel());far.setPos(p.position().add(12,0,0));far.setDeltaMovement(1,0,0);h.getLevel().addFreshEntity(far);
        var own=new net.minecraft.world.entity.projectile.Snowball(h.getLevel(),p);own.setPos(near.position());own.setDeltaMovement(1,0,0);h.getLevel().addFreshEntity(own);
        WildHunt.GODLY.get().tickActive(i,p);GodlySkill.slowProjectile(near);GodlySkill.slowProjectile(far);GodlySkill.slowProjectile(own);
        double speed=near.getDeltaMovement().x;
        h.assertTrue(speed<far.getDeltaMovement().x && far.getDeltaMovement().x<1 && own.getDeltaMovement().x==1,"Projectile slow does not respect proximity or ownership");
        GodlySkill.slowProjectile(near);h.assertTrue(near.getDeltaMovement().x==speed,"Repeated aura updates compound the projectile slow");
        near.getPersistentData().putLong("myths_dilation_until",0);GodlySkill.slowProjectile(near);h.assertTrue(Math.abs(near.getDeltaMovement().x-1)<.00001,"Projectile never recovered its velocity outside the aura");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void convergenceGateDependsOnNativeLineageAndCompletedGrowthNotCurrentEP(GameTestHelper h) {
        var p=player(h);DuoSystem.select(p,RaceFamily.SLIME);evolveSecondary(p,TensuraRaces.DEMON_SLIME.getId());evolveSecondary(p,TensuraRaces.GOD_SLIME.getId());
        double gate=DuoSystem.convergenceEP(p);h.assertTrue(gate==12_500_000,"Expected 125 percent of the fixed 10M last evolution gate, got "+gate);
        EnergyHelper.setBaseMaxEP(p,100_000_000);h.assertTrue(DuoSystem.convergenceEP(p)==gate,"Earning EP moved the awakening goalposts");
        DuoSystem.data(p).putDouble("secondary_ep_origin",100_000_000);DuoSystem.data(p).putDouble("secondary_last_evolution_ep",999_000_000);h.assertTrue(DuoSystem.convergenceEP(p)==gate,"Existing Duo saves lost their completed evolution gate");
        var config=io.github.manasmods.manascore.config.ConfigRegistry.getConfig(io.github.manasmods.tensura.config.race.SlimeConfig.class).GodSlime;
        double original=config.epRequirement;
        try {config.epRequirement=40_000_000;h.assertTrue(DuoSystem.convergenceEP(p)==50_000_000,"Higher native race evolution requirements did not raise awakening EP");}
        finally {config.epRequirement=original;}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void armedReversalSurvivesTemporarySuppressionAndReturnsWhileUnfunded(GameTestHelper h) {
        var p=tank(h,h.absolutePos(new net.minecraft.core.BlockPos(1,141,1)).getCenter());EnergyHelper.setBaseMaxEP(p,20_000_000);
        HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(100_000);energy.setMagicule(100_000);
        var start=p.position();WildHunt.GODLY.get().onPressed(i,p,0,2);p.setPos(start.add(3,0,0));energy.setMagicule(0);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tensura","sleep"),-1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        h.assertTrue(!i.getSkill().canInteractSkill(i,p),"Fixture did not suppress native skill interaction");
        WildHunt.GODLY.get().tickActive(i,p);h.assertTrue(i.getOrCreateTag().contains("anchor_until"),"Temporary suppression canceled a previously armed checkpoint");
        i.getOrCreateTag().putLong("anchor_until",h.getLevel().getGameTime());WildHunt.GODLY.get().tickActive(i,p);
        h.assertTrue(p.position().distanceTo(start)<.1 && energy.getMagicule()==88_000 && !i.getOrCreateTag().contains("anchor_until") && i.getCoolDown(2)==25,"Unfunded, suppressed checkpoint failed to return automatically");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void worldlineSteersSmoothlyAndIndependentlyOfFrameRate(GameTestHelper h) {
        var from=new net.minecraft.world.phys.Vec3(1,0,0);var target=new net.minecraft.world.phys.Vec3(0,0,1);
        var first=WorldlineAim.approach(from,target,1/60d);h.assertTrue(first.x>0 && first.z>0 && first.distanceTo(target)>.1,"Beam snapped immediately to a new aim");
        var fast=from;for(int n=0;n<6;n++)fast=WorldlineAim.approach(fast,target,1/60d);
        var slow=from;for(int n=0;n<2;n++)slow=WorldlineAim.approach(slow,target,1/20d);
        h.assertTrue(fast.distanceTo(slow)<.000001 && fast.distanceTo(target)<first.distanceTo(target),"Smoothing depends on frame rate or moves away from the aim");
        var reverse=WorldlineAim.approach(from,from.scale(-1),1/60d);
        h.assertTrue(Math.abs(reverse.length()-1)<.000001 && reverse.x>-.99,"Half-turn snapped or produced invalid beam geometry");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void worldlinePassesThroughWaterAndLavaButStopsAtSolidBlocks(GameTestHelper h) {
        var p=player(h);p.setPos(p.position().add(0,300,0));p.setYRot(-90);p.setXRot(0);
        var eye=net.minecraft.core.BlockPos.containing(p.getEyePosition());
        h.getLevel().setBlockAndUpdate(eye.offset(3,0,0),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
        h.getLevel().setBlockAndUpdate(eye.offset(5,0,0),net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
        h.getLevel().setBlockAndUpdate(eye.offset(10,0,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var wet=tank(h,p.position().add(8,0,0));var blocked=tank(h,p.position().add(13,0,0));
        HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();WildHunt.GODLY.get().onPressed(i,p,0,0);
        h.assertTrue(wet.getHealth()<1024 && blocked.getHealth()==1024,"Fluid blocked the damage ray or solid blocks failed to stop it");
        var end=WorldlineAim.end(p,p.getEyePosition(),p.getLookAngle());h.assertTrue(Math.abs(end.x-eye.getX()-10)<.001,"Beam did not terminate at the solid wall");
        WildHunt.GODLY.get().onRelease(i,p,1,0,0);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void reversalNeverLowersHealthSpiritualHealthOrEitherReserve(GameTestHelper h) {
        var p=player(h);HunterData.learnReward(p,WildHunt.GODLY.get());var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();var energy=TensuraStorages.getExistenceFrom(p);
        p.setHealth(100);energy.setSpiritualHealth(100);energy.setAura(100_000);energy.setMagicule(100_000);WildHunt.GODLY.get().onPressed(i,p,0,2);
        p.setHealth(200);energy.setSpiritualHealth(200);energy.setAura(200_000);energy.setMagicule(200_000);i.setCoolDown(0,2);WildHunt.GODLY.get().onPressed(i,p,0,2);
        h.assertTrue(p.getHealth()==200 && energy.getSpiritualHealth()==200 && energy.getAura()==200_000 && energy.getMagicule()==200_000,"Manual return reduced an improved stat");
        i.setCoolDown(0,2);WildHunt.GODLY.get().onPressed(i,p,0,2);energy.setAura(300_000);energy.setMagicule(300_000);i.getOrCreateTag().putLong("anchor_until",h.getLevel().getGameTime());WildHunt.GODLY.get().tickActive(i,p);
        h.assertTrue(energy.getAura()==300_000 && energy.getMagicule()==300_000,"Automatic return reduced an improved reserve");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void concordancePulseRestoresHalfMaximumHPAndSHPWithoutAFixedCap(GameTestHelper h) {
        var p=player(h);var race=FusionCatalog.RACES.get("human_slime").get();RaceAPI.getRaceFrom(p).setRace(race,false);var i=RaceAPI.getRaceFrom(p).getRace().orElseThrow();var energy=TensuraStorages.getExistenceFrom(p);
        float hp=p.getMaxHealth();double shp=p.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH);
        h.assertTrue(hp*.5>80 && shp>1000,"Fixture does not exercise a large uncapped heal");p.setHealth(hp*.25f);energy.setSpiritualHealth(shp*.25);energy.setAura(100_000);
        race.onActivateAbility(i,p);
        h.assertTrue(Math.abs(p.getHealth()-hp*.75)<.001 && Math.abs(energy.getSpiritualHealth()-shp*.75)<.001,"Concordance failed to restore half maximum HP and SHP");
        h.assertTrue(energy.getAura()==98_500 && i.getCooldown()==30,"Heal changed its aura cost or cooldown");race.onActivateAbility(i,p);
        h.assertTrue(energy.getAura()==98_500 && Math.abs(p.getHealth()-hp*.75)<.001,"Cooldown allowed an additional heal");
        i.setCooldown(0);p.setHealth(hp*.95f);energy.setSpiritualHealth(shp*.95);race.onActivateAbility(i,p);
        h.assertTrue(p.getHealth()==hp && energy.getSpiritualHealth()==shp,"Concordance overhealed beyond a pool maximum");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void daemonSecondaryUsesReceivingANameAndNativeWeightedAlternatives(GameTestHelper h) {
        var p=player(h);DuoSystem.select(p,RaceFamily.DAEMON);
        evolveSecondary(p,TensuraRaces.GREATER_DAEMON.getId());evolveSecondary(p,TensuraRaces.ARCH_DAEMON.getId());
        var energy=TensuraStorages.getExistenceFrom(p);energy.setName(null);energy.setSpiritualForm(true);
        EnergyHelper.setBaseMaxEP(p,DuoSystem.secondaryEvolutionEP(p));var secondary=DuoSystem.secondary(p).orElseThrow();
        h.assertTrue(DuoSystem.nativeProgress(p,secondary,TensuraRaces.DAEMON_LORD.get())==50,"Daemon Lord's weighted awakening contribution changed");
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for(int n=0;n<3;n++) {
            var mob=h.spawnWithNoFreeWill(EntityType.WOLF,3,1,2);
            io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket.name(mob,p,io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket.NamingType.LOW,"Named Test "+n);
            h.assertTrue(TensuraStorages.getExistenceFrom(mob).getName()!=null,"Native naming fixture failed");
        }
        h.assertTrue(energy.getName()==null && DuoSystem.nativeProgress(p,secondary,TensuraRaces.DAEMON_LORD.get())==50,"Naming other entities satisfied being named yourself");
        DuoSystem.evolve(p,TensuraRaces.DAEMON_LORD.getId());h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.ARCH_DAEMON.get(),"Missing native points were bypassed");
        io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket.name(p,null,io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket.NamingType.LOW,"Myths Named Hunter");
        h.assertTrue(energy.getName()!=null && DuoSystem.nativeProgress(p,secondary,TensuraRaces.DAEMON_LORD.get())==100,"Receiving a native name did not satisfy the secondary requirement");
        DuoSystem.evolve(p,TensuraRaces.DAEMON_LORD.getId());h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.DAEMON_LORD.get(),"Native two-of-three Daemon Lord alternatives were incorrectly made mandatory");
        EnergyHelper.setBaseMaxEP(p,DuoSystem.secondaryEvolutionEP(p));secondary=DuoSystem.secondary(p).orElseThrow();
        h.assertTrue(DuoSystem.nativeProgress(p,secondary,TensuraRaces.DEVIL_LORD.get())==75,"Devil Lord should still require a physical body");
        DuoSystem.evolve(p,TensuraRaces.DEVIL_LORD.getId());h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.DAEMON_LORD.get(),"Devil Lord physical-body requirement was bypassed");
        energy.setSpiritualForm(false);DuoSystem.evolve(p,TensuraRaces.DEVIL_LORD.getId());
        h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.DEVIL_LORD.get() && DuoSystem.primary(p).orElseThrow().getRace()==WildHunt.HUNTERS.get(2).get(),"Daemon secondary failed its final evolution or changed the primary race");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void everySecondaryBranchUsesTheSameNativeAndEPReadinessInItsMenu(GameTestHelper h) {
        var p=player(h);var d=DuoSystem.data(p);d.putDouble("secondary_ep_origin",20_000_000);d.putInt("secondary_stage",0);
        var seen=new java.util.HashSet<net.minecraft.resources.ResourceLocation>();var queue=new java.util.ArrayDeque<io.github.manasmods.manascore.race.api.ManasRace>();
        for(var family:RaceFamily.values())queue.add(family.baseRace());int edges=0;
        while(!queue.isEmpty()) {
            var race=queue.removeFirst();if(!seen.add(race.getRegistryName()))continue;
            var current=race.createDefaultInstance();DuoSystem.saveSecondary(p,current);EnergyHelper.setBaseMaxEP(p,7_500_000);
            var snapshot=MythsNetwork.snapshot(p,"");var rows=snapshot.getList("choices",net.minecraft.nbt.Tag.TAG_COMPOUND);
            var choices=DuoSystem.options(p,current);h.assertTrue(rows.size()==choices.size(),"Menu lost a native secondary option for "+race.getRegistryName());
            for(var next:choices) {
                edges++;queue.add(next);net.minecraft.nbt.CompoundTag row=null;
                for(var raw:rows){var candidate=(net.minecraft.nbt.CompoundTag)raw;if(candidate.getString("id").equals(next.getRegistryName().toString()))row=candidate;}
                h.assertTrue(row!=null && row.getBoolean("requirements_met")== (current.getEvolutionProgress(p,next)>=100),"Menu readiness differs from native weighted requirements: "+next.getRegistryName());
                h.assertTrue(row.getString("detail").contains("weighted points") && row.getString("detail").contains("7,500,000"),"Evolution checklist omitted native points or the added EP gate");
                EnergyHelper.setBaseMaxEP(p,7_499_999);h.assertTrue(!DuoSystem.secondaryRequirementsMet(p,current,next),"A secondary branch bypassed its added EP threshold");EnergyHelper.setBaseMaxEP(p,7_500_000);
            }
        }
        System.out.println("[Myths secondary audit] "+seen.size()+" native stages and "+edges+" evolution branches across all 15 families");
        h.assertTrue(seen.size()>=60 && edges>=50,"Too few native secondary stages were audited: "+seen.size()+" stages, "+edges+" branches");h.succeed();
    }
    private static String evolveSecondary(net.minecraft.server.level.ServerPlayer p,net.minecraft.resources.ResourceLocation id) {
        EnergyHelper.setBaseMaxEP(p,DuoSystem.secondaryEvolutionEP(p));return DuoSystem.evolve(p,id);
    }
    private static net.minecraft.world.entity.monster.Zombie tank(GameTestHelper h,net.minecraft.world.phys.Vec3 pos) {
        var chunk=net.minecraft.core.BlockPos.containing(pos);h.getLevel().getChunk(chunk);h.getLevel().setChunkForced(chunk.getX()>>4,chunk.getZ()>>4,true);var z=h.spawnWithNoFreeWill(EntityType.HUSK,2,1,2);z.setPos(pos);z.setNoGravity(true);
        z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1024);z.setHealth(1024);return z;
    }
}
