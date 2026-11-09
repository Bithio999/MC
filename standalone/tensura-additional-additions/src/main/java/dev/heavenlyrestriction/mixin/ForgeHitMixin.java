package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.forge.ForgeTempering;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class ForgeHitMixin {
  @WrapMethod(method = "hurt")
  private boolean forge$hit(DamageSource source, float amount, Operation<Boolean> original) {
    return ForgeTempering.observe(
        (LivingEntity) (Object) this, source, () -> original.call(source, amount));
  }
}
