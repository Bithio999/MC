package dev.heavenlyrestriction;

import dev.heavenlyrestriction.mentor.MentorEntity;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Opt-in visual review, restricted to the disposable Archive Review save. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class MentorArtReview {
  private static final Path OUT = Path.of("/workspace/validation-mentor/art");
  private static int phase, age, wait;
  private static volatile boolean ready;
  private static volatile String error;
  private static BlockPos origin;
  private static MentorEntity npc;
  private static boolean exteriorSet;
  private static volatile Vec3 expectedEye;

  private static void server(java.util.function.Consumer<ServerPlayer> job) {
    Minecraft.getInstance()
        .getSingleplayerServer()
        .execute(
            () -> {
              try {
                job.accept(
                    Minecraft.getInstance()
                        .getSingleplayerServer()
                        .getPlayerList()
                        .getPlayers()
                        .getFirst());
                ready = true;
              } catch (Throwable e) {
                error = e.toString();
                e.printStackTrace();
              }
            });
  }

  private static void look(ServerPlayer p, Vec3 eye, Vec3 target) {
    expectedEye = eye;
    var d = target.subtract(eye.add(0, p.getEyeHeight(), 0));
    float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90;
    float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
    p.teleportTo(p.server.overworld(), eye.x, eye.y, eye.z, Set.of(), yaw, pitch);
  }

  private static Vec3 at(double x, double y, double z) {
    return Vec3.atLowerCornerOf(origin).add(x, y, z);
  }

  private static void shot(String name) throws Exception {
    Files.createDirectories(OUT);
    try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      image.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  @SubscribeEvent
  public static void hud(RenderGuiEvent.Pre e) {
    if (Minecraft.getInstance().options.hideGui) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    mc.options.pauseOnLostFocus = false;
    mc.options.gamma().set(1d);
    if (mc.options.renderDistance().get() != 10) {
      mc.options.renderDistance().set(10);
      mc.options.broadcastOptions();
    }
    if (++age > 6000 || error != null) {
      try {
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve("failed.txt"), error == null ? "Timeout " + phase : error);
      } catch (Exception ignored) {
      }
      mc.stop();
      return;
    }
    if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
    if (!mc.getSingleplayerServer()
        .getWorldPath(LevelResource.ROOT)
        .toString()
        .contains("Archive Review")) {
      error = "Refusing non-review save";
      return;
    }
    if (mc.screen != null) return;
    if (++wait < 30) return;
    try {
      if (phase == 0) {
        ready = false;
        server(
            p -> {
              var level = p.server.overworld();
              p.setGameMode(GameType.SPECTATOR);
              p.removeAllEffects();
              p.setHealth(p.getMaxHealth());
              p.clearFire();
              for (var s :
                  io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(p)
                      .getLearnedSkills()) s.setToggled(false);
              dev.heavenlyrestriction.splitmind.SplitMindCasting.cancelAll(p);
              level.setDayTime(6000);
              level.setWeatherParameters(999999, 0, false, false);
              level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
              level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
              level.getWorldBorder().setCenter(0, 0);
              level.getWorldBorder().setSize(59999968);
              level.getWorldBorder().setWarningBlocks(0);
              level.getWorldBorder().setWarningTime(0);
              // Choose a fresh, nearly level patch using generated terrain, not prior review
              // buildings.
              var gen = level.getChunkSource().getGenerator();
              int ox = 3072, oz = 3072, y = 70, best = Integer.MAX_VALUE;
              search:
              for (int cx = -4096; cx <= 4096; cx += 512)
                for (int cz = -4096; cz <= 4096; cz += 512) {
                  int lo = 999, hi = -999;
                  for (var offset : new int[][] {{0, 0}, {30, 0}, {0, 32}, {30, 32}, {16, 16}}) {
                    int hy =
                        gen.getBaseHeight(
                            cx + offset[0],
                            cz + offset[1],
                            Heightmap.Types.WORLD_SURFACE_WG,
                            level,
                            level.getChunkSource().randomState());
                    lo = Math.min(lo, hy);
                    hi = Math.max(hi, hy);
                  }
                  int score = hi - lo + (lo < 65 ? 1000 : 0);
                  if (score < best) {
                    best = score;
                    ox = cx;
                    oz = cz;
                    y = hi;
                  }
                  if (best <= 5) break search;
                }
              if (best > 8)
                throw new IllegalStateException("No suitable flat review patch: " + best);
              origin = new BlockPos(ox, y, oz);
              try {
                Files.createDirectories(OUT);
                Files.writeString(
                    OUT.resolve("arena.txt"), origin.toShortString() + " slope " + best);
              } catch (Exception ex) {
                throw new IllegalStateException(ex);
              }
              for (int cx = (ox >> 4) - 3; cx <= (ox >> 4) + 4; cx++)
                for (int cz = (oz >> 4) - 3; cz <= (oz >> 4) + 4; cz++) level.getChunk(cx, cz);
              for (int dx = 0; dx < 31; dx++)
                for (int dz = 0; dz < 33; dz++) {
                  for (int yy = y - 5; yy < y + 40; yy++) {
                    var at = new BlockPos(ox + dx, yy, oz + dz);
                    var state =
                        yy < y - 1
                            ? Blocks.DIRT.defaultBlockState()
                            : yy == y - 1
                                ? Blocks.GRASS_BLOCK.defaultBlockState()
                                : Blocks.AIR.defaultBlockState();
                    if (!level.getBlockState(at).equals(state)) level.setBlock(at, state, 2);
                  }
                }
              level
                  .getEntitiesOfClass(MentorEntity.class, new AABB(origin).inflate(60))
                  .forEach(MentorEntity::discard);
              var template =
                  level.getStructureManager().getOrCreate(HeavenlyRestriction.id("mentor_archive"));
              if (!template.placeInWorld(
                  level,
                  origin,
                  origin,
                  new StructurePlaceSettings().setIgnoreEntities(false),
                  level.random,
                  2)) throw new IllegalStateException("Placement failed");
              var list =
                  level.getEntitiesOfClass(
                      MentorEntity.class, new AABB(at(0, 0, 0), at(31, 29, 33)));
              if (list.size() != 1)
                throw new IllegalStateException("Expected one Elias: " + list.size());
              npc = list.getFirst();
              npc.setNoAi(true);
              npc.setYRot(180);
              npc.setYHeadRot(180);
              npc.yBodyRot = 180;
              look(p, at(-12, 23, -13), at(14, 12, 16));
            });
        mc.options.hideGui = true;
        phase = 1;
        wait = 0;
        return;
      }
      if (!ready) return;
      if (phase == 1 && !exteriorSet) {
        exteriorSet = true;
        ready = false;
        server(p -> look(p, at(-12, 23, -13), at(14, 12, 16)));
        wait = 0;
        return;
      }
      if (wait < 240) return;
      if (expectedEye != null && mc.player.position().distanceToSqr(expectedEye) > 1) {
        error =
            "Camera did not reach requested position: "
                + mc.player.position()
                + " expected "
                + expectedEye;
        return;
      }
      if (phase == 1) {
        shot("mentor-archive-exterior");
        ready = false;
        server(p -> look(p, at(15, 6, 11), at(16, 6, 27)));
        phase = 2;
        wait = 0;
        return;
      }
      if (phase == 2) {
        shot("mentor-archive-interior");
        ready = false;
        server(p -> look(p, npc.position().add(0, .1, -3.2), npc.position().add(0, 1.1, 0)));
        phase = 3;
        wait = 0;
        return;
      }
      if (phase == 3) {
        shot("elias-kael-style");
        ready = false;
        server(p -> look(p, at(11, 9.4, 15), at(19, 6, 25)));
        phase = 4;
        wait = 0;
        return;
      }
      if (phase == 4) {
        shot("mentor-archive-gallery");
        Files.writeString(
            OUT.resolve("passed.txt"),
            "Actual client: revised template, exterior, gallery, interior and Kael-style Elias.\n");
        mc.stop();
        phase = 5;
      }
    } catch (Throwable ex) {
      error = ex.toString();
      ex.printStackTrace();
    }
  }
}
