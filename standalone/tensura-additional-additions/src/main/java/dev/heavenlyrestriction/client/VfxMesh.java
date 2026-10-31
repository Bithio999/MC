package dev.heavenlyrestriction.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.heavenlyrestriction.HeavenlyRestriction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Standard full-bright surfaces and premultiplied additive optics; no framebuffer/shader dependency. */
final class VfxMesh {
    static final ResourceLocation WHITE=tex("white"),BEAM=tex("beam"),GLOW=HeavenlyRestriction.id("textures/effect/aura_glow.png"),
        SILK=tex("silk"),CIRCUIT=tex("circuit"),FROST=tex("frost"),LEAF=tex("leaf"),MIRROR=tex("mirror"),
        CLOCK=tex("clock"),GATE=tex("gate"),RIFT=tex("rift"),WAVE=tex("wave"),STONE=tex("stone"),ICE=tex("ice"),
        BARK=tex("bark"),LEAF_SPRITE=tex("leaf_sprite"),CLOUD=tex("cloud"),FLAME=tex("flame");
    private static int lod;
    private VfxMesh(){}
    private static ResourceLocation tex(String name){return HeavenlyRestriction.id("textures/effect/vfx_"+name+".png");}
    static void begin(Vec3 position){double d=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(position);lod=d>64*64?2:d>32*32?1:0;}
    static int segments(int n){return Math.max(6,n>>lod);}
    static int decorations(int n){var particles=Minecraft.getInstance().options.particles().get();return particles==ParticleStatus.MINIMAL?0:Math.max(1,n>>(lod+(particles==ParticleStatus.DECREASED?1:0)));}
    static double smooth(double x){x=Math.clamp(x,0,1);return x*x*(3-2*x);}
    static double fade(float age,int life){return smooth(Math.min(age/4d,(life-age)/7d));}
    static int tint(int c,double intensity){int out=(int)(((c>>>24)&255)*Math.clamp(intensity,0,1))<<24;for(int shift:new int[]{16,8,0})out|=(int)(((c>>shift)&255)*Math.clamp(intensity,0,1))<<shift;return out;}
    static int opacity(int c,double a){return ((int)(255*Math.clamp(a,0,1))<<24)|(c&0xFFFFFF);}
    static Vec3 polar(double a,double r,double y){return new Vec3(Math.cos(a)*r,y,Math.sin(a)*r);}
    static void quad(PoseStack.Pose p,VertexConsumer mesh,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,boolean both){
        quadUV(p,mesh,a,b,c,d,color,both,0,0,1,1);
    }
    static void quadUV(PoseStack.Pose p,VertexConsumer mesh,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,boolean both,float u0,float v0,float u1,float v1){
        Vec3 normal=b.subtract(a).cross(c.subtract(a)).normalize();Vec3[] points=both?new Vec3[]{a,b,c,d,d,c,b,a}:new Vec3[]{a,b,c,d};
        float[][] uv={{u0,v0},{u1,v0},{u1,v1},{u0,v1},{u0,v1},{u1,v1},{u1,v0},{u0,v0}};
        for(int n=0;n<points.length;n++)mesh.addVertex(p,(float)points[n].x,(float)points[n].y,(float)points[n].z).setColor(color).setUv(uv[n][0],uv[n][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(p,(float)normal.x,(float)normal.y,(float)normal.z);
    }
    static void surface(PoseStack p,MultiBufferSource b,ResourceLocation texture,Vec3 a,Vec3 q,Vec3 c,Vec3 d,int color){quad(p.last(),b.getBuffer(VfxMaterials.surface(texture)),a,q,c,d,color,false);}
    static void seal(PoseStack p,MultiBufferSource b,ResourceLocation texture,Vec3 center,double radius,double spin,double tilt,int color){
        if(radius<=.001)return;p.pushPose();p.translate(center.x,center.y,center.z);p.mulPose(Axis.YP.rotation((float)spin));p.mulPose(Axis.XP.rotation((float)tilt));
        quad(p.last(),b.getBuffer(VfxMaterials.additive(texture)),new Vec3(-radius,0,-radius),new Vec3(radius,0,-radius),new Vec3(radius,0,radius),new Vec3(-radius,0,radius),color,true);p.popPose();
    }
    static void glow(PoseStack p,MultiBufferSource b,Vec3 center,double size,int color){
        if(size<=.001)return;p.pushPose();p.translate(center.x,center.y,center.z);p.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        quad(p.last(),b.getBuffer(VfxMaterials.additive(GLOW)),new Vec3(-size,-size,0),new Vec3(size,-size,0),new Vec3(size,size,0),new Vec3(-size,size,0),color,true);p.popPose();
    }
    static void billboard(PoseStack p,MultiBufferSource b,ResourceLocation texture,Vec3 center,double width,double height,double rotation,int color,boolean additive){
        p.pushPose();p.translate(center.x,center.y,center.z);p.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());p.mulPose(Axis.ZP.rotation((float)rotation));
        quad(p.last(),b.getBuffer(additive?VfxMaterials.additive(texture):VfxMaterials.surface(texture)),new Vec3(-width,-height,0),new Vec3(width,-height,0),new Vec3(width,height,0),new Vec3(-width,height,0),color,additive);p.popPose();
    }
    static void beam(PoseStack p,MultiBufferSource b,Vec3 start,Vec3 end,double width,int color){
        Vec3 axis=end.subtract(start).normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(width),up=axis.cross(side).normalize().scale(width);
        var mesh=b.getBuffer(VfxMaterials.additive(BEAM));quad(p.last(),mesh,start.add(side),end.add(side),end.subtract(side),start.subtract(side),color,true);quad(p.last(),mesh,start.add(up),end.add(up),end.subtract(up),start.subtract(up),color,true);
    }
    static void ring(PoseStack p,MultiBufferSource b,Vec3 center,double radius,double width,double rotation,int plane,int color){
        int count=segments(64);var mesh=b.getBuffer(VfxMaterials.additive(BEAM));
        for(int n=0;n<count;n++){double a=n*Math.PI*2/count+rotation,q=(n+1)*Math.PI*2/count+rotation;
            quad(p.last(),mesh,plane(a,radius-width,plane).add(center),plane(q,radius-width,plane).add(center),plane(q,radius+width,plane).add(center),plane(a,radius+width,plane).add(center),color,true);}
    }
    private static Vec3 plane(double a,double r,int plane){return plane==0?polar(a,r,0):plane==1?new Vec3(Math.cos(a)*r,Math.sin(a)*r,0):new Vec3(0,Math.cos(a)*r,Math.sin(a)*r);}
    static void tube(PoseStack p,MultiBufferSource b,List<Vec3> path,double root,double tip,ResourceLocation texture,int color){
        int sides=segments(8);for(int n=0;n<path.size()-1;n++){
            Vec3 a=path.get(n),q=path.get(n+1),axis=q.subtract(a).normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();double r0=root+(tip-root)*n/(path.size()-1d),r1=root+(tip-root)*(n+1)/(path.size()-1d);
            for(int k=0;k<sides;k++){double x=k*Math.PI*2/sides,y=(k+1)*Math.PI*2/sides;Vec3 d0=side.scale(Math.cos(x)).add(up.scale(Math.sin(x))),d1=side.scale(Math.cos(y)).add(up.scale(Math.sin(y)));
                quadUV(p.last(),b.getBuffer(VfxMaterials.surface(texture)),a.add(d0.scale(r0)),q.add(d0.scale(r1)),q.add(d1.scale(r1)),a.add(d1.scale(r0)),tint(color,.65+.35*Math.abs(Math.sin(x))),false,(float)n/3,(float)k/sides,(float)(n+1)/3,(float)(k+1)/sides);}
        }
    }
    static void crystal(PoseStack p,MultiBufferSource b,Vec3 base,Vec3 tip,double radius,int color){
        int sides=6;Vec3 axis=tip.subtract(base),side=axis.normalize().cross(Math.abs(axis.normalize().y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.normalize().cross(side).normalize();
        for(int n=0;n<sides;n++){double a=n*Math.PI/3,c=(n+1)*Math.PI/3;Vec3 q=base.add(axis.scale(.28)).add(side.scale(Math.cos(a)*radius)).add(up.scale(Math.sin(a)*radius)),d=base.add(axis.scale(.28)).add(side.scale(Math.cos(c)*radius)).add(up.scale(Math.sin(c)*radius));
            surface(p,b,ICE,base,q,d,base,tint(color,.75+.2*Math.cos(a)));surface(p,b,ICE,q,tip,d,d,tint(color,.8+.2*Math.sin(a)));
            beam(p,b,q,tip,.018,tint(0xFFE1FAFF,.5));}
        beam(p,b,base.add(axis.scale(.2)),tip,.045,tint(color,.7));
    }
    static void sphere(PoseStack p,MultiBufferSource b,Vec3 center,double radius,int color){
        int lat=segments(16),lon=segments(32);var mesh=b.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for(int y=0;y<lat;y++)for(int x=0;x<lon;x++){
            double a=-Math.PI/2+y*Math.PI/lat,q=a+Math.PI/lat,c=x*Math.PI*2/lon,d=c+Math.PI*2/lon;
            quad(p.last(),mesh,spherical(a,c,radius).add(center),spherical(q,c,radius).add(center),spherical(q,d,radius).add(center),spherical(a,d,radius).add(center),color,false);}
    }
    private static Vec3 spherical(double a,double b,double r){return new Vec3(Math.cos(a)*Math.cos(b)*r,Math.sin(a)*r,Math.cos(a)*Math.sin(b)*r);}
    static void motes(PoseStack p,MultiBufferSource b,double radius,float age,int count,int color){
        count=decorations(count);for(int n=0;n<count;n++){double phase=age*.018+n*.618,part=phase-Math.floor(phase),a=n*2.399963+age*.008,r=radius*(.25+.7*((n*17)%Math.max(1,count))/(double)Math.max(1,count));glow(p,b,polar(a,r,part*3),.16*Math.sin(part*Math.PI),tint(color,.75));}
    }
    static void bolt(PoseStack p,MultiBufferSource b,Vec3 from,Vec3 to,double width,float age,int seed,int color){
        Vec3 delta=to.subtract(from),side=delta.normalize().cross(Math.abs(delta.normalize().y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=delta.normalize().cross(side).normalize();int count=segments(Math.min(40,Math.max(8,(int)(delta.length()*2))));
        Vec3 previous=from;for(int n=1;n<=count;n++){double t=n/(double)count,envelope=Math.sin(t*Math.PI),noise=Math.sin(n*7.19+seed*2.3+age*.15)*.23;
            Vec3 end=from.add(delta.scale(t)).add(side.scale(noise*envelope)).add(up.scale(Math.cos(n*4.31+seed)*.15*envelope));beam(p,b,previous,end,width,color);if(n%6==0)beam(p,b,end,end.add(side.scale(Math.sin(n+seed)*.6)).add(up.scale(.3)),width*.4,tint(color,.5));previous=end;}
    }
}
