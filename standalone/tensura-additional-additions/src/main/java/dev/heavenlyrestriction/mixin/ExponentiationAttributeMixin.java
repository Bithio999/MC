package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.heavenlyrestriction.exponentiation.Exponentiation;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Return-value composition preserves native and other addon checks, without canceling their callbacks. */
@Mixin(value=AttributeInstance.class,priority=500)
public abstract class ExponentiationAttributeMixin {
    @ModifyReturnValue(method="getValue",at=@At("RETURN"))
    private double exponentiation$final(double value){var owner=((TensuraAttributeInstance)this).tensura$getOwner();return owner==null?value:Exponentiation.attribute(owner,(AttributeInstance)(Object)this,value);}
}
