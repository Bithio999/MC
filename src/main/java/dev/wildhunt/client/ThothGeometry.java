package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.wildhunt.ArtVisualEntity;
import dev.wildhunt.ThothElement;
import dev.wildhunt.WildHunt;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Full-bright textured spellcraft. Frame-time animation, bounded meshes, no particle entities. */
final class ThothGeometry {
    private static final ResourceLocation SEAL=WildHunt.id("textures/effect/thoth_seal.png"),
        PAGE=WildHunt.id("textures/effect/thoth_grimoire.png"),GLOW=WildHunt.id("textures/effect/thoth_glow.png");
    private static final int GOLD=0xFFE8C376,CYAN=0xFF83E9FF,FIRE=0xFFFF925B;
    private ThothGeometry(){}
    static boolean handles(int kind){return kind>=ArtVisualEntity.LEXICON && kind<=ArtVisualEntity.SPELL_RAY;}
    static void render(PoseStack poses,MultiBufferSource buffers,int kind,Vec3 data,float age,int life,int style){
        double fade=smooth(Math.min(age/8d,(life-age)/10d)),r=Math.abs(data.x),t=age*.05;
        if(kind==ArtVisualEntity.SPELL_RAY){
            // Each flash finishes before the next volley, keeping the faster spectrum legible.
            double visible=Math.min(life,ThothElement.LIBRARY_INTERVAL);if(age>=visible)return;
            fade=smooth(Math.min(age/1.1,(visible-age)/1.6));
            ThothElement element=ThothElement.at(style);int color=element.color;
            beam(poses,buffers,Vec3.ZERO,data,.045*fade,alpha(0xFFFFFFFF,.6*fade));
            beam(poses,buffers,Vec3.ZERO,data,.3*fade,alpha(color,.8*fade));
            flare(poses,buffers,data,.75*fade,color);
            signature(poses,buffers,element,data,.75,age/Math.max(1,life),fade);return;
        }
        if(kind==ArtVisualEntity.ASTRAL_LIBRARY){
            // Counter-rotating inscription discs, a tilted celestial meridian and three legible volumes.
            seal(poses,buffers,new Vec3(0,-1.9,0),r*1.28*fade,t*.18,0,alpha(GOLD,.7*fade));
            seal(poses,buffers,Vec3.ZERO,r*.65*fade,-t*.3,0,alpha(CYAN,.65*fade));
            seal(poses,buffers,new Vec3(0,.6,0),r*1.05*fade,t*.12,.65,alpha(CYAN,.32*fade));
            for(int n=0;n<3;n++){
                int chapter=style,active=ThothElement.libraryBook(chapter);
                double a=n*Math.PI*2/3+age*.025;int c=ThothElement.at(chapter+Math.floorMod(n-active,3)).color;
                Vec3 p=new Vec3(Math.cos(a)*r,Math.sin(age*.04+n)*.35,Math.sin(a)*r);
                book(poses,buffers,p,.95*fade,a+Math.PI/2,age+n*19,c);
                seal(poses,buffers,p.add(0,.02,0),1.35*fade,-t*.4+n,.95,alpha(c,.55*fade));
                orbit(poses,buffers,r,age,n,fade,c);
            }
            motes(poses,buffers,r,age,fade,36,CYAN);
            flare(poses,buffers,new Vec3(0,.1,0),.9*fade,GOLD);
        } else if(kind==ArtVisualEntity.LEXICON){
            int chapter=ThothElement.lexiconChapter(age);double transition=chapter==ThothElement.COUNT-1?0:smooth((age-ThothElement.lexiconTick(chapter))/(ThothElement.lexiconTick(chapter+1)-ThothElement.lexiconTick(chapter)));
            int c=blend(ThothElement.at(chapter).color,ThothElement.at(chapter+1).color,transition);
            seal(poses,buffers,new Vec3(0,.16,0),r*fade,t*.15,0,alpha(GOLD,.9*fade));
            seal(poses,buffers,new Vec3(0,.22,0),r*.74*fade,-t*.3,0,alpha(c,.8*fade));
            seal(poses,buffers,new Vec3(0,7*fade,0),r*.48*fade,-t*.2,0,alpha(c,.75*fade));
            for(int n=0;n<3;n++){
                double a=n*Math.PI*2/3-t*.2;Vec3 p=new Vec3(Math.cos(a)*r*.7,3+Math.sin(t*2+n)*.3,Math.sin(a)*r*.7);
                book(poses,buffers,p,1.1*fade,a+Math.PI/2,age+n*15,c);
                beam(poses,buffers,p,new Vec3(0,7*fade,0),.06*fade,c);
            }
            // Every eruption shares the exact server schedule and native element; the overall lifetime stays 70 ticks.
            for(int n=0;n<ThothElement.COUNT;n++){
                double elapsed=age-ThothElement.lexiconTick(n);if(elapsed<0 || elapsed>12)continue;
                double pulse=Math.sin(elapsed/12*Math.PI),radius=r*(.25+.75*elapsed/12);ThothElement element=ThothElement.at(n);
                seal(poses,buffers,new Vec3(0,.35+elapsed*.03,0),radius,t,0,alpha(element.color,pulse));
                beam(poses,buffers,new Vec3(0,12,0),Vec3.ZERO,.25*pulse,element.color);
                flare(poses,buffers,new Vec3(0,2,0),3*pulse,alpha(element.color,.65));
                signature(poses,buffers,element,Vec3.ZERO,r,elapsed/12,pulse*fade);
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
    /** Element-specific silhouettes supplement the existing books/seals instead of merely changing their tint. */
    private static void signature(PoseStack p,MultiBufferSource b,ThothElement element,Vec3 center,double radius,double progress,double fade){
        if(fade<.001)return;p.pushPose();p.translate(center.x,center.y,center.z);int c=element.color;double r=radius*(.3+.7*progress);
        switch(element){
            case FIRE->{
                VfxMesh.seal(p,b,VfxMesh.RIFT,new Vec3(0,.08,0),r,progress*.2,0,VfxMesh.tint(c,fade));
                for(int n=0;n<6;n++){Vec3 q=VfxMesh.polar(n*Math.PI/3,r*.65,radius*.22);VfxMesh.billboard(p,b,VfxMesh.FLAME,q,radius*.15*fade,radius*.55*fade,n*.3,VfxMesh.tint(c,fade),true);}
            }
            case WATER->{
                // Two rising, scrolling caustic ribbons and foam rims, rather than ice columns.
                for(int arm=0;arm<2;arm++)for(int n=0;n<32;n++){
                    double a=n*Math.PI*2/32+arm*Math.PI+progress*2,q=(n+1)*Math.PI*2/32+arm*Math.PI+progress*2;
                    Vec3 x=VfxMesh.polar(a,r*(.7+arm*.15),radius*(.15+.13*Math.sin(a))),y=VfxMesh.polar(q,r*(.7+arm*.15),radius*(.15+.13*Math.sin(q)));
                    VfxMesh.surface(p,b,VfxMesh.WAVE,x,y,y.add(0,radius*.24*fade,0),x.add(0,radius*.24*fade,0),VfxMesh.opacity(c,.6*fade));
                    VfxMesh.beam(p,b,x.add(0,radius*.24*fade,0),y.add(0,radius*.24*fade,0),.04*fade,VfxMesh.tint(c,fade));
                }
            }
            case LIGHTNING->{
                VfxMesh.seal(p,b,VfxMesh.CIRCUIT,new Vec3(0,.08,0),r,progress*.1,0,VfxMesh.tint(c,.7*fade));
                for(int n=0;n<5;n++)VfxMesh.bolt(p,b,new Vec3(0,radius*.9,0),VfxMesh.polar(n*Math.PI*2/5,r,.1),.08*fade,(float)(progress*12),n,VfxMesh.tint(c,fade));
            }
            case EARTH->{
                VfxMesh.seal(p,b,VfxMesh.CIRCUIT,new Vec3(0,.1,0),r,0,0,VfxMesh.tint(c,.65*fade));
                for(int n=0;n<6;n++){double a=n*Math.PI/3;Vec3 base=VfxMesh.polar(a,r*.7,.1),tip=base.add(0,radius*.8*fade,0);
                    VfxMesh.tube(p,b,java.util.List.of(base,base.lerp(tip,.72),tip),radius*.11*fade,.015,VfxMesh.STONE,0xFFD4B78D);
                    VfxMesh.beam(p,b,base,tip,.035*fade,VfxMesh.tint(c,fade));}
            }
            case WIND->{
                for(int arm=0;arm<3;arm++){Vec3 last=VfxMesh.polar(arm*Math.PI*2/3,r,0);for(int n=1;n<=30;n++){double s=n/30d,a=arm*Math.PI*2/3+s*Math.PI*3-progress*3;Vec3 next=VfxMesh.polar(a,r*(1-.5*s),s*radius*1.1*fade);VfxMesh.beam(p,b,last,next,.055*fade,VfxMesh.tint(c,(1-s*.5)*fade));last=next;}}
            }
            case LIGHT->{
                VfxMesh.seal(p,b,VfxMesh.MIRROR,new Vec3(0,.12,0),r,progress*.15,0,VfxMesh.tint(c,fade));
                for(int n=0;n<12;n++){double a=n*Math.PI/6;Vec3 x=VfxMesh.polar(a,r*.18,radius*.28),y=VfxMesh.polar(a,r*(n%2==0?1:.65),radius*.28);VfxMesh.beam(p,b,x,y,(n%2==0?.12:.045)*fade,VfxMesh.tint(c,fade));}
            }
            case SPACE->{
                for(int plane=0;plane<3;plane++)VfxMesh.ring(p,b,new Vec3(0,radius*.3,0),r*.7,.065*fade,progress,plane,VfxMesh.tint(c,fade));
                VfxMesh.seal(p,b,VfxMesh.RIFT,new Vec3(0,radius*.3,0),r,progress*.3,Math.PI/3,VfxMesh.tint(c,.75*fade));
            }
        }p.popPose();
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
