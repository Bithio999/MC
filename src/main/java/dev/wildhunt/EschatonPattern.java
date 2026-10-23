package dev.wildhunt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/** Shared analytic geometry: the server hit tests and client warning footprints use the same values. */
public final class EschatonPattern {
    private EschatonPattern(){}
    public static final int[] WARNING={24,32,32,38,28,48,36,200};
    public static final int[] LIFE={78,132,152,78,136,70,70,238};
    public static final double APOCALYPSE_RADIUS=56,REFUGE_RADIUS=7,LOOM_RADIUS=46;
    public static int count(CompoundTag p){return p.getInt("phase")>=3?8:p.getInt("phase")>=2?6:4;}
    public static Vec3 point(CompoundTag p,String key){return ApexArts.point(p,key);}
    public static Vec3 forward(CompoundTag p){return point(p,"forward");}
    public static double facing(CompoundTag p){var f=forward(p);return Math.atan2(f.z,f.x);}
    public static int edgeTick(int wave){return 24+wave*16;}
    public static double edgeInner(int wave){return wave==0?0:wave==1?12:24;}
    public static double edgeOuter(int wave){return wave==0?18:wave==1?32:46;}
    public static double edgeAngle(CompoundTag p,int wave){return facing(p)+(wave==1?Math.PI:0);}
    public static boolean edge(Vec3 delta,CompoundTag p,int wave){
        double r=delta.horizontalDistance();return Math.abs(delta.y)<=9 && r>=edgeInner(wave) && r<=edgeOuter(wave)
            && (r<1 || Math.cos(Math.atan2(delta.z,delta.x)-edgeAngle(p,wave))>=Math.cos(Math.toRadians(65)));
    }
    public static double loomAngle(CompoundTag p,double age,int spoke){return facing(p)+(age-32)*.014+spoke*Math.TAU/count(p);}
    public static Vec3 loomStart(CompoundTag p){return new Vec3(0,Math.clamp(point(p,"aim").y,-6,24),0);}
    public static Vec3 loomEnd(CompoundTag p,double age,int spoke){double a=loomAngle(p,age,spoke);return loomStart(p).add(Math.cos(a)*LOOM_RADIUS,0,Math.sin(a)*LOOM_RADIUS);}
    public static Vec3 meteor(CompoundTag p,int n){return point(p,"meteor_"+n);}
    public static int meteorTick(int n){return 32+n*14;}
    public static Vec3 cutCenter(CompoundTag p,int n){return point(p,"aim").add(forward(p).yRot((float)(Math.PI/2)).scale((n-1)*12));}
    public static Vec3 cutEnd(CompoundTag p,int n,int side){return cutCenter(p,n).add(forward(p).scale(side*36));}
    public static int waveTick(int n){return 28+n*20;}
    public static double waveRadius(double age,int n){return 2+(age-waveTick(n))*1.2;}
    public static double gapAngle(CompoundTag p,int n){return facing(p)+n*Math.TAU/3;}
    public static boolean inGap(Vec3 delta,CompoundTag p,int n){return Math.abs(Math.cos(Math.atan2(delta.z,delta.x)-gapAngle(p,n)))>=Math.cos(Math.toRadians(22));}
    public static boolean wave(Vec3 delta,CompoundTag p,int n,double age){double t=age-waveTick(n);return t>=0 && t<=44 && Math.abs(delta.y)<=8 && Math.abs(delta.horizontalDistance()-waveRadius(age,n))<=1.5 && !inGap(delta,p,n);}
    public static boolean refuge(Vec3 relative,CompoundTag p){
        for(int n=0;n<3;n++)if((p.getInt("broken")&(1<<n))!=0){Vec3 d=relative.subtract(point(p,"seal_"+n));if(d.horizontalDistanceSqr()<=REFUGE_RADIUS*REFUGE_RADIUS && Math.abs(d.y)<=10)return true;}
        return false;
    }
}
