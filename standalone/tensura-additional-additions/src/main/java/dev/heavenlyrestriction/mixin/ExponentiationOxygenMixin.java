package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.heavenlyrestriction.exponentiation.Exponentiation;
import net.minecraft.world.entity.*;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Air capacity is distinct from vanilla's oxygen-conservation attribute. */
@Mixin(Entity.class)
public abstract class ExponentiationOxygenMixin {
    private static final String AIR="heavenlyrestriction.exponentiation_air";
    @ModifyReturnValue(method="getMaxAirSupply",at=@At("RETURN"))
    private int exponentiation$capacity(int value){return (Object)this instanceof LivingEntity owner && Exponentiation.active(owner)?(int)Math.min(Integer.MAX_VALUE,20.0*Exponentiation.square(value/20.0)):value;}
    // Vanilla stores Air as a signed short, so use a full integer for larger custom air capacities.
    @Inject(method="saveWithoutId",at=@At("RETURN"))
    private void exponentiation$save(CompoundTag tag,CallbackInfoReturnable<CompoundTag> ci){var entity=(Entity)(Object)this;if(entity instanceof LivingEntity owner && Exponentiation.active(owner))ci.getReturnValue().putInt(AIR,entity.getAirSupply());}
    @Inject(method="load",at=@At("RETURN"))
    private void exponentiation$load(CompoundTag tag,CallbackInfo ci){var entity=(Entity)(Object)this;if(tag.contains(AIR))entity.setAirSupply(Math.max(-20,Math.min(entity.getMaxAirSupply(),tag.getInt(AIR))));}
}
