package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildhunt.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import static dev.wildhunt.client.VfxMesh.*;

@EventBusSubscriber(modid=WildHunt.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AnankeRenderer extends software.bernie.geckolib.renderer.GeoEntityRenderer<AnankeEntity> {
    private final EffectBatch optics=new EffectBatch();
    public AnankeRenderer(EntityRendererProvider.Context context){super(context,new AnankeGeoModel());shadowRadius=1.1f;addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));}
    @Override public int getPackedOverlay(AnankeEntity e,float u,float partial){return net.minecraft.client.renderer.texture.OverlayTexture.pack(0,e.hasDamageFeedback());}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(MythBosses.ANANKE.get(),AnankeRenderer::new);}
    @Override public ResourceLocation getTextureLocation(AnankeEntity entity){return WildHunt.id("textures/geo/ananke.png");}
    @Override public void render(AnankeEntity entity,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        super.render(entity,yaw,partial,p,b,light);float age=entity.tickCount+partial;VfxMesh.begin(entity.getPosition(partial));optics.begin();
        p.pushPose();p.translate(0,3.65+.04*Math.sin(age*.06),0);p.mulPose(Axis.YP.rotationDegrees(-yaw));p.mulPose(Axis.ZP.rotation(age*.004f));
        int gold=0xFFFFD486;ring(p,optics,Vec3.ZERO,1.1,.055,0,1,tint(gold,.95));ring(p,optics,Vec3.ZERO,1.32,.035,0,1,tint(gold,.6));
        for(int n=0;n<12;n++){double a=n*Math.PI/6;Vec3 at=new Vec3(Math.cos(a)*1.32,Math.sin(a)*1.32,0);beam(p,optics,at,new Vec3(Math.cos(a)*1.53,Math.sin(a)*1.53,0),.025,tint(gold,.8));}
        p.mulPose(Axis.XP.rotation(.7f));ring(p,optics,Vec3.ZERO,1.2,.028,age*.004,1,tint(0xFFB09AFF,.65));p.popPose();
        optics.flush(b);
    }
}
