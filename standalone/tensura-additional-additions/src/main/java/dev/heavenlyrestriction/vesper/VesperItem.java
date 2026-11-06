package dev.heavenlyrestriction.vesper;

import dev.heavenlyrestriction.HeavenlyArsenal;
import io.github.manasmods.tensura.item.TensuraToolTiers;
import io.github.manasmods.tensura.item.weapon.TensuraSwordItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.SimpleTier;

/** Native sword attributes, slightly above Night Rose; no dependency on the boss addon. */
public final class VesperItem extends TensuraSwordItem {
  private static final Tier TIER =
      new SimpleTier(
          TensuraToolTiers.HIHIIROKANE.getIncorrectBlocksForDrops(),
          3960,
          TensuraToolTiers.HIHIIROKANE.getSpeed(),
          TensuraToolTiers.HIHIIROKANE.getAttackDamageBonus(),
          TensuraToolTiers.HIHIIROKANE.getEnchantmentValue(),
          TensuraToolTiers.HIHIIROKANE::getRepairIngredient);

  public VesperItem() {
    super(TIER, 28, -2.3f, 1, 0, 0, 0, new Item.Properties().fireResistant().rarity(Rarity.EPIC));
  }

  public static void engrave(ItemStack stack, RegistryAccess access) {
    var engraving =
        access.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Afterwind.ENGRAVING);
    if (EnchantmentHelper.getItemEnchantmentLevel(engraving, stack) < 1)
      stack.enchant(engraving, 1);
  }

  public static ItemStack create(RegistryAccess access) {
    var stack = new ItemStack(HeavenlyArsenal.VESPER.get());
    engrave(stack, access);
    return stack;
  }

  @Override
  public void inventoryTick(
      ItemStack stack, Level level, Entity owner, int slot, boolean selected) {
    super.inventoryTick(stack, level, owner, slot, selected);
    if (!level.isClientSide()) engrave(stack, level.registryAccess());
  }

  @Override
  public boolean isFoil(ItemStack stack) {
    return false;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
    super.appendHoverText(stack, context, lines, flag);
    lines.add(
        Component.translatable("item.heavenlyrestriction.vesper.desc")
            .withStyle(ChatFormatting.GRAY));
  }
}
