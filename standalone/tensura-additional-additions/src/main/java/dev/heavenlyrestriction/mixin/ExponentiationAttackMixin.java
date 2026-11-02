package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.exponentiation.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
/** Attack attributes remain raw while a hit is calculated; its final damage is squared once. */
@Mixin(LivingEntity.class)
public abstract class ExponentiationAttackMixin {
    @WrapMethod(method="hurt")
    private boolean exponentiation$hurt(DamageSource source,float amount,Operation<Boolean> original){var owner=dev.heavenlyrestriction.RestrictionData.attacker(source);try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.DAMAGE)){return original.call(source,amount);}}
    @WrapMethod(method="readAdditionalSaveData")
    private void exponentiation$load(net.minecraft.nbt.CompoundTag tag,Operation<Void> original){try(var scope=ExponentiationContext.raw((LivingEntity)(Object)this,ExponentiationContext.RESTORE)){original.call(tag);}}
}
