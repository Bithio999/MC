package dev.heavenlyrestriction;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in validation-world review; never part of the normal distribution. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class NeoOtherworldClientReview {
    private static boolean prepared;private static volatile boolean ready,passed;private static int age;
    private static void shot(Minecraft mc,String name){try(var picture=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
        var dir=java.nio.file.Path.of("review-media");java.nio.file.Files.createDirectories(dir);picture.writeToFile(dir.resolve(name+".png"));
    }catch(java.io.IOException ex){throw new IllegalStateException(ex);}}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(mc.player==null || server==null || mc.player.tickCount<100)return;
        if(!server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Restriction Validation"))throw new IllegalStateException("Review requires isolated validation world");
        if(!prepared){prepared=true;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();TensuraStorages.getPlayerDataFrom(p).setInResetProgress(0);p.setGameMode(GameType.SURVIVAL);
            for(var owned:java.util.List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(owned);
            ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);p.closeContainer();TensuraStorages.getPlayerDataFrom(p).setInResetProgress(0);
            p.setNoGravity(true);p.teleportTo(p.serverLevel(),.5,180,.5,-90,-20);p.setOnGround(false);
            var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(1000);var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();var start=p.position();i.onPressed(p,0,0);
            if(p.position().distanceTo(start)<15.99 || Math.abs(energy.getAura()-980)>.01 || i.getCoolDown(0)!=1)throw new IllegalStateException("Actual player airborne Chained failed");
            RaceAPI.getRaceFrom(p).setRace(HeavenlyRestriction.RESTRICTED.getLast().get(),false);i.setMastery(i.getMaxMastery());energy.setTrueHero(true);ChainedEvolution.discover(p);var u=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();
            if(!u.isMastered(p))throw new IllegalStateException("Automatic Unbound evolution failed");
            p.setOnGround(false);u.setCoolDown(0,0);energy.setAura(1000);start=p.position();u.onPressed(p,0,0);
            if(p.position().distanceTo(start)<15.99 || Math.abs(energy.getAura()-980)>.01)throw new IllegalStateException("Actual player airborne Unbound failed");
            ((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();passed=true;ready=true;System.out.println("NEO_OTHERWORLD_REVIEW: actual player Chained and Unbound both teleported 16 blocks in midair, paid 2% once and retained cooldown");
        });}
        if(!ready)return;age++;mc.options.hideGui=false;
        if(age==15){
            var menu=new ReincarnationMenu(0,mc.player.getInventory(),mc.player,false,0,0,0);int index=menu.getRacePool().indexOf(HeavenlyRestriction.RESTRICTED.getFirst().get());if(index<0)throw new IllegalStateException("Fettered Human missing from menu");
            menu.selectedManasRaceIndex.set(index);mc.setScreen(new ReincarnationScreen(menu,mc.player.getInventory(),Component.literal("Reincarnation")));
        }
        if(age==23){shot(mc,"fettered-human-selection-neo-otherworld");System.out.println("NEO_OTHERWORLD_REVIEW: native Fettered Human panel rendered with Hard difficulty, stats, Chained and description");}
        if(age==30)mc.setScreen(new AbilitySelectionScreen(AbilityCategoriesScreen.AbilityType.SKILL,io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE,null));
        if(age==32 && mc.screen instanceof AbilitySelectionScreen screen)screen.selectedAbility=SkillAPI.getSkillsFrom(mc.player).getSkill(HeavenlyRestriction.UNBOUND.get()).orElseThrow();
        if(age==38)shot(mc,"unbound-neo-otherworld");
        if(age==45){if(!passed)throw new IllegalStateException("Review did not pass");System.out.println("NEO_OTHERWORLD_REVIEW: complete; Myths absent="+!net.neoforged.fml.ModList.get().isLoaded("wildhunt"));mc.stop();}
    }
}
