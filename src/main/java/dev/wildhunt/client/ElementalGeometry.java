package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildhunt.ArtVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static dev.wildhunt.client.VfxMesh.*;

/** Storm conductors, caustic water surfaces, volcanic oil and veined crystalline ice. */
final class ElementalGeometry {
    private static final int GOLD=0xFFFFD16E,TEAL=0xFF6EF0E1,ICE_COLOR=0xDDBAE8FF,FIRE=0xFFFF823A;
    private ElementalGeometry(){}
    static void render(PoseStack p,MultiBufferSource b,int kind,Vec3 data,float age,int life,int style){
        double f=fade(age,life);
        if(kind==ArtVisualEntity.RAILBOLT || kind==ArtVisualEntity.THUNDER_STEP){
            boolean step=kind==ArtVisualEntity.THUNDER_STEP;Vec3 axis=data.normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
            beam(p,b,Vec3.ZERO,data,(step?.055:.09)*f,tint(0xFFFFFFFF,f));
            for(int n=0;n<(step?2:3);n++)bolt(p,b,side.scale((n-1)*.08),data.add(side.scale((n-1)*.08)),.13*f,age,n,GOLD);
            for(double along=step?0:1;along<data.length();along+=step?2.5:5){
                Vec3 c=axis.scale(along);p.pushPose();p.translate(c.x,c.y,c.z);
                var rotation=new org.joml.Quaternionf().rotationTo(0,1,0,(float)axis.x,(float)axis.y,(float)axis.z);p.mulPose(rotation);
                seal(p,b,CIRCUIT,Vec3.ZERO,(step?.9:.5)*f,age*.06,0,tint(GOLD,.45*f));p.popPose();
                if(step)for(int k=0;k<4;k++){double a=k*Math.PI/2+age*.12;Vec3 offset=side.scale(Math.cos(a)*.55).add(up.scale(Math.sin(a)*.55));beam(p,b,c.add(offset),c.add(offset).add(axis.scale(1.5)),.045*f,tint(GOLD,.5*f));}
            }
            glow(p,b,data,.9*f,tint(GOLD,f));return;
        }
        if(kind==ArtVisualEntity.LASH){lash(p,b,data,age,life);return;}
        if(kind==ArtVisualEntity.TIDAL_WAVE){wave(p,b,data,age,life);return;}
        if(kind==ArtVisualEntity.WHIRLPOOL){whirlpool(p,b,data.x,age,life);return;}
        if(kind==ArtVisualEntity.OIL_POOL){oil(p,b,data.x,age,life,style>0);return;}
        if(kind==ArtVisualEntity.CRYSTAL || kind==ArtVisualEntity.FALLING_ICE){
            boolean fall=kind==ArtVisualEntity.FALLING_ICE;Vec3 base=fall?data.scale(Math.pow(Math.clamp(age/life,0,1),2)):Vec3.ZERO;
            Vec3 tip=base.add(0,fall?-2.5:data.y*smooth(Math.min(age/5d,(life-age)/6d)),0);

            crystal(p,b,base,tip,Math.min(.65,tip.distanceTo(base)*.22),0x99BAE8FF);
            seal(p,b,FROST,base.add(0,.05,0),.9*f,age*.012,0,tint(0xFF9AD8FF,.65*f));
            for(int n=0;n<3;n++){double a=n*Math.PI*2/3;Vec3 c=base.add(polar(a,.4,0));beam(p,b,c,c.add(0,Math.abs(tip.y-base.y)*.55,0),.02*f,tint(0xFFD9FCFF,.7*f));}return;
        }
        if(kind==ArtVisualEntity.ELEMENT_IMPACT){
            double r=data.x*(.25+.75*smooth(age/life)),pulse=Math.sin(Math.clamp(age/life,0,1)*Math.PI);
            if(style==0){for(int n=0;n<6;n++)bolt(p,b,Vec3.ZERO,polar(n*Math.PI/3+age*.02,r,.2),.1*f,age,n,tint(GOLD,f));ring(p,b,Vec3.ZERO,r,.16*f,age*.1,1,tint(GOLD,.7*f));}
            else if(style==1){for(int n=0;n<3;n++)ring(p,b,new Vec3(0,.1+n*.2,0),r*(1-n*.18),.13*f,0,0,tint(n==1?0xFFE68FA1:TEAL,f));}
            else {for(int n=0;n<6;n++){double a=n*Math.PI/3;billboard(p,b,FLAME,polar(a,r*.5,pulse*.9),.45*f,1.4*pulse,a,tint(FIRE,f),true);}seal(p,b,RIFT,new Vec3(0,.05,0),r,age*.01,0,tint(FIRE,f));}
            glow(p,b,new Vec3(0,.4,0),.7*pulse,tint(style==0?GOLD:style==1?TEAL:FIRE,f));return;
        }
        if(kind==ArtVisualEntity.HEALING_PULSE){
            double r=data.x*(.65+.35*smooth(age/life));int c=style==0?0xFF9FDEAA:style==1?0xFFBFE69D:GOLD;
            seal(p,b,LEAF,new Vec3(0,.06,0),r,age*.01,0,tint(c,.55*f));
            for(int n=0;n<3+style;n++){double a=n*Math.PI*2/(3+style)+age*.06;Vec3 last=polar(a,r,0);
                for(int k=1;k<12;k++){double t=k/11d;Vec3 next=polar(a+t*1.5,r*(1-.5*t),t*2.1);beam(p,b,last,next,.055*f,tint(c,.75*f));last=next;}}
        }
    }
    private static void wave(PoseStack p,MultiBufferSource b,Vec3 data,float age,int life){
        Vec3 travel=new Vec3(data.x,0,data.z),forward=travel.normalize(),right=forward.yRot((float)(Math.PI/2)),center=travel.scale(Math.clamp(age/life,0,1));double f=fade(age,life);
        int width=segments(32),height=segments(16);
        for(int x=0;x<width;x++)for(int y=0;y<height;y++){
            Vec3[] q=new Vec3[4];int index=0;
            for(int[] corner:new int[][]{{x,y},{x+1,y},{x+1,y+1},{x,y+1}}){double across=(corner[0]/(double)width*2-1)*data.y,t=corner[1]/(double)height;
                double h=(2.5+.17*Math.sin(across*2+age*.17))*Math.sin(t*Math.PI*.67)*f,curl=(Math.cos(t*Math.PI)-1)*.9;
                q[index++]=center.add(right.scale(across)).add(forward.scale(curl)).add(0,h,0);}
            int color=y>height*.8?0xDDF0FFF4:y>height*.5?0xBA95EBE7:0xCA589EBB;
            quadUV(p.last(),b.getBuffer(VfxMaterials.surface(WAVE)),q[0],q[1],q[2],q[3],color,false,(float)x/8+age*.008f,(float)y/8-age*.015f,(float)(x+1)/8+age*.008f,(float)(y+1)/8-age*.015f);
        }
        int count=segments(32);for(int n=0;n<count;n++){double across=(n/(double)(count-1)*2-1)*data.y;Vec3 c=center.add(right.scale(across)).add(forward.scale(-1.4)).add(0,(2.2+.17*Math.sin(across*2+age*.17))*f,0);
            glow(p,b,c,.18*f,tint(0xFFEAFFF2,.75*f));if(n%4==0)beam(p,b,c,c.add(forward.scale(-.6)).add(0,.25,0),.06*f,tint(TEAL,.7*f));}
    }
    private static void whirlpool(PoseStack p,MultiBufferSource b,double radius,float age,int life){
        double f=fade(age,life),r=radius*f;int steps=segments(80);
        // A shallow, raised caustic bowl keeps the vortex three-dimensional without altering terrain.
        for(int n=0;n<steps;n++){double a=n*Math.PI*2/steps,q=(n+1)*Math.PI*2/steps;
            surface(p,b,WAVE,polar(a,r*.16,.08),polar(q,r*.16,.08),polar(q,r,.5+.1*Math.sin(q*4-age*.08)),polar(a,r,.5+.1*Math.sin(a*4-age*.08)),opacity(0xFF92D9D8,.32*f));}

        for(int arm=0;arm<4;arm++)for(int n=0;n<steps;n++){
            Vec3[] q=new Vec3[4];int k=0;
            for(int[] corner:new int[][]{{n,0},{n+1,0},{n+1,1},{n,1}}){double t=corner[0]/(double)steps,a=arm*Math.PI/2+t*Math.PI*3-age*.065,rr=(.35+t*(r-.35))+(corner[1]-.5)*(.22+.5*t);
                q[k++]=polar(a,rr,.12+t*.4+Math.sin(t*Math.PI)*.22+corner[1]*.06);}
            surface(p,b,WAVE,q[0],q[1],q[2],q[3],arm==1?0xC5E3A2B8:0xDC96E8E6);
        }
        ring(p,b,new Vec3(0,.5,0),r,.1,age*.01,0,tint(TEAL,.65*f));
        int count=decorations(30);for(int n=0;n<count;n++){double t=(age*.012+n*.618)%1,a=n*2.39996-age*.08,rr=r*(1-t);glow(p,b,polar(a,rr,.2+Math.sin(t*Math.PI)*.45),.13*Math.sin(t*Math.PI),tint(n%3==0?0xFFF5B2BB:0xFFE3FFF3,.85*f));}
        for(int n=0;n<12;n++){double a=n*Math.PI/6+age*.015;Vec3 c=polar(a,r*.98,.15);crystal(p,b,c,c.add(polar(a,.2,.55*f)),.12,0xA3DF91AF);}
    }
    private static void lash(PoseStack p,MultiBufferSource b,Vec3 end,float age,int life){
        double f=smooth(age/4d)*smooth((life-age)/6d);int count=segments(32);List<Vec3> path=new ArrayList<>();Vec3 side=end.cross(new Vec3(0,1,0)).normalize();
        for(int n=0;n<=count;n++){double t=n/(double)count*f;path.add(end.scale(t).add(0,Math.sin(t*Math.PI)*(.7+end.length()*.04),0).add(side.scale(Math.sin(t*Math.PI*2-age*.15)*Math.sin(t*Math.PI)*.3)));}

        tube(p,b,path,.22*f,.018,STONE,0xFF443344);
        for(int n=2;n<count;n+=3){Vec3 c=path.get(n),q=path.get(n+1);beam(p,b,c.add(side.scale(.1)),q.add(side.scale(.1)),.075*f,tint(FIRE,.85*f));
            }
        for(int strand=0;strand<2;strand++)for(int n=0;n<count;n++){Vec3 a=path.get(n).add(side.scale(strand==0?.22:-.22)),q=path.get(n+1).add(side.scale(strand==0?.22:-.22));beam(p,b,a,q,.018*f,tint(FIRE,.7*f));}
    }
    private static void oil(PoseStack p,MultiBufferSource b,double radius,float age,int life,boolean lit){
        double f=fade(age,life),r=radius*f;int steps=segments(48);
        for(int n=0;n<steps;n++){double a=n*Math.PI*2/steps,q=(n+1)*Math.PI*2/steps,rr=r*(.92+.06*Math.sin(a*5)+.025*Math.sin(a*11));
            surface(p,b,STONE,new Vec3(0,.03,0),polar(a,rr,.035),polar(q,rr,.035),Vec3.ZERO.add(0,.03,0),0xE022182C);}
        seal(p,b,RIFT,new Vec3(0,.05,0),r*.95,age*.001,0,tint(lit?FIRE:0xFF925D97,(lit?.8:.3)*f));
        if(!lit){for(int n=0;n<3;n++)ring(p,b,new Vec3(0,.06,0),r*(.3+n*.25),.035,age*.01,0,tint(0xFFAB789D,.22*f));return;}
        int count=decorations(14);for(int n=0;n<count;n++){double a=n*2.39996,rr=r*.75*Math.sqrt((n+.5)/Math.max(1,count)),flicker=.8+.2*Math.sin(age*.21+n*1.7);Vec3 c=polar(a,rr,.7*flicker);
            double rotation=.12*Math.sin(n+age*.03);
            billboard(p,b,FLAME,c,.53*f,1.3*flicker*f,rotation,opacity(0xFF160D23,.95*f),false);
            billboard(p,b,FLAME,c.add(0,-.12,0),.32*f,.9*flicker*f,rotation,tint(0xFF9857F5,.65*f),true);
            billboard(p,b,FLAME,c.add(0,-.38,0),.14*f,.48*flicker*f,rotation,tint(FIRE,.85*f),true);
            billboard(p,b,CLOUD,c.add(0,1.2+(age*.02+n*.1)%1,0),.6*f,.9*f,age*.008,opacity(0xFF20192B,.55*f),false);}
        motes(p,b,r,age,12,tint(FIRE,.8*f));
    }
}
