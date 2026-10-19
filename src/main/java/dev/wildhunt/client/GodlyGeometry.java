package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.*;
import dev.wildhunt.ArtVisualEntity;
import net.minecraft.world.phys.Vec3;

/** Thick orbital bands, a dark gravity core, clockwork anchors and spatial shock fronts. */
final class GodlyGeometry {
    private GodlyGeometry() {}
    static void render(PoseStack.Pose pose,VertexConsumer mesh,int kind,Vec3 data,float age,int life){
        double fade=Math.clamp(Math.min(age/6d,(life-age)/10d),0,1),r=data.x;
        if(kind==ArtVisualEntity.CAUSAL_RAY){
            Vec3 axis=data.normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
            for(int n=0;n<3;n++){
                double w=(n==0?.5:n==1?.23:.075)*fade;int color=n==0?0x809B64FF:n==1?0xDDF3C66B:0xFFFFFFFF;
                for(var normal:new Vec3[]{side,up})quad(pose,mesh,normal.scale(w),normal.scale(-w),data.subtract(normal.scale(w)),data.add(normal.scale(w)),color);
            }
            return;
        }
        if(kind==ArtVisualEntity.CAUSAL_IMPACT){
            double grow=Math.clamp(age/16d,0,1),radius=r*(.25+grow*.75);
            for(int plane=0;plane<3;plane++)ring(pose,mesh,radius,.12*fade+.02,age*.08,plane,0xDDECCEFF,Vec3.ZERO);
            for(int n=0;n<8;n++){
                double a=n*Math.PI/4+age*.02;Vec3 p=new Vec3(Math.cos(a),Math.sin(a),Math.cos(a*2)).scale(radius*.9);
                Vec3 q=p.scale(.35),s=new Vec3(.07*fade,.04*fade,0);quad(pose,mesh,p.add(s),p.subtract(s),q.subtract(s),q.add(s),0xFFF8E1A2);
            }
            return;
        }
        if(kind==ArtVisualEntity.CAUSAL_ANCHOR){
            ring(pose,mesh,r*fade,.1,age*.015,0,0xFFF4D789,new Vec3(0,.08,0));
            ring(pose,mesh,r*.78*fade,.06,-age*.025,0,0xDDC39FFF,new Vec3(0,.12,0));
            dial(pose,mesh,r*fade,age*.035,Vec3.ZERO);
            for(int n=0;n<4;n++){double a=n*Math.PI/2+age*.025;Vec3 p=new Vec3(Math.cos(a)*r,1+Math.sin(age*.08+n)*.3,Math.sin(a)*r);diamond(pose,mesh,p,.2*fade,0xFFF0D28C);}
            return;
        }
        boolean verdict=kind==ArtVisualEntity.CAUSAL_VERDICT;
        Vec3 core=new Vec3(0,verdict?3:1.8,0);double charge=verdict?Math.clamp(age/60d,0,1):1;
        sphere(pose,mesh,core,(verdict?.5+charge*2.1:r*.22)*fade,0xF017102A);
        // Counter-rotating accretion bands: visible from above, below and the side.
        for(int n=0;n<4;n++){
            double rr=(verdict?r*(.2+.16*n):r*(.28+.13*n))*fade;
            ring(pose,mesh,rr,(.18+.07*n)*fade,age*.025*(n%2==0?1:-1),n%3,n%2==0?0xEED5A6FF:0xFFF1C978,core);
        }
        ring(pose,mesh,r*fade,.12,age*.01,0,0xFFF0D79A,new Vec3(0,.12,0));
        ring(pose,mesh,r*.94*fade,.045,-age*.02,0,0xFF9975DE,new Vec3(0,.16,0));
        dial(pose,mesh,r*fade,age*.008,new Vec3(0,.18,0));
        for(int n=0;n<12;n++){
            double a=n*Math.PI/6-age*.018;double rr=r*(.6+.2*Math.sin(age*.04+n));
            Vec3 p=new Vec3(Math.cos(a)*rr,core.y+Math.sin(a*2+age*.03)*r*.22,Math.sin(a)*rr);
            diamond(pose,mesh,p,.16*fade,n%2==0?0xFFEFD397:0xFFD1A5FF);
        }
        if(verdict){
            // A visible warning cage contracts around the three strike intervals.
            double cage=r*(1-.25*charge)*fade;
            ring(pose,mesh,cage,.15,age*.015,1,0xCABC8CFF,core);
            ring(pose,mesh,cage,.15,-age*.015,2,0xCAF3CD84,core);
            for(int n=0;n<6;n++){
                double a=n*Math.PI/3;Vec3 bottom=new Vec3(Math.cos(a)*r,.15,Math.sin(a)*r),top=new Vec3(0,8+charge*6,0),s=new Vec3(.05,0,.05);
                quad(pose,mesh,bottom.add(s),bottom.subtract(s),top.subtract(s),top.add(s),0x77CBA7FF);
            }
        }
    }
    private static Vec3 plane(double x,double z,int plane){return plane==0?new Vec3(x,0,z):plane==1?new Vec3(x,z,0):new Vec3(0,x,z);}
    private static void ring(PoseStack.Pose pose,VertexConsumer mesh,double radius,double width,double rotation,int plane,int color,Vec3 offset){
        for(int n=0;n<96;n++){
            double a=n*Math.PI/48+rotation,b=(n+1)*Math.PI/48+rotation;
            double ripple=1+.018*Math.sin(n*.7+rotation*3);
            Vec3 p=plane(Math.cos(a)*(radius-width)*ripple,Math.sin(a)*(radius-width)*ripple,plane).add(offset),q=plane(Math.cos(a)*(radius+width)*ripple,Math.sin(a)*(radius+width)*ripple,plane).add(offset);
            Vec3 s=plane(Math.cos(b)*(radius+width),Math.sin(b)*(radius+width),plane).add(offset),t=plane(Math.cos(b)*(radius-width),Math.sin(b)*(radius-width),plane).add(offset);
            quad(pose,mesh,p,q,s,t,color);
        }
    }
    private static void dial(PoseStack.Pose pose,VertexConsumer mesh,double r,double angle,Vec3 offset){
        for(int n=0;n<24;n++){
            double a=n*Math.PI/12;Vec3 direction=new Vec3(Math.cos(a),0,Math.sin(a)),side=new Vec3(-Math.sin(a),0,Math.cos(a)).scale(n%2==0?.07:.035);
            Vec3 p=direction.scale(r).add(offset),q=direction.scale(r*(n%2==0?.88:.94)).add(offset);quad(pose,mesh,p.add(side),p.subtract(side),q.subtract(side),q.add(side),0xFFFCE3A9);
        }
        for(int n=0;n<2;n++){
            double a=angle*(n==0?1:12);Vec3 end=new Vec3(Math.cos(a),0,Math.sin(a)).scale(r*(n==0?.65:.85)).add(offset),side=new Vec3(-Math.sin(a),0,Math.cos(a)).scale(.07);
            quad(pose,mesh,offset.add(side),offset.subtract(side),end.subtract(side),end.add(side),0xFFEAC5FF);
        }
    }
    private static Vec3 spherical(double a,double b,double r){return new Vec3(Math.cos(a)*Math.cos(b)*r,Math.sin(a)*r,Math.cos(a)*Math.sin(b)*r);}
    private static void sphere(PoseStack.Pose pose,VertexConsumer mesh,Vec3 center,double r,int color){
        for(int y=0;y<8;y++)for(int x=0;x<24;x++){
            double a=-Math.PI/2+y*Math.PI/8,b=a+Math.PI/8,c=x*Math.PI/12,d=c+Math.PI/12;
            quad(pose,mesh,spherical(a,c,r).add(center),spherical(b,c,r).add(center),spherical(b,d,r).add(center),spherical(a,d,r).add(center),color);
        }
    }
    private static void diamond(PoseStack.Pose p,VertexConsumer v,Vec3 c,double r,int color){
        quad(p,v,c.add(0,r,0),c.add(r,0,0),c.add(0,-r,0),c.add(-r,0,0),color);
        quad(p,v,c.add(0,r,0),c.add(0,0,r),c.add(0,-r,0),c.add(0,0,-r),color);
    }
    private static void quad(PoseStack.Pose p,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color){for(var point:new Vec3[]{a,b,c,d,d,c,b,a})v.addVertex(p,(float)point.x,(float)point.y,(float)point.z).setColor(color);}
}
