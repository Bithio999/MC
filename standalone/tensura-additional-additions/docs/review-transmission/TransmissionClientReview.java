package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.client.screen.SpatialMovementScreen;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.battlewill.UtilityArts;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;

/** Real native coordinate-screen requests in a disposable world. Never compiled into releases. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class TransmissionClientReview {
    private static boolean prepared,entered,submitted;private static volatile int stage;private static int age,screenAge;private static long seenCharge;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    private static void screenshot(Minecraft mc,String name){try(var picture=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
        var dir=Path.of("transmission-review");Files.createDirectories(dir);picture.writeToFile(dir.resolve(name+".png"));
    }catch(java.io.IOException ex){throw new IllegalStateException(ex);}}
    private static void coordinates(SpatialMovementScreen<?> screen,double x,double y,double z){try{
        for(var row:List.of(new Object[]{"fieldX",x},new Object[]{"fieldY",y},new Object[]{"fieldZ",z})){
            var field=SpatialMovementScreen.class.getDeclaredField((String)row[0]);field.setAccessible(true);((EditBox)field.get(screen)).setValue(row[1].toString());
        }
    }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
    private static void confirm(SpatialMovementScreen<?> screen){try{
        var method=SpatialMovementScreen.class.getDeclaredMethod("performAction",int.class);method.setAccessible(true);method.invoke(screen,5);
    }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(mc.player==null || server==null || mc.player.tickCount<100)return;
        require(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Transmission Validation"),"Requires disposable Transmission Validation world");
        if(++age>2400){mc.stop();throw new IllegalStateException("Transmission review timed out at stage "+stage);}
        if(!prepared){prepared=true;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.setGameMode(GameType.SURVIVAL);TensuraStorages.getPlayerDataFrom(p).setInResetProgress(0);
            for(var owned:List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(owned);
            io.github.manasmods.manascore.race.api.RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(),false);EnergyHelper.setBaseMaxEP(p,1_000_000);
            var energy=TensuraStorages.getExistenceFrom(p);energy.setSleepModeTime(0);
            for(var attr:List.of(TensuraAttributes.AURA_REGENERATION_MULTIPLIER,TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER)){p.getAttribute(attr).removeModifiers();p.getAttribute(attr).setBaseValue(0);}
            RestrictionData.learnReward(p,UtilityArts.INSTANT_MOVE.get());var parent=SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.INSTANT_MOVE.get()).orElseThrow();parent.setMastery(parent.getMaxMastery());parent.onSkillMastered(p);
            var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get()).orElseThrow();require(i.getMastery()<0,"Native mastery did not unlock learning entry");i.setMastery(0);
            TensuraStorages.getAbilityFrom(p).setAbilitySlot(0,0,i.getSkill(),0);p.setNoGravity(true);p.teleportTo(p.serverLevel(),.5,180,.5,-90,0);
            energy.setAura(1000);energy.setMagicule(700);((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();
            stage=1;i.onPressed(p,0,0);
        });return;}
        if((stage==1 || stage==3) && mc.screen instanceof SpatialMovementScreen<?> screen){
            if(!entered){entered=true;screenAge=0;coordinates(screen,stage==1?12.5:24.5,180,stage==1?16.5:32.5);}
            if(++screenAge==12){screenshot(mc,stage==1?"instant-transmission-coordinates":"instant-transmission-mastered");}
            if(screenAge==18 && !submitted){submitted=true;int next=stage==1?2:4;stage=next;confirm(screen);}
        }
        if((stage==2 || stage==4) && age%5==0)server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var energy=TensuraStorages.getExistenceFrom(p);var i=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get()).orElseThrow();
            if(stage==2){
                if(InstantTransmissionArt.charging(p) && seenCharge==0){seenCharge=p.serverLevel().getGameTime();require(Math.abs(energy.getAura()-800)<.01 && Math.abs(energy.getMagicule()-700)<.01,"Unmastered native GUI payment mismatch");}
                if(seenCharge!=0 && p.serverLevel().getGameTime()-seenCharge>=102){
                    require(!InstantTransmissionArt.charging(p) && Math.abs(p.getX()-12.5)<.01 && Math.abs(p.getZ()-16.5)<.01,"Real player did not complete delayed native GUI transmission");
                    require(Math.abs(energy.getAura()-800)<.01 && Math.abs(energy.getMagicule()-700)<.01,"Completion spent resources twice");
                    i.setMastery(i.getMaxMastery());energy.setAura(1000);((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();entered=false;submitted=false;stage=3;i.onPressed(p,0,0);
                }
            }else if(stage==4 && Math.abs(p.getX()-24.5)<.01 && Math.abs(p.getZ()-32.5)<.01){
                require(!InstantTransmissionArt.charging(p) && Math.abs(energy.getAura()-900)<.01 && Math.abs(energy.getMagicule()-700)<.01,"Mastered GUI payment/instant delivery mismatch");
                try{Files.createDirectories(Path.of("transmission-review"));Files.writeString(Path.of("transmission-review/passed.txt"),"Actual native coordinate UI: unmastered 20 blocks costs 200 aura after 100 ticks; mastered 20 blocks costs 100 aura immediately; MP remains 700.\n");}catch(java.io.IOException ex){throw new IllegalStateException(ex);}
                stage=5;screenAge=0;
            }
        });
        if(stage==5 && ++screenAge==15){screenshot(mc,"instant-transmission-complete");mc.stop();}
    }
}
