package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.wildhunt.ArtVisualEntity;
import dev.wildhunt.WildHunt;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Full-bright textured spellcraft. Frame-time animation, bounded meshes, no particle entities. */
final class ThothGeometry {
    private static final ResourceLocation SEAL=WildHunt.id("textures/effect/thoth_seal.png"),
        PAGE=WildHunt.id("textures/effect/thoth_grimoire.png"),GLOW=WildHunt.id("textures/effect/thoth_glow.png");
    private static final int GOLD=0xFFE8C376,CYAN=0xFF83E9FF,FIRE=0xFFFF925B,ICE=0xFFAFDAFF;
    private ThothGeometry(){}
    static boolean handles(int kind){return kind>=ArtVisualEntity.LEXICON && kind<=ArtVisualEntity.SPELL_RAY;}
    static void render(PoseStack poses,MultiBufferSource buffers,int kind,Vec3 data,float age,int life,int style){
        double fade=smooth(Math.min(age/8d,(life-age)/10d)),r=Math.abs(data.x),t=age*.05;
        if(kind==ArtVisualEntity.SPELL_RAY){
            int color=style==0?FIRE:style==1?ICE:GOLD;
            beam(poses,buffers,Vec3.ZERO,data,.08*fade,0xFFFFFFFF);
            beam(poses,buffers,Vec3.ZERO,data,.35*fade,color);
            flare(poses,buffers,data,1.2*fade,color);return;
        }
        if(kind==ArtVisualEntity.ASTRAL_LIBRARY){
            // Counter-rotating inscription discs, a tilted celestial meridian and three legible volumes.
            seal(poses,buffers,new Vec3(0,-1.9,0),r*1.28*fade,t*.18,0,alpha(GOLD,.7*fade));
            seal(poses,buffers,Vec3.ZERO,r*.65*fade,-t*.3,0,alpha(CYAN,.65*fade));
            seal(poses,buffers,new Vec3(0,.6,0),r*1.05*fade,t*.12,.65,alpha(CYAN,.32*fade));
            for(int n=0;n<3;n++){
                double a=n*Math.PI*2/3+age*.025;int c=n==0?FIRE:n==1?ICE:GOLD;
                Vec3 p=new Vec3(Math.cos(a)*r,Math.sin(age*.04+n)*.35,Math.sin(a)*r);
                book(poses,buffers,p,.95*fade,a+Math.PI/2,age+n*19,c);
                seal(poses,buffers,p.add(0,.02,0),1.35*fade,-t*.4+n,.95,alpha(c,.55*fade));
                orbit(poses,buffers,r,age,n,fade,c);
            }
            motes(poses,buffers,r,age,fade,36,CYAN);
            flare(poses,buffers,new Vec3(0,.1,0),.9*fade,GOLD);
        } else if(kind==ArtVisualEntity.LEXICON){
            double chapter=Math.min(2,Math.max(0,(age-12)/20));int c=chapter<1?blend(FIRE,ICE,chapter):blend(ICE,GOLD,chapter-1);
            seal(poses,buffers,new Vec3(0,.16,0),r*fade,t*.15,0,alpha(GOLD,.9*fade));
            seal(poses,buffers,new Vec3(0,.22,0),r*.74*fade,-t*.3,0,alpha(c,.8*fade));
            seal(poses,buffers,new Vec3(0,7*fade,0),r*.48*fade,-t*.2,0,alpha(c,.75*fade));
            for(int n=0;n<3;n++){
                double a=n*Math.PI*2/3-t*.2;Vec3 p=new Vec3(Math.cos(a)*r*.7,3+Math.sin(t*2+n)*.3,Math.sin(a)*r*.7);
                book(poses,buffers,p,1.1*fade,a+Math.PI/2,age+n*15,c);
                beam(poses,buffers,p,new Vec3(0,7*fade,0),.06*fade,c);
            }
            // Three separate impacts match the server's 12/32/52-tick chapter timings.
            for(int n=0;n<3;n++){
                double elapsed=age-(12+n*20);if(elapsed<0 || elapsed>18)continue;
                double pulse=Math.sin(elapsed/18*Math.PI),radius=r*(.25+.75*elapsed/18);
                int color=n==0?FIRE:n==1?ICE:GOLD;
                seal(poses,buffers,new Vec3(0,.35+elapsed*.03,0),radius,t,0,alpha(color,pulse));
                beam(poses,buffers,new Vec3(0,12,0),Vec3.ZERO,(n==2?.5:.3)*pulse,color);
                flare(poses,buffers,new Vec3(0,2,0),5*pulse,color);
                for(int k=0;k<14;k++){
                    double a=k*Math.PI/7,rr=r*.8;
                    Vec3 p=new Vec3(Math.cos(a)*rr,0,Math.sin(a)*rr);
                    Vec3 end=p.scale(n==0?1.2:.5).add(0,(n==1?6:3)*pulse,0);
                    beam(poses,buffers,p,end,(n==1?.12:.06)*pulse,color);
                }
            }
            motes(poses,buffers,r,age,fade,42,c);
        } else if(kind==ArtVisualEntity.SPELLBREAK){
            double travel=smooth(age/25d),rr=r*travel;
            for(int n=0;n<3;n++)seal(poses,buffers,new Vec3(0,.25+n*1.1,0),rr*(1-n*.12),t*(n%2==0?1:-1),n==2?.8:0,alpha(n==1?GOLD:CYAN,fade*.7));
            for(int n=0;n<16;n++){
                double a=n*Math.PI/8+t*.15;Vec3 p=new Vec3(Math.cos(a)*rr,1+Math.sin(n+age*.1)*.8,Math.sin(a)*rr);
                seal(poses,buffers,p,.6*fade,a,.9,alpha(GOLD,fade));
                beam(poses,buffers,p.scale(.8),p,.08*fade,CYAN);
            }
            flare(poses,buffers,new Vec3(0,1,0),3*fade*(1-travel),CYAN);
        } else if(kind==ArtVisualEntity.MANA_CRUCIBLE){
            seal(poses,buffers,new Vec3(0,.15,0),r*fade,t*.3,0,alpha(GOLD,fade));
            seal(poses,buffers,new Vec3(0,3.5,0),r*.7*fade,-t*.5,0,alpha(CYAN,fade));
            for(int arm=0;arm<4;arm++)for(int n=0;n<40;n++){
                double a=n*.13+arm*Math.PI/2+t*2,b=a+.13,y=n*.08,rr=r*(1-n/48d)*fade;
                Vec3 p=new Vec3(Math.cos(a)*rr,y,Math.sin(a)*rr),q=new Vec3(Math.cos(b)*rr,y+.08,Math.sin(b)*rr);
                beam(poses,buffers,p,q,.055*fade,arm%2==0?GOLD:CYAN);
            }
            flare(poses,buffers,new Vec3(0,3.5,0),2*fade,CYAN);motes(poses,buffers,r,age,fade,24,GOLD);
        }
    }
    private static void orbit(PoseStack p,MultiBufferSource b,double r,float age,int chapter,double fade,int color){
        // Continuous luminous ribbons curve behind each book rather than appearing as dotted particles.
        double start=chapter*Math.PI*2/3+age*.025;
        for(int n=0;n<28;n++){
            double a=start-n*.024,q=a-.024;
            Vec3 x=new Vec3(Math.cos(a)*r,Math.sin(age*.04+chapter)*.35,Math.sin(a)*r),y=new Vec3(Math.cos(q)*r,Math.sin(age*.04+chapter)*.35,Math.sin(q)*r);
            beam(p,b,x,y,.035*fade,alpha(color,(1-n/28d)*.7));
        }
    }
    private static void motes(PoseStack p,MultiBufferSource b,double radius,float age,double fade,int count,int color){
        for(int n=0;n<count;n++){
            double a=n*2.399963+age*.006,r=radius*(.25+.7*((n*17)%count)/(double)count),phase=age*.016+n*.618;
            double y=(phase-Math.floor(phase))*4,envelope=Math.sin((phase-Math.floor(phase))*Math.PI);
            flare(p,b,new Vec3(Math.cos(a)*r,y,Math.sin(a)*r),(.09+(n%4)*.025)*fade*envelope,color);
        }
    }
    private static void book(PoseStack p,MultiBufferSource buffers,Vec3 center,double size,double angle,float age,int color){
        p.pushPose();p.translate(center.x,center.y,center.z);p.mulPose(Axis.YP.rotation((float)-angle));
        p.mulPose(Axis.ZP.rotation((float)(Math.sin(age*.035)*.07)));
        for(int side:new int[]{-1,1}){
            // Hinged halves, separate page blocks, a moving translucent leaf and a gold spine.
            double opening=.22+Math.sin(age*.045)*.045;
            Vec3 a=new Vec3(0,-size*.7,0),b=new Vec3(0,size*.7,0),c=new Vec3(side*size,size*.7,size*opening),d=new Vec3(side*size,-size*.7,size*opening);
            quad(p.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(PAGE)),a,b,c,d,0xFFD6CEB1);
            beam(p,buffers,a,b,.055,color);beam(p,buffers,b,c,.045,color);beam(p,buffers,c,d,.045,color);beam(p,buffers,d,a,.045,color);
            double page=Math.sin(age*.065+side)*.2;
            quad(p.last(),buffers.getBuffer(RenderType.entityTranslucentEmissive(PAGE)),a.add(0,0,.035),b.add(0,0,.035),c.add(-side*.08,0,page+.04),d.add(-side*.08,0,page+.04),0xAFFFFFFF);
        }
        flare(p,buffers,new Vec3(0,size*.95,0),.55,color);p.popPose();
    }
    private static void seal(PoseStack p,MultiBufferSource buffers,Vec3 center,double radius,double angle,double tilt,int color){
        p.pushPose();p.translate(center.x,center.y,center.z);p.mulPose(Axis.YP.rotation((float)angle));p.mulPose(Axis.XP.rotation((float)tilt));
        quad(p.last(),buffers.getBuffer(RenderType.eyes(SEAL)),new Vec3(-radius,0,-radius),new Vec3(-radius,0,radius),new Vec3(radius,0,radius),new Vec3(radius,0,-radius),alpha(color,.55));p.popPose();
    }
    private static void beam(PoseStack p,MultiBufferSource b,Vec3 a,Vec3 end,double width,int color){
        if(width<=0)return;Vec3 axis=end.subtract(a).normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(width*3),up=axis.cross(side).normalize().scale(width*3);
        var v=b.getBuffer(RenderType.eyes(GLOW));quad(p.last(),v,a.add(side),end.add(side),end.subtract(side),a.subtract(side),color);quad(p.last(),v,a.add(up),end.add(up),end.subtract(up),a.subtract(up),color);
    }
    private static void flare(PoseStack p,MultiBufferSource b,Vec3 c,double size,int color){
        p.pushPose();p.translate(c.x,c.y,c.z);p.mulPose(net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        quad(p.last(),b.getBuffer(RenderType.eyes(GLOW)),new Vec3(-size,-size,0),new Vec3(-size,size,0),new Vec3(size,size,0),new Vec3(size,-size,0),color);p.popPose();
    }
    private static void quad(PoseStack.Pose p,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color){
        Vec3[] points={a,b,c,d,d,c,b,a};float[][] uv={{0,1},{0,0},{1,0},{1,1},{1,1},{1,0},{0,0},{0,1}};
        for(int n=0;n<8;n++)v.addVertex(p,(float)points[n].x,(float)points[n].y,(float)points[n].z).setColor(color).setUv(uv[n][0],uv[n][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(p,0,1,0);
    }
    private static double smooth(double x){x=Math.clamp(x,0,1);return x*x*(3-2*x);}
    private static int alpha(int c,double a){a=Math.clamp(a,0,1);int out=(int)(a*255)<<24;for(int shift:new int[]{16,8,0})out|=(int)(((c>>shift)&255)*a)<<shift;return out;}
    private static int blend(int a,int b,double t){int out=0xFF000000;for(int shift:new int[]{16,8,0})out|=(int)(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t)<<shift;return out;}
}
