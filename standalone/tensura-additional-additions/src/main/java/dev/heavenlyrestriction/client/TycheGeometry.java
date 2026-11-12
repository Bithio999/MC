package dev.heavenlyrestriction.client;

import static dev.heavenlyrestriction.client.VfxMesh.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.heavenlyrestriction.ArtVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** Thin minted coins, orbiting fortune-wheel segments, and gold threads; bounded, LOD-aware. */
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
    if (e.kind() == ArtVisualEntity.TYCHE_HOUSE) {
      double r = e.end().x * smooth(age / 14d);
      ring(p, b, new Vec3(0, .06, 0), r, .065, age * .003, 0, opacity(GOLD, fade * .8));
      ring(
          p, b, new Vec3(0, .07, 0), r * .97, .025, -age * .005, 0, opacity(WHITE_GOLD, fade * .5));
      int arms = decorations(12);
      for (int n = 0; n < arms; n++) {
        double a = n * Math.PI * 2 / Math.max(1, arms) + age * .004;
        Vec3 last = polar(a, r * .8, .08);
        for (int k = 1; k <= 10; k++) {
          var next =
              polar(a + k * .025, r * (.8 + .02 * k), .08 + .09 * Math.sin(k * Math.PI / 10));
          beam(p, b, last, next, .012, tint(GOLD, fade * .4));
          last = next;
        }
        coin(
            p,
            b,
            polar(a, r * .985, .6 + .2 * Math.sin(age * .05 + n)),
            .18,
            age * .045 + n,
            fade * .95);
      }
      // A compact fortune wheel keeps the moving field readable even when its edge is off-screen.
      ring(p, b, new Vec3(0, .09, 0), 2.25, .025, age * .007, 0, opacity(GOLD, fade * .6));
      for (int n = 0; n < decorations(12); n++) {
        double a = n * Math.PI / 6 + age * .007;
        beam(p, b, polar(a, 2.1, .09), polar(a, 2.4, .09), .018, opacity(WHITE_GOLD, fade * .7));
      }
      for (int n = 0; n < 3; n++) {
        double a = n * Math.PI * 2 / 3 + age * .025;
        coin(
            p, b, polar(a, 1.3, 1.2 + .2 * Math.sin(age * .05 + n)), .16, age * .1 + n, fade * .95);
      }
    } else {
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
