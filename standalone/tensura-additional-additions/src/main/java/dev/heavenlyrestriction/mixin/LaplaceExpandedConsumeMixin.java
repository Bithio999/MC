package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.laplace.*;
import dev.heavenlyrestriction.laplace.expand.*;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.github.manasmods.tensura.util.EnergyHelper", remap = false)
public abstract class LaplaceExpandedConsumeMixin {
  @WrapMethod(method = "isOutOfMagiculeConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D")
  private static double laplace$mp(
      LivingEntity owner, double cost, double remain, Operation<Double> original) {
    return consume(owner, cost, remain, false, original);
  }

  @WrapMethod(method = "isOutOfAuraConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D")
  private static double laplace$ap(
      LivingEntity owner, double cost, double remain, Operation<Double> original) {
    return consume(owner, cost, remain, true, original);
  }

  private static double consume(
      LivingEntity owner, double cost, double remain, boolean aura, Operation<Double> original) {
    var frame = ExpandedRuntime.current();
    if (frame == null || frame.owner() != owner || PaymentRouting.scoped(owner))
      return original.call(owner, cost, remain);
    double rate = aura ? LaplaceConfig.rates().magiculesPerAura() : 1;
    double bill = ExpandedRuntime.budget(owner, frame.skill(), frame.mode(), cost * rate);
    if (bill < 0) return cost;
    if (Double.isNaN(bill)) return original.call(owner, cost, remain);
    try (var scope = PaymentRouting.open(owner, frame.skill(), frame.mode(), true)) {
      scope.reserve(Math.max(0, remain) * rate);
      double shortfall = original.call(owner, bill / rate, remain);
      return scope.failed() ? cost : shortfall > 0 ? cost : 0;
    }
  }

  @WrapOperation(
      method = "isOutOfAuraConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D",
      at =
          @At(
              value = "INVOKE",
              target = "Lio/github/manasmods/tensura/storage/ep/IExistence;getAura()D"))
  private static double laplace$fund(
      IExistence energy,
      Operation<Double> original,
      @Local(argsOnly = true) LivingEntity owner,
      @Local(argsOnly = true, ordinal = 0) double cost,
      @Local(argsOnly = true, ordinal = 1) double remain) {
    if (!PaymentRouting.converted(owner)) return original.call(energy);
    double budget = cost * LaplaceConfig.rates().magiculesPerAura();
    double funding = PaymentRouting.funding(owner, energy, budget, energy.getMagicule());
    return funding >= budget ? cost + remain : 0;
  }

  @WrapOperation(
      method = "isOutOfAuraConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D",
      at =
          @At(
              value = "INVOKE",
              target = "Lio/github/manasmods/tensura/storage/ep/IExistence;setAura(D)V"))
  private static void laplace$pay(
      IExistence energy,
      double amount,
      Operation<Void> original,
      @Local(argsOnly = true) LivingEntity owner) {
    if (!PaymentRouting.debit(owner, energy)) original.call(energy, amount);
  }
}
