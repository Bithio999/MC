package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.wildhunt.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import static dev.wildhunt.client.VfxMesh.*;

/** Exact warning footprints, six-winged scripture and a visible refuge; original batched native geometry. */
final class AnankeGeometry {
    private static final int GOLD=0xFFFFCB74,WHITE_GOLD=0xFFFFEFD1,VIOLET=0xFFAA79FF,WARNING=0xFFFF6C97;
    private AnankeGeometry(){}
    static boolean handles(int kind){return kind>=ArtVisualEntity.ANANKE_FATEBREAKER && kind<=ArtVisualEntity.ANANKE_ASCENSION || kind==ArtVisualEntity.ANANKE_SHIFT;}
    static void render(ArtVisualEntity entity,PoseStack p,MultiBufferSource b,float partial){
        float age=entity.barrage().isEmpty()?entity.tickCount+partial:entity.barrageAge(partial);int kind=entity.kind();
        if(kind==ArtVisualEntity.ANANKE_ASCENSION){ascend(p,b,age,entity.lifetime(),entity.end().x);return;}
        if(kind==ArtVisualEntity.ANANKE_SHIFT){shift(p,b,age);return;}
        var state=entity.barrage();if(state.isEmpty())return;
        if(kind==ArtVisualEntity.ANANKE_FATEBREAKER)fan(p,b,state,age);
        else if(kind==ArtVisualEntity.ANANKE_STARFALL)stars(p,b,state,age);
        else if(kind==ArtVisualEntity.ANANKE_PROCESSION)procession(p,b,age);
        else dawn(p,b,age);
    }
    private static Vec3 point(CompoundTag t,String name){return new Vec3(t.getDouble(name+"_x"),t.getDouble(name+"_y"),t.getDouble(name+"_z"));}
    private static void fan(PoseStack p,MultiBufferSource b,CompoundTag state,float age){
        Vec3 f=point(state,"forward");double angle=Math.atan2(f.z,f.x),half=Math.toRadians(55),flash=Math.exp(-Math.max(0,age-24)*.15);
        int count=segments(44);for(int n=0;n<count;n++){
            double a=angle-half+n*(2*half/count),q=angle-half+(n+1)*(2*half/count);Vec3 a0=polar(a,16,.12),a1=polar(q,16,.12);
            if(age<24){quad(p.last(),b.getBuffer(VfxMaterials.additive(RIFT)),new Vec3(0,.12,0),a0,a1,new Vec3(0,.12,0),tint(WARNING,.22+.05*Math.sin(age*.4)),true);beam(p,b,a0,a1,.14,tint(GOLD,.7));}
            else {double r=16*smooth((age-24)/7d);Vec3 u=polar(a,r,1.1),v=polar(q,r,1.1);beam(p,b,u,v,.15,tint(WHITE_GOLD,flash));beam(p,b,polar(a,r,.2),u,.08,tint(VIOLET,flash*.55));}
        }
        beam(p,b,new Vec3(0,.12,0),polar(angle-half,16,.12),.055,tint(GOLD,.7));beam(p,b,new Vec3(0,.12,0),polar(angle+half,16,.12),.055,tint(GOLD,.7));
        if(age>=24){seal(p,b,GATE,new Vec3(0,.2,0),4,age*.015,0,tint(GOLD,flash*.5));glow(p,b,new Vec3(0,1,0),5,tint(WHITE_GOLD,flash*.5));}
    }
    private static void stars(PoseStack p,MultiBufferSource b,CompoundTag plan,float age){
        for(int n=0;n<plan.getInt("warned");n++){
            double birth=n*12,impact=28+n*12,t=age-impact;if(age<birth || t>30)continue;
            Vec3 at=AnankeRites.star(plan,n);
            if(t<0){
                ring(p,b,at.add(0,-.8,0),3,.16,0,0,tint(WARNING,.85));seal(p,b,GATE,at.add(0,-.78,0),2.5,age*.018+n,0,tint(GOLD,.5));
                beam(p,b,at.add(0,6,0),at.add(0,15,0),.025,tint(GOLD,.28));glow(p,b,at.add(0,12,0),.7+smooth((age-birth)/28)*.8,tint(WHITE_GOLD,.7));
            }else {
                double fade=Math.pow(1-t/30,2);Vec3 top=at.add(0,20*Math.max(0,1-t/4),0);
                beam(p,b,at,top,.38*fade,tint(WHITE_GOLD,fade));beam(p,b,at,top,.8*fade,tint(VIOLET,.3*fade));
                for(int plane=0;plane<3;plane++)ring(p,b,at.add(0,plane*.8,0),3*(1-Math.exp(-t*.3))+.2,.13*fade,age*.02+plane,plane%2,tint(plane==1?VIOLET:GOLD,.7*fade));
                glow(p,b,at,4,tint(WHITE_GOLD,Math.exp(-t*.35)));
            }
        }
    }
    private static void procession(PoseStack p,MultiBufferSource b,float age){
        if(age<30){ring(p,b,new Vec3(0,.12,0),25,.06,0,0,tint(WARNING,.45));seal(p,b,CLOCK,new Vec3(0,.15,0),3,age*.015,0,tint(GOLD,.6));return;}
        double t=age-30,r=3+Math.min(t,40)*.55,fade=Math.min(1,(86-age)/12d);int count=segments(72);
        var mesh=b.getBuffer(VfxMaterials.additive(WAVE));
        for(int n=0;n<count;n++){
            double a=n*Math.PI*2/count,q=(n+1)*Math.PI*2/count;Vec3 low0=polar(a,r,.1),low1=polar(q,r,.1),high0=polar(a,r,2.2+.45*Math.sin(a*8+t*.1)),high1=polar(q,r,2.2+.45*Math.sin(q*8+t*.1));
            quadUV(p.last(),mesh,low0,low1,high1,high0,tint(VIOLET,.48*fade),true,(float)n/count,0,(float)(n+1)/count,1);
            beam(p,b,high0,high1,.075,tint(GOLD,.75*fade));
        }
        ring(p,b,new Vec3(0,.15,0),r,.16,0,0,tint(WHITE_GOLD,.85*fade));ring(p,b,new Vec3(0,1,0),r-.6,.04,0,0,tint(VIOLET,.6*fade));
    }
    private static void refuge(PoseStack p,MultiBufferSource b,double strength,float age){
        ring(p,b,new Vec3(0,.18,0),7,.27,0,0,tint(GOLD,strength));ring(p,b,new Vec3(0,.18,0),12,.27,0,0,tint(GOLD,strength));
        int count=segments(64);for(int n=0;n<count;n++){
            double a=n*Math.PI*2/count,q=(n+1)*Math.PI*2/count;
            quad(p.last(),b.getBuffer(VfxMaterials.additive(WHITE)),polar(a,7,.13),polar(q,7,.13),polar(q,12,.13),polar(a,12,.13),tint(GOLD,.075*strength),true);
            quad(p.last(),b.getBuffer(VfxMaterials.additive(CIRCUIT)),polar(a,7,.14),polar(q,7,.14),polar(q,12,.14),polar(a,12,.14),tint(GOLD,.25*strength),true);
            if(n%4==0){beam(p,b,polar(a,7,.2),polar(a,12,.2),.03,tint(GOLD,.65*strength));beam(p,b,polar(a,12,0),polar(a,12,2),.025,tint(GOLD,.5*strength));}
        }
    }
    private static void dawn(PoseStack p,MultiBufferSource b,float age){
        double charge=smooth(age/75d);refuge(p,b,age<75?.8:.8*Math.max(0,(108-age)/33d),age);
        if(age<75){
            ring(p,b,new Vec3(0,.1,0),32,.23,0,0,tint(WARNING,.85));seal(p,b,CLOCK,new Vec3(0,.12,0),6.7,age*.006,0,tint(VIOLET,.4));
            seal(p,b,GATE,new Vec3(0,7,0),3+charge*3,-age*.014,.4,tint(GOLD,.65));seal(p,b,CLOCK,new Vec3(0,7,0),4+charge*2,age*.011,-.5,tint(VIOLET,.55));
            sphere(p,b,new Vec3(0,7,0),.6+charge*1.6,0xFF0B0715);glow(p,b,new Vec3(0,7,0),3+charge*4,tint(GOLD,.2));
            for(int n=0;n<6;n++){double a=n*Math.PI/3+age*.004;beam(p,b,polar(a,5,7),polar(a,5+charge*7,14+charge*4),.045,tint(WHITE_GOLD,.5*charge));}
        }else {
            double t=age-75,fade=Math.exp(-t*.11);glow(p,b,new Vec3(0,4,0),11,tint(WHITE_GOLD,.85*fade));
            ring(p,b,new Vec3(0,1,0),Math.min(7,t*.7),.5,t*.01,0,tint(WHITE_GOLD,.7*fade));
            ring(p,b,new Vec3(0,1,0),Math.max(12,32-t*1.4),.6,t*.01,0,tint(VIOLET,.8*fade));
            for(int n=0;n<16;n++){double a=n*Math.PI/8;beam(p,b,polar(a,13,.2),polar(a,32,4*Math.sin(t*.06)+3),.11,tint(GOLD,.65*fade));}
        }
    }
    private static void shift(PoseStack p,MultiBufferSource b,float age){
        double fade=Math.pow(Math.max(0,1-age/24d),2),r=1+3*smooth(age/24d);
        for(int n=0;n<3;n++)ring(p,b,new Vec3(0,1+n,0),r,.08,age*.02+n,n%2,tint(n==1?GOLD:VIOLET,fade));
        seal(p,b,GATE,new Vec3(0,.15,0),r,age*.018,0,tint(GOLD,.6*fade));
    }
    private static void ascend(PoseStack p,MultiBufferSource b,float age,int life,double size){
        double t=age/life,fade=Math.sin(Math.PI*Math.clamp(t,0,1)),r=2+size*smooth(t);
        ring(p,b,new Vec3(0,.15,0),r,.1,0,0,tint(GOLD,.8*fade));seal(p,b,GATE,new Vec3(0,.2,0),size*.7,age*.006,0,tint(VIOLET,.35*fade));
        for(int n=0;n<6;n++){double a=n*Math.PI/3;Vec3 base=polar(a,2,1),tip=polar(a,r*.8,4+6*(1-t));beam(p,b,base,tip,.06,tint(GOLD,.7*fade));glow(p,b,tip,.5,tint(WHITE_GOLD,fade));}
    }
}
