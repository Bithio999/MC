package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.Nullification;
import io.github.manasmods.tensura.handler.DamagingHandler;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Only waive spiritual-form contact immunity; retain all other native damage checks. */
@Mixin(DamagingHandler.class)
public abstract class NullificationSpiritualFormMixin {
    @WrapOperation(method="lambda$init$0",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/ep/IExistence;isSpiritualForm()Z"),remap=false)
    private static boolean nullification$spiritualContact(IExistence existence,Operation<Boolean> original,@Local(argsOnly=true) DamageSource source){
        return original.call(existence) && Nullification.wielder(source)==null;
    }
}
