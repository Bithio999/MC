package dev.heavenlyrestriction;

import dev.heavenlyrestriction.cartography.*;
import dev.heavenlyrestriction.client.cartography.*;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in normal-world fill/reopen review; never included in a release. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class MapClarityReview {
  private static final Path OUT = Path.of("/workspace/validation-marauders-map/capture");
  private static final int X = 332144, Z = -342144;
  private static int phase, ticks, count;
  private static long start;
  private static volatile String failure;
  private static final List<String> metrics = new ArrayList<>();

  private static void require(boolean value, String message) {
    if (!value) throw new IllegalStateException(message);
  }

  private static void server(java.util.function.Consumer<ServerPlayer> action) {
    var s = Minecraft.getInstance().getSingleplayerServer();
    s.execute(
        () -> {
          try {
            action.accept(s.getPlayerList().getPlayers().getFirst());
          } catch (Throwable ex) {
            failure = ex.toString();
            ex.printStackTrace();
          }
        });
  }

  private static void shot(String name) throws Exception {
    try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      image.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  private static void quiet(MaraudersMapScreen map) throws Exception {
    var f = MaraudersMapScreen.class.getDeclaredField("structures");
    f.setAccessible(true);
    f.setBoolean(map, false);
  }

  private static boolean covered(MaraudersMapScreen map) throws Exception {
    int[] bounds = new int[4];
    String[] names = {"left", "top", "mapW", "mapH"};
    for (int i = 0; i < 4; i++) {
      var f = MaraudersMapScreen.class.getDeclaredField(names[i]);
      f.setAccessible(true);
      bounds[i] = f.getInt(map);
    }
    for (int y = 0; y < 4; y++)
      for (int x = 0; x < 8; x++)
        if (!MapTerrainAtlas.hasPreviewAt(
            (int) map.view.worldX(bounds[0] + (x + .5) * bounds[2] / 8, bounds[0], bounds[2]),
            (int) map.view.worldZ(bounds[1] + (y + .5) * bounds[3] / 4, bounds[1], bounds[3])))
          return false;
    return true;
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("additions.mapClarity") || !MapPerformanceReview.done) return;
    var mc = Minecraft.getInstance();
    if (mc.player == null) return;
    try {
      Files.createDirectories(OUT);
      if (failure != null) throw new IllegalStateException(failure);
      require(++ticks < 2400, "Map clarity review timed out at phase " + phase);
      if (phase == 0) {
        Files.deleteIfExists(OUT.resolve("passed-map-performance.txt"));
        Files.deleteIfExists(OUT.resolve("failed-map-performance.txt"));
        mc.options.guiScale().set(2);
        mc.resizeDisplay();
        phase = 1;
        ticks = 0;
        server(
            p -> {
              require(
                  p.server
                      .getWorldPath(LevelResource.ROOT)
                      .normalize()
                      .getFileName()
                      .toString()
                      .equals("Marauders Map Review"),
                  "Refusing personal save");
              p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
              p.removeAllEffects();
              io.github.manasmods.manascore.race.api.RaceAPI.getRaceFrom(p)
                  .setRace(
                      io.github.manasmods.tensura.registry.race.TensuraRaces.HUMAN.get(), false);
              for (var i : List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills())) {
                if (i.isToggled()) i.onToggleOff(p);
                SkillAPI.getSkillsFrom(p).forgetSkill(i);
              }
              RestrictionData.learnReward(p, HeavenlyRestriction.MARAUDERS_MAP.get());
              SkillAPI.getSkillsFrom(p)
                  .getSkill(HeavenlyRestriction.MARAUDERS_MAP.get())
                  .orElseThrow()
                  .setMastery(0);
              MapService.open(p);
            });
        return;
      }
      if (!(mc.screen instanceof MaraudersMapScreen map)) return;
      if (phase == 1) {
        quiet(map);
        map.view.scale = .125;
        map.view.center(X, Z);
        start = System.nanoTime();
        phase = 2;
        ticks = 0;
        return;
      }
      if (phase == 2) {
        if (metrics.stream().noneMatch(v -> v.startsWith("viewportPreviewMs="))
            && map.terrainViewReady()
            && covered(map))
          metrics.add("viewportPreviewMs=" + (System.nanoTime() - start) / 1000000);
        if (ticks == 40) shot("marauders-map-fast-preview");
        if (ticks < 80 || !map.terrainViewReady() || !MapTerrainAtlas.progress().isEmpty()) return;
        require(covered(map), "Completed map has gaps");
        metrics.add("viewportReliefMs=" + (System.nanoTime() - start) / 1000000);
        metrics.add("retainedTiles=" + MapTerrainAtlas.tileCount());
        count = MapTerrainAtlas.tileCount();
        shot("marauders-map-crisp-unexplored");
        phase = 3;
        ticks = 0;
        server(
            p ->
                require(
                    p.serverLevel().getChunkSource().getChunkNow(X >> 4, Z >> 4) == null,
                    "Zoomed-out preview loaded distant terrain"));
        map.onClose();
        return;
      }
      if (phase == 4) {
        // Assert before recentering: the old implementation discarded every preview on open.
        require(
            MapTerrainAtlas.hasPreviewAt(X, Z) && MapTerrainAtlas.tileCount() >= count,
            "Reopening discarded cached preview textures");
        quiet(map);
        map.view.scale = .125;
        map.view.center(X, Z);
        metrics.add("reopenCacheHit=true");
        phase = 5;
        ticks = 0;
        return;
      }
      if (phase == 5 && ticks > 30) {
        require(covered(map), "Reopened map lost coverage");
        shot("marauders-map-retained-preview");
        map.view.center(mc.player.getX(), mc.player.getZ());
        map.view.scale = .125;
        phase = 6;
        ticks = 0;
        return;
      }
      if (phase == 6
          && ticks > 100
          && map.terrainViewReady()
          && MapTerrainAtlas.progress().isEmpty()) {
        shot("marauders-map-crisp-explored");
        mc.options.guiScale().set(1);
        mc.resizeDisplay();
        phase = 7;
        ticks = 0;
        return;
      }
      if (phase == 7
          && ticks > 100
          && map.terrainViewReady()
          && MapTerrainAtlas.progress().isEmpty()) {
        verifyRecordedMip(mc);
        metrics.add("recordedMipReadback=true");
        shot("marauders-map-crisp-explored-large");
        Files.writeString(
            OUT.resolve("passed-map-performance.txt"),
            String.join("\n", metrics)
                + "\n"
                + "PASS: full unexplored viewport, no distant terrain load, cache retained on"
                + " reopen, recorded subchunk mip pixels at minimum zoom and GUI scales 1/2.\n");
        mc.stop();
      }
    } catch (Throwable ex) {
      ex.printStackTrace();
      try {
        Files.writeString(OUT.resolve("failed-map-performance.txt"), ex.toString());
      } catch (Exception ignored) {
      }
      mc.stop();
    }
  }

  private static void verifyRecordedMip(Minecraft mc) throws Exception {
    var regionsField = MapAtlas.class.getDeclaredField("REGIONS");
    regionsField.setAccessible(true);
    var regions = (Map<?, ?>) regionsField.get(null);
    int x = mc.player.getBlockX(),
        z = mc.player.getBlockZ(),
        rx = Math.floorDiv(x, 256),
        rz = Math.floorDiv(z, 256);
    var region = regions.get(net.minecraft.world.level.ChunkPos.asLong(rx, rz));
    require(region != null, "No live recorded region at player");
    var fullField = region.getClass().getDeclaredField("image");
    fullField.setAccessible(true);
    var full = (com.mojang.blaze3d.platform.NativeImage) fullField.get(region);
    var tilesField = MapRegionMips.class.getDeclaredField("TILES");
    tilesField.setAccessible(true);
    for (var entry : ((Map<?, ?>) tilesField.get(null)).entrySet()) {
      var key = entry.getKey();
      int[] values = new int[3];
      String[] names = {"x", "z", "step"};
      for (int i = 0; i < 3; i++) {
        var field = key.getClass().getDeclaredField(names[i]);
        field.setAccessible(true);
        values[i] = field.getInt(key);
      }
      if (values[0] != rx || values[1] != rz || values[2] != 8) continue;
      var imageField = entry.getValue().getClass().getDeclaredField("image");
      imageField.setAccessible(true);
      var image = (com.mojang.blaze3d.platform.NativeImage) imageField.get(entry.getValue());
      int bx = Math.floorMod(x, 256) & ~7, bz = Math.floorMod(z, 256) & ~7;
      int expected = MapColorMip.sample(full::getPixelRGBA, bx, bz, 8);
      require(
          expected != 0 && image.getWidth() == 32 && image.getPixelRGBA(bx / 8, bz / 8) == expected,
          "Recorded mip differs from current native block pixels");
      return;
    }
    throw new IllegalStateException("No minimum-zoom mip for recorded player region");
  }

  @SubscribeEvent
  public static void reopen(ClientTickEvent.Post event) {
    if (Boolean.getBoolean("additions.mapClarity")
        && phase == 3
        && Minecraft.getInstance().screen == null) {
      phase = 4;
      server(MapService::open);
    }
  }
}
