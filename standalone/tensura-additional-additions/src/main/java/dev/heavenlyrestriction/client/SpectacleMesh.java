package dev.heavenlyrestriction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.heavenlyrestriction.HeavenlyRestriction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import static dev.heavenlyrestriction.client.VfxMesh.*;

/** Cel-shaped world geometry. Straight alpha keeps luminous colors from being attenuated twice. */
final class SpectacleMesh {
    static final int INK=0xFF120C27,WHITE=0xFFFFF9EF;
    static final ResourceLocation STREAK=HeavenlyRestriction.id("textures/effect/anime_streak.png"),
        FLARE=HeavenlyRestriction.id("textures/effect/anime_flare.png"),SMOKE=HeavenlyRestriction.id("textures/effect/anime_smoke.png"),HALO=HeavenlyRestriction.id("textures/effect/anime_halo.png");
    private SpectacleMesh(){}
    static int light(int color,double alpha){return opacity(color,alpha);}
    static double out(double t){return 1-Math.pow(1-Math.clamp(t,0,1),3);}
    static double envelope(double age,double duration){return age<0 || age>duration?0:Math.pow(1-age/duration,1.5);}
    static void face(PoseStack p,MultiBufferSource b,Vec3 a,Vec3 q,Vec3 c,Vec3 d,int color){
        quad(p.last(),b.getBuffer(VfxMaterials.surface(VfxMesh.WHITE)),a,q,c,d,color,false);
    }
    /** An actual four-sided tapered solid, with manually separated cel-lit faces. */
    static void lance(PoseStack p,MultiBufferSource b,Vec3 root,Vec3 tip,double width,int color,double alpha){
        if(width<.001 || alpha<.002 || root.distanceToSqr(tip)<.0001)return;
        Vec3 axis=tip.subtract(root).normalize(),side=axis.cross(Math.abs(axis.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(width),up=axis.cross(side).normalize().scale(width*.45),neck=root.lerp(tip,.2);
        Vec3[] rim={neck.add(side),neck.add(up),neck.subtract(side),neck.subtract(up)};
        int[] colors={color,WHITE,color,INK};
        for(int n=0;n<4;n++){Vec3 a=rim[n],q=rim[(n+1)%4];face(p,b,root,a,q,root,light(colors[n],alpha));face(p,b,a,tip,q,q,light(colors[n],alpha));}
    }
    /** Saturated, camera-independent crossed ribbons with a narrow white-hot center. */
    static void stroke(PoseStack p,MultiBufferSource b,Vec3 a,Vec3 q,double width,int color,double alpha){
        if(width<.001 || alpha<.002)return;
        Vec3 axis=q.subtract(a).normalize(),side=axis.cross(Math.abs(axis.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
        var mesh=b.getBuffer(VfxMaterials.radiant(STREAK));
        for(Vec3 across:new Vec3[]{side,up})for(int layer=0;layer<2;layer++){
            Vec3 w=across.scale(width*(layer==0?1:.24));
            quad(p.last(),mesh,a.add(w),q.add(w),q.subtract(w),a.subtract(w),light(layer==0?color:WHITE,alpha*(layer==0?.8:1)),false);
        }
    }
    /** Curved light with a saturated sheath and white core; distinct from solid crystal fragments. */
    static void plume(PoseStack p,MultiBufferSource b,Vec3 root,Vec3 tip,double width,int color,double alpha,double phase){
        if(width<.001 || alpha<.002 || root.distanceToSqr(tip)<.001)return;
        Vec3 axis=tip.subtract(root).normalize(),side=axis.cross(Math.abs(axis.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
        var mesh=b.getBuffer(VfxMaterials.radiant(STREAK));int count=12;
        for(int plane=0;plane<3;plane++){
            double angle=plane*Math.PI/3;Vec3 across=side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            for(int n=0;n<count;n++){
                double u=n/(double)count,v=(n+1d)/count;
                Vec3 a=root.lerp(tip,u).add(side.scale(Math.sin(u*Math.PI)*Math.sin(phase+u*3)*width*.22)),q=root.lerp(tip,v).add(side.scale(Math.sin(v*Math.PI)*Math.sin(phase+v*3)*width*.22));
                double wa=width*Math.pow(Math.sin(Math.PI*u),.6)*(1-u*.65),wb=width*Math.pow(Math.sin(Math.PI*v),.6)*(1-v*.65);
                for(int layer=0;layer<2;layer++){double fraction=layer==0?1:.22;Vec3 x=across.scale(wa*fraction),y=across.scale(wb*fraction);quad(p.last(),mesh,a.add(x),q.add(y),q.subtract(y),a.subtract(x),light(layer==0?color:WHITE,alpha*(layer==0?.45:.7)),false);}
            }
        }
    }
    static void sprite(PoseStack p,MultiBufferSource b,ResourceLocation texture,Vec3 at,double x,double y,double rotation,int color,double alpha){
        if(alpha<.002 || x<.001 || y<.001)return;
        var cameraRelative=p.last().pose().transformPosition((float)at.x,(float)at.y,(float)at.z,new org.joml.Vector3f());alpha*=Math.clamp(cameraRelative.length()/3d,.15,1);
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());p.mulPose(Axis.ZP.rotation((float)rotation));
        quad(p.last(),b.getBuffer(VfxMaterials.radiant(texture)),new Vec3(-x,-y,0),new Vec3(x,-y,0),new Vec3(x,y,0),new Vec3(-x,y,0),light(color,alpha),false);p.popPose();
    }
    static void halo(PoseStack p,MultiBufferSource b,Vec3 at,double size,int color,double alpha){sprite(p,b,HALO,at,size,size,0,color,alpha);}
    /** Swept blade volume, rather than a wire following an arc. */
    static void slash(PoseStack p,MultiBufferSource b,Vec3 center,double radius,double angle,double half,double breadth,double height,int color,double alpha){
        int count=segments(64);if(alpha<.002)return;
        for(int n=0;n<count;n++){
            double t=n/(double)count,v=(n+1d)/count,a=angle-half+t*half*2,q=angle-half+v*half*2;
            double wa=breadth*Math.pow(Math.sin(Math.PI*t),.65),wb=breadth*Math.pow(Math.sin(Math.PI*v),.65);
            Vec3 outerA=center.add(polar(a,radius,height)),outerB=center.add(polar(q,radius,height));
            Vec3 innerA=center.add(polar(a,radius-wa,height-.35*wa)),innerB=center.add(polar(q,radius-wb,height-.35*wb));
            face(p,b,innerA,innerB,outerB,outerA,light(color,.94*alpha));
            face(p,b,innerA.add(0,-.22,0),outerA,outerB,innerB.add(0,-.22,0),light(INK,.92*alpha));
            stroke(p,b,outerA,outerB,.09+.04*breadth,color,alpha);
        }
    }
    static void ribbon(PoseStack p,MultiBufferSource b,Vec3 a,Vec3 q,Vec3 side,double width,int color,double alpha){
        Vec3 w=side.normalize().scale(width);face(p,b,a.add(w),q.add(w),q.subtract(w),a.subtract(w),light(color,alpha));
    }
    /** A single ground pressure front: a readable broad band with a bright leading lip. */
    static void pressure(PoseStack p,MultiBufferSource b,Vec3 at,double radius,double width,int color,double alpha){
        if(radius<.01 || alpha<.002)return;int count=Math.max(48,segments(96));
        for(int n=0;n<count;n++){
            double a=n*Math.TAU/count,q=(n+1d)*Math.TAU/count;double inner=Math.max(0,radius-width);
            face(p,b,at.add(polar(a,inner,0)),at.add(polar(q,inner,0)),at.add(polar(q,radius,.3)),at.add(polar(a,radius,.3)),light(color,alpha*.55));
            stroke(p,b,at.add(polar(a,radius,.3)),at.add(polar(q,radius,.3)),.12,color,alpha);
        }
    }
    /** Localized impact frame, followed by cel-lit ejecta and a ground shock front. */
    static void burst(PoseStack p,MultiBufferSource b,Vec3 at,double age,double duration,double size,int color,int seed){
        double f=envelope(age,duration);if(f<=0)return;double travel=out(age/Math.min(14,duration)),flash=Math.exp(-age*.45);
        if(decorations(1)>0){
            sprite(p,b,FLARE,at,size*(.7+travel*.6),size*(.7+travel*.6),seed*2.39996,WHITE,.85*Math.exp(-age*.14));halo(p,b,at,size*.55,color,.2*f);
            if(age>3)for(int n=0;n<3;n++){double a=n*Math.TAU/3+seed*.3;Vec3 cloud=at.add(polar(a,size*.28*travel,age*.04));billboard(p,b,SMOKE,cloud,size*.22*travel,size*.16*travel,a,light(INK,.22*f),false);}
        }
        int count=segments(12);for(int n=0;n<count;n++){
            double a=n*2.399963+seed*.17,y=Math.sin(n*4.17+seed)*.65;
            Vec3 direction=new Vec3(Math.cos(a),y,Math.sin(a)).normalize();double reach=size*(.45+.35*Math.abs(Math.sin(n*8.13+seed)))*travel;
            Vec3 root=at.add(direction.scale(reach*.35)),tip=at.add(direction.scale(reach));
            lance(p,b,root,tip,(.14+.2*f)*Math.min(2,size/5),n%3==0?WHITE:color,f);
        }
        if(flash>.02)plume(p,b,at.add(0,-size*.1,0),at.add(0,size*(.65+travel),0),size*.13,color,flash,seed);
        pressure(p,b,at.add(0,-.5,0),size*travel,size*.11,color,f*.8);
    }
    /** Broad faceted fragments with a short direction trail; no particle storm. */
    static void shard(PoseStack p,MultiBufferSource b,Vec3 at,Vec3 direction,double size,int color,double alpha){
        lance(p,b,at.subtract(direction.scale(size*.4)),at.add(direction.scale(size)),size*.2,color,alpha);
        stroke(p,b,at.subtract(direction.scale(size*2)),at,.08*size,color,alpha*.6);
    }
}
