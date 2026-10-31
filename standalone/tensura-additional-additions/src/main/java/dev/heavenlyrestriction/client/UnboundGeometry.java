package dev.heavenlyrestriction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** Restrained stance, then a broad cel-lit crescent with a dark cutting edge and pressure wake. */
final class UnboundGeometry {
    private UnboundGeometry(){}
    static void stance(PoseStack p,MultiBufferSource b,Vec3 look,float age){
        double pulse=.7+.15*Math.sin(age*.25),appear=Math.min(1,age/5d);
        SpectacleMesh.pressure(p,b,new Vec3(0,.035,0),1.25*appear,.08,0xFFD8BF83,pulse*.45);
        Vec3 forward=new Vec3(look.x,0,look.z).normalize(),side=new Vec3(-forward.z,0,forward.x);
        for(int n=-1;n<=1;n+=2){
            Vec3 at=forward.scale(.6).add(side.scale(n*.65));
            SpectacleMesh.lance(p,b,at.add(0,.12,0),at.add(0,1.7*appear,0),.045,0xFFF4ECD5,.7);
            SpectacleMesh.stroke(p,b,at.add(side.scale(-n*.35)).add(0,1.5,0),at.add(0,1.7,0),.035,0xFFFFFFFF,.7);
        }
    }
    static void slash(PoseStack p,MultiBufferSource b,Vec3 travel,float age){
        if(travel.lengthSqr()<.001)return;
        Vec3 axis=travel.normalize(),side=axis.cross(Math.abs(axis.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
        double fade=Math.pow(Math.max(0,1-age/18d),1.6),reach=travel.length()*SpectacleMesh.out(age/7d);
        for(int layer=0;layer<3;layer++){
            double radius=3-layer*.32,depth=reach-layer*.65;
            for(int n=0;n<72;n++){
                double t=n/72d,v=(n+1d)/72,a=-Math.PI*.84+t*Math.PI*1.68,q=-Math.PI*.84+v*Math.PI*1.68;
                Vec3 edgeA=axis.scale(depth-Math.cos(a)*1.6).add(side.scale(Math.sin(a)*radius)).add(up.scale(Math.cos(a)*radius*.8));
                Vec3 edgeB=axis.scale(depth-Math.cos(q)*1.6).add(side.scale(Math.sin(q)*radius)).add(up.scale(Math.cos(q)*radius*.8));
                double width=Math.pow(Math.sin(t*Math.PI),.6)*(.9-layer*.16),widthB=Math.pow(Math.sin(v*Math.PI),.6)*(.9-layer*.16);
                SpectacleMesh.face(p,b,edgeA,edgeB,edgeB.subtract(axis.scale(widthB)),edgeA.subtract(axis.scale(width)),SpectacleMesh.light(layer==0?0xFF171B25:layer==1?0xFFD6B36A:0xFFFFF8DB,fade));
                if(layer==2)SpectacleMesh.stroke(p,b,edgeA,edgeB,.055,0xFFFFFFFF,fade);
            }
        }
        for(int n=-2;n<=2;n++){Vec3 offset=side.scale(n*.65).add(up.scale(n*.12));SpectacleMesh.stroke(p,b,offset,axis.scale(reach).add(offset),n==0?.12:.045,0xFFEDE6C6,fade*.65);}
        if(age<9)SpectacleMesh.burst(p,b,Vec3.ZERO,age,10,3,0xFFF4DDA8,27);
        if(age>=7)SpectacleMesh.burst(p,b,travel,age-7,11,4,0xFFFFEAC0,71);
    }
}
