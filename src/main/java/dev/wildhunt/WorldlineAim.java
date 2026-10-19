package dev.wildhunt;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** Shared, bounded beam steering and collision rules for the server and local preview. */
public final class WorldlineAim {
    private WorldlineAim() {}
    public static final double RESPONSE_SECONDS=.06;
    public static Vec3 approach(Vec3 current,Vec3 wanted,double seconds) {
        Vec3 target=wanted.normalize();if(target.lengthSqr()<.5)return current;
        if(current.lengthSqr()<.5)return target;
        Vec3 from=current.normalize();double dot=Math.clamp(from.dot(target),-1,1),angle=Math.acos(dot);
        if(angle<.00001)return target;
        double fraction=1-Math.exp(-Math.max(0,seconds)/RESPONSE_SECONDS);
        Vec3 tangent=target.subtract(from.scale(dot));
        // A half-turn has no unique tangent. Choose a stable arc rather than snapping through zero.
        if(tangent.lengthSqr()<1e-10)tangent=from.cross(Math.abs(from.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0));
        return from.scale(Math.cos(angle*fraction)).add(tangent.normalize().scale(Math.sin(angle*fraction))).normalize();
    }
    public static Vec3 end(LivingEntity caster,Vec3 eye,Vec3 facing) {
        Vec3 end=eye.add(facing.normalize().scale(80));
        return caster.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster)).getLocation();
    }
}
