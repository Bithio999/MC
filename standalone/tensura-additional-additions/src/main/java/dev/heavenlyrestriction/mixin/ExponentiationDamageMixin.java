package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.exponentiation.Exponentiation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Runs after all native/NeoForge Pre listeners, including Schema and Fusion. */
@Mixin(CommonHooks.class)
public abstract class ExponentiationDamageMixin {
    @ModifyReturnValue(method="onLivingDamagePre",at=@At("RETURN"),remap=false)
    private static float exponentiation$damage(float value,LivingEntity victim,DamageContainer damage){var owner=RestrictionData.attacker(damage.getSource());if(!victim.level().isClientSide() && owner!=victim && Exponentiation.active(owner)){float amount=Exponentiation.squareFloat(value);damage.setNewDamage(amount);return amount;}return value;}
}
