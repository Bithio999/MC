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
        event.registerEntityRenderer(HunterOtherworlders.ALMA.get(), context -> new HunterRenderer(context));
        event.registerEntityRenderer(HunterOtherworlders.GEMMA.get(), context -> new HunterRenderer(context));
    }
    private static final class HunterRenderer extends OtherworlderRenderer<HunterOtherworlderEntity> {
        private final OtherworlderEquipment gear=new OtherworlderEquipment();
        HunterRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context){super(context,false);}
        @Override public void render(HunterOtherworlderEntity e,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack p,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
            super.render(e,yaw,partial,p,buffers,light);gear.renderEquipment(e,partial,p,buffers,light);
        }
    }
}
