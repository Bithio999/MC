package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Direct-consumption spells (including healing) use the same single background discount. */
@Mixin(value = EnergyHelper.class, remap = false)
public abstract class MetisCostMixin {
  @ModifyVariable(
      method = "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;DD)Z",
      at = @At("HEAD"),
      argsOnly = true,
      index = 3)
  private static double metis$bill(
      double amount, LivingEntity owner, double aura, double magicules) {
    return SplitMindCasting.automatic(owner)
        ? amount * SplitMindCasting.magiculeFactor(owner)
        : amount;
  }

  @ModifyVariable(
      method = "isOutOfMagiculeConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D",
      at = @At("HEAD"),
      argsOnly = true,
      index = 1)
  private static double metis$healing(
      double amount, LivingEntity owner, double cost, double remain) {
    return SplitMindCasting.automatic(owner)
        ? amount * SplitMindCasting.magiculeFactor(owner)
        : amount;
  }

  @ModifyReturnValue(
      method = "isOutOfMagiculeConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D",
      at = @At("RETURN"))
  private static double metis$shortfall(
      double amount, LivingEntity owner, double cost, double remain) {
    return amount > 0 && SplitMindCasting.automatic(owner)
        ? amount / SplitMindCasting.magiculeFactor(owner)
        : amount;
  }
}
