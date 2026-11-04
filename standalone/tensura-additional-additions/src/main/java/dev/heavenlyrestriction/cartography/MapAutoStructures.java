package dev.heavenlyrestriction.cartography;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.*;

/**
 * Native structure generation on disposable chunks: complete worldgen markers without terrain
 * writes.
 */
public final class MapAutoStructures {
  private static final AtomicLong EPOCH = new AtomicLong();
  private static final ThreadPoolExecutor WORKER =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(8),
          r -> {
            var t = new Thread(r, "MaraudersMap-AutoStructures");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());

  private record Key(UUID world, String dimension, SurveyTiles.Tile tile) {}

  private record Cached(boolean success, List<MapData.Marker> markers) {}

  private static int cachedMarkers;
  private static final LinkedHashMap<Key, Cached> CACHE = new LinkedHashMap<>(32, .75f, true);
  private static final Map<Key, Job> JOBS = new HashMap<>();

  public static final class View {
    final List<SurveyTiles.Tile> tiles;
    final Set<SurveyTiles.Tile> done = new HashSet<>();
    final int minX, minZ, maxX, maxZ;
    Key active;
    public boolean error;

    View(int minX, int minZ, int maxX, int maxZ) {
      this.minX = minX;
      this.minZ = minZ;
      this.maxX = maxX;
      this.maxZ = maxZ;
      tiles = SurveyTiles.plan(minX, minZ, maxX, maxZ);
    }

    public int total() {
      return tiles.size();
    }

    public int completed() {
      return done.size();
    }
  }

  private static final class Job {
    final Key key;
    final Set<UUID> watchers = new HashSet<>();
    volatile boolean cancelled;
    Future<?> future;

    Job(Key key) {
      this.key = key;
    }

    void cancel() {
      cancelled = true;
      if (future != null) future.cancel(true);
    }

    boolean alive() {
      return !cancelled && !Thread.currentThread().isInterrupted();
    }
  }

  public static void view(ServerPlayer p, MapService.Session s, CompoundTag input) {
    if (input.contains("survey") && !input.getBoolean("survey")) {
      cancel(s);
      return;
    }
    int minX = input.contains("minX") ? input.getInt("minX") : s.cx - s.radius;
    int minZ = input.contains("minZ") ? input.getInt("minZ") : s.cz - s.radius;
    int maxX = input.contains("maxX") ? input.getInt("maxX") : s.cx + s.radius;
    int maxZ = input.contains("maxZ") ? input.getInt("maxZ") : s.cz + s.radius;
    if (minX > maxX
        || minZ > maxZ
        || (long) maxX - minX > 131072
        || (long) maxZ - minZ > 131072
        || Math.abs((long) minX - s.cx) > s.radius
        || Math.abs((long) maxX - s.cx) > s.radius
        || Math.abs((long) minZ - s.cz) > s.radius
        || Math.abs((long) maxZ - s.cz) > s.radius) return;
    var old = s.survey;
    if (old != null && old.minX == minX && old.minZ == minZ && old.maxX == maxX && old.maxZ == maxZ)
      return;
    cancel(s);
    s.survey = new View(minX, minZ, maxX, maxZ);
  }

  public static void advance(ServerPlayer p, MapService.Session s) {
    var view = s.survey;
    if (view == null) return;
    UUID world = MapData.get(p.server).world;
    if (view.active != null) {
      var completed = CACHE.get(view.active);
      if (completed != null) {
        view.done.add(view.active.tile);
        view.error |= !completed.success();
        completed.markers().forEach(MapData.get(p.server)::add);
        detach(s);
      } else if (!JOBS.containsKey(view.active)) detach(s);
      else return;
    }
    for (var tile : view.tiles) {
      if (view.done.contains(tile)) continue;
      var key = new Key(world, s.dimension, tile);
      var cached = CACHE.get(key);
      if (cached != null) {
        view.done.add(tile);
        view.error |= !cached.success();
        cached.markers().forEach(MapData.get(p.server)::add);
        continue;
      }
      var job = JOBS.get(key);
      if (job == null) {
        if (JOBS.size() >= 8) return;
        job = submit(MapService.level(p, s), key);
        if (job == null) return;
      }
      job.watchers.add(s.token);
      view.active = key;
      return;
    }
  }

  private static Job submit(ServerLevel level, Key key) {
    var state = level.getChunkSource().getGeneratorState();
    var sets = List.copyOf(state.possibleStructureSets());
    // Initialize native placement maps before the worker sees immutable worldgen state.
    if (!sets.isEmpty() && !sets.getFirst().value().structures().isEmpty())
      state.getPlacementsForStructure(sets.getFirst().value().structures().getFirst().structure());
    var generator = level.getChunkSource().getGenerator();
    var height = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
    var registry = level.registryAccess();
    var biomes = registry.registryOrThrow(Registries.BIOME);
    var structures = registry.registryOrThrow(Registries.STRUCTURE);
    var manager = level.structureManager();
    var templates = level.getStructureManager();
    var pieces = StructurePieceSerializationContext.fromLevel(level);
    var storage = level.getChunkSource().chunkMap;
    var regionFolder =
        net.minecraft.world.level.dimension.DimensionType.getStorageFolder(
                level.dimension(),
                level
                    .getServer()
                    .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT))
            .resolve("region");
    long epoch = EPOCH.get(), seed = level.getSeed();
    boolean generate = manager.shouldGenerateStructures();
    double minX = level.getWorldBorder().getMinX(),
        maxX = level.getWorldBorder().getMaxX(),
        minZ = level.getWorldBorder().getMinZ(),
        maxZ = level.getWorldBorder().getMaxZ();
    var job = new Job(key);
    JOBS.put(key, job);
    try {
      WORKER.purge();
      job.future =
          WORKER.submit(
              () -> {
                var markers = new LinkedHashMap<String, MapData.Marker>();
                boolean success = true;
                try (var memo = SurveyNoiseCache.open()) {
                  var candidates = new HashSet<Long>();
                  var tile = key.tile;
                  int left = tile.minChunkX(),
                      top = tile.minChunkZ(),
                      right = left + tile.chunks() - 1,
                      bottom = top + tile.chunks() - 1;
                  for (var set : sets) {
                    if (!job.alive()) return;
                    var placement = set.value().placement();
                    if (placement instanceof RandomSpreadStructurePlacement spread) {
                      int size = spread.spacing();
                      for (int z = Math.floorDiv(top, size); z <= Math.floorDiv(bottom, size); z++)
                        for (int x = Math.floorDiv(left, size);
                            x <= Math.floorDiv(right, size);
                            x++) {
                          if (!job.alive()) return;
                          var pos =
                              spread.getPotentialStructureChunk(
                                  state.getLevelSeed(), x * size, z * size);
                          if (inside(pos, left, top, right, bottom)
                              && placement.isStructureChunk(state, pos.x, pos.z))
                            candidates.add(pos.toLong());
                        }
                    } else if (placement instanceof ConcentricRingsStructurePlacement rings) {
                      var positions =
                          state.getRingPositionsFor(rings); // worker-only native future wait
                      if (positions != null)
                        for (var pos : positions)
                          if (inside(pos, left, top, right, bottom)
                              && placement.isStructureChunk(state, pos.x, pos.z))
                            candidates.add(pos.toLong());
                    } else {
                      for (int z = top; z <= bottom; z++)
                        for (int x = left; x <= right; x++) {
                          if (!job.alive()) return;
                          if (placement.isStructureChunk(state, x, z))
                            candidates.add(ChunkPos.asLong(x, z));
                        }
                    }
                  }
                  var ordered =
                      candidates.stream()
                          .map(ChunkPos::new)
                          .sorted(
                              Comparator.comparingDouble(
                                  pos ->
                                      Math.hypot(
                                          pos.x - (left + right) / 2d,
                                          pos.z - (top + bottom) / 2d)))
                          .toList();
                  for (var pos : ordered) {
                    if (!job.alive() || epoch != EPOCH.get()) return;
                    int wx = pos.getMiddleBlockX(), wz = pos.getMiddleBlockZ();
                    if (Math.abs((long) wx) > MapView.LIMIT
                        || Math.abs((long) wz) > MapView.LIMIT
                        || wx < minX
                        || wx >= maxX
                        || wz < minZ
                        || wz >= maxZ) continue;
                    // Read saved starts first. Existing terrain must win over regenerated
                    // predictions.
                    var regionPath =
                        regionFolder.resolve(
                            "r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
                    var read =
                        java.nio.file.Files.isRegularFile(regionPath)
                            ? storage.read(pos)
                            : CompletableFuture.completedFuture(Optional.<CompoundTag>empty());
                    Optional<CompoundTag> saved = null;
                    while (saved == null && job.alive()) {
                      try {
                        saved = read.get(100, TimeUnit.MILLISECONDS);
                      } catch (TimeoutException ignored) {
                      }
                    }
                    if (!job.alive()) return;
                    if (saved != null && saved.isPresent()) {
                      var root = saved.get();
                      var status = ChunkStatus.byName(root.getString("Status"));
                      if (status != null && status.isOrAfter(ChunkStatus.STRUCTURE_STARTS)) {
                        var starts = root.getCompound("structures").getCompound("starts");
                        for (String name : starts.getAllKeys()) {
                          var start =
                              StructureStart.loadStaticStart(
                                  pieces, starts.getCompound(name), seed);
                          if (start != null && start.isValid())
                            add(
                                markers,
                                key.dimension,
                                structures.getKey(start.getStructure()).toString(),
                                pos,
                                start,
                                false);
                        }
                        continue;
                      }
                    }
                    if (!generate) continue;
                    // Exactly the native weighted set/variant/fallback algorithm, against an
                    // unsaved chunk.
                    var proto = new ProtoChunk(pos, UpgradeData.EMPTY, height, biomes, null);
                    generator.createStructures(registry, state, manager, proto, templates);
                    for (var entry : proto.getAllStarts().entrySet())
                      if (entry.getValue() != null && entry.getValue().isValid())
                        add(
                            markers,
                            key.dimension,
                            structures.getKey(entry.getKey()).toString(),
                            pos,
                            entry.getValue(),
                            true);
                  }
                } catch (InterruptedException ex) {
                  Thread.currentThread().interrupt();
                  return;
                } catch (Exception ex) {
                  org.slf4j.LoggerFactory.getLogger(MapAutoStructures.class)
                      .warn("Could not survey structures in {} at {}", key.dimension, key.tile, ex);
                  success = false;
                }
                if (!job.alive() || epoch != EPOCH.get()) return;
                boolean complete = success;
                var result = List.copyOf(markers.values());
                level
                    .getServer()
                    .execute(
                        () -> {
                          if (epoch != EPOCH.get() || JOBS.get(key) != job || job.cancelled) return;
                          JOBS.remove(key);
                          if (MapData.get(level.getServer()).world.equals(key.world)) {
                            if (complete)
                              MapData.get(level.getServer())
                                  .replaceSurveyTile(key.dimension, key.tile, result);
                            else result.forEach(MapData.get(level.getServer())::add);
                            var old = CACHE.put(key, new Cached(complete, result));
                            cachedMarkers +=
                                result.size() - (old == null ? 0 : old.markers().size());
                            while (CACHE.size() > 512 || cachedMarkers > 65536) {
                              var first = CACHE.keySet().iterator().next();
                              cachedMarkers -= CACHE.remove(first).markers().size();
                            }
                          }
                        });
              });
      return job;
    } catch (RejectedExecutionException ex) {
      JOBS.remove(key);
      return null;
    }
  }

  private static boolean inside(ChunkPos pos, int left, int top, int right, int bottom) {
    return pos.x >= left && pos.x <= right && pos.z >= top && pos.z <= bottom;
  }

  private static void add(
      Map<String, MapData.Marker> markers,
      String dimension,
      String type,
      ChunkPos pos,
      StructureStart start,
      boolean predicted) {
    var box = start.getBoundingBox();
    String id = dimension + "/" + type + "/" + pos.toLong();
    if (markers.size() >= 65536 && !markers.containsKey(id))
      throw new IllegalStateException("Survey tile marker limit exceeded");
    markers.put(
        id,
        new MapData.Marker(
            id,
            type,
            dimension,
            box.getCenter().getX(),
            box.minY(),
            box.getCenter().getZ(),
            predicted));
  }

  private static void detach(MapService.Session s) {
    var view = s.survey;
    if (view == null || view.active == null) return;
    var job = JOBS.get(view.active);
    if (job != null) {
      job.watchers.remove(s.token);
      if (job.watchers.isEmpty()) {
        JOBS.remove(view.active);
        job.cancel();
      }
    }
    view.active = null;
  }

  public static void cancel(MapService.Session s) {
    if (s.survey != null && s.survey.error) {
      var it = CACHE.entrySet().iterator();
      while (it.hasNext()) {
        var e = it.next();
        if (!e.getValue().success()
            && e.getKey().dimension.equals(s.dimension)
            && s.survey.tiles.contains(e.getKey().tile)) {
          cachedMarkers -= e.getValue().markers().size();
          it.remove();
        }
      }
    }
    detach(s);
    s.survey = null;
  }

  public static void clear() {
    EPOCH.incrementAndGet();
    JOBS.values().forEach(Job::cancel);
    JOBS.clear();
    CACHE.clear();
    cachedMarkers = 0;
    WORKER.getQueue().clear();
  }

  private MapAutoStructures() {}
}
