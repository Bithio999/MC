package dev.heavenlyrestriction.forge;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** One active masterpiece per smith, including equipment outside loaded inventories. */
public final class ForgeLedger extends SavedData {
  final Map<UUID, UUID> works = new HashMap<>();

  public static ForgeLedger get(MinecraftServer server) {
    return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(
            new Factory<>(ForgeLedger::new, ForgeLedger::load), "heavenlyrestriction_forge");
  }

  public static ForgeLedger load(CompoundTag tag, HolderLookup.Provider lookup) {
    var ledger = new ForgeLedger();
    for (String key : tag.getAllKeys())
      try {
        ledger.works.put(UUID.fromString(key), tag.getUUID(key));
      } catch (IllegalArgumentException ignored) {
      }
    return ledger;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
    works.forEach((owner, work) -> tag.putUUID(owner.toString(), work));
    return tag;
  }

  public void designate(UUID owner, UUID work) {
    works.put(owner, work);
    setDirty();
  }

  public CompoundTag snapshot() {
    return save(new CompoundTag(), null);
  }
}
