package dev.wildhunt.client.guide;
import dev.wildhunt.WildHunt;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;
@EventBusSubscriber(modid=WildHunt.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class GuideClient {
    public static final KeyMapping KEY=new KeyMapping("key.wildhunt.guide",GLFW.GLFW_KEY_F8,"key.categories.wildhunt");
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(KEY);}
    public static void open(Screen parent){Minecraft.getInstance().setScreen(new GuideScreen(parent));}
    @EventBusSubscriber(modid=WildHunt.ID,value=Dist.CLIENT)
    public static final class Events {
        private static boolean requested;
        @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(requested){requested=false;open(null);}while(KEY.consumeClick())if(mc.player!=null && mc.screen==null)open(null);}
        @SubscribeEvent public static void init(ScreenEvent.Init.Post e){var s=e.getScreen();
            if(s instanceof PauseScreen)e.addListener(Button.builder(Component.literal("Myths Guide"),b->open(s)).bounds(8,8,112,20).build());
            else if(s instanceof io.github.manasmods.tensura.client.screen.MainScreen main){int bx=(s.width-254)/2,by=(s.height-163)/2+168;if(by+18<=s.height-3)e.addListener(Button.builder(Component.literal("Guide"),b->open(s)).bounds(bx,by,78,18).build());}
        }
        @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){e.getDispatcher().register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("mythsguide").executes(c->{requested=true;return 1;}));}
    }
}
