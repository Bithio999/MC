package dev.wildhunt.client;

import com.mojang.blaze3d.vertex.*;
import dev.wildhunt.ArtVisualEntity;
import net.minecraft.world.phys.Vec3;

/** Each kit has its own silhouette and animation: silk, inscriptions, roots, mirrors,
 * plasma, crystal trees, doors and an orbiting three-volume grimoire. */
final class MythicGeometry {
    private MythicGeometry(){}
    static void render(PoseStack.Pose pose,VertexConsumer mesh,int kind,Vec3 data,float age,int life){
        double fade=Math.clamp(Math.min(age/4d,(life-age)/6d),0,1),r=Math.abs(data.x)*fade;
        switch(kind){
            case ArtVisualEntity.THREAD_LINE->{
                Vec3 side=data.normalize().cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
                for(int strand=0;strand<3;strand++)for(int n=0;n<30;n++){
                    double a=n/30d,b=(n+1)/30d;Vec3 p=data.scale(a).add(side.scale(Math.sin(a*20-age*.2+strand*2)*.1)).add(0,-Math.sin(a*Math.PI)*.35,0);
                    Vec3 q=data.scale(b).add(side.scale(Math.sin(b*20-age*.2+strand*2)*.1)).add(0,-Math.sin(b*Math.PI)*.35,0);line(pose,mesh,p,q,.025*fade,strand==1?0xFFFFD9F5:0xFFDA4DAB);
                }diamond(pose,mesh,data,.28*fade,0xFFFFD4ED);
            }
            case ArtVisualEntity.RUNE_MINE,ArtVisualEntity.RUNE_BURST->{
                boolean burst=kind==ArtVisualEntity.RUNE_BURST;double size=burst?r*(.3+age/life):r;
                ring(pose,mesh,new Vec3(0,.12,0),size,.07,age*.01,0,0xFFFFB95B);
                for(int n=0;n<6;n++){
                    double a=n*Math.PI/3+age*.015;Vec3 p=new Vec3(Math.cos(a)*size,.14,Math.sin(a)*size),q=new Vec3(Math.cos(a+Math.PI*2/3)*size,.14,Math.sin(a+Math.PI*2/3)*size);line(pose,mesh,p,q,.035,0xFFF6D18A);
                    Vec3 tip=p.scale(.8).add(0,burst?age*.15:.3+Math.sin(age*.1)*.15,0);diamond(pose,mesh,tip,.13*fade,0xFFFFD78F);
                    if(burst)line(pose,mesh,p,p.add(0,Math.sin(age/life*Math.PI)*4,0),.12,0xFFED9C44);
                }
            }
            case ArtVisualEntity.LIVING_GROVE,ArtVisualEntity.WINTER_ORCHARD->{
                boolean winter=kind==ArtVisualEntity.WINTER_ORCHARD;int green=winter?0xFFA7EED8:0xFF70CE9C;
                ring(pose,mesh,new Vec3(0,.1,0),r,.09,0,0,green);
                for(int tree=0;tree<5;tree++){
                    double a=tree*Math.PI*2/5;Vec3 base=new Vec3(Math.cos(a)*r*.75,0,Math.sin(a)*r*.75);double height=(winter?3:2)*fade;
                    line(pose,mesh,base,base.add(0,height,0),.13,winter?0xFF65A5A5:0xFF806749);
                    for(int branch=0;branch<3;branch++){
                        double b=a+branch*Math.PI*2/3+Math.sin(age*.04)*.1;Vec3 tip=base.add(Math.cos(b)*1.1,height*.7+branch*.25,Math.sin(b)*1.1);
                        line(pose,mesh,base.add(0,height*.5,0),tip,.065,green);diamond(pose,mesh,tip,(winter?.4:.3)*fade,winter?0xFFD8FCFF:0xFFA4F3A0);
                    }
                }
                for(int n=0;n<18;n++){double a=n*Math.PI/9+age*.008;Vec3 p=new Vec3(Math.cos(a)*r*.6,.2+(age*.025+n*.2)%2.5,Math.sin(a)*r*.6);diamond(pose,mesh,p,.07*fade,green);}
            }
            case ArtVisualEntity.BRIAR->{
                for(int n=0;n<6;n++){
                    double a=n*Math.PI/3+age*.02;Vec3 base=new Vec3(Math.cos(a)*r,0,Math.sin(a)*r),tip=base.scale(.35).add(0,data.y*fade,0);
                    line(pose,mesh,base,tip,.08,0xFF68B279);line(pose,mesh,base.add(0,.5,0),base.scale(1.35).add(0,1,0),.06,0xFFD2E9A0);
                }
            }
            case ArtVisualEntity.MIRROR_WARD,ArtVisualEntity.MIRROR_SHARD,ArtVisualEntity.CRYSTAL_BULWARK->{
                int count=kind==ArtVisualEntity.MIRROR_SHARD?3:kind==ArtVisualEntity.CRYSTAL_BULWARK?6:8;
                for(int n=0;n<count;n++){
                    double a=n*Math.PI*2/count+age*(kind==ArtVisualEntity.MIRROR_WARD?.04:.015);Vec3 center=new Vec3(Math.cos(a)*r,1+Math.sin(age*.08+n)*.2,Math.sin(a)*r);
                    shard(pose,mesh,center,.35*fade,kind==ArtVisualEntity.CRYSTAL_BULWARK?0xBBAAF5D0:0xBBC1E9FF,a);
                }
            }
            case ArtVisualEntity.PRISM_RAY,ArtVisualEntity.PLASMA_LANCE,ArtVisualEntity.SPELL_RAY->{
                Vec3 axis=data.normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),up=axis.cross(side).normalize();
                boolean plasma=kind==ArtVisualEntity.PLASMA_LANCE;
                for(int n=0;n<3;n++){
                    Vec3 offset=side.scale((n-1)*.08*fade);line(pose,mesh,offset,data.add(offset),n==1?.07*fade:.035*fade,n==1?0xFFFFFFFF:plasma?0xFFFFB04F:n==0?0xFF86D8FF:0xFFE9AFFF);
                }
                for(double along=0;along<data.length();along+=1.5){
                    double a=along*.8-age*.2;Vec3 p=axis.scale(along).add(side.scale(Math.cos(a)*.3*fade)).add(up.scale(Math.sin(a)*.3*fade));diamond(pose,mesh,p,.1*fade,plasma?0xFFFFCB58:0xFFA9E5FF);
                }
            }
            case ArtVisualEntity.THUNDERHEAD->{
                for(int n=0;n<10;n++){
                    double a=n*Math.PI/5-age*.02;Vec3 p=new Vec3(Math.cos(a)*r*.65,6+Math.sin(n+age*.05)*.5,Math.sin(a)*r*.65);
                    shard(pose,mesh,p,1.4*fade,n%2==0?0xDD282437:0xCC63414E,a);
                    if(((int)age+n*7)%35<6){Vec3 tip=new Vec3(p.x*.85,.2,p.z*.85),mid=p.add(1,-3,0);line(pose,mesh,p,mid,.08,0xFFFFDD80);line(pose,mesh,mid,tip,.1,0xFFFFB050);}
                }ring(pose,mesh,new Vec3(0,.1,0),r,.15,age*.02,0,0xFFFF9F4E);
            }
            case ArtVisualEntity.CATACLYSM_FRONT->{
                ring(pose,mesh,new Vec3(0,.1,0),r,.12,0,0,0xFFFFC35A);
                double erupt=Math.max(0,age-(life-16))/12d;
                for(int n=0;n<8;n++){double a=n*Math.PI/4;Vec3 base=new Vec3(Math.cos(a)*r*.7,0,Math.sin(a)*r*.7);line(pose,mesh,base,base.scale(.6).add(0,erupt*7,0),.12*fade,0xFFFF8247);}
            }
            case ArtVisualEntity.CRYSTAL_BLOOM->{
                double bloom=Math.clamp((age-20)/12,0,1);ring(pose,mesh,new Vec3(0,.1,0),r,.07,age*.015,0,0xFF9EE4CE);
                for(int n=0;n<8;n++){double a=n*Math.PI/4;Vec3 base=new Vec3(Math.cos(a)*r*.25,0,Math.sin(a)*r*.25),tip=new Vec3(Math.cos(a)*r*bloom,1.4+bloom*3,Math.sin(a)*r*bloom);line(pose,mesh,base,tip,(.08+bloom*.2)*fade,0xFFC4F6FC);}
            }
            case ArtVisualEntity.TWIN_GATE->{
                double y=1.4*fade;int c=data.y==0?0xFFC7B1F6:0xFFF1D6AA;
                for(int n=0;n<3;n++){double x=(r+n*.09);line(pose,mesh,new Vec3(-x,0,0),new Vec3(-x,2.8*fade,0),.07,c);line(pose,mesh,new Vec3(x,0,0),new Vec3(x,2.8*fade,0),.07,c);line(pose,mesh,new Vec3(-x,2.8*fade,0),new Vec3(x,2.8*fade,0),.07,c);}
                for(int n=0;n<7;n++){double z=Math.sin(age*.07+n)*.12;line(pose,mesh,new Vec3(-r*.85,n*.4*fade,z),new Vec3(r*.85,n*.4*fade,z),.02,0x778C78D0);}
                diamond(pose,mesh,new Vec3(0,y,0),.23*fade,c);
            }
            case ArtVisualEntity.PARALLAX->{
                double distance=r*Math.max(.05,1-age/18d);
                for(int side:new int[]{-1,1}){
                    Vec3 a=new Vec3(side*distance,0,-r),b=new Vec3(side*distance,2.5,r);line(pose,mesh,a,b,.15*fade,side<0?0xFFE8C8FF:0xFFF3DEB3);line(pose,mesh,a.add(0,2.5,0),b.add(0,-2.5,0),.1*fade,0xFFAE92E9);
                }
            }
            case ArtVisualEntity.CROSSROADS->{
                for(int n=0;n<4;n++){double a=n*Math.PI/2;Vec3 p=new Vec3(Math.cos(a)*r,.2,Math.sin(a)*r);line(pose,mesh,p,p.scale(-1),.09,0xFFBEA0F0);shard(pose,mesh,p.add(0,1,0),.8*fade,0xDDEAD3FF,a);}
                ring(pose,mesh,new Vec3(0,.1,0),r,.09,0,0,0xFFEDD5AC);
            }
            case ArtVisualEntity.LEXICON->{
                int chapter=Math.min(2,(int)(age/22));int c=chapter==0?0xFFFFA15A:chapter==1?0xFFA3ECFF:0xFFFFE28E;
                for(int n=0;n<3;n++){double a=n*Math.PI*2/3-age*.01;Vec3 p=new Vec3(Math.cos(a)*r*.55,2+Math.sin(age*.05+n)*.3,Math.sin(a)*r*.55);book(pose,mesh,p,.8*fade,a+Math.PI/2,c);}
                ring(pose,mesh,new Vec3(0,.1,0),r,.12,age*.01,0,c);star(pose,mesh,r*.8,age*.008,c);
            }
            case ArtVisualEntity.SPELLBREAK->{
                double pulse=Math.clamp(age/18d,0,1)*r;for(int n=0;n<3;n++)ring(pose,mesh,new Vec3(0,.1+n*.6,0),pulse,.08,0,n==2?1:0,0xFF8AE7F5);
                for(int n=0;n<8;n++){double a=n*Math.PI/4;Vec3 p=new Vec3(Math.cos(a)*pulse,1,Math.sin(a)*pulse);shard(pose,mesh,p,.25*fade,0xFFEBD59C,a);}
            }
            case ArtVisualEntity.MANA_CRUCIBLE->{
                for(int n=0;n<2;n++){ring(pose,mesh,new Vec3(0,.2+n*.2,0),r*(1-n*.2),.09,age*.03*(n==0?1:-1),0,n==0?0xFFE0BD65:0xFF6CE5F5);}
                for(int n=0;n<18;n++){double a=n*Math.PI/9+age*.1,rr=r*(1-(age*.04+n*.1)%1);diamond(pose,mesh,new Vec3(Math.cos(a)*rr,1+Math.sin(a)*.5,Math.sin(a)*rr),.1*fade,n%2==0?0xFFFFCE85:0xFF8EEAFA);}
            }
            case ArtVisualEntity.ASTRAL_LIBRARY->{
                for(int n=0;n<3;n++){double a=n*Math.PI*2/3+age*.025;Vec3 p=new Vec3(Math.cos(a)*r,Math.sin(age*.04+n)*.35,Math.sin(a)*r);book(pose,mesh,p,.85*fade,a+Math.PI/2,n==0?0xFFFFB979:n==1?0xFFB0E6FF:0xFFFFE8A0);}
                ring(pose,mesh,Vec3.ZERO,r,.035,age*.025,0,0xFF71CDDF);star(pose,mesh,r*.6,-age*.01,0xFFDAC589);
            }
        }
    }
    private static void book(PoseStack.Pose p,VertexConsumer v,Vec3 c,double size,double angle,int color){
        Vec3 right=new Vec3(Math.cos(angle),0,Math.sin(angle)).scale(size),up=new Vec3(0,size*.68,0),normal=right.cross(up).normalize();
        // Upright open volumes remain recognizable from player height. Covers, page blocks,
        // gold rims and luminous writing are separate surfaces, rather than flat overhead quads.
        for(int side:new int[]{-1,1}){
            Vec3 outer=right.scale(side).add(normal.scale(size*.22));
            Vec3 a=c.subtract(up),b=c.add(up),d=c.add(outer).subtract(up),q=c.add(outer).add(up);
            quad(p,v,a,b,q,d,0xFF263B66);
            Vec3 lift=normal.scale(.035),inner=outer.scale(.9);
            Vec3 pa=c.subtract(up.scale(.91)).add(lift),pb=c.add(up.scale(.91)).add(lift),pc=c.add(inner).add(up.scale(.91)).add(lift),pd=c.add(inner).subtract(up.scale(.91)).add(lift);
            quad(p,v,pa,pb,pc,pd,0xFFFFEDC6);
            line(p,v,a,b,.035,color);line(p,v,b,q,.035,color);line(p,v,q,d,.035,color);line(p,v,d,a,.035,color);
            for(int n=0;n<5;n++){
                Vec3 start=c.add(right.scale(side*.15)).add(up.scale((n-2)*.3)).add(normal.scale(size*.08));
                Vec3 end=start.add(right.scale(side*.6)).add(normal.scale(size*.13));line(p,v,start,end,.018,color);
            }
        }
        diamond(p,v,c.add(0,size,0),.16*size,color);
    }
    private static void star(PoseStack.Pose p,VertexConsumer v,double r,double angle,int color){for(int n=0;n<6;n++){double a=n*Math.PI/3+angle,b=a+Math.PI*2/3;line(p,v,new Vec3(Math.cos(a)*r,.12,Math.sin(a)*r),new Vec3(Math.cos(b)*r,.12,Math.sin(b)*r),.03,color);}}
    private static void shard(PoseStack.Pose p,VertexConsumer v,Vec3 c,double r,int color,double angle){Vec3 side=new Vec3(Math.cos(angle),0,Math.sin(angle)).scale(r);quad(p,v,c.add(0,r*1.6,0),c.add(side),c.add(0,-r,0),c.subtract(side),color);line(p,v,c.add(0,r*1.6,0),c.add(side),.025,0xFFE3F5FF);line(p,v,c.add(0,r*1.6,0),c.subtract(side),.025,0xFFE3F5FF);}
    private static void diamond(PoseStack.Pose p,VertexConsumer v,Vec3 c,double r,int color){quad(p,v,c.add(0,r,0),c.add(r,0,0),c.add(0,-r,0),c.add(-r,0,0),color);quad(p,v,c.add(0,r,0),c.add(0,0,r),c.add(0,-r,0),c.add(0,0,-r),color);}
    private static void ring(PoseStack.Pose p,VertexConsumer v,Vec3 center,double radius,double width,double rotation,int plane,int color){for(int n=0;n<64;n++){double a=n*Math.PI/32+rotation,b=a+Math.PI/32;Vec3 a0=onPlane(a,radius-width,plane).add(center),a1=onPlane(a,radius+width,plane).add(center),b0=onPlane(b,radius-width,plane).add(center),b1=onPlane(b,radius+width,plane).add(center);quad(p,v,a0,a1,b1,b0,color);}}
    private static Vec3 onPlane(double a,double r,int plane){return plane==0?new Vec3(Math.cos(a)*r,0,Math.sin(a)*r):new Vec3(Math.cos(a)*r,Math.sin(a)*r,0);}
    private static void line(PoseStack.Pose p,VertexConsumer v,Vec3 a,Vec3 b,double width,int color){Vec3 axis=b.subtract(a).normalize(),side=axis.cross(Math.abs(axis.y)>.98?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(width),up=axis.cross(side).normalize().scale(width);quad(p,v,a.add(side),a.subtract(side),b.subtract(side),b.add(side),color);quad(p,v,a.add(up),a.subtract(up),b.subtract(up),b.add(up),color);}
    private static void quad(PoseStack.Pose p,VertexConsumer v,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color){for(var point:new Vec3[]{a,b,c,d,d,c,b,a})v.addVertex(p,(float)point.x,(float)point.y,(float)point.z).setColor(color);}
}
