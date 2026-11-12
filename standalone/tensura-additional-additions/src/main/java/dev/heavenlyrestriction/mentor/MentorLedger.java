package dev.heavenlyrestriction.mentor;

import dev.heavenlyrestriction.RestrictionData;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-side redemption authority, mirrored on the traveling character. Never reset by race. */
public final class MentorLedger extends SavedData {
  private static final String MARKER = "MentorLesson";
  private final Map<UUID, String> redeemed = new HashMap<>();

  public static MentorLedger get(MinecraftServer server) {
    return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(
            new Factory<>(MentorLedger::new, MentorLedger::load), "heavenlyrestriction_mentor");
  }

  public static MentorLedger load(CompoundTag t, HolderLookup.Provider lookup) {
    var data = new MentorLedger();
    for (var raw : t.getList("lessons", Tag.TAG_COMPOUND)) {
      var row = (CompoundTag) raw;
      if (row.hasUUID("player")) data.redeemed.put(row.getUUID("player"), row.getString("skill"));
    }
    return data;
  }

  public String lesson(UUID player) {
    return redeemed.getOrDefault(player, "");
  }

  @Override
  public CompoundTag save(CompoundTag t, HolderLookup.Provider lookup) {
    var rows = new ListTag();
    redeemed.forEach(
        (id, skill) -> {
          var row = new CompoundTag();
          row.putUUID("player", id);
          row.putString("skill", skill);
          rows.add(row);
        });
    t.put("lessons", rows);
    return t;
  }

  public static String reconcile(ServerPlayer p) {
    var data = get(p.server);
    var character = RestrictionData.of(p);
    var carried = character.getString(MARKER);
    var stored = data.redeemed.get(p.getUUID());
    if (stored != null) {
      character.putString(MARKER, stored);
      return stored;
    }
    if (!carried.isEmpty()) {
      data.redeemed.put(p.getUUID(), carried);
      data.setDirty();
    }
    return carried;
  }

  public static void record(ServerPlayer p, String skill) {
    var data = get(p.server);
    data.redeemed.put(p.getUUID(), skill);
    data.setDirty();
    RestrictionData.of(p).putString(MARKER, skill);
  }
}
