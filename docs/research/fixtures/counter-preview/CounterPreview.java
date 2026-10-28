package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.block.TensuraBlocks;
import io.github.manasmods.tensura.registry.entity.HumanEntityTypes;
import io.github.manasmods.tensura.entity.human.golem.TrainingDummyEntity;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.*;
import java.util.*;

/** Isolated actual-client capture. Never on the normal production source path. */
@EventBusSubscriber(modid=WildHunt.ID,value=Dist.CLIENT)
public final class CounterPreview {
    private static final Path OUT=Path.of("/tmp/counter-preview");
    private static final Vec3 PLAYER=new Vec3(-8.5,200,.5),BOSS=new Vec3(8,200,.5);
    private static volatile boolean ready,recording,done,failed;
    private static boolean prepared,cameraReady,captureRequested;private static int warmup;
    private static long startTick,startNano;private static Process recorder;
    private static ServerPlayer player;private static EschatonEntity boss;private static TrainingDummyEntity dummy;
    private static volatile String status="Preparing preview";
    private static volatile double playerEP,auraBefore,auraAfter,paid,mp,playerHP,playerSHP,bossDamage,dummyDamage,incoming;
    private static volatile double castAt,counterAt,hitAt;private static volatile int bossHits,dummyHits,canceled;
    private static double bossMPBeforeCast,bossMPAfterCast,bossMPBeforeRegen,bossMPAfterRegen,bossHPAfterHit,bossSHPAfterHit;private static volatile double regenMP;private static volatile double bossMPBeforeTick,bossHPHealed,regenExpected;private static boolean regenCaptured;private static int dummyOtherHits;private static long audioStarted;private static double bossMPBefore,bossSHPBefore,bossHPBefore;private static volatile double bossMPSpent,bossSHPLost;
    public static void audioReady(){audioStarted=System.nanoTime();}
    private static double seconds(){return (System.nanoTime()-startNano)/1e9;}
    private static void require(boolean yes,String reason){if(!yes){failed=true;throw new IllegalStateException("COUNTER_PREVIEW: "+reason);}}
    private static void near(double actual,double expected,String reason){require(Math.abs(actual-expected)<=Math.max(.01,Math.abs(expected)*1e-6),reason+": "+actual+" != "+expected);}
    private static void setup(ServerPlayer p){
        player=p;var level=p.serverLevel();
        require(p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Myths Validation"),"Only the isolated Myths Validation world is allowed");
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.getServer());level.setDayTime(12500);level.setWeatherParameters(6000,0,false,false);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.getServer());
        for(var e:com.google.common.collect.Lists.newArrayList(level.getAllEntities()))if(e.getTags().contains("counter_preview"))e.discard();
        for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++){
            double radius=Math.sqrt(x*x+z*z);if(radius>31)continue;
            var floor=new BlockPos(x,199,z);var material=radius>29.5?Blocks.POLISHED_BLACKSTONE:radius>28.6?Blocks.GILDED_BLACKSTONE:Blocks.SMOOTH_STONE;
            if((x%8==0 || z%8==0) && radius<28.6)material=Blocks.POLISHED_ANDESITE;
            level.setBlockAndUpdate(floor,material.defaultBlockState());
            for(int y=200;y<=210;y++)if(!level.isEmptyBlock(new BlockPos(x,y,z)))level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        for(var point:List.of(PLAYER,BOSS))for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setBlockAndUpdate(BlockPos.containing(point).offset(x,-1,z),Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        p.closeContainer();TensuraStorages.getPlayerDataFrom(p).setInResetProgress(0);TensuraStorages.getPlayerDataFrom(p).markDirty();
        for(var i:List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(i);
        p.getPersistentData().remove(WildHunt.ID);p.getInventory().clearContent();p.removeAllEffects();
        ReincarnationMenu.setRace(p,WildHunt.RESTRICTED.getFirst().get(),true,false);RaceAPI.getRaceFrom(p).setRace(WildHunt.RESTRICTED.getLast().get(),false);
        var skills=SkillAPI.getSkillsFrom(p);var old=skills.getSkill(WildHunt.CHAINED.get()).orElseThrow();old.setMastery(old.getMaxMastery());old.onToggleOn(p);
        var energy=TensuraStorages.getExistenceFrom(p);energy.setTrueHero(true);energy.setTrueDemonLord(false);ChainedState.refresh(p);
        double desired=8_000_000-EnergyHelper.getMaxMagicule(p);EnergyHelper.setMaxAura(p,EnergyHelper.getBaseMaxAura(p)+(desired-EnergyHelper.getMaxAura(p))/5);ChainedState.refresh(p);
        playerEP=EnergyHelper.getMaxEP(p);near(playerEP,8_000_000,"Effective total EP");
        ChainedEvolution.discover(p);var ultimate=skills.getSkill(WildHunt.UNBOUND.get()).orElseThrow();require(WildHunt.UNBOUND.get().addLearnPoint(ultimate,p,0,1),"Native Unbound evolution failed");
        for(var recovery:WildHunt.REGENERATION)skills.getSkill(recovery.get()).ifPresent(i->i.onToggleOff(p));
        p.experienceLevel=0;p.experienceProgress=0;p.totalExperience=0;p.getFoodData().setFoodLevel(20);p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(false);p.getAbilities().flying=false;p.getAbilities().mayfly=false;p.onUpdateAbilities();
        p.teleportTo(level,PLAYER.x,PLAYER.y,PLAYER.z,-90,0);p.setOnGround(true);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.NETHERITE_SWORD));
        p.setHealth(p.getMaxHealth());energy.setSpiritualHealth(p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));energy.setAura(EnergyHelper.getMaxAura(p));energy.setMagicule(EnergyHelper.getMaxMagicule(p));energy.markDirty();
        auraBefore=energy.getAura();mp=EnergyHelper.getMaxMagicule(p);playerHP=p.getHealth();playerSHP=energy.getSpiritualHealth();
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,WildHunt.UNBOUND.get(),1);TensuraStorages.getAbilityFrom(p).markDirty();
        boss=EschatonContent.ESCHATON.get().create(level);require(boss!=null,"Boss factory");boss.setPos(BOSS);boss.initializeOrigin();boss.setNoAi(true);boss.setYRot(90);boss.setYBodyRot(90);boss.setYHeadRot(90);boss.addTag("counter_preview");level.addFreshEntity(boss);boss.setTarget(p);
        var at=new BlockPos(8,200,2);level.setBlockAndUpdate(at,TensuraBlocks.TRAINING_DUMMY.get().defaultBlockState());
        dummy=HumanEntityTypes.TRAINING_DUMMY.get().create(level);require(dummy!=null,"Native dummy factory");dummy.setPos(at.getBottomCenter().add(0,.25,0));dummy.setYRot(90);dummy.setYBodyRot(90);dummy.setYHeadRot(90);dummy.addTag("counter_preview");dummy.setCustomName(Component.literal("Adjacent training dummy"));dummy.setCustomNameVisible(true);level.addFreshEntity(dummy);
        var scoreboard=level.getScoreboard();var team=scoreboard.getPlayerTeam("counter_preview_enemy");if(team==null)team=scoreboard.addPlayerTeam("counter_preview_enemy");team.setAllowFriendlyFire(false);scoreboard.addPlayerToTeam(boss.getStringUUID(),team);scoreboard.addPlayerToTeam(dummy.getStringUUID(),team);
        ((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();
        ready=true;status="8M EP Heavenbreaker vs. the Last Myth";
        System.out.println("COUNTER_PREVIEW: ready; EP="+playerEP+", max/current aura="+auraBefore+", MP="+mp+", HP="+playerHP+", SHP="+playerSHP+", boss EP="+EnergyHelper.getMaxEP(boss));
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST,receiveCanceled=true)
    public static void incoming(LivingIncomingDamageEvent e){
        if(!recording || e.getEntity()!=player || e.getSource().getEntity()!=boss)return;
        var source=(TensuraDamageSource)e.getSource();var ability=source.tensura$getAbilityInstance();
        if(ability==null || ability.getSkill()!=EschatonContent.DEMIURGE.get() || source.tensura$getAbilityMode()!=7)return;
        incoming=e.getOriginalAmount();if(e.isCanceled()){canceled++;hitAt=seconds();status="Apocalypse negated — Counter released";}
        System.out.println("COUNTER_PREVIEW: actual The End of All Myths; incoming="+incoming+", canceled="+e.isCanceled()+", source="+e.getSource().typeHolder().unwrapKey().orElseThrow().location());
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void before(LivingIncomingDamageEvent e){
        if(!recording || e.getEntity()!=boss || e.getSource().getEntity()!=player)return;
        var i=((TensuraDamageSource)e.getSource()).tensura$getAbilityInstance();if(i==null || i.getSkill()!=WildHunt.UNBOUND.get())return;
        var energy=TensuraStorages.getExistenceFrom(boss);bossMPBefore=energy.getMagicule();bossSHPBefore=energy.getSpiritualHealth();bossHPBefore=boss.getHealth();
    }
    @SubscribeEvent public static void hideHand(RenderHandEvent e){if(ready)e.setCanceled(true);}
    @SubscribeEvent public static void damage(LivingDamageEvent.Post e){
        if(!recording || e.getSource().getEntity()!=player)return;
        var source=(TensuraDamageSource)e.getSource();var ability=source.tensura$getAbilityInstance();if(ability==null || ability.getSkill()!=WildHunt.UNBOUND.get() || source.tensura$getAbilityMode()!=1)return;
        if(e.getEntity()==boss){bossDamage+=e.getNewDamage();bossHits++;bossHPAfterHit=boss.getHealth();bossSHPAfterHit=TensuraStorages.getExistenceFrom(boss).getSpiritualHealth();bossMPBeforeRegen=TensuraStorages.getExistenceFrom(boss).getMagicule();bossMPAfterRegen=bossMPBeforeRegen;bossMPSpent=Math.max(0,bossMPBefore-TensuraStorages.getExistenceFrom(boss).getMagicule());bossSHPLost=Math.max(0,bossSHPBefore-TensuraStorages.getExistenceFrom(boss).getSpiritualHealth());System.out.println("COUNTER_PREVIEW: boss hit, HP "+bossHPBefore+" -> "+boss.getHealth()+", MP "+bossMPBefore+" -> "+TensuraStorages.getExistenceFrom(boss).getMagicule()+", SHP "+bossSHPBefore+" -> "+TensuraStorages.getExistenceFrom(boss).getSpiritualHealth());}
        if(e.getEntity()==dummy){dummyDamage+=e.getNewDamage();dummyHits++;}
        System.out.println("COUNTER_PREVIEW: Counter hit "+e.getEntity().getType()+", actual damage="+e.getNewDamage());
    }
    @SubscribeEvent public static void preTick(ServerTickEvent.Pre e){if(recording && boss!=null)bossMPBeforeTick=TensuraStorages.getExistenceFrom(boss).getMagicule();}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void healing(LivingHealEvent e){
        if(!recording || e.getEntity()!=boss || bossHits==0 || regenCaptured)return;
        boolean infinite=StackWalker.getInstance().walk(frames->frames.anyMatch(f->f.getClassName().equals("io.github.manasmods.tensura.effect.ability.InstantRegenerationEffect")));
        if(!infinite)return;
        bossHPHealed=e.getAmount();regenExpected=(int)(bossHPHealed*io.github.manasmods.tensura.ability.skill.extra.InfiniteRegenerationSkill.CONFIG.magiculeCostMastered);
        bossMPBeforeRegen=bossMPBeforeTick;bossMPAfterRegen=TensuraStorages.getExistenceFrom(boss).getMagicule();regenMP=bossMPBeforeRegen-bossMPAfterRegen;regenCaptured=true;
        near(regenMP,regenExpected,"Exact Infinite Regeneration cost");
        System.out.println("COUNTER_PREVIEW: verified native Infinite Regeneration, HP="+bossHPHealed+", configured MP/HP="+io.github.manasmods.tensura.ability.skill.extra.InfiniteRegenerationSkill.CONFIG.magiculeCostMastered+", MP "+bossMPBeforeRegen+" -> "+bossMPAfterRegen+", actual loss="+regenMP);
    }
    @SubscribeEvent public static void serverTick(ServerTickEvent.Post e){
        if(!recording || done || failed || player==null)return;
        try{
            var level=player.serverLevel();long age=level.getGameTime()-startTick;
            if(age==40){var art=SkillAPI.getSkillsFrom(boss).getSkill(EschatonContent.DEMIURGE.get()).orElseThrow();bossMPBeforeCast=TensuraStorages.getExistenceFrom(boss).getMagicule();art.onPressed(boss,0,7);require(boss.isCasting(),"Native apocalypse did not start");bossMPAfterCast=TensuraStorages.getExistenceFrom(boss).getMagicule();castAt=seconds();status="Eschaton casts The End of All Myths";}
            if(age==220){var art=SkillAPI.getSkillsFrom(player).getSkill(WildHunt.UNBOUND.get()).orElseThrow();auraBefore=TensuraStorages.getExistenceFrom(player).getAura();art.onPressed(player,0,1);require(UnboundCounter.active(player),"Counter stance did not start");auraAfter=TensuraStorages.getExistenceFrom(player).getAura();paid=auraBefore-auraAfter;near(paid,auraBefore*.1,"Counter entry cost");counterAt=seconds();status="Counter stance — waiting for the strike";}
            if(age==270){require(canceled==1,"Apocalypse was not negated exactly once");require(bossHits==1 && dummyHits==1,"Counter did not hit boss and adjacent dummy once: "+bossHits+" / "+dummyHits);require(dummyDamage>0 && bossDamage>0,"Counter did not damage both targets");require(regenCaptured && regenMP>0,"Infinite Regeneration did not consume magicules");near(player.getHealth(),playerHP,"Player HP changed");near(TensuraStorages.getExistenceFrom(player).getSpiritualHealth(),playerSHP,"Player SHP changed");near(EnergyHelper.getMaxEP(player),8_000_000,"Final total EP");status="One slash. Eschaton and adjacent dummy both hit.";}
            if(age>=340){done=true;status="Counter preview complete";}
        }catch(Throwable ex){failed=true;ex.printStackTrace();}
    }
    @SubscribeEvent public static void clientTick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(mc.player==null || server==null || mc.player.tickCount<100)return;
        if(!prepared){prepared=true;mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.renderDistance().set(6);mc.options.fov().set(62);mc.options.particles().set(ParticleStatus.ALL);mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0d);mc.options.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(0d);mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(.75d);server.execute(()->setup(server.getPlayerList().getPlayers().getFirst()));}
        if(failed){if(recorder!=null)recorder.destroy();mc.stop();return;}
        if(!ready)return;
        if(mc.screen!=null)mc.setScreen(null);mc.gui.getChat().clearMessages(true);mc.gui.setOverlayMessage(Component.empty(),false);mc.options.hideGui=false;
        if(!cameraReady){cameraReady=true;mc.options.setCameraType(CameraType.FIRST_PERSON);var camera=new ArmorStand(EntityType.ARMOR_STAND,mc.level);camera.setPos(0,207,27);var look=new Vec3(0,203,0).subtract(camera.getEyePosition());float yaw=(float)Math.toDegrees(Math.atan2(-look.x,look.z)),pitch=(float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance()));camera.setYRot(yaw);camera.setXRot(pitch);camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;camera.setOldPosAndRot();mc.setCameraEntity(camera);}
        if(!recording && !captureRequested && ++warmup>=100){captureRequested=true;
            try{Files.createDirectories(OUT);startNano=System.nanoTime();recorder=new ProcessBuilder("ffmpeg","-hide_banner","-loglevel","warning","-y","-f","x11grab","-draw_mouse","0","-framerate","60","-video_size","1280x720","-i",System.getenv().getOrDefault("DISPLAY",":99")+".0","-c:v","libx264","-preset","veryfast","-crf","18","-pix_fmt","yuv420p","-movflags","+faststart",OUT.resolve("raw.mp4").toString()).redirectError(OUT.resolve("ffmpeg.log").toFile()).start();
                server.execute(()->{startTick=player.serverLevel().getGameTime();recording=true;System.out.println("COUNTER_PREVIEW: recording started");});
            }catch(Exception ex){throw new IllegalStateException(ex);}
        }
        if(recording && (done || seconds()>40)){
            require(done,"Preview timed out");
            try{recorder.getOutputStream().write("q\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));recorder.getOutputStream().flush();require(recorder.waitFor(20,java.util.concurrent.TimeUnit.SECONDS),"Recorder did not close");
                var result=new com.google.gson.JsonObject();result.addProperty("player_total_ep",playerEP);result.addProperty("player_max_magicules",mp);result.addProperty("player_aura_before",auraBefore);result.addProperty("player_aura_after_entry",auraAfter);result.addProperty("aura_paid",paid);result.addProperty("boss_total_ep",EnergyHelper.getMaxEP(boss));result.addProperty("attack","Demiurge / The End of All Myths");result.addProperty("incoming_damage",incoming);result.addProperty("negated_hits",canceled);result.addProperty("eschaton_counter_hits",bossHits);result.addProperty("eschaton_actual_damage",bossDamage);result.addProperty("eschaton_magicules_before_cast",bossMPBeforeCast);result.addProperty("eschaton_magicules_after_cast",bossMPAfterCast);result.addProperty("eschaton_magicules_before_regeneration",bossMPBeforeRegen);result.addProperty("eschaton_magicules_after_regeneration",bossMPAfterRegen);result.addProperty("eschaton_regeneration_magicule_loss",regenMP);result.addProperty("eschaton_hp_healed_by_infinite_regeneration",bossHPHealed);result.addProperty("infinite_regeneration_magicules_per_hp",io.github.manasmods.tensura.ability.skill.extra.InfiniteRegenerationSkill.CONFIG.magiculeCostMastered);result.addProperty("eschaton_magicules_spent_on_hit",bossMPSpent);result.addProperty("eschaton_shp_lost_on_hit",bossSHPLost);result.addProperty("dummy_counter_hits",dummyHits);result.addProperty("dummy_actual_damage",dummyDamage);result.addProperty("player_hp_before_after",playerHP);result.addProperty("player_shp_before_after",playerSHP);result.addProperty("cast_at_seconds",castAt);result.addProperty("counter_at_seconds",counterAt);result.addProperty("hit_at_seconds",hitAt);result.addProperty("audio_offset_seconds",audioStarted==0?-1:(startNano-audioStarted)/1e9);result.addProperty("duration_seconds",seconds());Files.writeString(OUT.resolve("metrics.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));
                System.out.println("COUNTER_PREVIEW: verified and recorded: "+result);mc.stop();
            }catch(Exception ex){throw new IllegalStateException(ex);}
        }
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){
        if(!ready)return;var mc=Minecraft.getInstance();var g=e.getGuiGraphics();int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight(),x=w/2-160;
        g.fill(x,10,x+207,139,0xDD0B1220);g.fill(x,10,x+2,139,0xFFE0C680);
        g.drawString(mc.font,"UNBOUND / COUNTER",x+10,19,0xFFF1D9A0,false);
        g.drawString(mc.font,"Effective EP  "+String.format(Locale.ROOT,"%,.0f",playerEP),x+10,34,0xFFFFFFFF,false);
        g.drawString(mc.font,"Eschaton  1,000,000,000 EP",x+10,47,0xFF9DE8EF,false);
        g.drawString(mc.font,"Attack  The End of All Myths",x+10,60,0xFFE4EAF2,false);
        g.drawString(mc.font,"Negated  "+(canceled>0?String.format(Locale.ROOT,"%,.0f",incoming):"—"),x+10,73,0xFFAFEDBC,false);
        g.drawString(mc.font,"Boss HP damage  "+(bossHits>0?String.format(Locale.ROOT,"%,.1f",bossDamage):"—"),x+10,86,0xFF9DE8EF,false);
        g.drawString(mc.font,"Regen MP cost  "+String.format(Locale.ROOT,"%,.0f",regenMP),x+10,99,0xFF9DE8EF,false);
        g.drawString(mc.font,"MP now  "+String.format(Locale.ROOT,"%,.0f",bossMPAfterRegen),x+10,112,0xFF9DE8EF,false);
        g.drawString(mc.font,"Dummy damage  "+(dummyHits>0?String.format(Locale.ROOT,"%,.1f",dummyDamage):"—"),x+10,125,0xFFECC38E,false);
        int width=mc.font.width(status);g.fill((w-width)/2-10,h-64,(w+width)/2+10,h-43,0xCF0B1220);g.drawCenteredString(mc.font,status,w/2,h-58,0xFFF2E6CA);
    }
}
