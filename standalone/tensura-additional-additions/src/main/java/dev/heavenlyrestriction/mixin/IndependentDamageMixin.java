package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.IndependentHitSource;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Leaves native Space/skill metadata intact; never changes another attack's hurt cooldown. */
@Mixin(DamageSource.class)
public abstract class IndependentDamageMixin implements IndependentHitSource {
    @Unique private boolean restriction$independent;
    @Override public void restriction$independentHit(){restriction$independent=true;}
    @Inject(method="is(Lnet/minecraft/tags/TagKey;)Z",at=@At("HEAD"),cancellable=true)
    private void restriction$collision(TagKey<DamageType> tag,CallbackInfoReturnable<Boolean> result){
        if(restriction$independent && tag.equals(DamageTypeTags.BYPASSES_COOLDOWN))result.setReturnValue(true);
    }
}
