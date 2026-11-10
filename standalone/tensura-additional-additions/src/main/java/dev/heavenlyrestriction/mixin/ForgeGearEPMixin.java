package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.heavenlyrestriction.forge.*;
import io.github.manasmods.tensura.handler.DeathHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Join the existing native equipment award, after all kill eligibility / gamerule checks. */
@Mixin(value = DeathHandler.class, remap = false)
public abstract class ForgeGearEPMixin {
  private static final String GROWTH =
      "gearGetEP(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;D)V";

  @Inject(method = GROWTH, at = @At("RETURN"))
  private static void forge$arsenal(
      LivingEntity owner, EquipmentSlot slot, double ep, CallbackInfo ci) {
    if (slot == EquipmentSlot.MAINHAND
        && owner instanceof ServerPlayer player
        && !ForgeGearGrowth.active(owner)) ForgeArsenal.gainGearEP(player, ep);
  }

  @WrapOperation(
      method = GROWTH,
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
  private static ItemStack forge$read(
      LivingEntity owner, EquipmentSlot slot, Operation<ItemStack> original) {
    var deployed = ForgeGearGrowth.get(owner, slot);
    return deployed != null ? deployed : original.call(owner, slot);
  }

  @WrapOperation(
      method = GROWTH,
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/LivingEntity;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
  private static void forge$evolve(
      LivingEntity owner, EquipmentSlot slot, ItemStack item, Operation<Void> original) {
    if (!ForgeGearGrowth.replace(owner, slot, item)) original.call(owner, slot, item);
  }
}
