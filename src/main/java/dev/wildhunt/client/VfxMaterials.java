package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/** Native shaders with linear filtering so fine optical masks do not sparkle under motion. */
final class VfxMaterials extends RenderStateShard {
    private static final Map<ResourceLocation,RenderType> ADDITIVE=new HashMap<>(),SURFACE=new HashMap<>(),RADIANT=new HashMap<>();
    private VfxMaterials(){super("myths_vfx",()->{},()->{});}
    static RenderType additive(ResourceLocation texture){return ADDITIVE.computeIfAbsent(texture,t->RenderType.create("myths_optics",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_EYES_SHADER)
            .setTextureState(new TextureStateShard(t,true,false)).setTransparencyState(ADDITIVE_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));}
    static RenderType surface(ResourceLocation texture){return SURFACE.computeIfAbsent(texture,t->RenderType.create("myths_surface",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,true,true,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(new TextureStateShard(t,true,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setOverlayState(OVERLAY).createCompositeState(false)));}
    /** Straight-alpha emission for the new white-RGB masks; legacy premultiplied optics keep their path. */
    static RenderType radiant(ResourceLocation texture){return RADIANT.computeIfAbsent(texture,t->RenderType.create("myths_radiance",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_EYES_SHADER)
            .setTextureState(new TextureStateShard(t,true,false)).setTransparencyState(LIGHTNING_TRANSPARENCY)
            .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));}
}
