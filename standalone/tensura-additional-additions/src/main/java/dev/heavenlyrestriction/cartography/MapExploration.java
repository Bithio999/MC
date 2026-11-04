package dev.heavenlyrestriction.cartography;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;

/** Shared real explored surfaces. Reads only already-loaded chunks; disk I/O never blocks ticks. */
public final class MapExploration {
  private static final ThreadPoolExecutor IO =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(64),
          r -> {
            var t = new Thread(r, "MaraudersMap-SharedTerrain");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());

  private record Key(UUID world, String dimension, int plane, int x, int z) {}

  private record Cached(CompoundTag tile, long sampled) {}

  private static final Map<Key, Cached> CACHE = new LinkedHashMap<>(32, .75f, true);
  private static final Map<UUID, Integer> SCANS = new HashMap<>();
  private static final List<ChunkPos> OFFSETS = new ArrayList<>();
  private static long ticks, epoch;
  private static int playerCursor;

  static {
    for (int z = -8; z <= 8; z++) for (int x = -8; x <= 8; x++) OFFSETS.add(new ChunkPos(x, z));
    OFFSETS.sort(Comparator.comparingInt(p -> p.x * p.x + p.z * p.z));
  }

  public static String layer(String dimension, int plane) {
    return dimension + "/" + plane;
  }

  private static Path path(MinecraftServer server, Key key) {
    try {
      String hash =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(key.dimension.getBytes(StandardCharsets.UTF_8)))
              .substring(0, 16);
      return server
          .getWorldPath(LevelResource.ROOT)
          .resolve("data/heavenlyrestriction_map_terrain")
          .resolve(key.world.toString())
          .resolve(hash)
          .resolve(Integer.toString(key.plane))
          .resolve(Math.floorDiv(key.x, 32) + "_" + Math.floorDiv(key.z, 32))
          .resolve(key.x + "_" + key.z + ".dat");
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void put(Key key, CompoundTag tile, long time) {
    CACHE.put(key, new Cached(tile, time));
    while (CACHE.size() > 1024) CACHE.remove(CACHE.keySet().iterator().next());
  }

  public static void tick(MinecraftServer server) {
    ticks++;
    var players = server.getPlayerList().getPlayers();
    SCANS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    if (players.isEmpty() || IO.getQueue().remainingCapacity() < 4) return;
    // Two recorded chunks per tick globally, distributed fairly across all players, with or without
    // Map.
    for (int budget = 0; budget < 2; budget++) {
      var p = players.get(Math.floorMod(playerCursor++, players.size()));
      int scan = SCANS.getOrDefault(p.getUUID(), 0);
      for (int tries = 0; tries < 8; tries++) {
        var offset = OFFSETS.get(Math.floorMod(scan++, OFFSETS.size()));
        int radius = Math.min(8, server.getPlayerList().getViewDistance());
        if (Math.abs(offset.x) > radius || Math.abs(offset.z) > radius) continue;
        var chunk =
            p.serverLevel()
                .getChunkSource()
                .getChunkNow(p.chunkPosition().x + offset.x, p.chunkPosition().z + offset.z);
        if (chunk == null) continue;
        var data = MapData.get(server);
        int plane = MapService.plane(p, p.serverLevel());
        var key =
            new Key(
                data.world,
                p.level().dimension().location().toString(),
                plane,
                chunk.getPos().x,
                chunk.getPos().z);
        var old = CACHE.get(key);
        if (old != null && ticks - old.sampled < 1200) continue;
        record(p.serverLevel(), chunk, plane);
        break;
      }
      SCANS.put(p.getUUID(), scan);
    }
  }

  public static void record(ServerLevel level, LevelChunk chunk, int plane) {
    var server = level.getServer();
    var data = MapData.get(server);
    var key =
        new Key(
            data.world,
            level.dimension().location().toString(),
            plane,
            chunk.getPos().x,
            chunk.getPos().z);
    var tile = MapTerrain.detail(level, chunk, plane);
    int[] blocks = tile.getIntArray("blocks"),
        heights = tile.getIntArray("heights"),
        tints = tile.getIntArray("tints"),
        depths = tile.getIntArray("depths");
    long r = 0, g = 0, b = 0;
    var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
    for (int z = 0; z < 16; z++)
      for (int x = 0; x < 16; x++) {
        int at = z * 16 + x;
        var state = Block.stateById(blocks[at]);
        pos.set(chunk.getPos().getMinBlockX() + x, heights[at], chunk.getPos().getMinBlockZ() + z);
        int color = tints[at] != -1 ? tints[at] : state.getMapColor(level, pos).col;
        color =
            MapTerrain.shade(
                color,
                heights[at],
                heights[z * 16 + Math.max(0, x - 1)],
                heights[z * 16 + Math.min(15, x + 1)],
                heights[Math.max(0, z - 1) * 16 + x],
                heights[Math.min(15, z + 1) * 16 + x],
                depths[at],
                1);
        r += (color >> 16) & 255;
        g += (color >> 8) & 255;
        b += color & 255;
      }
    int average = 0xff000000 | ((int) (r / 256) << 16) | ((int) (g / 256) << 8) | (int) (b / 256);
    if (data.explored.put(layer(key.dimension, plane), key.x, key.z, average)) data.setDirty();
    var old = CACHE.get(key);
    boolean changed = old == null || !old.tile.equals(tile);
    put(key, tile, ticks);
    if (!changed) return;
    // Stable block-state palettes survive registry-ID changes when a modpack is updated.
    var saved = tile.copy();
    var palette = new ListTag();
    var lookup = new HashMap<Integer, Integer>();
    int[] indices = new int[256];
    for (int i = 0; i < 256; i++) {
      Integer index = lookup.get(blocks[i]);
      if (index == null) {
        index = palette.size();
        lookup.put(blocks[i], index);
        palette.add(NbtUtils.writeBlockState(Block.stateById(blocks[i])));
      }
      indices[i] = index;
    }
    saved.put("palette", palette);
    saved.putIntArray("blocks", indices);
    saved.putInt("schema", 1);
    Path target = path(server, key);
    try {
      IO.execute(
          () -> {
            try {
              Files.createDirectories(target.getParent());
              Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
              NbtIo.writeCompressed(saved, tmp);
              try {
                Files.move(
                    tmp,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
              } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
              }
            } catch (Exception ex) {
              org.slf4j.LoggerFactory.getLogger(MapExploration.class)
                  .warn("Could not save shared map terrain at {}", target, ex);
            }
          });
    } catch (RejectedExecutionException ignored) {
      CACHE.remove(key);
    }
  }

  public static boolean request(
      ServerPlayer p,
      MapService.Session s,
      ServerLevel level,
      int x,
      int z,
      int plane,
      String id,
      Consumer<CompoundTag> deliver) {
    var key = new Key(MapData.get(p.server).world, s.dimension, plane, x, z);
    var cached = CACHE.get(key);
    if (cached != null) {
      var shared = cached.tile.copy();
      shared.putBoolean("shared", true);
      deliver.accept(shared);
      return true;
    }
    Path target = path(p.server, key);
    // The overview is a cheap positive-existence index; no filesystem probe on the tick thread.
    if (MapData.get(p.server).explored.sample(layer(s.dimension, plane), x * 16, z * 16, 16) == 0)
      return false;
    long generation = epoch;
    try {
      IO.execute(
          () -> {
            CompoundTag tile = null;
            try {
              if (Files.isRegularFile(target)) {
                tile = NbtIo.readCompressed(target, NbtAccounter.create(1 << 20));
                var palette = tile.getList("palette", Tag.TAG_COMPOUND);
                int[] indices = tile.getIntArray("blocks");
                if (tile.getInt("schema") != 1
                    || tile.getInt("tx") != x
                    || tile.getInt("tz") != z
                    || !tile.getBoolean("detail")
                    || tile.getInt("step") != 1
                    || tile.getIntArray("heights").length != 256
                    || tile.getIntArray("tints").length != 256
                    || tile.getIntArray("depths").length != 256
                    || indices.length != 256
                    || palette.isEmpty()
                    || palette.size() > 256)
                  throw new java.io.IOException("Invalid shared terrain tile");
                var registry = level.registryAccess().lookupOrThrow(Registries.BLOCK);
                int[] states = new int[palette.size()];
                for (int i = 0; i < states.length; i++)
                  states[i] =
                      Block.getId(NbtUtils.readBlockState(registry, palette.getCompound(i)));
                for (int i = 0; i < indices.length; i++) {
                  if (indices[i] < 0 || indices[i] >= states.length)
                    throw new java.io.IOException("Invalid shared palette");
                  indices[i] = states[indices[i]];
                }
                tile.putIntArray("blocks", indices);
                tile.remove("palette");
                tile.remove("schema");
              }
            } catch (Exception ex) {
              tile = null;
            }
            var result = tile;
            p.server.execute(
                () -> {
                  if (generation != epoch
                      || MapService.session(p) != s
                      || s.terrainClosed
                      || !MapService.owns(p)) return;
                  if (result != null) {
                    put(key, result, ticks - 1200);
                    var shared = result.copy();
                    shared.putBoolean("shared", true);
                    deliver.accept(shared);
                  } else {
                    var data = MapData.get(p.server);
                    if (data.explored.remove(layer(s.dimension, plane), x, z)) data.setDirty();
                    s.terrain.remove(id);
                    var retry = new CompoundTag();
                    retry.putUUID("session", s.token);
                    retry.putBoolean("terrain", true);
                    retry.putBoolean("retry", true);
                    retry.putString("tile", id);
                    MapNetwork.send(p, retry);
                  }
                });
          });
      return true;
    } catch (RejectedExecutionException ex) {
      return false;
    }
  }

  public static int[] overlay(
      MinecraftServer server, String dimension, int plane, CompoundTag tile) {
    if (tile.getBoolean("detail")) return new int[0];
    int step = tile.getInt("step"), tx = tile.getInt("tx"), tz = tile.getInt("tz");
    if (step <= 0) return new int[0];
    int[] pixels = new int[1024];
    var index = MapData.get(server).explored;
    for (int z = 0; z < 32; z++)
      for (int x = 0; x < 32; x++)
        pixels[z * 32 + x] =
            index.sample(layer(dimension, plane), (tx * 32 + x) * step, (tz * 32 + z) * step, step);
    return pixels;
  }

  public static void flush() {
    try {
      IO.submit(() -> {}).get(5, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException | TimeoutException | RejectedExecutionException ex) {
      org.slf4j.LoggerFactory.getLogger(MapExploration.class)
          .warn("Shared map I/O was still pending at shutdown", ex);
    }
  }

  public static void clear() {
    flush();
    CACHE.clear();
    SCANS.clear();
    epoch++;
    ticks = 0;
    playerCursor = 0;
  }

  private MapExploration() {}
}
