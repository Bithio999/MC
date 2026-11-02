package dev.heavenlyrestriction.mixin;
import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.tensura.registry.attribute.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=AttributeInstance.class,priority=500)
public abstract class ExponentiationGrowthWriteMixin {
    @ModifyVariable(method="setBaseValue",at=@At("HEAD"),argsOnly=true)
    private double exponentiation$award(double proposed){var attribute=(AttributeInstance)(Object)this;var owner=((TensuraAttributeInstance)this).tensura$getOwner();if((attribute.getAttribute().equals(TensuraAttributes.MAX_AURA) || attribute.getAttribute().equals(TensuraAttributes.MAX_MAGICULE)) && ExponentiationContext.rawAttribute(owner,ExponentiationContext.GROWTH) && Exponentiation.active(owner) && proposed>attribute.getBaseValue())return attribute.getBaseValue()+Exponentiation.square(proposed-attribute.getBaseValue());return proposed;}
}
