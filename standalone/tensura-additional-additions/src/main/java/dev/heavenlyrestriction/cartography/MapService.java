package dev.heavenlyrestriction.cartography;

import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Native, verified structure starts; bounded incremental discovery with no synchronous chunk waits.
 */
public final class MapService {
  public static final int SEARCH_RANGE = 100000, SEARCH_RADIUS = SEARCH_RANGE / 16;
  private static int generationBudget = 2;
  private static final Map<UUID, Session> SESSIONS = new HashMap<>();

  public static final class Session {
    public final UUID token = UUID.randomUUID();
    public final String dimension, originDimension;
    public long expiry, nextRequest, nextLocate, terrainTick;
    public int terrainRequests;
    public volatile int cx, cz, radius = 512;
    public volatile boolean terrainClosed;
    public Search search;
    public MapAutoStructures.View survey;
    public MapTravel.Pending travel;
    private ListTag catalog;
    public String message = "";
    public MapData.Marker found;
    public final Set<String> terrain = new HashSet<>();
    public final Map<String, MapTerrain.Pending> details = new LinkedHashMap<>();

    Session(ServerPlayer p) {
      this(p, p.level().dimension().location().toString());
    }

    Session(ServerPlayer p, String dimension) {
      this.dimension = dimension;
      originDimension = p.level().dimension().location().toString();
      cx = p.getBlockX();
      cz = p.getBlockZ();
      expiry = p.serverLevel().getGameTime() + 12000;
    }
  }

  public static final class Search extends MapStructureSearch {
    Search(ServerLevel level, Holder<Structure> type, int x, int z) {
      super(level, type, x, z);
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

  public static ServerLevel level(ServerPlayer p, Session s) {
    var id = ResourceLocation.parse(s.dimension);
    var level =
        p.server.getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, id));
    return java.util.Objects.requireNonNull(level, "Viewed map dimension is no longer available");
  }

  public static int plane(ServerPlayer p, ServerLevel level) {
    return level.dimensionType().hasCeiling()
        ? (level == p.serverLevel() ? Math.floorDiv(p.getBlockY(), 16) * 16 : 64)
        : Integer.MAX_VALUE;
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
    close(p);
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
    var old = SESSIONS.remove(p.getUUID());
    if (old != null) {
      old.terrainClosed = true;
      MapAutoStructures.cancel(old);
      MapTerrain.cancel(old);
      MapTravel.cancel(old);
      if (old.search != null) old.search.stop();
    }
  }

  public static boolean validPin(ServerPlayer p, CompoundTag t) {
    return validPin(p, t, p.serverLevel());
  }

  private static boolean validPin(ServerPlayer p, CompoundTag t, ServerLevel level) {
    String label = t.getString("name");
    return !label.isBlank()
        && label.length() <= 64
        && label.codePoints().noneMatch(c -> Character.isISOControl(c) || c == 167)
        && Math.abs((long) t.getInt("x")) <= 29999984
        && Math.abs((long) t.getInt("z")) <= 29999984
        && t.getInt("y") >= level.getMinBuildHeight()
        && t.getInt("y") < level.getMaxBuildHeight()
        && t.getInt("color") >= 0
        && t.getInt("color") < 8
        && t.getInt("icon") >= 0
        && t.getInt("icon") < 6
        && level
            .getWorldBorder()
            .isWithinBounds(new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z")));
  }

  public static boolean pin(ServerPlayer p, CompoundTag t) {
    return pin(p, t, p.serverLevel());
  }

  private static boolean pin(ServerPlayer p, CompoundTag t, ServerLevel level) {
    if (!owns(p) || !validPin(p, t, level)) return false;
    UUID id = t.hasUUID("uuid") ? t.getUUID("uuid") : UUID.randomUUID();
    var d = MapData.get(p.server);
    if (t.hasUUID("uuid") && d.pins(p.getUUID()).stream().noneMatch(a -> a.id().equals(id)))
      return false;
    return d.putPin(
        p.getUUID(),
        new MapData.Pin(
            id,
            level.dimension().location().toString(),
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
        || !p.server
            .levelKeys()
            .contains(
                net.minecraft.resources.ResourceKey.create(
                    Registries.DIMENSION, ResourceLocation.parse(s.dimension)))
        || !s.originDimension.equals(p.level().dimension().location().toString())
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
    if (!action.equals("terrain")) s.message = "";
    var data = MapData.get(p.server);
    if (action.equals("dimension")) {
      var id = ResourceLocation.tryParse(t.getString("dimension"));
      var target =
          id == null
              ? null
              : p.server.getLevel(
                  net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, id));
      if (target == null
          || Math.abs((long) t.getInt("x")) > MapView.LIMIT
          || Math.abs((long) t.getInt("z")) > MapView.LIMIT) {
        var rejected = snapshot(p, s);
        rejected.putBoolean("dimensionFailed", true);
        rejected.putString("message", "That dimension is no longer available.");
        MapNetwork.send(p, rejected);
        return;
      }
      close(p);
      var next = new Session(p, id.toString());
      next.cx = t.getInt("x");
      next.cz = t.getInt("z");
      SESSIONS.put(p.getUUID(), next);
      var reply = snapshot(p, next);
      reply.putUUID("previousSession", s.token);
      reply.putBoolean("dimensionChanged", true);
      MapNetwork.send(p, reply);
      return;
    }
    switch (action) {
      case "terrain" -> {
        if (s.terrainTick != now) {
          s.terrainTick = now;
          s.terrainRequests = 0;
        }
        if (++s.terrainRequests > 8) return;
        MapTerrain.request(p, s, t);
        return;
      }
      case "routes" -> {
        if (!MapTravel.options(p, s, t.getString("id"))) {
          s.message = "No usable coordinate-teleport skill is available for that structure.";
          MapNetwork.send(p, snapshot(p, s, false));
        }
        return;
      }
      case "travel" -> {
        if (!MapTravel.prepare(p, s, t.getString("id"), t.getString("skill")))
          s.message = "Could not prepare travel. Check your teleport skill and try again.";
      }
      case "view" -> {
        long x = t.getInt("x"), z = t.getInt("z");
        if (Math.abs(x) > 29999984 || Math.abs(z) > 29999984) return;
        s.cx = (int) x;
        s.cz = (int) z;
        s.radius = Math.clamp(t.getInt("radius"), 64, 65536);
        MapAutoStructures.view(p, s, t);
      }
      case "pin" ->
          s.message =
              pin(p, t, level(p, s))
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
          MapNetwork.send(p, snapshot(p, s, false));
          return;
        }
        s.nextLocate = now + 100;
        if (s.search != null) s.search.stop();
        s.search = null;
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
                        !m.predicted()
                            && m.dimension().equals(s.dimension)
                            && m.type().equals(id.toString())
                            && Math.hypot(m.x() - (double) s.cx, m.z() - (double) s.cz)
                                <= SEARCH_RANGE)
                .min(
                    Comparator.comparingDouble(
                        m -> Math.hypot(m.x() - (double) s.cx, m.z() - (double) s.cz)))
                .orElse(null);
        if (s.found != null) {
          s.search = null;
          s.message =
              "Found "
                  + name(id.toString())
                  + " in recorded structures. The map will center on the result.";
          break;
        }
        s.search = new Search(level(p, s), holder.get(), s.cx, s.cz);
        s.message =
            s.search.done
                ? (s.search.error.isEmpty()
                    ? "That structure has no world-generation placement in this dimension."
                    : s.search.error)
                : "Searching within 100,000 blocks of the map center…";
      }
      case "cancel" -> {
        if (s.search != null) s.search.stop();
        s.search = null;
        s.message = "Structure search cancelled.";
      }
      default -> {
        return;
      }
    }
    MapNetwork.send(p, snapshot(p, s, false));
  }

  public static void loaded(ChunkEvent.Load e) {
    if (e.getLevel() instanceof ServerLevel level && level.getServer().overworld() != null) {
      capture(level, e.getChunk());
    }
  }

  public static void capture(ServerLevel level, ChunkAccess chunk) {
    if (chunk == null || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.STRUCTURE_STARTS))
      return;
    var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
    var data = MapData.get(level.getServer());
    String dimension = level.dimension().location().toString();
    var actual = new ArrayList<MapData.Marker>();
    for (var entry : chunk.getAllStarts().entrySet()) {
      var start = entry.getValue();
      var id = registry.getKey(entry.getKey());
      if (id == null || start == null || !start.isValid()) continue;
      var box = start.getBoundingBox();
      String key = dimension + "/" + id + "/" + chunk.getPos().toLong();
      actual.add(
          new MapData.Marker(
              key,
              id.toString(),
              dimension,
              box.getCenter().getX(),
              box.minY(),
              box.getCenter().getZ()));
    }
    data.reconcileStarts(dimension, chunk.getPos(), actual);
    MapAutoStructures.confirm(data.world, dimension, chunk.getPos(), actual);
    for (var session : SESSIONS.values())
      if (session.found != null
          && session.found.dimension().equals(dimension)
          && chunk.getPos().equals(session.found.sourceChunk()))
        session.found = data.structures.get(session.found.key());
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
    MapTerrain.tick();
    var server = e.getServer();
    MapExploration.tick(server);
    for (var p : server.getPlayerList().getPlayers()) {
      var s = session(p);
      if (s == null) continue;
      if (!owns(p)
          || !p.server
              .levelKeys()
              .contains(
                  net.minecraft.resources.ResourceKey.create(
                      Registries.DIMENSION, ResourceLocation.parse(s.dimension)))
          || !s.originDimension.equals(p.level().dimension().location().toString())
          || p.level().getGameTime() > s.expiry) {
        close(p);
        var t = context(p);
        t.putBoolean("closed", true);
        MapNetwork.send(p, t);
        continue;
      }
      MapAutoStructures.advance(p, s);
      MapTerrain.advance(p, s);
      MapTravel.advance(p, s);
      if (session(p) != s) continue;
      if (s.search != null && !s.search.done) advance(p, s);
      if (p.tickCount % 10 == 0) {
        var players = new CompoundTag();
        players.putUUID("session", s.token);
        players.putBoolean("playersOnly", true);
        players.put("players", players(p));
        MapNetwork.send(p, players);
      }
      if (p.tickCount % 20 == 0) MapNetwork.send(p, snapshot(p, s, false));
    }
  }

  public static void advance(ServerPlayer p, Session s) {
    var job = s.search;
    if (job == null || job.done) return;
    var level = level(p, s);
    if (job.pending != null) {
      if (!job.pending.isDone()) {
        job.refreshTicket();
        return;
      }
      job.releaseTicket();
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
            if (s.found == null
                || Math.hypot(s.found.x() - (double) job.blockX, s.found.z() - (double) job.blockZ)
                    > SEARCH_RANGE) {
              s.found = null;
              job.pending = null;
              return;
            }
            job.stop();
            job.done = true;
            s.message = "Found " + name(id) + ". The map will center on the result.";
            return;
          }
        }
      } catch (RuntimeException ex) {
        s.message = "Search could not read that candidate; continuing.";
      }
      job.pending = null;
    }
    if (job.ready == null) job.ready = job.poll();
    if (job.ready != null) {
      if (generationBudget <= 0) return;
      generationBudget--;
      try {
        job.schedule();
      } catch (RuntimeException ex) {
        job.stop();
        job.done = true;
        s.message = "Search could not read that structure. Try another search.";
      }
      return;
    }
    if (job.exhausted()) {
      job.done = true;
      s.message =
          job.error.isEmpty()
              ? "No matching structure found within 100,000 blocks of the search center."
              : job.error;
      job.stop();
    }
  }

  public static CompoundTag snapshot(ServerPlayer p, Session s) {
    return snapshot(p, s, true);
  }

  private static CompoundTag snapshot(ServerPlayer p, Session s, boolean includeCatalog) {
    var t = context(p);
    var level = level(p, s);
    t.putString("playerDimension", s.originDimension);
    t.putString("dimension", s.dimension);
    t.putString("dimensionName", name(s.dimension));
    t.putInt("minY", level.getMinBuildHeight());
    t.putInt("maxY", level.getMaxBuildHeight());
    t.putInt("plane", plane(p, level));
    var dimensions = new ListTag();
    for (var other : p.server.getAllLevels()) {
      var dimension = new CompoundTag();
      String id = other.dimension().location().toString();
      dimension.putString("id", id);
      dimension.putString("name", name(id));
      dimensions.add(dimension);
    }
    t.put("dimensions", dimensions);
    t.putUUID("session", s.token);
    t.putInt("viewX", s.cx);
    t.putInt("viewZ", s.cz);
    t.putInt("viewRadius", s.radius);
    t.putInt("travelSkills", MapTravel.skills(p).size());
    t.putBoolean("preparingTravel", s.travel != null);
    t.put("players", players(p));
    t.putString("message", s.message);
    t.putBoolean("surveying", s.survey != null && s.survey.completed() < s.survey.total());
    if (s.survey != null) {
      t.putInt("surveyDone", s.survey.completed());
      t.putInt("surveyTotal", s.survey.total());
      t.putBoolean("surveyError", s.survey.error);
    }
    var d = MapData.get(p.server);
    var pins = new ListTag();
    d.pins(p.getUUID()).stream()
        .filter(pin -> pin.dimension().equals(s.dimension))
        .forEach(pin -> pins.add(pin.tag()));
    t.put("pins", pins);
    var marks = new ListTag();
    var visible =
        d
            .structuresIn(
                s.dimension, s.cx - s.radius, s.cz - s.radius, s.cx + s.radius, s.cz + s.radius)
            .stream()
            .sorted(
                Comparator.comparingDouble(
                    m -> Math.hypot(m.x() - (double) s.cx, m.z() - (double) s.cz)))
            .map(m -> new StructureMarkerClusters.Point<>(m, m.x(), m.z(), m.type()))
            .toList();
    for (var group : StructureMarkerClusters.group(visible, 2048)) {
      var m = group.representative();
      var tag = m.tag();
      tag.putString(
          "name",
          name(m.type()) + (group.count() > 1 ? " (+" + (group.count() - 1) + " nearby)" : ""));
      tag.putInt("count", group.count());
      marks.add(tag);
    }
    t.put("markers", marks);
    // The registry is immutable for the session. Keep it on the client rather than
    // rebuilding and transmitting the complete modpack catalog every second.
    if (includeCatalog && s.catalog == null) {
      var types = new ListTag();
      var registry = p.registryAccess().registryOrThrow(Registries.STRUCTURE);
      registry.entrySet().stream()
          .sorted(Comparator.comparing(a -> a.getKey().location().toString()))
          .forEach(
              entry -> {
                var tag = new CompoundTag();
                String id = entry.getKey().location().toString();
                tag.putString("id", id);
                tag.putString("name", name(id));
                types.add(tag);
              });
      s.catalog = types;
    }
    if (includeCatalog) t.put("types", s.catalog.copy());
    if (s.search != null) {
      t.putBoolean("searching", !s.search.done);
      t.putInt("checked", s.search.checked);
      t.putInt("candidates", s.search.candidates);
      t.putString("searchType", s.search.type.unwrapKey().orElseThrow().location().toString());
    }
    if (s.found != null) s.found = d.structures.get(s.found.key());
    if (s.found != null) {
      var tag = s.found.tag();
      tag.putString("name", name(s.found.type()));
      t.put("found", tag);
    }
    return t;
  }

  public static ListTag players(ServerPlayer viewer) {
    var list = new ListTag();
    for (var player : viewer.server.getPlayerList().getPlayers()) {
      var t = new CompoundTag();
      t.putUUID("uuid", player.getUUID());
      t.putString("id", "player/" + player.getUUID());
      t.putString("name", player.getGameProfile().getName());
      t.putString("dimension", player.level().dimension().location().toString());
      t.putString("dimensionName", name(t.getString("dimension")));
      t.putBoolean("player", true);
      t.putInt("x", player.getBlockX());
      t.putInt("y", player.getBlockY());
      t.putInt("z", player.getBlockZ());
      t.putDouble("px", player.getX());
      t.putDouble("pz", player.getZ());
      t.putFloat("yaw", player.getYRot());
      list.add(t);
    }
    return list;
  }

  public static void clear() {
    SESSIONS
        .values()
        .forEach(
            s -> {
              s.terrainClosed = true;
              MapAutoStructures.cancel(s);
              MapTerrain.cancel(s);
              MapTravel.cancel(s);
              if (s.search != null) s.search.stop();
            });
    MapAutoStructures.clear();
    MapTerrain.clear();
    MapExploration.clear();
    SESSIONS.clear();
  }

  private MapService() {}
}
