package dev.wildhunt;

import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.config.ability.skill.UniqueSkillConfig;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.world.TensuraGameRules;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;
import static dev.wildhunt.ChainedGameTests.near;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class ChainedReincarnationGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeBarrierAbsorptionCountsAsAMagiculeCostForMythsUniques(GameTestHelper h){
        var p=fettered(h);double price=io.github.manasmods.tensura.ability.skill.extra.MultilayerBarrierSkill.CONFIG.magiculeCost;
        try{
            io.github.manasmods.tensura.ability.skill.extra.MultilayerBarrierSkill.CONFIG.magiculeCost=1;
            var pool=ReincarnationMenu.getReincarnationSkills(ReincarnationMenu.getSkillPool(),h.getLevel(),true,p);
            for(var apex:List.of(Apex.UTH_DUNA,Apex.JIN_DAHAAD))h.assertTrue(!pool.contains(WildHunt.UNIQUE.get(apex).get()),"Magicule-paid absorption survived filtering: "+apex);
            h.assertTrue(pool.contains(WildHunt.UNIQUE.get(Apex.REY_DAU).get()) && pool.contains(WildHunt.UNIQUE.get(Apex.NU_UDRA).get()),"Aura-only arts were excluded");
            io.github.manasmods.tensura.ability.skill.extra.MultilayerBarrierSkill.CONFIG.magiculeCost=0;
            for(var apex:List.of(Apex.UTH_DUNA,Apex.JIN_DAHAAD))h.assertTrue(!ChainedReincarnation.hasMagiculeCost(WildHunt.UNIQUE.get(apex).get(),p),"Free absorption retained a cached MP cost");
        }finally{io.github.manasmods.tensura.ability.skill.extra.MultilayerBarrierSkill.CONFIG.magiculeCost=price;}
        h.succeed();
    }
    private static ServerPlayer fettered(GameTestHelper h){var p=player(h);ReincarnationMenu.setRace(p,WildHunt.RESTRICTED.getFirst().get(),true,false);return p;}
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativePoolsFilterEveryMagiculeModeWithoutChangingGlobalPoolsOrResources(GameTestHelper h){
        var p=fettered(h);var primary=ReincarnationMenu.getSkillPool();var secondary=ReincarnationMenu.getSecondSkillPool();var pool=ReincarnationMenu.getReincarnationSkills(primary,h.getLevel(),true,p);
        var bonus=ReincarnationMenu.getReincarnationSkills(secondary,h.getLevel(),true,p);double mp=EnergyHelper.getMaxMagicule(p),ap=TensuraStorages.getExistenceFrom(p).getAura();int owned=SkillAPI.getSkillsFrom(p).getLearnedSkills().size();
        for(var skill:List.of(UniqueSkills.MARTIAL_MASTER.get(),UniqueSkills.ANALYST.get(),UniqueSkills.OBSERVER.get()))h.assertTrue(pool.contains(skill),"Aura/free skill incorrectly excluded: "+skill.getRegistryName());
        for(var skill:List.of(UniqueSkills.HEALER.get(),UniqueSkills.MUSICIAN.get(),UniqueSkills.REVERSER.get(),UniqueSkills.UNYIELDING.get(),UniqueSkills.REAPER.get(),UniqueSkills.SNIPER.get()))h.assertTrue(!pool.contains(skill),"Magicule skill survived filtering: "+skill.getRegistryName());
        h.assertTrue(bonus.stream().noneMatch(s->ChainedReincarnation.hasMagiculeCost(s,p)),"Second pool bypassed filter");
        for(int n=0;n<3;n++)ChainedReincarnation.filter(primary,p);
        h.assertTrue(primary.equals(ReincarnationMenu.getSkillPool()) && secondary.equals(ReincarnationMenu.getSecondSkillPool()),"Global pools mutated");
        near(h,EnergyHelper.getMaxMagicule(p),mp,"Inspection changed max MP");near(h,TensuraStorages.getExistenceFrom(p).getAura(),ap,"Inspection spent aura");h.assertTrue(SkillAPI.getSkillsFrom(p).getLearnedSkills().size()==owned,"Inspection learned a probe");
        System.out.println("[Fettered reincarnation] eligible primary="+pool.stream().map(s->s.getRegistryName().toString()).sorted().toList());h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void otherRacesAndExistingInheritedSkillsAreUnaffected(GameTestHelper h){
        var p=player(h);var pool=List.<ManasSkill>of(UniqueSkills.HEALER.get(),UniqueSkills.MARTIAL_MASTER.get());h.assertTrue(ChainedReincarnation.filter(pool,p)==pool,"Human received Fettered filter");
        ReincarnationMenu.setRace(p,WildHunt.RESTRICTED.getFirst().get(),true,false);
        // An already recorded root is retained; filtering never removes or rewrites inheritance.
        try(var scope=ChainedContext.reincarnation(p,UniqueSkills.HEALER.get())){HunterData.learnReward(p,UniqueSkills.HEALER.get());ChainedState.remember(p,UniqueSkills.HEALER.get());}
        ChainedReincarnation.filter(pool,p);h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.HEALER.get()).isPresent() && ChainedState.inherited(p,UniqueSkills.HEALER.get()),"Existing root was removed");
        RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(),false);h.assertTrue(ChainedReincarnation.filter(pool,p)==pool,"Owning Chained filtered another race");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void configuredInlineCostsAndApiCostsStayLive(GameTestHelper h){
        var p=fettered(h);var c=(UniqueSkillConfig)ConfigRegistry.getConfig(UniqueSkillConfig.class);double curse=c.Reverser.magiculeCostCurse;double blast=c.Musician.magiculeCostBlast,wave=c.Musician.magiculeCostWave,requiem=c.Musician.magiculeCostRequiem;
        try{
            c.Reverser.magiculeCostCurse=1;h.assertTrue(ChainedReincarnation.hasMagiculeCost(UniqueSkills.REVERSER.get(),p),"Inline curse cost missed");
            c.Reverser.magiculeCostCurse=0;h.assertTrue(!ChainedReincarnation.hasMagiculeCost(UniqueSkills.REVERSER.get(),p),"Zero inline cost remained cached");
            c.Musician.magiculeCostBlast=c.Musician.magiculeCostWave=c.Musician.magiculeCostRequiem=0;h.assertTrue(!ChainedReincarnation.hasMagiculeCost(UniqueSkills.MUSICIAN.get(),p),"Zero ability costs blocked due to acquisition price");
            c.Musician.magiculeCostRequiem=1;h.assertTrue(ChainedReincarnation.hasMagiculeCost(UniqueSkills.MUSICIAN.get(),p),"A later mode's cost was missed");
        }finally{c.Reverser.magiculeCostCurse=curse;c.Musician.magiculeCostBlast=blast;c.Musician.magiculeCostWave=wave;c.Musician.magiculeCostRequiem=requiem;}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void masteryAndToggleOnlyCostsAreInspectedWithoutExecutingTheSkill(GameTestHelper h){
        var p=fettered(h);
        for(var probe:List.of(new Probe(true,false),new Probe(false,true),new Probe(false,false)))h.assertTrue(ChainedReincarnation.hasMagiculeCost(probe,p)==(probe.mastered || probe.toggled),"Mastery/toggle-only cost detection failed");
        h.succeed();
    }
    private static final class Probe extends Skill {
        final boolean mastered,toggled;Probe(boolean mastered,boolean toggled){super(SkillType.UNIQUE);this.mastered=mastered;this.toggled=toggled;}
        // ManasCore instances require a registered skill ID, even for a detached probe.
        @Override public io.github.manasmods.tensura.ability.TensuraSkillInstance createDefaultInstance(){return UniqueSkills.ANALYST.get().createDefaultInstance();}
        @Override public int getModes(ManasSkillInstance i){return 3;}
        @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return mode==2 && (mastered && i.getMastery()==i.getMaxMastery() || toggled && i.isToggled())?1:0;}
        @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){throw new IllegalStateException("Cost inspection cast a skill");}
        @Override public void onToggleOn(ManasSkillInstance i,LivingEntity e){throw new IllegalStateException("Cost inspection applied a toggle");}
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void manualBlacklistWinsAndAllowlistDoesNotAddAbsentCandidates(GameTestHelper h){
        var p=fettered(h);var oldBlack=ChainedReincarnationConfig.BLACKLIST.get();var oldAllow=ChainedReincarnationConfig.ALLOWLIST.get();boolean oldAuto=ChainedReincarnationConfig.BLOCK_MAGICULE_COSTS.get();
        try{
            String id=UniqueSkills.HEALER.get().getRegistryName().toString();ChainedReincarnationConfig.ALLOWLIST.set(List.of(id));h.assertTrue(!ChainedReincarnation.blocked(UniqueSkills.HEALER.get(),p),"Explicit exception ignored");
            ChainedReincarnationConfig.BLACKLIST.set(List.of(id));h.assertTrue(ChainedReincarnation.blocked(UniqueSkills.HEALER.get(),p),"Blacklist did not take priority");
            h.assertTrue(ChainedReincarnation.filter(List.of(UniqueSkills.MARTIAL_MASTER.get()),p).equals(List.of(UniqueSkills.MARTIAL_MASTER.get())),"Allowlist injected a missing skill");
            ChainedReincarnationConfig.BLOCK_MAGICULE_COSTS.set(false);h.assertTrue(!ChainedReincarnation.blocked(UniqueSkills.MUSICIAN.get(),p) && ChainedReincarnation.blocked(UniqueSkills.HEALER.get(),p),"Disabling automatic filter changed explicit blacklist");
        }finally{ChainedReincarnationConfig.BLACKLIST.set(oldBlack);ChainedReincarnationConfig.ALLOWLIST.set(oldAllow);ChainedReincarnationConfig.BLOCK_MAGICULE_COSTS.set(oldAuto);}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void actualNativeBirthAndBonusRollCannotGrantBlacklistedSkills(GameTestHelper h){
        var p=player(h);var c=ReincarnationMenu.CONFIG.Skills;var primary=c.startingSkills;var secondary=c.secondSkills;int number=c.skillNumber;double chance=c.additionalUniqueChance;var oldBlack=ChainedReincarnationConfig.BLACKLIST.get();
        try{
            c.startingSkills=List.of("tensura:healer","tensura:martial_master","tensura:analyst");c.secondSkills=List.of("tensura:healer","tensura:observer");c.skillNumber=2;c.additionalUniqueChance=100;
            ChainedReincarnationConfig.BLACKLIST.set(ReincarnationMenu.getSkillPool().stream().map(v->v.getRegistryName().toString()).filter(id->!List.of("tensura:healer","tensura:martial_master","tensura:analyst").contains(id)).toList());
            ReincarnationMenu.setRace(p,WildHunt.RESTRICTED.getFirst().get(),true,true);
            var skills=SkillAPI.getSkillsFrom(p);h.assertTrue(skills.getSkill(UniqueSkills.HEALER.get()).isEmpty(),"Native birth rolled a blacklisted skill");h.assertTrue(skills.getSkill(UniqueSkills.OBSERVER.get()).isPresent(),"Second pool did not use its allowed candidate");
            h.assertTrue(skills.getSkill(UniqueSkills.MARTIAL_MASTER.get()).isPresent() || skills.getSkill(UniqueSkills.ANALYST.get()).isPresent(),"Primary pool lost its valid roll");h.assertTrue(skills.getSkill(WildHunt.CHAINED.get()).isPresent(),"Guaranteed racial Chained was filtered");
        }finally{c.startingSkills=primary;c.secondSkills=secondary;c.skillNumber=number;c.additionalUniqueChance=chance;ChainedReincarnationConfig.BLACKLIST.set(oldBlack);}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void emptyFilteredPoolReturnsWithoutGrantingOrReplacingSkills(GameTestHelper h){
        var p=fettered(h);var c=ReincarnationMenu.CONFIG.Skills;var old=c.startingSkills;var oldBlack=ChainedReincarnationConfig.BLACKLIST.get();var before=List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills());
        try{c.startingSkills=List.of("tensura:healer","tensura:reaper");ChainedReincarnationConfig.BLACKLIST.set(ReincarnationMenu.getSkillPool().stream().map(v->v.getRegistryName().toString()).toList());ReincarnationMenu.randomUniqueSkill(p,true);h.assertTrue(SkillAPI.getSkillsFrom(p).getLearnedSkills().containsAll(before) && SkillAPI.getSkillsFrom(p).getLearnedSkills().size()==before.size() && ChainedState.owned(p),"Empty pool granted a fallback or removed Chained");}
        finally{c.startingSkills=old;ChainedReincarnationConfig.BLACKLIST.set(oldBlack);}h.succeed();
    }
}
