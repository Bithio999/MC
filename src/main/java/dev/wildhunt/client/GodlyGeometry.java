package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildhunt.ArtVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import static dev.wildhunt.client.VfxMesh.*;

/** Aion's graphic language: torn spatial blades, folded time and a collapsing gravitational iris. */
final class GodlyGeometry {
    private static final int GOLD=0xFFFFC95F,PURPLE=0xFF9E72FF,WHITE_GOLD=0xFFFFF0CD,INK=0xFF100A24;
    private GodlyGeometry(){}
    static void render(PoseStack p,MultiBufferSource b,int kind,Vec3 data,float age,int life){
        double f=fade(age,life),r=Math.abs(data.x);
        if(kind==ArtVisualEntity.CAUSAL_RAY){worldline(p,b,data,age,f);return;}
        if(kind==ArtVisualEntity.CAUSAL_ANCHOR){anchor(p,b,r,age,f);return;}
        if(kind==ArtVisualEntity.CAUSAL_IMPACT){
            if(r>2){
                SpectacleMesh.burst(p,b,new Vec3(0,.5,0),age,life,r*2.1,GOLD,3);
                clock(p,b,new Vec3(0,.06,0),r*(.3+.7*SpectacleMesh.out(age/12d)),-age*.04,f);
                for(int n=0;n<6;n++){double a=n*Math.TAU/6-age*.03;Vec3 at=polar(a,r*(1-age/life),.8+age*.04);SpectacleMesh.shard(p,b,at,new Vec3(0,1,0),.6,GOLD,f);}
            }else{
                // Repeated beam contacts stay compact; the large return gets the full impact grammar.
                double hit=Math.exp(-age*.2);SpectacleMesh.lance(p,b,new Vec3(-r,-r*.3,0),new Vec3(r,r*.6,0),.3,PURPLE,hit);
                SpectacleMesh.lance(p,b,new Vec3(0,-r,0),new Vec3(0,r,0),.16,WHITE_GOLD,hit);
                SpectacleMesh.sprite(p,b,SpectacleMesh.FLARE,Vec3.ZERO,r*1.6,r*1.6,.6,WHITE_GOLD,.5*hit);
            }return;
        }
        if(kind==ArtVisualEntity.CAUSAL_SPHERE){
            clock(p,b,new Vec3(0,-.8,0),r*f,-age*.02,f);
            for(int n=0;n<6;n++){double a=n*Math.TAU/6+age*.025;SpectacleMesh.shard(p,b,polar(a,r*f,.3),polar(a+Math.PI/2,1,.2).normalize(),.6,GOLD,f);}return;
        }
        horizon(p,b,r,data.y,age,f);
    }
    private static void worldline(PoseStack p,MultiBufferSource b,Vec3 end,float age,double f){
        double length=end.length();if(length<.05)return;Vec3 axis=end.normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
        // Solid ink silhouette and saturated edges stay inside the native 1.25-block hit tolerance.
        int count=segments(48);for(int n=0;n<count;n++){
            double t=n/(double)count,v=(n+1d)/count,w0=(.42+.23*Math.pow(Math.sin(t*23-age*.12),2))*f,w1=(.42+.23*Math.pow(Math.sin(v*23-age*.12),2))*f;
            Vec3 a=end.scale(t),q=end.scale(v);
            SpectacleMesh.face(p,b,a.add(up.scale(w0)),q.add(up.scale(w1)),q.subtract(up.scale(w1)),a.subtract(up.scale(w0)),SpectacleMesh.light(INK,.95*f));
            SpectacleMesh.face(p,b,a.add(up.scale(w0)).add(side.scale(.12)),q.add(up.scale(w1)).add(side.scale(.12)),q.add(up.scale(w1*.45)).add(side.scale(.12)),a.add(up.scale(w0*.45)).add(side.scale(.12)),SpectacleMesh.light(PURPLE,.9*f));
            SpectacleMesh.stroke(p,b,a.add(up.scale(w0)),q.add(up.scale(w1)),.08,GOLD,f);
        }
        SpectacleMesh.stroke(p,b,Vec3.ZERO,end,.3,PURPLE,f);SpectacleMesh.stroke(p,b,Vec3.ZERO,end,.09,WHITE_GOLD,f);
        for(int n=0;n<6;n++){
            double d=(age*.85+n*length/6)%length;Vec3 at=axis.scale(d),across=up.scale(.4);
            SpectacleMesh.lance(p,b,at.subtract(across).subtract(axis.scale(.6)),at.add(across).add(axis.scale(.7)),.13,n%2==0?GOLD:PURPLE,f);
        }
        SpectacleMesh.sprite(p,b,SpectacleMesh.FLARE,end,1.2,1.2,age*.01,WHITE_GOLD,.4*f);
    }
    private static void anchor(PoseStack p,MultiBufferSource b,double radius,float age,double f){
        clock(p,b,new Vec3(0,.06,0),radius*f,age*.018,f);Vec3 center=new Vec3(0,1.55,0);
        for(int n=0;n<4;n++){
            double a=n*Math.TAU/4+Math.PI/4;Vec3 lower=center.add(polar(a,.8,-1.2)),upper=center.add(polar(a,.8,1.2));
            SpectacleMesh.lance(p,b,lower,center,.19,GOLD,f);SpectacleMesh.lance(p,b,center,upper,.17,GOLD,f);
            SpectacleMesh.face(p,b,lower,center,upper,upper,SpectacleMesh.light(PURPLE,.2*f));
        }
        SpectacleMesh.stroke(p,b,center.add(0,1,0),center.add(0,-1,0),.12,GOLD,.75*f);
        for(int n=0;n<4;n++){double part=(age*.025+n*.25)%1;Vec3 at=center.add(.15*Math.sin(n),1-part*2,.15*Math.cos(n));SpectacleMesh.shard(p,b,at,new Vec3(0,-1,0),.15,WHITE_GOLD,f);}
        SpectacleMesh.halo(p,b,center,1.2,PURPLE,.12*f);
    }
    private static void horizon(PoseStack p,MultiBufferSource b,double radius,double pull,float age,double f){
        if(radius<.01)return;Vec3 center=new Vec3(0,1.8,0);double core=radius*.17*f;
        SingularityGeometry.originSeed(p,b,center,core,age,79);
        // Six broad folded petals carry material inward. The thin outer boundary denotes pull, not damage.
        for(int arm=0;arm<6;arm++){
            double offset=arm*Math.TAU/6-age*.013;Vec3 previous=polar(offset,radius*f,.1);int count=segments(36);
            for(int k=1;k<=count;k++){
                double t=k/(double)count,a=offset+t*Math.PI*1.4;Vec3 q=polar(a,radius*(1-.83*t)*f,.1+1.7*t);
                double width=(.1+Math.sin(t*Math.PI)*.9)*f;
                SpectacleMesh.ribbon(p,b,previous,q,polar(a,1,.2),width,arm%2==0?INK:0xFF2C1454,.87*f);
                SpectacleMesh.stroke(p,b,previous,q,.1,arm%2==0?GOLD:PURPLE,.8*f);previous=q;
            }
        }
        clock(p,b,new Vec3(0,.08,0),radius*f,age*.006,.8*f);
        if(pull>radius){
            ring(p,b,new Vec3(0,.04,0),pull*f,.045,0,0,tint(PURPLE,.24*f));
            for(int n=0;n<decorations(12);n++){
                double t=(age*.018+n*.618)%1,a=n*2.399963+t*1.2,r=(pull-(pull-core)*t)*f;Vec3 at=polar(a,r,.2+Math.sin(t*Math.PI)*1.4);
                SpectacleMesh.shard(p,b,at,center.subtract(at).normalize(),.22+.45*t,n%2==0?GOLD:PURPLE,Math.sin(t*Math.PI)*f);
            }
        }
        SpectacleMesh.halo(p,b,center,core*2.5,PURPLE,.12*f);
    }
    private static void clock(PoseStack p,MultiBufferSource b,Vec3 at,double radius,double angle,double f){
        if(radius<.01)return;seal(p,b,CLOCK,at,radius,angle,0,tint(GOLD,.6*f));
        // Broad broken hour marks and two substantial hands, rather than a ring of hairlines.
        for(int n=0;n<12;n++){double a=n*Math.TAU/12;SpectacleMesh.lance(p,b,at.add(polar(a,radius*.8,.02)),at.add(polar(a,radius,.02)),Math.min(.22,radius*.07),GOLD,.9*f);}
        for(int n=0;n<2;n++){double a=angle*(n==0?1:7);SpectacleMesh.lance(p,b,at,at.add(polar(a,radius*(n==0?.5:.78),.04)),Math.min(.19,radius*.055),n==0?GOLD:PURPLE,f);}
    }
}
