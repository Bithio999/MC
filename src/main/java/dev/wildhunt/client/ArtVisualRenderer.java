package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildhunt.ArtVisualEntity;
import dev.wildhunt.WildHunt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Procedural meshes: solid tapered tentacles and faceted ice, not particle lines. */
@EventBusSubscriber(modid = WildHunt.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArtVisualRenderer extends EntityRenderer<ArtVisualEntity> {
    private static final ResourceLocation ICE = ResourceLocation.withDefaultNamespace("textures/block/blue_ice.png");
    public ArtVisualRenderer(EntityRendererProvider.Context context) { super(context); }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ArtVisualEntity.TYPE.get(), ArtVisualRenderer::new);
    }
    @Override public ResourceLocation getTextureLocation(ArtVisualEntity entity) { return ICE; }
    @Override public void render(ArtVisualEntity entity, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;
        if (entity.kind() == ArtVisualEntity.LASH) {
            double extent = Math.clamp(age / 4, 0, 1) * Math.clamp((entity.lifetime() - age) / 6, 0, 1);
            tentacle(poses.last(), buffers, entity.end(), extent, age, light);
        } else {
            Vec3 base = Vec3.ZERO; double height = entity.end().y;
            if (entity.kind() == ArtVisualEntity.FALLING_ICE) {
                double progress = Math.clamp(age / entity.lifetime(), 0, 1);
                base = entity.end().scale(progress * progress); height = -2.5;
            } else {
                double growth = Math.clamp(age / 4, 0, 1) * Math.clamp((entity.lifetime() - age) / 5, 0, 1);
                height *= growth;
            }
            crystal(poses.last(), buffers.getBuffer(RenderType.entityTranslucent(ICE)), base, height, light);
        }
        super.render(entity, yaw, partial, poses, buffers, light);
    }
    private static Vec3 curve(Vec3 end, double t, float age) {
        Vec3 side = end.cross(new Vec3(0, 1, 0)).normalize();
        return end.scale(t).add(0, Math.sin(t * Math.PI) * (0.7 + end.length() * 0.04), 0)
            .add(side.scale(Math.sin(t * Math.PI * 2 - age * 0.2) * Math.sin(t * Math.PI) * 0.3));
    }
    private static void tentacle(PoseStack.Pose pose, MultiBufferSource buffers, Vec3 end, double extent, float age, int light) {
        if (extent <= 0.001) return;
        Vec3[][] rings = new Vec3[25][8];
        for (int i = 0; i <= 24; i++) {
            double t = extent * i / 24; Vec3 center = curve(end, t, age);
            Vec3 tangent = curve(end, Math.min(1, t + 0.01), age).subtract(curve(end, Math.max(0, t - 0.01), age)).normalize();
            Vec3 right = tangent.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.01) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(tangent).normalize(); double radius = (0.035 + 0.28 * (1 - i / 24.0)) * Math.min(1, extent * 3);
            for (int j = 0; j < 8; j++) {
                double a = j * Math.PI / 4; rings[i][j] = center.add(right.scale(Math.cos(a) * radius)).add(up.scale(Math.sin(a) * radius));
            }
        }
        // Untextured position/color geometry keeps the orange bands emissive,
        // without tinting them through a gray texture or a weather framebuffer.
        VertexConsumer mesh = buffers.getBuffer(RenderType.debugQuads());
        for (int i = 0; i < 24; i++) for (int j = 0; j < 8; j++) {
            int color = i % 5 == 3 ? 0xffff7618 : (j % 2 == 0 ? 0xff211a28 : 0xff38283c);
            for (Vec3 p : new Vec3[]{rings[i][j], rings[i+1][j], rings[i+1][(j+1)%8], rings[i][(j+1)%8]})
                mesh.addVertex(pose, (float)p.x, (float)p.y, (float)p.z).setColor(color);
        }
    }

    private static void crystal(PoseStack.Pose pose, VertexConsumer mesh, Vec3 base, double height, int light) {
        if (Math.abs(height) < 0.01) return;
        double radius = Math.min(0.65, Math.abs(height) * 0.22); Vec3 lower = base, tip = base.add(0, height, 0);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3, b = (i + 1) * Math.PI / 3;
            Vec3 p = base.add(Math.cos(a)*radius, height*0.3, Math.sin(a)*radius);
            Vec3 q = base.add(Math.cos(b)*radius, height*0.3, Math.sin(b)*radius);
            int color = i % 2 == 0 ? 0xe6b5e7ff : 0xe67db8e0;
            quad(pose, mesh, lower, q, p, p, color, light);
            quad(pose, mesh, p, q, tip, tip, color, light);
        }
    }
    private static void quad(PoseStack.Pose pose, VertexConsumer mesh, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int light) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a)).normalize();
        vertex(pose, mesh, a, 0, 0, normal, color, light); vertex(pose, mesh, b, 1, 0, normal, color, light);
        vertex(pose, mesh, c, 1, 1, normal, color, light); vertex(pose, mesh, d, 0, 1, normal, color, light);
    }
    private static void vertex(PoseStack.Pose pose, VertexConsumer mesh, Vec3 p, float u, float v, Vec3 n, int color, int light) {
        mesh.addVertex(pose, (float)p.x, (float)p.y, (float)p.z).setColor(color).setUv(u,v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float)n.x, (float)n.y, (float)n.z);
    }
}
