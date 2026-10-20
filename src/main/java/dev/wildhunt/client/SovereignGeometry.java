package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildhunt.ArtVisualEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static dev.wildhunt.client.VfxMesh.*;

/** Articulated fate looms, caustic mirrorseas and Lugh's ivory/gold reconstructed armaments. */
final class SovereignGeometry {
    private static final int GOLD=0xFFFFE5A1,IVORY=0xFFFFF4DA,MINT=0xFFA9E9C7,PURPLE=0xFFD6A4FF,SILK_COLOR=0xFFF4DDFF,WATER=0xFF77E7F0,DEEP=0xFF184866;
    private SovereignGeometry() {}
    static boolean handles(int kind){return kind>=ArtVisualEntity.FATE_ANCHOR && kind<=ArtVisualEntity.RECONSTRUCTED_CROWN;}
    static void render(PoseStack p,MultiBufferSource b,int kind,Vec3 data,float age,int life,int style){
        double f=fade(age,life),r=Math.abs(data.x),t=age*.05;
        switch(kind){
            case ArtVisualEntity.FATE_ANCHOR->{
                seal(p,b,SILK,new Vec3(0,.08,0),1.15*f,-t*.13,0,tint(PURPLE,.75*f));
                crystal(p,b,new Vec3(0,.1,0),new Vec3(0,1.35*f,0),.23*f,0xFFBD91D9);
                for(int n=0;n<8;n++){double a=n*Math.PI/4;Vec3 root=polar(a,.22,.5),joint=polar(a,.65,.75+.12*Math.sin(t+n)),foot=polar(a,.95,.08);tube(p,b,List.of(root,joint,foot),.045*f,.012,MIRROR,tint(0xFFD6B181,f));beam(p,b,joint,foot,.015*f,tint(PURPLE,f));}
                ring(p,b,new Vec3(0,.6,0),.42*f,.025,t,1,tint(SILK_COLOR,.8*f));
            }
            case ArtVisualEntity.FATE_THREAD,ArtVisualEntity.LOOM_TRANSIT,ArtVisualEntity.ARTISAN_THREAD->{
                boolean artisan=kind==ArtVisualEntity.ARTISAN_THREAD;int color=artisan?GOLD:PURPLE;Vec3 side=side(data);int pieces=segments(40);
                for(int strand=0;strand<3;strand++){Vec3 last=Vec3.ZERO;for(int n=1;n<=pieces;n++){double a=n/(double)pieces;Vec3 next=data.scale(a).add(side.scale(Math.sin(a*28-t*3+strand*Math.PI*2/3)*.065*Math.sin(a*Math.PI))).add(0,-.17*Math.sin(a*Math.PI),0);beam(p,b,last,next,(strand==0?.024:.012)*f,tint(color,.75*f));last=next;}}
                for(int n=0;n<3;n++){double q=(age*.025+n/3d)%1;Vec3 c=data.scale(q);seal(p,b,artisan?CIRCUIT:SILK,c,.18*f,t,Math.PI/2,tint(artisan?IVORY:SILK_COLOR,.75*f));}
            }
            case ArtVisualEntity.CHECKMATE_EDGE->{
                double closing=smooth((age-24)/12d);Vec3 side=side(data),from=Vec3.ZERO,to=data;
                beam(p,b,from,to,.045*f,tint(PURPLE,.9*f));
                int count=segments(30);for(int n=0;n<count;n++){double a=n/(double)count,q=(n+1)/(double)count;Vec3 x=data.scale(a),y=data.scale(q);
                    beam(p,b,x.add(0,closing*4,0),y.add(0,closing*4,0),.04*f,tint(SILK_COLOR,f));
                    if(n%3==0){Vec3 top=x.add(side.scale(Math.sin(n*.7+t)*closing*.7)).add(0,4*closing,0);beam(p,b,x,top,.025*f,tint(PURPLE,.65*f));}
                    if(age>=40 && n%5==0){double burst=Math.sin(Math.clamp((age-40)/12,0,1)*Math.PI);crystal(p,b,x,x.add(0,3*burst,0),.28*f,0xFFAE83C4);}
                }
            }
            case ArtVisualEntity.MIRRORSEA->{
                waterDisc(p,b,r*f,age,.38*f);
                for(int n=0;n<3;n++)ring(p,b,new Vec3(0,.12+n*.03,0),r*(.45+.25*n),.028*f,-t*.1,0,tint(WATER,(.25+.15*n)*f));
                for(int n=0;n<6;n++){double a=n*Math.PI/3;Vec3 c=polar(a,r*.72,1.5+.15*Math.sin(t+n));p.pushPose();p.translate(c.x,c.y,c.z);p.mulPose(Axis.YP.rotation((float)-a));
                    Vec3 top=new Vec3(0,1.6,0),right=new Vec3(.9,0,.12),bottom=new Vec3(0,-1.4,0),left=new Vec3(-.9,0,.12);
                    surface(p,b,MIRROR,top,right,bottom,left,opacity(DEEP,.32*f));
                    for(Vec3[] edge:new Vec3[][]{{top,right},{right,bottom},{bottom,left},{left,top}})beam(p,b,edge[0],edge[1],.035*f,tint(WATER,.85*f));
                    seal(p,b,WAVE,Vec3.ZERO,.78*f,t*.15,Math.PI/2,tint(WATER,.45*f));p.popPose();
                }
                for(int arm=0;arm<2;arm++)for(int n=0;n<segments(48);n++){double a=n*Math.PI*2/segments(48)+arm*Math.PI+t*.18,q=(n+1)*Math.PI*2/segments(48)+arm*Math.PI+t*.18;
                    Vec3 x=polar(a,r*.93,.3+.2*Math.sin(a*3+t)),y=polar(q,r*.93,.3+.2*Math.sin(q*3+t));surface(p,b,WAVE,x,y,y.add(0,.4,0),x.add(0,.4,0),opacity(WATER,.24*f));}
            }
            case ArtVisualEntity.REFRACTED_HARPOON->{
                Vec3 axis=data.normalize(),side=side(data),up=axis.cross(side).normalize();beam(p,b,Vec3.ZERO,data,.08*f,tint(WATER,.65*f));
                for(int arm=0;arm<2;arm++){Vec3 last=Vec3.ZERO;for(int n=1;n<=segments(48);n++){double a=n/(double)segments(48),spin=a*data.length()*.7-age*.12+arm*Math.PI;Vec3 next=data.scale(a).add(side.scale(Math.cos(spin)*.23)).add(up.scale(Math.sin(spin)*.23));beam(p,b,last,next,.033*f,tint(arm==0?IVORY:WATER,.7*f));last=next;}}
                Vec3 tip=data.scale(smooth(age/9d));spear(p,b,tip,axis,1.15*f,WATER);glow(p,b,tip,.7*f,tint(WATER,.6*f));
                for(double d=3;d<data.length();d+=8)lens(p,b,axis.scale(d),axis,.55*f,age*.02,WATER);
            }
            case ArtVisualEntity.SOVEREIGN_BREAKER->{
                double radius=r*Math.clamp(age/40d,0,1);int count=segments(96);
                for(int n=0;n<count;n++){double a=n*Math.PI*2/count,q=(n+1)*Math.PI*2/count,height=(1.5+.5*Math.sin(a*9-age*.1))*f,nextHeight=(1.5+.5*Math.sin(q*9-age*.1))*f;
                    Vec3 x=polar(a,radius,.1),y=polar(q,radius,.1),u=polar(q,radius-.6,nextHeight),v=polar(a,radius-.6,height);
                    surface(p,b,WAVE,x,y,u,v,opacity(WATER,.6*f));beam(p,b,v,u,.07*f,tint(IVORY,.85*f));
                    if(n%6==0)beam(p,b,x,v,.025*f,tint(WATER,.6*f));}
                ring(p,b,new Vec3(0,.1,0),radius+.4,.065*f,t,0,tint(WATER,.6*f));
            }
            case ArtVisualEntity.WATER_REFRACTION->{
                for(int n=0;n<4;n++){double a=n*Math.PI/2+t;Vec3 x=polar(a,r*f,.2);surface(p,b,MIRROR,x,x.add(0,.6*f,0),x.scale(.4).add(0,1.1*f,0),x.scale(.4),opacity(WATER,.45*f));}
                ring(p,b,Vec3.ZERO,r*smooth(age/life),.07*f,t,1,tint(WATER,f));
            }
            case ArtVisualEntity.PERENNIAL_SEED->{
                // A quiet active rescue marker, with no particle cloud or defensive shell.
                Vec3 c=new Vec3(0,2.2+Math.sin(age*.06)*.08,0);
                for(int n=0;n<4;n++){double a=n*Math.PI/2+t*.3;Vec3 root=c.add(polar(a,.18,.05));crystal(p,b,root,root.add(polar(a,.3,.25)),.09*f,0xFFB8E8BE);}
                glow(p,b,c,.14*f,tint(MINT,.65*f));
            }
            case ArtVisualEntity.REBIRTH_BLOOM->{
                double open=smooth(age/14d);seal(p,b,LEAF,new Vec3(0,.08,0),r*f,t*.1,0,tint(MINT,.7*f));
                for(int n=0;n<8;n++){double a=n*Math.PI/4;Vec3 root=polar(a,.2,0),tip=polar(a,r*.8*open,2.5*open);crystal(p,b,root,tip,.22*f,0xFFB9E9D1);beam(p,b,root,tip,.035*f,tint(MINT,.8*f));}
                motes(p,b,r,age,18,MINT);
            }
            case ArtVisualEntity.SKILL_APPRAISAL->{
                for(int n=0;n<3;n++){double y=.5+n*.8+Math.sin(t+n)*.05;ring(p,b,new Vec3(0,y,0),r*(.7+.2*Math.sin(t+n)),.035*f,t*(n%2==0?1:-1),0,tint(GOLD,.75*f));}
                tablet(p,b,new Vec3(0,3.4,0),1*f,age,style);beam(p,b,new Vec3(-r,.2,0),new Vec3(r,2.8,0),.018*f,tint(MINT,.6*f));
            }
            case ArtVisualEntity.COUNTERMAND->{
                Vec3 c=new Vec3(0,2.6,0);double shut=smooth(age/12d);tablet(p,b,c,.7*f,age,1);
                for(int n=0;n<2;n++){p.pushPose();p.translate(c.x,c.y,c.z);p.mulPose(Axis.ZP.rotation((float)(n*Math.PI/2)));beam(p,b,new Vec3(-1.1*f,0,.15),new Vec3(1.1*f,0,.15),.065*f,tint(GOLD,.85*f));p.popPose();}
                ring(p,b,c,r*.65*f,.045*f,0,1,tint(GOLD,.8*f));crystal(p,b,c.add(0,-.2,.2),c.add(0,.45*shut,.2),.16*f,0xFF413A57);
            }
            case ArtVisualEntity.GRAND_SYNTHESIS->{
                seal(p,b,CIRCUIT,new Vec3(0,.09,0),r*f,0,0,tint(GOLD,.4*f));
                for(int n=0;n<3;n++){double phase=20+n*16,collapse=smooth((age-phase+7)/12d),a=n*Math.PI*2/3-.3;
                    Vec3 c=polar(a,r*.72*(1-.75*collapse),4+Math.sin(t+n)*.2+collapse*2);int form=(style/(n==0?1:n==1?3:9))%3;
                    tablet(p,b,c,1.45*f*(1-.35*collapse),age+n*10,form);
                    Vec3 base=polar(a,r*.72,.15);beam(p,b,base,c,.025*f,tint(MINT,.55*f));
                    if(age>=phase-5 && age<phase+9){double flash=Math.sin(Math.clamp((age-phase+5)/14,0,1)*Math.PI);beam(p,b,c,new Vec3(0,.2,0),.14*flash*f,tint(GOLD,flash*f));}
                }
                for(int plane=0;plane<3;plane++)ring(p,b,new Vec3(0,2,0),r*.35*f,.025*f,t*.14,plane,tint(MINT,.4*f));
            }
            case ArtVisualEntity.RECONSTRUCTED_BLADE->{
                p.pushPose();p.mulPose(Axis.YP.rotation((float)data.z));double sweep=Math.clamp(age/14d,0,1);
                for(int sweepIndex=0;sweepIndex<(style==1?3:1);sweepIndex++)for(int layer=0;layer<3;layer++){double radius=r*(1-layer*.09),angle=-1.2+sweep*2.4-layer*.08+sweepIndex*Math.PI*2/3;Vec3 last=polar(angle-.65,radius*.3,0);
                    for(int n=1;n<=segments(30);n++){double q=n/(double)segments(30),a=angle-.65+q*.65;Vec3 next=polar(a,radius*(.3+.7*q),Math.sin(q*Math.PI)*.25);beam(p,b,last,next,(.14-.03*layer)*f,tint(layer==0?IVORY:GOLD,(1-layer*.2)*f));last=next;}}
                p.popPose();
            }
            case ArtVisualEntity.RECONSTRUCTED_LANCE->{
                Vec3 axis=data.normalize(),tip=data.scale(smooth(age/10d));beam(p,b,Vec3.ZERO,data,.06*f,tint(MINT,.55*f));spear(p,b,tip,axis,1.5*f,GOLD);
                for(int n=0;n<4;n++){Vec3 c=data.scale(Math.clamp(age/10d-n*.09,0,1));lens(p,b,c,axis,(.55+n*.08)*f,age*.02,GOLD);}
            }
            case ArtVisualEntity.RECONSTRUCTED_CROWN->{
                double rise=smooth(age/10d);seal(p,b,CIRCUIT,new Vec3(0,.08,0),r*f,-t*.12,0,tint(GOLD,.6*f));
                for(int n=0;n<7;n++){Vec3 c=polar(n*Math.PI*2/7,r*.65,0);spear(p,b,c.add(0,5*rise,0),new Vec3(0,-1,0),(.7+rise*.3)*f,IVORY);beam(p,b,c,c.add(0,5*rise,0),.03*f,tint(MINT,.65*f));}
                ring(p,b,new Vec3(0,4*rise,0),r*.65*f,.065*f,t*.1,0,tint(GOLD,.8*f));
            }
        }
    }
    /** Radial UV projection clips the caustic surface to the actual circular basin. */
    private static void waterDisc(PoseStack p,MultiBufferSource b,double radius,float age,double alpha){
        var mesh=b.getBuffer(VfxMaterials.surface(WAVE));int count=segments(96);double spin=age*.002;
        for(int n=0;n<count;n++){
            double a=n*Math.PI*2/count,q=(n+1)*Math.PI*2/count;
            for(Vec3 v:new Vec3[]{new Vec3(0,.06,0),polar(a,radius,.06),polar(q,radius,.06),new Vec3(0,.06,0)}){
                double edge=radius<.001?0:v.horizontalDistance()/radius;
                float u=(float)(.5+(v.x*Math.cos(spin)-v.z*Math.sin(spin))/Math.max(.01,radius*2)),w=(float)(.5+(v.x*Math.sin(spin)+v.z*Math.cos(spin))/Math.max(.01,radius*2));
                mesh.addVertex(p.last(),(float)v.x,(float)v.y,(float)v.z).setColor(opacity(WATER,alpha*(1-edge*.6))).setUv(u,w).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(p.last(),0,1,0);
            }
        }
    }
    private static Vec3 side(Vec3 d){Vec3 a=d.normalize();return a.cross(Math.abs(a.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();}
    private static void lens(PoseStack p,MultiBufferSource b,Vec3 c,Vec3 axis,double r,double spin,int color){
        p.pushPose();p.translate(c.x,c.y,c.z);p.mulPose(new org.joml.Quaternionf().rotationTo(0,1,0,(float)axis.x,(float)axis.y,(float)axis.z));seal(p,b,MIRROR,Vec3.ZERO,r,spin,0,tint(color,.6));ring(p,b,Vec3.ZERO,r,.025,spin,0,tint(color,.8));p.popPose();
    }
    private static void spear(PoseStack p,MultiBufferSource b,Vec3 tip,Vec3 direction,double size,int color){
        if(size<.001)return;Vec3 axis=direction.normalize(),side=side(axis),up=axis.cross(side).normalize(),base=tip.subtract(axis.scale(size*2));
        beam(p,b,base,tip,.055*size,tint(color,.9));crystal(p,b,tip.subtract(axis.scale(size*.7)),tip,size*.16,color);
        for(int n:new int[]{-1,1}){Vec3 wing=tip.subtract(axis.scale(size*.6)).add(side.scale(n*size*.5));beam(p,b,tip.subtract(axis.scale(size)),wing,.04*size,color);beam(p,b,wing,tip.subtract(axis.scale(size*.2)),.03*size,color);}
        beam(p,b,base.add(up.scale(size*.1)),tip.subtract(axis.scale(size*.65)),.02*size,tint(MINT,.8));
    }
    private static void tablet(PoseStack p,MultiBufferSource b,Vec3 center,double size,float age,int form){
        p.pushPose();p.translate(center.x,center.y,center.z);p.mulPose(net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());p.mulPose(Axis.ZP.rotation((float)(Math.sin(age*.02)*.06)));
        Vec3 a=new Vec3(-size,-size*.8,0),q=new Vec3(size,-size*.8,0),c=new Vec3(size,size*.8,0),d=new Vec3(-size,size*.8,0);
        Vec3 back=new Vec3(0,0,-.14*size);
        surface(p,b,WHITE,a,q,c,d,0xFFE9DFBE);
        for(Vec3[] edge:new Vec3[][]{{a,q},{q,c},{c,d},{d,a}}){
            surface(p,b,WHITE,edge[0],edge[1],edge[1].add(back),edge[0].add(back),0xFF9D864E);
            beam(p,b,edge[0],edge[1],.035*size,tint(GOLD,.9));
        }
        // A dark recessed face keeps the inscribed armament readable against bright terrain.
        surface(p,b,WHITE,a.scale(.85).add(0,0,.012),q.scale(.85).add(0,0,.012),c.scale(.85).add(0,0,.012),d.scale(.85).add(0,0,.012),0xFF263D38);
        seal(p,b,CIRCUIT,new Vec3(0,0,.03),size*.62,age*.008,Math.PI/2,tint(MINT,.75));
        for(int row=0;row<3;row++){
            double y=size*(.56-row*.075);beam(p,b,new Vec3(-size*.55,y,.045),new Vec3(size*.55,y,.045),.009*size,tint(GOLD,.7));
        }
        if(form==0){beam(p,b,new Vec3(-size*.35,-size*.4,.07),new Vec3(size*.35,size*.4,.07),.07*size,IVORY);beam(p,b,new Vec3(-size*.3,0,.07),new Vec3(size*.1,-size*.35,.07),.04*size,GOLD);}
        else if(form==1){beam(p,b,new Vec3(0,-size*.5,.07),new Vec3(0,size*.5,.07),.06*size,IVORY);beam(p,b,new Vec3(-size*.25,size*.15,.07),new Vec3(0,size*.5,.07),.04*size,GOLD);beam(p,b,new Vec3(size*.25,size*.15,.07),new Vec3(0,size*.5,.07),.04*size,GOLD);}
        else for(int n=-1;n<=1;n++)beam(p,b,new Vec3(n*size*.28,-size*.25,.07),new Vec3(n*size*.28,size*(n==0?.45:.25),.07),.055*size,IVORY);
        p.popPose();
    }
}
