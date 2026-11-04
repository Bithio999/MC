package dev.heavenlyrestriction.cartography;

import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Native, verified structure starts; bounded incremental discovery with no synchronous chunk waits.
 */
public final class MapService {
  public static final int SEARCH_RADIUS = 128, SCAN_BUDGET = 512, MAX_CANDIDATES = 128;
  private static int generationBudget = 2;
  private static final Map<UUID, Session> SESSIONS = new HashMap<>();

  public static final class Session {
    public final UUID token = UUID.randomUUID();
    public final String dimension;
    public long expiry, nextRequest, nextLocate;
    public int cx, cz, radius = 512;
    public Search search;
    public String message = "";
    public MapData.Marker found;

    Session(ServerPlayer p) {
      dimension = p.level().dimension().location().toString();
      cx = p.getBlockX();
      cz = p.getBlockZ();
      expiry = p.serverLevel().getGameTime() + 12000;
    }
  }

  public static final class Search {
    public final Holder<Structure> type;
    public final List<StructurePlacement> placements;
    public final int x, z;
    public int ring, edge, checked, candidates;
    public CompletableFuture<ChunkResult<ChunkAccess>> pending;
    public boolean done;
    public ChunkPos ready;

    Search(ServerPlayer p, Holder<Structure> type, int x, int z) {
      this.type = type;
      this.x = x >> 4;
      this.z = z >> 4;
      placements =
          p.serverLevel().getChunkSource().getGeneratorState().getPlacementsForStructure(type);
      if (placements.isEmpty()) done = true;
    }

    private ChunkPos next() {
      if (ring > SEARCH_RADIUS) return null;
      if (ring == 0) {
        ring = 1;
        return new ChunkPos(x, z);
      }
      int side = ring * 2;
      int segment = edge / side, offset = edge % side;
      int dx =
          switch (segment) {
            case 0 -> -ring + offset;
            case 1 -> ring;
            case 2 -> ring - offset;
            default -> -ring;
          };
      int dz =
          switch (segment) {
            case 0 -> -ring;
            case 1 -> -ring + offset;
            case 2 -> ring;
            default -> ring - offset;
          };
      if (++edge == side * 4) {
        edge = 0;
        ring++;
      }
      return new ChunkPos(x + dx, z + dz);
    }
  }

  public static boolean owns(ServerPlayer p) {
    return p.isAlive()
        && !p.isSpectator()
        && SkillAPI.getSkillsFrom(p)
            .getSkill(HeavenlyRestriction.MARAUDERS_MAP.get())
            .filter(i -> i.getMastery() >= 0 && i.canInteractSkill(p) && i.canActivateSkill(p, 0))
            .isPresent();
  }

  public static Session session(ServerPlayer p) {
    return SESSIONS.get(p.getUUID());
  }

  public static String name(String id) {
    var key = ResourceLocation.tryParse(id);
    String value = key == null ? id : key.getPath();
    StringBuilder out = new StringBuilder();
    for (String word : value.replace('/', ' ').replace('_', ' ').split(" "))
      if (!word.isEmpty()) {
        if (!out.isEmpty()) out.append(' ');
        out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
    return out.toString();
  }

  public static CompoundTag context(ServerPlayer p) {
    var t = new CompoundTag();
    t.putUUID("world", MapData.get(p.server).world);
    t.putString("dimension", p.level().dimension().location().toString());
    t.putString("dimensionName", name(p.level().dimension().location().toString()));
    return t;
  }

  public static void login(ServerPlayer p) {
    MapNetwork.send(p, context(p));
  }

  public static boolean open(ServerPlayer p) {
    if (!owns(p)) return false;
    var s = new Session(p);
    SESSIONS.put(p.getUUID(), s);
    captureNearby(p);
    var t = snapshot(p, s);
    t.putBoolean("open", true);
    MapNetwork.send(p, t);
    return true;
  }

  public static void changedDimension(ServerPlayer p) {
    close(p);
    var state = context(p);
    state.putBoolean("closed", true);
    MapNetwork.send(p, state);
  }

  public static void close(ServerPlayer p) {
    SESSIONS.remove(p.getUUID());
  }

  public static boolean validPin(ServerPlayer p, CompoundTag t) {
    String label = t.getString("name");
    return !label.isBlank()
        && label.length() <= 64
        && label.codePoints().noneMatch(c -> Character.isISOControl(c) || c == 167)
        && Math.abs((long) t.getInt("x")) <= 29999984
        && Math.abs((long) t.getInt("z")) <= 29999984
        && t.getInt("y") >= p.level().getMinBuildHeight()
        && t.getInt("y") < p.level().getMaxBuildHeight()
        && t.getInt("color") >= 0
        && t.getInt("color") < 8
        && t.getInt("icon") >= 0
        && t.getInt("icon") < 6
        && p.level()
            .getWorldBorder()
            .isWithinBounds(new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z")));
  }

  public static boolean pin(ServerPlayer p, CompoundTag t) {
    if (!owns(p) || !validPin(p, t)) return false;
    UUID id = t.hasUUID("uuid") ? t.getUUID("uuid") : UUID.randomUUID();
    var d = MapData.get(p.server);
    if (t.hasUUID("uuid") && d.pins(p.getUUID()).stream().noneMatch(a -> a.id().equals(id)))
      return false;
    return d.putPin(
        p.getUUID(),
        new MapData.Pin(
            id,
            p.level().dimension().location().toString(),
            t.getString("name").strip(),
            t.getInt("x"),
            t.getInt("y"),
            t.getInt("z"),
            t.getInt("color"),
            t.getInt("icon")));
  }

  public static void request(ServerPlayer p, UUID token, String action, CompoundTag t) {
    var s = session(p);
    if (s == null || !s.token.equals(token)) return;
    if (!owns(p)
        || !s.dimension.equals(p.level().dimension().location().toString())
        || p.level().getGameTime() > s.expiry) {
      close(p);
      var end = context(p);
      end.putBoolean("closed", true);
      MapNetwork.send(p, end);
      return;
    }
    long now = p.level().getGameTime();
    if (action.equals("close")) {
      close(p);
      return;
    }
    // View refreshes may be coalesced; deliberate pin/cancel actions must not be lost.
    if (action.equals("view")) {
      if (now < s.nextRequest) return;
      s.nextRequest = now + 6;
    }
    s.expiry = now + 12000;
    s.message = "";
    var data = MapData.get(p.server);
    switch (action) {
      case "view" -> {
        long x = t.getInt("x"), z = t.getInt("z");
        if (Math.abs(x) > 29999984 || Math.abs(z) > 29999984) return;
        s.cx = (int) x;
        s.cz = (int) z;
        s.radius = Math.clamp(t.getInt("radius"), 64, 8192);
      }
      case "pin" ->
          s.message =
              pin(p, t)
                  ? "Pin saved."
                  : "Could not save pin. Use a name, valid coordinates and at most 256 pins.";
      case "delete" -> {
        if (t.hasUUID("uuid"))
          s.message =
              data.removePin(p.getUUID(), t.getUUID("uuid"))
                  ? "Pin deleted."
                  : "Pin no longer exists.";
      }
      case "locate" -> {
        if (now < s.nextLocate) {
          s.message = "Please wait a moment before starting another search.";
          MapNetwork.send(p, snapshot(p, s));
          return;
        }
        s.nextLocate = now + 100;
        var id = ResourceLocation.tryParse(t.getString("type"));
        var holder =
            id == null
                ? Optional.<Holder.Reference<Structure>>empty()
                : p.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(id);
        if (holder.isEmpty()) return;
        s.found =
            data.structures.values().stream()
                .filter(
                    m ->
                        m.dimension().equals(s.dimension)
                            && m.type().equals(id.toString())
                            && Math.hypot(m.x() - (double) s.cx, m.z() - (double) s.cz) <= 2048)
                .min(
                    Comparator.comparingDouble(
                        m -> Math.hypot(m.x() - (double) s.cx, m.z() - (double) s.cz)))
                .orElse(null);
        if (s.found != null) {
          s.search = null;
          s.message =
              "Found "
                  + name(id.toString())
                  + " in recorded structures. Select Show result to center it.";
          break;
        }
        s.search = new Search(p, holder.get(), s.cx, s.cz);
        s.message =
            s.search.done
                ? "That structure has no world-generation placement in this dimension."
                : "Searching around the map center…";
      }
      case "cancel" -> {
        s.search = null;
        s.message = "Structure search cancelled.";
      }
      default -> {
        return;
      }
    }
    MapNetwork.send(p, snapshot(p, s));
  }

  public static void loaded(ChunkEvent.Load e) {
    if (e.getLevel() instanceof ServerLevel level && level.getServer().overworld() != null)
      capture(level, e.getChunk());
  }

  public static void capture(ServerLevel level, ChunkAccess chunk) {
    if (chunk.getAllStarts().isEmpty()) return;
    var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
    var d = MapData.get(level.getServer());
    for (var entry : chunk.getAllStarts().entrySet()) {
      var start = entry.getValue();
      var id = registry.getKey(entry.getKey());
      if (id == null || start == null || !start.isValid()) continue;
      var box = start.getBoundingBox();
      String dim = level.dimension().location().toString();
      String key = dim + "/" + id + "/" + chunk.getPos().toLong();
      d.add(
          new MapData.Marker(
              key, id.toString(), dim, box.getCenter().getX(), box.minY(), box.getCenter().getZ()));
    }
  }

  private static void captureNearby(ServerPlayer p) {
    for (int x = -6; x <= 6; x++)
      for (int z = -6; z <= 6; z++) {
        var chunk =
            p.serverLevel()
                .getChunkSource()
                .getChunkNow((p.getBlockX() >> 4) + x, (p.getBlockZ() >> 4) + z);
        if (chunk != null) capture(p.serverLevel(), chunk);
      }
  }

  public static void tick(ServerTickEvent.Post e) {
    generationBudget = 2;
    var server = e.getServer();
    for (var p : server.getPlayerList().getPlayers()) {
      var s = session(p);
      if (s == null) continue;
      if (!owns(p)
          || !s.dimension.equals(p.level().dimension().location().toString())
          || p.level().getGameTime() > s.expiry) {
        close(p);
        var t = context(p);
        t.putBoolean("closed", true);
        MapNetwork.send(p, t);
        continue;
      }
      if (s.search != null && !s.search.done) advance(p, s);
      if (p.tickCount % 20 == 0) MapNetwork.send(p, snapshot(p, s));
    }
  }

  public static void advance(ServerPlayer p, Session s) {
    var job = s.search;
    if (job == null || job.done) return;
    var level = p.serverLevel();
    if (job.pending != null) {
      if (!job.pending.isDone()) return;
      try {
        var chunk = job.pending.getNow(null);
        if (chunk != null && chunk.isSuccess()) {
          var access = chunk.orElse(null);
          capture(level, access);
          var start = access.getStartForStructure(job.type.value());
          if (start != null && start.isValid()) {
            var id = job.type.unwrapKey().orElseThrow().location().toString();
            s.found =
                MapData.get(p.server).structures.values().stream()
                    .filter(
                        m ->
                            m.type().equals(id)
                                && m.dimension().equals(s.dimension)
                                && m.key().endsWith("/" + access.getPos().toLong()))
                    .findFirst()
                    .orElse(null);
            job.done = true;
            s.message = "Found " + name(id) + ". Select Show result to center it.";
            return;
          }
        }
      } catch (RuntimeException ex) {
        s.message = "Search could not read that candidate; continuing.";
      }
      job.pending = null;
    }
    if (job.ready != null) {
      if (generationBudget <= 0) return;
      generationBudget--;
      job.pending =
          ((dev.heavenlyrestriction.mixin.MapChunkAccess) level.getChunkSource())
              .additions$mapFuture(job.ready.x, job.ready.z, ChunkStatus.STRUCTURE_STARTS, true);
      job.ready = null;
      return;
    }
    var state = level.getChunkSource().getGeneratorState();
    for (int n = 0; n < SCAN_BUDGET; n++) {
      var pos = job.next();
      if (pos == null || job.candidates >= MAX_CANDIDATES) {
        job.done = true;
        s.message =
            "No matching structure found in this search. Move the map center and search again.";
        return;
      }
      job.checked++;
      if ((long) (pos.x - job.x) * (pos.x - job.x) + (long) (pos.z - job.z) * (pos.z - job.z)
          > SEARCH_RADIUS * SEARCH_RADIUS) continue;
      if (Math.abs((long) pos.getMinBlockX()) > 29999984
          || Math.abs((long) pos.getMinBlockZ()) > 29999984) continue;
      if (!level
          .getWorldBorder()
          .isWithinBounds(
              new BlockPos(
                  pos.getMinBlockX() + 8, level.getMinBuildHeight(), pos.getMinBlockZ() + 8)))
        continue;
      boolean candidate = false;
      for (var placement : job.placements)
        if (placement.isStructureChunk(state, pos.x, pos.z)) {
          candidate = true;
          break;
        }
      if (!candidate) continue;
      job.candidates++;
      job.ready = pos;
      return;
    }
  }

  public static CompoundTag snapshot(ServerPlayer p, Session s) {
    var t = context(p);
    t.putUUID("session", s.token);
    t.putString("message", s.message);
    var d = MapData.get(p.server);
    var pins = new ListTag();
    d.pins(p.getUUID()).stream()
        .filter(pin -> pin.dimension().equals(s.dimension))
        .forEach(pin -> pins.add(pin.tag()));
    t.put("pins", pins);
    var marks = new ListTag();
    d.structures.values().stream()
        .filter(
            m ->
                m.dimension().equals(s.dimension)
                    && Math.abs((long) m.x() - s.cx) <= s.radius
                    && Math.abs((long) m.z() - s.cz) <= s.radius)
        .sorted(
            Comparator.comparingDouble(
                m -> Math.hypot(m.x() - (double) s.cx, m.z() - (double) s.cz)))
        .limit(2048)
        .forEach(
            m -> {
              var tag = m.tag();
              tag.putString("name", name(m.type()));
              marks.add(tag);
            });
    t.put("markers", marks);
    var types = new ListTag();
    var registry = p.registryAccess().registryOrThrow(Registries.STRUCTURE);
    registry.entrySet().stream()
        .sorted(Comparator.comparing(a -> a.getKey().location().toString()))
        .limit(1024)
        .forEach(
            entry -> {
              var tag = new CompoundTag();
              String id = entry.getKey().location().toString();
              tag.putString("id", id);
              tag.putString("name", name(id));
              types.add(tag);
            });
    t.put("types", types);
    if (s.search != null) {
      t.putBoolean("searching", !s.search.done);
      t.putInt("checked", s.search.checked);
      t.putInt("candidates", s.search.candidates);
      t.putString("searchType", s.search.type.unwrapKey().orElseThrow().location().toString());
    }
    if (s.found != null) {
      var tag = s.found.tag();
      tag.putString("name", name(s.found.type()));
      t.put("found", tag);
    }
    return t;
  }

  public static void clear() {
    SESSIONS.clear();
  }

  private MapService() {}
}
