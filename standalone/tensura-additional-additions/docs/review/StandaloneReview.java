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
    private static boolean prepared;private static volatile boolean sawBlockedNotice;private static volatile boolean ready;private static volatile Vec3 expectedLanding;private static int age;
    @SubscribeEvent public static void chat(net.neoforged.neoforge.client.event.ClientChatReceivedEvent.System event){
        if(event.getMessage().getString().startsWith("Chained blocks")){sawBlockedNotice=true;System.out.println("RESTRICTION_REVIEW: client received rejection: "+event.getMessage().getString());}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(mc.player==null || server==null)return;
        if(!prepared){prepared=true;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var level=p.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(6000);
            for(int x=-4;x<25;x++)for(int z=-6;z<=6;z++)level.setBlockAndUpdate(new BlockPos(x,70,z),Blocks.SMOOTH_STONE.defaultBlockState());
            for(var owned:java.util.List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(owned);
            ReincarnationMenu.setRace(p,io.github.manasmods.tensura.registry.race.TensuraRaces.HUMAN.get(),true,false);p.setGameMode(GameType.CREATIVE);EnergyHelper.setBaseMaxEP(p,100000);
            var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(10000);energy.setMagicule(10000);p.teleportTo(level,.5,71,.5,-90,8);p.setOnGround(true);
            var nativeSkill=io.github.manasmods.tensura.registry.skill.UniqueSkills.MARTIAL_MASTER.get();double oldReach=io.github.manasmods.tensura.ability.skill.unique.MartialMasterSkill.CONFIG.ultraDistance;
            try{io.github.manasmods.tensura.ability.skill.unique.MartialMasterSkill.CONFIG.ultraDistance=16;nativeSkill.onPressed(nativeSkill.createDefaultInstance(),p,0,0);expectedLanding=p.position();}finally{io.github.manasmods.tensura.ability.skill.unique.MartialMasterSkill.CONFIG.ultraDistance=oldReach;}
            ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);EnergyHelper.setMaxAura(p,100000);energy.setAura(10000);p.teleportTo(level,.5,71,.5,-90,8);p.setOnGround(true);
            var skill=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();skill.onPressed(p,0,0);
            if(p.position().distanceToSqr(expectedLanding)>.0001 || Math.abs(energy.getAura()-9800)>.001 || skill.getCoolDown(0)!=1)throw new IllegalStateException("Real-player native landing/payment/cooldown comparison failed");
            ChainedState.refresh(p);skill.onPressed(p,0,0);if(Math.abs(energy.getAura()-9800)>.001 || skill.getCoolDown(0)!=1)throw new IllegalStateException("Immediate repeat ignored cooldown");
            System.out.println("RESTRICTION_REVIEW: real-player downward dash matches native Martial Master at "+expectedLanding+", paid 200 aura, 1-second cooldown survives refresh and blocks immediate recast; Myths absent="+!net.neoforged.fml.ModList.get().isLoaded("wildhunt"));
            double mp=EnergyHelper.getBaseMaxMagicule(p),aura=energy.getAura();var forbidden=io.github.manasmods.tensura.registry.skill.UniqueSkills.MUSICIAN.get();
            if(SkillAPI.getSkillsFrom(p).learnSkill(forbidden.createDefaultInstance()) || SkillAPI.getSkillsFrom(p).getSkill(forbidden).isPresent() || EnergyHelper.getBaseMaxMagicule(p)!=mp || energy.getAura()!=aura)throw new IllegalStateException("Real-player blocked acquisition charged or succeeded");
            ready=true;
        });}
        if(!ready)return;age++;if(mc.screen!=null)mc.setScreen(null);mc.options.hideGui=age>=35;
        if(age==25){if(!sawBlockedNotice)throw new IllegalStateException("Client did not receive the Chained rejection notice");if(mc.player.position().distanceToSqr(expectedLanding)>1)throw new IllegalStateException("Client did not receive the native-targeted teleport: "+mc.player.position());System.out.println("RESTRICTION_REVIEW: client landing synchronized at "+mc.player.position());}
        if(age==40)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();var skill=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.CHAINED.get()).orElseThrow();if(skill.onCoolDown(0))throw new IllegalStateException("Native cooldown did not expire");p.teleportTo(p.serverLevel(),.5,71,.5,-90,8);p.setOnGround(true);var energy=TensuraStorages.getExistenceFrom(p);double aura=energy.getAura();skill.onPressed(p,0,0);if(Math.abs(energy.getAura()-aura*.98)>.001 || p.position().distanceToSqr(expectedLanding)>.0001)throw new IllegalStateException("Real-player recast after expiry failed");p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(p.serverLevel(),20,77,-17,35.2f,13.5f);System.out.println("RESTRICTION_REVIEW: native cooldown expired and repeat cast succeeded");});
        if(age>45 && age%10==0){int now=age;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var level=p.serverLevel();p.teleportTo(level,20,77,-17,35.2f,13.5f);
            if(now<100)ArtVisualEntity.spawn(level,ArtVisualEntity.CHAINED_RUSH,new Vec3(0,72,0),new Vec3(16,0,0),14);
            else ArtVisualEntity.spawn(level,ArtVisualEntity.HEALING_PULSE,new Vec3(8,71,0),new Vec3(1.5,0,0),18).style(2);
        });}
        if(age==30 || age==85 || age==145){
            System.out.println("RESTRICTION_REVIEW: client camera="+mc.player.position()+" yaw="+mc.player.getYRot()+" pitch="+mc.player.getXRot());
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){var dir=java.nio.file.Path.of("../docs/media");java.nio.file.Files.createDirectories(dir);image.writeToFile(dir.resolve(age==30?"chained-block-message.png":age==85?"standalone-dash.png":"standalone-recovery.png"));System.out.println("RESTRICTION_REVIEW: saved screenshot at "+age);}
            catch(java.io.IOException e){throw new IllegalStateException(e);}
        }
        if(age==165){System.out.println("RESTRICTION_REVIEW: client review complete");mc.stop();}
    }
}
