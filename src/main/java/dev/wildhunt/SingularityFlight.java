package dev.wildhunt;

import net.minecraft.world.phys.Vec3;

/** Seeded, continuous choreography shared by damage timing and every client's renderer. */
public final class SingularityFlight {
    public static final int COUNT=20, FIRST_IMPACT=32, IMPACT_INTERVAL=2, IMPACT_LIFE=32, LIFE=104;
    private SingularityFlight(){}
    public static double random(int seed,int node,int salt){
        long v=((long)seed<<32) ^ node*0x9E3779B97F4A7C15L ^ salt*0xBF58476D1CE4E5B9L;
        v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;v=(v^(v>>>27))*0x94D049BB133111EBL;return ((v^(v>>>31))>>>11)*0x1.0p-53;
    }
    public static int impact(int node){return FIRST_IMPACT+IMPACT_INTERVAL*node;}
    public static int duration(int seed,int node){return 10+(int)(random(seed,node,8)*6);}
    public static int launch(int seed,int node){return impact(node)-duration(seed,node);}
    public static double birth(int seed,int node){return node*.38+random(seed,node,7)*2;}
    public static Vec3 origin(int seed,int node){
        double a=node*2.3999632297+(random(seed,node,0)-.5)*.65,r=8+random(seed,node,1)*5;
        return new Vec3(Math.cos(a)*r,3.4+random(seed,node,2)*8.2,Math.sin(a)*r);
    }
    public static Vec3 impactOffset(int seed,int node){
        double a=node*2.3999632297+random(seed,node,21)*.4,r=.8+random(seed,node,22)*.7;
        return new Vec3(Math.cos(a)*r,(random(seed,node,23)-.5)*.9,Math.sin(a)*r);
    }
    private static Vec3 hovered(int seed,int node,double age){
        double born=birth(seed,node),t=Math.max(0,age-born),a=random(seed,node,3)*Math.PI*2;
        // Small, smooth suspended motion; the launch samples this same function exactly.
        return origin(seed,node).add(.15*Math.sin(t*.055+a),.14*Math.sin(t*.075+a*2),.15*Math.cos(t*.055+a));
    }
    public static Vec3 position(int seed,int node,double age,Vec3 aim){
        int launch=launch(seed,node);if(age<=launch)return hovered(seed,node,age);
        double u=Math.clamp((age-launch)/duration(seed,node),0,1);
        // Accelerate into the strike, without slowing to a stop immediately before impact.
        double t=u*u*(.78+.22*u),v=1-t;
        Vec3 start=hovered(seed,node,launch),end=aim.add(impactOffset(seed,node));
        Vec3 tangent=new Vec3(-start.z,0,start.x).normalize();double bend=(random(seed,node,4)<.5?-1:1)*(3.5+random(seed,node,5)*5);
        Vec3 first=start.add(tangent.scale(bend)).add(start.normalize().scale(1.8)).add(0,2.4+random(seed,node,6)*2,0);
        Vec3 last=end.add(tangent.scale(-bend*.58)).add(start.subtract(end).scale(.13)).add(0,2+random(seed,node,9)*3,0);
        return start.scale(v*v*v).add(first.scale(3*v*v*t)).add(last.scale(3*v*t*t)).add(end.scale(t*t*t));
    }
    public static Vec3 velocity(int seed,int node,double age,Vec3 aim){return position(seed,node,age+.025,aim).subtract(position(seed,node,age-.025,aim)).scale(20);}
    public static double stretch(double speed){return 1+Math.clamp(speed*.62,0,1.9);}
    public static double transverse(double stretch){return 1/Math.sqrt(stretch);}
}
