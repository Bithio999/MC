package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.vesper.Afterwind;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Include native spiritual-body damage and cancelled/immune hits in the same transaction. */
@Mixin(LivingEntity.class)
public abstract class AfterwindHitMixin {
  @WrapMethod(method = "hurt")
  private boolean vesper$commit(DamageSource source, float amount, Operation<Boolean> original) {
    var target = (LivingEntity) (Object) this;
    return Afterwind.observe(target, source, () -> original.call(source, amount));
  }
}
