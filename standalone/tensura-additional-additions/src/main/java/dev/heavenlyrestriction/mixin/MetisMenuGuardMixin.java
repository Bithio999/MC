package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Defense in depth: a background callback cannot open a native container. */
@Mixin(ServerPlayer.class)
public abstract class MetisMenuGuardMixin {
  @Inject(
      method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;",
      at = @At("HEAD"),
      cancellable = true)
  private void additions$noAutomaticMenu(
      MenuProvider provider, CallbackInfoReturnable<OptionalInt> out) {
    if (SplitMindCasting.automatic((ServerPlayer) (Object) this))
      out.setReturnValue(OptionalInt.empty());
  }

  @Inject(
      method =
          "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;",
      at = @At("HEAD"),
      cancellable = true)
  private void additions$noAutomaticExtendedMenu(
      MenuProvider provider,
      Consumer<RegistryFriendlyByteBuf> data,
      CallbackInfoReturnable<OptionalInt> out) {
    if (SplitMindCasting.automatic((ServerPlayer) (Object) this))
      out.setReturnValue(OptionalInt.empty());
  }
}
