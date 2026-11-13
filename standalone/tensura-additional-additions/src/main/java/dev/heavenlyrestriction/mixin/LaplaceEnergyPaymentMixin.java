package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.laplace.PaymentRouting;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "io.github.manasmods.tensura.util.EnergyHelper", remap = false)
public abstract class LaplaceEnergyPaymentMixin {
  @WrapOperation(
      method = "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;DD)Z",
      at =
          @At(
              value = "INVOKE",
              target = "Lio/github/manasmods/tensura/storage/ep/IExistence;getMagicule()D"))
  private static double laplace$funding0(
      IExistence energy,
      Operation<Double> original,
      @Local(argsOnly = true) LivingEntity owner,
      @Local(argsOnly = true, ordinal = 1) double cost) {
    return PaymentRouting.funding(owner, energy, cost, original.call(energy));
  }

  @WrapOperation(
      method = "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;DD)Z",
      at =
          @At(
              value = "INVOKE",
              target = "Lio/github/manasmods/tensura/storage/ep/IExistence;setMagicule(D)V"))
  private static void laplace$debit0(
      IExistence energy,
      double amount,
      Operation<Void> original,
      @Local(argsOnly = true) LivingEntity owner) {
    if (!PaymentRouting.debit(owner, energy)) original.call(energy, amount);
  }

  @WrapOperation(
      method = "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;DD)Z",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V",
              remap = true))
  private static void laplace$message0(
      Player player,
      Component message,
      boolean actionBar,
      Operation<Void> original,
      @Local(argsOnly = true) LivingEntity owner) {
    original.call(player, PaymentRouting.message(owner, message), actionBar);
  }

  @WrapOperation(
      method = "isOutOfMagiculeConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D",
      at =
          @At(
              value = "INVOKE",
              target = "Lio/github/manasmods/tensura/storage/ep/IExistence;getMagicule()D"))
  private static double laplace$funding1(
      IExistence energy,
      Operation<Double> original,
      @Local(argsOnly = true) LivingEntity owner,
      @Local(argsOnly = true, ordinal = 0) double cost,
      @Local(argsOnly = true, ordinal = 1) double remain) {
    double funding = PaymentRouting.funding(owner, energy, cost, original.call(energy));
    return PaymentRouting.converted(owner) && funding >= cost ? cost + remain : funding;
  }

  @WrapOperation(
      method = "isOutOfMagiculeConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D",
      at =
          @At(
              value = "INVOKE",
              target = "Lio/github/manasmods/tensura/storage/ep/IExistence;setMagicule(D)V"))
  private static void laplace$debit1(
      IExistence energy,
      double amount,
      Operation<Void> original,
      @Local(argsOnly = true) LivingEntity owner) {
    if (!PaymentRouting.debit(owner, energy)) original.call(energy, amount);
  }

  @WrapOperation(
      method = "isOutOfMagiculeConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V",
              remap = true))
  private static void laplace$message1(
      Player player,
      Component message,
      boolean actionBar,
      Operation<Void> original,
      @Local(argsOnly = true) LivingEntity owner) {
    original.call(player, PaymentRouting.message(owner, message), actionBar);
  }

  @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(
      method =
          "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;IF)Z")
  private static boolean laplace$route(
      LivingEntity owner,
      io.github.manasmods.manascore.skill.api.ManasSkillInstance skill,
      int mode,
      float multiplier,
      Operation<Boolean> original) {
    try (var scope = PaymentRouting.open(owner, skill, mode)) {
      return original.call(owner, skill, mode, multiplier) || scope.failed();
    }
  }

  @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(
      method = "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;DD)Z")
  private static boolean laplace$direct(
      LivingEntity owner, double aura, double mp, Operation<Boolean> original) {
    var frame = dev.heavenlyrestriction.laplace.expand.ExpandedRuntime.current();
    if (frame == null || frame.owner() != owner || PaymentRouting.scoped(owner))
      return original.call(owner, aura, mp);
    double bill =
        dev.heavenlyrestriction.laplace.expand.ExpandedRuntime.budget(
            owner,
            frame.skill(),
            frame.mode(),
            mp + aura * dev.heavenlyrestriction.laplace.LaplaceConfig.rates().magiculesPerAura());
    if (bill < 0) return true;
    if (Double.isNaN(bill)) return original.call(owner, aura, mp);
    try (var scope = PaymentRouting.open(owner, frame.skill(), frame.mode())) {
      return original.call(owner, 0d, bill) || scope.failed();
    }
  }
}
