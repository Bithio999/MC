package dev.heavenlyrestriction.mixin;
import dev.heavenlyrestriction.fusion.Fusion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.function.Predicate;
/** The embedded participant is not a second world target. Covers vanilla/Jade rays and typed Tensura scans. */
@Mixin(Level.class)
public abstract class FusionEntityQueryMixin {
    @ModifyVariable(method={"getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;","getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V"},at=@At("HEAD"),argsOnly=true,ordinal=0)
    private Predicate<Entity> fusion$excludeEmbedded(Predicate<Entity> original){return e->!Fusion.embedded(e) && original.test(e);}
}
