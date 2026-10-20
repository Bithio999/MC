package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildhunt.ArtVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import static dev.wildhunt.client.VfxMesh.*;

/** Aion's astronomical machinery: chronometers, an accretion lens and a luminous worldline. */
final class GodlyGeometry {
    private static final int GOLD=0xFFF5D48E,PURPLE=0xFFBE99FF,WHITE_GOLD=0xFFFFEAC6;
    private GodlyGeometry(){}
    static void render(PoseStack p,MultiBufferSource b,int kind,Vec3 data,float age,int life){
        double f=fade(age,life),r=Math.abs(data.x),t=age*.05;
        if(kind==ArtVisualEntity.CAUSAL_RAY){
            Vec3 axis=data.normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
            beam(p,b,Vec3.ZERO,data,.035*f,tint(0xFFFFFFFF,.9*f));beam(p,b,Vec3.ZERO,data,.16*f,tint(GOLD,.75*f));beam(p,b,Vec3.ZERO,data,.3*f,tint(PURPLE,.25*f));
            for(double d=2;d<data.length();d+=6){Vec3 c=axis.scale(d);p.pushPose();p.translate(c.x,c.y,c.z);p.mulPose(new org.joml.Quaternionf().rotationTo(0,1,0,(float)axis.x,(float)axis.y,(float)axis.z));
                seal(p,b,CLOCK,Vec3.ZERO,(.5+.06*Math.sin(t*3-d))*f,age*.025,0,tint(GOLD,.45*f));p.popPose();}
            for(int n=0;n<2;n++){Vec3 last=Vec3.ZERO;int count=segments(48);for(int k=1;k<=count;k++){double s=k/(double)count,a=s*data.length()*.5-t*2+n*Math.PI;Vec3 next=data.scale(s).add(side.scale(Math.cos(a)*.15*f)).add(up.scale(Math.sin(a)*.15*f));beam(p,b,last,next,.025*f,tint(n==0?PURPLE:GOLD,.7*f));last=next;}}
            // Tiny moving time slices leave the actual aim center readable.
            for(int n=0;n<8;n++){double along=(t*8+n*5)%Math.max(.01,data.length());Vec3 c=axis.scale(along);beam(p,b,c.add(side.scale(.28)),c.add(side.scale(.48)).add(up.scale(.12)),.025*f,tint(WHITE_GOLD,.8*f));}return;
        }
        if(kind==ArtVisualEntity.CAUSAL_SPHERE){
            seal(p,b,RIFT,new Vec3(0,-.85,0),r*f,t*.12,0,tint(GOLD,.65*f));
            for(int arm=0;arm<2;arm++){Vec3 last=polar(arm*Math.PI,r*f,-.7);for(int n=1;n<=segments(64);n++){double s=n/(double)segments(64),a=arm*Math.PI+s*Math.PI*4-t;Vec3 next=polar(a,r*(1-.75*s)*f,-.7+s*4*f);beam(p,b,last,next,.075*f,tint(arm==0?GOLD:0xFF97E9E7,.7*f));last=next;}}
            motes(p,b,r,age,32,tint(GOLD,f));return;
        }
        if(kind==ArtVisualEntity.CAUSAL_ANCHOR){
            clock(p,b,new Vec3(0,.08,0),r*f,t*.3,f);
            // An actual three-dimensional hourglass: curved ribs, transparent crystal and flowing sand.
            Vec3 center=new Vec3(0,1.3,0);double size=.55*f;
            for(int n=0;n<6;n++){double a=n*Math.PI/3+t*.2;Vec3 lower=center.add(polar(a,size,-.8*f)),upper=center.add(polar(a,size,.8*f));beam(p,b,lower,upper,.035*f,tint(GOLD,.85*f));
                surface(p,b,ICE,lower,center,center.add(polar(a+Math.PI/3,size,-.8*f)),lower,0x5586BBDD);
                surface(p,b,ICE,upper,center,center.add(polar(a+Math.PI/3,size,.8*f)),upper,0x55C6B5E8);}
            ring(p,b,center.add(0,-.8*f,0),size,.09*f,t,0,tint(GOLD,f));ring(p,b,center.add(0,.8*f,0),size,.09*f,t,0,tint(GOLD,f));
            beam(p,b,center.add(0,.7*f,0),center.add(0,-.7*f,0),.035*f,tint(WHITE_GOLD,.8*f));
            for(int n=0;n<decorations(12);n++){double y=.7-((t*.6+n*.113)%1)*1.4;glow(p,b,center.add(.07*Math.sin(n),y*f,.07*Math.cos(n)),.08*f,tint(GOLD,.9*f));}
            return;
        }
        if(kind==ArtVisualEntity.CAUSAL_IMPACT){
            double progress=Math.clamp(age/life,0,1),radius=r*(.2+.8*smooth(progress));
            clock(p,b,new Vec3(0,.08,0),radius,t*.3,.7*f);
            for(int plane=0;plane<3;plane++)ring(p,b,new Vec3(0,.8,0),radius*(.65+.1*plane),.09*f,t*.2,plane,tint(plane==1?GOLD:PURPLE,.7*f));
            for(int n=0;n<12;n++){Vec3 c=polar(n*Math.PI/6+age*.01,radius,.8+Math.sin(n)*.3);beam(p,b,c.scale(.4),c,.06*f,tint(n%2==0?GOLD:PURPLE,f));}return;
        }
        boolean verdict=kind==ArtVisualEntity.CAUSAL_VERDICT;Vec3 center=new Vec3(0,verdict?4:1.8,0);
        if(!verdict && data.y>r){
            ring(p,b,new Vec3(0,.06,0),data.y*f,.04,t*.1,0,tint(PURPLE,.16*f));
            int count=decorations(32);for(int n=0;n<count;n++){double phase=(age*.014+n*.618)%1,a=n*2.39996+t*.04,rr=data.y-(data.y-r*.4)*phase;Vec3 c=polar(a,rr,.15+Math.sin(phase*Math.PI)*.35);beam(p,b,c,c.add(polar(a, .7,0)),.05*f,tint(PURPLE,Math.sin(phase*Math.PI)*.35*f));}
        }
        double charge=verdict?smooth(age/60d):1,coreRadius=(verdict?.4+charge*1.4:r*.17)*f;
        sphere(p,b,center,coreRadius,0xFF080611);
        for(int n=0;n<3;n++){
            p.pushPose();p.translate(center.x,center.y,center.z);p.mulPose(com.mojang.math.Axis.ZP.rotation((float)(.35+n*.65)));p.mulPose(com.mojang.math.Axis.XP.rotation((float)(n*.4)));
            double rr=(verdict?r*(.25+n*.12):coreRadius*(1.25+n*.45));seal(p,b,CLOCK,Vec3.ZERO,rr*f,t*(n%2==0?.13:-.2),0,tint(n%2==0?GOLD:PURPLE,(verdict?.48:.65)*f));
            ring(p,b,Vec3.ZERO,rr*f,.11*f,t,0,tint(GOLD,.55*f));
            if(n==0){ring(p,b,Vec3.ZERO,rr*f,.32*f,t,0,tint(PURPLE,.24*f));ring(p,b,Vec3.ZERO,rr*f,.16*f,t,0,tint(WHITE_GOLD,.6*f));}
            p.popPose();
        }
        // Photon ring and lens boundary make the damaging core distinct from the far pull perimeter.
        ring(p,b,center,coreRadius*1.06,.09*f,t,1,tint(WHITE_GOLD,.9*f));clock(p,b,new Vec3(0,.09,0),r*f,t*.12,.5*f);
        if(verdict){
            seal(p,b,CLOCK,new Vec3(0,10+charge*3,0),r*.5*f,-t*.2,0,tint(GOLD,.6*f));
            for(int n=0;n<6;n++){Vec3 base=polar(n*Math.PI/3,r*f,.12);beam(p,b,base,center.add(0,8+charge*2,0),.045*f,tint(PURPLE,.32*f));}
            for(int n=0;n<3;n++){double phase=age-(20+n*20);if(phase>=0 && phase<13){double pulse=Math.sin(phase/13*Math.PI);ring(p,b,new Vec3(0,.25,0),r*(.3+.7*phase/13),.24*pulse,t,0,tint(n==2?WHITE_GOLD:GOLD,f));beam(p,b,center.add(0,12,0),Vec3.ZERO,.35*pulse,tint(GOLD,f));glow(p,b,center,3*pulse,tint(PURPLE,.55*f));}}
        }else{
            for(int n=0;n<8;n++){double a=n*Math.PI/4+t*.1;Vec3 c=polar(a,coreRadius*1.7,center.y+Math.sin(a*2+t)*.35);glow(p,b,c,.15*f,tint(n%2==0?GOLD:PURPLE,.7*f));}
        }
    }
    private static void clock(PoseStack p,MultiBufferSource b,Vec3 center,double radius,double angle,double fade){
        seal(p,b,CLOCK,center,radius,angle*.12,0,tint(GOLD,.7*fade));
        for(int n=0;n<2;n++){double a=angle*(n==0?1:7);Vec3 end=center.add(polar(a,radius*(n==0?.55:.82),.02)),side=polar(a+Math.PI/2,.045,0);surface(p,b,WHITE,center.add(side),end,end.subtract(side),center.subtract(side),opacity(n==0?GOLD:PURPLE,.8*fade));beam(p,b,center,end,.035,tint(GOLD,.7*fade));}
    }
}
