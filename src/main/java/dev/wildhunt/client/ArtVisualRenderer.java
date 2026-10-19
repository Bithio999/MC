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
    private record AimFrame(Vec3 direction,long time) {}
    private final java.util.Map<ArtVisualEntity,AimFrame> aimFrames=new java.util.WeakHashMap<>();
    public ArtVisualRenderer(EntityRendererProvider.Context context) { super(context); }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ArtVisualEntity.TYPE.get(), ArtVisualRenderer::new);
    }
    @Override public ResourceLocation getTextureLocation(ArtVisualEntity entity) { return ICE; }
    @Override public boolean shouldRender(ArtVisualEntity entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z) {
        return entity.kind()==ArtVisualEntity.CAUSAL_RAY && entity.ownedBy(net.minecraft.client.Minecraft.getInstance().player) || super.shouldRender(entity,frustum,x,y,z);
    }
    @Override public void render(ArtVisualEntity entity, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;
        if (entity.kind() == ArtVisualEntity.LASH) {
            double extent = Math.clamp(age / 4, 0, 1) * Math.clamp((entity.lifetime() - age) / 6, 0, 1);
            tentacle(poses.last(), buffers, entity.end(), extent, age, light);
        } else if(entity.kind()>=ArtVisualEntity.CAUSAL_DOMAIN || entity.kind()==ArtVisualEntity.CAUSAL_RAY) {
            Vec3 vector=entity.end();poses.pushPose();
            var player=net.minecraft.client.Minecraft.getInstance().player;
            if(entity.kind()==ArtVisualEntity.CAUSAL_RAY && entity.ownedBy(player)) {
                // Smooth locally every frame using the same steering response and solid-only ray as the server.
                long now=System.nanoTime();Vec3 wanted=player.getViewVector(1);var previous=aimFrames.get(entity);
                Vec3 facing=previous==null?wanted:dev.wildhunt.WorldlineAim.approach(previous.direction(),wanted,Math.min(.1,(now-previous.time())/1e9));
                aimFrames.put(entity,new AimFrame(facing,now));
                Vec3 eye=player.getEyePosition(partial),end=dev.wildhunt.WorldlineAim.end(player,eye,facing);
                Vec3 side=facing.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
                Vec3 start=eye.add(facing.scale(Math.min(.9,eye.distanceTo(end)*.3))).add(side.scale(.35)).add(0,-.25,0);
                Vec3 offset=start.subtract(entity.getPosition(partial));poses.translate(offset.x,offset.y,offset.z);vector=end.subtract(start);
            }
            GodlyGeometry.render(poses.last(),buffers.getBuffer(RenderType.debugQuads()),entity.kind(),vector,age,entity.lifetime());poses.popPose();
        } else if (entity.kind() == ArtVisualEntity.CAUSAL_SPHERE) {
            causal(poses.last(), buffers.getBuffer(RenderType.debugQuads()), entity.end(), age, entity.lifetime(), entity.kind() == ArtVisualEntity.CAUSAL_RAY);
        } else if (entity.kind() == ArtVisualEntity.TIDAL_WAVE) {
            tidalWave(poses.last(), buffers.getBuffer(RenderType.debugQuads()), entity.end(), age, entity.lifetime());
        } else if (entity.kind() == ArtVisualEntity.WHIRLPOOL) {
            whirlpool(poses.last(), buffers.getBuffer(RenderType.debugQuads()), entity.end().x, age, entity.lifetime());
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
    /** Concentric orbit rings, a rotating clock dial, and a luminous spatial cut. */
    private static void causal(PoseStack.Pose pose, VertexConsumer mesh, Vec3 data, float age, int life, boolean ray) {
        double fade=Math.min(1,age/4d)*Math.min(1,(life-age)/8d);
        if(ray) {
            Vec3 axis=data.normalize();
            Vec3 side=axis.cross(Math.abs(axis.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(.08*fade);
            for(int n=0;n<3;n++) {
                Vec3 offset=new Vec3(0,(n-1)*.11,0);
                colorQuad(pose,mesh,offset.add(side),offset.subtract(side),data.subtract(side).add(offset),data.add(side).add(offset),n==1?0xffffffff:0xffd8b166);
            }
            return;
        }
        double radius=data.x*fade;
        for(int ring=0;ring<3;ring++)for(int n=0;n<96;n++) {
            double a=n*Math.PI/48+age*.012*(ring==1?-1:1),b=a+Math.PI/48;
            double r=radius*(ring==2?.62:1); double w=.035+(data.y>0?.025:0);
            Vec3[] points=new Vec3[4]; int k=0;
            for(double[] pair:new double[][]{{a,r-w},{a,r+w},{b,r+w},{b,r-w}}) {
                double x=Math.cos(pair[0])*pair[1],y=Math.sin(pair[0])*pair[1];
                points[k++]=ring==0?new Vec3(x,.15,y):ring==1?new Vec3(x,y,0):new Vec3(0,y,x);
            }
            colorQuad(pose,mesh,points[0],points[1],points[2],points[3],ring==1?0xeec8a4ff:0xeef4d98c);
        }
        for(int n=0;n<12;n++) {
            double a=n*Math.PI/6-age*.02; Vec3 p=new Vec3(Math.cos(a)*radius,.16,Math.sin(a)*radius),q=p.scale(.88).add(0,.02,0),side=new Vec3(-Math.sin(a)*.04,0,Math.cos(a)*.04);
            colorQuad(pose,mesh,p.add(side),p.subtract(side),q.subtract(side),q.add(side),0xfff6e9c0);
        }
    }
    private static void colorQuad(PoseStack.Pose pose, VertexConsumer mesh, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        for (Vec3 p : new Vec3[]{a,b,c,d,d,c,b,a}) mesh.addVertex(pose, (float)p.x, (float)p.y, (float)p.z).setColor(color);
    }
    /** A curled, scalloped wall of teal water with a pale foaming crest. */
    private static void tidalWave(PoseStack.Pose pose, VertexConsumer mesh, Vec3 data, float age, int life) {
        Vec3 travel = new Vec3(data.x, 0, data.z), direction = travel.normalize(), right = direction.yRot((float)(Math.PI / 2));
        Vec3 center = travel.scale(Math.clamp(age / life, 0, 1));
        double rise = Math.min(1, age / 2.0) * Math.min(1, (life - age) / 3.0);
        for (int x = 0; x < 24; x++) for (int y = 0; y < 10; y++) {
            Vec3[] q = new Vec3[4]; int j=0;
            for (int[] corner : new int[][]{{x,y},{x+1,y},{x+1,y+1},{x,y+1}}) {
                double across = (corner[0] / 12.0 - 1) * data.y, t = corner[1] / 10.0;
                double height = (2.2 + Math.cos(across * 1.5 + age * 0.2) * 0.2) * Math.sin(t * Math.PI * 0.64) * rise;
                double curl = (Math.cos(t * Math.PI * 0.95) - 1) * 0.7;
                q[j++] = center.add(right.scale(across)).add(direction.scale(curl)).add(0,height,0);
            }
            int color = y >= 8 ? (x % 3 == 0 ? 0xfff2fff7 : 0xffb1e9df) : y >= 5 ? 0xff48c0b7 : y >= 2 ? 0xff278eac : 0xff214f7d;
            colorQuad(pose,mesh,q[0],q[1],q[2],q[3],color);
        }
    }
    /** Three rotating tapered spiral ribbons with coral fins and foam tips. */
    private static void whirlpool(PoseStack.Pose pose, VertexConsumer mesh, double radius, float age, int life) {
        double grow = Math.min(1, age / 6.0) * Math.min(1, (life-age) / 8.0);
        for (int arm = 0; arm < 3; arm++) for (int i=0; i<64; i++) {
            Vec3[] q = new Vec3[4]; int j=0;
            for (int[] corner : new int[][]{{i,0},{i+1,0},{i+1,1},{i,1}}) {
                double t = corner[0] / 64.0, a = arm * Math.PI*2/3 + t*Math.PI*3 - age*0.12;
                double r = (0.5 + t*(radius-0.5)) * grow;
                double thickness = 0.12 + t*0.45;
                r += corner[1]*thickness;
                q[j++] = new Vec3(Math.cos(a)*r,0.12+Math.sin(t*Math.PI)*0.25+corner[1]*0.12,Math.sin(a)*r);
            }
            int color = i > 57 ? 0xffdefff1 : arm == 1 && i > 35 ? 0xffcd6878 : (i%8<4 ? 0xff32b5b8 : 0xff236b9b);
            colorQuad(pose,mesh,q[0],q[1],q[2],q[3],color);
        }
        for (int i=0; i<16; i++) {
            double a=i*Math.PI/8+age*0.035,r=radius*grow;
            Vec3 p = new Vec3(Math.cos(a)*r,0.1,Math.sin(a)*r), tangent = new Vec3(-Math.sin(a),0,Math.cos(a)).scale(0.35);
            colorQuad(pose,mesh,p.subtract(tangent),p.add(tangent),p.add(tangent.scale(0.5)).add(0,0.6,0),p.subtract(tangent.scale(0.5)).add(0,0.6,0),0xffdf8192);
        }
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
