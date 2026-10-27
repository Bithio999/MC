package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.*;
import io.github.manasmods.tensura.handler.DamagingHandler;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Numeric resistance is one resistance tier. Keep barriers and interference separate. */
@Mixin(DamagingHandler.class)
public abstract class ChainedResistanceAttributesMixin {
    @WrapOperation(method="applyResistanceAttributes",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"),remap=false)
    private static double restriction$numericTier(LivingEntity entity,Holder<Attribute> attribute,Operation<Double> original,@Local(argsOnly=true) DamageSource source){
        if(ChainedState.mastered(RestrictionData.attacker(source)) && !attribute.equals(TensuraAttributes.MAGIC_INTERFERENCE) && !attribute.equals(TensuraAttributes.RESISTANCE_DEGRADATION))return 0;
        return original.call(entity,attribute);
    }
}
