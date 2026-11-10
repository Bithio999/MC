package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.forge.ForgeItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Credit the actual take, never a recipe preview. Quick-transfer is credited by its wrapper. */
@Mixin(targets = "io.github.manasmods.tensura.menu.SmithingBenchMenu$1", remap = false)
public abstract class ForgeBenchTakeMixin {
  @Inject(method = "onTake", at = @At("HEAD"))
  private void forge$taken(Player player, ItemStack stack, CallbackInfo ci) {
    ForgeItems.craftedBy(stack, player);
  }
}
