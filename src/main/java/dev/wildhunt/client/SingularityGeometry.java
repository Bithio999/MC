package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.wildhunt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static dev.wildhunt.client.VfxMesh.*;

/** Opaque horizons, Doppler accretion, projected photon optics and velocity-driven deformation. */
final class SingularityGeometry {
    private static final int GOLD=0xFFFFAA40,IVORY=0xFFFFD99A,VIOLET=0xFF8865FF,BLUE=0xFF61BBFF;
    private static final ResourceLocation DISK=WildHunt.id("textures/effect/singularity_accretion.png");
    private static final Vec3[][] CORE=coreMesh(12,28),CORE_LOW=coreMesh(6,14);
    private SingularityGeometry(){}
    private static Vec3[][] coreMesh(int latitude,int longitude){
        var mesh=new Vec3[latitude*longitude][4];int n=0;
        for(int y=0;y<latitude;y++)for(int x=0;x<longitude;x++){
            double a=-Math.PI/2+y*Math.PI/latitude,q=a+Math.PI/latitude,c=x*Math.PI*2/longitude,d=c+Math.PI*2/longitude;
            mesh[n++]=new Vec3[]{unit(a,c),unit(q,c),unit(q,d),unit(a,d)};
        }return mesh;
    }
    private static Vec3 unit(double a,double b){return new Vec3(Math.cos(a)*Math.cos(b),Math.sin(a),Math.cos(a)*Math.sin(b));}
    private static Vec3 point(CompoundTag t,String key){return new Vec3(t.getDouble(key+"_x"),t.getDouble(key+"_y"),t.getDouble(key+"_z"));}
    static void render(ArtVisualEntity entity,PoseStack p,MultiBufferSource b,float partial){
        var state=entity.barrage();if(state.isEmpty())return;double age=entity.barrageAge(partial);int seed=state.getInt("seed"),locked=state.getInt("locked");
        Vec3 live=point(state,"aim");var target=entity.level().getEntity(state.getInt("target_id"));
        if(state.hasUUID("target") && target instanceof LivingEntity living && living.isAlive() && target.getUUID().equals(state.getUUID("target"))){
            Vec3 center=target.getPosition(partial).add(target.getBoundingBox().getCenter().subtract(target.position()));
            if(center.distanceToSqr(entity.position())<=24*24)live=center.subtract(entity.position());
        }
        for(int node=0;node<SingularityFlight.COUNT;node++){
            int wave=node/4;Vec3 aim=(locked&(1<<wave))==0?live:point(state,"aim_"+wave);double born=SingularityFlight.birth(seed,node),arrival=SingularityFlight.impact(wave);
            if(age<born || age>arrival+24)continue;
            double radius=.64+SingularityFlight.random(seed,node,12)*.22;
            if(age>=arrival){VfxMesh.begin(entity.position().add(aim));impact(p,b,aim.add(SingularityFlight.impactOffset(seed,node)),age-arrival,seed,node,state.getBoolean("empowered"));continue;}
            Vec3 at=SingularityFlight.position(seed,node,age,aim),velocity=SingularityFlight.velocity(seed,node,age,aim);double speed=velocity.length();
            double grow=smooth((age-born)/7),launch=SingularityFlight.launch(seed,node),flight=Math.max(0,age-launch);
            double anticipate=age<launch?smooth((age-launch+4)/4):Math.exp(-flight*.65);
            double stretch=SingularityFlight.stretch(speed)*(1-.28*anticipate),cross=SingularityFlight.transverse(stretch);
            // Face the departure tangent during anticipation, even while eased speed is zero.
            Vec3 heading=age<launch+.1?SingularityFlight.velocity(seed,node,launch+.1,aim):velocity;
            Vec3 axis=heading.lengthSqr()>1e-12?heading.normalize():aim.subtract(at).normalize();
            VfxMesh.begin(entity.position().add(at));
            if(age>launch)trail(p,b,seed,node,age,aim,launch,grow);
            hole(p,b,at,axis,radius*grow,stretch,cross,age,seed,node);
            if(age<born+10){double phase=(age-born)/10;photon(p,b,at,axis,radius*(1+phase*1.8),stretch,cross,.024,tint(VIOLET,.45*Math.sin(phase*Math.PI)));}
        }
    }
    private static void darkCore(PoseStack p,MultiBufferSource b){
        var mesh=b.getBuffer(RenderType.entityCutoutNoCull(WHITE));var pose=p.last();
        for(var face:segments(12)<12?CORE_LOW:CORE)for(var point:face)
            mesh.addVertex(pose,(float)point.x,(float)point.y,(float)point.z).setColor(0xFF030209).setUv(0,0)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,1,0);
    }
    private static void hole(PoseStack p,MultiBufferSource b,Vec3 at,Vec3 axis,double radius,double stretch,double cross,double age,int seed,int node){
        if(radius<.001)return;
        glow(p,b,at,radius*2.5,tint(VIOLET,.11));
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(new Quaternionf().rotationTo(0,0,1,(float)axis.x,(float)axis.y,(float)axis.z));
        p.scale((float)(radius*cross),(float)(radius*cross),(float)(radius*stretch));darkCore(p,b);
        p.mulPose(Axis.XP.rotation((float)(.6+SingularityFlight.random(seed,node,13)*.8)));p.mulPose(Axis.ZP.rotation((float)(SingularityFlight.random(seed,node,14)*Math.PI)));
        accretion(p,b,age,seed,node);p.popPose();
        // The ellipse is the analytic screen projection of the stretched 3D horizon.
        photon(p,b,at,axis,radius*1.035,stretch,cross,.017,tint(IVORY,.72));
        photon(p,b,at,axis,radius*1.105,stretch,cross,.028,tint(GOLD,.42));
        photon(p,b,at,axis,radius*1.26,stretch,cross,.035,tint(VIOLET,.15));
    }
    private static void photon(PoseStack p,MultiBufferSource b,Vec3 at,Vec3 axis,double radius,double longitudinal,double transverse,double width,int color){
        var camera=Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        var right=new Vector3f(1,0,0).rotate(camera);var up=new Vector3f(0,1,0).rotate(camera);
        double x=axis.x*right.x+axis.y*right.y+axis.z*right.z,y=axis.x*up.x+axis.y*up.y+axis.z*up.z;
        double major=Math.sqrt(transverse*transverse+(longitudinal*longitudinal-transverse*transverse)*(x*x+y*y));
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(camera);p.mulPose(Axis.ZP.rotation((float)Math.atan2(y,x)));p.scale((float)major,(float)transverse,1);
        ring(p,b,new Vec3(0,0,.018),radius,width,0,1,color);p.popPose();
    }
    private static void accretion(PoseStack p,MultiBufferSource b,double age,int seed,int node){
        int count=segments(48);var mesh=b.getBuffer(VfxMaterials.surface(DISK));var emission=b.getBuffer(VfxMaterials.additive(DISK));double spin=age*.006+SingularityFlight.random(seed,node,15)*6;
        for(int layer=0;layer<3;layer++)for(int n=0;n<count;n++){
            double a=n*Math.PI*2/count,q=(n+1)*Math.PI*2/count;
            double inner=1.12+layer*.27,outer=inner+.24+layer*.06;
            // A permanently visible high arch suggests the lensed rear face of the disk.
            double warp=layer==2?.42:layer==1?.16:0;
            Vec3 i0=diskPoint(a,inner,warp),i1=diskPoint(q,inner,warp),o0=diskPoint(a,outer,warp),o1=diskPoint(q,outer,warp);
            double doppler=.5+.5*Math.cos(a+spin*.25);int hot=blend(GOLD,BLUE,Math.pow(doppler,5));if(layer==0)hot=blend(hot,IVORY,.2);
            quadUV(p.last(),emission,i0,i1,o1,o0,tint(hot,layer==0?.5:layer==1?.32:.16),true,(float)(n/(double)count+spin),0,(float)((n+1d)/count+spin),1);
            quadUV(p.last(),mesh,i0,i1,o1,o0,opacity(hot,layer==0?.95:layer==1?.8:.45),false,(float)(n/(double)count+spin),(float)0,(float)((n+1d)/count+spin),1);
        }
        // Thin white-hot inner rim survives reduced particle settings.
        ring(p,b,Vec3.ZERO,1.12,.028,spin,0,tint(IVORY,.85));
        int specks=decorations(3);for(int n=0;n<specks;n++){
            double phase=(age*.033+n*.337+node*.113)%1,a=phase*Math.PI*3+node;
            Vec3 position=polar(a,1.9-phase*.7,.08*Math.sin(a*2));glow(p,b,position,.045,tint(IVORY,Math.sin(phase*Math.PI)*.75));
        }
    }
    private static Vec3 diskPoint(double angle,double radius,double warp){return new Vec3(Math.cos(angle)*radius,warp*Math.pow(Math.max(0,Math.sin(angle)),2)*radius,Math.sin(angle)*radius);}
    private static int blend(int a,int b,double t){int out=0xFF000000;for(int shift:new int[]{0,8,16})out|=(int)(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t)<<shift;return out;}
    private static void trail(PoseStack p,MultiBufferSource b,int seed,int node,double age,Vec3 aim,double launch,double grow){
        int count=segments(18);double tail=Math.max(launch,age-6.5);Vec3 previous=SingularityFlight.position(seed,node,tail,aim);
        for(int n=1;n<=count;n++){
            double t=n/(double)count;Vec3 next=SingularityFlight.position(seed,node,tail+(age-tail)*t,aim);double strength=t*t*grow;
            beam(p,b,previous,next,.23*strength,tint(VIOLET,.45*strength));beam(p,b,previous,next,.055*strength,tint(GOLD,.7*strength));
            if(n%3==0 && decorations(1)>0){Vec3 tangent=next.subtract(previous).normalize();beam(p,b,next,next.subtract(tangent.scale(.3)),.016,tint(IVORY,.65*strength));}previous=next;
        }
    }
    private static void impact(PoseStack p,MultiBufferSource b,Vec3 at,double age,int seed,int node,boolean empowered){
        double life=24,t=age/life,fade=Math.pow(1-t,2),r=(empowered?4.5:3.75)*(1-Math.exp(-age*.24));
        if(age<2){p.pushPose();p.translate(at.x,at.y,at.z);p.scale((float)(.6*(1-age/2)),(float)(.6*(1-age/2)),(float)(.6*(1-age/2)));darkCore(p,b);p.popPose();}
        double flash=Math.exp(-age*.34);glow(p,b,at,.6+flash*2,tint(IVORY,.85*flash));glow(p,b,at,1.4+age*.065,tint(VIOLET,.3*fade));
        photon(p,b,at,new Vec3(0,1,0),r,1,1,.08+.07*fade,tint(GOLD,.5*fade));
        for(int plane=0;plane<2;plane++){
            p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(Axis.YP.rotation((float)(node*2.39996)));p.mulPose(Axis.XP.rotation((float)(plane*.8+.25)));
            ring(p,b,Vec3.ZERO,r*(plane==0?1:.8),(.065+.07*fade),age*.025,0,tint(plane==0?IVORY:VIOLET,.62*fade));p.popPose();
        }
        int count=decorations(9);for(int n=0;n<count;n++){
            double a=SingularityFlight.random(seed,node,n+40)*Math.PI*2,y=SingularityFlight.random(seed,node,n+60)*2-1;
            Vec3 direction=new Vec3(Math.cos(a),y,Math.sin(a)).normalize(),outer=at.add(direction.scale(r*(.7+.3*SingularityFlight.random(seed,node,n+80))));
            beam(p,b,outer.subtract(direction.scale(.35+fade*.7)),outer,.04*fade,tint(n%2==0?GOLD:BLUE,.9*fade));
        }
    }
}
