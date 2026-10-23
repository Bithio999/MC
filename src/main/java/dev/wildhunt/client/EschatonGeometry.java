package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildhunt.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static dev.wildhunt.client.VfxMesh.*;

/** Original Origin-Sovereign optics. Warning geometry is shared with the server's analytic patterns. */
final class EschatonGeometry {
    static final int CYAN=0xFF9DF7ED,PEARL=0xFFE9FDFF,RED=0xFFFF727F,GOLD=0xFFFFDA96,INK=0xFF11182B;
    private EschatonGeometry(){}
    static boolean handles(int kind){return kind>=ArtVisualEntity.ESCHATON_EDGE && kind<=ArtVisualEntity.ESCHATON_SHATTER;}
    static void render(ArtVisualEntity e,PoseStack p,MultiBufferSource b,float partial){
        float age=e.barrage().isEmpty()?e.tickCount+partial:e.barrageAge(partial);var plan=e.barrage();double fade=VfxMesh.fade(age,e.lifetime());
        if(e.kind()==ArtVisualEntity.ESCHATON_ASCENSION){ascension(p,b,age,e.lifetime(),e.end().x,(int)e.end().y);return;}
        if(e.kind()==ArtVisualEntity.ESCHATON_SHATTER){shatter(p,b,age,e.lifetime(),Math.max(1,e.end().x));return;}
        if(plan.isEmpty())return;
        switch(e.kind()-ArtVisualEntity.ESCHATON_EDGE){
            case 0->edges(p,b,plan,age);
            case 1->loom(p,b,plan,age);
            case 2->genesis(p,b,plan,age);
            case 3->testament(p,b,plan,age);
            case 4->wake(p,b,plan,age);
            case 5->exchange(p,b,age);
            case 6->thronefall(p,b,plan,age);
            case 7->apocalypse(p,b,plan,age);
        }
    }
    private static void edges(PoseStack p,MultiBufferSource b,CompoundTag state,float age){
        for(int wave=0;wave<3;wave++){
            double strike=EschatonPattern.edgeTick(wave),time=age-strike;if(time>20)continue;
            double inner=EschatonPattern.edgeInner(wave),outer=EschatonPattern.edgeOuter(wave),angle=EschatonPattern.edgeAngle(state,wave),half=Math.toRadians(65);
            double preview=age<strike?.26:.8*Math.exp(-time*.13);int count=segments(64);var mesh=b.getBuffer(VfxMaterials.additive(RIFT));
            for(int n=0;n<count;n++){
                double a=angle-half+2*half*n/count,q=angle-half+2*half*(n+1)/count;
                if(time<0)quad(p.last(),mesh,polar(a,inner,.14+wave*.006),polar(q,inner,.14+wave*.006),polar(q,outer,.14+wave*.006),polar(a,outer,.14+wave*.006),tint(RED,preview*(.6+.4*Math.sin(age*.12+wave))),true);
                double r=time<0?outer:inner+(outer-inner)*smooth(time/7);
                beam(p,b,polar(a,r,time<0?.18:1.6),polar(q,r,time<0?.18:1.6),time<0?.065:.3,tint(time<0?RED:PEARL,preview));
                if(time>=0)beam(p,b,polar(a,r,.2),polar(a,r,5+2*Math.sin(n*.3)),.045,tint(CYAN,preview*.5));
            }
            for(int s:new int[]{-1,1}){double a=angle+s*half;beam(p,b,polar(a,inner,.2),polar(a,outer,.2),.065,tint(GOLD,.7));}
            if(inner>0 && time<0)arc(p,b,inner,angle-half,angle+half,.055,tint(GOLD,.5));
        }
        seal(p,b,CIRCUIT,new Vec3(0,.16,0),4,age*.004,0,tint(CYAN,.55));
    }
    private static void arc(PoseStack p,MultiBufferSource b,double r,double a,double q,double width,int color){int count=segments(48);for(int n=0;n<count;n++)beam(p,b,polar(a+(q-a)*n/count,r,.16),polar(a+(q-a)*(n+1)/count,r,.16),width,color);}
    private static void loom(PoseStack p,MultiBufferSource b,CompoundTag state,float age){
        double active=age>=32 && age<112?1:age<32?.25:Math.max(0,(132-age)/20);Vec3 origin=EschatonPattern.loomStart(state);
        ring(p,b,origin,EschatonPattern.LOOM_RADIUS,.08,0,0,tint(RED,.5*active));
        seal(p,b,GATE,origin,6,age*.012,0,tint(CYAN,.5*active));
        for(int n=0;n<EschatonPattern.count(state);n++){
            Vec3 end=EschatonPattern.loomEnd(state,age,n),axis=end.subtract(origin).normalize(),side=new Vec3(-axis.z,0,axis.x);
            int color=age<32?RED:n%2==0?CYAN:PEARL;
            beam(p,b,origin,end,age<32?.04:.16,tint(color,active));
            beam(p,b,origin.add(side.scale(1.1)),end.add(side.scale(1.1)),.035,tint(CYAN,.45*active));beam(p,b,origin.subtract(side.scale(1.1)),end.subtract(side.scale(1.1)),.035,tint(CYAN,.45*active));
            if(age>=32){for(int k=0;k<5;k++){double t=((age*.025+k/5d)%1);Vec3 at=origin.lerp(end,t);ring(p,b,at,.18+Math.sin(t*Math.PI)*.45,.035,age*.03,1,tint(PEARL,.7*active));}}
            glow(p,b,end,.8,tint(color,.7*active));
        }
        glow(p,b,origin,2.8,tint(PEARL,.6*active));
    }
    private static void genesis(PoseStack p,MultiBufferSource b,CompoundTag state,float age){
        for(int n=0;n<Math.min(7,state.getInt("warned"));n++){
            double birth=n*14,strike=EschatonPattern.meteorTick(n),t=age-strike;if(age<birth || t>30)continue;var element=ThothElement.at(n);Vec3 at=EschatonPattern.meteor(state,n);
            if(t<0){
                ring(p,b,at.add(0,-.7,0),4.5,.1,0,0,tint(RED,.8));seal(p,b,CIRCUIT,at.add(0,-.68,0),4.25,age*.01,0,tint(element.color,.6));
                double charge=smooth((age-birth)/32d);Vec3 star=at.add(0,16+3*Math.sin(n),0);signature(p,b,star,element,age+n*9,charge);
                beam(p,b,at,star,.018,tint(element.color,.2+.3*charge));
            }else{
                double f=Math.exp(-t*.12);beam(p,b,at.add(0,Math.max(0,24-t*6),0),at,.6*f,tint(PEARL,f));
                ring(p,b,at.add(0,-.5,0),Math.min(7,t*.55)+.2,.2*f,0,0,tint(element.color,.8*f));
                signature(p,b,at,element,age+n*9,f);glow(p,b,at,5.5,tint(element.color,.7*f));
            }
        }
    }
    private static void signature(PoseStack p,MultiBufferSource b,Vec3 at,ThothElement element,float age,double power){
        double r=.7+power*.9;
        switch(element){
            case FIRE->{for(int n=0;n<4;n++)billboard(p,b,FLAME,at.add(polar(n*Math.PI/2,.6,0)),.75*r,1.5*r,age*.01+n,tint(element.color,.7*power),true);}
            case WATER->{seal(p,b,WAVE,at,r*1.5,age*.02,0,tint(element.color,.6*power));ring(p,b,at,r,.12,0,1,tint(PEARL,.6*power));}
            case LIGHTNING->{for(int n=0;n<5;n++)bolt(p,b,at,at.add(polar(n*Math.TAU/5,r*2,Math.sin(n)*1.4)),.045,age,n, tint(element.color,power));}
            case EARTH->{for(int n=0;n<5;n++)crystal(p,b,at.add(polar(n*Math.TAU/5,r*.6,-.6)),at.add(polar(n*Math.TAU/5,r*.9,1.4)),.22*r,tint(element.color,.85*power));}
            case WIND->{for(int strand=0;strand<3;strand++){Vec3 previous=at.add(polar(strand*Math.TAU/3,r,-1.2));for(int n=1;n<=segments(24);n++){double t=n/(double)segments(24);Vec3 q=at.add(polar(t*Math.TAU+age*.035+strand*Math.TAU/3,r*(.5+.5*Math.sin(t*Math.PI)),-1.2+t*2.4));beam(p,b,previous,q,.04,tint(element.color,.75*power));previous=q;}}}
            case LIGHT->{for(int n=0;n<12;n++){double a=n*Math.TAU/12+age*.004;beam(p,b,at.add(polar(a,r*.3,0)),at.add(polar(a,r*2,Math.sin(a*2)*.4)),.04,tint(element.color,.8*power));}ring(p,b,at,r,.05,0,1,tint(PEARL,power));}
            case SPACE->{sphere(p,b,at,r*.55,INK);for(int plane=0;plane<3;plane++)ring(p,b,at,r*(.9+plane*.14),.05,age*.02*(plane%2==0?1:-1),plane,tint(element.color,.8*power));}
        }
        glow(p,b,at,r,tint(element.color,.65*power));
    }
    private static void testament(PoseStack p,MultiBufferSource b,CompoundTag state,float age){
        double t=age-38,f=t<0?1:Math.exp(-t*.12);Vec3 side=EschatonPattern.forward(state).yRot((float)(Math.PI/2)).scale(2.4);
        for(int n=0;n<3;n++){
            Vec3 a=EschatonPattern.cutEnd(state,n,-1),q=EschatonPattern.cutEnd(state,n,1);
            if(t<0){
                quad(p.last(),b.getBuffer(VfxMaterials.additive(RIFT)),a.add(side),q.add(side),q.subtract(side),a.subtract(side),tint(RED,.22+.08*Math.sin(age*.25)),true);
                beam(p,b,a.add(side),q.add(side),.07,tint(RED,.8));beam(p,b,a.subtract(side),q.subtract(side),.07,tint(RED,.8));
                for(int k=0;k<5;k++){Vec3 at=a.lerp(q,k/4d);beam(p,b,at.add(0,-2,0),at.add(0,8,0),.025,tint(CYAN,.4));}
            }else{
                quad(p.last(),b.getBuffer(VfxMaterials.additive(RIFT)),a.add(0,-8*f,0),q.add(0,-8*f,0),q.add(0,8*f,0),a.add(0,8*f,0),tint(CYAN,.8*f),true);
                beam(p,b,a,q,.25*f,tint(PEARL,f));beam(p,b,a.add(side),q.add(side),.08*f,tint(RED,.8*f));
            }
        }
    }
    private static void wake(PoseStack p,MultiBufferSource b,CompoundTag state,float age){
        for(int wave=0;wave<3;wave++){
            double t=age-EschatonPattern.waveTick(wave);if(t>52)continue;double r=t<0?56:EschatonPattern.waveRadius(age,wave),gap=EschatonPattern.gapAngle(state,wave);
            int count=segments(112);for(int n=0;n<count;n++){
                double a=n*Math.TAU/count,q=(n+1)*Math.TAU/count;Vec3 delta=polar((a+q)*.5,1,0);
                if(EschatonPattern.inGap(delta,state,wave)){if(n%4==0)beam(p,b,polar(a,Math.max(0,r-1),.15),polar(a,r+1,.15),.06,tint(GOLD,.75));continue;}
                if(t<0){if(wave==0)beam(p,b,polar(a,r,.18),polar(q,r,.18),.05,tint(RED,.55));}
                else{double f=Math.min(1,(52-t)/8);quad(p.last(),b.getBuffer(VfxMaterials.additive(WAVE)),polar(a,r,.1),polar(q,r,.1),polar(q,r,7+.5*Math.sin(a*12-age*.08)),polar(a,r,7+.5*Math.sin(a*12-age*.08)),tint(wave%2==0?CYAN:RED,.25*f),true);beam(p,b,polar(a,r,.2),polar(q,r,.2),.16,tint(PEARL,.8*f));beam(p,b,polar(a,r,7),polar(q,r,7),.045,tint(CYAN,.7*f));}
            }
            if(t<0)for(int side:new int[]{-1,1}){double a=gap+(side<0?Math.PI:0);for(double offset:new double[]{-.38,.38})beam(p,b,polar(a+offset,2,.2),polar(a+offset,56,.2),.045,tint(GOLD,.75));}
        }
    }
    private static void exchange(PoseStack p,MultiBufferSource b,float age){
        double charge=smooth(age/48d),f=age<48?1:Math.exp(-(age-48)*.13);Vec3 core=new Vec3(0,4,0);
        for(int n=0;n<6;n++){double a=n*Math.TAU/6;Vec3 last=polar(a,5,.3);for(int k=1;k<=segments(32);k++){double t=k/(double)segments(32),theta=a+t*Math.TAU+age*.02;Vec3 at=polar(theta,5*(1-t),.3+4*t);beam(p,b,last,at,.045,tint(n%2==0?GOLD:CYAN,.65*f));last=at;}}
        for(int plane=0;plane<3;plane++)ring(p,b,core,1.4+plane*.5,.055,age*.025*(plane%2==0?1:-1),plane,tint(CYAN,.75*f));glow(p,b,core,2.8,tint(PEARL,.4*charge*f));
        seal(p,b,CIRCUIT,new Vec3(0,.12,0),5,age*.007,0,tint(GOLD,.5*f));
    }
    private static void thronefall(PoseStack p,MultiBufferSource b,CompoundTag state,float age){
        Vec3 at=EschatonPattern.point(state,"landing");double t=age-36;
        ring(p,b,at.add(0,.15,0),9,.12,0,0,tint(t<0?RED:CYAN,t<0?.85:Math.exp(-t*.12)));
        if(t<0){seal(p,b,RIFT,at.add(0,.16,0),8.7,-age*.008,0,tint(RED,.4));for(int n=0;n<8;n++){double a=n*Math.TAU/8;Vec3 q=at.add(polar(a,9,0));beam(p,b,q,q.add(0,7*smooth(age/36d),0),.04,tint(CYAN,.5));}}
        else{double f=Math.exp(-t*.12);beam(p,b,at,at.add(0,Math.max(0,18-t*3),0),.7*f,tint(PEARL,f));for(int n=0;n<3;n++)ring(p,b,at.add(0,n*.4,0),Math.min(12,.6+t*.75+n),.18*f,0,0,tint(n%2==0?CYAN:GOLD,.8*f));for(int n=0;n<decorations(12);n++){double a=n*Math.TAU/12;crystal(p,b,at.add(polar(a,4,0)),at.add(polar(a,5,2+Math.sin(n)*1.4)),.35,tint(PEARL,.7*f));}glow(p,b,at,8,tint(PEARL,.65*f));}
    }
    private static void apocalypse(PoseStack p,MultiBufferSource b,CompoundTag state,float age){
        boolean interrupted=state.getBoolean("interrupted");double t=age-200,charge=smooth(age/200d),fade=interrupted?.25:t<0?1:Math.exp(-t*.08);Vec3 sun=new Vec3(0,13,0);
        ring(p,b,new Vec3(0,.16,0),EschatonPattern.APOCALYPSE_RADIUS,.15,0,0,tint(RED,.8*fade));
        for(int n=0;n<3;n++){
            Vec3 at=EschatonPattern.point(state,"seal_"+n);if(at.y<-90)continue;boolean broken=(state.getInt("broken")&(1<<n))!=0;
            if(broken){
                ring(p,b,at.add(0,-.8,0),7,.16,0,0,tint(GOLD,.9*fade));seal(p,b,GATE,at.add(0,-.78,0),6.8,age*.004,0,tint(GOLD,.4*fade));
                int count=segments(64);for(int k=0;k<count;k++){double a=k*Math.TAU/count,q=(k+1)*Math.TAU/count;quad(p.last(),b.getBuffer(VfxMaterials.additive(CIRCUIT)),at.add(polar(a,7,-1)),at.add(polar(q,7,-1)),at.add(polar(q,7,8)),at.add(polar(a,7,8)),tint(GOLD,.12*fade),true);}
            }else{
                ring(p,b,at.add(0,-.8,0),3.5,.1,0,0,tint(RED,.7*fade));Vec3 top=at.add(0,2,0);beam(p,b,top,sun,.055,tint(CYAN,.65*fade));
                for(int k=0;k<4;k++){double r=((age*.015+k*.25)%1);glow(p,b,top.lerp(sun,r),.3,tint(PEARL,.7*fade));}
            }
        }
        if(t<0 && !interrupted){
            // Stable lensed accretion and photon rings surround the dark world seed.
            // The large canopy is decorative; the red ground perimeter is the hit boundary.
            SingularityGeometry.originSeed(p,b,sun,.7+charge*2.5,age,state.getInt("seed"));
            for(int n=0;n<3;n++){p.pushPose();p.translate(sun.x,sun.y,sun.z);p.mulPose(Axis.XP.rotation(n*.72f));ring(p,b,Vec3.ZERO,3.2+charge*3+n*.7,.11,age*.01*(n%2==0?1:-1),1,tint(n%2==0?CYAN:RED,.8));p.popPose();}
            Vec3 canopy=new Vec3(0,24,0);double canopyRadius=12+charge*20;
            seal(p,b,CIRCUIT,canopy,canopyRadius,-age*.0018,0,tint(CYAN,.24*charge));
            ring(p,b,canopy,canopyRadius,.12,0,0,tint(PEARL,.55*charge));
            ring(p,b,canopy,canopyRadius*.88,.055,0,0,tint(RED,.7*charge));
            for(int n=0;n<7;n++){
                double a=n*Math.TAU/7+age*.0018;Vec3 node=canopy.add(polar(a,canopyRadius,0));
                seal(p,b,GATE,node,1.15,age*.008+n,0,tint(PEARL,.65*charge));
                Vec3 previous=sun.add(polar(a,3,0));int steps=segments(24);
                for(int k=1;k<=steps;k++){double v=k/(double)steps;Vec3 q=sun.add(polar(a+Math.sin(v*Math.PI)*.3,3+(canopyRadius-3)*v,11*v+5*Math.sin(v*Math.PI)));beam(p,b,previous,q,.07,tint(n%2==0?CYAN:RED,.42*charge));previous=q;}
            }
            for(int n=0;n<decorations(14);n++){double a=n*Math.TAU/14+age*.002;Vec3 start=polar(a,20+charge*25,.3);beam(p,b,start,sun.add(polar(a,1.5,0)),.025,tint(CYAN,.35*charge));}
            seal(p,b,CIRCUIT,new Vec3(0,.14,0),16,age*.003,0,tint(CYAN,.35));glow(p,b,sun,8+charge*4,tint(RED,.15*charge));
        }else if(!interrupted){
            double r=Math.min(56,t*3.8);for(int plane=0;plane<3;plane++)ring(p,b,sun,r,.8*fade,t*.006,plane,tint(plane==1?RED:PEARL,.7*fade));
            glow(p,b,sun,18,tint(PEARL,.7*fade));for(int n=0;n<16;n++){double a=n*Math.TAU/16;beam(p,b,sun,sun.add(polar(a,Math.min(56,12+t*2),Math.sin(a*3)*8)),.25*fade,tint(CYAN,.75*fade));}
        }else{shatter(p,b,age%24,30,10);}
    }
    private static void ascension(PoseStack p,MultiBufferSource b,float age,int life,double radius,int form){
        double birth=smooth(age/20d),fade=smooth((life-age)/24d);int color=form>=3?RED:CYAN;
        for(int n=0;n<3;n++){double r=(radius*.35+n*1.4)*birth;ring(p,b,new Vec3(0,1+n*2.2,0),r,.09,age*.008,0,tint(color,.7*fade));seal(p,b,GATE,new Vec3(0,1+n*2.2,0),r*.95,-age*.006+n,0,tint(color,.3*fade));}
        for(int n=0;n<decorations(12);n++){double a=n*Math.TAU/12;Vec3 from=polar(a,radius*.5,.2);beam(p,b,from,from.add(0,8+birth*12,0),.04,tint(PEARL,.55*fade));glow(p,b,from.add(0,age*.15,0),.35,tint(color,.7*fade));}
        glow(p,b,new Vec3(0,4,0),6,tint(color,.3*fade));
    }
    private static void shatter(PoseStack p,MultiBufferSource b,float age,int life,double radius){
        double t=Math.clamp(age/life,0,1),f=Math.pow(1-t,2),r=radius*(1-Math.exp(-t*5));
        for(int plane=0;plane<3;plane++)ring(p,b,Vec3.ZERO,r*(1-plane*.15),.09*f,age*.006,plane,tint(plane==1?GOLD:CYAN,.8*f));
        for(int n=0;n<decorations(18);n++){double a=n*2.399963,up=Math.sin(n*7.1);Vec3 end=new Vec3(Math.cos(a)*r,up*r*.5,Math.sin(a)*r);beam(p,b,end.scale(.65),end,.055*f,tint(PEARL,.8*f));glow(p,b,end,.18+.2*f,tint(CYAN,.6*f));}glow(p,b,Vec3.ZERO,radius*.6,tint(PEARL,.7*Math.exp(-age*.15)));
    }
}
