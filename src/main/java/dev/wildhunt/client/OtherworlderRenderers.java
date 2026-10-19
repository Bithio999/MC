package dev.wildhunt.client;

import dev.wildhunt.*;
import io.github.manasmods.tensura.client.entity.human.OtherworlderRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = WildHunt.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OtherworlderRenderers {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HunterOtherworlders.ALMA.get(), context -> new OtherworlderRenderer<>(context, false));
        event.registerEntityRenderer(HunterOtherworlders.GEMMA.get(), context -> new OtherworlderRenderer<>(context, false));
    }
}
