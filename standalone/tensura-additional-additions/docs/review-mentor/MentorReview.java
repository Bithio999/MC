package dev.heavenlyrestriction;

import dev.heavenlyrestriction.chromatic.ChromaticContent;
import dev.heavenlyrestriction.mentor.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Explicitly restricted to an existing disposable review save. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class MentorReview {
  private static final Path OUT = Path.of("/workspace/validation-mentor");
  private static int phase, age, wait;
  private static volatile boolean ready;
  private static volatile String error;
  private static MentorEntity npc;
  private static Vec3 center, view;

  private static void check(boolean ok, String text) {
    if (!ok) throw new IllegalStateException(text);
  }

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
    var d = target.subtract(eye.add(0, p.getEyeHeight(), 0));
    float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90,
        pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
    p.teleportTo(p.serverLevel(), eye.x, eye.y, eye.z, Set.of(), yaw, pitch);
  }

  private static void shot(String name) throws Exception {
    Files.createDirectories(OUT);
    try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      image.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  private static void click(String name) {
    var screen = Minecraft.getInstance().screen;
    var b =
        screen.children().stream()
            .filter(
                c ->
                    c instanceof AbstractButton a
                        && a.visible
                        && a.getMessage().getString().equals(name))
            .map(c -> (AbstractButton) c)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing button " + name));
    check(b.active, "Disabled button " + name);
    check(
        screen.mouseClicked(b.getX() + b.getWidth() / 2d, b.getY() + b.getHeight() / 2d, 0),
        "Unconsumed click");
  }

  private static void bounds() {
    var s = Minecraft.getInstance().screen;
    var controls =
        s.children().stream()
            .filter(c -> c instanceof AbstractWidget w && w.visible)
            .map(c -> (AbstractWidget) c)
            .toList();
    for (var w : controls)
      check(
          w.getX() >= 0
              && w.getY() >= 0
              && w.getX() + w.getWidth() <= s.width
              && w.getY() + w.getHeight() <= s.height,
          "Out-of-bounds widget " + w.getMessage());
    for (int a = 0; a < controls.size(); a++)
      for (int b = a + 1; b < controls.size(); b++) {
        var x = controls.get(a);
        var y = controls.get(b);
        check(
            !(x.getX() < y.getX() + y.getWidth()
                && x.getX() + x.getWidth() > y.getX()
                && x.getY() < y.getY() + y.getHeight()
                && x.getY() + x.getHeight() > y.getY()),
            "Overlapping widgets");
      }
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    mc.options.pauseOnLostFocus = false;
    if (mc.screen != null) org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 2, 2);
    if (++age > 6000 || error != null) {
      try {
        Files.createDirectories(OUT);
        Files.writeString(
            OUT.resolve("failed.txt"), error == null ? "Timeout phase " + phase : error);
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
    if (++wait < 30) return;
    try {
      if (phase == 0) {
        ready = false;
        server(
            p -> {
              p.setGameMode(GameType.SPECTATOR);
              p.removeAllEffects();
              p.setHealth(p.getMaxHealth());
              p.clearFire();
              var pools = io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(p);
              pools.setSpiritualHealth(p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));
              for (var i : List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))
                i.setToggled(false);
              dev.heavenlyrestriction.splitmind.SplitMindCasting.cancelAll(p);
              p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(8_000_000);
              p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(2_000_000);
              p.getInventory().clearContent();
              p.getInventory().add(new ItemStack(ChromaticContent.ORB.get(), 1));
              SkillAPI.getSkillsFrom(p)
                  .getSkill(HeavenlyRestriction.PROSPECTOR.get())
                  .ifPresent(i -> SkillAPI.getSkillsFrom(p).forgetSkill(i));
              var level = p.server.overworld();
              var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
              var holder =
                  registry.getHolderOrThrow(
                      net.minecraft.resources.ResourceKey.create(
                          Registries.STRUCTURE, HeavenlyRestriction.id("mentor_archive")));
              var found =
                  level
                      .getChunkSource()
                      .getGenerator()
                      .findNearestMapStructure(
                          level, HolderSet.direct(holder), BlockPos.ZERO, 16, false);
              check(found != null, "No naturally locatable archive");
              var pos = found.getFirst();
              for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++)
                  level.getChunk((pos.getX() >> 4) + x, (pos.getZ() >> 4) + z);
              var start = level.getChunkAt(pos).getStartForStructure(holder.value());
              check(start != null && start.isValid(), "Natural structure start missing");
              var bounds = start.getBoundingBox();
              center =
                  new Vec3(
                      (bounds.minX() + bounds.maxX()) / 2d,
                      bounds.minY(),
                      (bounds.minZ() + bounds.maxZ()) / 2d);
              p.teleportTo(level, center.x, center.y + 15, center.z, Set.of(), 0, 0);
              try {
                Files.writeString(OUT.resolve("natural-location.txt"), pos.toShortString());
              } catch (Exception ex) {
                throw new IllegalStateException(ex);
              }
              view = center.add(35, 42, -40);
              look(p, view, center.add(0, 7, 0));
              level.setDayTime(6000);
              level.getWorldBorder().setCenter(0, 0);
              level.getWorldBorder().setSize(59999968);
            });
        phase = 1;
        wait = 0;
        return;
      }
      if (!ready) return;
      if (phase == 1) {
        mc.options.hideGui = true;
        if (wait < 90) return;
        shot("mentor-archive-exterior");
        ready = false;
        server(
            p -> {
              var list =
                  p.serverLevel()
                      .getEntitiesOfClass(
                          MentorEntity.class,
                          new net.minecraft.world.phys.AABB(center, center).inflate(70));
              check(list.size() == 1, "Natural archive missing mentor: " + list.size());
              npc = list.getFirst();
              var forward = npc.getLookAngle();
              look(
                  p,
                  npc.position().add(forward.scale(7)).add(0, 1, 0),
                  npc.position().add(0, 1, 0));
            });
        phase = 2;
        wait = 0;
        return;
      }
      if (phase == 2) {
        if (wait < 60) return;
        shot("mentor-archive-interior");
        ready = false;
        server(
            p -> {
              p.setGameMode(GameType.SURVIVAL);
              look(p, npc.position().add(npc.getLookAngle().scale(3)), npc.getEyePosition());
              MentorNetwork.open(p, npc);
            });
        mc.options.hideGui = false;
        phase = 3;
        wait = 0;
        return;
      }
      if (!(mc.screen instanceof dev.heavenlyrestriction.client.MentorScreen)) return;
      if (phase == 3) {
        bounds();
        var box =
            (EditBox)
                mc.screen.children().stream()
                    .filter(c -> c instanceof EditBox)
                    .findFirst()
                    .orElseThrow();
        box.setValue("Prospector");
        phase = 4;
        wait = 0;
        return;
      }
      if (phase == 4) {
        click("Prospector");
        phase = 5;
        wait = 0;
        return;
      }
      if (phase == 5) {
        bounds();
        shot("mentor-skill-menu");
        click("Learn this skill");
        phase = 6;
        wait = 0;
        return;
      }
      if (phase == 6) {
        bounds();
        shot("mentor-lesson-confirmation");
        click("Cancel");
        mc.options.guiScale().set(4);
        mc.resizeDisplay();
        phase = 7;
        wait = 0;
        return;
      }
      if (phase == 7) {
        bounds();
        shot("mentor-compact-menu");
        click("Learn this skill");
        phase = 8;
        wait = 0;
        return;
      }
      if (phase == 8) {
        bounds();
        click("Accept lesson");
        phase = 9;
        wait = 0;
        return;
      }
      if (phase == 9) {
        bounds();
        ready = false;
        server(
            p -> {
              check(
                  MentorLedger.reconcile(p)
                      .equals(HeavenlyRestriction.PROSPECTOR.getId().toString()),
                  "Redemption missing");
              check(p.getInventory().countItem(ChromaticContent.ORB.get()) == 0, "Orb not spent");
              check(Math.abs(EnergyHelper.getBaseMaxEP(p) - 9_000_000) < .01, "Wrong EP cost");
            });
        phase = 10;
        wait = 0;
        return;
      }
      if (phase == 10) {
        shot("mentor-lesson-completed");
        Files.writeString(
            OUT.resolve("passed.txt"),
            "Natural archive located and generated with one NPC. Real menu search/selection,"
                + " confirmation/cancel, compact control bounds, purchase, EP charge, Orb"
                + " consumption and persistent redemption passed.");
        mc.options.guiScale().set(2);
        mc.stop();
        phase = 11;
      }
    } catch (Throwable ex) {
      error = ex.toString();
      ex.printStackTrace();
    }
  }
}
