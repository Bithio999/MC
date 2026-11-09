package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.HeavenlyRestriction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ForgeContent {
  private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
      DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, HeavenlyRestriction.ID);
  private static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.ENTITY_TYPE, HeavenlyRestriction.ID);
  public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> WORK =
      COMPONENTS.register(
          "forge_work",
          () ->
              DataComponentType.<CompoundTag>builder()
                  .persistent(CompoundTag.CODEC)
                  .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                  .build());
  public static final DeferredHolder<EntityType<?>, EntityType<ArsenalBlade>> BLADE =
      ENTITIES.register(
          "living_arsenal",
          () ->
              EntityType.Builder.<ArsenalBlade>of(ArsenalBlade::new, MobCategory.MISC)
                  .sized(.35f, .35f)
                  .clientTrackingRange(8)
                  .updateInterval(1)
                  .noSave()
                  .noSummon()
                  .build("heavenlyrestriction:living_arsenal"));

  public static void register(IEventBus bus) {
    COMPONENTS.register(bus);
    ENTITIES.register(bus);
  }

  private ForgeContent() {}
}
