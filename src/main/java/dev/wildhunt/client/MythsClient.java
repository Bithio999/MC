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
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        MythsNetwork.clientStateReceiver=tag->{
            var mc=Minecraft.getInstance();if(mc.player==null)return;
            if(mc.screen instanceof MythsScreen screen)screen.update(tag);
            else mc.setScreen(new MythsScreen(tag,mc.screen instanceof ChatScreen?null:mc.screen));
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
        }
        @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
            var screen=event.getScreen();
            if(!(screen instanceof io.github.manasmods.tensura.client.screen.MainScreen) && !(screen instanceof io.github.manasmods.tensura.client.screen.EvolutionScreen))return;
            event.addListener(Button.builder(Component.literal("Tensura Myths"),b->PacketDistributor.sendToServer(new MythsNetwork.Request("open","")))
                .bounds(screen.width/2-106,Math.min(screen.height-22,screen.height/2+84),112,18).build());
        }
    }
}
