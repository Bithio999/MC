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
    static void enabled(GameTestHelper h,AnankeEntity b){for(var id:MythBosses.NATIVE_SKILLS){var i=SkillAPI.getSkillsFrom(b).getSkill(ResourceLocation.parse(id)).orElseThrow();h.assertTrue(i.isMastered(b) && (!i.canBeToggled(b) || i.isToggled()),"Inactive stored support: "+id);}}
    static AnankeEntity reload(GameTestHelper h,AnankeEntity b,boolean legacy){var tag=new CompoundTag();b.save(tag);if(legacy)tag.remove("myth_support_version");b.discard();var copy=(AnankeEntity)EntityType.loadEntityRecursive(tag,h.getLevel(),e->e);h.getLevel().addFreshEntity(copy);return copy;}
    static void isolateRecovery(AnankeEntity b){var i=support(b,"self_regeneration");i.onToggleOff(b);i.setToggled(false);b.getAttribute(TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION).setBaseValue(0);b.setHealth(24000);TensuraStorages.getExistenceFrom(b).setSpiritualHealth(60000);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void everyStoredSupportIsEnabledWithoutDuplicateModifiers(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));enabled(h,b);double space=b.getAttributeValue(TensuraAttributes.SPACE_BOOST),sense=b.getAttributeValue(TensuraAttributes.PRESENCE_SENSE);
        for(int n=0;n<20;n++)b.initializeMyth();
        h.runAfterDelay(25,()->{enabled(h,b);h.assertTrue(b.getAttributeValue(TensuraAttributes.SPACE_BOOST)==space && b.getAttributeValue(TensuraAttributes.PRESENCE_SENSE)==sense,"Repeated initialization multiplied support");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void commandStyleSpawnWithoutFinalizeAlsoActivatesStoredSkills(GameTestHelper h){
        var p=player(h);var b=MythBosses.ANANKE.get().create(h.getLevel());b.setPos(p.position().add(10,0,0));b.setNoAi(true);h.getLevel().addFreshEntity(b);
        h.runAfterDelay(2,()->{enabled(h,b);h.assertTrue(b.getMaxHealth()==48000,"Command-style spawn lost HP");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void legacyGhostBonusesMigrateOnceAndReloadKeepsWounds(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));b.setHealth(10000);var e=TensuraStorages.getExistenceFrom(b);e.setMagicule(100000);e.setSpiritualHealth(60000);
        double space=b.getAttributeValue(TensuraAttributes.SPACE_BOOST),melee=b.getAttributeValue(TensuraAttributes.AUTO_MELEE_DODGE_CHANCE),projectile=b.getAttributeValue(TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE);
        for(var id:List.of("mathematician","observer","chant_annulment","spatial_domination","physical_attack_resistance","spiritual_attack_resistance","magic_resistance","holy_attack_resistance","abnormal_condition_nullification","pain_nullification"))support(b,id).setToggled(false);
        var copy=reload(h,b,true);h.runAfterDelay(25,()->{enabled(h,copy);h.assertTrue(copy.getHealth()<11000 && TensuraStorages.getExistenceFrom(copy).getMagicule()<200000,"Migration refilled wounds/resources");
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
            h.assertTrue(!skill.onTakenDamage(i,b,source,Changeable.of(100f)),"Native defense did not protect: "+entry);
            i.setToggled(false);h.assertTrue(skill.onTakenDamage(i,b,source,Changeable.of(100f)),"OFF comparison did not permit hit");i.setToggled(true);
        }
        h.assertTrue(!b.addEffect(new MobEffectInstance(MobEffects.POISON,200)) && SkillUtils.shouldCancelPain(b),"Native status/pain protection inactive");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ordinaryAndGodlyAttackersRetainDifferentDefenseInteractions(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var i=support(b,"physical_attack_resistance");var skill=(ResistSkill)i.getSkill();
        var ordinary=b.damageSources().playerAttack(p);h.assertTrue(!skill.onBeingDamaged(i,b,ordinary,100),"Ordinary attack ignored resistance");
        HunterData.learnReward(p,WildHunt.MYTHIC.get(MythicKind.THOTH).get());var godly=b.damageSources().playerAttack(p);
        h.assertTrue(skill.onBeingDamaged(i,b,godly,100) && i.isToggled(),"Godly penetration failed or disabled the target's defense");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void endgameUltimateAndGodlyHitsStillDamageTheRepairedBoss(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));
        var ultimate=learn(p,MythicKind.JANUS);ultimate.setMastery(ultimate.getMaxMastery());
        afterTracking(h,List.of(b),()->{
            float before=b.getHealth();double soul=TensuraStorages.getExistenceFrom(b).getSpiritualHealth();
            MythicArts.hit((MythicSkill)ultimate.getSkill(),ultimate,p,b,1200,1,io.github.manasmods.tensura.damage.TensuraDamageTypes.SPACE_ELEMENTAL);
            h.assertTrue(b.getHealth()<before || TensuraStorages.getExistenceFrom(b).getSpiritualHealth()<soul,"Native Ultimate penetration could not damage repaired support");
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
    @GameTest(template="empty",timeoutTicks=240)
    public static void renewalWaitsThenPaysForFourBoundedHpAndShpPulses(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));b.setTarget(z);isolateRecovery(b);b.recovery.damaged(b);
        h.assertTrue(!b.recovery.tryBegin(b),"Renewal ignored recent damage");
        h.runAfterDelay(85,()->{h.assertTrue(b.recovery.tryBegin(b) && b.riteMode()==5,"Recovery did not begin after damage delay");float hp=b.getHealth();double shp=TensuraStorages.getExistenceFrom(b).getSpiritualHealth();
            h.runAfterDelay(39,()->{b.recovery.tick(b);h.assertTrue(b.getHealth()==hp,"Renewal healed before its windup");});
            for(int pulse=0;pulse<4;pulse++){final int index=pulse;h.runAfterDelay(40+pulse*20,()->{
                double mp=TensuraStorages.getExistenceFrom(b).getMagicule();b.recovery.tick(b);
                h.assertTrue(Math.abs(b.getHealth()-hp-1440*(index+1))<1 && Math.abs(TensuraStorages.getExistenceFrom(b).getSpiritualHealth()-shp-3600*(index+1))<1,"Recovery exceeded/missed bounded pulse");
                h.assertTrue(Math.abs(mp-TensuraStorages.getExistenceFrom(b).getMagicule()-144000)<1,"Restored HP/SHP charged incorrectly");
                float after=b.getHealth();b.recovery.tick(b);h.assertTrue(b.getHealth()==after,"Same frame paid/healed twice");
                if(index==3){h.assertTrue(!b.recovery.active() && !b.recovery.tryBegin(b),"Recovery did not end or ignored cooldown");h.succeed();}
            });}
        });
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void damageInterruptsRenewalAndReloadCannotResetItsCooldown(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));b.setTarget(z);isolateRecovery(b);b.recovery.damaged(b);
        h.runAfterDelay(85,()->{h.assertTrue(b.recovery.tryBegin(b),"Recovery fixture failed");h.runAfterDelay(5,()->{var visual=h.getLevel().getEntitiesOfClass(ArtVisualEntity.class,b.getBoundingBox().inflate(8),v->v.ownedBy(b) && v.kind()==ArtVisualEntity.ANANKE_RENEWAL).getFirst();
            b.hurt(b.damageSources().genericKill(),100);h.assertTrue(!b.recovery.active() && visual.isRemoved(),"Accepted damage did not cancel recovery/visual");var copy=reload(h,b,false);copy.setTarget(z);
            h.runAfterDelay(85,()->{h.assertTrue(!copy.recovery.active() && !copy.recovery.tryBegin(copy),"Reload resumed recovery or reset cooldown");h.succeed();});});});
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void soulDamageAndCountermandCancelRenewalWithoutFreeHealing(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));b.setTarget(z);isolateRecovery(b);b.recovery.damaged(b);
        h.runAfterDelay(85,()->{h.assertTrue(b.recovery.tryBegin(b),"Recovery fixture failed");b.recovery.observeDamage(b);TensuraStorages.getExistenceFrom(b).setSpiritualHealth(59000);b.recovery.observeDamage(b);h.assertTrue(!b.recovery.active(),"SHP-only damage failed to interrupt");
            var other=boss(h,p.position().add(25,0,0));other.setTarget(z);isolateRecovery(other);h.assertTrue(other.recovery.tryBegin(other),"Second recovery fixture failed");
            var seal=new CompoundTag();seal.putString("skill","wildhunt:moira");seal.putLong("until",h.getLevel().getGameTime()+120);HunterData.of(other).put("lugh_seal",seal);other.recovery.tick(other);h.assertTrue(!other.recovery.active() && other.getHealth()==24000,"Countermand failed to cancel intrinsic recovery");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void renewalNeedsMagiculesAndRespectsNativeHealingSuppression(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));b.setTarget(z);isolateRecovery(b);
        TensuraStorages.getExistenceFrom(b).setMagicule(0);h.assertTrue(!b.recovery.tryBegin(b),"Unfunded recovery began");TensuraStorages.getExistenceFrom(b).setMagicule(1000000);
        // Frost is a native healing gate not covered by Abnormal Condition Nullification.
        var frost=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.FROST);
        b.addEffect(new MobEffectInstance(frost,200));h.assertTrue(SkillUtils.shouldCancelHealing(b) && !b.recovery.tryBegin(b),"Native healing suppression failed");h.succeed();
    }
}
