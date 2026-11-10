package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.forge.ForgeSynthesis;
import io.github.manasmods.tensura.data.TensuraTags;
import io.github.manasmods.tensura.enchantment.EngravingHelper;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops new curse rolls at their native source, including deployed and worn equipment. */
@Mixin(value = EngravingHelper.class, remap = false)
public abstract class ForgeCurseMixin {
  @Inject(method = "applyCurseEngraving", at = @At("HEAD"), cancellable = true)
  private static void forge$curseRoll(
      LivingEntity owner, ItemStack item, int level, CallbackInfo ci) {
    if (ForgeSynthesis.active(owner).isPresent()) ci.cancel();
  }

  @Inject(method = "increaseEngraving", at = @At("HEAD"), cancellable = true)
  private static void forge$curseIncrease(
      LivingEntity owner,
      ItemStack item,
      Holder<Enchantment> engraving,
      int level,
      CallbackInfo ci) {
    if (engraving == null || engraving.is(TensuraTags.Enchantments.TSUKUMOGAMI)) return;
    if ((engraving.is(EnchantmentTags.CURSE)
            || EngravingHelper.CONFIG.curseEngraving.contains(engraving.getRegisteredName()))
        && ForgeSynthesis.active(owner).isPresent()) ci.cancel();
  }
}
