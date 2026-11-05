package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.heavenlyrestriction.chromatic.ChromaticOrb;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RepairItemRecipe.class)
public abstract class ChromaticRepairMixin {
  @ModifyReturnValue(
      method =
          "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
      at = @At("RETURN"))
  private ItemStack chromatic$preserve(
      ItemStack result, CraftingInput input, HolderLookup.Provider registries) {
    ChromaticOrb.preserve(result, input.items(), false);
    return result;
  }
}
