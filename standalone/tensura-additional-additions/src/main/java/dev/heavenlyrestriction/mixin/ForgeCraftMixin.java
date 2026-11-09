package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.forge.ForgeItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Smithing also calls this hook, even when no crafting-table event is fired. */
@Mixin(ItemStack.class)
public abstract class ForgeCraftMixin {
  @Inject(method = "onCraftedBy", at = @At("RETURN"))
  private void forge$crafted(Level level, Player player, int count, CallbackInfo ci) {
    if (!level.isClientSide()) ForgeItems.craftedBy((ItemStack) (Object) this, player);
  }
}
