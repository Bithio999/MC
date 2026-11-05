package dev.heavenlyrestriction;

import dev.heavenlyrestriction.cartography.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Reproducible native-generator benchmark; only the guarded disposable review save is accepted. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class MapPerformanceReview {
  private static boolean started;
  static volatile boolean done;
  private static volatile String failure;
  private static int age;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    var mc = Minecraft.getInstance();
    if (mc.player == null || mc.getSingleplayerServer() == null || ++age < 100) return;
    if (done && Boolean.getBoolean("additions.mapClarity")) return;
    if (done || failure != null) {
      try {
        if (failure != null)
          Files.writeString(
              Path.of("/workspace/validation-marauders-map/performance-failed.txt"), failure);
      } catch (Exception ignored) {
      }
      mc.stop();
      return;
    }
    if (started) return;
    started = true;
    var server = mc.getSingleplayerServer();
    server.execute(
        () -> {
          try {
            if (!server
                .getWorldPath(LevelResource.ROOT)
                .normalize()
                .getFileName()
                .toString()
                .equals("Marauders Map Review"))
              throw new IllegalStateException("Refusing personal save");
            var level = server.overworld();
            var generator = level.getChunkSource().getGenerator();
            var random = level.getChunkSource().randomState();
            var height = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
            var field = MapTerrain.class.getDeclaredField("EPOCH");
            field.setAccessible(true);
            long epoch = ((java.util.concurrent.atomic.AtomicLong) field.get(null)).get();
            CompletableFuture.runAsync(
                () -> {
                  try {
                    var rows = new ArrayList<String>();
                    for (boolean quick : new boolean[] {true, false})
                      for (int step : new int[] {1, 8}) {
                        long start = System.nanoTime();
                        var tile =
                            MapTerrain.preview(
                                generator,
                                random,
                                height,
                                Math.floorDiv(300000, 32 * step),
                                Math.floorDiv(-310000, 32 * step),
                                step,
                                Integer.MAX_VALUE,
                                epoch,
                                quick,
                                () -> true);
                        if (tile == null || tile.getIntArray("pixels").length != 1024)
                          throw new IllegalStateException("Incomplete preview");
                        double ms = (System.nanoTime() - start) / 1e6;
                        String row =
                            "quick="
                                + quick
                                + " step="
                                + step
                                + " ms="
                                + String.format(Locale.ROOT, "%.3f", ms);
                        rows.add(row);
                        System.out.println("[Map performance] " + row);
                      }
                    for (int step : new int[] {1, 8}) {
                      long start = System.nanoTime();
                      var tile =
                          MapTerrain.preview(
                              generator,
                              random,
                              height,
                              Math.floorDiv(300000, 32 * step),
                              Math.floorDiv(-310000, 32 * step),
                              step,
                              Integer.MAX_VALUE,
                              epoch,
                              false,
                              () -> true);
                      if (tile == null) throw new IllegalStateException("Warm preview cancelled");
                      rows.add(
                          "warm=true step="
                              + step
                              + " ms="
                              + String.format(
                                  Locale.ROOT, "%.3f", (System.nanoTime() - start) / 1e6));
                    }
                    String label = System.getProperty("additions.mapBenchmark", "after");
                    Path out =
                        Path.of(
                            "/workspace/validation-marauders-map/performance-" + label + ".txt");
                    Files.writeString(out, String.join("\n", rows) + "\n");
                    done = true;
                  } catch (Throwable e) {
                    failure = e.toString();
                    e.printStackTrace();
                  }
                });
          } catch (Throwable e) {
            failure = e.toString();
            e.printStackTrace();
          }
        });
  }
}
