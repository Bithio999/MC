package dev.wildhunt;

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
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(eye,rayEnd).inflate(1),e->HunterData.canHarm(caster,e) && !e.isSpectator())){
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
        var landing=safeNear(level,caster,end,look,Double.NEGATIVE_INFINITY);
        if(landing.isPresent())return landing;
        // A nearby surface can snap to a block center behind the caster. Retry its exact
        // contact position instead of teleporting backward or rejecting available space.
        Vec3 contact=hit.getLocation().subtract(look.scale(caster.getBbWidth()*.5+.05));
        Vec3 feet=contact.subtract(0,caster.getEyeHeight(),0);
        if(hit.getDirection()==net.minecraft.core.Direction.UP)feet=new Vec3(feet.x,hit.getLocation().y,feet.z);
        return safeNear(level,caster,feet,look,Double.NEGATIVE_INFINITY);
    }
    private static final double LANDING_STEP=.25,VERTICAL_SEARCH=1.5,MIN_MOVE=.001;
    private static Optional<Vec3> safeNear(ServerLevel level,LivingEntity caster,Vec3 end,Vec3 look,double floor){
        if(forward(caster,end,look) && safe(level,caster,end))return Optional.of(end);
        // Preserve successful native destinations; on failure, find standing room progressively
        // closer to the caster. Query real collision surfaces so slabs/stairs/snow are not rounded.
        double distance=end.distanceTo(caster.position());
        for(double backoff=0;backoff<=distance;backoff+=LANDING_STEP){
            Vec3 base=end.subtract(look.scale(backoff));base=new Vec3(base.x,Math.max(base.y,floor),base.z);
            if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(base)))continue;
            var heights=new java.util.TreeSet<Double>();heights.add(base.y);heights.add(caster.getY());
            var probe=caster.getBoundingBox().move(base.subtract(caster.position())).inflate(0,VERTICAL_SEARCH+.001,0);
            for(var shape:level.getBlockCollisions(caster,probe))for(var box:shape.toAabbs())heights.add(box.maxY);
            final Vec3 at=base;
            var ordered=heights.stream().filter(y->y>=floor && Math.abs(y-at.y)<=VERTICAL_SEARCH).sorted(java.util.Comparator.comparingDouble(y->Math.abs(y-at.y))).toList();
            for(double height:ordered){
                Vec3 candidate=new Vec3(base.x,height,base.z);
                if(forward(caster,candidate,look) && safe(level,caster,candidate) && clearView(level,caster,candidate))return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
    private static boolean forward(LivingEntity caster,Vec3 end,Vec3 look){return end.subtract(caster.position()).dot(look)>=MIN_MOVE;}
    private static boolean clearView(ServerLevel level,LivingEntity caster,Vec3 end){
        // Vertical adjustments must stay on this side of walls and canopies, never on their roof.
        return level.clip(new ClipContext(caster.getEyePosition(),end.add(0,caster.getEyeHeight(),0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster)).getType()==HitResult.Type.MISS;
    }
    private static boolean safe(ServerLevel level,LivingEntity caster,Vec3 end){
        var cell=net.minecraft.core.BlockPos.containing(end);
        var body=caster.getBoundingBox().move(end.subtract(caster.position()));
        return body.minY>=level.getMinBuildHeight() && body.maxY<=level.getMaxBuildHeight() && level.hasChunkAt(cell) && level.getWorldBorder().isWithinBounds(body) && !level.getBlockState(cell).is(TensuraBlockTags.SKILL_NOT_TELEPORTABLE) && level.noCollision(caster,body) && end.distanceToSqr(caster.position())>=MIN_MOVE*MIN_MOVE;
    }
}
