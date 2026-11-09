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
  private static final int[] COLORS = {
    0xff9beaf5, 0xffe6a5ff, 0xffffd78a, 0xffa0b5ff, 0xffffa7cf, 0xffa0efca
  };

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
    int color = opus ? 0xffffcf74 : COLORS[Math.floorMod(blade.index(), COLORS.length)];
    if (blade.phase() != 0) {
      var points = new ArrayList<>(blade.trail);
      for (int n = 0; n + 1 < points.size(); n++) {
        Vec3 a = points.get(n).subtract(at), b = points.get(n + 1).subtract(at);
        double weight = Math.pow(1 - n / (double) points.size(), 1.7);
        SpectacleMesh.stroke(pose, buffers, a, b, .12 * weight, color, .65 * weight);
        SpectacleMesh.lance(pose, buffers, b, a, .10 * weight, color, .30 * weight);
      }
    } else {
      double pulse = .65 + .15 * Math.sin((blade.tickCount + partial + blade.index() * 5) * .1);
      SpectacleMesh.pressure(pose, buffers, new Vec3(0, -.35, 0), .24, .025, color, pulse * .35);
    }
    SpectacleMesh.sprite(
        pose,
        buffers,
        SpectacleMesh.FLARE,
        Vec3.ZERO,
        blade.phase() == 1 ? .55 : .22,
        blade.phase() == 1 ? .55 : .22,
        0,
        color,
        blade.phase() == 1 ? .5 : .16);
    pose.pushPose();
    pose.mulPose(
        Axis.YP.rotationDegrees(
            -net.minecraft.util.Mth.rotLerp(partial, blade.yRotO, blade.getYRot())));
    pose.mulPose(
        Axis.XP.rotationDegrees(
            net.minecraft.util.Mth.rotLerp(partial, blade.xRotO, blade.getXRot())));
    pose.mulPose(Axis.ZP.rotationDegrees(blade.phase() == 0 ? -35 : -45));
    pose.scale(.95f, .95f, .95f);
    Minecraft.getInstance()
        .getItemRenderer()
        .renderStatic(
            blade.item(),
            ItemDisplayContext.FIXED,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            pose,
            buffers,
            blade.level(),
            blade.getId());
    pose.popPose();
  }
}
