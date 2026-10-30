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
public static void dedicatedServersCannotRollLearnCopyOrActivateWorldJumper(GameTestHelper h){WorldHopGameTests.dedicatedServersCannotRollLearnCopyOrActivateWorldJumper(h);}
@GameTest(template="empty",timeoutTicks=100)
public static void metadataKeepsCharacterAndDimensionGeneratorsButChangesSeed(GameTestHelper h){WorldHopGameTests.metadataKeepsCharacterAndDimensionGeneratorsButChangesSeed(h);}
@GameTest(template="empty",timeoutTicks=100)
public static void landingRejectsWaterHazardsAndBlockedHeadroom(GameTestHelper h){WorldHopGameTests.landingRejectsWaterHazardsAndBlockedHeadroom(h);}
@GameTest(template="empty",timeoutTicks=100)
public static void nativeActivationLearnsAuraSlashBeforeSchema(GameTestHelper h){SchemaGameTests.nativeActivationLearnsAuraSlashBeforeSchema(h);}
@GameTest(template="empty",timeoutTicks=100)
public static void nativeFailedLearningCannotSkipSchemasThousandPoints(GameTestHelper h){SchemaGameTests.nativeFailedLearningCannotSkipSchemasThousandPoints(h);}
@GameTest(template="empty",timeoutTicks=100)
public static void masteryCompletesAtTwoThousandAndNeverEarlier(GameTestHelper h){SchemaGameTests.masteryCompletesAtTwoThousandAndNeverEarlier(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void masterySharesOneThirtySecondIntervalAcrossEveryBuild(GameTestHelper h){SchemaGameTests.masterySharesOneThirtySecondIntervalAcrossEveryBuild(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativePartialBillsPreserveTheOriginalRecoveryUnits(GameTestHelper h){SchemaGameTests.regenerativePartialBillsPreserveTheOriginalRecoveryUnits(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeWeightsShareAllSlotsAndPersistTheNewEffect(GameTestHelper h){SchemaGameTests.regenerativeWeightsShareAllSlotsAndPersistTheNewEffect(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeBoostsHealAndDirectHpRestoresOnceButHonorsCancellation(GameTestHelper h){SchemaGameTests.regenerativeBoostsHealAndDirectHpRestoresOnceButHonorsCancellation(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeBoostsNativeSpiritualRegenAndDirectRestoresWithACap(GameTestHelper h){SchemaGameTests.regenerativeBoostsNativeSpiritualRegenAndDirectRestoresWithACap(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeCostsAreAtomicAndTheConsumingOverloadsChargeOnce(GameTestHelper h){SchemaGameTests.regenerativeCostsAreAtomicAndTheConsumingOverloadsChargeOnce(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeDoesNotTaxFreeActionsHostileDrainsOrCreativeCosts(GameTestHelper h){SchemaGameTests.regenerativeDoesNotTaxFreeActionsHostileDrainsOrCreativeCosts(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedBoostsRegenerativeHealingButNeverItsResourcePenalty(GameTestHelper h){SchemaGameTests.chainedBoostsRegenerativeHealingButNeverItsResourcePenalty(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void regenerativeSurchargeAlsoAppliesToManualDashAndCounterPayments(GameTestHelper h){SchemaGameTests.regenerativeSurchargeAlsoAppliesToManualDashAndCounterPayments(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void actualHealingTrainsRegenerativeAndToggleForgetRemoveItsEffects(GameTestHelper h){SchemaGameTests.actualHealingTrainsRegenerativeAndToggleForgetRemoveItsEffects(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void mixedEffectsShareTheirOrderedWeightsAndPersistBothIds(GameTestHelper h){SchemaGameTests.mixedEffectsShareTheirOrderedWeightsAndPersistBothIds(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void strengthAddsToWeaponBaseAndCleansOnlyItsOwnModifier(GameTestHelper h){SchemaGameTests.strengthAddsToWeaponBaseAndCleansOnlyItsOwnModifier(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedStrengthBoostsOnlyPositiveDamageOnce(GameTestHelper h){SchemaGameTests.chainedStrengthBoostsOnlyPositiveDamageOnce(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void strengthVulnerabilityAppliesToMeleeMagicProjectilesEnvironmentAndSoul(GameTestHelper h){SchemaGameTests.strengthVulnerabilityAppliesToMeleeMagicProjectilesEnvironmentAndSoul(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void strengthOnlyBuildTrainsOnLandedHitsWithFullAura(GameTestHelper h){SchemaGameTests.strengthOnlyBuildTrainsOnLandedHitsWithFullAura(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void outgoingPenaltyAlsoScalesFlatBonusesAddedByNativeEffects(GameTestHelper h){SchemaGameTests.outgoingPenaltyAlsoScalesFlatBonusesAddedByNativeEffects(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void learningRequiresOneThousandAndMasteryTwoThousand(GameTestHelper h){SchemaGameTests.learningRequiresOneThousandAndMasteryTwoThousand(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void allMasteryMilestonesRetainFullDrawbacks(GameTestHelper h){SchemaGameTests.allMasteryMilestonesRetainFullDrawbacks(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void serverValidatesBuildsAndPersistsThemThroughSkillSerialization(GameTestHelper h){SchemaGameTests.serverValidatesBuildsAndPersistsThemThroughSkillSerialization(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void toggleAndMasteryChangeActualRateWithoutStackingRefreshes(GameTestHelper h){SchemaGameTests.toggleAndMasteryChangeActualRateWithoutStackingRefreshes(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeAuraAndSleepRecoveryAreBothBoostedAndCapped(GameTestHelper h){SchemaGameTests.nativeAuraAndSleepRecoveryAreBothBoostedAndCapped(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedBuffAppliesOnceAndNeverMagnifiesThePenalty(GameTestHelper h){SchemaGameTests.chainedBuffAppliesOnceAndNeverMagnifiesThePenalty(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void convertedMagiculeRegenIsBoostedButRetainedMagiculesRemainFlat(GameTestHelper h){SchemaGameTests.convertedMagiculeRegenIsBoostedButRetainedMagiculesRemainFlat(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void meleeProjectilesAndDirectSoulDamagePayThePenaltyExactlyOnce(GameTestHelper h){SchemaGameTests.meleeProjectilesAndDirectSoulDamagePayThePenaltyExactlyOnce(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void masteryRequiresActualRecoveryAndCannotBeFarmedBySavingOrDuplicatePulses(GameTestHelper h){SchemaGameTests.masteryRequiresActualRecoveryAndCannotBeFarmedBySavingOrDuplicatePulses(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void battlewillCategoryAndSelfOnlyCapabilitiesAreNative(GameTestHelper h){InstantTransmissionGameTests.battlewillCategoryAndSelfOnlyCapabilitiesAreNative(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void nativeInstantMoveMasteryUnlocksOneTrainableEvolution(GameTestHelper h){InstantTransmissionGameTests.nativeInstantMoveMasteryUnlocksOneTrainableEvolution(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void existingMasteredParentsBackfillButBorrowedCopiesDoNot(GameTestHelper h){InstantTransmissionGameTests.existingMasteredParentsBackfillButBorrowedCopiesDoNot(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void nativeCoordinateRequestBecomesInstantAtMasteryAndPaysOnlyAura(GameTestHelper h){InstantTransmissionGameTests.nativeCoordinateRequestBecomesInstantAtMasteryAndPaysOnlyAura(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void unmasteredRequestWaitsExactlyOneHundredTicksAndChargesOnce(GameTestHelper h){InstantTransmissionGameTests.unmasteredRequestWaitsExactlyOneHundredTicksAndChargesOnce(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void malformedUnlearnedUnfundedAndCrossDimensionRequestsDoNotTeleport(GameTestHelper h){InstantTransmissionGameTests.malformedUnlearnedUnfundedAndCrossDimensionRequestsDoNotTeleport(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void nativePortalRequestCannotCreateAPortalOrChargeResources(GameTestHelper h){InstantTransmissionGameTests.nativePortalRequestCannotCreateAPortalOrChargeResources(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void spatialBlockadeAndLosingTheSkillCancelPendingTransmission(GameTestHelper h){InstantTransmissionGameTests.spatialBlockadeAndLosingTheSkillCancelPendingTransmission(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void nativeTeleportEventCanVetoAnInstantTransmission(GameTestHelper h){InstantTransmissionGameTests.nativeTeleportEventCanVetoAnInstantTransmission(h);}
    @GameTest(template="empty",timeoutTicks=160)
    public static void chainedCanLearnTheEvolutionAndReceivesBattlewillMasteryBonus(GameTestHelper h){InstantTransmissionGameTests.chainedCanLearnTheEvolutionAndReceivesBattlewillMasteryBonus(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void spiritualContactBelongsOnlyToTheAttackingMainHandWeapon(GameTestHelper h){NullificationSpiritualGameTests.spiritualContactBelongsOnlyToTheAttackingMainHandWeapon(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void warGnomeReceivesFullPhysicalWeaponDamageWhileKeepingItsArmor(GameTestHelper h){NullificationSpiritualGameTests.warGnomeReceivesFullPhysicalWeaponDamageWhileKeepingItsArmor(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void spearCanHitSpiritualFormWithoutChangingTheTargetsForm(GameTestHelper h){NullificationSpiritualGameTests.spearCanHitSpiritualFormWithoutChangingTheTargetsForm(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void chainedAndUnboundWeaponArtsCarrySpiritualContact(GameTestHelper h){NullificationSpiritualGameTests.chainedAndUnboundWeaponArtsCarrySpiritualContact(h);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void auditAdditionalReincarnationSkills(GameTestHelper h) throws Exception {
        var p=TestFixtures.player(h);ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);var rows=new java.util.ArrayList<java.util.Map<String,Object>>();
        for(var skill:ReincarnationMenu.getSkillPool()){
            if(java.util.Set.of("tensura","heavenlyrestriction").contains(skill.getRegistryName().getNamespace()))continue;
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
