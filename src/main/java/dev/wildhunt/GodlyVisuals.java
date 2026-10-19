package dev.wildhunt;

import io.github.manasmods.tensura.entity.magic.MagicCircle;
import io.github.manasmods.tensura.entity.variant.MagicCircleVariant;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Synced geometry plus Tensura's native SPACE/GRAVITY/RECOVERY magic circles. */
public final class GodlyVisuals {
    private GodlyVisuals() {}
    public static MagicCircle circle(ServerLevel level,LivingEntity owner,Vec3 center,float radius,int ticks,MagicCircleVariant variant){
        var circle=new MagicCircle(level,owner);circle.setVariant(variant);circle.setFollowPosition(false);circle.setFollowViewDistance(-1);
        circle.setSpinning(true);circle.setSize(radius);circle.setRemoveIn(ticks);circle.setPos(center);circle.setXRot(-90);level.addFreshEntity(circle);return circle;
    }
    public static void impact(ServerLevel level,Vec3 center,double radius){
        ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_IMPACT,center,new Vec3(radius,0,0),22);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,center.x,center.y,center.z,32,radius*.2,radius*.2,radius*.2,.12);
        level.sendParticles(ParticleTypes.END_ROD,center.x,center.y,center.z,16,.4,.6,.4,.08);
    }
}
