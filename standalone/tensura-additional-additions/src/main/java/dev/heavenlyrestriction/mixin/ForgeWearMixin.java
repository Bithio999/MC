package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.forge.ForgeItems;
import io.github.manasmods.tensura.registry.item.misc.TensuraDataComponents;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both ordinary item wear and Tensura's independent EP-durability reserve are protected. */
@Mixin(ItemStack.class)
public abstract class ForgeWearMixin {
  @Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true)
  private void forge$masterpiece(CallbackInfoReturnable<Boolean> out) {
    if (!out.getReturnValue() && ForgeItems.masterpiece((ItemStack) (Object) this))
      out.setReturnValue(true);
  }

  @WrapMethod(
      method =
          "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V")
  private void forge$wear(
      int amount,
      ServerLevel level,
      LivingEntity owner,
      Consumer<Item> broken,
      Operation<Void> original) {
    original.call(ForgeItems.wear((ItemStack) (Object) this, amount), level, owner, broken);
  }

  @WrapMethod(
      method =
          "set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;")
  private <T> T forge$epWear(DataComponentType<T> type, T value, Operation<T> original) {
    var self = (ItemStack) (Object) this;
    if (type == TensuraDataComponents.EP_DURABILITY.get()
        && value instanceof Double next
        && ForgeItems.forged(self)) {
      double old = self.getOrDefault(TensuraDataComponents.EP_DURABILITY.get(), next);
      value = (T) Double.valueOf(ForgeItems.spiritualWear(self, old, next));
    }
    return original.call(type, value);
  }
}
