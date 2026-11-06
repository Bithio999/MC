package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.vesper.Afterwind;
import dev.heavenlyrestriction.vesper.VesperItem;
import io.github.manasmods.tensura.registry.item.misc.TensuraDataComponents;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Keep native names, engraving levels and attributes; put live effects below their engraving. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class VesperTooltip {
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void tooltip(ItemTooltipEvent event) {
    var stack = event.getItemStack();
    if (!(stack.getItem() instanceof VesperItem)) return;
    var lines = event.getToolTip();
    var ep = stack.get(TensuraDataComponents.EP.get());
    var durability = stack.get(TensuraDataComponents.EP_DURABILITY.get());
    for (int n = 0; n < lines.size(); n++) {
      var line = lines.get(n);
      if (ep != null && durability != null && key(line, "tooltip.tensura.gear_durability_EP"))
        lines.set(
            n,
            Component.translatable(
                    "tooltip.tensura.gear_durability_EP", number(durability), number(ep))
                .withStyle(line.getStyle()));
    }
    var registry = event.getContext().registries();
    if (registry == null) return;
    var holder = registry.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Afterwind.ENGRAVING);
    int level = EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    if (level < 1) return;
    for (int n = 0; n < lines.size(); n++) {
      if (!key(lines.get(n), "enchantment.heavenlyrestriction.afterwind")) continue;
      lines.addAll(
          n + 1,
          List.of(
              detail("mark", number(Afterwind.MARK_TICKS / 20d)),
              detail("damage", number(Afterwind.coefficient(level) * 100)),
              detail(
                  "timing",
                  number(Afterwind.DELAY_TICKS / 20d),
                  number(Afterwind.COOLDOWN_TICKS / 20d),
                  number(Afterwind.RANGE))));
      return;
    }
  }

  private static boolean key(Component line, String key) {
    return line.getContents() instanceof TranslatableContents text && text.getKey().equals(key);
  }

  private static String number(double value) {
    return NumberFormat.getNumberInstance(Locale.ROOT).format(value);
  }

  private static Component detail(String key, Object... arguments) {
    return Component.literal("  ")
        .append(
            Component.translatable("item.heavenlyrestriction.vesper.afterwind." + key, arguments))
        .withStyle(ChatFormatting.GRAY);
  }

  private VesperTooltip() {}
}
