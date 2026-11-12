package dev.heavenlyrestriction.mentor;

import dev.architectury.registry.registries.*;
import dev.heavenlyrestriction.HeavenlyRestriction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.*;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class MentorContent {
  private static final DeferredRegister<EntityType<?>> TYPES =
      DeferredRegister.create(HeavenlyRestriction.ID, Registries.ENTITY_TYPE);
  private static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(HeavenlyRestriction.ID, Registries.ITEM);
  public static final RegistrySupplier<EntityType<MentorEntity>> MENTOR =
      TYPES.register(
          "elias",
          () ->
              EntityType.Builder.<MentorEntity>of(MentorEntity::new, MobCategory.MISC)
                  .sized(.6f, 1.8f)
                  .eyeHeight(1.62f)
                  .clientTrackingRange(10)
                  .build(HeavenlyRestriction.id("elias").toString()));
  public static final RegistrySupplier<Item> EGG =
      ITEMS.register(
          "elias_spawn_egg",
          () -> new DeferredSpawnEggItem(MENTOR, 0x273447, 0xd8bb7d, new Item.Properties()));

  public static void register(IEventBus bus, ModContainer container) {
    TYPES.register();
    ITEMS.register();
    bus.addListener(
        (EntityAttributeCreationEvent e) -> e.put(MENTOR.get(), MentorEntity.attributes().build()));
    bus.addListener(MentorNetwork::register);
    bus.addListener(
        (net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
          if (e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) e.accept(EGG.get());
        });
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) -> {
          if (e.getEntity() instanceof MentorEntity npc && !e.getLevel().isClientSide()) {
            var race = io.github.manasmods.manascore.race.api.RaceAPI.getRaceFrom(npc);
            if (race.getRace().isEmpty())
              race.setRace(
                  io.github.manasmods.tensura.registry.race.TensuraRaces.HUMAN.get(), false);
          }
        });
    container.registerConfig(
        ModConfig.Type.SERVER, MentorConfig.SPEC, "tensura-additions-mentor.toml");
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerLoggedInEvent e) -> {
          if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)
            MentorLedger.reconcile(p);
        });
    NeoForge.EVENT_BUS.addListener(
        net.neoforged.bus.api.EventPriority.LOWEST,
        (PlayerEvent.Clone e) -> {
          if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)
            MentorLedger.reconcile(p);
        });
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> {
          if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p
              && p.tickCount % 20 == 0) MentorLedger.reconcile(p);
        });
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerLoggedOutEvent e) -> MentorNetwork.close(e.getEntity()));
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerChangedDimensionEvent e) -> MentorNetwork.close(e.getEntity()));
  }

  private MentorContent() {}
}
