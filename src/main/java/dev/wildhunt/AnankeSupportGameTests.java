package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;
import static dev.wildhunt.MythBossGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class AnankeSupportGameTests {
    static ManasSkillInstance support(AnankeEntity b,String id){return SkillAPI.getSkillsFrom(b).getSkill(ResourceLocation.parse("tensura:"+id)).orElseThrow();}
    static void enabled(GameTestHelper h,AnankeEntity b){for(var id:MythBosses.NATIVE_SKILLS){var found=SkillAPI.getSkillsFrom(b).getSkill(ResourceLocation.parse(id));h.assertTrue(found.isPresent(),"Support not initialized yet: "+id+"; ticks="+b.tickCount+"; removed="+b.isRemoved());var i=found.orElseThrow();h.assertTrue(i.isMastered(b) && (!i.canBeToggled(b) || i.isToggled()),"Inactive stored support: "+id);}}
    static AnankeEntity reload(GameTestHelper h,AnankeEntity b,boolean legacy){var tag=new CompoundTag();b.save(tag);if(legacy)tag.remove("myth_support_version");b.discard();var copy=(AnankeEntity)EntityType.loadEntityRecursive(tag,h.getLevel(),e->e);h.getLevel().addFreshEntity(copy);return copy;}
    @GameTest(template="empty",timeoutTicks=100)
    public static void fullNativeDefenseRegistryIsGrantedMasteredAndEnabled(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var ids=new HashSet<String>();
        try{for(var field:io.github.manasmods.tensura.registry.skill.ResistanceSkills.class.getFields()){
            if(!dev.architectury.registry.registries.RegistrySupplier.class.isAssignableFrom(field.getType()))continue;
            var skill=(ManasSkill)((dev.architectury.registry.registries.RegistrySupplier<?>)field.get(null)).get();ids.add(skill.getRegistryName().toString());
            var i=SkillAPI.getSkillsFrom(b).getSkill(skill).orElseThrow();h.assertTrue(i.isMastered(b) && i.isToggled(),"Missing/inactive native defense: "+skill.getRegistryName());
        }}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        h.assertTrue(ids.size()==44 && ids.equals(new HashSet<>(MythBosses.NATIVE_DEFENSES)),"Defense package does not match native registry");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeInfiniteRegenerationRestoresBothPoolsAndSpendsMagicules(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var energy=TensuraStorages.getExistenceFrom(b);var i=support(b,"infinite_regeneration");
        h.assertTrue(i.isToggled() && i.isMastered(b),"Native Infinite Regeneration not enabled/mastered");
        b.setHealth(47000);energy.setSpiritualHealth(119000);energy.setMagicule(15000000);
        h.runAfterDelay(15,()->{h.assertTrue(b.getHealth()==48000 && energy.getSpiritualHealth()==120000,"Native regeneration failed to restore HP/SHP");
            h.assertTrue(energy.getMagicule()<15000000,"Native regeneration restored resources for free");
            h.assertTrue(!b.isCasting() && b.riteMode()==0,"Removed recovery channel returned");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void priorPackageMigratesToInfiniteRegenerationWithoutRestoringTheRemovedChannel(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));
        var old=SkillAPI.getSkillRegistry().get(ResourceLocation.parse("tensura:self_regeneration"));HunterData.learnReward(b,old);
        var tag=new CompoundTag();b.save(tag);tag.putInt("myth_support_version",1);tag.putLong("myth_renewal_remaining",500);b.discard();
        var copy=(AnankeEntity)EntityType.loadEntityRecursive(tag,h.getLevel(),e->e);h.getLevel().addFreshEntity(copy);
        h.startSequence().thenWaitUntil(()->{enabled(h,copy);h.assertTrue(SkillAPI.getSkillsFrom(copy).getSkill(old).isEmpty(),"Old gradual regeneration survived migration");}).thenExecute(()->{
            var saved=new CompoundTag();copy.save(saved);h.assertTrue(!saved.contains("myth_renewal_remaining") && !copy.isCasting() && saved.getInt("myth_support_version")==2,"Removed recovery state survived migration");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void everyStoredSupportIsEnabledWithoutDuplicateModifiers(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));enabled(h,b);double space=b.getAttributeValue(TensuraAttributes.SPACE_BOOST),sense=b.getAttributeValue(TensuraAttributes.PRESENCE_SENSE);
        for(int n=0;n<20;n++)b.initializeMyth();
        h.runAfterDelay(25,()->{enabled(h,b);h.assertTrue(b.getAttributeValue(TensuraAttributes.SPACE_BOOST)==space && b.getAttributeValue(TensuraAttributes.PRESENCE_SENSE)==sense,"Repeated initialization multiplied support");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void commandStyleSpawnWithoutFinalizeAlsoActivatesStoredSkills(GameTestHelper h){
        var p=player(h);var b=MythBosses.ANANKE.get().create(h.getLevel());b.setPos(p.position().add(10,0,0));b.setNoAi(true);h.getLevel().addFreshEntity(b);
        h.startSequence().thenWaitUntil(()->enabled(h,b)).thenExecute(()->{h.assertTrue(b.getMaxHealth()==48000,"Command-style spawn lost HP");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void legacyGhostBonusesMigrateOnceAndReloadKeepsWounds(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));b.setHealth(10000);var e=TensuraStorages.getExistenceFrom(b);e.setMagicule(15000000);e.setSpiritualHealth(60000);
        double space=b.getAttributeValue(TensuraAttributes.SPACE_BOOST),melee=b.getAttributeValue(TensuraAttributes.AUTO_MELEE_DODGE_CHANCE),projectile=b.getAttributeValue(TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE);
        for(var id:List.of("mathematician","observer","chant_annulment","spatial_domination","physical_attack_resistance","spiritual_attack_resistance","magic_resistance","holy_attack_resistance","abnormal_condition_nullification","pain_nullification"))support(b,id).setToggled(false);
        var copy=reload(h,b,true);h.assertTrue(copy.getHealth()==10000 && TensuraStorages.getExistenceFrom(copy).getMagicule()==15000000,"Loading refilled native resources");h.runAfterDelay(25,()->{enabled(h,copy);h.assertTrue(TensuraStorages.getExistenceFrom(copy).getMagicule()<15000000,"Migration did not pay for native wound recovery");
            h.assertTrue(copy.getAttributeValue(TensuraAttributes.SPACE_BOOST)==space && copy.getAttributeValue(TensuraAttributes.AUTO_MELEE_DODGE_CHANCE)==melee && copy.getAttributeValue(TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE)==projectile,"Migration duplicated ghost modifiers");
            var again=reload(h,copy,false);h.runAfterDelay(25,()->{enabled(h,again);h.assertTrue(again.getAttributeValue(TensuraAttributes.SPACE_BOOST)==space && again.getAttributeValue(TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE)==projectile,"Second load multiplied support");h.succeed();});});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeDefenseCallbacksAndStatusProtectionActuallyOperate(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));
        var types=Map.of("physical_attack_resistance","minecraft:mob_attack","magic_resistance","tensura:magic","holy_attack_resistance","tensura:holy_damage","spiritual_attack_resistance","tensura:soul_scatter", "light_attack_resistance","tensura:light_elemental", "darkness_attack_resistance","tensura:darkness_elemental", "spatial_attack_resistance","tensura:space_elemental");
        for(var entry:types.entrySet()){
            var i=support(b,entry.getKey());var skill=(ResistSkill)i.getSkill();var source=new DamageSource(h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(ResourceLocation.parse(entry.getValue())).orElseThrow(),p);
            h.assertTrue(skill.isDamageResisted(b,source,i),"Fixture source not classified: "+entry);
            var nullification=support(b,entry.getKey().replace("_resistance","_nullification"));nullification.setToggled(false);
            h.assertTrue(!skill.onTakenDamage(i,b,source,Changeable.of(100f)),"Native defense did not protect: "+entry);
            i.setToggled(false);h.assertTrue(skill.onTakenDamage(i,b,source,Changeable.of(100f)),"OFF comparison did not permit hit");i.setToggled(true);nullification.setToggled(true);
            h.assertTrue(!((ResistSkill)nullification.getSkill()).onTakenDamage(nullification,b,source,Changeable.of(100f)),"Native nullification inactive: "+nullification.getSkillId());
        }
        h.assertTrue(!b.addEffect(new MobEffectInstance(MobEffects.POISON,200)) && SkillUtils.shouldCancelPain(b),"Native status/pain protection inactive");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ordinaryAndGodlyAttackersRetainDifferentDefenseInteractions(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var i=support(b,"physical_attack_nullification");var skill=(ResistSkill)i.getSkill();
        var ordinary=b.damageSources().playerAttack(p);h.assertTrue(!skill.onBeingDamaged(i,b,ordinary,100),"Ordinary attack ignored resistance");
        HunterData.learnReward(p,WildHunt.MYTHIC.get(MythicKind.THOTH).get());var godly=b.damageSources().playerAttack(p);
        h.assertTrue(skill.onBeingDamaged(i,b,godly,100) && i.isToggled(),"Godly penetration failed or disabled the target's defense");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeNullificationsBlockOrdinaryUltimateButGodlyOwnershipStillPenetrates(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));
        var ultimate=learn(p,MythicKind.JANUS);ultimate.setMastery(ultimate.getMaxMastery());
        afterTracking(h,List.of(b),()->{
            float before=b.getHealth();double soul=TensuraStorages.getExistenceFrom(b).getSpiritualHealth();
            MythicArts.hit((MythicSkill)ultimate.getSkill(),ultimate,p,b,1200,1,io.github.manasmods.tensura.damage.TensuraDamageTypes.SPACE_ELEMENTAL);
            h.assertTrue(b.getHealth()==before && TensuraStorages.getExistenceFrom(b).getSpiritualHealth()==soul,"Level-one Ultimate bypass incorrectly defeated nullification");
            var godly=learn(p,MythicKind.THOTH);godly.setMastery(godly.getMaxMastery());b.invulnerableTime=0;before=b.getHealth();soul=TensuraStorages.getExistenceFrom(b).getSpiritualHealth();
            MythicArts.hit((MythicSkill)godly.getSkill(),godly,p,b,1200,0,io.github.manasmods.tensura.damage.TensuraDamageTypes.LIGHT_ELEMENTAL);
            h.assertTrue(b.getHealth()<before || TensuraStorages.getExistenceFrom(b).getSpiritualHealth()<soul,"Godly hit could not damage repaired support");
            h.assertTrue(b.isAlive(),"Small endgame probes unexpectedly one-shot the Myth");enabled(h,b);h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void farCleaveIsFreeAndAiChoosesAnAvailableRangedRite(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(35,0,0));b.setTarget(z);var i=moira(b);double mp=TensuraStorages.getExistenceFrom(b).getMagicule();i.onPressed(b,0,0);
        h.assertTrue(!b.isCasting() && i.getCoolDown(0)==0 && TensuraStorages.getExistenceFrom(b).getMagicule()==mp,"Out-of-range fan consumed cost/cooldown");
        h.assertTrue(AnankeTactics.chooseRite(b,i,z,0)==1,"AI chose a short-range move against distant target");i.setCoolDown(10,1);h.assertTrue(AnankeTactics.chooseRite(b,i,z,0)==-1,"AI selected an unavailable or out-of-range rite");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void paidRepositionRespectsCastingCollisionAndEncounterBounds(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(35,0,0));b.setTarget(z);double mp=TensuraStorages.getExistenceFrom(b).getMagicule();var before=b.position();
        for(double angle:new double[]{0,Math.PI/3,-Math.PI/3,2*Math.PI/3,-2*Math.PI/3}){var at=z.position().add(new Vec3(-1,0,0).yRot((float)angle).scale(10));h.getLevel().setBlockAndUpdate(net.minecraft.core.BlockPos.containing(at).above(),Blocks.STONE.defaultBlockState());}
        h.assertTrue(!AnankeTactics.reposition(b,z) && b.position().equals(before) && TensuraStorages.getExistenceFrom(b).getMagicule()==mp,"Blocked shift moved/charged");
        for(double angle:new double[]{0,Math.PI/3,-Math.PI/3,2*Math.PI/3,-2*Math.PI/3}){var at=z.position().add(new Vec3(-1,0,0).yRot((float)angle).scale(10));h.getLevel().setBlockAndUpdate(net.minecraft.core.BlockPos.containing(at).above(),Blocks.AIR.defaultBlockState());}
        h.assertTrue(AnankeTactics.reposition(b,z) && b.distanceToSqr(z)>=9*9 && b.distanceToSqr(z)<=11*11 && Math.abs(mp-TensuraStorages.getExistenceFrom(b).getMagicule()-20000)<1,"Paid shift did not close distance correctly");
        z.setPos(b.position().add(30,0,0));castBoss(b,z,1);before=b.position();h.assertTrue(!AnankeTactics.reposition(b,z) && b.position().equals(before),"Shift moved a locked warning origin");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void starfallReleasesMovementWhileItsCosmeticTailRemains(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));castBoss(b,z,1);var visual=b.rite.getUUID("visual");
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getEntity(visual)!=null,"Visual fixture not indexed yet")).thenExecute(()->{
            // Advance every frame only after the actual visual exists in the native index.
            var plan=b.rite;for(int age=0;age<=96;age++)age(h,b,age);
            h.assertTrue(plan.getInt("star")==6 && !b.isCasting() && b.riteMode()==0,"Starfall skipped hits or retained its movement lock");
            var effect=h.getLevel().getEntity(visual);h.assertTrue(effect!=null && !effect.isRemoved(),"Releasing movement deleted the visual tail");
            AnankeRites.clear(h.getLevel(),b);h.assertTrue(effect.isRemoved(),"Encounter cleanup left a fading owned warning");h.succeed();
        });
    }
}
