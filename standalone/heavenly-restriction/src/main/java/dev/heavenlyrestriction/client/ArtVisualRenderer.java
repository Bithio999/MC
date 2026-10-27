package dev.heavenlyrestriction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.heavenlyrestriction.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import static dev.heavenlyrestriction.client.VfxMesh.*;

/** Original smooth broken-chain dash and authored recovery bloom, with no unrelated skill rigs. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ArtVisualRenderer extends software.bernie.geckolib.renderer.GeoEntityRenderer<ArtVisualEntity> {
    private final EffectBatch batch=new EffectBatch();
    public ArtVisualRenderer(EntityRendererProvider.Context context){super(context,new RecoveryModel());addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(ArtVisualEntity.TYPE.get(),ArtVisualRenderer::new);}
    @Override public ResourceLocation getTextureLocation(ArtVisualEntity e){return getGeoModel().getTextureResource(e);}
    @Override public RenderType getRenderType(ArtVisualEntity e,ResourceLocation texture,MultiBufferSource buffers,float partial){return RenderType.entityTranslucent(texture);}
    @Override public void renderCubesOfBone(PoseStack p,software.bernie.geckolib.cache.object.GeoBone bone,com.mojang.blaze3d.vertex.VertexConsumer out,int light,int overlay,int color){if(!SmoothGeoGeometry.render(p,bone,out,light,overlay,color))super.renderCubesOfBone(p,bone,out,light,overlay,color);}
    @Override protected void applyRotations(ArtVisualEntity e,PoseStack p,float age,float rotation,float partial,float scale){}
    @Override public void render(ArtVisualEntity e,float yaw,float partial,PoseStack p,MultiBufferSource buffers,int light){
        float age=e.tickCount+partial;VfxMesh.begin(e.getPosition(partial));batch.begin();
        if(e.kind()==ArtVisualEntity.CHAINED_RUSH){ChainedGeometry.render(p,batch,e.end(),age);batch.flush(buffers);return;}
        p.pushPose();if(e.caster()!=null){Vec3 delta=e.caster().getPosition(partial).subtract(e.getPosition(partial));p.translate(delta.x,delta.y,delta.z);}
        double f=fade(age,e.lifetime()),r=e.end().x*(.65+.35*smooth(age/e.lifetime()));int style=e.style(),color=style==0?0xFF9FDEAA:style==1?0xFFBFE69D:0xFFFFD784;
        seal(p,batch,LEAF,new Vec3(0,.06,0),r,age*.01,0,tint(color,.55*f));
        for(int n=0;n<3+style;n++){double a=n*Math.PI*2/(3+style)+age*.06;Vec3 last=polar(a,r,0);for(int k=1;k<12;k++){double t=k/11d;Vec3 next=polar(a+t*1.5,r*(1-.5*t),t*2.1);beam(p,batch,last,next,.055*f,tint(color,.75*f));last=next;}}
        batch.flush(buffers);float scale=(float)Math.min(1.5,Math.max(.4,Math.abs(e.end().x))/.85);p.scale(scale,1,scale);super.render(e,0,partial,p,buffers,light);p.popPose();
    }
}
