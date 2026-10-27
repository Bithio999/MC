package dev.wildhunt;

import dev.wildhunt.myths.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class SovereignGameTests {
    /** Source/shape probe; real damage handling remains covered by the native combat fixtures. */
    private static final class Probe extends net.minecraft.world.entity.monster.Husk {
        final List<DamageSource> sources=new ArrayList<>();final List<Float> amounts=new ArrayList<>();
        Probe(GameTestHelper h,Vec3 p){super(EntityType.HUSK,h.getLevel());setPos(p);setNoAi(true);setNoGravity(true);h.getLevel().addFreshEntity(this);}
        @Override public boolean hurt(DamageSource source,float amount){sources.add(source);amounts.add(amount);return true;}
    }
    private static void recent(LivingEntity actor,LivingEntity victim,MythicKind kind,float damage){
        var source=WildHunt.MYTHIC.get(kind).get().createSource(WildHunt.MYTHIC.get(kind).get().createDefaultInstance(),actor,TensuraDamageTypes.EARTH_ELEMENTAL,0);
        LughArts.observeDamage(SkillAPI.getSkillsFrom(victim),victim,source,Changeable.of(damage));
    }
    private static void anchor(ManasSkillInstance i,Vec3 p,long now){
        var t=new CompoundTag();ApexArts.point(t,"p",p);t.putLong("until",now+400);t.putLong("armed",now+12);var list=i.getOrCreateTag().getList("anchors",Tag.TAG_COMPOUND);list.add(t);i.getOrCreateTag().put("anchors",list);
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void newUltimateRecipesConsumeIngredientsAndPreservePresets(GameTestHelper h){
        h.assertTrue(MythicEvolution.ultimates().size()==5,"Five Ultimate recipes were not registered");
        for(var kind:List.of(MythicKind.ANANSI,MythicKind.AEGIR)){
            var p=player(h);var storage=SkillAPI.getSkillsFrom(p);var recipe=MythicEvolution.ingredients(kind);
            for(var s:recipe){HunterData.learnReward(p,s);var i=storage.getSkill(s).orElseThrow();i.setMastery(i.getMaxMastery());i.setCoolDown(17,0);}
            var abilities=TensuraStorages.getAbilityFrom(p);abilities.setAbilitySlot(8,2,recipe.getFirst(),1);
            h.assertTrue(MythicEvolution.ready(p,kind),"New recipe not ready: "+kind);MythicEvolution.synthesize(p,kind.id());
            var result=storage.getSkill(WildHunt.MYTHIC.get(kind).get()).orElseThrow();
            h.assertTrue(result.getCoolDown(2)==17 && result.getModes()==3 && recipe.stream().noneMatch(s->storage.getSkill(s).isPresent()) && abilities.getAbilitySlot(8,2).getSkill()==result.getSkill(),"New synthesis duplicated ingredients or lost native state: "+kind);
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void lughAscensionUsesThreadweaverAndExposesAllMenuState(GameTestHelper h){
        var p=player(h);var ultimate=learn(p,MythicKind.AEGIR);ultimate.setMastery(ultimate.getMaxMastery());String source=ultimate.getSkillId().toString();
        MythsProgression.ascend(p,source,"lugh");h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(WildHunt.MYTHIC.get(MythicKind.LUGH).get()).isEmpty(),"Lugh ignored its extra ingredient");
        var thread=learn(p,MythicKind.THREADWEAVER);thread.setMastery(thread.getMaxMastery());thread.setCoolDown(23,1);var abilities=TensuraStorages.getAbilityFrom(p);abilities.setAbilitySlot(7,1,thread.getSkill(),1);
        MythsProgression.ascend(p,source,"lugh");var result=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.MYTHIC.get(MythicKind.LUGH).get()).orElseThrow();
        h.assertTrue(result.is(MythsTiers.GODLY) && result.is(io.github.manasmods.tensura.data.TensuraSkillTags.ULTIMATE_SKILLS) && GodlyPassives.ownsGodly(p) && result.getModes()==4 && result.getCoolDown(3)==23,"Lugh lost tier, modes or cooldown inheritance");
        var snapshot=MythsNetwork.snapshot(p,"");h.assertTrue(snapshot.getBoolean("has_lugh") && snapshot.getList("recipes",Tag.TAG_COMPOUND).size()==5 && abilities.getAbilitySlot(7,1).getSkill()==result.getSkill(),"Native menu omitted new progression state");
        h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(thread.getSkill()).isEmpty() && SkillAPI.getSkillsFrom(p).getSkill(ultimate.getSkill()).isEmpty(),"Ascension retained its consumed ingredients");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void lughStartsUsableAndArchiveCyclesFreelyWithoutRefundOrMastery(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.LUGH);var s=(MythicSkill)i.getSkill();var energy=TensuraStorages.getExistenceFrom(p);var archive=LughArts.archive(i);
        h.assertTrue(archive.size()==3 && archive.getCompound(0).getInt("form")==0 && archive.getCompound(1).getInt("form")==1 && archive.getCompound(2).getInt("form")==2,"Lugh did not start with three usable forms");
        i.setCoolDown(9,1);double mp=energy.getMagicule(),mastery=i.getMastery();p.setShiftKeyDown(true);cast(p,i,1);p.setShiftKeyDown(false);
        h.assertTrue(i.getOrCreateTag().getInt("selected_art")==1 && i.getCoolDown(1)==9 && energy.getMagicule()==mp && i.getMastery()==mastery,"Archive selection spent/refunded resources, reset cooldown or granted mastery");
        var copy=ManasSkillInstance.fromNBT(i.toNBT());h.assertTrue(LughArts.selectedName(copy).equals("Heavenly Thread") && LughArts.archive(copy).size()==3,"Archive failed native save/load");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void lughBanksRealNativeDamageAndDoesNotDuplicateArchivedSources(GameTestHelper h){
        var p=LughPassiveGameTests.observer(h,player(h).position());var actor=target(h,p.position().add(8,0,0));var victim=target(h,p.position().add(10,0,0));var i=LughPassiveGameTests.enable(p);var sourceSkill=learn(actor,MythicKind.RUNE_SMITH);
        afterTracking(h,List.of(p,actor,victim),()->{
            double mp=TensuraStorages.getExistenceFrom(p).getMagicule();victim.hurt(((MythicSkill)sourceSkill.getSkill()).createSource(sourceSkill,actor,TensuraDamageTypes.EARTH_ELEMENTAL,0),100);
            var copied=LughArts.library(i).getCompound(3);h.assertTrue(copied.getString("source").equals(sourceSkill.getSkillId().toString()) && copied.getString("type").equals("tensura:earth_elemental") && copied.getFloat("damage")==3500 && TensuraStorages.getExistenceFrom(p).getMagicule()==mp,"Automatic bank lost provenance, bounded power or charged an extra scan cost");
            victim.invulnerableTime=0;victim.hurt(((MythicSkill)sourceSkill.getSkill()).createSource(sourceSkill,actor,TensuraDamageTypes.EARTH_ELEMENTAL,0),100);h.assertTrue(LughArts.library(i).size()==4 && LughArts.archive(i).getCompound(0).getString("name").equals("Sovereign Edge"),"Repeated observation duplicated a record or replaced a binding");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void skillBankAndInvalidGodlyCountermandAreFree(GameTestHelper h){
        var p=player(h);var actor=target(h,p.position().add(8,0,0));var victim=target(h,p.position().add(10,0,0));var i=learn(p,MythicKind.LUGH);
        afterTracking(h,List.of(actor,victim),()->{
            double mp=TensuraStorages.getExistenceFrom(p).getMagicule();cast(p,i,0);h.assertTrue(i.getCoolDown(0)==0 && TensuraStorages.getExistenceFrom(p).getMagicule()==mp,"Missing observation consumed a cast");
            recent(actor,victim,MythicKind.THOTH,5000);cast(p,i,2);h.assertTrue(i.getCoolDown(2)==0 && TensuraStorages.getExistenceFrom(p).getMagicule()==mp && !HunterData.of(actor).contains("lugh_seal"),"Countermand sealed a Godly origin or charged a failed cast");
            HunterData.of(actor).getCompound("last_technique").putLong("time",h.getLevel().getGameTime()-601);cast(p,i,0);h.assertTrue(i.getCoolDown(0)==0 && TensuraStorages.getExistenceFrom(p).getMagicule()==mp,"Stale observation was accepted");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void reconstructedFormsHaveDifferentGeometryAndBoundedSafeSources(GameTestHelper h){
        var p=player(h);var ahead=new Probe(h,p.position().add(8,0,0));var offAxis=new Probe(h,p.position().add(8,0,5));var i=learn(p,MythicKind.LUGH);
        afterTracking(h,List.of(ahead,offAxis),()->{
            cast(p,i,1);h.assertTrue(ahead.sources.size()==1 && offAxis.sources.size()==1,"Blade did not sweep a forward fan");
            i.setCoolDown(0,1);i.getOrCreateTag().putInt("selected_art",1);cast(p,i,1);h.assertTrue(ahead.sources.size()==2 && offAxis.sources.size()==1,"Lance did not restrict hits to its aim line");
            i.setCoolDown(0,1);i.getOrCreateTag().putInt("selected_art",2);cast(p,i,1);h.assertTrue(ahead.sources.size()==3 && offAxis.sources.size()==2,"Crown did not strike the cursor area");
            var record=LughArts.archive(i).getCompound(2);record.putString("type","tensura:disintegration");record.putFloat("damage",Float.MAX_VALUE);i.setCoolDown(0,1);cast(p,i,1);
            h.assertTrue(ahead.sources.getLast().is(TensuraDamageTypes.MAGIC_GENERIC) && ahead.amounts.getLast()<=9000 && ((TensuraDamageSource)ahead.sources.getLast()).tensura$getAbilityInstance()==i,"Reconstruction inherited a special bypass source, unbounded damage or the original skill authority");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void countermandSealsOneTechniquePreservesOwnershipAndExpires(GameTestHelper h){
        var p=player(h);var actor=target(h,p.position().add(8,0,0));var victim=target(h,p.position().add(10,0,0));var i=learn(p,MythicKind.LUGH);var sealed=learn(actor,MythicKind.RUNE_SMITH);var other=learn(actor,MythicKind.MIRRORBOUND);var godly=learn(actor,MythicKind.THOTH);sealed.setCoolDown(4,1);
        afterTracking(h,List.of(actor,victim),()->{
            recent(actor,victim,MythicKind.RUNE_SMITH,300);var saved=sealed.toNBT();cast(p,i,2);
            h.assertTrue(!sealed.getSkill().canActivateSkill(sealed,actor,0) && other.getSkill().canActivateSkill(other,actor,0) && godly.getSkill().canActivateSkill(godly,actor,0),"Countermand was global, ignored the exact skill or suppressed Godly casting");
            h.assertTrue(!SkillAPI.getSkillsFrom(actor).startHoldSkill(sealed,0,0) && sealed.toNBT().equals(saved),"Native activation accepted the seal or changed learned skill state");
            HunterData.of(actor).getCompound("lugh_seal").putLong("until",h.getLevel().getGameTime());LughArts.tickSeal(actor);
            h.assertTrue(sealed.getSkill().canActivateSkill(sealed,actor,0) && !HunterData.of(actor).contains("lugh_seal"),"Expired seal retained its restriction");
            i.setCoolDown(0,2);cast(p,i,2);SkillAPI.getSkillsFrom(p).forgetSkill(i);h.assertTrue(!LughArts.sealed(actor,sealed.getSkill()),"Forgetting Lugh left another player's skill sealed");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void grandSynthesisSnapshotsThreeFormsAndKeepsSixteenTickSpacing(GameTestHelper h){
        var p=player(h);var target=new Probe(h,p.position().add(8,0,0));var i=learn(p,MythicKind.LUGH);
        afterTracking(h,List.of(target),()->{
            cast(p,i,3);LughArts.archive(i).getCompound(0).putString("type","tensura:fire_elemental");
            h.startSequence().thenIdle(19).thenExecute(()->{tick(p,i);h.assertTrue(target.sources.isEmpty(),"Synthesis struck before its warning");})
                .thenIdle(1).thenExecute(()->{tick(p,i);h.assertTrue(target.sources.size()==1 && target.sources.getFirst().is(TensuraDamageTypes.AURA_SLASH),"Synthesis did not use its frozen first form");})
                .thenIdle(16).thenExecute(()->{tick(p,i);h.assertTrue(target.sources.size()==2 && target.sources.getLast().is(TensuraDamageTypes.SPACE_ELEMENTAL),"Second synthesis art had the wrong source or timing");})
                .thenIdle(16).thenExecute(()->{tick(p,i);h.assertTrue(target.sources.size()==3 && target.sources.getLast().is(TensuraDamageTypes.EARTH_ELEMENTAL) && !i.getOrCreateTag().contains("synthesis_at"),"Final synthesis art or cleanup failed");h.succeed();});
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void lughLifecycleStopsPendingAttacksButKeepsLearnedArchive(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.LUGH);cast(p,i,3);String learned=LughArts.selectedName(i);MythicArts.clearTransient(p);
        h.assertTrue(!i.getOrCreateTag().contains("synthesis_at") && !i.getOrCreateTag().hasUUID("synthesis_visual") && LughArts.selectedName(i).equals(learned),"Lifecycle retained an attack or erased the archive");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void anansiNetworkArmsCutsOncePerSecondAndLimitsAnchors(GameTestHelper h){
        var p=player(h);var target=new Probe(h,p.position().add(10,0,0));var i=learn(p,MythicKind.ANANSI);
        afterTracking(h,List.of(target),()->{
            // Start the real 12-tick arming window after entity-index promotion.
            long now=h.getLevel().getGameTime();anchor(i,p.position().add(6,0,-4),now);anchor(i,p.position().add(14,0,4),now);
            tick(p,i);h.assertTrue(target.sources.isEmpty(),"Unarmed threads dealt damage");for(var value:i.getOrCreateTag().getList("anchors",Tag.TAG_COMPOUND))((CompoundTag)value).putLong("armed",0);
            tick(p,i);tick(p,i);h.assertTrue(target.sources.size()==1 && target.sources.getFirst().is(TensuraDamageTypes.SPACE_ELEMENTAL),"Thread did not cut or applied repeated same-tick damage");
            for(int n=0;n<4;n++){i.setCoolDown(0,0);cast(p,i,0);}h.assertTrue(i.getOrCreateTag().getList("anchors",Tag.TAG_COMPOUND).size()==3,"Network exceeded its three-anchor bound");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void puppetPassageTradesAnAnchorForTheCastersPositionAndMomentum(GameTestHelper h){
        var fixture=player(h);var p=target(h,fixture.position());p.setYRot(-90);p.setXRot(0);io.github.manasmods.tensura.util.EnergyHelper.setBaseMaxEP(p,20_000_000);TensuraStorages.getExistenceFrom(p).setAura(1_000_000);var i=learn(p,MythicKind.ANANSI);var destination=p.position().add(12,0,0);anchor(i,destination,h.getLevel().getGameTime());p.setDeltaMovement(.3,.2,-.1);Vec3 before=p.position(),velocity=p.getDeltaMovement();cast(p,i,1);
        h.assertTrue(p.position().distanceTo(destination)<.01 && p.getDeltaMovement().equals(velocity) && ApexArts.point(i.getOrCreateTag().getList("anchors",Tag.TAG_COMPOUND).getCompound(0),"p").distanceTo(before)<.01,"Puppet Passage position="+p.position()+" destination="+destination+" velocity="+p.getDeltaMovement()+" node="+i.getOrCreateTag());
        i.setCoolDown(0,1);h.getLevel().setBlockAndUpdate(net.minecraft.core.BlockPos.containing(before),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());p.setYRot(90);double aura=TensuraStorages.getExistenceFrom(p).getAura();cast(p,i,1);
        h.assertTrue(p.position().distanceTo(destination)<.01 && TensuraStorages.getExistenceFrom(p).getAura()==aura,"Blocked passage teleported or charged resources");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void checkmateUsesTriangleInteriorThenEarthAndConsumesNetwork(GameTestHelper h){
        var p=player(h);var inside=new Probe(h,p.position().add(10,0,0));var outside=new Probe(h,p.position().add(10,0,10));var i=learn(p,MythicKind.ANANSI);long now=h.getLevel().getGameTime();
        anchor(i,p.position().add(5,0,-5),now);anchor(i,p.position().add(15,0,-5),now);anchor(i,p.position().add(10,0,5),now);
        afterTracking(h,List.of(inside,outside),()->{cast(p,i,2);tick(p,i);h.assertTrue(inside.sources.isEmpty() && i.getOrCreateTag().getList("anchors",Tag.TAG_COMPOUND).isEmpty(),"Checkmate skipped its warning or retained anchors");
            i.getOrCreateTag().putLong("loom_at",h.getLevel().getGameTime());tick(p,i);h.assertTrue(inside.sources.size()==1 && inside.sources.getFirst().is(TensuraDamageTypes.SPACE_ELEMENTAL) && outside.sources.isEmpty(),"Checkmate lost its triangular interior");
            i.getOrCreateTag().putLong("loom_at",h.getLevel().getGameTime());tick(p,i);h.assertTrue(inside.sources.size()==2 && inside.sources.getLast().is(TensuraDamageTypes.EARTH_ELEMENTAL) && !i.getOrCreateTag().contains("loom_at"),"Checkmate's second native damage type or cleanup failed");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void aegirRefractsThreeRaysWithoutTripleHittingTheSameTarget(GameTestHelper h){
        var p=player(h);var target=new Probe(h,p.position().add(24,0,0));var i=learn(p,MythicKind.AEGIR);
        afterTracking(h,List.of(target),()->{cast(p,i,1);cast(p,i,0);h.assertTrue(target.sources.size()==1 && target.sources.getFirst().is(TensuraDamageTypes.WATER_ELEMENTAL) && target.amounts.getFirst()>=2200,"Refraction lost water damage, empowered damage or per-cast target deduplication");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void mirrorseaRedirectsIncomingProjectilesAndPreservesAlliedShots(GameTestHelper h){
        var p=player(h);var enemy=target(h,p.position().add(20,0,0));var i=learn(p,MythicKind.AEGIR);ApexArts.point(i.getOrCreateTag(),"mirrorsea",p.position().add(8,0,0));i.getOrCreateTag().putLong("mirrorsea_until",h.getLevel().getGameTime()+200);
        var hostile=new net.minecraft.world.entity.projectile.Snowball(h.getLevel(),enemy);hostile.setPos(p.position().add(15,1,0));hostile.setNoGravity(true);h.getLevel().addFreshEntity(hostile);
        var own=new net.minecraft.world.entity.projectile.Snowball(h.getLevel(),p);own.setPos(p.position().add(10,1,0));own.setNoGravity(true);h.getLevel().addFreshEntity(own);
        h.runAfterDelay(5,()->h.startSequence().thenWaitUntil(()->h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Snowball.class,hostile.getBoundingBox().inflate(.1)).contains(hostile) && h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Snowball.class,own.getBoundingBox().inflate(.1)).contains(own),"Projectiles not tracked yet")).thenExecute(()->{
            hostile.setDeltaMovement(-1,0,0);own.setDeltaMovement(1,0,0);tick(p,i);h.assertTrue(hostile.getOwner()==p && hostile.getDeltaMovement().x>0 && own.getOwner()==p && own.getDeltaMovement().x==1,"Mirrorsea lost projectile attribution or changed a friendly shot");h.succeed();}));
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void sovereignBreakerAdvancesAsAnAnnulusAndHitsEachVictimOnlyOnce(GameTestHelper h){
        var p=player(h);var near=new Probe(h,p.position().add(8,0,0));var far=new Probe(h,p.position().add(16,0,0));var i=learn(p,MythicKind.AEGIR);
        afterTracking(h,List.of(near,far),()->{cast(p,i,2);tick(p,i);h.assertTrue(near.sources.isEmpty() && far.sources.isEmpty(),"Breaker hit the entire disk immediately");
            i.getOrCreateTag().putLong("breaker_start",h.getLevel().getGameTime()-18);tick(p,i);tick(p,i);h.assertTrue(near.sources.size()==1 && far.sources.isEmpty(),"Breaker did not advance or damaged its inner disk twice");
            i.getOrCreateTag().putLong("breaker_start",h.getLevel().getGameTime()-36);tick(p,i);h.assertTrue(near.sources.size()==1 && far.sources.size()==1,"Outer breaker failed its once-per-victim rule");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void perennialSeedsOnlyMarkAlliesAndCleanseOrdinaryAfflictions(GameTestHelper h){
        var p=player(h);var enemy=target(h,p.position().add(3,0,0));var i=learn(p,MythicKind.DEMETER);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,200));
        afterTracking(h,List.of(enemy),()->{cast(p,i,1);h.assertTrue(HunterData.of(p).contains("perennial_seed") && !HunterData.of(enemy).contains("perennial_seed") && !p.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"Seed healed enemies or failed its ordinary cleanse");
            SkillAPI.getSkillsFrom(p).forgetSkill(i);h.assertTrue(!HunterData.of(p).contains("perennial_seed"),"Forgetting Demeter retained its rescue seed");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void allUltimatesUseNativePartialPenetrationAndOrdinaryBoostsDoNotStack(GameTestHelper h){
        var p=player(h);var target=new Probe(h,p.position().add(8,0,0));for(var kind:MythicEvolution.ultimates()){
            var i=learn(p,kind);MythicArts.hit((MythicSkill)i.getSkill(),i,p,target,1000,0,TensuraDamageTypes.SPACE_ELEMENTAL);
            h.assertTrue(((TensuraDamageSource)target.sources.getLast()).tensura$getResistanceBypassLevel()==1,"Ultimate did not retain native partial penetration: "+kind);
        }
        for(var kind:List.of(MythicKind.JANUS,MythicKind.ANANSI,MythicKind.THOTH)){var i=learn(p,kind);i.getSkill().onToggleOn(i,p);}
        h.assertTrue(Math.abs(p.getAttributeValue(TensuraAttributes.SPACE_BOOST)-3.5)<.001,"Two ordinary Ultimate boosts stacked or Godly boost was lost");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void vritraStormRoutesPlasmaAlongASecondClippedAngle(GameTestHelper h){
        var p=player(h);var target=new Probe(h,p.position().add(32,2.5,6));var i=learn(p,MythicKind.VRITRA);
        ApexArts.point(i.getOrCreateTag(),"thunder",p.position().add(0,0,12));i.getOrCreateTag().putLong("thunder_until",h.getLevel().getGameTime()+160);
        h.getLevel().setBlockAndUpdate(net.minecraft.core.BlockPos.containing(p.getEyePosition().add(64,0,0)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        afterTracking(h,List.of(target),()->{cast(p,i,0);h.assertTrue(target.sources.size()==1 && target.sources.getFirst().is(TensuraDamageTypes.LIGHTNING_ELEMENTAL),"Storm routing failed when the aimed endpoint was a solid block");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void lifecycleFindsSeedSealAndVisualRecipientsAcrossDimensions(GameTestHelper h){
        var p=player(h);var other=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);var at=new net.minecraft.core.BlockPos(9,180,9);other.getChunk(at);other.setChunkForced(0,0,true);
        var actor=new net.minecraft.world.entity.monster.Husk(EntityType.HUSK,other);actor.setPos(9.5,180,9.5);actor.setNoGravity(true);actor.setNoAi(true);other.addFreshEntity(actor);
        var lugh=learn(p,MythicKind.LUGH);var demeter=learn(p,MythicKind.DEMETER);var targets=new ListTag();targets.add(NbtUtils.createUUID(actor.getUUID()));lugh.getOrCreateTag().put("sealed_targets",targets);demeter.getOrCreateTag().put("seed_recipients",targets.copy());
        var seal=new CompoundTag();seal.putUUID("owner",p.getUUID());seal.putString("skill","wildhunt:rune_smith");seal.putLong("until",other.getGameTime()+120);HunterData.of(actor).put("lugh_seal",seal);
        var seed=new CompoundTag();seed.putUUID("owner",p.getUUID());seed.putLong("until",other.getGameTime()+240);HunterData.of(actor).put("perennial_seed",seed);
        var visual=ArtVisualEntity.spawn(other,ArtVisualEntity.GRAND_SYNTHESIS,actor.position(),new Vec3(10,0,0),74);lugh.getOrCreateTag().putUUID("synthesis_visual",visual.getUUID());
        h.runAfterDelay(5,()->h.startSequence().thenWaitUntil(()->h.assertTrue(other.getEntity(actor.getUUID())==actor && other.getEntity(visual.getUUID())==visual,"Other-dimension recipients are not tracked yet")).thenExecute(()->{
            MythicArts.clear(lugh,p);MythicArts.clear(demeter,p);
            h.assertTrue(!HunterData.of(actor).contains("lugh_seal") && !HunterData.of(actor).contains("perennial_seed") && visual.isRemoved(),"Owner lifecycle left its effects behind in the previous dimension");actor.discard();h.succeed();
        }));
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void sovereignEffectsClearTheirPendingDamageAndRemainInertVisuals(GameTestHelper h){
        var p=player(h);for(var kind:List.of(MythicKind.ANANSI,MythicKind.AEGIR)){
            var i=learn(p,kind);if(kind==MythicKind.ANANSI){anchor(i,p.position().add(6,0,-3),h.getLevel().getGameTime());anchor(i,p.position().add(12,0,3),h.getLevel().getGameTime());cast(p,i,2);}else {cast(p,i,1);cast(p,i,2);}
            MythicArts.clear(i,p);h.assertTrue(!i.getOrCreateTag().contains("loom_at") && !i.getOrCreateTag().contains("mirrorsea_until") && !i.getOrCreateTag().contains("breaker_until"),"Lifecycle left pending sovereign damage");
        }
        var visuals=new ArrayList<ArtVisualEntity>();
        for(int kind=ArtVisualEntity.FATE_ANCHOR;kind<=ArtVisualEntity.RECONSTRUCTED_CROWN;kind++){var visual=ArtVisualEntity.spawn(h.getLevel(),kind,p.position(),new Vec3(3,0,0),3);visuals.add(visual);h.assertTrue(!visual.isPickable() && !visual.isPushable(),"New geometry was a collidable combat entity");}
        h.runAfterDelay(8,()->{h.assertTrue(visuals.stream().allMatch(Entity::isRemoved),"New geometry ignored its native ticking lifetime");h.succeed();});
    }
}
