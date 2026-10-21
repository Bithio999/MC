package dev.wildhunt;

import dev.wildhunt.myths.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.data.TensuraSkillTags;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.boss.template.BossFightInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class MythBossGameTests {
    static AnankeEntity boss(GameTestHelper h,Vec3 at){var b=AnankeItems.summon(h.getLevel(),at);h.assertTrue(b!=null,"Boss did not summon in open fixture");b.setNoAi(true);return b;}
    static void age(GameTestHelper h,AnankeEntity b,int ticks){b.rite.putLong("start",h.getLevel().getGameTime()-ticks);AnankeRites.tick(h.getLevel(),b);}
    static ManasSkillInstance moira(AnankeEntity b){return SkillAPI.getSkillsFrom(b).getSkill(MythBosses.MOIRA.get()).orElseThrow();}
    static void castBoss(AnankeEntity b,LivingEntity target,int mode){b.setTarget(target);moira(b).setCoolDown(0,mode);moira(b).onPressed(b,0,mode);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeStatsAndRealSkillPackageAreInitializedOnce(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var e=TensuraStorages.getExistenceFrom(b);
        h.assertTrue(b.getMaxHealth()==48000 && b.getHealth()==48000,"Native HP clamped: "+b.getMaxHealth());
        h.assertTrue(EnergyHelper.getMaxAura(b)==15000000 && EnergyHelper.getMaxMagicule(b)==15000000,"Native resource split is not 15M/15M: "+EnergyHelper.getMaxAura(b)+"/"+EnergyHelper.getMaxMagicule(b));
        h.assertTrue(b.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)==120000 && e.getSpiritualHealth()==120000,"Native SHP is wrong");
        h.assertTrue(RaceAPI.getRaceFrom(b).getRace().orElseThrow().getRace()==MythBosses.RACE.get(),"Boss race lost");
        var skills=SkillAPI.getSkillsFrom(b);h.assertTrue(skills.getLearnedSkills().stream().filter(i->i.getMastery()>=0).count()==MythBosses.NATIVE_SKILLS.size()+2,"Wrong skill count: "+skills.getLearnedSkills().stream().map(v->v.getSkillId().toString()).toList());
        for(var i:skills.getLearnedSkills())if(i.getMastery()>=0)h.assertTrue(i.isMastered(b),"Unmastered boss skill: "+i.getSkillId());
        h.assertTrue(moira(b).is(TensuraSkillTags.ULTIMATE_SKILLS) && !MythsTiers.isGodly(MythBosses.MOIRA.get()) && GodlyPassives.ownsGodly(b),"Native Ultimate/Godly classification broken");
        e.setMagicule(1000);b.setHealth(5000);b.initializeMyth();h.assertTrue(b.getHealth()==5000 && e.getMagicule()==1000,"Initialization refilled a living boss");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void bossLineageAndMoiraNeverEnterDefaultReincarnationOrSynthesis(GameTestHelper h){
        var p=player(h);for(int pool=0;pool<3;pool++){var menu=new ReincarnationMenu(0,p.getInventory(),p,false,pool,0,0);h.assertTrue(!menu.getRacePool().contains(MythBosses.RACE.get()) && !menu.getRandomRacePool().contains(MythBosses.RACE.get()),"Boss race entered default reincarnation");}
        h.assertTrue(!ReincarnationMenu.getSkillPool().contains(MythBosses.MOIRA.get()) && !MythBosses.MOIRA.get().canBeSlotted(MythBosses.MOIRA.get().createDefaultInstance(),p,0),"Boss Ultimate became a player roll/slot");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void fatebreakerWaitsThenHitsOnlyItsWarnedFanAndChargesNativeMagicules(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var front=target(h,b.position().add(8,0,0));var back=target(h,b.position().add(-8,0,0));
        afterTracking(h,List.of(b,front,back),()->{double before=TensuraStorages.getExistenceFrom(b).getMagicule();castBoss(b,front,0);
            h.assertTrue(b.isCasting() && moira(b).getCoolDown(0)==4 && Math.abs(before-TensuraStorages.getExistenceFrom(b).getMagicule()-4000)<1,"Native cast costs/cooldown failed");
            age(h,b,23);h.assertTrue(front.getHealth()==1024,"Cleave landed before its warning");age(h,b,24);h.assertTrue(front.getHealth()<1024 && back.getHealth()==1024,"Cleave did not respect its fan");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void starfallLocksEachWarningSeparatelyAsTargetMoves(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));
        afterTracking(h,List.of(b,z),()->{castBoss(b,z,1);Vec3 first=AnankeRites.star(b.rite,0);z.setPos(z.position().add(0,0,14));age(h,b,12);Vec3 second=AnankeRites.star(b.rite,1);
            h.assertTrue(AnankeRites.star(b.rite,0).equals(first) && second.z>8,"Warnings slid or did not aim at the moving target");z.setPos(b.position().add(first).add(0,-z.getBbHeight()*.5,0));age(h,b,27);h.assertTrue(z.getHealth()==1024,"Starfall hit before its warning");age(h,b,28);h.assertTrue(z.getHealth()<1024,"Warned starfall did not damage at its saved point");h.assertTrue(AnankeRites.star(b.rite,1).equals(second),"Second warning moved after being locked");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void processionHitsItsHollowFrontOnceAndLeavesTheInteriorSafe(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));var center=target(h,b.position().add(0,0,1));z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8000);z.setHealth(8000);
        afterTracking(h,List.of(b,z,center),()->{castBoss(b,z,2);age(h,b,30);h.assertTrue(z.getHealth()==8000 && center.getHealth()==1024,"Procession filled its hollow interior");age(h,b,39);float hp=z.getHealth();h.assertTrue(hp<8000,"Expanding front failed to hit");z.invulnerableTime=0;age(h,b,40);h.assertTrue(z.getHealth()==hp && center.getHealth()==1024,"Wave hit twice or filled its center");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void lastDawnHasRealGoldenRefugeAndOuterBoundary(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var danger=target(h,b.position().add(3,0,0));var refuge=target(h,b.position().add(9,0,0));var outer=target(h,b.position().add(34,0,0));
        afterTracking(h,List.of(b,danger,refuge,outer),()->{castBoss(b,danger,3);age(h,b,74);h.assertTrue(danger.getHealth()==1024,"Dawn struck before 75-tick warning");age(h,b,75);h.assertTrue(danger.getHealth()<1024 && refuge.getHealth()==1024 && outer.getHealth()==1024,"Last Dawn ignored gold refuge or 32-block boundary");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void distantStarWarningsKeepTheirActualPlacementAndSkyLanceInTheRenderBounds(GameTestHelper h){
        var p=player(h);var plan=new CompoundTag();plan.putInt("warned",1);ApexArts.point(plan,"star_0",new Vec3(-65,0,68));
        var visual=ArtVisualEntity.spawn(h.getLevel(),ArtVisualEntity.ANANKE_STARFALL,p.position(),new Vec3(24,0,0),150);visual.barrage(plan);
        h.assertTrue(visual.getBoundingBoxForCulling().contains(p.position().add(-65,20,68)),"A distant warned lance was culled by the caster's origin");h.assertTrue(visual.getType()==ArtVisualEntity.MYTH_TYPE.get() && visual.getType().clientTrackingRange()==12 && ArtVisualEntity.TYPE.get().clientTrackingRange()==8 && visual.shouldRenderAtSqrDistance(145*145),"Encounter warning disappeared within a possible arena separation");visual.discard();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void countermandInterruptsMoiraWithoutRemovingGodlyOwnership(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));
        afterTracking(h,List.of(b,z),()->{castBoss(b,z,0);var visual=b.rite.getUUID("visual");var seal=new CompoundTag();seal.putString("skill","wildhunt:moira");seal.putLong("until",h.getLevel().getGameTime()+120);HunterData.of(b).put("lugh_seal",seal);age(h,b,24);
            h.assertTrue(!b.isCasting() && z.getHealth()==1024 && (h.getLevel().getEntity(visual)==null || h.getLevel().getEntity(visual).isRemoved()),"Countermand did not cancel warned attack/visual");h.assertTrue(GodlyPassives.ownsGodly(b),"Interrupt removed Godly ownership");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void saveReloadPreservesWoundsAndResourcesButCancelsPendingRites(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));b.setHealth(8000);var e=TensuraStorages.getExistenceFrom(b);e.setMagicule(100000);e.setSpiritualHealth(60000);castBoss(b,z,0);
        var tag=new CompoundTag();b.save(tag);b.discard();var copy=(AnankeEntity)EntityType.loadEntityRecursive(tag,h.getLevel(),entity->entity);h.assertTrue(copy!=null,"Reload failed");h.getLevel().addFreshEntity(copy);
        h.assertTrue(copy.getHealth()==8000 && !copy.isCasting() && copy.riteMode()==0,"Reload healed wounds or resumed an untelegraphed rite");h.runAfterDelay(2,()->{h.assertTrue(copy.getHealth()<9000 && TensuraStorages.getExistenceFrom(copy).getMagicule()<200000 && TensuraStorages.getExistenceFrom(copy).getSpiritualHealth()<70000,"Reload initialized free resources");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ritualConsumesOnlyOnSuccessAndNativeBossTagsArePresent(GameTestHelper h){
        var p=player(h);var at=p.blockPosition().offset(10,-1,0);h.getLevel().setBlockAndUpdate(at,Blocks.LODESTONE.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MythBosses.WRIT.get()));
        var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));
        h.getLevel().setBlockAndUpdate(at.above(2),Blocks.STONE.defaultBlockState());h.assertTrue(MythBosses.WRIT.get().useOn(context)==InteractionResult.FAIL && p.getMainHandItem().getCount()==1,"Blocked ritual consumed its writ");h.getLevel().setBlockAndUpdate(at.above(2),Blocks.AIR.defaultBlockState());
        h.assertTrue(MythBosses.WRIT.get().useOn(context)==InteractionResult.CONSUME && p.getMainHandItem().isEmpty(),"Successful ritual failed/was not consumed");
        h.runAfterDelay(5,()->{var b=h.getLevel().getEntitiesOfClass(AnankeEntity.class,new AABB(at).inflate(8)).getFirst();h.assertTrue(b.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.ResourceLocation.parse("c:bosses"))),"Native progression boss tag missing");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MythBosses.WRIT.get()));h.assertTrue(MythBosses.WRIT.get().useOn(context)==InteractionResult.FAIL && p.getMainHandItem().getCount()==1,"Duplicate ritual consumed a writ");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void trophyRestoresCappedNativeResourcesAndStartsFiveMinuteCooldown(GameTestHelper h){
        var p=player(h);p.setHealth(p.getMaxHealth()*.1f);var e=TensuraStorages.getExistenceFrom(p);double max=p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);e.setSpiritualHealth(max*.1);e.setAura(0);e.setMagicule(0);var stack=new ItemStack(MythBosses.TESTAMENT.get(),2);
        MythBosses.TESTAMENT.get().finishUsingItem(stack,h.getLevel(),p);h.assertTrue(Math.abs(p.getHealth()-p.getMaxHealth()*.6)<.01 && Math.abs(e.getSpiritualHealth()-max*.6)<.01,"Trophy did not restore half maximum HP/SHP");h.assertTrue(e.getAura()==EnergyHelper.getMaxAura(p) && e.getMagicule()==EnergyHelper.getMaxMagicule(p) && stack.getCount()==1 && p.getCooldowns().isOnCooldown(MythBosses.TESTAMENT.get()),"Trophy resources/consumption/cooldown wrong");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void bossHealthIsRealAndKeepsExternalDebuffsWithoutChangingOtherEntities(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,p.position().add(20,0,0));
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(48000);h.assertTrue(z.getMaxHealth()<=10000,"Boss raised the global health limit");
        b.getAttribute(Attributes.MAX_HEALTH).addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(WildHunt.id("myth_test_wound"),-.25,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        h.assertTrue(b.getMaxHealth()==36000,"Boss ignored external health modifiers");b.setHealth(36000);b.hurt(b.damageSources().genericKill(),4000);h.assertTrue(b.isAlive() && b.getHealth()<=32000,"Boss has a hidden incoming damage cap");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void liveAiEntersLastPhaseAndPaysForItsUltimate(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));z.setInvulnerable(true);b.setTarget(z);b.setHealth(12000);b.setNoAi(false);
        h.runAfterDelay(110,()->{h.assertTrue(b.phase()==3,"Live AI did not unlock final phase");h.assertTrue(b.isCasting() && b.riteMode()==4 && moira(b).getCoolDown(3)>0,"Live AI did not cast/pay for Last Dawn");h.assertTrue(SkillAPI.getSkillsFrom(b).getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).orElseThrow().getCoolDown(3)>0,"Phase AI did not use Astral Library");b.setNoAi(true);h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeArenaAdapterRestrictsPlayerTargetsToJoinedParticipants(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var fight=new BossFightInstance(b.blockPosition(),h.getLevel().dimension(),48);String id="test_ananke_"+b.getUUID();var holder=TensuraStorages.getBossFightHolder(h.getLevel().getServer().overworld());holder.addBossFight(id,fight);b.onSpawned(h.getLevel(),id,fight);
        h.assertTrue(b.getBossFightId().equals(id) && !b.eligible(p) && !HunterData.canHarm(b,p),"Arena allowed an unjoined player");fight.getJoinedPlayers().add(p.getUUID());h.assertTrue(b.eligible(p),"Arena rejected a participant");h.succeed();
    }
}
