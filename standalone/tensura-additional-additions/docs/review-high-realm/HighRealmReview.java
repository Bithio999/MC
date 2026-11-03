package dev.heavenlyrestriction;

import dev.heavenlyrestriction.highrealm.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class HighRealmReview {
  private static final Path OUT = Path.of("/workspace/validation-high-realm/capture");
  private static final BlockPos CORE = new BlockPos(0, 161, 0);
  private static volatile int phase;
  private static int age, tick;
  private static volatile String error;

  private static void check(boolean ok, String m) {
    if (!ok) throw new IllegalStateException(m);
  }

  private static void server(
      java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> work) {
    var s = Minecraft.getInstance().getSingleplayerServer();
    s.execute(
        () -> {
          try {
            work.accept(s.getPlayerList().getPlayers().getFirst());
          } catch (Throwable e) {
            e.printStackTrace();
            error = e.toString();
          }
        });
  }

  private static void shot(String name) {
    try {
      Files.createDirectories(OUT);
      try (var im = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
        im.writeToFile(OUT.resolve(name + ".png"));
      }
    } catch (Exception e) {
      error = e.toString();
    }
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    var mc = Minecraft.getInstance();
    mc.options.pauseOnLostFocus = false;
    if (++age > 10000 || error != null) {
      try {
        Files.createDirectories(OUT);
        Files.writeString(
            OUT.resolve("failure.txt"), error == null ? "Timeout phase " + phase : error);
      } catch (Exception ignored) {
      }
      mc.stop();
      return;
    }
    if (mc.player == null
        || mc.level == null
        || mc.getSingleplayerServer() == null
        || mc.getOverlay() != null) return;
    if (phase == 0 && mc.player.tickCount > 60) {
      phase = 1;
      mc.setScreen(null);
      mc.options.guiScale().set(2);
      mc.options.renderDistance().set(8);
      mc.options.hideGui = false;
      mc.options.particles().set(net.minecraft.client.ParticleStatus.ALL);
      mc.options.hideGui = true;
      mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
      try {
        Files.createDirectories(OUT);
        Files.deleteIfExists(OUT.resolve("failure.txt"));
        Files.deleteIfExists(OUT.resolve("passed.txt"));
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      server(
          p -> {
            check(
                p.server
                    .getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath()
                    .normalize()
                    .getFileName()
                    .toString()
                    .equals("High Realm Review"),
                "Disposable save required");
            check(
                p.server.getLevel(HighRealm.LEVEL) != null,
                "Datapack dimension not added to existing save");
            p.removeAllEffects();
            p.getInventory().clearContent();
            for (var i : List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))
              SkillAPI.getSkillsFrom(p).forgetSkill(i);
            RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(), false);
            p.setGameMode(GameType.SURVIVAL);
            var pool = TensuraStorages.getExistenceFrom(p);
            pool.setSleepModeTime(-1);
            pool.markDirty();
            for (int x = -6; x <= 6; x++)
              for (int z = -4; z <= 8; z++)
                p.serverLevel()
                    .setBlock(CORE.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
            RealmData.get(p.server)
                .gates
                .entrySet()
                .removeIf(e -> e.getValue().core().equals(CORE));
            HighRealmGameTests.frame(p.serverLevel(), CORE, Direction.Axis.X);
            p.connection.teleport(.5, 162, 3.5, 180, 20);
            EnergyHelper.setBaseMaxEP(p, HighRealmConfig.ENTRY_EP.get() - 1);
            check(!HighRealm.activate(p, CORE, Direction.Axis.X), "Below-threshold gate activated");
            EnergyHelper.setBaseMaxEP(p, HighRealmConfig.ENTRY_EP.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHER_STAR));
            p.getInventory().setItem(8, new ItemStack(Items.DIAMOND, 7));
            RestrictionData.learnReward(p, HeavenlyRestriction.SCHEMA.get());
            phase = 2;
          });
      tick = 0;
      return;
    }
    if (phase == 2 && ++tick > 40) {
      mc.gameMode.useItemOn(
          mc.player,
          InteractionHand.MAIN_HAND,
          new BlockHitResult(Vec3.atCenterOf(CORE), Direction.SOUTH, CORE, false));
      phase = 3;
      tick = 0;
      return;
    }
    if (phase == 3 && ++tick > 50) {
      phase = 4;
      server(
          p -> {
            check(
                RealmData.get(p.server).gates.values().stream()
                    .anyMatch(g -> g.core().equals(CORE)),
                "Real right-click packet did not awaken gate");
            check(p.getMainHandItem().isEmpty(), "Nether star not consumed exactly once");
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.connection.teleport(.5, 164, 10.5, 180, 7);
            phase = 5;
          });
      tick = 0;
      return;
    }
    if (phase == 5 && ++tick > 40) {
      shot("high-realm-gate");
      phase = 6;
      server(
          p -> {
            p.teleportTo(.5, 162, .5);
          });
      tick = 0;
      return;
    }
    if (phase == 6 && mc.level.dimension().equals(HighRealm.LEVEL) && ++tick > 80) {
      phase = 7;
      server(
          p -> {
            check(p.level().dimension().equals(HighRealm.LEVEL), "Charging gate did not enter");
            check(!p.serverLevel().isDay(), "Fixed twilight would block natural hostile spawns");
            check(
                p.getInventory().getItem(8).getCount() == 7
                    && SkillAPI.getSkillsFrom(p)
                        .getSkill(HeavenlyRestriction.SCHEMA.get())
                        .isPresent(),
                "Inventory/skill lost on dimension travel");
            var c = RealmData.get(p.server).sanctuary;
            check(
                c != null && p.serverLevel().getBlockState(c).is(Blocks.BEDROCK),
                "Return sanctum missing");
            p.connection.teleport(c.getX() + .5, c.getY() + 3, c.getZ() + 8.5, 180, 10);
            p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            phase = 8;
          });
      tick = 0;
      return;
    }
    if (phase == 8 && ++tick > 70) {
      shot("high-realm-arrival");
      mc.options.hideGui = true;
      phase = 9;
      server(
          p -> {
            var c = RealmData.get(p.server).sanctuary;
            int top = c.getY();
            for (int x : new int[] {-48, 0, 48})
              for (int z : new int[] {-48, 0, 48})
                top =
                    Math.max(
                        top,
                        p.serverLevel()
                            .getHeight(
                                net.minecraft.world.level.levelgen.Heightmap.Types
                                    .MOTION_BLOCKING_NO_LEAVES,
                                x,
                                z));
            p.connection.teleport(48.5, top + 36, 80.5, 150, 45);
            phase = 10;
          });
      tick = 0;
      return;
    }
    if (phase == 10 && ++tick > 120) {
      check(
          Math.abs(mc.player.getXRot() - 45) < 1, "Landscape camera rotation was not synchronized");
      shot("high-realm-landscape");
      phase = 11;
      server(
          p -> {
            EnergyHelper.setBaseMaxEP(p, 1);
            check(HighRealm.leave(p), "Return blocked by lost EP");
            check(
                p.level().dimension().equals(Level.OVERWORLD),
                "Return did not reach entrance dimension");
            check(
                p.position().distanceTo(Vec3.atBottomCenterOf(CORE.above()).add(0, 0, 3)) < 10,
                "Return ignored individual entrance");
            check(p.getInventory().getItem(8).getCount() == 7, "Return inventory changed");
            phase = 12;
          });
      tick = 0;
      return;
    }
    if (phase == 12 && mc.level.dimension().equals(Level.OVERWORLD) && ++tick > 40) {
      phase = 13;
      server(
          p -> {
            EnergyHelper.setBaseMaxEP(p, HighRealmConfig.ENTRY_EP.get());
            check(
                HighRealm.enter(p, new RealmData.Gate(Level.OVERWORLD, CORE, Direction.Axis.X)),
                "Existing activated gate failed on reuse");
            check(p.getMainHandItem().isEmpty(), "Second star required");
            phase = 14;
          });
      tick = 0;
      return;
    }
    if (phase == 14 && mc.level.dimension().equals(HighRealm.LEVEL) && ++tick > 40) {
      phase = 15;
      server(
          p -> {
            EnergyHelper.setBaseMaxEP(p, 0);
            var c = RealmData.get(p.server).sanctuary;
            p.teleportTo(.5, c.getY() + 1, .5);
            p.setPortalCooldown(0);
          });
      tick = 0;
      return;
    }
    if (phase == 15 && mc.level.dimension().equals(Level.OVERWORLD) && ++tick > 40) {
      try {
        Files.writeString(
            OUT.resolve("passed.txt"),
            "PASS: existing save gains High Realm; EP threshold; actual right-click C2S activation"
                + " consumes exactly one Nether Star; two-second walking entry; safe sanctum;"
                + " inventory/skills retained; low-EP safe return to individual source; reuse"
                + " without another star; walking exit without EP.\n");
      } catch (Exception ex) {
        error = ex.toString();
      }
      mc.options.hideGui = false;
      var guide = new dev.heavenlyrestriction.client.guide.GuideScreen(null);
      mc.setScreen(guide);
      var category =
          guide.children().stream()
              .filter(
                  c ->
                      c instanceof net.minecraft.client.gui.components.Button b
                          && b.getMessage().getString().equals("Dimensions"))
              .map(c -> (net.minecraft.client.gui.components.Button) c)
              .findFirst()
              .orElseThrow();
      category.onPress();
      for (var c : guide.children())
        if (c instanceof net.minecraft.client.gui.components.EditBox search)
          search.setValue("High Realm");
      guide.select("high-realm", true);
      phase = 16;
      tick = 0;
    }
    if (phase == 16 && ++tick > 25) {
      shot("high-realm-guide");
      mc.options.guiScale().set(3);
      mc.resizeDisplay();
      phase = 17;
      tick = 0;
    }
    if (phase == 17 && ++tick > 25) {
      shot("high-realm-guide-small");
      mc.stop();
    }
  }
}
