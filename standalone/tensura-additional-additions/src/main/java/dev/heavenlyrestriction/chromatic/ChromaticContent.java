package dev.heavenlyrestriction.chromatic;

import com.mojang.serialization.Codec;
import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.highrealm.HighRealmElites;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;

public final class ChromaticContent {
  private static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(Registries.ITEM, HeavenlyRestriction.ID);
  private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
      DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, HeavenlyRestriction.ID);

  public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> INFUSED =
      COMPONENTS.register(
          "chromatic_infused",
          () ->
              DataComponentType.<Boolean>builder()
                  .persistent(Codec.BOOL)
                  .networkSynchronized(ByteBufCodecs.BOOL)
                  .build());
  public static final DeferredHolder<Item, Item> BULWARK_SHARD =
      material("bulwark_shard", Rarity.UNCOMMON);
  public static final DeferredHolder<Item, Item> SENTINEL_SHARD =
      material("sentinel_shard", Rarity.UNCOMMON);
  public static final DeferredHolder<Item, Item> SKIRMISHER_SHARD =
      material("skirmisher_shard", Rarity.UNCOMMON);
  public static final DeferredHolder<Item, Item> STARLESS_CORE =
      material("starless_core", Rarity.RARE);
  public static final DeferredHolder<Item, Item> ORB =
      ITEMS.register(
          "chromatic_orb",
          () ->
              new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)) {
                @Override
                public void appendHoverText(
                    ItemStack stack,
                    TooltipContext context,
                    List<Component> lines,
                    TooltipFlag flag) {
                  lines.add(
                      Component.translatable("item.heavenlyrestriction.chromatic_orb.hint")
                          .withStyle(ChatFormatting.GRAY));
                  lines.add(
                      Component.translatable("item.heavenlyrestriction.chromatic_orb.exclusions")
                          .withStyle(ChatFormatting.DARK_GRAY));
                }
              });

  private static DeferredHolder<Item, Item> material(String name, Rarity rarity) {
    return ITEMS.register(name, () -> new Item(new Item.Properties().rarity(rarity)));
  }

  public static Item shard(HighRealmElites.Kind kind) {
    return switch (kind) {
      case BULWARK -> BULWARK_SHARD.get();
      case SENTINEL -> SENTINEL_SHARD.get();
      case SKIRMISHER -> SKIRMISHER_SHARD.get();
    };
  }

  public static void register(IEventBus bus) {
    COMPONENTS.register(bus);
    ITEMS.register(bus);
    bus.addListener(
        (net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
          if (e.getTabKey().equals(CreativeModeTabs.INGREDIENTS))
            for (var item :
                List.of(BULWARK_SHARD, SENTINEL_SHARD, SKIRMISHER_SHARD, STARLESS_CORE, ORB))
              e.accept(item.get());
        });
    NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, ChromaticOrb::anvil);
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e) -> {
          if (ChromaticOrb.infused(e.getItemStack()))
            e.getToolTip()
                .add(
                    Component.translatable("item.heavenlyrestriction.chromatic_infused")
                        .withStyle(ChatFormatting.AQUA));
        });
  }

  private ChromaticContent() {}
}
