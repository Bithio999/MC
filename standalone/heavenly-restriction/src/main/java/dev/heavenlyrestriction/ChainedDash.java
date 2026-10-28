package dev.heavenlyrestriction;

import io.github.manasmods.tensura.data.TensuraBlockTags;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/** Eye-level targeting with safe air landings and a stop just in front of the aimed enemy. */
public final class ChainedDash {
    public static final double REACH=16;
    private ChainedDash(){}
    public static Optional<Vec3> destination(LivingEntity caster){
        if(!(caster.level() instanceof ServerLevel level) || !caster.onGround() && !caster.isInWaterOrBubble())return Optional.empty();
        var hit=ObjectSelectionHelper.getPlayerPOVHitResult(level,caster,ClipContext.Fluid.NONE,REACH);
        Vec3 eye=caster.getEyePosition(),look=caster.getLookAngle();
        double distance=hit.getType()==HitResult.Type.BLOCK?eye.distanceTo(hit.getLocation()):REACH;
        Vec3 rayEnd=eye.add(look.scale(distance));
        LivingEntity aimed=null;double nearest=distance;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(eye,rayEnd).inflate(1),e->RestrictionData.canHarm(caster,e) && !e.isSpectator())){
            var intersection=target.getBoundingBox().inflate(target.getPickRadius()).clip(eye,rayEnd);
            if(intersection.isPresent()){
                double candidate=eye.distanceTo(intersection.get());
                if(candidate<nearest){nearest=candidate;aimed=target;}
            }
        }
        if(aimed!=null){
            // Expand by the caster's width so the landing cannot overlap the victim's body.
            double clearance=caster.getBbWidth()*.5+.1;
            var box=aimed.getBoundingBox().inflate(clearance,0,clearance);
            Vec3 contact=box.contains(eye)?eye:box.clip(eye,rayEnd).orElse(eye.add(look.scale(nearest)));
            Vec3 end=contact.subtract(look.scale(.05)).subtract(0,caster.getEyeHeight(),0);
            end=new Vec3(end.x,Math.max(end.y,aimed.getY()),end.z);
            return safeNear(level,caster,end,look,aimed.getY());
        }
        // A MISS has no real block face. Applying its synthetic face offset can put an
        // upward-looking landing in the floor. Keep the aim displacement relative to the feet.
        if(hit.getType()==HitResult.Type.MISS)return safeNear(level,caster,caster.position().add(look.scale(REACH)),look,Double.NEGATIVE_INFINITY);
        var cell=hit.getBlockPos().relative(hit.getDirection());
        Vec3 end=ObjectSelectionHelper.getFloorPos(cell);
        if(!level.getBlockState(cell).canBeReplaced())end=ObjectSelectionHelper.getFloorPos(cell.above());
        if(level.getBlockState(cell).is(TensuraBlockTags.SKILL_NOT_TELEPORTABLE))return Optional.empty();
        return safe(level,caster,end)?Optional.of(end):Optional.empty();
    }
    private static Optional<Vec3> safeNear(ServerLevel level,LivingEntity caster,Vec3 end,Vec3 look,double floor){
        // Small landing-only backoff handles tight corners; never sweep the feet along the ray.
        for(int step=0;step<=4;step++){
            Vec3 candidate=end.subtract(look.scale(step*.25));
            candidate=new Vec3(candidate.x,Math.max(candidate.y,floor),candidate.z);
            if(candidate.distanceToSqr(caster.position())>REACH*REACH+.001 || candidate.subtract(caster.position()).dot(look)<.25)continue;
            if(safe(level,caster,candidate))return Optional.of(candidate);
        }
        return Optional.empty();
    }
    private static boolean safe(ServerLevel level,LivingEntity caster,Vec3 end){
        var cell=net.minecraft.core.BlockPos.containing(end);
        var body=caster.getBoundingBox().move(end.subtract(caster.position()));
        return body.minY>=level.getMinBuildHeight() && body.maxY<=level.getMaxBuildHeight() && level.hasChunkAt(cell) && level.getWorldBorder().isWithinBounds(body) && !level.getBlockState(cell).is(TensuraBlockTags.SKILL_NOT_TELEPORTABLE) && level.noCollision(caster,body) && end.distanceToSqr(caster.position())>=.0625;
    }
}
