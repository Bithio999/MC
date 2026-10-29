package dev.heavenlyrestriction;

import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.*;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import static dev.heavenlyrestriction.ChainedGameTests.near;

/** Opt-in integration checks against the exact pack; excluded from ordinary releases. */
@GameTestHolder("heavenlyrestriction_compat")
@PrefixGameTestTemplate(false)
public final class NeoOtherworldGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void auditAdditionalReincarnationSkills(GameTestHelper h) throws Exception {
        var p=TestFixtures.player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);var rows=new java.util.ArrayList<java.util.Map<String,Object>>();
        for(var skill:ReincarnationMenu.getSkillPool()){
            if(skill.getRegistryName().getNamespace().equals("tensura"))continue;
            var row=new java.util.LinkedHashMap<String,Object>();row.put("id",skill.getRegistryName().toString());row.put("class",skill.getClass().getName());row.put("magiculeCost",ChainedReincarnation.hasMagiculeCost(skill,p));row.put("targetSkillTaking",ChainedReincarnation.takesTargetSkills(skill));row.put("blocked",ChainedReincarnation.blocked(skill,p));rows.add(row);
        }
        var eligible=rows.stream().filter(row->!((Boolean)row.get("blocked"))).map(row->(String)row.get("id")).collect(java.util.stream.Collectors.toSet());
        h.assertTrue(rows.size()==39 && eligible.equals(java.util.Set.of("mysticism:cultivator","mysticism:malleable","mysticism:repeater","mysticism:scholar","tensuramoreskills:fungus_infection","tensuramoreskills:boogie_woogie","tensuramoreskills:adaptability","tno_tensura_extras:optimist","tno_tensura_extras:looter","tno_tensura_extras:time_traveler")),"The audited additional Fettered pool changed");
        var rolled=ReincarnationMenu.getReincarnationSkills(ReincarnationMenu.getSkillPool(),h.getLevel(),true,p).stream().filter(skill->!skill.getRegistryName().getNamespace().equals("tensura")).map(skill->skill.getRegistryName().toString()).collect(java.util.stream.Collectors.toSet());
        h.assertTrue(rolled.equals(eligible),"The native Fettered roll did not apply all audited exclusions");
        java.nio.file.Files.writeString(java.nio.file.Path.of("additional-skill-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void addonCopySkillsAreExcludedAndPrideRemainsAllowed(GameTestHelper h){
        var p=TestFixtures.player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        for(String id:java.util.List.of("stextras:imitator","tno_tensura_extras:all_rounder","traddon:total_reflection","mysticism:hidden_ruler","tensuramoreskills:satella")){
            var skill=io.github.manasmods.manascore.skill.api.SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id));
            h.assertTrue(skill!=null && ChainedReincarnation.takesTargetSkills(skill) && ChainedReincarnation.blocked(skill,p),"Addon target-skill copying escaped the Fettered blacklist: "+id);
        }
        h.assertTrue(!ChainedReincarnation.blocked(io.github.manasmods.tensura.registry.skill.UniqueSkills.PRIDE.get(),p),"Pack compatibility blacklisted Pride");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void inlineAddonMagiculeChargesAreExcluded(GameTestHelper h){
        var p=TestFixtures.player(h);
        for(String id:java.util.List.of("mysticism:subjugator","mysticism:gardener","tensuramoreskills:authority_of_greed")){
            var skill=SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id));
            h.assertTrue(skill!=null && ChainedReincarnation.hasMagiculeCost(skill,p) && ChainedReincarnation.blocked(skill,p),"Inline magicule charge escaped filtering: "+id);
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void epCommandAndFivefoldAura(GameTestHelper h){ChainedResourceGameTests.exactNativeEPAddCommandUsesNinetyTenAndFivefoldAuraAfterEPReset(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void airWalkLearning(GameTestHelper h){AirSovereigntyGameTests.chainedCanLearnTheEvolutionAndItsBattlewillMasteryBonusApplies(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void inheritedCreatorMenu(GameTestHelper h){
        var p=TestFixtures.player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        var skill=io.github.manasmods.tensura.registry.skill.UniqueSkills.CREATOR.get();
        h.assertTrue(ChainedGameTests.reincarnated(p,skill),"Native reincarnation Creator grant failed");
        var storage=SkillAPI.getSkillsFrom(p);var root=storage.getSkill(skill).orElseThrow();double mp=EnergyHelper.getMaxMagicule(p);
        var fighter=io.github.manasmods.tensura.registry.skill.UniqueSkills.FIGHTER.get();
        var observer=io.github.manasmods.tensura.registry.skill.UniqueSkills.OBSERVER.get();
        var forged=new io.github.manasmods.tensura.ability.TensuraSkillInstance(fighter);forged.getOrCreateTag().putString("Creator","tensura:creator");
        h.assertTrue(!io.github.manasmods.tensura.ability.SkillHelper.learnSkill(p,forged,1200),"Untrusted Creator metadata bypassed Chained");
        h.assertTrue(new io.github.manasmods.tensura.menu.SkillCreationMenu(0,skill,1,java.util.List.of(fighter.getRegistryName())).clickMenuButton(p,0),"Creation menu failed");
        var first=storage.getSkill(fighter).orElseThrow();h.assertTrue(first.isTemporarySkill() && first.getRemoveTime()==skill.getCreatedSkillTimer(root,p) && ChainedState.inherited(p,fighter),"Unmastered creation lost native duration or inheritance");
        first.setRemoveTime(60);root.setMastery(root.getMaxMastery());root.setCoolDown(0,1);
        h.assertTrue(new io.github.manasmods.tensura.menu.SkillCreationMenu(0,skill,1,java.util.List.of(fighter.getRegistryName())).clickMenuButton(p,0) && first.getRemoveTime()==1200,"Native temporary renewal failed");
        root.setMastery(root.getMaxMastery());root.setCoolDown(0,1);
        h.assertTrue(new io.github.manasmods.tensura.menu.SkillCreationMenu(0,skill,1,java.util.List.of(observer.getRegistryName())).clickMenuButton(p,0),"Mastered creation failed");
        var second=storage.getSkill(observer).orElseThrow();h.assertTrue(!second.isTemporarySkill() && second.getRemoveTime()==skill.getCreatedSkillTimer(root,p) && ChainedState.inherited(p,observer),"Tensura 2.0.1.4 permanent mastered creation lost inheritance");
        near(h,EnergyHelper.getMaxMagicule(p),mp,"Creation spent permanent magicules");h.assertTrue(!ChainedContext.inherited(p),"Creation leaked its learning scope");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void automaticUnboundEvolution(GameTestHelper h){UnboundGameTests.automaticEvolutionPreservesResourcesMasteryToggleCooldownAndAllPresets(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeSkillLearningGate(GameTestHelper h){UnboundGameTests.forbiddenSkillsRemainDiscoverableAndTrainableButCannotFinish(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void weaponAwareDash(GameTestHelper h){HeavenlyArsenalGameTests.actualDashAppliesWeaponDamageAndFireAspectToBothHits(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void weaponAwareCounter(GameTestHelper h){HeavenlyArsenalGameTests.actualCounterUsesReleaseWeaponAndHitsTheAdjacentTarget(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void resistanceCostsUseAura(GameTestHelper h){ChainedGameTests.ordinaryResistanceAndNullificationAcquisitionSpendAuraWithoutReducingMagiculeCapacity(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void fearSlownessBurdenImmunity(GameTestHelper h){HeavenlyArsenalGameTests.bothBodiesBlockOnlyTheRequestedEffectsWhileEquipped(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void spearAndHihiirokaneStats(GameTestHelper h){HeavenlyArsenalGameTests.weaponStatsMatchHihiirokaneAndBuiltinEngravingIsNative(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void ominousVaultLoot(GameTestHelper h){HeavenlyArsenalGameTests.actualOminousVaultLootRollsRareEngravedSpearsAndNormalVaultDoesNot(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void mysticismConstantEnergyStopsConvertedRegeneration(GameTestHelper h) throws Exception {
        var p=TestFixtures.player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);
        var energy=TensuraStorages.getExistenceFrom(p);var method=ExistenceStorage.class.getDeclaredMethod("handleMagiculeRegen",IExistence.class,net.minecraft.world.entity.LivingEntity.class,double.class,double.class);method.setAccessible(true);
        for(String id:java.util.List.of("constant_energy","constant_energy_debuff")){
            var effect=BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("mysticism",id)).orElseThrow();
            p.addEffect(new MobEffectInstance(effect,100));energy.setAura(0);energy.setMagicule(10);
            method.invoke(null,energy,p,200d,EnergyHelper.getMaxMagicule(p));
            near(h,energy.getAura(),0,"Mysticism energy lock allowed converted aura");near(h,energy.getMagicule(),10,"Mysticism energy lock allowed MP regeneration");p.removeEffect(effect);
        }
        method.invoke(null,energy,p,200d,EnergyHelper.getMaxMagicule(p));h.assertTrue(energy.getAura()>0 && energy.getMagicule()>10,"Regeneration did not resume after lock removal");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void birthPoolAndPackAddonsRemainPresent(GameTestHelper h){
        var p=TestFixtures.player(h);var menu=new ReincarnationMenu(0,p.getInventory(),p,false,0,0,0);
        h.assertTrue(menu.getRacePool().contains(HeavenlyRestriction.RESTRICTED.getFirst().get()),"Pack race pool hid Fettered Human");
        for(String id:java.util.List.of("tensura_tno","mysticism","stextras","tensuramoreskills"))h.assertTrue(net.neoforged.fml.ModList.get().isLoaded(id),"Missing exact-pack gameplay addon: "+id);
        ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow(()->new IllegalStateException("Fettered birth missing Chained; actual race="+io.github.manasmods.manascore.race.api.RaceAPI.getRaceFrom(p).getRace()+" skills="+SkillAPI.getSkillsFrom(p).getLearnedSkills()));
        h.assertTrue(i.getMastery()>=0 && EnergyHelper.getBaseMaxAura(p)>=1500 && EnergyHelper.getBaseMaxAura(p)<=2000 && EnergyHelper.getBaseMaxMagicule(p)>=50 && EnergyHelper.getBaseMaxMagicule(p)<=100,"Pack changed Chained or birth pools");h.succeed();
    }
}
