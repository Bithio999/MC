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

/** Cinematic accretion, stable disk planes, velocity-deformed horizons and independent spatial detonations. */
final class SingularityGeometry {
    private static final int GOLD=0xFFFFA244,IVORY=0xFFFFE4B7,VIOLET=0xFF8865FF,BLUE=0xFF84CFFF;
    private static final ResourceLocation DISK=WildHunt.id("textures/effect/singularity_accretion.png"),BURST=WildHunt.id("textures/effect/singularity_burst.png");
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
        var state=entity.barrage();if(state.isEmpty())return;double age=entity.barrageAge(partial);int seed=state.getInt("seed"),impacted=state.getInt("impacted");
        Vec3 live=point(state,"aim");var target=entity.level().getEntity(state.getInt("target_id"));
        if(state.hasUUID("target") && target instanceof LivingEntity living && living.isAlive() && target.getUUID().equals(state.getUUID("target")))
            live=target.getPosition(partial).add(target.getBoundingBox().getCenter().subtract(target.position())).subtract(entity.position());
        for(int node=0;node<SingularityFlight.COUNT;node++){
            double born=SingularityFlight.birth(seed,node),arrival=SingularityFlight.impact(node);
            if(age<born || age>arrival+SingularityFlight.IMPACT_LIFE)continue;
            if(age>=arrival){
                // An impact stays where it happened, while every still-flying hole keeps homing.
                Vec3 at=(impacted&(1<<node))!=0?point(state,"impact_"+node):live.add(SingularityFlight.impactOffset(seed,node));
                VfxMesh.begin(entity.position().add(at));impact(p,b,at,age-arrival,seed,node,state.getBoolean("empowered"));continue;
            }
            Vec3 at=SingularityFlight.position(seed,node,age,live),velocity=SingularityFlight.velocity(seed,node,age,live);double speed=velocity.length();
            double radius=.68+SingularityFlight.random(seed,node,12)*.18,grow=smooth((age-born)/7),launch=SingularityFlight.launch(seed,node),flight=Math.max(0,age-launch);
            double anticipate=age<launch?smooth((age-launch+4)/4):Math.exp(-flight*.65);
            double stretch=SingularityFlight.stretch(speed)*(1-.24*anticipate),cross=SingularityFlight.transverse(stretch);
            Vec3 heading=age<launch+.1?SingularityFlight.velocity(seed,node,launch+.1,live):velocity;
            Vec3 axis=heading.lengthSqr()>1e-12?heading.normalize():live.subtract(at).normalize();
            VfxMesh.begin(entity.position().add(at));
            if(age>launch)trail(p,b,seed,node,age,live,launch,grow);
            hole(p,b,at,axis,radius*grow,stretch,cross,age,seed,node);
            if(age<born+10){double phase=(age-born)/10;photon(p,b,at,axis,radius*(1+phase*1.8),stretch,cross,.024,tint(VIOLET,.45*Math.sin(phase*Math.PI)));}
        }
    }
    private static void darkCore(PoseStack p,MultiBufferSource b){
        var mesh=b.getBuffer(RenderType.entityCutoutNoCull(WHITE));var pose=p.last();
        for(var face:segments(12)<12?CORE_LOW:CORE)for(var point:face)
            mesh.addVertex(pose,(float)point.x,(float)point.y,(float)point.z).setColor(0xFF020106).setUv(0,0)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,1,0);
    }
    private static void hole(PoseStack p,MultiBufferSource b,Vec3 at,Vec3 axis,double radius,double stretch,double cross,double age,int seed,int node){
        if(radius<.001)return;
        glow(p,b,at,radius*3.8,tint(GOLD,.075));
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(new Quaternionf().rotationTo(0,0,1,(float)axis.x,(float)axis.y,(float)axis.z));
        p.scale((float)(radius*cross),(float)(radius*cross),(float)(radius*stretch));darkCore(p,b);p.popPose();
        // The disk has its own fixed world-space normal: no velocity quaternion, roll or body stretch.
        Vec3 normal=new Vec3((SingularityFlight.random(seed,node,13)-.5)*.5,1,(SingularityFlight.random(seed,node,14)-.5)*.5).normalize();
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(new Quaternionf().rotationTo(0,1,0,(float)normal.x,(float)normal.y,(float)normal.z));p.scale((float)radius,(float)radius,(float)radius);
        accretion(p,b,age,seed,node);p.popPose();
        lensedDisk(p,b,at,radius,normal,stretch,cross,age,seed,node);
        photon(p,b,at,axis,radius*1.035,stretch,cross,.022,tint(IVORY,.78));
        photon(p,b,at,axis,radius*1.10,stretch,cross,.04,tint(GOLD,.28));
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
        int count=segments(64),bands=6;double flow=age*.003+SingularityFlight.random(seed,node,15);
        var surface=b.getBuffer(VfxMaterials.surface(DISK));var emission=b.getBuffer(VfxMaterials.additive(DISK));
        for(int band=0;band<bands;band++)for(int n=0;n<count;n++){
            double a=n*Math.PI*2/count,q=(n+1)*Math.PI*2/count,v0=band/(double)bands,v1=(band+1d)/bands;
            double inner=1.08+v0*1.65,outer=1.08+v1*1.65;
            Vec3 i0=polar(a,inner,0),i1=polar(q,inner,0),o0=polar(a,outer,0),o1=polar(q,outer,0);
            double bright=.68+.32*Math.cos(a+.8);int color=blend(IVORY,GOLD,v0*.85);
            // Restrained asymmetry preserves the black silhouette, as in Gargantua's cinematic treatment.
            color=blend(color,BLUE,Math.pow(Math.max(0,Math.cos(a+.8)),8)*.14);
            quadUV(p.last(),surface,i0,i1,o1,o0,opacity(color,.94),false,(float)(n/(double)count+flow),(float)v0,(float)((n+1d)/count+flow),(float)v1);
            quadUV(p.last(),emission,i0,i1,o1,o0,tint(color,.24*bright),true,(float)(n/(double)count+flow),(float)v0,(float)((n+1d)/count+flow),(float)v1);
        }
        ring(p,b,Vec3.ZERO,1.08,.035,0,0,tint(IVORY,.8));
    }
    private static void lensedDisk(PoseStack p,MultiBufferSource b,Vec3 at,double radius,Vec3 normal,double stretch,double cross,double age,int seed,int node){
        // Screen-space rear-disk images are view dependent, but never turn with flight direction.
        var camera=Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        var right=new Vector3f(1,0,0).rotate(camera);var up=new Vector3f(0,1,0).rotate(camera);
        double nx=normal.x*right.x+normal.y*right.y+normal.z*right.z,ny=normal.x*up.x+normal.y*up.y+normal.z*up.z;
        double roll=Math.atan2(-nx,ny),flow=age*.003+SingularityFlight.random(seed,node,15);
        p.pushPose();p.translate(at.x,at.y,at.z);p.mulPose(camera);p.mulPose(Axis.ZP.rotation((float)roll));p.scale((float)radius,(float)radius,(float)radius);
        var mesh=b.getBuffer(VfxMaterials.additive(DISK));int count=segments(48);
        for(int side:new int[]{-1,1})for(int band=0;band<3;band++)for(int n=0;n<count;n++){
            double a=n*Math.PI/count,q=(n+1)*Math.PI/count,inner=1.12+band*.17,outer=inner+.18;
            double height=side==1?1.22:.96,alpha=side==1?.58:.19;
            Vec3 i0=lensPoint(a,inner,height,side),i1=lensPoint(q,inner,height,side),o0=lensPoint(a,outer,height,side),o1=lensPoint(q,outer,height,side);
            quadUV(p.last(),mesh,i0,i1,o1,o0,tint(blend(IVORY,GOLD,.3+band*.28),alpha*(1-band*.14)),true,(float)(n/(double)count+flow),band/3f,(float)((n+1d)/count+flow),(band+1)/3f);
        }p.popPose();
    }
    private static Vec3 lensPoint(double a,double r,double height,int side){return new Vec3(Math.cos(a)*r*(1+.55*Math.pow(Math.cos(a),4)),side*Math.sin(a)*r*height,.025);}
    private static int blend(int a,int b,double t){int out=0xFF000000;for(int shift:new int[]{0,8,16})out|=(int)(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t)<<shift;return out;}
    private static void trail(PoseStack p,MultiBufferSource b,int seed,int node,double age,Vec3 aim,double launch,double grow){
        int count=segments(18);double tail=Math.max(launch,age-4.5);Vec3 previous=SingularityFlight.position(seed,node,tail,aim);
        for(int n=1;n<=count;n++){
            double t=n/(double)count;Vec3 next=SingularityFlight.position(seed,node,tail+(age-tail)*t,aim);double strength=t*t*grow;
            beam(p,b,previous,next,.3*strength,tint(VIOLET,.35*strength));beam(p,b,previous,next,.07*strength,tint(GOLD,.72*strength));
            previous=next;
        }
    }
    private static void impact(PoseStack p,MultiBufferSource b,Vec3 at,double age,int seed,int node,boolean empowered){
        double t=Math.clamp(age/SingularityFlight.IMPACT_LIFE,0,1),fade=Math.pow(1-t,2),size=empowered?12:10;
        double radius=size*(1-Math.exp(-age*.18)),flash=Math.exp(-age*.32),hot=Math.exp(-age*.14);
        // Short brilliant ignition, long expanding pressure front: big without permanent whiteout.
        glow(p,b,at,3+flash*7.5,tint(IVORY,.96*flash));glow(p,b,at,2.5+radius*.5,tint(VIOLET,.15*fade));
        billboard(p,b,BURST,at,2.5+radius*.9,2.5+radius*.9,node*2.399963,tint(IVORY,.9*hot),true);
        photon(p,b,at,new Vec3(0,1,0),radius,1,1,.32+.55*fade,tint(GOLD,.58*fade));
        p.pushPose();p.translate(at.x,at.y,at.z);
        ring(p,b,Vec3.ZERO,radius,.3+.6*fade,0,0,tint(IVORY,.6*fade));
        p.mulPose(Axis.ZP.rotation((float)(.22+SingularityFlight.random(seed,node,90)*.45)));
        ring(p,b,Vec3.ZERO,radius*.78,.25+.5*fade,0,0,tint(VIOLET,.56*fade));p.popPose();
        // A faceted expanding shell and arcing ejecta give the blast depth from every viewing angle.
        int rays=decorations(12);for(int n=0;n<rays;n++){
            double a=SingularityFlight.random(seed,node,n+40)*Math.PI*2,y=SingularityFlight.random(seed,node,n+60)*1.5-.5;
            Vec3 direction=new Vec3(Math.cos(a),y,Math.sin(a)).normalize();double reach=radius*(.75+.4*SingularityFlight.random(seed,node,n+80));
            Vec3 end=at.add(direction.scale(reach)).add(0,-age*age*.0015,0),mid=at.add(direction.scale(reach*.6)).add(0,1.5*Math.sin(t*Math.PI),0);
            beam(p,b,at.add(direction.scale(reach*.3)),mid,.14*fade,tint(VIOLET,.38*fade));
            beam(p,b,mid,end,.10*fade,tint(n%3==0?BLUE:GOLD,.85*fade));
            glow(p,b,end,.22*fade,tint(IVORY,.8*fade));
        }
        if(age<7){double r=2+age*1.2;beam(p,b,at.add(-r,0,0),at.add(r,0,0),.09*hot,tint(IVORY,.65*hot));}
    }
}
