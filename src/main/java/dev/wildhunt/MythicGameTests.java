package dev.wildhunt;

import dev.wildhunt.myths.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class MythicGameTests {
    private static final java.util.concurrent.atomic.AtomicInteger ARENAS=new java.util.concurrent.atomic.AtomicInteger();
    private static ServerPlayer player(GameTestHelper h){
        // Follow the runner's fresh test region: fixed world coordinates can reload
        // mobs saved by a previous GameTest run and contaminate target-cap tests.
        var origin=h.absolutePos(BlockPos.ZERO);
        Vec3 p=new Vec3(origin.getX()+7000+ARENAS.getAndIncrement()*256,160,origin.getZ()+7000);
        for(int x=-1;x<=2;x++)for(int z=-1;z<=1;z++){var pos=BlockPos.containing(p).offset(x*16,0,z*16);h.getLevel().getChunk(pos);h.getLevel().setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);}
        var e=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MythicTest"));e.setPos(p);e.setYRot(-90);e.setXRot(0);e.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        RaceAPI.getRaceFrom(e).setRace(WildHunt.HUNTERS.get(2).get(),false);EnergyHelper.setBaseMaxEP(e,20_000_000);var energy=TensuraStorages.getExistenceFrom(e);energy.setTrueHero(true);energy.setAura(1_000_000);energy.setMagicule(1_000_000);return e;
    }
    private static net.minecraft.world.entity.monster.Husk target(GameTestHelper h,Vec3 p){var z=new net.minecraft.world.entity.monster.Husk(net.minecraft.world.entity.EntityType.HUSK,h.getLevel());z.setPos(p);z.setNoAi(true);z.setNoGravity(true);z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);z.setHealth(1024);h.getLevel().addFreshEntity(z);return z;}
    private static ManasSkillInstance learn(LivingEntity p,MythicKind k){var s=WildHunt.MYTHIC.get(k).get();HunterData.learnReward(p,s);return SkillAPI.getSkillsFrom(p).getSkill(s).orElseThrow();}
    private static void afterTracking(GameTestHelper h,List<? extends LivingEntity> targets,Runnable checks){
        h.runAfterDelay(5,()->h.startSequence().thenWaitUntil(()->{
            for(var target:targets)h.assertTrue(h.getLevel().getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(.1)).contains(target),"Fixture target is not accessible in the entity manager yet");
        }).thenExecute(checks));
    }
    private static void cast(LivingEntity p,ManasSkillInstance i,int mode){i.getSkill().onPressed(i,p,0,mode);}
    private static void tick(LivingEntity p,ManasSkillInstance i){((MythicSkill)i.getSkill()).tickActive(i,p);}
    private static void due(ManasSkillInstance i,long time){for(var value:i.getOrCreateTag().getList("bursts",Tag.TAG_COMPOUND))((CompoundTag)value).putLong("at",time);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void eightNewKitsHaveNativeTiersNamesModesAndUniqueRolls(GameTestHelper h){
        var pool=io.github.manasmods.tensura.menu.ReincarnationMenu.getSkillPool();var names=new HashSet<String>();
        for(var kind:MythicKind.values()){
            var s=WildHunt.MYTHIC.get(kind).get();h.assertTrue(names.add(s.getName().getString()) && s.getName().getString().equals(kind.title),"Untranslated or duplicated name: "+kind);
            var i=s.createDefaultInstance();h.assertTrue(i.getModes()==kind.modes.length,"Wrong native modes: "+kind);
            h.assertTrue(MythsTiers.isGodly(s)==kind.godly(),"Wrong Godly classification: "+kind);
            if(kind.unique())h.assertTrue(Collections.frequency(pool,s)==1 && i.is(io.github.manasmods.tensura.data.TensuraSkillTags.UNIQUE_SKILLS),"Unique roll/tag missing: "+kind);
            else h.assertTrue(!pool.contains(s) && i.is(io.github.manasmods.tensura.data.TensuraSkillTags.ULTIMATE_SKILLS),"Ultimate entered roll pool or lost native tag: "+kind);
            if(kind.godly())h.assertTrue(i.is(MythsTiers.GODLY),"Thoth is missing the Godly tag");
        }
        h.assertTrue(WildHunt.rollableUniques().size()==8,"Wrong total playable Unique count");
        for(var old:WildHunt.ULTIMATE.values())h.assertTrue(!pool.contains(old.get()) && !old.get().createDefaultInstance().is(io.github.manasmods.tensura.data.TensuraSkillTags.ULTIMATE_SKILLS),"Retired Ultimate remains playable-tagged");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void synthesisChecksBothIngredientsAndPreservesAllPresetsAndCooldowns(GameTestHelper h){
        var p=player(h);var storage=SkillAPI.getSkillsFrom(p);var recipe=MythicEvolution.ingredients(MythicKind.VRITRA);
        for(var s:recipe)HunterData.learnReward(p,s);var first=storage.getSkill(recipe.getFirst()).orElseThrow();first.setMastery(first.getMaxMastery());
        MythicEvolution.synthesize(p,"vritra");h.assertTrue(storage.getSkill(WildHunt.MYTHIC.get(MythicKind.VRITRA).get()).isEmpty(),"Unmastered second ingredient was accepted");
        var second=storage.getSkill(recipe.getLast()).orElseThrow();second.setMastery(second.getMaxMastery());second.setCoolDown(27,1);
        EnergyHelper.setBaseMaxEP(p,1_999_999);h.assertTrue(!MythicEvolution.ready(p,MythicKind.VRITRA),"Synthesis EP gate bypassed");EnergyHelper.setBaseMaxEP(p,20_000_000);
        var a=TensuraStorages.getAbilityFrom(p);a.setAbilitySlot(0,0,recipe.getFirst(),1);a.setAbilitySlot(8,2,recipe.getLast(),0);HunterData.learnReward(p,WildHunt.UNIQUE.get(Apex.UTH_DUNA).get());
        MythicEvolution.synthesize(p,"vritra");var result=storage.getSkill(WildHunt.MYTHIC.get(MythicKind.VRITRA).get()).orElseThrow();
        h.assertTrue(recipe.stream().allMatch(s->storage.getSkill(s).isEmpty()),"Synthesis duplicated ingredients");h.assertTrue(storage.getSkill(WildHunt.UNIQUE.get(Apex.UTH_DUNA).get()).isPresent(),"Synthesis removed an unrelated skill");
        h.assertTrue(a.getAbilitySlot(0,0).getSkill()==result.getSkill() && a.getAbilitySlot(8,2).getSkill()==result.getSkill(),"Synthesis lost a preset");
        for(int mode=0;mode<3;mode++)h.assertTrue(result.getCoolDown(mode)==27,"Synthesis reset a source cooldown");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void retiredUltimatesMigrateMasteryCooldownsAndCleanTheirBarriers(GameTestHelper h){
        var p=player(h);var storage=SkillAPI.getSkillsFrom(p);var old=WildHunt.ULTIMATE.get(Apex.JIN_DAHAAD).get();HunterData.learnReward(p,old);var i=storage.getSkill(old).orElseThrow();i.setMastery(i.getMaxMastery()*.6);i.setCoolDown(37,2);
        var id=WildHunt.id("ymir_mantle");p.getAttribute(TensuraAttributes.MULTILAYER_BARRIER).addOrReplacePermanentModifier(new AttributeModifier(id,350,AttributeModifier.Operation.ADD_VALUE));TensuraStorages.getAbilityFrom(p).setAbilitySlot(8,2,old,2);
        double aura=TensuraStorages.getExistenceFrom(p).getAura();old.onPressed(i,p,0,0);h.assertTrue(TensuraStorages.getExistenceFrom(p).getAura()==aura,"Retired skill can still cast");
        MythicEvolution.migrate(p);var result=storage.getSkill(WildHunt.MYTHIC.get(MythicKind.DEMETER).get()).orElseThrow();
        h.assertTrue(storage.getSkill(old).isEmpty() && Math.abs(result.getMastery()/result.getMaxMastery()-.6)<.001,"Migration lost mastery or retained retired skill");h.assertTrue(result.getCoolDown(0)==37 && TensuraStorages.getAbilityFrom(p).getAbilitySlot(8,2).getSkill()==result.getSkill(),"Migration lost cooldown or preset");h.assertTrue(p.getAttribute(TensuraAttributes.MULTILAYER_BARRIER).getModifier(id)==null,"Retired barrier modifier survived migration");MythicEvolution.migrate(p);h.assertTrue(storage.getLearnedSkills().stream().filter(v->v.getSkill()==result.getSkill()).count()==1,"Migration duplicated reward");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void threadLeashPullsOnlyOutsideItsRadiusAndSeverConsumesIt(GameTestHelper h){
        var p=player(h);var z=target(h,p.position().add(8,0,0));var i=learn(p,MythicKind.THREADWEAVER);afterTracking(h,List.of(z),()->{
            cast(p,i,0);h.assertTrue(i.getOrCreateTag().hasUUID("tether_target") && z.getHealth()<1024,"Needle failed to hit/tether");z.setDeltaMovement(Vec3.ZERO);tick(p,i);h.assertTrue(z.getDeltaMovement().lengthSqr()==0,"Leash pulls while inside its radius");
            z.setPos(z.position().add(0,0,5));tick(p,i);h.assertTrue(z.getDeltaMovement().z<0,"Leash did not pull toward its fixed anchor");z.invulnerableTime=0;float before=z.getHealth();cast(p,i,1);h.assertTrue(z.getHealth()<before && !i.getOrCreateTag().contains("tether_until"),"Sever did not consume the tether and damage");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void runeMineArmsTriggersOnceAndCommandDetonationHasNoPhantomCast(GameTestHelper h){
        var p=player(h);var z=target(h,p.position().add(9,0,0));var i=learn(p,MythicKind.RUNE_SMITH);afterTracking(h,List.of(z),()->{
            cast(p,i,0);float before=z.getHealth();tick(p,i);h.assertTrue(z.getHealth()==before,"Mine triggered before arming");i.getOrCreateTag().putLong("mine_armed",h.getLevel().getGameTime());tick(p,i);h.assertTrue(z.getHealth()<before && !i.getOrCreateTag().contains("mine_until"),"Mine did not trigger");
            z.invulnerableTime=0;float after=z.getHealth();tick(p,i);h.assertTrue(z.getHealth()==after,"Consumed mine triggered twice");double aura=TensuraStorages.getExistenceFrom(p).getAura();cast(p,i,1);h.assertTrue(i.getCoolDown(1)==0 && aura==TensuraStorages.getExistenceFrom(p).getAura(),"Command without a rune consumed resources");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void groveHealsOwnerAndAlliedPetButNotNeutralEntities(GameTestHelper h){
        var p=player(h);p.setYRot(0);p.setXRot(90);var wolf=new net.minecraft.world.entity.animal.Wolf(EntityType.WOLF,h.getLevel());wolf.setPos(p.position().add(2,0,0));wolf.setNoGravity(true);wolf.setNoAi(true);wolf.tame(p);h.getLevel().addFreshEntity(wolf);var stranger=target(h,p.position().add(3,0,0));var i=learn(p,MythicKind.VERDANT_KEEPER);
        afterTracking(h,List.of(wolf,stranger),()->{p.setHealth(p.getMaxHealth()*.5f);wolf.setHealth(wolf.getMaxHealth()*.5f);stranger.setHealth(512);double pet=wolf.getHealth();var energy=TensuraStorages.getExistenceFrom(p);energy.setSpiritualHealth(100);double shp=energy.getSpiritualHealth();cast(p,i,0);
            // Aim straight down from an airborne fixture, then put the stationary grove at the owner's feet.
            ApexArts.point(i.getOrCreateTag(),"grove",p.position());tick(p,i);
            h.assertTrue(p.getHealth()>p.getMaxHealth()*.5 && wolf.getHealth()>pet && energy.getSpiritualHealth()>shp,"Grove failed allied HP/SHP recovery");h.assertTrue(stranger.getHealth()==512,"Grove healed a neutral enemy");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void mirrorAegisTransfersProjectileOwnershipAndLeavesOutgoingFriendlyShots(GameTestHelper h){
        var p=player(h);var enemy=target(h,p.position().add(12,0,0));var i=learn(p,MythicKind.MIRRORBOUND);
        var hostile=new net.minecraft.world.entity.projectile.Snowball(h.getLevel(),enemy);hostile.setPos(p.position().add(2,1,0));hostile.setDeltaMovement(-1,0,0);h.getLevel().addFreshEntity(hostile);
        var own=new net.minecraft.world.entity.projectile.Snowball(h.getLevel(),p);own.setPos(p.position().add(1,1,0));own.setDeltaMovement(1,0,0);h.getLevel().addFreshEntity(own);
        h.runAfterDelay(5,()->{hostile.setPos(p.position().add(2,1,0));hostile.setDeltaMovement(-1,0,0);own.setPos(p.position().add(1,1,0));own.setDeltaMovement(1,0,0);cast(p,i,1);tick(p,i);h.assertTrue(hostile.getOwner()==p && hostile.getDeltaMovement().x>0,"Hostile projectile was not reflected with correct ownership");h.assertTrue(own.getOwner()==p && own.getDeltaMovement().x==1,"Mirror changed a friendly projectile");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void plasmaBoreLeavesDelayedFireAndCataclysmIsTelegraphed(GameTestHelper h){
        var p=player(h);var z=target(h,p.position().add(8,0,0));var i=learn(p,MythicKind.VRITRA);afterTracking(h,List.of(z),()->{
            cast(p,i,0);h.assertTrue(z.getHealth()<1024 && !i.getOrCreateTag().getList("bursts",Tag.TAG_COMPOUND).isEmpty(),"Plasma failed: health="+z.getHealth()+" cooldown="+i.getCoolDown(0)+" look="+p.getLookAngle()+" tag="+i.getOrCreateTag());float first=z.getHealth();z.invulnerableTime=0;tick(p,i);h.assertTrue(z.getHealth()==first,"Fuse exploded before warning elapsed");due(i,h.getLevel().getGameTime());tick(p,i);h.assertTrue(z.getHealth()<first,"Delayed fire did not explode");
            cast(p,i,2);h.assertTrue(i.getOrCreateTag().getList("bursts",Tag.TAG_COMPOUND).size()==7,"Cataclysm did not create a bounded seven-step front");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void winterHarvestConsumesOrchardAndFixedWarningsCanBeDodged(GameTestHelper h){
        var p=player(h);var z=target(h,p.position().add(8,0,0));var i=learn(p,MythicKind.DEMETER);afterTracking(h,List.of(z),()->{
            cast(p,i,0);cast(p,i,2);h.assertTrue(!i.getOrCreateTag().contains("orchard_until"),"Harvest did not consume orchard");var marks=i.getOrCreateTag().getList("bursts",Tag.TAG_COMPOUND);h.assertTrue(!marks.isEmpty(),"No crystal bloom warnings");float before=z.getHealth();tick(p,i);h.assertTrue(z.getHealth()==before,"Crystal bloom hit before its telegraph");z.setPos(z.position().add(0,0,15));due(i,h.getLevel().getGameTime());tick(p,i);h.assertTrue(z.getHealth()==before,"Crystal bloom followed an enemy away from its fixed warning");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void bulwarkExpiryRemovesOnlyItsOwnNativeBarrierModifiers(GameTestHelper h){
        var p=player(h);var foreign=WildHunt.id("foreign_ward_test");var a=p.getAttribute(TensuraAttributes.MULTILAYER_BARRIER);a.addOrReplacePermanentModifier(new AttributeModifier(foreign,91,AttributeModifier.Operation.ADD_VALUE));double baseline=a.getValue();MythicArts.lendBarrier(h.getLevel(),p,p,250,35,120);h.assertTrue(a.getValue()==baseline+250,"Bulwark does not use native HUD barrier");
        var lease=p.getPersistentData().getCompound("wildhunt").getList("crystal_wards",Tag.TAG_COMPOUND).getCompound(0);lease.putLong("until",h.getLevel().getGameTime());MythicArts.tickTarget(p);h.assertTrue(a.getValue()==baseline && a.getModifier(foreign)!=null,"Bulwark expired a foreign barrier");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void janusGatesPreserveMomentumAndRejectBlockedDestinations(GameTestHelper h){
        var fixture=player(h);var p=target(h,fixture.position());p.setYRot(-90);p.setXRot(0);EnergyHelper.setBaseMaxEP(p,20_000_000);TensuraStorages.getExistenceFrom(p).setAura(100_000);
        var i=learn(p,MythicKind.JANUS);cast(p,i,0);Vec3 exit=ApexArts.point(i.getOrCreateTag(),"gate_b");i.setCoolDown(0,0);var velocity=new Vec3(.3,.2,-.1);p.setDeltaMovement(velocity);cast(p,i,0);h.assertTrue(p.position().distanceTo(exit)<.001 && p.getDeltaMovement().equals(velocity),"Gate lost destination or momentum: position="+p.position()+" exit="+exit+" velocity="+p.getDeltaMovement()+" tag="+i.getOrCreateTag()+" cd="+i.getCoolDown(0)+" aura="+TensuraStorages.getExistenceFrom(p).getAura()+" safe="+MythicArts.safe(h.getLevel(),p,exit)+" cast="+MythicArts.canCast((MythicSkill)i.getSkill(),h.getLevel(),i,p,0));
        i.setCoolDown(0,0);Vec3 a=ApexArts.point(i.getOrCreateTag(),"gate_a");h.getLevel().setBlockAndUpdate(BlockPos.containing(a),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());double aura=TensuraStorages.getExistenceFrom(p).getAura();cast(p,i,0);h.assertTrue(p.position().distanceTo(exit)<.001 && aura==TensuraStorages.getExistenceFrom(p).getAura() && i.getCoolDown(0)==0,"Blocked gate consumed resources or teleported into a wall");h.getLevel().setBlockAndUpdate(BlockPos.containing(a),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void thothAscensionRequiresRuneSmithAndRetainsNativeUltimateCompatibility(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.JANUS);i.setMastery(i.getMaxMastery());String source=i.getSkillId().toString();MythsProgression.ascend(p,source,"thoth");h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).isEmpty(),"Thoth did not require Rune Smith");var rune=learn(p,MythicKind.RUNE_SMITH);rune.setMastery(rune.getMaxMastery());rune.setCoolDown(18,1);MythsProgression.ascend(p,source,"thoth");var result=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.MYTHIC.get(MythicKind.THOTH).get()).orElseThrow();
        h.assertTrue(result.is(MythsTiers.GODLY) && result.is(io.github.manasmods.tensura.data.TensuraSkillTags.ULTIMATE_SKILLS) && result.getCoolDown(0)==18,"Godly classification or source cooldown lost");h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(rune.getSkill()).isEmpty() && SkillAPI.getSkillsFrom(p).getSkill(i.getSkill()).isEmpty(),"Thoth duplicated its ingredients");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void manaCrucibleConvertsTwoAuraPerMagiculeAndCannotExceedTheCap(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.THOTH);var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10000);energy.setMagicule(EnergyHelper.getMaxMagicule(p)-3000);double before=energy.getMagicule();cast(p,i,2);h.assertTrue(energy.getAura()==2000 && energy.getMagicule()==EnergyHelper.getMaxMagicule(p) && energy.getMagicule()-before==3000,"Crucible refunded its cast, created energy or exceeded capacity");h.assertTrue(i.getCoolDown(2)==20,"Crucible cooldown missing");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void lexiconUsesThreeElementChaptersAndOnlyMagicules(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.THOTH);double aura=TensuraStorages.getExistenceFrom(p).getAura(),mp=TensuraStorages.getExistenceFrom(p).getMagicule();cast(p,i,0);var bursts=i.getOrCreateTag().getList("bursts",Tag.TAG_COMPOUND);h.assertTrue(bursts.size()==3 && bursts.getCompound(0).getString("type").equals("fire") && bursts.getCompound(1).getString("type").equals("ice") && bursts.getCompound(2).getString("type").equals("lightning"),"Lexicon chapters lost distinct native damage types");h.assertTrue(TensuraStorages.getExistenceFrom(p).getAura()==aura && TensuraStorages.getExistenceFrom(p).getMagicule()==mp-5000,"Thoth charged wrong energy");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void newTogglesOwnModifiersWithoutCreatingPassiveBarriers(GameTestHelper h){
        var p=player(h);for(var k:MythicKind.values()){
            var i=learn(p,k);var s=(MythicSkill)i.getSkill();s.onToggleOn(i,p);h.assertTrue(i.isToggled(),"Toggle failed: "+k);var id=WildHunt.id(k.id()+"_passive");var barrier=p.getAttribute(TensuraAttributes.MULTILAYER_BARRIER);
            h.assertTrue(barrier.getModifier(id)==null,"Unrelated kit gained a passive barrier: "+k);
            s.onToggleOff(i,p);h.assertTrue(!i.isToggled() && barrier.getModifier(id)==null,"Toggle left owned barrier behind");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void forgottenSkillsCancelDelayedAttacksAndTheirVisuals(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.THOTH);cast(p,i,3);var t=i.getOrCreateTag();UUID visual=t.getUUID("library_visual");cast(p,i,0);SkillAPI.getSkillsFrom(p).forgetSkill(i);h.assertTrue(!t.contains("library_until") && t.getList("bursts",Tag.TAG_COMPOUND).isEmpty() && h.getLevel().getEntity(visual)==null,"Forgetting left an autonomous attack or owned visual alive");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void prismSplitsToThreeAndBriarsApplyNativeWebbed(GameTestHelper h){
        var p=player(h);var a=target(h,p.position().add(8,0,0));var b=target(h,p.position().add(8,0,3));var c=target(h,p.position().add(8,0,-3));var d=target(h,p.position().add(8,0,6));var mirror=learn(p,MythicKind.MIRRORBOUND);var verdant=learn(p,MythicKind.VERDANT_KEEPER);
        afterTracking(h,List.of(a,b,c,d),()->{cast(p,mirror,0);h.assertTrue(a.getHealth()<1024 && b.getHealth()<1024 && c.getHealth()<1024 && d.getHealth()==1024,"Prism did not limit its split to three distinct targets");for(var v:List.of(a,b,c,d))v.invulnerableTime=0;cast(p,verdant,1);
            h.assertTrue(a.hasEffect(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.WEBBED)),"Briar Grasp failed native Webbed");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void spellbreakDrainsAndDisruptsNonGodlyButPreservesGodlyResources(GameTestHelper h){
        var p=player(h);var a=target(h,p.position().add(8,0,0));var b=target(h,p.position().add(8,0,3));var i=learn(p,MythicKind.THOTH);HunterData.learnReward(b,WildHunt.GODLY.get());
        for(var v:List.of(a,b)){EnergyHelper.setBaseMaxEP(v,100_000);TensuraStorages.getExistenceFrom(v).setMagicule(5000);}
        afterTracking(h,List.of(a,b),()->{
            // Native resource regeneration can run while new chunks become ready.
            // Set the measured reserves immediately before the atomic cast.
            for(var v:List.of(a,b))TensuraStorages.getExistenceFrom(v).setMagicule(5000);
            double mp=TensuraStorages.getExistenceFrom(p).getMagicule();cast(p,i,1);
            h.assertTrue(TensuraStorages.getExistenceFrom(a).getMagicule()==3500 && TensuraStorages.getExistenceFrom(b).getMagicule()==5000,"Spellbreak reserves: ordinary="+TensuraStorages.getExistenceFrom(a).getMagicule()+" Godly="+TensuraStorages.getExistenceFrom(b).getMagicule());h.assertTrue(TensuraStorages.getExistenceFrom(p).getMagicule()==mp-3250,"Spellbreak did not recover exactly half the actual drained magicules");
            var silence=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.SILENCE);h.assertTrue(a.hasEffect(silence) && !b.hasEffect(silence) && b.getHealth()<1024,"Spellbreak did not distinguish native disruption from attack damage");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void astralLibraryFollowsCasterCapsTargetsAndCannotFireTwiceInOneTick(GameTestHelper h){
        var p=player(h);var targets=new ArrayList<net.minecraft.world.entity.monster.Husk>();for(int n=0;n<4;n++)targets.add(target(h,p.position().add(7+n*2,0,0)));var i=learn(p,MythicKind.THOTH);
        afterTracking(h,targets,()->{cast(p,i,3);var tag=i.getOrCreateTag();tag.putLong("library_next",0);tick(p,i);h.assertTrue(targets.stream().filter(v->v.getHealth()<1024).count()==3,"Library target cap is not three");float hp=targets.getFirst().getHealth();long next=tag.getLong("library_next");for(var v:targets)v.invulnerableTime=0;tick(p,i);h.assertTrue(targets.getFirst().getHealth()==hp && tag.getLong("library_next")==next,"Library fired twice in one tick");
            p.setPos(p.position().add(0,0,3));tick(p,i);var visual=(ArtVisualEntity)h.getLevel().getEntity(tag.getUUID("library_visual"));h.assertTrue(visual.position().distanceTo(p.position().add(0,2.5,0))<.001,"Library did not follow caster");MythicArts.clearTransient(p);h.assertTrue(!tag.contains("library_until") && visual.isRemoved(),"Lifecycle left an autonomous library active");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void janusEdictDisplacesSafelyAndParallaxWaitsForItsWarning(GameTestHelper h){
        var p=player(h);var a=target(h,p.position().add(8,0,0));var b=target(h,p.position().add(8,0,3));var i=learn(p,MythicKind.JANUS);
        afterTracking(h,List.of(a,b),()->{Vec3 before=b.position();cast(p,i,2);h.assertTrue(Math.abs(b.getZ()-p.getZ()+3)<.001 && b.getHealth()<1024,"Crossroads did not mirror target across its field center");
            for(var v:List.of(a,b))v.invulnerableTime=0;float hp=a.getHealth();cast(p,i,1);tick(p,i);h.assertTrue(a.getHealth()==hp,"Parallax hit before warning elapsed");due(i,h.getLevel().getGameTime());tick(p,i);h.assertTrue(a.getHealth()<hp,"Parallax failed at warned position");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void newPassivesStopUnfundedAndRemoveTheirActualAttributeContributions(GameTestHelper h){
        var p=player(h);for(var kind:MythicKind.values()){
            var i=learn(p,kind);var s=(MythicSkill)i.getSkill();var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(100000);energy.setMagicule(100000);double speed=p.getAttributeValue(Attributes.MOVEMENT_SPEED),magic=p.getAttributeValue(TensuraAttributes.MAGIC_BARRIER),flame=p.getAttributeValue(TensuraAttributes.FLAME_BOOST);s.onToggleOn(i,p);h.assertTrue(i.isToggled(),"Passive activation failed: "+kind);energy.setAura(0);energy.setMagicule(0);i.getOrCreateTag().putLong("upkeep",0);tick(p,i);
            h.assertTrue(!i.isToggled() && p.getAttributeValue(Attributes.MOVEMENT_SPEED)==speed && p.getAttributeValue(TensuraAttributes.MAGIC_BARRIER)==magic && p.getAttributeValue(TensuraAttributes.FLAME_BOOST)==flame,"Unfunded passive retained attributes: "+kind);
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void vritraFullComboKeepsAllTwentyOneDelayedExplosions(GameTestHelper h){
        var p=player(h);var targets=new ArrayList<net.minecraft.world.entity.monster.Husk>();
        for(int n=0;n<8;n++)targets.add(target(h,p.position().add(8+n,0,0)));
        var i=learn(p,MythicKind.VRITRA);
        afterTracking(h,targets,()->{
            cast(p,i,0);cast(p,i,1);tick(p,i);cast(p,i,2);
            var pending=i.getOrCreateTag().getList("bursts",Tag.TAG_COMPOUND);
            h.assertTrue(pending.size()==21,"Full Vritra combo dropped promised fire fuses: "+pending.size());
            h.assertTrue(pending.stream().filter(v->((CompoundTag)v).getInt("mode")==2).count()==7,"A visual Cataclysm warning was left without its damage event");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void ordinaryElementalSourcesUseStrongestWhileGodlyAndPenaltiesRemain(GameTestHelper h){
        var p=player(h);var a=p.getAttribute(TensuraAttributes.LIGHTNING_BOOST);a.setBaseValue(1);
        var op=AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        var small=WildHunt.id("stack_small");var large=WildHunt.id("stack_large");var other=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("test_native","domination");
        a.addOrUpdateTransientModifier(new AttributeModifier(small,.3,op));a.addOrUpdateTransientModifier(new AttributeModifier(large,.5,op));a.addOrUpdateTransientModifier(new AttributeModifier(other,1,op));
        h.assertTrue(Math.abs(a.getValue()-2)<.0001,"Ordinary sources multiplied: "+a.getValue());
        a.addOrUpdateTransientModifier(new AttributeModifier(WildHunt.id("thoth_passive"),.75,op));
        h.assertTrue(Math.abs(a.getValue()-3.5)<.0001,"Godly bonus was limited with ordinary sources");
        a.addOrUpdateTransientModifier(new AttributeModifier(WildHunt.id("element_penalty"),-.2,op));
        h.assertTrue(Math.abs(a.getValue()-2.8)<.0001,"Elemental penalty was discarded");a.removeModifier(other);
        h.assertTrue(Math.abs(a.getValue()-2.1)<.0001,"Removing strongest source did not reveal next strongest");
        var attack=new AttributeInstance(Attributes.ATTACK_DAMAGE,a0->{});attack.setBaseValue(10);attack.addOrUpdateTransientModifier(new AttributeModifier(small,.5,op));attack.addOrUpdateTransientModifier(new AttributeModifier(large,.5,op));h.assertTrue(attack.getValue()==22.5,"Element policy changed non-elemental attributes");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void elementalPolicyComparesMixedOperationsAndPreservesInnateBase(GameTestHelper h){
        var p=player(h);var a=p.getAttribute(TensuraAttributes.FLAME_BOOST);a.setBaseValue(1);
        a.addOrUpdateTransientModifier(new AttributeModifier(WildHunt.id("flat_element"),1.4,AttributeModifier.Operation.ADD_VALUE));
        a.addOrUpdateTransientModifier(new AttributeModifier(WildHunt.id("base_element"),.8,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        a.addOrUpdateTransientModifier(new AttributeModifier(WildHunt.id("total_element"),1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        h.assertTrue(Math.abs(a.getValue()-2.4)<.0001,"Mixed operations stacked or chose wrong source: "+a.getValue());
        a.setBaseValue(3);h.assertTrue(Math.abs(a.getValue()-6)<.0001,"Innate elemental base was capped");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void revisedPassivesClearSavedLegacyShieldsWithoutRemovingForeignDefense(GameTestHelper h){
        var p=player(h);var foreign=WildHunt.id("unrelated_saved_shield");
        for(var kind:List.of(MythicKind.MIRRORBOUND,MythicKind.DEMETER,MythicKind.JANUS,MythicKind.THOTH)){
            var i=learn(p,kind);var s=(MythicSkill)i.getSkill();var id=WildHunt.id(kind.id()+"_passive");
            for(var holder:List.of(TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MAGIC_BARRIER,TensuraAttributes.MULTILAYER_BARRIER)){
                var a=p.getAttribute(holder);a.addOrReplacePermanentModifier(new AttributeModifier(foreign,27,AttributeModifier.Operation.ADD_VALUE));a.addOrReplacePermanentModifier(new AttributeModifier(id,500,AttributeModifier.Operation.ADD_VALUE));
            }
            s.onToggleOn(i,p);
            for(var holder:List.of(TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MAGIC_BARRIER,TensuraAttributes.MULTILAYER_BARRIER))h.assertTrue(p.getAttribute(holder).getModifier(id)==null && p.getAttribute(holder).getModifier(foreign)!=null,"Saved passive shield survived or foreign defense removed: "+kind);
            s.onToggleOff(i,p);
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void revisedDamageBeatsPreviousUniqueAndUltimateBaselinesInNativeCombat(GameTestHelper h){
        var p=player(h);var z=target(h,p.position().add(8,0,0));var unique=learn(p,MythicKind.THREADWEAVER);var ultimate=learn(p,MythicKind.VRITRA);
        afterTracking(h,List.of(z),()->{
            var us=(MythicSkill)unique.getSkill();var ult=(MythicSkill)ultimate.getSkill();
            MythicArts.hit(us,unique,p,z,90,0,io.github.manasmods.tensura.damage.TensuraDamageTypes.SPACE_ELEMENTAL);float old=1024-z.getHealth();z.setHealth(1024);z.invulnerableTime=0;
            cast(p,unique,0);float improved=1024-z.getHealth();h.assertTrue(old>0 && improved>=old*1.7,"Unique damage failed to improve against native defenses");
            z.setHealth(1024);z.invulnerableTime=0;MythicArts.hit(ult,ultimate,p,z,330,0,io.github.manasmods.tensura.damage.TensuraDamageTypes.LIGHTNING_ELEMENTAL);float previous=1024-z.getHealth();z.setHealth(1024);z.invulnerableTime=0;cast(p,ultimate,0);
            h.assertTrue(previous>0 && 1024-z.getHealth()>=previous*1.9,"Ultimate damage failed to improve against native defenses");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void crystalBulwarkKeepsDefenseAsAnActiveTeamSpecialty(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.DEMETER);cast(p,i,1);var id=WildHunt.id("crystal_bulwark/"+p.getUUID());
        h.assertTrue(p.getAttribute(TensuraAttributes.MULTILAYER_BARRIER).getModifier(id).amount()==500 && p.getAttribute(TensuraAttributes.PHYSICAL_BARRIER).getModifier(id).amount()==50 && p.getAttribute(TensuraAttributes.MAGIC_BARRIER).getModifier(id).amount()==50,"Dedicated Bulwark did not receive stronger native protection");
        var lease=HunterData.of(p).getList("crystal_wards",Tag.TAG_COMPOUND).getCompound(0);h.assertTrue(lease.getLong("until")==h.getLevel().getGameTime()+160,"Bulwark duration is not eight seconds");h.succeed();
    }

}
