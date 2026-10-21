package dev.wildhunt.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.wildhunt.WildHunt;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import java.util.Locale;

/** Keep this encounter's native bars clear of Tensura's upper-left player HUD. */
@EventBusSubscriber(modid=WildHunt.ID,value=Dist.CLIENT)
public final class AnankeBossHud {
    private AnankeBossHud(){}
    @SubscribeEvent public static void progress(CustomizeGuiOverlayEvent.BossEventProgress event){
        String marker=event.getBossEvent().getName().getStyle().getInsertion();
        if(!"wildhunt:ananke_hp".equals(marker) && !"wildhunt:ananke_shp".equals(marker))return;
        event.setCanceled(true);var g=event.getGuiGraphics();var boss=event.getBossEvent();int x=Math.max(4,g.guiWidth()-190),y=event.getY();
        String color=boss.getColor().name().toLowerCase(Locale.ROOT);int progress=Math.clamp((int)Math.ceil(182*boss.getProgress()),0,182);
        RenderSystem.enableBlend();
        g.blitSprite(sprite(color+"_background"),182,5,0,0,x,y,182,5);
        if(boss.getOverlay()!=BossEvent.BossBarOverlay.PROGRESS)g.blitSprite(sprite(boss.getOverlay().name().toLowerCase(Locale.ROOT)+"_background"),182,5,0,0,x,y,182,5);
        if(progress>0){g.blitSprite(sprite(color+"_progress"),182,5,0,0,x,y,progress,5);if(boss.getOverlay()!=BossEvent.BossBarOverlay.PROGRESS)g.blitSprite(sprite(boss.getOverlay().name().toLowerCase(Locale.ROOT)+"_progress"),182,5,0,0,x,y,progress,5);}
        RenderSystem.disableBlend();var font=Minecraft.getInstance().font;g.drawString(font,boss.getName(),x+91-font.width(boss.getName())/2,y-9,0xFFFFFF);
    }
    private static ResourceLocation sprite(String name){return ResourceLocation.withDefaultNamespace("boss_bar/"+name);}
}
