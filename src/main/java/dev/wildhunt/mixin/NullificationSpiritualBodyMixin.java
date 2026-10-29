package dev.wildhunt.mixin;

import dev.wildhunt.Nullification;
import io.github.manasmods.tensura.race.RaceUtils;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Defeat the spiritual body's separate physical reduction without changing damage type. */
@Mixin(RaceUtils.class)
public abstract class NullificationSpiritualBodyMixin {
    @Inject(method="getPhysicalAttackInputMultiplier",at=@At("HEAD"),cancellable=true,remap=false)
    private static void nullification$spiritualContact(DamageSource source,CallbackInfoReturnable<Float> out){
        if(Nullification.wielder(source)!=null)out.setReturnValue(1f);
    }
}
