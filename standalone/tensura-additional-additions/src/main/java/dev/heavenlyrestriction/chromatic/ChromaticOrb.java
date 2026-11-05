package dev.heavenlyrestriction.chromatic;

import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.tensura.enchantment.EngravingHelper;
import io.github.manasmods.tensura.registry.item.misc.TensuraDataComponents;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * A single permanent enhancement; native engraving effects read the upgraded enchantment levels.
 */
public final class ChromaticOrb {
  public static final TagKey<Enchantment> ELIGIBLE =
      TagKey.create(Registries.ENCHANTMENT, HeavenlyRestriction.id("chromatic_eligible"));
  public static final TagKey<Enchantment> EXCLUDED =
      TagKey.create(Registries.ENCHANTMENT, HeavenlyRestriction.id("chromatic_excluded"));

  public static boolean infused(ItemStack stack) {
    return stack.getOrDefault(ChromaticContent.INFUSED.get(), false);
  }

  public static boolean eligible(Holder<Enchantment> enchantment) {
    if (!enchantment.is(ELIGIBLE)
        || enchantment.is(EXCLUDED)
        || enchantment.is(EnchantmentTags.CURSE)) return false;
    var key = enchantment.unwrapKey().orElse(null);
    if (key == null
        || protectedName(key.location().getPath())
        || EngravingHelper.CONFIG.curseEngraving.contains(key.location().toString())) return false;
    return enchantment.tags().noneMatch(tag -> protectedName(tag.location().getPath()));
  }

  private static boolean protectedName(String path) {
    return path.contains("bless")
        || path.equals("curse")
        || path.equals("curses")
        || path.startsWith("curse_")
        || path.endsWith("_curse")
        || path.endsWith("/curses");
  }

  private static boolean equipment(ItemStack stack) {
    return stack.getCount() == 1
        && !stack.has(DataComponents.STORED_ENCHANTMENTS)
        && (stack.isDamageableItem()
            || Equipable.get(stack) != null
            || stack.has(TensuraDataComponents.MAX_EP.get()));
  }

  public static ItemStack upgrade(ItemStack equipment) {
    if (!equipment(equipment) || infused(equipment)) return ItemStack.EMPTY;
    var enchantments = equipment.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    var selected = enchantments.entrySet().stream().filter(e -> eligible(e.getKey())).toList();
    // 255 is the serialized enchantment-level limit, not an engraving's ordinary maximum.
    if (selected.isEmpty() || selected.stream().anyMatch(e -> e.getIntValue() >= 255))
      return ItemStack.EMPTY;
    var result = equipment.copy();
    var raised = new ItemEnchantments.Mutable(enchantments);
    for (var entry : selected) raised.set(entry.getKey(), entry.getIntValue() + 1);
    result.set(DataComponents.ENCHANTMENTS, raised.toImmutable());
    result.set(ChromaticContent.INFUSED.get(), true);
    return result;
  }

  public static void anvil(AnvilUpdateEvent event) {
    if (!event.getRight().is(ChromaticContent.ORB.get())) return;
    var result = upgrade(event.getLeft());
    if (result.isEmpty()) {
      event.setCanceled(true);
      return;
    }
    if (event.getName() != null) {
      if (event.getName().isBlank()) result.remove(DataComponents.CUSTOM_NAME);
      else if (!event.getName().equals(event.getLeft().getHoverName().getString()))
        result.set(DataComponents.CUSTOM_NAME, Component.literal(event.getName()));
    }
    event.setOutput(result);
    event.setMaterialCost(1);
    event.setCost(1);
  }

  /** Keep the one-use history when either ingredient survives as repaired equipment. */
  public static void preserve(ItemStack result, List<ItemStack> inputs, boolean engravings) {
    if (result.isEmpty() || inputs.stream().noneMatch(ChromaticOrb::infused)) return;
    result.set(ChromaticContent.INFUSED.get(), true);
    if (!engravings) return;
    var current = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    var retained = new ItemEnchantments.Mutable(current);
    for (var source : inputs) {
      if (!infused(source)) continue;
      for (var entry :
          source.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet())
        if (eligible(entry.getKey()) && current.getLevel(entry.getKey()) > 0)
          retained.set(
              entry.getKey(), Math.max(retained.getLevel(entry.getKey()), entry.getIntValue()));
    }
    result.set(DataComponents.ENCHANTMENTS, retained.toImmutable());
  }

  private ChromaticOrb() {}
}
