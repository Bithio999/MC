package dev.heavenlyrestriction;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in isolated review only. Never compiled by regular builds. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class StandaloneReview {
    private static boolean prepared;private static int age;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(mc.player==null || server==null)return;
        if(!prepared){prepared=true;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(6000);
            for(int x=-4;x<25;x++)for(int z=-6;z<=6;z++)level.setBlockAndUpdate(new BlockPos(x,70,z),Blocks.SMOOTH_STONE.defaultBlockState());
            ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);p.setGameMode(GameType.CREATIVE);
            p.teleportTo(level,0,71,0,-90,0);var energy=TensuraStorages.getExistenceFrom(p);EnergyHelper.setMaxAura(p,100000);energy.setAura(10000);
            var skill=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();skill.onPressed(p,0,0);
            if(Math.abs(p.getX()-16)>.001 || Math.abs(energy.getAura()-9800)>.001 || skill.onCoolDown(0))throw new IllegalStateException("Actual-player dash failed");
            System.out.println("RESTRICTION_REVIEW: actual player teleported 16 blocks, paid 200 aura, no cooldown; Myths absent="+!net.neoforged.fml.ModList.get().isLoaded("wildhunt"));
            p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(level,20,77,-17,35.2f,13.5f);
        });}
        age++;if(mc.screen!=null)mc.setScreen(null);mc.options.hideGui=true;
        if(age%10==0){int now=age;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var level=p.serverLevel();p.teleportTo(level,20,77,-17,35.2f,13.5f);
            if(now<100)ArtVisualEntity.spawn(level,ArtVisualEntity.CHAINED_RUSH,new Vec3(0,72,0),new Vec3(16,0,0),14);
            else ArtVisualEntity.spawn(level,ArtVisualEntity.HEALING_PULSE,new Vec3(8,71,0),new Vec3(1.5,0,0),18).style(2);
        });}
        if(age==85 || age==145){
            System.out.println("RESTRICTION_REVIEW: client camera="+mc.player.position()+" yaw="+mc.player.getYRot()+" pitch="+mc.player.getXRot());
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){var dir=java.nio.file.Path.of("../docs/media");java.nio.file.Files.createDirectories(dir);image.writeToFile(dir.resolve(age==85?"standalone-dash.png":"standalone-recovery.png"));System.out.println("RESTRICTION_REVIEW: saved screenshot at "+age);}
            catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
        if(age==165){System.out.println("RESTRICTION_REVIEW: client review complete");mc.stop();}
    }
}
