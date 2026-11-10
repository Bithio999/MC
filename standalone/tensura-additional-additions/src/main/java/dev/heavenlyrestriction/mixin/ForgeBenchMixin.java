package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.forge.ForgeItems;
import io.github.manasmods.tensura.menu.SmithingBenchMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Tensura's bench never invokes ItemStack.onCraftedBy or the NeoForge crafting event. */
@Mixin(value = SmithingBenchMenu.class, remap = false)
public abstract class ForgeBenchMixin {
  @ModifyArg(
      method = "setupResultSlot",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
      index = 1)
  private ItemStack forge$prepare(ItemStack output) {
    var bench = (SmithingBenchMenu) (Object) this;
    ForgeItems.prepare(output, bench.getPlayer());
    return output;
  }

  @WrapMethod(method = "quickMoveStack")
  private ItemStack forge$quickTake(Player player, int slot, Operation<ItemStack> original) {
    var bench = (SmithingBenchMenu) (Object) this;
    if (slot == 36) ForgeItems.prepare(bench.getSlot(36).getItem(), player);
    var result = original.call(player, slot);
    if (slot == 36 && !result.isEmpty()) ForgeItems.craftedBy(result, player);
    return result;
  }
}
