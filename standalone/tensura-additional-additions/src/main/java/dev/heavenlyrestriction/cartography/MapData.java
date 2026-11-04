package dev.heavenlyrestriction.cartography;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-specific structures and private pins. Terrain pixels never pass through the server. */
public final class MapData extends SavedData {
  public static final int MAX_PINS = 256, MAX_STRUCTURES = 65536;
  public UUID world = UUID.randomUUID();
  private long seed = Long.MIN_VALUE;
  public final LinkedHashMap<String, Marker> structures = new LinkedHashMap<>();
  private final Map<UUID, LinkedHashMap<UUID, Pin>> pins = new HashMap<>();

  public record Marker(String key, String type, String dimension, int x, int y, int z) {
    public CompoundTag tag() {
      var t = new CompoundTag();
      t.putString("id", key);
      t.putString("type", type);
      t.putString("dimension", dimension);
      t.putInt("x", x);
      t.putInt("y", y);
      t.putInt("z", z);
      return t;
    }

    static Marker load(CompoundTag t) {
      return new Marker(
          t.getString("id"),
          t.getString("type"),
          t.getString("dimension"),
          t.getInt("x"),
          t.getInt("y"),
          t.getInt("z"));
    }
  }

  public record Pin(
      UUID id, String dimension, String name, int x, int y, int z, int color, int icon) {
    public CompoundTag tag() {
      var t = new CompoundTag();
      t.putUUID("uuid", id);
      t.putString("id", id.toString());
      t.putString("dimension", dimension);
      t.putString("name", name);
      t.putInt("x", x);
      t.putInt("y", y);
      t.putInt("z", z);
      t.putInt("color", color);
      t.putInt("icon", icon);
      t.putBoolean("pin", true);
      return t;
    }

    static Pin load(CompoundTag t) {
      return new Pin(
          t.getUUID("uuid"),
          t.getString("dimension"),
          t.getString("name"),
          t.getInt("x"),
          t.getInt("y"),
          t.getInt("z"),
          t.getInt("color"),
          t.getInt("icon"));
    }
  }

  public static MapData get(MinecraftServer server) {
    var d =
        server
            .overworld()
            .getDataStorage()
            .computeIfAbsent(
                new Factory<>(MapData::new, MapData::load), "heavenlyrestriction_marauders_map");
    d.ensureSeed(server.overworld().getSeed());
    return d;
  }

  public void ensureSeed(long next) {
    if (seed == next) return;
    if (seed != Long.MIN_VALUE) {
      world = UUID.randomUUID();
      structures.clear();
      pins.clear();
    }
    seed = next;
    setDirty();
  }

  public List<Pin> pins(UUID owner) {
    return List.copyOf(pins.getOrDefault(owner, new LinkedHashMap<>()).values());
  }

  public boolean putPin(UUID owner, Pin pin) {
    var own = pins.computeIfAbsent(owner, k -> new LinkedHashMap<>());
    if (!own.containsKey(pin.id()) && own.size() >= MAX_PINS) return false;
    own.put(pin.id(), pin);
    setDirty();
    return true;
  }

  public boolean removePin(UUID owner, UUID id) {
    var own = pins.get(owner);
    if (own == null || own.remove(id) == null) return false;
    setDirty();
    return true;
  }

  public void add(Marker marker) {
    if (marker.equals(structures.get(marker.key()))) return;
    structures.put(marker.key(), marker);
    while (structures.size() > MAX_STRUCTURES)
      structures.remove(structures.keySet().iterator().next());
    setDirty();
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    tag.putUUID("world", world);
    tag.putLong("seed", seed);
    var marks = new ListTag();
    structures.values().forEach(m -> marks.add(m.tag()));
    tag.put("structures", marks);
    var owners = new ListTag();
    pins.forEach(
        (owner, list) -> {
          var o = new CompoundTag();
          o.putUUID("owner", owner);
          var rows = new ListTag();
          list.values().forEach(p -> rows.add(p.tag()));
          o.put("pins", rows);
          owners.add(o);
        });
    tag.put("owners", owners);
    return tag;
  }

  public static MapData load(CompoundTag tag, HolderLookup.Provider registries) {
    var d = new MapData();
    if (tag.hasUUID("world")) d.world = tag.getUUID("world");
    d.seed = tag.contains("seed") ? tag.getLong("seed") : Long.MIN_VALUE;
    for (var t : tag.getList("structures", Tag.TAG_COMPOUND)) {
      if (d.structures.size() >= MAX_STRUCTURES) break;
      var m = Marker.load((CompoundTag) t);
      if (!m.key().isEmpty()) d.structures.put(m.key(), m);
    }
    for (var t : tag.getList("owners", Tag.TAG_COMPOUND)) {
      var o = (CompoundTag) t;
      if (!o.hasUUID("owner")) continue;
      for (var p : o.getList("pins", Tag.TAG_COMPOUND)) {
        var value = (CompoundTag) p;
        if (value.hasUUID("uuid")) d.putPin(o.getUUID("owner"), Pin.load(value));
      }
    }
    d.setDirty(false);
    return d;
  }
}
