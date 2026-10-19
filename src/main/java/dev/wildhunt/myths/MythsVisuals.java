package dev.wildhunt.myths;

import dev.wildhunt.ArtVisualEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class MythsVisuals {
    private MythsVisuals() {}
    public static void convergence(ServerLevel level,Vec3 center,int first,int second) {
        ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_SPHERE,center,new Vec3(4,1,0),60);
        for(int i=0;i<48;i++) {
            double a=i*Math.PI/12; double r=1+i/16d;
            int c=i%2==0?first:second;
            var dust=new DustParticleOptions(new Vector3f(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f),1.5f);
            level.sendParticles(dust,center.x+Math.cos(a)*r,center.y+i/16d,center.z+Math.sin(a)*r,1,0,0,0,0);
        }
    }
}
