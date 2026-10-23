package dev.wildhunt;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.data.TensuraSkillTags;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.boss.template.BossFightInstance;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class EschatonGameTests {
    static void loadEntityArea(GameTestHelper h,Vec3 at){loadEntityArea(h,at,1);}
    static void loadEntityArea(GameTestHelper h,Vec3 at,int radius){var center=net.minecraft.core.BlockPos.containing(at);for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){var pos=center.offset(x*16,0,z*16);h.getLevel().getChunk(pos);h.getLevel().setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);}}
    static net.minecraft.world.entity.monster.Husk target(GameTestHelper h,Vec3 at){loadEntityArea(h,at);return MythicGameTests.target(h,at);}
    static EschatonEntity origin(GameTestHelper h,Vec3 at){loadEntityArea(h,at,3);var b=EschatonItems.summon(h.getLevel(),at);h.assertTrue(b!=null,"Final Myth did not summon in open space");b.setNoAi(true);return b;}
    static ManasSkillInstance skill(EschatonEntity b){return SkillAPI.getSkillsFrom(b).getSkill(EschatonContent.DEMIURGE.get()).orElseThrow();}
    static void cast(EschatonEntity b,LivingEntity target,int mode){b.setTarget(target);skill(b).setCoolDown(0,mode);skill(b).onPressed(b,0,mode);}
    static void age(GameTestHelper h,EschatonEntity b,int ticks){b.plan.putLong("start",h.getLevel().getGameTime()-ticks);EschatonRites.tick(h.getLevel(),b);}
    static void done(GameTestHelper h,EschatonEntity b){b.discard();h.succeed();}
    static void full(GameTestHelper h,EschatonEntity b,int form){var e=TensuraStorages.getExistenceFrom(b);h.assertTrue(b.isAlive() && b.form()==form && b.getHealth()==1_000_000 && e.getSpiritualHealth()==4_000_000 && e.getAura()==500_000_000 && e.getMagicule()==500_000_000,"Form revival failed to restore four native pools: form="+b.form()+", HP="+b.getHealth()+", SHP="+e.getSpiritualHealth()+", AP="+e.getAura()+", MP="+e.getMagicule());}
    @GameTest(template="empty",batch="origin_registration",timeoutTicks=100)
    public static void billionEpIsRealAndTheNativeSkillPackageIsStoredMasteredAndEnabled(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var e=TensuraStorages.getExistenceFrom(b);var storage=SkillAPI.getSkillsFrom(b);
        h.assertTrue(EnergyHelper.getBaseMaxEP(b)==1_000_000_000 && EnergyHelper.getMaxEP(b)==1_000_000_000,"EP label is not the actual native billion: "+EnergyHelper.getBaseMaxEP(b)+" / "+EnergyHelper.getMaxEP(b));full(h,b,1);
        h.assertTrue(RaceAPI.getRaceFrom(b).getRace().orElseThrow().getRace()==EschatonContent.RACE.get(),"Wrong Origin Sovereign race");
        for(String id:MythBosses.NATIVE_SKILLS){var i=storage.getSkill(net.minecraft.resources.ResourceLocation.parse(id)).orElseThrow();h.assertTrue(i.isMastered(b) && (!i.canBeToggled(b) || i.isToggled()),"Inactive/unmastered native support: "+id);}
        h.assertTrue(storage.getSkill(WildHunt.GODLY.get()).isPresent() && storage.getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).orElseThrow().isToggled(),"Actual Godly auxiliary package absent");
        h.assertTrue(GodlyPassives.ownsGodly(b) && dev.wildhunt.myths.MythsTiers.isGodly(EschatonContent.DEMIURGE.get()) && skill(b).is(TensuraSkillTags.ULTIMATE_SKILLS),"Godly/Ultimate compatibility broken");
        e.setMagicule(123);b.setHealth(800_000);b.initializeOrigin();h.assertTrue(e.getMagicule()==123 && b.getHealth()==800_000,"Initialization created free recovery");done(h,b);
    }
    @GameTest(template="empty",batch="origin_registration",timeoutTicks=100)
    public static void bossOnlyRaceAndGodlyNeverEnterReincarnationOrPlayerSlots(GameTestHelper h){
        var p=player(h);for(int n=0;n<3;n++){var menu=new ReincarnationMenu(0,p.getInventory(),p,false,n,0,0);h.assertTrue(!menu.getRacePool().contains(EschatonContent.RACE.get()) && !menu.getRandomRacePool().contains(EschatonContent.RACE.get()),"Boss lineage entered reincarnation");}
        h.assertTrue(!ReincarnationMenu.getSkillPool().contains(EschatonContent.DEMIURGE.get()) && !EschatonContent.DEMIURGE.get().canBeSlotted(EschatonContent.DEMIURGE.get().createDefaultInstance(),p,0),"Boss Godly became a rollable/player skill");h.succeed();
    }
    @GameTest(template="empty",batch="origin_registration",timeoutTicks=100)
    public static void millionHpIsEntityLocalAndKeepsRealExternalModifiers(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var z=target(h,p.position().add(24,0,0));z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);h.assertTrue(z.getMaxHealth()<=10_000,"Final boss raised the global HP limit");
        b.getAttribute(Attributes.MAX_HEALTH).addOrUpdateTransientModifier(new AttributeModifier(WildHunt.id("origin_test_wound"),-.25,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));h.assertTrue(b.getMaxHealth()==750_000,"Entity-local HP ignored a real debuff");b.setHealth(750_000);b.hurt(b.damageSources().genericKill(),100_000);h.assertTrue(b.getHealth()==650_000 && b.form()==1,"Damage was capped or a nonfatal wound advanced forms");done(h,b);
    }
    @GameTest(template="empty",batch="origin_phases",timeoutTicks=100)
    public static void fundedNativeInfiniteRegenerationHasPriorityOverAForm(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var i=learn(p,MythicKind.THOTH);i.setMastery(i.getMaxMastery());var e=TensuraStorages.getExistenceFrom(b);
        afterTracking(h,List.of(b),()->{MythicArts.hit((MythicSkill)i.getSkill(),i,p,b,20_000_000,0,TensuraDamageTypes.MAGIC_GENERIC);h.assertTrue(b.isAlive() && b.form()==1,"Funded native regeneration lost to a form transition");
            h.startSequence().thenWaitUntil(()->h.assertTrue(b.form()==1 && b.getHealth()==b.getMaxHealth() && e.getMagicule()<500_000_000,"Native regeneration has not spent real fuel and recovered HP")).thenExecute(()->{e.setMagicule(0);b.invulnerableTime=0;MythicArts.hit((MythicSkill)i.getSkill(),i,p,b,20_000_000,0,TensuraDamageTypes.MAGIC_GENERIC);full(h,b,2);done(h,b);});});
    }
    @GameTest(template="empty",batch="origin_phases",timeoutTicks=100)
    public static void exhaustedFatalHitsSpendExactlyTwoFormsThenGiveOneFinalDeath(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var i=learn(p,MythicKind.THOTH);i.setMastery(i.getMaxMastery());var e=TensuraStorages.getExistenceFrom(b);
        for(int form=2;form<=3;form++){e.setMagicule(0);e.setAura(5);b.invulnerableTime=0;MythicArts.hit((MythicSkill)i.getSkill(),i,p,b,20_000_000,0,TensuraDamageTypes.MAGIC_GENERIC);full(h,b,form);h.assertTrue(!HunterData.bossKills(p).contains("wildhunt:eschaton"),"An intermediate form gave victory credit");}
        e.setMagicule(0);b.invulnerableTime=0;MythicArts.hit((MythicSkill)i.getSkill(),i,p,b,20_000_000,0,TensuraDamageTypes.MAGIC_GENERIC);h.assertTrue(!b.isAlive() && b.form()==3 && HunterData.bossKills(p).contains("wildhunt:eschaton"),"Final death/credit was prevented or a fourth life appeared");done(h,b);
    }
    @GameTest(template="empty",batch="origin_phases",timeoutTicks=100)
    public static void spiritualFatalDamageAlsoRestoresAllFourPoolsOnce(GameTestHelper h){var p=player(h);var b=origin(h,p.position().add(12,0,0));var e=TensuraStorages.getExistenceFrom(b);e.setMagicule(0);var source=TensuraDamageTypes.getEntityDamageSource(h.getLevel(),TensuraDamageTypes.SOUL_SCATTER,p);h.assertTrue(TensuraDamageHelper.directSpiritualHurt(b,p,source,20_000_000,0),"Spiritual source failed");full(h,b,2);done(h,b);}
    @GameTest(template="empty",batch="origin_phases",timeoutTicks=100)
    public static void nativePainNullificationDoesNotHideConfirmedDamageFeedback(GameTestHelper h){var p=player(h);var b=origin(h,p.position().add(12,0,0));b.hurt(b.damageSources().playerAttack(p),100);h.assertTrue(!b.hasDamageFeedback(),"Nullified hit falsely showed damage");b.invulnerableTime=0;b.hurt(b.damageSources().genericKill(),100);h.assertTrue(b.hasDamageFeedback(),"Confirmed hit did not synchronize its flash");h.runAfterDelay(11,()->{h.assertTrue(!b.hasDamageFeedback(),"Flash never expired");done(h,b);});}
    @GameTest(template="empty",batch="origin_edges",timeoutTicks=100)
    public static void cleavePaysNativeAuraWaitsForItsWarningAndRespectsTheFan(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var front=target(h,b.position().add(10,0,0));var back=target(h,b.position().add(-10,0,0));
        afterTracking(h,List.of(b,front,back),()->{double ap=TensuraStorages.getExistenceFrom(b).getAura();cast(b,front,0);h.assertTrue(b.isCasting() && skill(b).getCoolDown(0)>0 && TensuraStorages.getExistenceFrom(b).getAura()<ap,"Cleave had no real cost/cooldown");age(h,b,23);h.assertTrue(front.getHealth()==1024 && back.getHealth()==1024,"Cleave landed before its warning");age(h,b,24);h.assertTrue(front.getHealth()<1024 && back.getHealth()==1024,"Cleave ignored its real fan");done(h,b);});
    }
    @GameTest(template="empty",batch="origin_loom",timeoutTicks=100)
    public static void rotatingLoomUsesItsActualRaysAndLeavesTheGapSafe(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var beam=target(h,b.position().add(20,0,0));var gap=target(h,b.position().add(14,0,14));
        afterTracking(h,List.of(b,beam,gap),()->{cast(b,beam,1);age(h,b,31);h.assertTrue(beam.getHealth()==1024,"Loom hit during the 32-tick warning");age(h,b,32);h.assertTrue(beam.getHealth()<1024 && gap.getHealth()==1024,"Rotating rays filled their safe gaps");done(h,b);});
    }
    @GameTest(template="empty",batch="origin_genesis",timeoutTicks=100)
    public static void eachOfSevenElementalWarningsLocksWhenItAppears(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var z=target(h,b.position().add(20,0,0));
        afterTracking(h,List.of(b,z),()->{cast(b,z,2);Vec3 first=EschatonPattern.meteor(b.plan,0);z.setPos(z.position().add(0,0,16));age(h,b,14);Vec3 second=EschatonPattern.meteor(b.plan,1);h.assertTrue(second.z>10 && EschatonPattern.meteor(b.plan,0).equals(first),"A locked warning slid with the player");age(h,b,31);h.assertTrue(z.getHealth()==1024,"A genesis star landed early");z.setPos(b.position().add(first).add(0,-z.getBbHeight()*.5,0));age(h,b,32);h.assertTrue(z.getHealth()<1024 && EschatonPattern.meteor(b.plan,1).equals(second),"A star missed its locked point or moved another warning");h.assertTrue(ThothElement.COUNT==7,"Spectrum changed unexpectedly");done(h,b);});
    }
    @GameTest(template="empty",batch="origin_wave",timeoutTicks=100)
    public static void nullTestamentWaitsThenHitsOnlyItsThreeSpatialCorridors(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var line=target(h,b.position().add(20,0,0));var gap=target(h,b.position().add(20,0,6));
        afterTracking(h,List.of(b,line,gap),()->{cast(b,line,3);age(h,b,37);h.assertTrue(line.getHealth()==1024,"Testament struck before its warning");age(h,b,38);h.assertTrue(line.getHealth()<1024 && gap.getHealth()==1024,"Testament did not respect its three corridors");EschatonRites.silence(b,p,skill(b));h.assertTrue(p.hasEffect(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.MAGIC_INTERFERENCE)),"Actual Godly interference source was rejected");done(h,b);});
    }
    @GameTest(template="empty",batch="origin_dive",timeoutTicks=100)
    public static void thronefallLocksItsLandingWarnsAndHitsOnce(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var z=target(h,b.position().add(20,0,0));var outside=target(h,b.position().add(20,0,16));
        afterTracking(h,List.of(b,z,outside),()->{cast(b,z,6);h.assertTrue(b.isCasting(),"Open dive failed");Vec3 landing=ApexArts.point(b.plan,"root").add(ApexArts.point(b.plan,"landing"));age(h,b,35);h.assertTrue(z.getHealth()==1024,"Dive landed before its warning");age(h,b,36);h.assertTrue(b.position().distanceToSqr(landing)<.01 && z.getHealth()<1024 && outside.getHealth()==1024,"Dive missed its locked point or hit beyond nine blocks");age(h,b,37);h.assertTrue(b.plan.getBoolean("struck"),"Dive did not record its one-time hit");done(h,b);});
    }
    @GameTest(template="empty",batch="origin_cleanup",timeoutTicks=100)
    public static void removingTheBossDiscardsAllOwnedSealsAndFinaleWarnings(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var z=target(h,b.position().add(20,0,0));
        afterTracking(h,List.of(b,z),()->{cast(b,z,7);h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getEntitiesOfClass(GenesisSealEntity.class,b.getBoundingBox().inflate(80),s->s.ownedBy(b)).size()==3 && !h.getLevel().getEntitiesOfClass(ArtVisualEntity.class,b.getBoundingBox().inflate(80),v->v.ownedBy(b)).isEmpty(),"Waiting for all owned seal/visual sections")).thenExecute(()->{var seals=h.getLevel().getEntitiesOfClass(GenesisSealEntity.class,b.getBoundingBox().inflate(80),s->s.ownedBy(b));var effects=h.getLevel().getEntitiesOfClass(ArtVisualEntity.class,b.getBoundingBox().inflate(80),v->v.ownedBy(b));b.discard();h.assertTrue(seals.stream().allMatch(Entity::isRemoved) && effects.stream().allMatch(Entity::isRemoved),"Boss removal left orphaned damaging warnings or seals");h.succeed();});});
    }
    @GameTest(template="empty",batch="origin_wave",timeoutTicks=100)
    public static void worldEaterFrontHasTwoRealGoldenGapsAndOneHitPerWave(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var aim=target(h,b.position().add(20,0,0));var danger=target(h,b.position().add(0,0,14));var safe=target(h,b.position().add(14,0,0));
        afterTracking(h,List.of(b,aim,danger,safe),()->{cast(b,aim,4);age(h,b,27);h.assertTrue(danger.getHealth()==1024,"Wave hit before warning");age(h,b,38);h.assertTrue(danger.getHealth()<1024 && safe.getHealth()==1024,"World-eater front ignored its gaps");h.assertTrue(b.plan.getList("hits_0",Tag.TAG_INT_ARRAY).contains(NbtUtils.createUUID(danger.getUUID())),"Wave lacks independent victim deduplication");done(h,b);});
    }
    @GameTest(template="empty",batch="origin_exchange",timeoutTicks=100)
    public static void paidExchangeTransfersFiniteAuraOnceAndCanBeInterruptedWithoutARefund(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var z=target(h,b.position().add(20,0,0));var e=TensuraStorages.getExistenceFrom(b);
        afterTracking(h,List.of(b,z),()->{e.setMagicule(100_000_000);double aura=e.getAura();cast(b,z,5);h.assertTrue(b.isCasting() && e.getAura()<aura && e.getMagicule()==100_000_000,"Exchange did not pay before its warning");age(h,b,47);h.assertTrue(e.getMagicule()==100_000_000,"Exchange paid out early");age(h,b,48);h.assertTrue(e.getMagicule()==108_000_000,"Exchange conversion is not exactly 20M AP to 8M MP");age(h,b,49);h.assertTrue(e.getMagicule()==108_000_000,"Exchange paid twice");EschatonRites.clear(h.getLevel(),b);skill(b).setCoolDown(0,5);cast(b,z,5);double paid=e.getAura();EschatonRites.clear(h.getLevel(),b);h.assertTrue(e.getAura()==paid && e.getMagicule()==108_000_000,"Interrupted conversion refunded/payed resources");done(h,b);});
    }
    @GameTest(template="empty",batch="origin_seals",timeoutTicks=100)
    public static void destructibleSealsDrainRealResourcesOpenRefugesAndInterruptTheApocalypse(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var z=target(h,b.position().add(20,0,0));
        afterTracking(h,List.of(b,z),()->{cast(b,z,7);h.assertTrue(b.isCasting(),"Apocalypse did not start");double mp=TensuraStorages.getExistenceFrom(b).getMagicule(),ap=TensuraStorages.getExistenceFrom(b).getAura();
            h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getEntitiesOfClass(GenesisSealEntity.class,b.getBoundingBox().inflate(80),s->s.ownedBy(b)).size()==3,"Waiting for three native seal sections to become visible")).thenExecute(()->{var seals=h.getLevel().getEntitiesOfClass(GenesisSealEntity.class,b.getBoundingBox().inflate(80),s->s.ownedBy(b));double currentMp=TensuraStorages.getExistenceFrom(b).getMagicule(),currentAp=TensuraStorages.getExistenceFrom(b).getAura();
                var first=seals.getFirst();h.assertTrue(first.getMaxHealth()==120_000 && EnergyHelper.getMaxEP(first)<=20,"Seal HP/minimal native EP is wrong: "+first.getMaxHealth()+"/"+EnergyHelper.getMaxEP(first));int slot=first.slot();first.hurt(first.damageSources().genericKill(),1_000_000);
                h.assertTrue(TensuraStorages.getExistenceFrom(b).getMagicule()==currentMp-50_000_000 && TensuraStorages.getExistenceFrom(b).getAura()==currentAp-25_000_000,"Seal did not drain its real native reserves");h.assertTrue(EschatonPattern.refuge(ApexArts.point(b.plan,"seal_"+slot),b.plan),"Broken seal opened no refuge");
                for(var seal:seals)if(!seal.isRemoved())seal.hurt(seal.damageSources().genericKill(),1_000_000);h.assertTrue(b.isStaggered() && b.plan.getBoolean("interrupted") && b.plan.getInt("broken")==7,"All three seals did not interrupt and stagger");double now=TensuraStorages.getExistenceFrom(b).getMagicule();b.brokenSeal(slot);h.assertTrue(TensuraStorages.getExistenceFrom(b).getMagicule()==now,"A seal drained twice");age(h,b,200);h.assertTrue(z.getHealth()==1024,"Interrupted apocalypse still dealt damage");done(h,b);
            });
        });
    }
    @GameTest(template="empty",batch="origin_apocalypse",timeoutTicks=100)
    public static void survivingApocalypseHitsOnlyItsWarnedSphereAndRespectsBrokenSealRefuges(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var danger=target(h,b.position().add(5,0,0));var safe=target(h,b.position().add(18,1,0));var outside=target(h,b.position().add(58,0,0));
        afterTracking(h,List.of(b,danger,safe,outside),()->{cast(b,danger,7);b.plan.putInt("broken",1);ApexArts.point(b.plan,"seal_0",new Vec3(18,1,0));age(h,b,199);h.assertTrue(danger.getHealth()==1024,"Apocalypse landed before ten seconds");age(h,b,200);h.assertTrue(danger.getHealth()<1024 && safe.getHealth()==1024 && outside.getHealth()==1024,"Apocalypse ignored a golden refuge or the 56m boundary");done(h,b);});
    }
    @GameTest(template="empty",batch="origin_persistence",timeoutTicks=100)
    public static void reloadPreservesSpentFormsAndPoolsAndCancelsUntelegraphedAttacks(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));b.hurt(b.damageSources().genericKill(),20_000_000);var z=target(h,b.position().add(20,0,0));cast(b,z,2);var e=TensuraStorages.getExistenceFrom(b);b.setHealth(700_000);e.setAura(12_345);e.setMagicule(67_890);e.setSpiritualHealth(123_000);var tag=new CompoundTag();b.save(tag);b.discard();var copy=(EschatonEntity)EntityType.loadEntityRecursive(tag,h.getLevel(),entity->entity);h.assertTrue(copy!=null,"Origin reload failed");h.getLevel().addFreshEntity(copy);
        h.assertTrue(copy.form()==2 && copy.getHealth()==700_000 && !copy.isCasting() && copy.art()==0 && TensuraStorages.getExistenceFrom(copy).getMagicule()==67_890,"Reload reset wounds, spent forms, resources or a cast");h.runAfterDelay(2,()->{h.assertTrue(TensuraStorages.getExistenceFrom(copy).getMagicule()<100_000,"Reload initialized free reserves");done(h,copy);});
    }
    @GameTest(template="empty",batch="origin_arena",timeoutTicks=100)
    public static void arenaMembershipProtectsBystandersAcrossAllMythsArtsAndIncomingHits(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));String id="origin_test_"+b.getUUID();var fight=new BossFightInstance(b.blockPosition(),h.getLevel().dimension(),96);TensuraStorages.getBossFightHolder(h.getLevel().getServer().overworld()).addBossFight(id,fight);b.onSpawned(h.getLevel(),id,fight);
        h.assertTrue(!b.eligible(p) && !HunterData.canHarm(b,p) && !b.hurt(b.damageSources().playerAttack(p),1000),"Native arena allowed an unjoined player to interfere");fight.getJoinedPlayers().add(p.getUUID());h.assertTrue(b.eligible(p) && HunterData.canHarm(b,p),"Joined participant was excluded");done(h,b);
    }
    @GameTest(template="empty",batch="origin_rewards",timeoutTicks=100)
    public static void crownAwardsNativeEpAndPermanentAttributesExactlyOnce(GameTestHelper h){
        var p=player(h);double before=EnergyHelper.getBaseMaxEP(p),hp=p.getAttribute(Attributes.MAX_HEALTH).getValue(),shp=p.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).getValue();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(EschatonContent.CROWN.get()));
        EschatonContent.CROWN.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(HunterData.of(p).getBoolean("mythbreakers_legacy") && Math.abs(EnergyHelper.getBaseMaxEP(p)-before-25_000_000)<1 && p.getMainHandItem().isEmpty(),"Crown did not grant and consume its one-time native EP reward");h.assertTrue(Math.abs(p.getAttribute(Attributes.MAX_HEALTH).getValue()-hp*1.2)<.01 && Math.abs(p.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).getValue()-shp*1.2)<.01,"Crown's native HP/SHP modifiers absent");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(EschatonContent.CROWN.get()));EschatonContent.CROWN.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(p.getMainHandItem().getCount()==1 && Math.abs(EnergyHelper.getBaseMaxEP(p)-before-25_000_000)<1,"A second crown stacked or was consumed");h.succeed();
    }
    @GameTest(template="empty",batch="origin_rewards",timeoutTicks=100)
    public static void blockedOrDuplicateSurvivalRitualConsumesNothing(GameTestHelper h){
        var p=player(h);var at=p.blockPosition().offset(12,-1,0);h.getLevel().setBlockAndUpdate(at,Blocks.BEACON.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(EschatonContent.INVITATION.get()));var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));h.getLevel().setBlockAndUpdate(at.above(4),Blocks.STONE.defaultBlockState());
        h.assertTrue(EschatonContent.INVITATION.get().useOn(context)==InteractionResult.FAIL && p.getMainHandItem().getCount()==1,"Blocked ritual consumed its invitation");h.getLevel().setBlockAndUpdate(at.above(4),Blocks.AIR.defaultBlockState());h.assertTrue(EschatonContent.INVITATION.get().useOn(context)==InteractionResult.CONSUME && p.getMainHandItem().isEmpty(),"Valid beacon ritual failed");
        h.runAfterDelay(2,()->{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(EschatonContent.INVITATION.get()));var result=EschatonContent.INVITATION.get().useOn(context);var found=new ArrayList<String>();for(var entity:h.getLevel().getAllEntities())if(entity instanceof EschatonEntity b)found.add("alive="+b.isAlive()+",distance="+b.distanceToSqr(Vec3.atBottomCenterOf(at.above()))+",removed="+b.isRemoved());h.assertTrue(result==InteractionResult.FAIL && p.getMainHandItem().getCount()==1,"Duplicate ritual: result="+result+", count="+p.getMainHandItem().getCount()+", bosses="+found);for(var b:h.getLevel().getEntitiesOfClass(EschatonEntity.class,new AABB(at).inflate(32)))b.discard();h.succeed();});
    }
    @GameTest(template="empty",batch="origin_ai",timeoutTicks=220)
    public static void realAiActivatesPaidDemiurgeAgainstALivingTarget(GameTestHelper h){
        var p=player(h);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);var b=origin(h,p.position().add(12,0,0));var z=target(h,b.position().add(24,0,0));z.setInvulnerable(true);b.setTarget(z);b.setNoAi(false);
        h.startSequence().thenWaitUntil(()->{boolean used=false;for(int mode=0;mode<8;mode++)used|=skill(b).onCoolDown(mode);h.assertTrue(used && TensuraStorages.getExistenceFrom(b).getAura()+TensuraStorages.getExistenceFrom(b).getMagicule()<1_000_000_000,"Live AI has not paid for a native Demiurge activation");}).thenExecute(()->done(h,b));
    }
    @GameTest(template="empty",batch="origin_admin",timeoutTicks=100)
    public static void exhaustedAiSkipsUnaffordableArtsWithoutChargingAndKeepsAuraOptions(GameTestHelper h){
        var p=player(h);var b=origin(h,p.position().add(12,0,0));var z=target(h,b.position().add(20,0,0));b.setTarget(z);var e=TensuraStorages.getExistenceFrom(b);e.setMagicule(0);e.setAura(1_000_000);skill(b).setCoolDown(100,0);h.assertTrue(b.chooseArt(skill(b),z)==-1 && e.getAura()==1_000_000 && e.getMagicule()==0,"Exhausted AI chose an unpaid art or charged selection");skill(b).setCoolDown(0,0);h.assertTrue(b.chooseArt(skill(b),z)==0,"Exhausted AI ignored its affordable aura cleave");done(h,b);
    }
    @GameTest(template="empty",batch="origin_admin",timeoutTicks=100)
    public static void operatorKillDoesNotSpendFormsAndWarningsKeepLongRangeNetworkBounds(GameTestHelper h){var p=player(h);var b=origin(h,p.position().add(12,0,0));b.kill();h.assertTrue(!b.isAlive() && b.form()==1,"Operator kill spent a form instead of removing the boss");var v=ArtVisualEntity.spawn(h.getLevel(),ArtVisualEntity.ESCHATON_GENESIS,p.position(),new Vec3(48,0,0),152);var plan=new CompoundTag();plan.putInt("warned",1);ApexArts.point(plan,"meteor_0",new Vec3(-90,0,90));v.barrage(plan);h.assertTrue(v.getType()==ArtVisualEntity.FINAL_TYPE.get() && v.getType().clientTrackingRange()==20 && v.shouldRenderAtSqrDistance(250*250) && v.getBoundingBoxForCulling().contains(p.position().add(-90,38,90)),"Distant final-boss warnings were culled or untracked");v.discard();b.discard();h.succeed();}
}
