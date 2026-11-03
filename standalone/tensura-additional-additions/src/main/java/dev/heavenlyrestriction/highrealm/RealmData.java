package dev.heavenlyrestriction.highrealm;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/**
 * Terrain-specific gate locations and individual return points; no forced chunks or ticket
 * persistence.
 */
public final class RealmData extends SavedData {
  public record Gate(ResourceKey<Level> dimension, BlockPos core, Direction.Axis axis) {
    public String key() {
      return dimension.location() + "/" + core.asLong();
    }
  }

  public record Home(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch) {}

  public final Map<String, Gate> gates = new LinkedHashMap<>();
  public final Map<UUID, Home> homes = new HashMap<>();
  public BlockPos sanctuary;
  public int balanceVersion;
  private long seed = Long.MIN_VALUE;

  public static RealmData get(MinecraftServer s) {
    var d =
        s.overworld()
            .getDataStorage()
            .computeIfAbsent(
                new Factory<>(RealmData::new, RealmData::load), "heavenlyrestriction_high_realm");
    if (d.seed != s.overworld().getSeed()) {
      d.gates.clear();
      d.homes.clear();
      d.sanctuary = null;
      d.seed = s.overworld().getSeed();
      d.setDirty();
    }
    return d;
  }

  public void add(Gate gate) {
    gates.put(gate.key(), gate);
    setDirty();
  }

  public static RealmData load(CompoundTag t, HolderLookup.Provider lookup) {
    var d = new RealmData();
    d.seed = t.getLong("seed");
    d.balanceVersion = t.getInt("balanceVersion");
    if (t.contains("sanctuary")) d.sanctuary = BlockPos.of(t.getLong("sanctuary"));
    for (var raw : t.getList("gates", 10)) {
      try {
        var a = (CompoundTag) raw;
        var dim =
            ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.parse(a.getString("dimension")));
        var g =
            new Gate(
                dim,
                BlockPos.of(a.getLong("pos")),
                a.getBoolean("z") ? Direction.Axis.Z : Direction.Axis.X);
        d.gates.put(g.key(), g);
      } catch (RuntimeException ignored) {
      }
    }
    for (var raw : t.getList("homes", 10)) {
      try {
        var a = (CompoundTag) raw;
        d.homes.put(
            a.getUUID("player"),
            new Home(
                ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    ResourceLocation.parse(a.getString("dimension"))),
                new Vec3(a.getDouble("x"), a.getDouble("y"), a.getDouble("z")),
                a.getFloat("yaw"),
                a.getFloat("pitch")));
      } catch (RuntimeException ignored) {
      }
    }
    return d;
  }

  @Override
  public CompoundTag save(CompoundTag t, HolderLookup.Provider lookup) {
    t.putLong("seed", seed);
    t.putInt("balanceVersion", balanceVersion);
    if (sanctuary != null) t.putLong("sanctuary", sanctuary.asLong());
    var gs = new ListTag();
    for (var g : gates.values()) {
      var a = new CompoundTag();
      a.putString("dimension", g.dimension.location().toString());
      a.putLong("pos", g.core.asLong());
      a.putBoolean("z", g.axis == Direction.Axis.Z);
      gs.add(a);
    }
    t.put("gates", gs);
    var hs = new ListTag();
    homes.forEach(
        (id, h) -> {
          var a = new CompoundTag();
          a.putUUID("player", id);
          a.putString("dimension", h.dimension.location().toString());
          a.putDouble("x", h.pos.x);
          a.putDouble("y", h.pos.y);
          a.putDouble("z", h.pos.z);
          a.putFloat("yaw", h.yaw);
          a.putFloat("pitch", h.pitch);
          hs.add(a);
        });
    t.put("homes", hs);
    return t;
  }
}
