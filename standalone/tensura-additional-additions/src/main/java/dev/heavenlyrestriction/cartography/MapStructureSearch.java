package dev.heavenlyrestriction.cartography;

import dev.heavenlyrestriction.mixin.MapChunkAccess;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.Holder;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.*;

/** Native placement-region search and biome/generation-point filtering outside the server tick. */
public class MapStructureSearch {
  private static final ThreadPoolExecutor WORKER =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(8),
          r -> {
            var thread = new Thread(r, "MaraudersMap-Structures");
            thread.setDaemon(true);
            return thread;
          },
          new ThreadPoolExecutor.AbortPolicy());
  private static final TicketType<UUID> TICKET =
      TicketType.create("additions_structure_search", UUID::compareTo, 200);
  public final Holder<Structure> type;
  public final List<StructurePlacement> placements;
  public final int x, z, blockX, blockZ;
  public volatile int checked, candidates;
  public volatile boolean finished, cancelled;
  public boolean done;
  public volatile String error = "";
  public ChunkPos ready, requested;
  public CompletableFuture<ChunkResult<ChunkAccess>> pending;
  private final ServerLevel level;
  private final UUID ticket = UUID.randomUUID();
  private final ArrayBlockingQueue<ChunkPos> found = new ArrayBlockingQueue<>(2);
  private Future<?> work;

  protected MapStructureSearch(ServerPlayer player, Holder<Structure> type, int x, int z) {
    level = player.serverLevel();
    this.type = type;
    blockX = x;
    blockZ = z;
    this.x = x >> 4;
    this.z = z >> 4;
    var state = level.getChunkSource().getGeneratorState();
    placements = List.copyOf(state.getPlacementsForStructure(type));
    if (placements.isEmpty()) {
      done = finished = true;
      return;
    }
    var generator = level.getChunkSource().getGenerator();
    if (generator.getBiomeSource().possibleBiomes().stream()
        .noneMatch(type.value().biomes()::contains)) {
      error = "This structure has no suitable biome in this dimension.";
      done = finished = true;
      return;
    }
    var random = level.getChunkSource().randomState();
    var height = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
    var registry = level.registryAccess();
    var templates = level.getStructureManager();
    long seed = level.getSeed();
    double minX = Math.max(-MapView.LIMIT, level.getWorldBorder().getMinX()),
        maxX = Math.min(MapView.LIMIT, level.getWorldBorder().getMaxX()),
        minZ = Math.max(-MapView.LIMIT, level.getWorldBorder().getMinZ()),
        maxZ = Math.min(MapView.LIMIT, level.getWorldBorder().getMaxZ());
    try {
      WORKER.purge();
      work =
          WORKER.submit(
              () -> {
                try {
                  var cursors = new ArrayList<Cursor>();
                  for (var placement : placements) {
                    Iterator<ChunkPos> iterator;
                    if (placement instanceof RandomSpreadStructurePlacement spread) {
                      int spacing = spread.spacing(),
                          radius = (MapService.SEARCH_RADIUS + spacing - 1) / spacing + 1;
                      var grid =
                          new StructureSearchGrid(
                              Math.floorDiv(this.x, spacing),
                              Math.floorDiv(this.z, spacing),
                              radius);
                      iterator =
                          new Iterator<>() {
                            public boolean hasNext() {
                              return grid.hasNext();
                            }

                            public ChunkPos next() {
                              var cell = grid.next();
                              return spread.getPotentialStructureChunk(
                                  state.getLevelSeed(), cell.x() * spacing, cell.z() * spacing);
                            }
                          };
                    } else if (placement instanceof ConcentricRingsStructurePlacement rings) {
                      // Ring positions are computed by native worldgen futures; this wait is
                      // worker-only.
                      var positions = state.getRingPositionsFor(rings);
                      var sorted =
                          positions == null
                              ? new ArrayList<ChunkPos>()
                              : new ArrayList<>(positions);
                      sorted.sort(Comparator.comparingDouble(this::distance));
                      iterator = sorted.iterator();
                    } else {
                      var grid =
                          new StructureSearchGrid(this.x, this.z, MapService.SEARCH_RADIUS + 32);
                      iterator =
                          new Iterator<>() {
                            public boolean hasNext() {
                              return grid.hasNext();
                            }

                            public ChunkPos next() {
                              var cell = grid.next();
                              return new ChunkPos(cell.x(), cell.z());
                            }
                          };
                    }
                    if (iterator.hasNext())
                      cursors.add(new Cursor(placement, iterator, iterator.next()));
                  }
                  var queue =
                      new PriorityQueue<Cursor>(
                          Comparator.comparingDouble(c -> distance(c.position)));
                  queue.addAll(cursors);
                  while (!queue.isEmpty()
                      && !cancelled
                      && !Thread.currentThread().isInterrupted()) {
                    var cursor = queue.remove();
                    var pos = cursor.position;
                    if (cursor.iterator.hasNext())
                      queue.add(
                          new Cursor(cursor.placement, cursor.iterator, cursor.iterator.next()));
                    if (checked < Integer.MAX_VALUE) checked++;
                    if (distance(pos)
                        > (MapService.SEARCH_RANGE + 512d) * (MapService.SEARCH_RANGE + 512d))
                      continue;
                    int wx = pos.getMiddleBlockX(), wz = pos.getMiddleBlockZ();
                    if (wx < minX || wx >= maxX || wz < minZ || wz >= maxZ) continue;
                    if (!cursor.placement.isStructureChunk(state, pos.x, pos.z)) continue;
                    if (candidates < Integer.MAX_VALUE) candidates++;
                    var context =
                        new Structure.GenerationContext(
                            registry,
                            generator,
                            generator.getBiomeSource(),
                            random,
                            templates,
                            seed,
                            pos,
                            height,
                            type.value().biomes()::contains);
                    if (type.value().findValidGenerationPoint(context).isEmpty()) continue;
                    while (!cancelled && !found.offer(pos, 50, TimeUnit.MILLISECONDS)) {}
                  }
                } catch (InterruptedException ex) {
                  Thread.currentThread().interrupt();
                } catch (RuntimeException ex) {
                  error =
                      "This structure's generator could not complete the search. Try another type.";
                } finally {
                  finished = true;
                }
              });
    } catch (RejectedExecutionException ex) {
      error = "Structure searches are busy. Please try again shortly.";
      done = finished = true;
    }
  }

  private record Cursor(
      StructurePlacement placement, Iterator<ChunkPos> iterator, ChunkPos position) {}

  private double distance(ChunkPos pos) {
    double dx = pos.getMiddleBlockX() - (double) blockX,
        dz = pos.getMiddleBlockZ() - (double) blockZ;
    return dx * dx + dz * dz;
  }

  public ChunkPos poll() {
    return found.poll();
  }

  public boolean exhausted() {
    return finished && found.isEmpty();
  }

  public void schedule() {
    requested = ready;
    ready = null;
    refreshTicket();
    try {
      pending =
          ((MapChunkAccess) level.getChunkSource())
              .additions$mapFuture(requested.x, requested.z, ChunkStatus.STRUCTURE_STARTS, true);
    } catch (RuntimeException ex) {
      releaseTicket();
      throw ex;
    }
  }

  public void refreshTicket() {
    if (requested != null)
      ((MapChunkAccess) level.getChunkSource())
          .additions$mapTickets()
          .addTicket(TICKET, requested, ChunkLevel.byStatus(ChunkStatus.STRUCTURE_STARTS), ticket);
  }

  public void releaseTicket() {
    if (requested != null)
      ((MapChunkAccess) level.getChunkSource())
          .additions$mapTickets()
          .removeTicket(
              TICKET, requested, ChunkLevel.byStatus(ChunkStatus.STRUCTURE_STARTS), ticket);
    requested = null;
  }

  public void stop() {
    cancelled = true;
    releaseTicket();
    found.clear();
    if (work != null) work.cancel(true);
  }
}
