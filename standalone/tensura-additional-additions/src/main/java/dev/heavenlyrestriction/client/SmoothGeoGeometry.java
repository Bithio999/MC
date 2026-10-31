package dev.heavenlyrestriction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;
import java.util.*;

/** Continuous surfaces attached to editor-authored GeckoLib bones. No extra render pass:
 * the supplied consumer preserves the atlas, glow mask, lighting and damage overlay. */
final class SmoothGeoGeometry {
    private record Vertex(Vec3 point,Vec3 normal,float u,float v) {}
    private record Surface(List<Vertex>[] levels) {}
    private static final Map<String,Surface> CACHE=new HashMap<>();
    private SmoothGeoGeometry(){}

    static boolean render(PoseStack p,GeoBone bone,VertexConsumer out,int light,int overlay,int color){
        String name=bone.getName();if(!name.startsWith("surface_"))return false;
        if(bone.isHidden())return true;
        Surface surface=CACHE.computeIfAbsent(name,SmoothGeoGeometry::bake);
        int level=VfxMesh.segments(96)>=96?0:VfxMesh.segments(96)>=48?1:2;
        p.pushPose();
        // renderCubesOfBone receives a matrix translated away from the pivot.
        // Restore that pivot to place our local mesh in the same editor coordinates.
        // GeoBone already stores the editor's X coordinate reflected into game space.
        software.bernie.geckolib.util.RenderUtil.translateToPivotPoint(p,bone);
        var pose=p.last();
        for(Vertex v:surface.levels[level]){
            out.addVertex(pose,(float)v.point.x,(float)v.point.y,(float)v.point.z)
                .setColor(color).setUv(v.u,v.v).setOverlay(overlay).setLight(light)
                .setNormal(pose,(float)v.normal.x,(float)v.normal.y,(float)v.normal.z);
        }
        p.popPose();return true;
    }

    @SuppressWarnings("unchecked")
    private static Surface bake(String name){
        String[] parts=name.split("_");String shape=parts[1];
        double x=Integer.parseInt(parts[3])/1600d,y=Integer.parseInt(parts[4])/1600d,z=Integer.parseInt(parts[5])/1600d;
        int mat=Integer.parseInt(parts[6]);List<Vertex>[] levels=new List[3];
        for(int lod=0;lod<3;lod++){
            if(lod>0 && (shape.equals("gem") || shape.equals("box"))){levels[lod]=levels[0];continue;}
            if(lod==2 && shape.equals("leaf")){levels[lod]=levels[1];continue;}
            List<Vertex> mesh=new ArrayList<>();
            switch(shape){
                case "ring"->ring(mesh,x,y,z,mat,new int[]{96,64,32}[lod],false);
                case "crescent"->ring(mesh,x,y,z,mat,new int[]{96,64,32}[lod],true);
                case "gem"->gem(mesh,x,y,z,mat);
                case "leaf"->leaf(mesh,x,y,z,mat,lod==0?16:8);
                case "box"->box(mesh,x,y,z,mat);
                default->throw new IllegalArgumentException("Unknown authored surface: "+name);
            }
            levels[lod]=List.copyOf(mesh);
        }
        return new Surface(levels);
    }

    // Beveled rectangular annulus, smooth around its circumference. Eight profile
    // bands retain a readable metal edge instead of rounding it into a hose.
    private static void ring(List<Vertex> mesh,double radius,double width,double thickness,int mat,int count,boolean crescent){
        double w=width*.5,h=thickness*.5,b=Math.min(w,h)*.42;
        double[][] profile={{-w,-h+b},{-w+b,-h},{w-b,-h},{w,-h+b},{w,h-b},{w-b,h},{-w+b,h},{-w,h-b}};
        for(int n=0;n<count;n++){
            double a=crescent?Math.toRadians(-140+n*280d/count):n*Math.TAU/count,q=crescent?Math.toRadians(-140+(n+1)*280d/count):(n+1)*Math.TAU/count;
            double ta=crescent?Math.pow(Math.max(0,Math.sin(n*Math.PI/count)),.65):1,tq=crescent?Math.pow(Math.max(0,Math.sin((n+1)*Math.PI/count)),.65):1;
            for(int k=0;k<8;k++){
                double[] c=profile[k],d=profile[(k+1)%8];
                double nr=d[1]-c[1],ny=c[0]-d[0],len=Math.hypot(nr,ny);nr/=len;ny/=len;
                // Clockwise cross section needs outward normals.
                Vec3 na=new Vec3(Math.cos(a)*nr,ny,Math.sin(a)*nr),nq=new Vec3(Math.cos(q)*nr,ny,Math.sin(q)*nr);
                face(mesh,ringPoint(a,radius+c[0]*ta,c[1]*ta),ringPoint(a,radius+d[0]*ta,d[1]*ta),ringPoint(q,radius+d[0]*tq,d[1]*tq),ringPoint(q,radius+c[0]*tq,c[1]*tq),na,na,nq,nq,mat);
            }
            // Fine luminous rails on both faces, driven by the same animated bone.
            for(int side:new int[]{-1,1}){
                double yy=side*(h+.0008),r=radius+width*.19,t=width*.045;
                Vec3 normal=new Vec3(0,side,0);
                if(side>0)face(mesh,ringPoint(a,radius+(r-t-radius)*ta,yy*ta),ringPoint(q,radius+(r-t-radius)*tq,yy*tq),ringPoint(q,radius+(r+t-radius)*tq,yy*tq),ringPoint(a,radius+(r+t-radius)*ta,yy*ta),normal,normal,normal,normal,3);
                else face(mesh,ringPoint(a,radius+(r+t-radius)*ta,yy*ta),ringPoint(q,radius+(r+t-radius)*tq,yy*tq),ringPoint(q,radius+(r-t-radius)*tq,yy*tq),ringPoint(a,radius+(r-t-radius)*ta,yy*ta),normal,normal,normal,normal,3);
            }
        }
    }
    private static Vec3 ringPoint(double a,double radius,double y){return new Vec3(Math.cos(a)*radius,y,Math.sin(a)*radius);}

    private static void gem(List<Vertex> mesh,double x,double y,double z,int mat){
        double[] heights={-.5,-.25,.18,.5},radii={0,1,.72,0};
        for(int layer=0;layer<3;layer++)for(int n=0;n<8;n++){
            double a=n*Math.TAU/8+Math.PI/8,b=(n+1)*Math.TAU/8+Math.PI/8;
            flat(mesh,gemPoint(a,x,z,radii[layer],y*heights[layer]),gemPoint(a,x,z,radii[layer+1],y*heights[layer+1]),gemPoint(b,x,z,radii[layer+1],y*heights[layer+1]),gemPoint(b,x,z,radii[layer],y*heights[layer]),mat);
        }
    }
    private static Vec3 gemPoint(double a,double x,double z,double r,double y){return new Vec3(Math.cos(a)*x*.5*r,y,Math.sin(a)*z*.5*r);}

    private static void leaf(List<Vertex> mesh,double x,double y,double z,int mat,int count){
        // Closed, curved lanceolate surface. Narrow ends meet at genuine tips;
        // a raised midrib gives petals/foliage volume without rectangular plates.
        for(int n=0;n<count;n++){
            double a=n/(double)count,b=(n+1d)/count;
            for(int side:new int[]{-1,1})for(int half:new int[]{-1,1}){
                Vec3 p=leafPoint(a,half,x,y,z,side),q=leafPoint(b,half,x,y,z,side),r=leafPoint(b,0,x,y,z,side),s=leafPoint(a,0,x,y,z,side);
                if(side*half>0)flat(mesh,p,q,r,s,mat);else flat(mesh,s,r,q,p,mat);
            }
        }
    }
    private static Vec3 leafPoint(double t,int half,double x,double y,double z,int side){
        double f=Math.pow(Math.max(0,Math.sin(t*Math.PI)),.65);
        return new Vec3(half*x*.5*f,(t-.5)*y,z*(Math.sin(t*Math.PI)*.25+side*f*(half==0?.22:0)));
    }

    private static void box(List<Vertex> mesh,double x,double y,double z,int mat){
        double b=Math.min(x,Math.min(y,z))*.16;
        double[] yy={-y/2,-y/2+b,y/2-b,y/2};
        for(int k=0;k<3;k++)for(int n=0;n<8;n++)flat(mesh,corner(n,x,z,k==0?b:0,yy[k],b),corner(n,x,z,k==2?b:0,yy[k+1],b),corner(n+1,x,z,k==2?b:0,yy[k+1],b),corner(n+1,x,z,k==0?b:0,yy[k],b),mat);
        for(int side:new int[]{-1,1})for(int n=0;n<8;n++){
            Vec3 center=new Vec3(0,side*y/2,0),a=corner(n,x,z,b,side*y/2,b),q=corner(n+1,x,z,b,side*y/2,b);
            if(side<0)flat(mesh,center,a,q,center,mat);else flat(mesh,center,q,a,center,mat);
        }
    }
    private static Vec3 corner(int n,double x,double z,double inset,double y,double b){
        double w=Math.max(.0001,x/2-inset),d=Math.max(.0001,z/2-inset),c=Math.min(b,Math.min(w,d)*.5);
        return switch(n%8){case 0->new Vec3(-w+c,y,-d);case 1->new Vec3(w-c,y,-d);case 2->new Vec3(w,y,-d+c);case 3->new Vec3(w,y,d-c);case 4->new Vec3(w-c,y,d);case 5->new Vec3(-w+c,y,d);case 6->new Vec3(-w,y,d-c);default->new Vec3(-w,y,-d+c);};
    }
    private static void flat(List<Vertex> mesh,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int mat){
        Vec3 normal=b.subtract(a).cross(c.subtract(a));
        if(normal.lengthSqr()<1e-16)normal=c.subtract(a).cross(d.subtract(a));
        normal=normal.normalize();face(mesh,a,b,c,d,normal,normal,normal,normal,mat);
    }
    private static void face(List<Vertex> mesh,Vec3 a,Vec3 b,Vec3 c,Vec3 d,Vec3 na,Vec3 nb,Vec3 nc,Vec3 nd,int mat){
        float u0=(mat*32+3)/256f,u1=(mat*32+29)/256f,v0=3/128f,v1=29/128f;
        mesh.add(new Vertex(a,na,u0,v0));mesh.add(new Vertex(b,nb,u1,v0));mesh.add(new Vertex(c,nc,u1,v1));mesh.add(new Vertex(d,nd,u0,v1));
    }
}
