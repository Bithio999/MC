package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.*;
import dev.wildhunt.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

final class GenesisSealRenderer extends GeoEntityRenderer<GenesisSealEntity> {
    private static final class Model extends GeoModel<GenesisSealEntity>{
        @Override public ResourceLocation getModelResource(GenesisSealEntity e){return WildHunt.id("geo/myths/genesis_seal.geo.json");}
        @Override public ResourceLocation getTextureResource(GenesisSealEntity e){return WildHunt.id("textures/geo/eschaton.png");}
        @Override public ResourceLocation getAnimationResource(GenesisSealEntity e){return WildHunt.id("animations/myths/genesis_seal.animation.json");}
    }
    GenesisSealRenderer(EntityRendererProvider.Context c){super(c,new Model());shadowRadius=.8f;addRenderLayer(new AutoGlowingGeoLayer<>(this));}
    @Override public void renderCubesOfBone(PoseStack p,software.bernie.geckolib.cache.object.GeoBone bone,VertexConsumer out,int light,int overlay,int color){if(!SmoothGeoGeometry.render(p,bone,out,light,overlay,color))super.renderCubesOfBone(p,bone,out,light,overlay,color);}
    @Override public void render(GenesisSealEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){VfxMesh.begin(e.getPosition(partial));super.render(e,yaw,partial,p,b,light);}
}
