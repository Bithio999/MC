package dev.wildhunt.mixin;

import dev.wildhunt.ChainedResources;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cap all current-MP writers; native capacity and exhaustion values retain their own rules. */
@Mixin(ExistenceStorage.class)
public abstract class ChainedReserveMixin {
    @Shadow(remap=false) protected abstract LivingEntity getOwner();
    @ModifyVariable(method="setMagicule",at=@At("HEAD"),argsOnly=true,remap=false)
    private double myths$currentMagicules(double amount){return ChainedResources.capMagicules(getOwner(),amount);}
    @Inject(method="setEP",at=@At("RETURN"),remap=false)
    private void myths$combinedEnergy(double amount,CallbackInfo out){ChainedResources.reconcile(getOwner());}
}
