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
public final class ArtVisualRenderer extends software.bernie.geckolib.renderer.GeoEntityRenderer<ArtVisualEntity> {
    private final EffectBatch batch=new EffectBatch();
    private record AimFrame(Vec3 direction,long time) {}
    private final java.util.Map<ArtVisualEntity,AimFrame> aimFrames=new java.util.WeakHashMap<>();
    public ArtVisualRenderer(EntityRendererProvider.Context context) { super(context,new MythsArtModel());addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this)); }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ArtVisualEntity.TYPE.get(), ArtVisualRenderer::new);
        event.registerEntityRenderer(ArtVisualEntity.MYTH_TYPE.get(), ArtVisualRenderer::new);
        event.registerEntityRenderer(ArtVisualEntity.FINAL_TYPE.get(), ArtVisualRenderer::new);
    }
    @Override public ResourceLocation getTextureLocation(ArtVisualEntity entity) { return getGeoModel().getTextureResource(entity); }
    @Override public void renderCubesOfBone(PoseStack p,software.bernie.geckolib.cache.object.GeoBone bone,com.mojang.blaze3d.vertex.VertexConsumer out,int light,int overlay,int color){
        if(!SmoothGeoGeometry.render(p,bone,out,light,overlay,color))super.renderCubesOfBone(p,bone,out,light,overlay,color);
    }
    @Override public net.minecraft.client.renderer.RenderType getRenderType(ArtVisualEntity e,ResourceLocation texture,MultiBufferSource buffers,float partial){return net.minecraft.client.renderer.RenderType.entityTranslucent(texture);}
    @Override public boolean shouldRender(ArtVisualEntity entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z) {
        return entity.kind()==ArtVisualEntity.CAUSAL_RAY && entity.ownedBy(net.minecraft.client.Minecraft.getInstance().player) || super.shouldRender(entity,frustum,x,y,z);
    }
    @Override public void render(ArtVisualEntity entity, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;
        VfxMesh.begin(entity.getPosition(partial));batch.begin();
        if(EschatonGeometry.handles(entity.kind())){EschatonGeometry.render(entity,poses,batch,partial);}
        else if(AnankeGeometry.handles(entity.kind())){AnankeGeometry.render(entity,poses,batch,partial);}
        else if(entity.kind()==ArtVisualEntity.CAUSAL_VERDICT && !entity.barrage().isEmpty()){SingularityGeometry.render(entity,poses,batch,partial);}
        else if(SovereignGeometry.handles(entity.kind())) {
            poses.pushPose();
            if((entity.kind()==ArtVisualEntity.PERENNIAL_SEED || entity.kind()==ArtVisualEntity.COUNTERMAND) && entity.caster()!=null){Vec3 offset=entity.caster().getPosition(partial).subtract(entity.getPosition(partial));poses.translate(offset.x,offset.y,offset.z);}
            SovereignGeometry.render(poses,batch,entity.kind(),entity.end(partial),age,entity.lifetime(),entity.style());poses.popPose();
        } else if(ThothGeometry.handles(entity.kind())) {
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
        renderAuthored(entity,partial,poses,buffers,light);
    }
    @Override protected void applyRotations(ArtVisualEntity e,PoseStack p,float age,float rotation,float partial,float scale){}
    private void drawRig(ArtVisualEntity e,float partial,PoseStack p,MultiBufferSource b,int light){super.render(e,0,partial,p,b,light);}
    private static void scale(PoseStack p,double x,double y,double z){p.scale((float)Math.max(.001,x),(float)Math.max(.001,y),(float)Math.max(.001,z));}
    private void renderAuthored(ArtVisualEntity e,float partial,PoseStack p,MultiBufferSource b,int light){
        var rig=dev.wildhunt.visual.VisualRig.forKind(e.kind());if(rig==null)return;
        // Fluid surfaces, horizons and exact hit footprints remain procedural. Authored
        // armatures provide solid detail; the twenty moving holes retain their optics.
        if(e.kind()==ArtVisualEntity.CAUSAL_VERDICT && !e.barrage().isEmpty())return;
        if(e.distanceToSqr(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())>96*96 && !AnankeGeometry.handles(e.kind()) && !EschatonGeometry.handles(e.kind()))return;
        float age=e.barrage().isEmpty()?e.tickCount+partial:e.barrageAge(partial);Vec3 d=e.end(partial);double r=Math.max(.4,Math.abs(d.x));
        p.pushPose();
        switch(rig.placement()){
            case FINALE->{ }
            case FINALE_SHATTER->{double size=Math.max(.3,e.end().x/5);scale(p,size,size,size);}
            case BEAM,LASH,TIP->{
                Vec3 axis=d.normalize();
                if(e.kind()==ArtVisualEntity.CAUSAL_RAY && e.ownedBy(net.minecraft.client.Minecraft.getInstance().player)){
                    var player=net.minecraft.client.Minecraft.getInstance().player;var frame=aimFrames.get(e);if(frame!=null)axis=frame.direction();
                    Vec3 eye=player.getEyePosition(partial),side=axis.cross(new Vec3(0,1,0)).normalize();
                    Vec3 start=eye.add(axis.scale(.9)).add(side.scale(.35)).add(0,-.25,0),offset=start.subtract(e.getPosition(partial));p.translate(offset.x,offset.y,offset.z);
                }
                else {
                    var mc=net.minecraft.client.Minecraft.getInstance();var player=mc.player;
                    if(player!=null && mc.getCameraEntity()==player && mc.options.getCameraType().isFirstPerson() && e.getPosition(partial).distanceToSqr(player.getEyePosition(partial))<.75*.75){
                        Vec3 side=axis.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
                        Vec3 q=axis.scale(Math.min(.9,d.length()*.3)).add(side.scale(.35)).add(0,-.25,0);p.translate(q.x,q.y,q.z);
                    }
                }
                if(rig.placement()==dev.wildhunt.visual.VisualRig.Placement.TIP){Vec3 q=d.scale(VfxMesh.smooth(age/9d)).subtract(axis.scale(.85));p.translate(q.x,q.y,q.z);}
                else if(e.kind()==ArtVisualEntity.CAUSAL_RAY){Vec3 q=axis.scale(Math.min(1,d.length()*.2));p.translate(q.x,q.y,q.z);}
                p.mulPose(new org.joml.Quaternionf().rotationTo(0,rig.placement()==dev.wildhunt.visual.VisualRig.Placement.LASH?0:1,rig.placement()==dev.wildhunt.visual.VisualRig.Placement.LASH?1:0,(float)axis.x,(float)axis.y,(float)axis.z));
                if(rig.placement()!=dev.wildhunt.visual.VisualRig.Placement.LASH)scale(p,.65,.65,.65);
            }
            case CRYSTAL->{scale(p,1,Math.max(.3,d.y/1.4),1);}
            case FALL->{Vec3 q=d.scale(Math.pow(Math.clamp(age/e.lifetime(),0,1),2));p.translate(q.x,q.y,q.z);p.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));}
            case WAVE->{Vec3 q=new Vec3(d.x,0,d.z).scale(Math.clamp(age/e.lifetime(),0,1));p.translate(q.x,q.y,q.z);p.mulPose(com.mojang.math.Axis.YP.rotation((float)Math.atan2(d.x,d.z)));scale(p,Math.max(1,d.y),1,1);}
            case FOLLOW,MARKER,LIBRARY->{
                if(e.caster()!=null){Vec3 q=e.caster().getPosition(partial).subtract(e.getPosition(partial));p.translate(q.x,q.y,q.z);}
                if(rig.placement()==dev.wildhunt.visual.VisualRig.Placement.MARKER)p.translate(0,e.kind()==ArtVisualEntity.PERENNIAL_SEED?1.4:1.2,0);
                else if(rig.placement()==dev.wildhunt.visual.VisualRig.Placement.LIBRARY)p.translate(0,2.5,0);
                scale(p,Math.min(1.5,r/0.85),1,Math.min(1.5,r/0.85));
            }
            case RADIAL->{scale(p,Math.min(1.5,r/0.85),1,Math.min(1.5,r/0.85));}
            case HORIZON->{p.translate(0,1,0);scale(p,r*.22,r*.22,r*.22);}
            case ANCHOR->{if(e.kind()==ArtVisualEntity.CAUSAL_ANCHOR)p.translate(0,.15,0);}
            case GATE->{
                p.mulPose(com.mojang.math.Axis.YP.rotation((float)d.z));
                // Keep the frame centered on the 2.8-block opening as its width changes.
                double vertical=1.4/(13/16d);p.translate(0,1.4-(18/16d)*vertical,0);scale(p,r/(13/16d),vertical,1);
            }
            case SKY->{p.translate(0,5,0);scale(p,r*.7,1,r*.7);}
            case SINGULARITY->{p.translate(0,4,0);scale(p,2,2,2);}
            case BREAKER->{double radius=r*Math.clamp(age/40d,0,1);scale(p,radius/0.85,1,radius/0.85);}
            case EDGE->{Vec3 q=d.scale(.5);p.translate(q.x,q.y,q.z);p.mulPose(com.mojang.math.Axis.YP.rotation((float)Math.atan2(-d.z,d.x)));scale(p,Math.max(1,d.length()*.35),1,1);}
            case BLADE->{p.mulPose(com.mojang.math.Axis.YP.rotation((float)d.z));p.mulPose(com.mojang.math.Axis.ZP.rotation((float)(-1.2+Math.clamp(age/14d,0,1)*2.4)));scale(p,1,r/1.5,1);}
            case BOSSFAN->{Vec3 forward=planPoint(e.barrage(),"forward");p.translate(forward.x*2,1.7,forward.z*2);p.mulPose(new org.joml.Quaternionf().rotationTo(0,1,0,(float)forward.x,(float)forward.y,(float)forward.z));scale(p,1.7,1.7,1.7);}
            case STARS->{
                var plan=e.barrage();for(int n=0;n<plan.getInt("warned");n++){
                    double t=age-(28+n*12);if(age<n*12 || t>8)continue;Vec3 q=dev.wildhunt.AnankeRites.star(plan,n);p.pushPose();p.translate(q.x,q.y+Math.max(0,12*(1-Math.max(0,t)/4)),q.z);p.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));scale(p,1.5,1.5,1.5);drawRig(e,partial,p,b,light);p.popPose();
                }p.popPose();return;
            }
            case PROCESSION->{if(age>=30){p.popPose();return;}scale(p,2,2,2);}
            case DAWN->{if(age>=82){p.popPose();return;}p.translate(0,7-.75,0);double s=2+VfxMesh.smooth(age/75d)*3;scale(p,s,s,s);}
            case ASCEND->{scale(p,3,3,3);}
        }
        drawRig(e,partial,p,b,light);p.popPose();
    }
    private static Vec3 planPoint(net.minecraft.nbt.CompoundTag t,String key){return new Vec3(t.getDouble(key+"_x"),t.getDouble(key+"_y"),t.getDouble(key+"_z"));}
}
