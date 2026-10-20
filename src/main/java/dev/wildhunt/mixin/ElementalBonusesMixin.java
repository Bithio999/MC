package dev.wildhunt.mixin;

import dev.wildhunt.ElementalBonuses;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttributeInstance.class)
public abstract class ElementalBonusesMixin {
    @Inject(method="calculateValue",at=@At("RETURN"),cancellable=true)
    private void myths$strongestElementalSource(CallbackInfoReturnable<Double> ci) {
        var instance=(AttributeInstance)(Object)this;
        if(instance.getAttribute().unwrapKey().filter(k->ElementalBonuses.elemental(k.location())).isEmpty())return;
        ci.setReturnValue(instance.getAttribute().value().sanitizeValue(ElementalBonuses.value(instance.getBaseValue(),instance.getModifiers())));
    }
}
