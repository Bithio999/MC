package dev.heavenlyrestriction.client.prospector;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
/** A small color-only pass. Depth is disabled only inside this material and its native clear restores the state. */
final class ProspectorMaterial extends RenderStateShard {
    // Minecraft's NO_DEPTH_TEST shard is a no-op, not a disable operation.
    // This pass follows particles; explicitly disable and restore their enabled depth test.
    private static final LayeringStateShard THROUGH_TERRAIN=new LayeringStateShard("prospector_through_terrain",com.mojang.blaze3d.systems.RenderSystem::disableDepthTest,com.mojang.blaze3d.systems.RenderSystem::enableDepthTest);
    static final RenderType MARKERS=RenderType.create("prospector_ore_markers",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,4096,false,true,
        RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setLayeringState(THROUGH_TERRAIN).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));
    private ProspectorMaterial(){super("prospector",()->{},()->{});}
}
