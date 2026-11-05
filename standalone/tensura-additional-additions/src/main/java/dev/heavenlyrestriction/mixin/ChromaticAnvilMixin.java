package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.chromatic.ChromaticOrb;
import java.util.List;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class ChromaticAnvilMixin {
  @Inject(method = "createResult", at = @At("RETURN"))
  private void chromatic$preserve(CallbackInfo ci) {
    var menu = (AnvilMenu) (Object) this;
    ChromaticOrb.preserve(
        menu.getSlot(2).getItem(),
        List.of(menu.getSlot(0).getItem(), menu.getSlot(1).getItem()),
        true);
  }
}
