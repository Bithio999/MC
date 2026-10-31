package dev.heavenlyrestriction;
import dev.heavenlyrestriction.worldhop.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;
/** Opt-in destructive-mechanics review in a copied disposable world only. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class WorldHopClientReview {
    private static volatile int phase;private static int age,frames;private static MinecraftServer oldServer;private static Path first,previous;private static long seed;private static double ep;private static String race;private static Map<String,Double> skills;
    private static void check(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    private static Path path(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();}
    private static Map<String,Double> skills(ServerPlayer p){var result=new TreeMap<String,Double>();for(var i:SkillAPI.getSkillsFrom(p).getLearnedSkills())if(!i.isTemporarySkill())result.put(i.getSkillId().toString(),i.getMastery());return result;}
    private static void log(String message){try{Files.createDirectories(Path.of("world-hop-review"));Files.writeString(Path.of("world-hop-review/results.txt"),message+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(java.io.IOException e){throw new RuntimeException(e);}}
    private static void hop(ServerPlayer p){
        seed=p.server.overworld().getSeed();previous=path(p.server);oldServer=p.server;var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()).orElseThrow();
        TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,i.getSkill(),0);i.setCoolDown(0,0);i.onPressed(p,0,0);log("Requested real World Hop from "+previous.getFileName()+" seed "+seed);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(++age>16000){mc.stop();throw new IllegalStateException("World Hop review timeout at "+phase);}
        var server=mc.getSingleplayerServer();if(server==null || mc.player==null || mc.getOverlay()!=null || mc.player.tickCount<100)return;
        if(phase==0){check(path(server).getFileName().toString().equals("World Hop Validation"),"Only run in disposable World Hop Validation world");phase=1;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(false);p.setNoGravity(false);p.removeAllEffects();for(var owned:SkillAPI.getSkillsFrom(p).getLearnedSkills())if(owned.isToggled())owned.onToggleOff(p);TensuraStorages.getPlayerDataFrom(p).setInResetProgress(0);
            try(var scope=ChainedContext.reincarnation(p,HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get())){check(SkillHelper.learnSkill(p,HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()),"Could not obtain single-player skill");}
            var sword=new ItemStack(Items.DIAMOND_SWORD);sword.set(DataComponents.CUSTOM_NAME,Component.literal("Parallel World Keepsake"));p.getInventory().setItem(0,sword);p.getInventory().setItem(1,new ItemStack(Items.DIAMOND,37));p.getPersistentData().putString("world_hop_foreign_progress","preserved");
            ep=EnergyHelper.getMaxEP(p);race=RaceAPI.getRaceFrom(p).getRace().map(i->i.getRace().getRegistryName().toString()).orElse("");skills=skills(p);skills.compute(HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get().getRegistryName().toString(),(k,v)->v+1);
            first=path(server);try{for(String region:List.of("region","DIM-1/region","DIM1/region","dimensions/review/test/region")){var file=first.resolve(region).resolve("world-hop-marker.keep");Files.createDirectories(file.getParent());Files.writeString(file,"previous terrain");}}catch(java.io.IOException ex){throw new RuntimeException(ex);}
            check(ReincarnationMenu.getReincarnationSkills(ReincarnationMenu.getSkillPool(),p.serverLevel(),true,p).stream().noneMatch(s->s==HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()),"Owned truly unique skill appeared twice");
            hop(p);phase=2;
        });return;}
        if((phase==2 || phase==4) && server!=oldServer && ++frames>100){int completing=phase;phase=9;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();if(p.getPersistentData().contains(WorldHopMetadata.MARKER)){phase=completing;return;}
            check(server.overworld().getSeed()!=seed,"World seed did not change");check(!path(server).equals(previous),"Previous world was overwritten");check(Files.isRegularFile(previous.resolve("level.dat")),"Previous world missing");
            for(String region:List.of("region","DIM-1/region","DIM1/region","dimensions/review/test/region"))check(!Files.exists(path(server).resolve(region).resolve("world-hop-marker.keep")),"Old terrain copied: "+region);
            check(p.getPersistentData().getString("world_hop_foreign_progress").equals("preserved"),"Persistent progress lost");check(p.getInventory().getItem(0).is(Items.DIAMOND_SWORD) && p.getInventory().getItem(0).get(DataComponents.CUSTOM_NAME)!=null && p.getInventory().getItem(0).get(DataComponents.CUSTOM_NAME).getString().equals("Parallel World Keepsake") && p.getInventory().getItem(1).is(Items.DIAMOND) && p.getInventory().getItem(1).getCount()==37,"Inventory/components lost: sword="+p.getInventory().getItem(0)+" custom="+p.getInventory().getItem(0).get(DataComponents.CUSTOM_NAME)+" diamonds="+p.getInventory().getItem(1));
            check(Math.abs(EnergyHelper.getMaxEP(p)-ep)<.01,"EP changed across worlds");check(RaceAPI.getRaceFrom(p).getRace().map(i->i.getRace().getRegistryName().toString()).orElse("").equals(race),"Race lost");check(skills(p).equals(skills),"Permanent skills/mastery lost: "+skills(p)+" expected "+skills);
            check(!p.isInvulnerable() && !p.isNoGravity(),"Temporary landing protection leaked");check(WorldHop.safe(p.serverLevel(),p,p.blockPosition()),"Player was not placed on safe ground");log("Hop "+(completing==2?1:2)+" passed: seed "+server.overworld().getSeed()+", EP "+ep+", race/skills/inventory/progress intact, safe ground "+p.blockPosition());
            if(completing==2 && Integer.getInteger("worldhop.review.hops",2)>1){skills.compute(HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get().getRegistryName().toString(),(k,v)->v+1);frames=0;hop(p);phase=4;}
            else{
                var integrated=(net.minecraft.client.server.IntegratedServer)server;check(integrated.publishServer(GameType.SURVIVAL,false,0),"Could not open disposable LAN session");check(!WorldHop.available(p),"LAN availability guard failed");var skill=HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get();check(!ReincarnationMenu.getReincarnationSkills(ReincarnationMenu.getSkillPool(),p.serverLevel(),true,p).contains(skill),"LAN could roll skill");
                SkillAPI.getSkillsFrom(p).forgetSkill(skill);check(!SkillHelper.learnSkill(p,skill),"LAN could acquire skill");check(!SkillHelper.learnSkill(p,skill.createLearningInstance(p)),"LAN could learn skill");check(WorldHop.claim(server,UUID.randomUUID()).isEmpty(),"LAN spoofed ticket accepted");log("Published LAN rejects roll/acquisition/learning/transfer; original world intact: "+first);phase=6;frames=0;
            }
        });}
        if(phase==6 && ++frames>20){try{Files.writeString(Path.of("world-hop-review/passed.txt"),"Real native World Hop activations changed seeds and regenerated all dimensions. Character, skills, EP, inventory components and progress survived. Preceding worlds remain intact. Safe-ground and LAN restrictions verified.\n");try(var pic=Screenshot.takeScreenshot(mc.getMainRenderTarget())){pic.writeToFile(Path.of("world-hop-review/arrival.png"));}}catch(java.io.IOException ex){throw new RuntimeException(ex);}phase=7;mc.stop();}
    }
}
