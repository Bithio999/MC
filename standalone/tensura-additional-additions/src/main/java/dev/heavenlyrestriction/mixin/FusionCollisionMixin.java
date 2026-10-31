package dev.heavenlyrestriction.mixin;
import dev.heavenlyrestriction.fusion.Fusion;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** A retained body must never push or collide with its controller, even through a direct callback. */
@Mixin(Entity.class)
public abstract class FusionCollisionMixin {
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void fusion$noPush(Entity other,CallbackInfo ci){if(Fusion.embedded((Entity)(Object)this) || Fusion.embedded(other))ci.cancel();}
    @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
    private void fusion$noCollision(Entity other,CallbackInfoReturnable<Boolean> cir){if(Fusion.embedded((Entity)(Object)this) || Fusion.embedded(other))cir.setReturnValue(false);}
}
