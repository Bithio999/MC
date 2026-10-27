package dev.wildhunt;

import dev.wildhunt.myths.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.gametest.*;
import static dev.wildhunt.MythicGameTests.player;
import static dev.wildhunt.ChainedGameTests.near;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class ProgressionEPGameTests {
    private static void bonus(ServerPlayer p,double amount){
        p.getAttribute(TensuraAttributes.MAX_AURA).addOrReplacePermanentModifier(new AttributeModifier(WildHunt.id("total_ep_test_capacity"),amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private static void total(ServerPlayer p,double target){
        bonus(p,0);bonus(p,target-EnergyHelper.getMaxEP(p));
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void hunterMenuAndCommandCountEffectiveEPAtTheSameFixedThreshold(GameTestHelper h) throws Exception {
        var p=player(h);RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(),false);EnergyHelper.setBaseMaxEP(p,1000);
        HunterData.recordBossKill(p,WildHunt.id("total_ep_fixture_boss"));
        var current=RaceAPI.getRaceFrom(p).getRace().orElseThrow();var target=WildHunt.HUNTERS.getFirst().get();
        h.assertTrue(current.getEvolutionProgress(p,target)<100,"Low total EP passed the native menu");
        h.assertTrue(p.getServer().getCommands().getDispatcher().execute("wildhunt evolve",p.createCommandSourceStack())==0,"Low total EP passed the command");
        bonus(p,99_000);near(h,EnergyHelper.getMaxEP(p),100_000,"Effective hunter threshold");
        near(h,current.getEvolutionProgress(p,target),100,"Menu ignored effective capacity");
        h.assertTrue(EnergyHelper.getBaseMaxEP(p)==1000,"Fixture raised raw EP");
        h.assertTrue(p.getServer().getCommands().getDispatcher().execute("wildhunt evolve",p.createCommandSourceStack())==1,"Command ignored effective capacity");
        h.assertTrue(RaceAPI.getRaceFrom(p).getRace().orElseThrow().getRace()==target,"Native hunter evolution did not happen");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void secondaryNativeBranchesAndAwakeningCountBonusesWithoutMovingTheirGates(GameTestHelper h){
        var p=player(h);EnergyHelper.setBaseMaxEP(p,1_000_000);bonus(p,4_000_000);DuoSystem.select(p,RaceFamily.SLIME);
        h.assertTrue(DuoSystem.secondary(p).isPresent(),"Selection ignored 5M total EP");
        bonus(p,6_500_000);var secondary=DuoSystem.secondary(p).orElseThrow();
        h.assertTrue(DuoSystem.nativeProgress(p,secondary,TensuraRaces.DEMON_SLIME.get())>=100,"Native secondary requirements still use raw EP");
        var menu=MythsNetwork.snapshot(p,"");near(h,menu.getDouble("ep"),7_500_000,"Menu shows raw EP");
        h.assertTrue(menu.getList("choices",10).getCompound(0).getBoolean("requirements_met"),"Menu rejected effective secondary EP");
        bonus(p,0);h.assertTrue(!DuoSystem.secondaryRequirementsMet(p,secondary,TensuraRaces.DEMON_SLIME.get()),"Removing capacity did not update readiness");
        bonus(p,6_500_000);DuoSystem.evolve(p,TensuraRaces.DEMON_SLIME.getId());
        h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.DEMON_SLIME.get(),"Effective EP did not permit the secondary evolution");
        total(p,10_000_000);String result=DuoSystem.evolve(p,TensuraRaces.GOD_SLIME.getId());
        h.assertTrue(DuoSystem.secondary(p).orElseThrow().getRace()==TensuraRaces.GOD_SLIME.get(),"Native final stage failed: "+result+" [base="+EnergyHelper.getBaseMaxEP(p)+", total="+EnergyHelper.getMaxEP(p)+", stage="+DuoSystem.data(p).getInt("secondary_stage")+"]");
        double fixed=DuoSystem.convergenceEP(p);near(h,fixed,12_500_000,"Awakening threshold changed");DuoSystem.convergence(p);
        h.assertTrue(!DuoSystem.converged(p),"Below-threshold total EP allowed awakening");
        total(p,12_500_000);near(h,DuoSystem.convergenceEP(p),fixed,"Capacity bonus moved the awakening threshold");DuoSystem.convergence(p);
        h.assertTrue(DuoSystem.converged(p),"12.5M effective EP did not permit awakening with low raw EP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void synthesisAndGodlyAscensionUseTotalCapacityEvenWithEmptyCurrentReserves(GameTestHelper h){
        var p=player(h);EnergyHelper.setBaseMaxEP(p,500_000);var storage=SkillAPI.getSkillsFrom(p);
        for(var source:MythicEvolution.ingredients(MythicKind.VRITRA)){HunterData.learnReward(p,source);var i=storage.getSkill(source).orElseThrow();i.setMastery(i.getMaxMastery());}
        bonus(p,1_499_000);h.assertTrue(!MythicEvolution.ready(p,MythicKind.VRITRA),"Synthesis ignored its fixed threshold");
        bonus(p,1_500_000);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(0);energy.setMagicule(0);
        h.assertTrue(MythicEvolution.ready(p,MythicKind.VRITRA),"Synthesis uses raw EP or current reserves");
        var snapshot=MythsNetwork.snapshot(p,"");near(h,snapshot.getDouble("ep"),2_000_000,"Synthesis menu disagrees with server");
        MythicEvolution.synthesize(p,"vritra");var ultimate=WildHunt.MYTHIC.get(MythicKind.VRITRA).get();var i=storage.getSkill(ultimate).orElseThrow();i.setMastery(i.getMaxMastery());
        bonus(p,11_499_000);MythsProgression.ascend(p,ultimate.getRegistryName().toString());h.assertTrue(storage.getSkill(WildHunt.GODLY.get()).isEmpty(),"Godly ascension ignored its fixed threshold");
        bonus(p,11_500_000);MythsProgression.ascend(p,ultimate.getRegistryName().toString());
        h.assertTrue(storage.getSkill(WildHunt.GODLY.get()).isPresent() && EnergyHelper.getBaseMaxEP(p)<12_000_000,"Godly ascension ignored effective EP");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void secondaryEPContextRestoresNativeQueriesAfterNestedAndFailedQueries(GameTestHelper h){
        var p=player(h);EnergyHelper.setBaseMaxEP(p,1_000_000);bonus(p,9_000_000);
        var instance=TensuraRaces.SLIME.get().createDefaultInstance();var gate=new EvolutionRequirement.EPRequirement(10_000_000);
        near(h,gate.getProgress(instance,p),.1,"Native query was globally changed");
        near(h,ProgressionEP.withTotalRequirements(p,()->gate.getProgress(instance,p)),1,"Secondary query ignored total EP");
        var other=player(h);EnergyHelper.setBaseMaxEP(other,1_000_000);bonus(other,9_000_000);
        near(h,ProgressionEP.withTotalRequirements(p,()->gate.getProgress(instance,other)),.1,"Secondary query leaked to another owner");
        try{ProgressionEP.withTotalRequirements(p,()->{throw new IllegalStateException("fixture");});}catch(IllegalStateException expected){}
        near(h,gate.getProgress(instance,p),.1,"Failed secondary query leaked its context");
        near(h,ProgressionEP.withTotalRequirements(p,()->{ProgressionEP.withTotalRequirements(other,()->gate.getProgress(instance,other));return gate.getProgress(instance,p);}),1,"Nested query lost the outer owner");
        near(h,gate.getProgress(instance,p),.1,"Nested query leaked its context");h.succeed();
    }
}
