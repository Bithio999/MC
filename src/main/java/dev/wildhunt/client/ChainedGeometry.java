package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** A brief torn chain and pressure wake, sized to the actual collision-limited journey. */
final class ChainedGeometry {
    private ChainedGeometry(){}
    static void render(PoseStack p,MultiBufferSource b,Vec3 travel,float age){
        double fade=Math.pow(Math.max(0,1-age/14d),1.4);if(fade<.001 || travel.lengthSqr()<.001)return;
        Vec3 axis=travel.normalize(),side=axis.cross(Math.abs(axis.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
        for(int n=0;n<12;n++){
            double t=(n+.5)/12d;Vec3 at=travel.scale(t).add(up.scale(Math.sin(t*Math.PI)*.2));double separation=Math.min(1,age/8d)*.8;
            Vec3 across=n%2==0?side:up,normal=axis.cross(across).normalize();
            for(int half=-1;half<=1;half+=2){
                Vec3 old=null;for(int k=0;k<=12;k++){double a=(half<0?Math.PI:0)+k*Math.PI/12;Vec3 q=at.add(axis.scale(Math.cos(a)*.47)).add(across.scale(Math.sin(a)*.27+half*separation)).add(normal.scale(age*.015*half));
                    if(old!=null){SpectacleMesh.ribbon(p,b,old,q,normal,.075,0xFF191B25,fade);SpectacleMesh.stroke(p,b,old,q,.035,0xFFD9E4E5,.8*fade);}old=q;
                }
            }
        }
        for(int n=0;n<3;n++){Vec3 offset=side.scale((n-1)*.36);SpectacleMesh.stroke(p,b,offset.add(axis.scale(age*.25)),travel.add(offset),n==1?.10:.045,0xFFE5EEDF,fade*.6);}
        SpectacleMesh.burst(p,b,travel,age,14,4,0xFFE4D4AD,11);
    }
}
