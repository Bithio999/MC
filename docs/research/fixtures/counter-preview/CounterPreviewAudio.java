package dev.wildhunt;

@net.neoforged.fml.common.EventBusSubscriber(modid=WildHunt.ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class CounterPreviewAudio {
    @net.neoforged.bus.api.SubscribeEvent public static void ready(net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent event){CounterPreview.audioReady();}
}
