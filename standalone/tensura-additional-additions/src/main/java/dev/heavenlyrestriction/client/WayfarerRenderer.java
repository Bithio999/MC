package dev.heavenlyrestriction.client;

import static dev.heavenlyrestriction.client.VfxMesh.*;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.highrealm.boss.*;
import io.github.manasmods.tensura.client.entity.human.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Native player skin/held weapon with emissive embroidery and smooth, batched encounter geometry.
 */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class WayfarerRenderer extends OtherworlderRenderer<KaelVeyrEntity> {
  private final EffectBatch batch = new EffectBatch();
  private static final int CYAN = 0xff79deec, WHITE_STAR = 0xffe7faff, GOLD_STAR = 0xffffd18b;

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
      if (e.pattern() == KaelVeyrEntity.TEMPEST) {
        leftArm.xRot = -1.45f;
        leftArm.yRot = -.25f;
        leftArm.zRot = -.16f;
        leftSleeve.copyFrom(leftArm);
      } else if (e.pattern() == KaelVeyrEntity.CONSTELLATION) {
        leftArm.xRot = -2.25f;
        leftArm.zRot = -.3f;
        leftSleeve.copyFrom(leftArm);
      } else if (e.pattern() == KaelVeyrEntity.WALTZ
          && e.patternAge(0) < WayfarerCombat.WALTZ_WINDUP) {
        rightArm.xRot = -1.85f;
        rightArm.yRot = -.65f;
        rightSleeve.copyFrom(rightArm);
      }
    }
  }

  @Override
  public boolean shouldRender(KaelVeyrEntity e, Frustum f, double x, double y, double z) {
    return super.shouldRender(e, f, x, y, z)
        || e.pattern() != 0
            && e.distanceToSqr(x, y, z) < 128 * 128
            && f.isVisible(
                e.getBoundingBox()
                    .minmax(new AABB(e.patternAim(), e.patternAim()))
                    .inflate(30, 14, 30));
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
    if (e.pattern() == 0) return;
    begin(e.getPosition(partial));
    batch.begin();
    Vec3 offset = e.getPosition(partial),
        root = e.patternOrigin().subtract(offset),
        aim = e.patternAim().subtract(offset);
    float age = e.patternAge(partial);
    double fade = smooth(Math.min(age / 5, (e.duration() - age) / 10));
    switch (e.pattern()) {
      case KaelVeyrEntity.RIFT_DRAW -> draw(p, e, root, aim, age, fade);
      case KaelVeyrEntity.CONSTELLATION -> constellation(p, e, offset, age, fade);
      case KaelVeyrEntity.TEMPEST -> {
        seal(p, batch, CIRCUIT, new Vec3(0, .08, 0), 1.8, age * .015, 0, tint(CYAN, .45 * fade));
        for (int n = 0; n < 3; n++) {
          Vec3 last = polar(age * .08 + n * Math.PI * 2 / 3, 1.15, .3);
          for (int k = 1; k <= 16; k++) {
            double t = k / 16d;
            Vec3 next =
                polar(age * .08 + n * Math.PI * 2 / 3 + t * 2.2, 1.15 * (1 - .4 * t), .3 + t * 2.2);
            beam(p, batch, last, next, .025, tint(WHITE_STAR, .55 * fade));
            last = next;
          }
        }
        ring(p, batch, new Vec3(0, 1.1, 0), 1.15, .025, age * .05, 1, tint(CYAN, .65 * fade));
      }
      case KaelVeyrEntity.WALTZ -> waltz(p, e, root, age, fade);
      default -> {}
    }
    batch.flush(buffers);
  }

  private void star(PoseStack p, Vec3 at, double r, double spin, int color) {
    ring(p, batch, at, r, .018, spin, 0, color);
    for (int i = 0; i < 6; i++)
      beam(
          p,
          batch,
          at.add(polar(spin + i * Math.PI / 3, r, 0)),
          at.add(polar(spin + (i + 2) * Math.PI / 3, r, 0)),
          .016,
          color);
    glow(p, batch, at, .16, tint(color, .8));
  }

  private void draw(PoseStack p, KaelVeyrEntity e, Vec3 root, Vec3 aim, float age, double fade) {
    if (age < WayfarerCombat.DRAW_WINDUP) {
      double charge = smooth(age / WayfarerCombat.DRAW_WINDUP);
      star(p, aim.add(0, -.75, 0), .8 + charge * .5, -age * .025, tint(GOLD_STAR, .65 * fade));
      ring(
          p,
          batch,
          new Vec3(0, .85, 0),
          .65 + charge * .3,
          .03,
          age * .06,
          1,
          tint(CYAN, .7 * fade));
      for (int n = 0; n < 8; n++) {
        double a = n * Math.PI / 4 + age * .05;
        glow(p, batch, polar(a, .9 - charge * .3, .5 + charge), .1, tint(CYAN, .6 * fade));
      }
    } else {
      Vec3 delta = root.scale(-1);
      Vec3 last = root.add(0, 1, 0);
      for (int n = 1; n <= 24; n++) {
        double t = n / 24d;
        Vec3 next = root.add(delta.scale(t)).add(0, 1 + Math.sin(t * Math.PI) * 1.5, 0);
        beam(p, batch, last, next, .07 * (1 - t * .6), tint(CYAN, fade));
        last = next;
      }
      Vec3 axis = e.castTarget() == null ? WayfarerCombat.direction(e) : e.getLookAngle();
      crescent(p, new Vec3(0, 1, 0), axis, 4.3, .35, fade);
    }
  }

  private void constellation(PoseStack p, KaelVeyrEntity e, Vec3 offset, float age, double fade) {
    for (int i = 0; i < 6; i++) {
      Vec3 at = WayfarerCombat.lance(e, i).subtract(offset);
      double grow = smooth((age - i * 2) / 9), impact = WayfarerCombat.LANCE_WINDUP + i * 5;
      star(p, at.add(0, -.7, 0), 3.25 * grow, age * .008, tint(CYAN, .42 * fade));
      double flight = Math.clamp((age - (impact - 12)) / 12, 0, 1);
      if (age < impact) {
        Vec3 head = at.add(0, 10 * (1 - flight * flight), 0);
        ring(p, batch, at.add(0, 10, 0), .7, .03, age * .035, 0, tint(GOLD_STAR, .8 * fade));
        beam(p, batch, at.add(0, .3, 0), at.add(0, 10, 0), .015, tint(CYAN, .22 * fade));
        crystal(p, batch, head.add(0, 2.8, 0), head, .19, tint(WHITE_STAR, grow * fade));
        glow(p, batch, head, .5, tint(CYAN, .7 * fade));
      } else if (age - impact < 13) {
        double t = (age - impact) / 13d, envelope = Math.pow(1 - t, 2) * fade;
        glow(p, batch, at, 2 * (1 - t), tint(WHITE_STAR, envelope));
        ring(p, batch, at, .4 + t * 4.8, .12 * (1 - t), age * .04, 0, tint(CYAN, envelope));
        for (int n = 0; n < 6; n++) {
          Vec3 tip = at.add(polar(n * Math.PI / 3, 1.5 + t * 3, t * 2));
          beam(p, batch, at, tip, .045, tint(GOLD_STAR, envelope));
        }
      }
    }
  }

  private void crescent(
      PoseStack p, Vec3 origin, Vec3 axis, double radius, double width, double f) {
    double angle = Math.atan2(axis.z, axis.x);
    int pieces = segments(48);
    double spread = Math.toRadians(70);
    for (int n = 0; n < pieces; n++) {
      double a = angle - spread + 2 * spread * n / pieces,
          b = angle - spread + 2 * spread * (n + 1) / pieces;
      double envelope = Math.sin((n + .5) * Math.PI / pieces);
      Vec3 start = origin.add(polar(a, radius, 0)), end = origin.add(polar(b, radius, 0));
      beam(p, batch, start, end, width * envelope, tint(CYAN, .8 * f));
      beam(
          p,
          batch,
          start.add(0, .015, 0),
          end.add(0, .015, 0),
          width * .18 * envelope,
          tint(WHITE_STAR, f));
    }
  }

  private void waltz(PoseStack p, KaelVeyrEntity e, Vec3 root, float age, double fade) {
    Vec3 axis = WayfarerCombat.direction(e);
    double theta = Math.atan2(axis.z, axis.x);
    if (age < WayfarerCombat.WALTZ_WINDUP) {
      star(p, root.add(0, .08, 0), 2.2, age * .02, tint(GOLD_STAR, .5 * fade));
      for (int n = 0; n < 3; n++) {
        double angle = theta + Math.toRadians((n - 1) * 30);
        for (int side : new int[] {-1, 1})
          beam(
              p,
              batch,
              root.add(0, .15, 0),
              root.add(polar(angle + side * Math.toRadians(38), 24, .15)),
              .025,
              tint(CYAN, .35 * fade));
        crescent(
            p,
            root.add(0, .15, 0),
            axis.yRot((float) Math.toRadians((n - 1) * 30)),
            24,
            .03,
            .22 * fade);
      }
      glow(
          p,
          batch,
          root.add(0, 1.3, 0),
          .25 + smooth(age / WayfarerCombat.WALTZ_WINDUP) * .7,
          tint(WHITE_STAR, .7 * fade));
    } else
      for (int wave = 0; wave < 3; wave++) {
        double part = age - WayfarerCombat.WALTZ_WINDUP - wave * 14;
        if (part < 0 || part > 12) continue;
        double radius = Math.min(27, (part + 1) * 3), endFade = Math.min(1, (12 - part) / 4);
        crescent(
            p,
            root.add(0, .9, 0),
            axis.yRot((float) Math.toRadians((wave - 1) * 30)),
            radius,
            .3,
            endFade * fade);
      }
  }
}
