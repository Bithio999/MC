package dev.heavenlyrestriction;

import dev.heavenlyrestriction.worldhop.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;

/** Optional real-game filming fixture, restricted to a copied disposable save. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class WorldHopPreview {
    private static final Path OUT=Path.of("/workspace/validation-world-hop-video/capture");
    private static volatile int phase;private static int ticks,warmup;private static boolean prepared;private static Process recorder;
    private static long start,audioStart,postStart;private static MinecraftServer source;private static Path sourcePath;
    private static volatile long seedBefore,seedAfter;private static double epBefore,epAfter,castAt,returnAt,visibleAt;
    private static volatile String raceBefore,raceAfter;private static volatile int diamondsAfter;private static volatile boolean keptSword,previousExists;
    private static float startYaw=120;private static int timeout;
    public static void audioReady(){audioStart=System.nanoTime();}
    private static double seconds(){return (System.nanoTime()-start)/1e9;}
    private static void check(boolean yes,String why){if(!yes)throw new IllegalStateException("WORLD_HOP_PREVIEW: "+why);}
    private static void setup(ServerPlayer p){
        check(p.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("World Hop Preview"),"Only a disposable World Hop Preview save is allowed");
        p.closeContainer();p.removeAllEffects();for(var i:List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(i.getSkill());
        RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(),false);EnergyHelper.setBaseMaxEP(p,1_000_000);TensuraStorages.getPlayerDataFrom(p).setInResetProgress(0);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);p.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(60);p.setHealth(20);
        var energy=TensuraStorages.getExistenceFrom(p);energy.setSpiritualHealth(60);energy.setAura(EnergyHelper.getMaxAura(p));energy.setMagicule(EnergyHelper.getMaxMagicule(p));
        check(SkillHelper.learnSkill(p,HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()),"Native acquisition failed");
        p.getInventory().clearContent();var sword=new ItemStack(Items.DIAMOND_SWORD);sword.set(DataComponents.CUSTOM_NAME,Component.literal("World Hop Keepsake"));p.getInventory().setItem(0,sword);p.getInventory().setItem(1,new ItemStack(Items.DIAMOND,37));p.getInventory().selected=0;
        var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()).orElseThrow();TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,i.getSkill(),0);p.setGameMode(GameType.SURVIVAL);
        var level=p.serverLevel();level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);level.setDayTime(6000);level.setWeatherParameters(60000,0,false,false);
        BlockPos spawn=level.getSharedSpawnPos(),best=null;int highest=-1000;
        for(int dx=-48;dx<=48;dx+=8)for(int dz=-48;dz<=48;dz+=8){int x=spawn.getX()+dx,z=spawn.getZ()+dz;int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);BlockPos feet=new BlockPos(x,y,z);if(y>highest && WorldHop.safe(level,p,feet)){best=feet;highest=y;}}
        check(best!=null,"No safe filming position");p.teleportTo(best.getX()+.5,best.getY(),best.getZ()+.5);p.setYRot(startYaw);p.setXRot(13);p.setInvulnerable(false);p.setNoGravity(false);p.resetFallDistance();
        p.getPersistentData().putString("world_hop_preview_progress","retained");source=p.server;sourcePath=source.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();seedBefore=source.overworld().getSeed();epBefore=EnergyHelper.getMaxEP(p);raceBefore=RaceAPI.getRaceFrom(p).getRace().orElseThrow().getRace().getRegistryName().toString();
        p.containerMenu.broadcastChanges();((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();phase=1;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;if(++timeout>18000){mc.stop();throw new IllegalStateException("Preview timed out at "+phase);}
        var server=mc.getSingleplayerServer();
        if(!prepared && server!=null && mc.player!=null && mc.getOverlay()==null && mc.player.tickCount>20){
            prepared=true;mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.renderDistance().set(5);mc.options.simulationDistance().set(5);mc.options.fov().set(65);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.particles().set(ParticleStatus.MINIMAL);mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0d);mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(.7d);server.execute(()->setup(server.getPlayerList().getPlayers().getFirst()));return;
        }
        if(phase==1 && mc.player!=null && ++warmup>240 && mc.levelRenderer.countRenderedSections()>12){
            try{Files.createDirectories(OUT);start=System.nanoTime();recorder=new ProcessBuilder("ffmpeg","-hide_banner","-loglevel","warning","-y","-f","x11grab","-draw_mouse","0","-framerate","30","-video_size","1280x720","-i",System.getenv().getOrDefault("DISPLAY",":99")+".0","-c:v","libx264","-preset","veryfast","-crf","18","-pix_fmt","yuv420p",OUT.resolve("raw.mp4").toString()).redirectError(OUT.resolve("ffmpeg.log").toFile()).start();phase=2;mc.gui.getChat().clearMessages(true);}catch(Exception ex){throw new IllegalStateException(ex);}
        }
        if(phase==2 && mc.player!=null){
            double t=seconds();mc.player.setYRot(startYaw+(float)(t*3));mc.player.setXRot(13);
            if(t>=9){phase=3;castAt=seconds();source.execute(()->{var p=source.getPlayerList().getPlayers().getFirst();var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()).orElseThrow();i.onPressed(p,0,0);System.out.println("WORLD_HOP_PREVIEW: cast at "+castAt);});}
        }
        if(phase==3 && server!=null && server!=source && mc.player!=null && mc.player.tickCount>30){phase=4;returnAt=seconds();server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();if(p.getPersistentData().contains(WorldHopMetadata.MARKER)){phase=3;return;}seedAfter=server.overworld().getSeed();epAfter=EnergyHelper.getMaxEP(p);raceAfter=RaceAPI.getRaceFrom(p).getRace().orElseThrow().getRace().getRegistryName().toString();diamondsAfter=p.getInventory().getItem(1).getCount();var sword=p.getInventory().getItem(0);keptSword=sword.is(Items.DIAMOND_SWORD) && sword.get(DataComponents.CUSTOM_NAME)!=null && sword.get(DataComponents.CUSTOM_NAME).getString().equals("World Hop Keepsake");previousExists=Files.isRegularFile(sourcePath.resolve("level.dat"));
            check(seedBefore!=seedAfter && Math.abs(epBefore-epAfter)<.01 && raceBefore.equals(raceAfter) && diamondsAfter==37 && keptSword && previousExists,"Character/world checks failed");check(p.getPersistentData().getString("world_hop_preview_progress").equals("retained"),"Saved progress lost");check(SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()).isPresent(),"Skill lost");check(WorldHop.safe(p.serverLevel(),p,p.blockPosition()),"Unsafe landing");phase=5;
        });}
        if(phase==5 && mc.player!=null && ++ticks>240 && mc.levelRenderer.countRenderedSections()>12){phase=6;postStart=System.nanoTime();visibleAt=seconds();startYaw=mc.player.getYRot();mc.player.setXRot(18);mc.gui.getChat().clearMessages(true);}
        if(phase==6 && mc.player!=null){double t=(System.nanoTime()-postStart)/1e9;mc.player.setYRot(startYaw+(float)(t*3));mc.player.setXRot(18);if(t>13){phase=7;finish(mc);}}
    }
    private static void finish(Minecraft mc){try{
        double duration=seconds();recorder.getOutputStream().write("q\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));recorder.getOutputStream().flush();check(recorder.waitFor(20,java.util.concurrent.TimeUnit.SECONDS),"Recorder did not finish");
        var m=new com.google.gson.JsonObject();m.addProperty("seed_before",seedBefore);m.addProperty("seed_after",seedAfter);m.addProperty("ep_before",epBefore);m.addProperty("ep_after",epAfter);m.addProperty("race_before",raceBefore);m.addProperty("race_after",raceAfter);m.addProperty("diamonds_before",37);m.addProperty("diamonds_after",diamondsAfter);m.addProperty("named_sword_preserved",keptSword);m.addProperty("previous_world_retained",previousExists);m.addProperty("cast_at",castAt);m.addProperty("client_return_at",returnAt);m.addProperty("terrain_visible_at",visibleAt);m.addProperty("duration",duration);m.addProperty("audio_offset",audioStart==0?0:(start-audioStart)/1e9);m.addProperty("version","1.3.0");Files.writeString(OUT.resolve("metrics.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(m));mc.stop();
    }catch(Exception ex){throw new IllegalStateException(ex);}}
}
