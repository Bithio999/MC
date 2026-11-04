package dev.heavenlyrestriction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.highrealm.boss.*;
import io.github.manasmods.tensura.client.entity.human.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Native player skin/held weapon with emissive embroidery and a simple casting pose. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class WayfarerRenderer extends OtherworlderRenderer<KaelVeyrEntity> {

  public WayfarerRenderer(EntityRendererProvider.Context context) {
    super(context, false);
    model = new WayfarerModel(context.bakeLayer(ModelLayers.PLAYER));
    addLayer(
        new RenderLayer<KaelVeyrEntity, PlayerLikeModel<KaelVeyrEntity>>(this) {
          @Override
          public void render(
              PoseStack p,
              MultiBufferSource buffers,
              int light,
              KaelVeyrEntity e,
              float a,
              float b,
              float partial,
              float age,
              float yaw,
              float pitch) {
            getParentModel()
                .renderToBuffer(
                    p,
                    buffers.getBuffer(
                        RenderType.eyes(
                            HeavenlyRestriction.id("textures/entity/kael_veyr_emissive.png"))),
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    0xffffffff);
          }
        });
  }

  @SubscribeEvent
  public static void register(EntityRenderersEvent.RegisterRenderers e) {
    e.registerEntityRenderer(WayfarerContent.KAEL.get(), WayfarerRenderer::new);
  }

  private static final class WayfarerModel extends PlayerLikeModel<KaelVeyrEntity> {
    WayfarerModel(ModelPart root) {
      super(root, false);
    }

    @Override
    public void setupAnim(
        KaelVeyrEntity e, float limb, float amount, float age, float yaw, float pitch) {
      super.setupAnim(e, limb, amount, age, yaw, pitch);
      if (e.casting()) {
        leftArm.xRot = -1.45f;
        leftArm.yRot = -.25f;
        leftArm.zRot = -.16f;
        leftSleeve.copyFrom(leftArm);
      }
    }
  }

  @Override
  public void render(
      KaelVeyrEntity e,
      float yaw,
      float partial,
      PoseStack p,
      MultiBufferSource buffers,
      int light) {
    int hurt = e.hurtTime;
    if (e.hitFlash()) e.hurtTime = Math.max(6, hurt);
    super.render(e, yaw, partial, p, buffers, light);
    e.hurtTime = hurt;
  }
}
