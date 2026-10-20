package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildhunt.ArtVisualEntity;
import dev.wildhunt.WildHunt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Render dispatch for bounded textured skill geometry and locally smoothed channels. */
@EventBusSubscriber(modid = WildHunt.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArtVisualRenderer extends EntityRenderer<ArtVisualEntity> {
    private static final ResourceLocation ICE = ResourceLocation.withDefaultNamespace("textures/block/blue_ice.png");
    private final EffectBatch batch=new EffectBatch();
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
        VfxMesh.begin(entity.getPosition(partial));batch.begin();
        if(ThothGeometry.handles(entity.kind())) {
            poses.pushPose();
            if((entity.kind()==ArtVisualEntity.ASTRAL_LIBRARY || entity.kind()==ArtVisualEntity.MANA_CRUCIBLE) && entity.caster()!=null){
                Vec3 offset=entity.caster().getPosition(partial).add(0,entity.kind()==ArtVisualEntity.ASTRAL_LIBRARY?2.5:0,0).subtract(entity.getPosition(partial));poses.translate(offset.x,offset.y,offset.z);
            }
            ThothGeometry.render(poses,batch,entity.kind(),entity.end(partial),age,entity.lifetime(),entity.style());poses.popPose();
        } else if(entity.kind()==ArtVisualEntity.CAUSAL_RAY || entity.kind()>=ArtVisualEntity.CAUSAL_SPHERE && entity.kind()<=ArtVisualEntity.CAUSAL_IMPACT) {
            Vec3 vector=entity.end(partial);poses.pushPose();
            var player=net.minecraft.client.Minecraft.getInstance().player;
            if(entity.kind()==ArtVisualEntity.CAUSAL_RAY && entity.ownedBy(player)) {
                long now=System.nanoTime();Vec3 wanted=player.getViewVector(1);var previous=aimFrames.get(entity);
                Vec3 facing=previous==null?wanted:dev.wildhunt.WorldlineAim.approach(previous.direction(),wanted,Math.min(.1,(now-previous.time())/1e9));
                aimFrames.put(entity,new AimFrame(facing,now));
                Vec3 eye=player.getEyePosition(partial),end=dev.wildhunt.WorldlineAim.end(player,eye,facing);
                Vec3 side=facing.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
                Vec3 start=eye.add(facing.scale(Math.min(.9,eye.distanceTo(end)*.3))).add(side.scale(.35)).add(0,-.25,0);
                Vec3 offset=start.subtract(entity.getPosition(partial));poses.translate(offset.x,offset.y,offset.z);vector=end.subtract(start);
            }
            GodlyGeometry.render(poses,batch,entity.kind(),vector,age,entity.lifetime());poses.popPose();
        } else if(entity.kind()>=ArtVisualEntity.THREAD_LINE && entity.kind()<=ArtVisualEntity.MIRROR_SHARD || entity.kind()==ArtVisualEntity.GATE_TRANSIT) {
            poses.pushPose();
            if(entity.kind()==ArtVisualEntity.TWIN_GATE)poses.mulPose(com.mojang.math.Axis.YP.rotation((float)entity.end().z));
            if((entity.kind()==ArtVisualEntity.MIRROR_WARD || entity.kind()==ArtVisualEntity.CRYSTAL_BULWARK) && entity.caster()!=null){
                Vec3 offset=entity.caster().getPosition(partial).add(0,entity.kind()==ArtVisualEntity.MIRROR_WARD?1:0,0).subtract(entity.getPosition(partial));poses.translate(offset.x,offset.y,offset.z);
            }
            Vec3 vector=entity.end(partial);var mc=net.minecraft.client.Minecraft.getInstance();var player=mc.player;
            if((entity.kind()==ArtVisualEntity.PRISM_RAY || entity.kind()==ArtVisualEntity.PLASMA_LANCE || entity.kind()==ArtVisualEntity.THREAD_LINE) && player!=null && mc.getCameraEntity()==player && mc.options.getCameraType().isFirstPerson() && entity.getPosition(partial).distanceToSqr(player.getEyePosition(partial))<.75*.75){
                Vec3 axis=vector.normalize(),side=axis.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
                Vec3 offset=axis.scale(Math.min(.9,vector.length()*.3)).add(side.scale(.35)).add(0,-.25,0);poses.translate(offset.x,offset.y,offset.z);vector=vector.subtract(offset);
            }
            MythicGeometry.render(poses,batch,entity.kind(),vector,age,entity.lifetime(),entity.style());poses.popPose();
        } else {
            poses.pushPose();
            if(entity.kind()==ArtVisualEntity.HEALING_PULSE && entity.caster()!=null){Vec3 offset=entity.caster().getPosition(partial).subtract(entity.getPosition(partial));poses.translate(offset.x,offset.y,offset.z);}
            Vec3 vector=entity.end(partial);var mc=net.minecraft.client.Minecraft.getInstance();var player=mc.player;
            // Keep the near muzzle outside the camera; the world-space endpoint is unchanged.
            if(entity.kind()==ArtVisualEntity.RAILBOLT && player!=null && mc.getCameraEntity()==player && mc.options.getCameraType().isFirstPerson() && entity.getPosition(partial).distanceToSqr(player.getEyePosition(partial))<.75*.75){
                Vec3 axis=vector.normalize(),side=axis.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
                Vec3 offset=axis.scale(Math.min(.9,vector.length()*.3)).add(side.scale(.35)).add(0,-.25,0);poses.translate(offset.x,offset.y,offset.z);vector=vector.subtract(offset);
            }
            ElementalGeometry.render(poses,batch,entity.kind(),vector,age,entity.lifetime(),entity.style());poses.popPose();
        }
        batch.flush(buffers);
        super.render(entity, yaw, partial, poses, buffers, light);
    }
}
