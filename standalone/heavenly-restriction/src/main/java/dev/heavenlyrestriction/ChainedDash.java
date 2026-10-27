package dev.heavenlyrestriction;

import io.github.manasmods.tensura.data.TensuraBlockTags;
import io.github.manasmods.tensura.util.ObjectSelectionHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/** Martial Master's native eye ray, adjacent landing cell and one-block replacement fallback. */
public final class ChainedDash {
    public static final double REACH=16;
    private ChainedDash(){}
    public static Optional<Vec3> destination(LivingEntity caster){
        if(!(caster.level() instanceof ServerLevel level) || !caster.onGround() && !caster.isInWaterOrBubble())return Optional.empty();
        var hit=ObjectSelectionHelper.getPlayerPOVHitResult(level,caster,ClipContext.Fluid.NONE,REACH);
        var cell=hit.getBlockPos().relative(hit.getDirection());
        Vec3 end=ObjectSelectionHelper.getFloorPos(cell);
        if(!level.getBlockState(cell).canBeReplaced())end=ObjectSelectionHelper.getFloorPos(cell.above());
        if(level.getBlockState(cell).is(TensuraBlockTags.SKILL_NOT_TELEPORTABLE))return Optional.empty();
        var body=caster.getBoundingBox().move(end.subtract(caster.position()));
        // Only validate the landing body: a downward-looking ray must not sweep the feet into the floor.
        if(body.minY<level.getMinBuildHeight() || body.maxY>level.getMaxBuildHeight() || !level.hasChunkAt(net.minecraft.core.BlockPos.containing(end)) || !level.getWorldBorder().isWithinBounds(body) || !level.noCollision(caster,body) || end.distanceToSqr(caster.position())<.0625)return Optional.empty();
        return Optional.of(end);
    }
}
