package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.heavenlyrestriction.forge.ForgeEngravingPolicy;
import io.github.manasmods.tensura.menu.ResearcherEnchantingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ResearcherEnchantingMenu.class, remap = false)
public abstract class ForgeResearchMenuMixin {
  @WrapOperation(
      method = {"previewEnchantmentsOnItem", "handleFinalizeEnchantment"},
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;setEnchantments(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/ItemEnchantments;)V"))
  private void forge$apply(ItemStack output, ItemEnchantments selected, Operation<Void> original) {
    var menu = (ResearcherEnchantingMenu) (Object) this;
    original.call(
        output,
        ForgeEngravingPolicy.application(
            menu.getSkill(), menu.getItemInput().getItem(0), selected));
    ForgeEngravingPolicy.markAllowance(menu.getSkill(), output);
  }
}
