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
        WorldHopNetwork.worldsReceiver=WorldSelectScreen::receive;
        WorldHopNetwork.statusReceiver=WorldHopClient::landed;
        try{WorldHopLibrary.recover(Minecraft.getInstance().getLevelSource().getBaseDir());}
        catch(Exception ex){LoggerFactory.getLogger("TensuraAdditionalAdditions/WorldHop").error("An interrupted World Jump needs recovery",ex);}
    });}
    @EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void open(net.neoforged.neoforge.client.event.ScreenEvent.Opening e){
            var mc=Minecraft.getInstance();if(mc.getSingleplayerServer()==null && e.getNewScreen() instanceof net.minecraft.client.gui.screens.worldselection.SelectWorldScreen){
                try{WorldHopLibrary.recover(mc.getLevelSource().getBaseDir());}
                catch(Exception ex){LoggerFactory.getLogger("TensuraAdditionalAdditions/WorldHop").error("Travel recovery left all saves intact",ex);e.setNewScreen(new RecoveryScreen(null,"Interrupted travel needs recovery. Saves have been preserved; see the log."));}
            }
        }
    }
    private static void landed(boolean success){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null)return;
        Path active=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
        if(!success){abort(active,"No safe landing was found. Your previous world has been restored.");return;}
        CompletableFuture.runAsync(()->{try{WorldHopLibrary.finish(active);}catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}}).whenComplete((ignored,error)->mc.execute(()->{
            if(error==null){previous=null;busy=false;}else{LoggerFactory.getLogger("TensuraAdditionalAdditions/WorldHop").error("Could not finish library transfer",error);abort(active,"Travel could not finish. Your previous world has been restored.\n"+WorldHopIo.describe(error));}
        }));
    }
    private static void abort(Path active,String reason){
        var mc=Minecraft.getInstance();Path fallback=previous==null?active:previous;
        if(mc.level!=null)mc.level.disconnect();
        mc.disconnect(new MessageScreen("Restoring your previous world…"));
        CompletableFuture.runAsync(()->{try{WorldHopLibrary.rollback(active);}catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}}).whenComplete((ignored,error)->mc.execute(()->{
            if(error!=null){LoggerFactory.getLogger("TensuraAdditionalAdditions/WorldHop").error("Travel data preserved for recovery",error);recover(null,"Automatic recovery could not finish. Your saves are preserved.\n"+WorldHopIo.describe(error));}
            else recover(fallback,reason);
        }));
    }
    private static void begin(UUID token){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(busy || server==null)return;
        var claimed=WorldHop.claim(server,token);if(claimed.isEmpty())return;var ticket=claimed.get();
        try{
            Path active=ticket.source().toRealPath(),saves=mc.getLevelSource().getBaseDir().toRealPath();if(!active.getParent().equals(saves))throw new java.io.IOException("The world is not in this instance's saves directory");
            busy=true;previous=active;MessageScreen progress=new MessageScreen("Saving your current world…");mc.level.disconnect();mc.disconnect(progress);
            progress.message=ticket.destination()==null?"Creating a fresh-seed world…":"Preparing your selected world…";
            CompletableFuture.runAsync(()->{
                try{
                    WorldHopLibrary.Transaction tx;
                    try(var activeAccess=mc.getLevelSource().validateAndCreateAccess(active.getFileName().toString())){
                        if(!activeAccess.getLevelPath(net.minecraft.world.level.storage.LevelResource.ROOT).toRealPath().equals(active))throw new java.io.IOException("The active slot changed");
                        var data=WorldHopMetadata.read(active.resolve("level.dat")).getCompound("Data");
                        var journal=WorldSelect.journal(active,data.getString("LevelName"),data.getCompound("WorldGenSettings").getLong("seed"));
                        if(ticket.destination()==null && !WorldHopJournal.canCreate(journal))throw new java.io.IOException("The 128-world library is full");
                        WorldHopMigration.migrate(active,journal,(path,seed)->{
                            try(var access=mc.getLevelSource().validateAndCreateAccess(path.getFileName().toString())){
                                if(!access.getLevelPath(net.minecraft.world.level.storage.LevelResource.ROOT).toRealPath().equals(path) || WorldHopMetadata.read(path.resolve("level.dat")).getCompound("Data").getCompound("WorldGenSettings").getLong("seed")!=seed)throw new java.io.IOException("Recorded world changed");
                            }
                        });
                        tx=WorldHopLibrary.begin(active,journal.current());String slot=active.getFileName().toString();
                        if(ticket.destination()==null){
                            WorldHopFiles.stage(active,tx.next(),stage->{
                                WorldHopMetadata.rewrite(stage,ticket.seed(),ticket.player(),slot,slot,WorldHopJournal.nextParallelNumber(journal));
                                var fresh=WorldHopMetadata.read(stage.resolve("level.dat")).getCompound("Data");var destination=new WorldHopJournal.Entry(UUID.randomUUID().toString(),slot,fresh.getString("LevelName"),ticket.seed());
                                WorldHopJournal.write(stage,WorldHopJournal.arrive(journal,destination,slot));
                            },false);
                        }else{
                            var entry=journal.find(ticket.destination().id()).filter(e->!e.id().equals(journal.current())).orElseThrow(()->new java.io.IOException("Destination not visited"));
                            Path destination=WorldHopLibrary.archived(active,entry.id()).orElseThrow(()->new java.io.IOException("Selected world is missing"));
                            if(WorldHopMetadata.read(destination.resolve("level.dat")).getCompound("Data").getCompound("WorldGenSettings").getLong("seed")!=entry.seed())throw new java.io.IOException("Selected world's seed changed");
                            WorldHopFiles.stage(destination,tx.next(),stage->{WorldHopMetadata.revisit(stage,active,ticket.player(),slot,slot);WorldHopMetadata.setName(stage,entry.name());WorldHopJournal.write(stage,WorldHopJournal.arrive(journal,entry,slot));},true);
                        }
                    }
                    if(net.minecraft.util.DirectoryLock.isLocked(active))throw new java.io.IOException("The active save was opened elsewhere");
                    WorldHopLibrary.install(tx);
                }catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}
            }).whenComplete((ignored,error)->mc.execute(()->{
                if(error!=null){LoggerFactory.getLogger("TensuraAdditionalAdditions/WorldHop").error("World Jump could not prepare the library",error);abort(active,"World Jump could not finish. Your previous world has been restored.\n"+WorldHopIo.describe(error));return;}
                progress.message="Entering your parallel world…";
                mc.createWorldOpenFlows().openWorld(active.getFileName().toString(),()->abort(active,"The world could not be opened. Your previous world has been restored."));
            }));
        }catch(Exception ex){busy=false;LoggerFactory.getLogger("TensuraAdditionalAdditions/WorldHop").error("Could not prepare World Jump",ex);if(mc.player!=null)mc.player.sendSystemMessage(Component.literal("World Jump could not start. Your world is unchanged."));else if(previous!=null)abort(previous,"World Jump could not start. Your previous world has been restored.");}
    }

    private static void recover(Path original,String reason){busy=false;Minecraft.getInstance().setScreen(new RecoveryScreen(original,reason));}
    private static class MessageScreen extends Screen {
        volatile String message;
        MessageScreen(String text){super(Component.literal("World Hop"));message=text;}
        @Override public boolean shouldCloseOnEsc(){return false;}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fillGradient(0,0,width,height,0xff080f20,0xff1e2446);int cx=width/2,cy=height/2;
            g.drawCenteredString(font,"PARALLEL WORLD JUMPER",cx,cy-58,0xffbbdcff);
            var lines=font.split(Component.literal(message),Math.max(120,Math.min(500,width-32)));int lineY=cy-26;
            for(int n=0;n<Math.min(8,lines.size());n++){var line=lines.get(n);g.drawString(font,line,cx-font.width(line)/2,lineY,0xffeeeeff,false);lineY+=10;}
            g.drawCenteredString(font,"Your previous world is preserved.",cx,cy+54,0xffa2aec5);
        }
    }
    private static final class RecoveryScreen extends MessageScreen {
        private final Path original;
        RecoveryScreen(Path old,String reason){super(reason);original=old;}
        @Override protected void init(){
            if(original!=null)addRenderableWidget(Button.builder(Component.literal("Return to previous world"),b->Minecraft.getInstance().createWorldOpenFlows().openWorld(original.getFileName().toString(),()->Minecraft.getInstance().setScreen(new TitleScreen()))).bounds(width/2-110,height/2+70,220,20).build());
            addRenderableWidget(Button.builder(Component.literal("World list"),b->Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(new TitleScreen()))).bounds(width/2-110,height/2+95,220,20).build());
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);for(var widget:renderables)widget.render(g,mx,my,partial);}
    }
}
