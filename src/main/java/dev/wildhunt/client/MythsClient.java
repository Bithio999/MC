package dev.wildhunt.client;

import dev.wildhunt.WildHunt;
import dev.wildhunt.myths.MythsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=WildHunt.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class MythsClient {
    private static final net.minecraft.client.KeyMapping SECONDARY=new net.minecraft.client.KeyMapping("key.wildhunt.secondary_race",org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,"key.categories.wildhunt");
    @SubscribeEvent public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {event.register(SECONDARY);}
    public static net.minecraft.nbt.CompoundTag state=new net.minecraft.nbt.CompoundTag();
    private static String requested;
    public static void open(String view) {requested=view;PacketDistributor.sendToServer(new MythsNetwork.Request("open",view));}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        MythsNetwork.clientStateReceiver=tag->{
            var mc=Minecraft.getInstance();if(mc.player==null)return;state=tag;if(tag.getBoolean("silent")){if(mc.screen instanceof MythsEvolutionScreen screen)screen.refreshState(tag);return;}
            if(requested!=null){var view=requested;requested=null;mc.setScreen(view.equals("godly")?new GodlyAscensionScreen():new MythsEvolutionScreen(tag,view.equals("convergence")));}
            else if(mc.screen instanceof MythsEvolutionScreen screen)screen.update(tag);
            else if(mc.screen instanceof GodlyAscensionScreen screen){if(tag.getBoolean("has_godly"))mc.setScreen(new GodlySelectionScreen());else screen.refresh();}
            else if(mc.screen==null || mc.screen instanceof ChatScreen)mc.setScreen(new MythsEvolutionScreen(tag,false));
        };
    }
    @EventBusSubscriber(modid=WildHunt.ID,value=Dist.CLIENT)
    public static final class Screens {
        private static boolean held;
        private static int heartbeat;
        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            var mc=Minecraft.getInstance();boolean down=mc.player!=null && mc.screen==null && SECONDARY.isDown();
            if(down!=held && mc.getConnection()!=null)PacketDistributor.sendToServer(new MythsNetwork.Request(down?"race_press":"race_release",""));
            if(down && ++heartbeat%10==0)PacketDistributor.sendToServer(new MythsNetwork.Request("race_hold",""));
            held=down;
            if((mc.screen instanceof io.github.manasmods.tensura.client.screen.MainScreen || mc.screen instanceof MythsEvolutionScreen) && mc.player!=null && mc.player.tickCount%20==0)PacketDistributor.sendToServer(new MythsNetwork.Request("sync",""));
        }
        @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
            var screen=event.getScreen();
            if(screen instanceof io.github.manasmods.tensura.client.screen.templates.SimpleScreen)PacketDistributor.sendToServer(new MythsNetwork.Request("sync",""));
            if(screen instanceof io.github.manasmods.tensura.client.screen.AbilitySelectionScreen && (screen instanceof GodlySelectionScreen || screen instanceof GodlyAscensionScreen)) {
                int left=(screen.width-254)/2-1,top=(screen.height-163)/2;
                addTab(event,left+164,top+8,59,screen instanceof GodlyAscensionScreen?"Godly":"Ascension",()->{if(screen instanceof GodlyAscensionScreen)Minecraft.getInstance().setScreen(new GodlySelectionScreen());else open("godly");});
            }
        }
        private static void addTab(ScreenEvent.Init.Post event,int x,int y,int w,String text,Runnable click) {
            var button=new io.github.manasmods.tensura.client.screen.widgets.SimpleButton(x,y,w,13,Component.literal(text),Component.literal(text),b->click.run());
            event.addListener(button);
        }
    }
}
