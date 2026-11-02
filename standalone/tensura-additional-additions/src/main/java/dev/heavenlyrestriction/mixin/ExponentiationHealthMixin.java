package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.tensura.storage.effect.EffectStorage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** The final health-data writer sees native healing and Schema's completed delta, including direct heals. */
@Mixin(value=LivingEntity.class,priority=500)
public abstract class ExponentiationHealthMixin {
    @WrapMethod(method="setHealth")
    private void exponentiation$capacity(float health,Operation<Void> original){try(var scope=ExponentiationContext.raw((LivingEntity)(Object)this,ExponentiationContext.FINAL)){original.call(health);}}
    @WrapOperation(method="setHealth",at=@At(value="INVOKE",target="Lnet/minecraft/network/syncher/SynchedEntityData;set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V"))
    private void exponentiation$heal(SynchedEntityData data,EntityDataAccessor<Object> key,Object value,Operation<Void> original){var owner=(LivingEntity)(Object)this;if(!owner.level().isClientSide() && value instanceof Float proposed && Exponentiation.active(owner))value=(float)Exponentiation.gain(owner,owner.getHealth(),proposed,Math.min(Exponentiation.FLOAT_LIMIT,EffectStorage.getSeveranceMaxHealth(owner)));original.call(data,key,value);}
}
