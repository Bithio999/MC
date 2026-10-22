package dev.wildhunt;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.damage.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import static dev.wildhunt.MythicGameTests.*;
import static dev.wildhunt.MythBossGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class AnankePhaseGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void fundedInfiniteRegenerationPreventsDeathBeforeSpendingAPhase(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var skill=learn(p,MythicKind.THOTH);skill.setMastery(skill.getMaxMastery());
        var e=TensuraStorages.getExistenceFrom(b);e.setMagicule(15000000);
        MythicArts.hit((MythicSkill)skill.getSkill(),skill,p,b,1000000,0,TensuraDamageTypes.MAGIC_GENERIC);
        h.assertTrue(b.isAlive() && b.phase()==1,"Funded Infinite Regeneration lost priority to a phase change");
        h.runAfterDelay(15,()->{
            h.assertTrue(b.phase()==1 && b.getHealth()==b.getMaxHealth() && e.getMagicule()<15000000,"Native recovery failed to restore HP and spend magicules before a phase");
            e.setMagicule(0);b.invulnerableTime=0;
            MythicArts.hit((MythicSkill)skill.getSkill(),skill,p,b,1000000,0,TensuraDamageTypes.MAGIC_GENERIC);full(h,b,2);h.succeed();
        });
    }
    static void full(GameTestHelper h,AnankeEntity b,int phase){
        var e=TensuraStorages.getExistenceFrom(b);
        h.assertTrue(b.isAlive() && b.phase()==phase && b.getHealth()==b.getMaxHealth(),"Phase/HP did not revive: "+b.phase()+" / "+b.getHealth());
        h.assertTrue(e.getSpiritualHealth()==b.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH) && e.getAura()==io.github.manasmods.tensura.util.EnergyHelper.getMaxAura(b)
            && e.getMagicule()==io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(b),"A phase revival did not refill all native pools");
        h.assertTrue(b.deathTime==0 && !b.isCasting() && b.riteMode()==0,"Revival retained death/cast state");
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nonfatalWoundsNeverAdvancePhases(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var regen=AnankeSupportGameTests.support(b,"infinite_regeneration");regen.onToggleOff(b);regen.setToggled(false);
        b.hurt(b.damageSources().genericKill(),40000);h.assertTrue(b.phase()==1 && b.getHealth()==8000,"Nonfatal damage advanced the phase");b.setNoAi(false);
        h.runAfterDelay(15,()->{h.assertTrue(b.phase()==1,"Low health still triggered a percentage phase change");b.setNoAi(true);h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void exhaustedFatalHpHitsAdvanceOnePhaseEachThenActuallyKill(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var skill=learn(p,MythicKind.THOTH);skill.setMastery(skill.getMaxMastery());
        var source=((MythicSkill)skill.getSkill()).createSource(skill,p,TensuraDamageTypes.MAGIC_GENERIC,0);
        h.assertTrue(io.github.manasmods.tensura.effect.ability.InstantRegenerationEffect.canStopDeath(source,b),"Fixture is not an ordinary regeneration-protected fatal source");
        var e=TensuraStorages.getExistenceFrom(b);e.setAura(123);e.setMagicule(0);e.setSpiritualHealth(789);
        MythicArts.hit((MythicSkill)skill.getSkill(),skill,p,b,1000000,0,TensuraDamageTypes.MAGIC_GENERIC);full(h,b,2);
        h.assertTrue(HunterData.bossKills(p).isEmpty(),"An intermediate phase awarded boss credit");
        e.setAura(23);e.setMagicule(0);e.setSpiritualHealth(89);b.invulnerableTime=0;
        MythicArts.hit((MythicSkill)skill.getSkill(),skill,p,b,1000000,0,TensuraDamageTypes.MAGIC_GENERIC);full(h,b,3);
        h.assertTrue(HunterData.bossKills(p).isEmpty(),"Second revival awarded boss credit");
        e.setMagicule(0);b.invulnerableTime=0;MythicArts.hit((MythicSkill)skill.getSkill(),skill,p,b,1000000,0,TensuraDamageTypes.MAGIC_GENERIC);
        h.assertTrue(!b.isAlive() && b.phase()==3,"Third fatal hit was prevented or gave another life");
        h.assertTrue(HunterData.bossKills(p).contains("wildhunt:ananke"),"Final death did not award boss credit");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void spiritualDeathAlsoConsumesExactlyOnePhase(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var e=TensuraStorages.getExistenceFrom(b);e.setAura(1);e.setMagicule(2);
        var source=TensuraDamageTypes.getEntityDamageSource(h.getLevel(),TensuraDamageTypes.SOUL_SCATTER,p);
        h.assertTrue(TensuraDamageHelper.directSpiritualHurt(b,p,source,1000000,0),"Native spiritual hit did not execute");full(h,b,2);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void revivalCancelsPendingRitesAndDoesNotDropIntermediateLoot(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));var z=target(h,b.position().add(8,0,0));castBoss(b,z,1);var visual=b.rite.getUUID("visual");
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getEntity(visual)!=null,"Visual fixture not indexed yet")).thenExecute(()->{
            b.hurt(b.damageSources().genericKill(),1000000);full(h,b,2);h.assertTrue(h.getLevel().getEntity(visual)==null || h.getLevel().getEntity(visual).isRemoved(),"Revival left its old rite active");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,b.getBoundingBox().inflate(8)).isEmpty(),"Intermediate phase dropped rewards");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void spentLivesPersistAcrossReloadAndDoNotRefillOnLoad(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));b.hurt(b.damageSources().genericKill(),1000000);var e=TensuraStorages.getExistenceFrom(b);b.setHealth(30000);e.setAura(123);e.setMagicule(456);e.setSpiritualHealth(50000);
        var tag=new CompoundTag();b.save(tag);b.discard();var copy=(AnankeEntity)EntityType.loadEntityRecursive(tag,h.getLevel(),entity->entity);h.getLevel().addFreshEntity(copy);
        h.assertTrue(copy.phase()==2 && copy.getHealth()==30000 && TensuraStorages.getExistenceFrom(copy).getMagicule()==456,"Reload restored a spent life or refilled resources");
        copy.invulnerableTime=0;copy.hurt(copy.damageSources().genericKill(),1000000);full(h,copy,3);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void confirmedDamageFlashesDespitePainNullificationAndBlockedHitsDoNot(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));
        h.assertTrue(io.github.manasmods.tensura.ability.SkillUtils.shouldCancelPain(b),"Native Pain Nullification fixture inactive");
        b.hurt(b.damageSources().playerAttack(p),100);h.assertTrue(!b.hasDamageFeedback(),"Blocked/nullified hit falsely flashed as damage");
        b.invulnerableTime=0;b.hurt(b.damageSources().genericKill(),100);h.assertTrue(b.hasDamageFeedback(),"Confirmed HP hit had no synchronized flash");
        h.runAfterDelay(11,()->{h.assertTrue(!b.hasDamageFeedback(),"Hit flash did not expire");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void spiritualOnlyHitsAlsoProduceDamageFeedback(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));
        h.assertTrue(TensuraDamageHelper.directSpiritualHurt(b,p,100,0),"Native SHP-only fixture failed");
        h.startSequence().thenWaitUntil(()->h.assertTrue(b.hasDamageFeedback(),"SHP-only hit did not flash")).thenExecute(()->{h.assertTrue(b.phase()==1,"SHP-only hit consumed a life");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void operatorKillStillRemovesTheBossImmediately(GameTestHelper h){
        var p=player(h);var b=boss(h,p.position().add(10,0,0));b.kill();h.assertTrue(!b.isAlive() && b.phase()==1,"Operator kill spent a combat life instead of removing the boss");h.succeed();
    }
}
