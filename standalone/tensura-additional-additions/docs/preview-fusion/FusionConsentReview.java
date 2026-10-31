package dev.heavenlyrestriction;
import dev.heavenlyrestriction.fusion.Fusion;
import dev.heavenlyrestriction.client.FusionClient;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class FusionConsentReview {
    private static final Path OUT=Path.of("/workspace/validation-fusion/ui-review");private static volatile int phase;private static int age,frames;private static volatile String error;
    private static void check(boolean yes,String why){if(!yes){error=why;throw new IllegalStateException("FUSION_CONSENT_REVIEW: "+why);}}
    private static void shot(String name){try{Files.createDirectories(OUT);try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUT.resolve(name+".png"));}}catch(Exception ex){throw new RuntimeException(ex);}}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){if(!Boolean.getBoolean("fusion.review.consentOnly"))return;var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;if(++age>2400 || error!=null){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("failure.txt"),error==null?"timeout phase "+phase:error);}catch(Exception ignored){}mc.stop();return;}
        var server=mc.getSingleplayerServer();if(server==null || mc.player==null || mc.getOverlay()!=null)return;
        if(phase==0 && mc.player.tickCount>60){phase=1;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();check(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Fusion Preview"),"Not disposable review world");p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);
            var guest=net.neoforged.neoforge.common.util.FakePlayerFactory.get(p.serverLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"Fusion Guest"));guest.setPos(p.position().add(2,0,0));guest.setGameMode(GameType.SURVIVAL);SkillAPI.getSkillsFrom(guest).learnSkill(HeavenlyRestriction.FUSION.get().createDefaultInstance());EnergyHelper.setBaseMaxEP(guest,1_000_000);check(Fusion.invite(guest,p)!=null,"Invitation failed");});return;}
        if(phase==1 && mc.screen instanceof FusionClient.Consent && ++frames>30){shot("fusion-consent");var b=(Button)mc.screen.children().stream().filter(c->c instanceof Button button && button.getMessage().getString().equals("Accept fusion")).findFirst().orElseThrow();b.onPress();phase=2;frames=0;return;}
        if(phase==2 && ++frames>50){phase=20;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();check(Fusion.body(p) && !p.isSpectator(),"Client consent did not produce a receiving-player controlled body");phase=3;});return;}
        if(phase==3){shot("fusion-player-host");mc.setScreen(new PauseScreen(true));phase=4;frames=0;return;}
        if(phase==4 && ++frames>20){shot("fusion-exit");((Button)mc.screen.children().stream().filter(c->c instanceof Button b && b.getMessage().getString().equals("Separate Fusion")).findFirst().orElseThrow()).onPress();phase=5;frames=0;return;}
        if(phase==5 && ++frames>20){phase=50;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();check(Fusion.session(p)==null && p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Client separation did not restore player");try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("passed.txt"),"PASS: actual server invitation → native client consent button → authenticated reply packet → receiving-player control persists → native pause-menu separation → restored player. Inviter used a server-side test player.\n");}catch(Exception ex){throw new RuntimeException(ex);}phase=6;});return;}
        if(phase==6)mc.stop();
    }
}
