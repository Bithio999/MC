package dev.heavenlyrestriction.highrealm.boss;

import dev.architectury.registry.registries.*;
import dev.heavenlyrestriction.HeavenlyRestriction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** Registered independently from Myths; a native humanoid/otherworlder with a real encounter. */
public final class WayfarerContent {
  private static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(HeavenlyRestriction.ID, Registries.ENTITY_TYPE);
  private static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(HeavenlyRestriction.ID, Registries.ITEM);
  public static final RegistrySupplier<EntityType<KaelVeyrEntity>> KAEL =
      ENTITIES.register(
          "kael_veyr",
          () ->
              EntityType.Builder.<KaelVeyrEntity>of(KaelVeyrEntity::new, MobCategory.MONSTER)
                  .sized(.6f, 1.8f)
                  .eyeHeight(1.62f)
                  .clientTrackingRange(12)
                  .updateInterval(2)
                  .build(HeavenlyRestriction.id("kael_veyr").toString()));
  public static final RegistrySupplier<Item> EGG =
      ITEMS.register(
          "kael_veyr_spawn_egg",
          () -> new DeferredSpawnEggItem(KAEL, 0x172234, 0x86dce2, new Item.Properties()));

  public static void register(IEventBus bus) {
    ENTITIES.register();
    ITEMS.register();
    bus.addListener(
        (EntityAttributeCreationEvent e) -> e.put(KAEL.get(), KaelVeyrEntity.attributes().build()));
    bus.addListener(WayfarerSpawns::placements);
    bus.addListener(
        (net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
          if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) e.accept(EGG.get());
        });
    NeoForge.EVENT_BUS.addListener(WayfarerSpawns::finalizing);
    NeoForge.EVENT_BUS.addListener(
        net.neoforged.bus.api.EventPriority.LOWEST, WayfarerSpawns::joined);
  }

  private WayfarerContent() {}
}
