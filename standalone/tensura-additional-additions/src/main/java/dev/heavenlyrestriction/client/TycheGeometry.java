package dev.heavenlyrestriction.client;

import static dev.heavenlyrestriction.client.VfxMesh.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.heavenlyrestriction.ArtVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** Thin minted coin bursts and gold threads; bounded, LOD-aware. */
final class TycheGeometry {
  static final int GOLD = 0xffffcf62, WHITE_GOLD = 0xfffff1b9;

  private static void coin(
      PoseStack p, MultiBufferSource b, Vec3 at, double r, double spin, double opacity) {
    p.pushPose();
    p.translate(at.x, at.y, at.z);
    p.mulPose(Axis.YP.rotation((float) spin));
    p.mulPose(Axis.ZP.rotation((float) (spin * .24)));
    int n = segments(20);
    double depth = r * .13;
    for (int k = 0; k < n; k++) {
      double a = k * Math.PI * 2 / n, c = (k + 1) * Math.PI * 2 / n;
      var q = new Vec3(Math.cos(a) * r, Math.sin(a) * r, depth);
      var t = new Vec3(Math.cos(c) * r, Math.sin(c) * r, depth);
      surface(p, b, WHITE, new Vec3(0, 0, depth), q, t, t, tint(GOLD, opacity));
      surface(
          p,
          b,
          WHITE,
          new Vec3(0, 0, -depth),
          new Vec3(t.x, t.y, -depth),
          new Vec3(q.x, q.y, -depth),
          new Vec3(q.x, q.y, -depth),
          tint(GOLD, opacity * .7));
      surface(
          p,
          b,
          WHITE,
          q,
          new Vec3(q.x, q.y, -depth),
          new Vec3(t.x, t.y, -depth),
          t,
          tint(WHITE_GOLD, opacity * .8));
    }
    ring(p, b, new Vec3(0, 0, depth + .003), r * .8, r * .035, 0, 1, tint(WHITE_GOLD, opacity));
    beam(
        p,
        b,
        new Vec3(-r * .25, 0, depth + .01),
        new Vec3(r * .25, 0, depth + .01),
        r * .04,
        tint(WHITE_GOLD, opacity));
    beam(
        p,
        b,
        new Vec3(0, -r * .4, depth + .01),
        new Vec3(0, r * .4, depth + .01),
        r * .04,
        tint(WHITE_GOLD, opacity));
    p.popPose();
  }

  static void render(ArtVisualEntity e, PoseStack p, MultiBufferSource b, float age) {
    if (e.caster() != null) {
      var d = e.caster().getPosition(age - e.tickCount).subtract(e.getPosition(age - e.tickCount));
      p.translate(d.x, d.y, d.z);
    }
    double fade = fade(age, e.lifetime());
    if (e.kind() == ArtVisualEntity.TYCHE_HOUSE) return; // Retired protocol ID.
    {
      double t = age / e.lifetime(),
          r = e.kind() == ArtVisualEntity.TYCHE_REBIRTH ? 1.1 + Math.sin(t * Math.PI) * 1.3 : 1.15;
      int count = decorations(e.kind() == ArtVisualEntity.TYCHE_REBIRTH ? 16 : 10);
      for (int n = 0; n < count; n++) {
        double a = n * Math.PI * 2 / Math.max(1, count) + t * 1.5;
        var at =
            polar(a, r * (.7 + .3 * Math.sin(t * Math.PI)), .5 + t * 1.8 + .2 * Math.sin(a * 2));
        coin(p, b, at, .14, age * .15 + n, fade);
        if (e.kind() == ArtVisualEntity.TYCHE_REBIRTH)
          beam(p, b, new Vec3(0, 1, 0), at, .008, tint(GOLD, fade * .25));
      }
      ring(p, b, new Vec3(0, .06, 0), r, .025, age * .02, 0, tint(WHITE_GOLD, fade * .8));
      if (e.kind() == ArtVisualEntity.TYCHE_REBIRTH) {
        glow(p, b, new Vec3(0, 1, 0), (1 - t) * 1.5, tint(WHITE_GOLD, fade * .3));
        ring(p, b, new Vec3(0, 1, 0), r * .7, .02, age * .05, 1, tint(GOLD, fade * .7));
      }
    }
  }
}
