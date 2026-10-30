package dev.heavenlyrestriction;
@net.neoforged.fml.common.EventBusSubscriber(modid=HeavenlyRestriction.ID,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class WorldHopPreviewAudio {
 @net.neoforged.bus.api.SubscribeEvent public static void ready(net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent e){WorldHopPreview.audioReady();}
}
