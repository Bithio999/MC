package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.wildhunt.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import static dev.wildhunt.client.VfxMesh.*;

@EventBusSubscriber(modid=WildHunt.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class EschatonRenderer extends GeoEntityRenderer<EschatonEntity> {
    private final EffectBatch optics=new EffectBatch();
    public EschatonRenderer(EntityRendererProvider.Context c){super(c,new EschatonGeoModel());shadowRadius=1.8f;addRenderLayer(new AutoGlowingGeoLayer<>(this));}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(EschatonContent.ESCHATON.get(),EschatonRenderer::new);e.registerEntityRenderer(EschatonContent.SEAL.get(),GenesisSealRenderer::new);}
    @Override public int getPackedOverlay(EschatonEntity e,float u,float partial){return OverlayTexture.pack(0,e.hasDamageFeedback());}
    @Override public void renderCubesOfBone(PoseStack p,software.bernie.geckolib.cache.object.GeoBone bone,VertexConsumer out,int light,int overlay,int color){if(!SmoothGeoGeometry.render(p,bone,out,light,overlay,color))super.renderCubesOfBone(p,bone,out,light,overlay,color);}
    @Override public void render(EschatonEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        begin(e.getPosition(partial));super.render(e,yaw,partial,p,b,light);float age=e.tickCount+partial;optics.begin();int color=e.form()==3?EschatonGeometry.RED:EschatonGeometry.CYAN;
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(180-yaw));
        Vec3 core=new Vec3(0,4.55,-.48);ring(p,optics,core,.38,.026,age*.012,1,tint(EschatonGeometry.PEARL,.7));glow(p,optics,core,.55,tint(color,.3+.12*Math.sin(age*.09)));
        // A stable prismatic heart and flowing vertical script; the crown/mantle are authored meshes.
        if(e.form()>=2)for(int n=0;n<4;n++){double a=n*Math.TAU/4+age*.005;Vec3 low=polar(a,2.4,1),high=polar(a+.4,2.4,6.5);beam(p,optics,low,high,.016,tint(color,.25));}
        p.popPose();optics.flush(b);
    }
}
