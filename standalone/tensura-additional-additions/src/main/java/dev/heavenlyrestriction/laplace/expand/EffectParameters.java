package dev.heavenlyrestriction.laplace.expand;

import net.minecraft.nbt.CompoundTag;

public interface EffectParameters {
  net.minecraft.world.effect.MobEffectInstance additions$hidden();

  CompoundTag additions$parameters();

  void additions$parameters(CompoundTag value);
}
