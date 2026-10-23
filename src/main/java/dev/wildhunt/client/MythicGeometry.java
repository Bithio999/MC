package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildhunt.ArtVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static dev.wildhunt.client.VfxMesh.*;

/** Distinct silk looms, rune circuitry, living vegetation, prismatic glass, plasma, ice gardens and gates. */
final class MythicGeometry {
    private static final int SILVER=0xFFC5E9FF,GOLD=0xFFFFD586,PINK=0xFFFFACDE,GREEN=0xFF9BE5AE,FIRE=0xFFFF9C47,ICE_COLOR=0xCFB9F3ED,PURPLE=0xFFD1B2FF;
    private MythicGeometry(){}
    static void render(PoseStack p,MultiBufferSource b,int kind,Vec3 data,float age,int life,int style){
        double f=fade(age,life),r=Math.abs(data.x),t=age*.05;
        switch(kind){
            case ArtVisualEntity.THREAD_LINE->{
                Vec3 axis=data.normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();int count=segments(40);
                for(int strand=0;strand<4;strand++){Vec3 last=Vec3.ZERO;for(int n=1;n<=count;n++){double a=n/(double)count,w=Math.sin(a*Math.PI);Vec3 next=data.scale(a).add(side.scale(Math.sin(a*32-age*.11+strand*Math.PI/2)*.07*w)).add(up.scale(Math.cos(a*32-age*.11+strand*Math.PI/2)*.07*w)).add(0,-w*.25,0);beam(p,b,last,next,.025*f,tint(strand%2==0?PINK:0xFFEEE5FF,.75*f));last=next;}}
                p.pushPose();p.translate(data.x,data.y,data.z);p.mulPose(new org.joml.Quaternionf().rotationTo(0,1,0,(float)axis.x,(float)axis.y,(float)axis.z));seal(p,b,SILK,Vec3.ZERO,.75*f,age*.02,0,tint(PINK,.65*f));p.popPose();
                for(int n=0;n<4;n++){double a=(t*.25+n*.25)%1;glow(p,b,data.scale(a),.16*f,tint(PINK,.8*f));}
            }
            case ArtVisualEntity.RUNE_MINE,ArtVisualEntity.RUNE_BURST->{
                if(data.y>0){
                    double pulse=smooth(age/life);seal(p,b,RIFT,new Vec3(0,.08,0),r*f,t*.2,0,tint(FIRE,.65*f));
                    for(int n=0;n<3;n++){Vec3 c=polar(n*Math.PI*2/3+t*.5,r*(.85-.65*pulse),.3+pulse);beam(p,b,c,new Vec3(0,1.5*pulse,0),.06*f,tint(GOLD,.85*f));glow(p,b,c,.25*f,tint(FIRE,f));}return;
                }
                boolean burst=kind==ArtVisualEntity.RUNE_BURST;double charge=burst?smooth(age/8d):smooth(age/20d),radius=r*(burst?.3+.7*smooth(age/life):1)*f;
                seal(p,b,CIRCUIT,new Vec3(0,.08,0),radius,t*.1,0,tint(GOLD,.85*f));seal(p,b,CIRCUIT,new Vec3(0,burst?.3:.5*charge,0),radius*.6,-t*.22,0,tint(0xFFFFE8AB,.55*f));
                for(int n=0;n<6;n++){Vec3 base=polar(n*Math.PI/3+t*.08,radius*.7,.15),tip=base.add(0,burst?Math.sin(age/life*Math.PI)*4:.45*charge,0);crystal(p,b,base,tip,.15*f,0xFFC49D67);
                    if(burst)beam(p,b,base,tip,.16*f,tint(GOLD,f));else beam(p,b,base,base.add(polar(n*Math.PI/3+Math.PI/2,.4,.3)),.035*f,tint(GOLD,.65*charge));}
                if(burst){ring(p,b,new Vec3(0,.2,0),radius,.2*f,0,0,tint(GOLD,f));glow(p,b,new Vec3(0,.5,0),2.2*f,tint(GOLD,.65*f));}
            }
            case ArtVisualEntity.LIVING_GROVE,ArtVisualEntity.WINTER_ORCHARD->garden(p,b,r,age,life,kind==ArtVisualEntity.WINTER_ORCHARD);
            case ArtVisualEntity.BRIAR->{
                for(int n=0;n<6;n++){double a=n*Math.PI/3;List<Vec3> vine=new ArrayList<>();int count=segments(16);for(int k=0;k<=count;k++){double s=k/(double)count;vine.add(polar(a+s*1.4,r*(1-.65*s)*f,data.y*s*f));}tube(p,b,vine,.12*f,.018,BARK,0xFF83AE74);
                    for(int k=3;k<count;k+=4){Vec3 c=vine.get(k);crystal(p,b,c,c.add(polar(a+k,.3*f,.25*f)),.06,0xFFDCF7AF);} }
                seal(p,b,LEAF,new Vec3(0,.07,0),r*f,t*.1,0,tint(GREEN,.55*f));
            }
            case ArtVisualEntity.MIRROR_WARD,ArtVisualEntity.MIRROR_SHARD,ArtVisualEntity.CRYSTAL_BULWARK->{
                boolean ward=kind==ArtVisualEntity.CRYSTAL_BULWARK;int count=kind==ArtVisualEntity.MIRROR_SHARD?3:ward?6:8;
                for(int n=0;n<count;n++){double a=n*Math.PI*2/count+age*(ward?.012:.035);Vec3 center=polar(a,r,ward?1.1:Math.sin(t*1.5+n)*.2);
                    p.pushPose();p.translate(center.x,center.y,center.z);p.mulPose(com.mojang.math.Axis.YP.rotation((float)-a));double w=(ward?.5:.4)*f,h=(ward?.9:.7)*f;
                    Vec3 top=new Vec3(0,h,0),right=new Vec3(w,0,.1),bottom=new Vec3(0,-h,0),left=new Vec3(-w,0,.1),middle=new Vec3(0,0,.16);
                    surface(p,b,ICE,top,right,middle,middle,ward?0x9981CCBD:0x997BBED4);surface(p,b,ICE,right,bottom,middle,middle,ward?0x998EDACD:0x99CBDFF3);surface(p,b,ICE,bottom,left,middle,middle,0x7799B4E0);surface(p,b,ICE,left,top,middle,middle,0x99B2E1EE);
                    for(Vec3[] edge:new Vec3[][]{{top,right},{right,bottom},{bottom,left},{left,top}})beam(p,b,edge[0],edge[1],.035*f,tint(ward?GREEN:SILVER,.85*f));
                    seal(p,b,ward?FROST:MIRROR,Vec3.ZERO,(ward?.42:.36)*f,t*.2,Math.PI/2,tint(ward?GREEN:SILVER,.65*f));p.popPose();}
                if(ward)ring(p,b,new Vec3(0,.08,0),r,.04*f,t,0,tint(GREEN,.45*f));
            }
            case ArtVisualEntity.PRISM_RAY,ArtVisualEntity.PLASMA_LANCE->{
                boolean plasma=kind==ArtVisualEntity.PLASMA_LANCE;Vec3 axis=data.normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
                beam(p,b,Vec3.ZERO,data,.065*f,tint(0xFFFFFFFF,f));beam(p,b,Vec3.ZERO,data,.22*f,tint(plasma?FIRE:SILVER,.65*f));
                for(int n=0;n<(plasma?2:3);n++){int color=plasma?n==0?FIRE:0xFFC8A9FF:n==0?0xFFAAEFFF:n==1?0xFFFAB6EE:0xFFFFEAB3;
                    Vec3 last=Vec3.ZERO;int count=segments(48);for(int k=1;k<=count;k++){double s=k/(double)count,a=s*data.length()*.9-age*.15+n*Math.PI*2/3;Vec3 next=data.scale(s).add(side.scale(Math.cos(a)*.18*f)).add(up.scale(Math.sin(a)*.18*f));beam(p,b,last,next,.035*f,tint(color,.8*f));last=next;}}
                for(double d=2;d<data.length();d+=plasma?5:7){p.pushPose();Vec3 c=axis.scale(d);p.translate(c.x,c.y,c.z);p.mulPose(new org.joml.Quaternionf().rotationTo(0,1,0,(float)axis.x,(float)axis.y,(float)axis.z));seal(p,b,plasma?CIRCUIT:MIRROR,Vec3.ZERO,(plasma?.65:.4)*f,age*.03,0,tint(plasma?FIRE:SILVER,.6*f));p.popPose();}
                glow(p,b,data,.75*f,tint(plasma?FIRE:SILVER,.8*f));
            }
            case ArtVisualEntity.THUNDERHEAD->{
                for(int n=0;n<decorations(16);n++){double a=n*2.39996-t*.07;Vec3 c=polar(a,r*(.25+.6*((n*7)%16)/16d),5.5+Math.sin(n+t)*.6);billboard(p,b,CLOUD,c,2.3*f,1.6*f,Math.sin(n)*.5,opacity(n%2==0?0xFF2E2637:0xFF4A3138,.8*f),false);}
                seal(p,b,CIRCUIT,new Vec3(0,.08,0),r*f,t*.1,0,tint(FIRE,.65*f));
                for(int n=0;n<6;n++){double phase=(age+n*11)%43;if(phase<8){Vec3 c=polar(n*Math.PI/3,r*.65,5.7);bolt(p,b,c,c.multiply(.8,0,.8).add(0,.15,0),.13*f,age,n,tint(GOLD,Math.sin(phase/8*Math.PI)*f));}}
                ring(p,b,new Vec3(0,5.2,0),r*.75*f,.16,t,0,tint(FIRE,.5*f));
            }
            case ArtVisualEntity.CATACLYSM_FRONT->{
                double hit=life-12,eruption=Math.sin(Math.clamp((age-hit)/12,0,1)*Math.PI);seal(p,b,RIFT,new Vec3(0,.07,0),r*f,t*.02,0,tint(FIRE,.8*f));ring(p,b,new Vec3(0,.08,0),r*f,.055,t,0,tint(GOLD,.65*f));
                if(age>=hit)for(int n=0;n<7;n++){double a=n*Math.PI*2/7;Vec3 c=polar(a,r*.5,0);crystal(p,b,c,c.add(polar(a,r*.2,5*eruption)),.45*f,0xFF5D3541);beam(p,b,c,c.add(0,6*eruption,0),.16*f,tint(FIRE,f));billboard(p,b,FLAME,c.add(0,2*eruption,0),.7*f,2.5*eruption,a,tint(FIRE,f),true);}
            }
            case ArtVisualEntity.CRYSTAL_BLOOM->{
                double bloom=smooth((age-24)/8d);seal(p,b,FROST,new Vec3(0,.08,0),r*f,t*.12,0,tint(GREEN,.85*f));
                for(int n=0;n<8;n++){double a=n*Math.PI/4;Vec3 root=polar(a,.25,0),tip=polar(a,r*.8*bloom,.2+4*bloom);if(bloom>0)crystal(p,b,root,tip,(.08+.28*bloom)*f,ICE_COLOR);else glow(p,b,polar(a,r*.8,.1),.12*f,tint(GREEN,.8*f));}
                if(bloom>0)ring(p,b,new Vec3(0,.2,0),r*(.4+.6*bloom),.12*f,t,0,tint(SILVER,.8*f));
            }
            case ArtVisualEntity.TWIN_GATE->gate(p,b,r,age,life,data.y>0);
            case ArtVisualEntity.PARALLAX->{
                if(style==1){
                    double close=smooth(age/8d);seal(p,b,SILK,new Vec3(0,.15,0),r*f,t*.2,0,tint(PINK,.7*f));
                    for(int n=0;n<8;n++){Vec3 c=polar(n*Math.PI/4,r*(1-close),1);beam(p,b,c,c.scale(.15),.075*f,tint(PINK,f));}
                    seal(p,b,SILK,new Vec3(0,1,0),r*.6*f,-t*.3,Math.PI/2,tint(PINK,.5*f));glow(p,b,new Vec3(0,1,0),1.6*f,tint(PINK,.6*f));return;
                }
                double hit=16,close=smooth(age/16d),distance=r*(1-close),flash=Math.sin(Math.clamp((age-hit)/(life-hit),0,1)*Math.PI);
                for(int side:new int[]{-1,1}){Vec3 a=new Vec3(side*distance,.15,-r),q=new Vec3(side*distance,3,r);beam(p,b,a,q,.13*f,tint(side<0?PURPLE:GOLD,f));beam(p,b,a.add(0,2.8,0),q.add(0,-2.8,0),.06*f,tint(PURPLE,.7*f));
                    p.pushPose();p.translate(side*distance,1.4,0);seal(p,b,RIFT,Vec3.ZERO,r*.65*f,t*.1,Math.PI/2,tint(PURPLE,.4*f));p.popPose();}
                if(age>=hit)glow(p,b,new Vec3(0,1.4,0),2*flash,tint(PURPLE,.7*f));
            }
            case ArtVisualEntity.CROSSROADS->{
                seal(p,b,GATE,new Vec3(0,.09,0),r*f,t*.1,0,tint(PURPLE,.5*f));
                for(int n=0;n<4;n++){double a=n*Math.PI/2;Vec3 c=polar(a,r*f,.15);beam(p,b,c,c.scale(-1).add(0,.3,0),.04*f,tint(GOLD,.65*f));p.pushPose();p.translate(c.x,.8,c.z);seal(p,b,RIFT,Vec3.ZERO,.9*f,-t*.2,Math.PI/2,tint(PURPLE,.7*f));p.popPose();}
            }
            case ArtVisualEntity.GATE_TRANSIT->{
                Vec3 axis=data.normalize(),side=axis.cross(new Vec3(0,1,0)).normalize();for(int n=0;n<3;n++)beam(p,b,side.scale((n-1)*.18),data.add(side.scale((n-1)*.18)),.08*f,tint(n==1?GOLD:PURPLE,.7*f));
                glow(p,b,data,.7*f,tint(GOLD,f));
            }
        }
    }
    private static void garden(PoseStack p,MultiBufferSource b,double r,float age,int life,boolean winter){
        double f=fade(age,life),growth=smooth(age/20d),t=age*.05;int color=winter?ICE_COLOR:GREEN;
        seal(p,b,winter?FROST:LEAF,new Vec3(0,.07,0),r*f,t*.025,0,tint(color,.4*f));
        for(int tree=0;tree<5;tree++){double a=tree*Math.PI*2/5;Vec3 root=polar(a,r*.7,0);double height=(winter?4.2:3.1)*growth*f;List<Vec3> trunk=new ArrayList<>();
            for(int n=0;n<=8;n++)trunk.add(root.add(Math.sin(n*.4+a)*.18,n/8d*height,Math.cos(n*.3+a)*.15));tube(p,b,trunk,.18*f,.04,winter?ICE:BARK,winter?0xC08DBFC7:0xFF91A078);
            for(int branch=0;branch<5;branch++){double angle=a+branch*2.39996;Vec3 start=trunk.get(3+branch),tip=start.add(polar(angle,1.45*f,(.5+branch*.1)*growth));List<Vec3> path=new ArrayList<>();for(int n=0;n<=6;n++){double s=n/6d;path.add(start.lerp(tip,s).add(0,Math.sin(s*Math.PI)*.22,0));}tube(p,b,path,.065*f,.015,winter?ICE:BARK,winter?0xB5B6E5DD:0xFF9BB284);
                if(winter){
                    crystal(p,b,tip,tip.add(polar(angle,.35,.7*f)),.27*f,ICE_COLOR);
                    for(int k=0;k<path.size()-1;k++)beam(p,b,path.get(k),path.get(k+1),.018*f,tint(SILVER,.5*f));
                    if(branch%2==0)seal(p,b,FROST,tip.add(0,.2,0),.3*f,t*.1,Math.PI/3,tint(SILVER,.5*f));
                }
                else for(int leaf=0;leaf<3;leaf++){Vec3 c=tip.add(polar(angle+leaf*2.1,.22*leaf*f,.07*Math.sin(age*.04+leaf)));billboard(p,b,LEAF_SPRITE,c,.3*f,.43*f,angle+Math.sin(t+tree)*.1,opacity(0xFF95D9A0,.9*f),false);}
            }
            for(int n=0;n<3;n++){List<Vec3> rootPath=new ArrayList<>();for(int k=0;k<=8;k++){double s=k/8d;rootPath.add(root.add(polar(a+n*2.1+s*.4,s*1.7*f,.12*Math.sin(s*Math.PI))));}tube(p,b,rootPath,.07*f,.01,winter?ICE:BARK,winter?0xB5AFE8E1:0xFF79966C);}
        }
        motes(p,b,r*.85,age,30,tint(winter?SILVER:GREEN,.8*f));
        double phase=(age%20)/20;ring(p,b,new Vec3(0,.15,0),r*(.25+.75*phase),.08*f,0,0,tint(color,(1-phase)*.5*f));
    }
    private static void gate(PoseStack p,MultiBufferSource b,double r,float age,int life,boolean exit){
        double f=fade(age,life),t=age*.05;int color=exit?GOLD:PURPLE;double height=2.8*f;
        p.pushPose();p.translate(0,height*.5,0);p.scale((float)(r*f),(float)(height*.5),1);
        ring(p,b,Vec3.ZERO,1,.015,0,1,tint(color,.9*f));p.popPose();
        // A recessed, translucent aperture with independent spirals, rather than an opaque screen.
        for(int layer=0;layer<3;layer++){p.pushPose();p.translate(0,1.4*f,-.025-layer*.045);p.scale(1,(float)(1.4/(r*.92)),1);seal(p,b,GATE,Vec3.ZERO,r*.92*f,t*(layer%2==0?.2:-.13),Math.PI/2,tint(color,(.35-layer*.08)*f));p.popPose();}
        for(int n=0;n<8;n++){double y=(n*.35+t*.3)%2.8;glow(p,b,new Vec3((n%2==0?-1:1)*r*f,y,.05),.13*f,tint(color,.8*f));}
        seal(p,b,CIRCUIT,new Vec3(0,.04,0),r*.8*f,t*.12,0,tint(color,.45*f));
    }
}
