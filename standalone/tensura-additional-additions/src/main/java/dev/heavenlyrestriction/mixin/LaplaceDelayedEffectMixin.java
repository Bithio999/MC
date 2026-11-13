package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.laplace.expand.*;
import net.minecraft.nbt.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEffectInstance.class)
public abstract class LaplaceDelayedEffectMixin implements EffectParameters {
  @Unique private CompoundTag additions$values;
  @Shadow private MobEffectInstance hiddenEffect;

  public MobEffectInstance additions$hidden() {
    return hiddenEffect;
  }

  public CompoundTag additions$parameters() {
    return additions$values;
  }

  public void additions$parameters(CompoundTag value) {
    additions$values = value == null ? null : value.copy();
  }

  @WrapMethod(method = "tick")
  private boolean laplace$tick(LivingEntity owner, Runnable ended, Operation<Boolean> original) {
    if (additions$values == null && !ParameterRuntime.active()) return original.call(owner, ended);
    try (var scope = ParameterSnapshots.effect(owner, (MobEffectInstance) (Object) this)) {
      return original.call(owner, ended);
    }
  }

  @WrapMethod(method = "update")
  private boolean laplace$update(MobEffectInstance incoming, Operation<Boolean> original) {
    int old = ((MobEffectInstance) (Object) this).getAmplifier();
    boolean changed = original.call(incoming);
    if (changed && incoming.getAmplifier() >= old)
      additions$parameters(((EffectParameters) (Object) incoming).additions$parameters());
    return changed;
  }

  @Inject(method = "setDetailsFrom", at = @At("RETURN"))
  private void laplace$details(MobEffectInstance source, CallbackInfo ci) {
    additions$parameters(((EffectParameters) (Object) source).additions$parameters());
  }

  @Inject(method = "<init>(Lnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("RETURN"))
  private void laplace$copy(MobEffectInstance source, CallbackInfo ci) {
    additions$parameters(((EffectParameters) (Object) source).additions$parameters());
  }

  @ModifyReturnValue(method = "save", at = @At("RETURN"))
  private Tag laplace$save(Tag original) {
    if (original instanceof CompoundTag tag) {
      if (additions$values != null) tag.put("additions_laplace", additions$values.copy());
      var chain = new ListTag();
      var hidden = hiddenEffect;
      for (int n = 0; hidden != null && n < 128; n++) {
        var effect = (EffectParameters) (Object) hidden;
        var row = new CompoundTag();
        var values = effect.additions$parameters();
        if (values != null) row.put("values", values.copy());
        chain.add(row);
        hidden = effect.additions$hidden();
      }
      if (!chain.isEmpty()) tag.put("additions_laplace_hidden", chain);
    }
    return original;
  }

  @ModifyReturnValue(method = "load", at = @At("RETURN"))
  private static MobEffectInstance laplace$load(MobEffectInstance result, CompoundTag tag) {
    if (result != null && tag.contains("additions_laplace"))
      ((EffectParameters) (Object) result)
          .additions$parameters(tag.getCompound("additions_laplace"));
    if (result != null) {
      var chain = tag.getList("additions_laplace_hidden", 10);
      var hidden = ((EffectParameters) (Object) result).additions$hidden();
      for (int n = 0; hidden != null && n < Math.min(128, chain.size()); n++) {
        var effect = (EffectParameters) (Object) hidden;
        var row = chain.getCompound(n);
        effect.additions$parameters(row.contains("values") ? row.getCompound("values") : null);
        hidden = effect.additions$hidden();
      }
    }
    return result;
  }
}
