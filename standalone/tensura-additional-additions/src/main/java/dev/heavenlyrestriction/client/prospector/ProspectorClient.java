package dev.heavenlyrestriction.client.prospector;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.heavenlyrestriction.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class ProspectorClient {
    private static final OreSurvey SURVEY=new OreSurvey();private static Object lastLevel;
    public static OreSurvey survey(){return SURVEY;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){var mc=Minecraft.getInstance();if(mc.level!=lastLevel){SURVEY.clear();ProspectorOres.clear();lastLevel=mc.level;}if(mc.level==null || ProspectorMining.active(mc.player).isEmpty()){SURVEY.clear();return;}if(!mc.isPaused())SURVEY.tick(mc.level,mc.player.position());}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){SURVEY.clear();lastLevel=null;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;var mc=Minecraft.getInstance();if(mc.player==null || mc.options.hideGui || ProspectorMining.active(mc.player).isEmpty())return;
        var matrix=RenderSystem.getModelViewStack();matrix.pushMatrix();matrix.set(event.getModelViewMatrix());RenderSystem.applyModelViewMatrix();var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(ProspectorMaterial.MARKERS);var pose=new PoseStack();var camera=event.getCamera().getPosition();
        try{for(var marker:SURVEY.markers()){if(marker.pos().distToCenterSqr(mc.player.position())>32*32 || !event.getFrustum().isVisible(new AABB(marker.pos()).inflate(.3)))continue;Vec3 at=Vec3.atCenterOf(marker.pos());double distance=at.distanceTo(camera);if(distance<.7)continue;float size=(float)(.18+distance*.004),width=(float)(.009+distance*.0003);int alpha=(int)(125-45*Math.min(1,distance/32)),color=alpha<<24|marker.kind().color();
            pose.pushPose();pose.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(size,size,size);float w=width/size;
            for(int x:new int[]{-1,1})for(int y:new int[]{-1,1}){line(out,pose,x,y,x*.55f,y,w,color);line(out,pose,x,y,x,y*.55f,w,color);}glyph(out,pose,marker.kind().glyph(),w,color);pose.popPose();}
            buffers.endBatch(ProspectorMaterial.MARKERS);
        }finally{matrix.popMatrix();RenderSystem.applyModelViewMatrix();}
    }
    private static void line(VertexConsumer out,PoseStack p,float x,float y,float a,float b,float width,int color){float dx=a-x,dy=b-y,len=(float)Math.hypot(dx,dy);if(len<.001)return;float nx=-dy/len*width,ny=dx/len*width;out.addVertex(p.last(),x+nx,y+ny,0).setColor(color);out.addVertex(p.last(),a+nx,b+ny,0).setColor(color);out.addVertex(p.last(),a-nx,b-ny,0).setColor(color);out.addVertex(p.last(),x-nx,y-ny,0).setColor(color);}
    private static void diamond(VertexConsumer v,PoseStack p,float r,float w,int c){line(v,p,0,r,r,0,w,c);line(v,p,r,0,0,-r,w,c);line(v,p,0,-r,-r,0,w,c);line(v,p,-r,0,0,r,w,c);}
    private static void glyph(VertexConsumer v,PoseStack p,int g,float w,int c){switch(g){
        case 0->{line(v,p,-.25f,-.25f,.25f,-.25f,w,c);line(v,p,.25f,-.25f,.25f,.25f,w,c);line(v,p,.25f,.25f,-.25f,.25f,w,c);line(v,p,-.25f,.25f,-.25f,-.25f,w,c);}
        case 1->{line(v,p,-.2f,-.4f,-.2f,.4f,w,c);line(v,p,.2f,-.4f,.2f,.4f,w,c);}
        case 2->{line(v,p,-.35f,-.3f,0,-.3f,w,c);line(v,p,0,-.3f,0,.3f,w,c);line(v,p,0,.3f,.35f,.3f,w,c);}
        case 3->{diamond(v,p,.42f,w,c);line(v,p,-.5f,0,.5f,0,w,c);}
        case 4->{line(v,p,-.4f,0,.4f,0,w,c);line(v,p,0,-.4f,0,.4f,w,c);}
        case 5->{for(int n=-1;n<=1;n++)line(v,p,n*.3f,-.15f,n*.3f,.15f,w,c);}
        case 6->diamond(v,p,.44f,w,c);
        case 7->{line(v,p,0,.45f,.4f,-.35f,w,c);line(v,p,.4f,-.35f,-.4f,-.35f,w,c);line(v,p,-.4f,-.35f,0,.45f,w,c);}
        case 8->{line(v,p,-.32f,-.4f,.32f,.4f,w,c);line(v,p,-.05f,-.4f,.42f,.2f,w,c);}
        case 9->{diamond(v,p,.42f,w,c);line(v,p,-.15f,-.15f,.15f,.15f,w,c);}
        case 10->{diamond(v,p,.48f,w,c);diamond(v,p,.21f,w,c);line(v,p,-.65f,0,-.48f,0,w,c);line(v,p,.48f,0,.65f,0,w,c);}
        default->{diamond(v,p,.4f,w,c);line(v,p,-.3f,g==12?-.3f:0,.3f,g==13?.3f:0,w,c);}
    }}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){var mc=Minecraft.getInstance();if(mc.player==null || mc.screen!=null || mc.options.hideGui || ProspectorMining.active(mc.player).isEmpty())return;var camera=mc.gameRenderer.getMainCamera();var at=camera.getPosition();var aim=Vec3.directionFromRotation(camera.getXRot(),camera.getYRot());OreSurvey.Marker selected=null;double best=.997;
        for(var marker:SURVEY.markers()){if(marker.pos().distToCenterSqr(mc.player.position())>32*32)continue;var d=Vec3.atCenterOf(marker.pos()).subtract(at);double dot=d.normalize().dot(aim);if(dot>best){best=dot;selected=marker;}}
        if(selected==null)return;var text=Component.translatable("heavenlyrestriction.prospector.deposit",selected.kind().label(),selected.count(),Math.round(Vec3.atCenterOf(selected.pos()).distanceTo(mc.player.position())));var g=event.getGuiGraphics();int width=mc.font.width(text),x=(g.guiWidth()-width)/2,y=g.guiHeight()/2+17;g.fill(x-6,y-3,x+width+6,y+12,0xb0121d26);g.fill(x-6,y-3,x-4,y+12,0xcc000000|selected.kind().color());g.drawString(mc.font,text,x,y,0xffdce6eb,false);
    }
}
