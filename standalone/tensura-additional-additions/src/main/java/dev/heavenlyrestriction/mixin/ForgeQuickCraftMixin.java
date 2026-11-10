package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.forge.ForgeItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Sign the result before quick-transfer copies it; credit only a successful take. */
@Mixin({CraftingMenu.class, InventoryMenu.class, ItemCombinerMenu.class})
public abstract class ForgeQuickCraftMixin {
  @WrapMethod(method = "quickMoveStack")
  private ItemStack forge$quickTake(Player player, int slot, Operation<ItemStack> original) {
    var menu = (AbstractContainerMenu) (Object) this;
    boolean result =
        menu instanceof SmithingMenu smith
            ? slot == smith.getResultSlot()
            : !(menu instanceof ItemCombinerMenu) && slot == 0;
    if (result && slot >= 0 && slot < menu.slots.size())
      ForgeItems.prepare(menu.getSlot(slot).getItem(), player);
    var taken = original.call(player, slot);
    if (result && !taken.isEmpty()) ForgeItems.craftedBy(taken, player);
    return taken;
  }
}
