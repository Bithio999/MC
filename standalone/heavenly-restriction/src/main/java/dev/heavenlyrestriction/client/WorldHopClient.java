package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.worldhop.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid=HeavenlyRestriction.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class WorldHopClient {
    private static boolean busy;private static Path previous;
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        WorldHop.clientAvailable=e->{var mc=Minecraft.getInstance();var s=mc.getSingleplayerServer();return e==mc.player && s!=null && !s.isPublished() && s.getPlayerList().getPlayerCount()==1;};
        WorldHopNetwork.receiver=WorldHopClient::begin;
        WorldHopNetwork.statusReceiver=ok->{var mc=Minecraft.getInstance();if(mc.getSingleplayerServer()==null)return;if(ok){previous=null;busy=false;}else {Path old=previous;if(old==null){var host=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());if(host!=null)old=WorldHopFiles.previous(mc.getLevelSource().getBaseDir(),host.getPersistentData().getCompound(WorldHopMetadata.MARKER).getString("Previous")).orElse(null);}mc.level.disconnect();mc.disconnect(new MessageScreen("No safe landing was found."));recover(old,"World Hop could not find safe ground. Your previous world is unchanged.");}};
    });}
    @EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){busy=false;}
    }
    private static void begin(UUID token){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(busy || server==null)return;
        var claimed=WorldHop.claim(server,token);if(claimed.isEmpty())return;var ticket=claimed.get();
        try{
            Path old=ticket.source().toRealPath(),saves=mc.getLevelSource().getBaseDir().toRealPath();if(!old.getParent().equals(saves))throw new java.io.IOException("The world is not in this instance's saves directory");
            busy=true;previous=old;MessageScreen progress=new MessageScreen("Saving your current world…");mc.level.disconnect();mc.disconnect(progress);
            // disconnect waits for integrated-server shutdown and all chunk/player writes to finish.
            progress.message="Creating a fresh-seed world…";
            String folder="PWJ-"+UUID.randomUUID();
            CompletableFuture.supplyAsync(()->{
                try{return WorldHopFiles.fork(old,folder,stage->WorldHopMetadata.rewrite(stage,ticket.seed(),ticket.player(),old.getFileName().toString(),folder));}
                catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}
            }).whenComplete((fresh,error)->mc.execute(()->{
                if(error!=null){LoggerFactory.getLogger("TensuraAdditionalAdditions/WorldHop").error("World Hop left the previous world unchanged",error);recover(old,"World Hop could not create the new world. Your previous world is unchanged.");return;}
                progress.message="Entering your parallel world…";
                mc.createWorldOpenFlows().openWorld(fresh.getFileName().toString(),()->recover(old,"The new world could not be opened. Your previous world is unchanged."));
            }));
        }catch(Exception ex){busy=false;LoggerFactory.getLogger("TensuraAdditionalAdditions/WorldHop").error("Could not prepare World Hop",ex);if(mc.player!=null)mc.player.sendSystemMessage(Component.literal("World Hop could not start. Your world is unchanged."));else if(previous!=null)recover(previous,"World Hop could not start. Your previous world is unchanged.");}
    }
    private static void recover(Path original,String reason){busy=false;Minecraft.getInstance().setScreen(new RecoveryScreen(original,reason));}
    private static class MessageScreen extends Screen {
        volatile String message;
        MessageScreen(String text){super(Component.literal("World Hop"));message=text;}
        @Override public boolean shouldCloseOnEsc(){return false;}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fillGradient(0,0,width,height,0xff080f20,0xff1e2446);int cx=width/2,cy=height/2;
            int beat=(int)(System.currentTimeMillis()/300%4);g.drawCenteredString(font,"PARALLEL WORLD JUMPER",cx,cy-38,0xffbbdcff);
            g.drawCenteredString(font,message+".".repeat(beat),cx,cy-8,0xffeeeeff);g.drawCenteredString(font,"Your previous world is preserved.",cx,cy+18,0xffa2aec5);
        }
    }
    private static final class RecoveryScreen extends MessageScreen {
        private final Path original;
        RecoveryScreen(Path old,String reason){super(reason);original=old;}
        @Override protected void init(){
            if(original!=null)addRenderableWidget(Button.builder(Component.literal("Return to previous world"),b->Minecraft.getInstance().createWorldOpenFlows().openWorld(original.getFileName().toString(),()->Minecraft.getInstance().setScreen(new TitleScreen()))).bounds(width/2-110,height/2+45,220,20).build());
            addRenderableWidget(Button.builder(Component.literal("World list"),b->Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(new TitleScreen()))).bounds(width/2-110,height/2+70,220,20).build());
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);for(var widget:renderables)widget.render(g,mx,my,partial);}
    }
}
