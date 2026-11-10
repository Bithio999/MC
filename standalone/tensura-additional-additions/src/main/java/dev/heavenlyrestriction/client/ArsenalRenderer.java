package dev.heavenlyrestriction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.forge.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** The real item model leads a prismatic tapered wake, with a quieter hovering formation. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class ArsenalRenderer extends EntityRenderer<ArsenalBlade> {
  public ArsenalRenderer(EntityRendererProvider.Context context) {
    super(context);
    shadowRadius = 0;
  }

  @SubscribeEvent
  public static void register(EntityRenderersEvent.RegisterRenderers e) {
    e.registerEntityRenderer(ForgeContent.BLADE.get(), ArsenalRenderer::new);
  }

  @Override
  public ResourceLocation getTextureLocation(ArsenalBlade b) {
    return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
  }

  @Override
  public void render(
      ArsenalBlade blade,
      float yaw,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light) {
    Vec3 at = blade.getPosition(partial);
    VfxMesh.begin(at);
    boolean opus = ForgeItems.masterpiece(blade.item());
    int color = opus ? 0xffffcf74 : 0xffb9eafa;
    if (blade.phase() == 1) {
      var points = new ArrayList<>(blade.trail);
      for (int n = 0; n + 1 < points.size(); n++) {
        Vec3 a = points.get(n).subtract(at), b = points.get(n + 1).subtract(at);
        double weight = Math.pow(1 - n / (double) points.size(), 1.7);
        SpectacleMesh.stroke(pose, buffers, a, b, .045 * weight, color, .40 * weight);
      }
    }
    pose.pushPose();
    pose.mulPose(new org.joml.Quaternionf(blade.previousRotation).slerp(blade.rotation, partial));
    // NONE avoids the baked FIXED Y rotation; diagonal item sprites become a local +Y blade.
    pose.mulPose(Axis.ZP.rotationDegrees(45));
    float scale = blade.phase() == 1 ? .55f : .43f;
    pose.scale(scale, scale, scale);
    Minecraft.getInstance()
        .getItemRenderer()
        .renderStatic(
            blade.item(),
            ItemDisplayContext.NONE,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            pose,
            buffers,
            blade.level(),
            blade.getId());
    pose.popPose();
  }
}
