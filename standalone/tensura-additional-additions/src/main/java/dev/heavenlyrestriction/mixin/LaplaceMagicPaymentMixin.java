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

@Mixin(targets = "io.github.manasmods.tensura.ability.magic.Magic", remap = false)
public abstract class LaplaceMagicPaymentMixin {
  @WrapOperation(
      method =
          "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;DD)Z",
      at =
          @At(
              value = "INVOKE",
              target = "Lio/github/manasmods/tensura/storage/ep/IExistence;getMagicule()D"))
  private double laplace$funding0(
      IExistence energy,
      Operation<Double> original,
      @Local(argsOnly = true) LivingEntity owner,
      @Local(argsOnly = true, ordinal = 1) double cost) {
    return PaymentRouting.funding(owner, energy, cost, original.call(energy));
  }

  @WrapOperation(
      method =
          "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;DD)Z",
      at =
          @At(
              value = "INVOKE",
              target = "Lio/github/manasmods/tensura/storage/ep/IExistence;setMagicule(D)V"))
  private void laplace$debit0(
      IExistence energy,
      double amount,
      Operation<Void> original,
      @Local(argsOnly = true) LivingEntity owner) {
    if (!PaymentRouting.debit(owner, energy)) original.call(energy, amount);
  }

  @WrapOperation(
      method =
          "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;DD)Z",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V",
              remap = true))
  private void laplace$message0(
      Player player,
      Component message,
      boolean actionBar,
      Operation<Void> original,
      @Local(argsOnly = true) LivingEntity owner) {
    original.call(player, PaymentRouting.message(owner, message), actionBar);
  }

  @WrapOperation(
      method =
          "isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;DD)Z",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/LivingEntity;getUseItem()Lnet/minecraft/world/item/ItemStack;",
              remap = true))
  private net.minecraft.world.item.ItemStack laplace$staff(
      LivingEntity owner, Operation<net.minecraft.world.item.ItemStack> original) {
    return PaymentRouting.converted(owner)
        ? net.minecraft.world.item.ItemStack.EMPTY
        : original.call(owner);
  }
}
