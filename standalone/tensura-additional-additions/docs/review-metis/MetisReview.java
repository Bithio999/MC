package dev.heavenlyrestriction;

import dev.heavenlyrestriction.metis.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.magic.AspectualMagics;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in real client review; its only save mutation is the disposable Wayfarer Review world. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class MetisReview {
  private static final Path OUT = Path.of("/workspace/validation-wayfarer/capture");
  private static int phase, age, wait, guiScale;
  private static boolean hideGui;
  private static net.minecraft.client.gui.screens.Screen previous;
  private static volatile String error;
  private static volatile boolean prepared;

  private static void check(boolean ok, String text) {
    if (!ok) throw new IllegalStateException(text);
  }

  private static void server(java.util.function.Consumer<ServerPlayer> work) {
    var server = Minecraft.getInstance().getSingleplayerServer();
    server.execute(
        () -> {
          try {
            work.accept(server.getPlayerList().getPlayers().getFirst());
          } catch (Throwable e) {
            error = e.toString();
            e.printStackTrace();
          }
        });
  }

  private static void learn(ServerPlayer p, ManasSkill skill) {
    RestrictionData.learnReward(p, skill);
    var i = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();
    i.setMastery(i.getMaxMastery());
    i.markDirty();
  }

  private static void shot(String name) throws Exception {
    Files.createDirectories(OUT);
    try (var im = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      im.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  private static CompoundTag state() {
    try {
      var f = dev.heavenlyrestriction.client.MetisScreen.class.getDeclaredField("state");
      f.setAccessible(true);
      return (CompoundTag) f.get(Minecraft.getInstance().screen);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static void click(String label) {
    var screen = Minecraft.getInstance().screen;
    var button =
        screen.children().stream()
            .filter(
                c ->
                    c instanceof AbstractButton b
                        && b.visible
                        && b.getMessage().getString().equals(label))
            .map(c -> (AbstractButton) c)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing button " + label));
    check(button.active, "Disabled button " + label);
    check(
        screen.mouseClicked(
            button.getX() + button.getWidth() / 2d, button.getY() + button.getHeight() / 2d, 0),
        "Click not handled " + label);
  }

  private static void bounds() {
    var screen = Minecraft.getInstance().screen;
    var widgets =
        screen.children().stream()
            .filter(c -> c instanceof AbstractWidget w && w.visible)
            .map(c -> (AbstractWidget) c)
            .toList();
    for (var w : widgets)
      check(
          w.getX() >= 0
              && w.getY() >= 0
              && w.getX() + w.getWidth() <= screen.width
              && w.getY() + w.getHeight() <= screen.height,
          "Widget outside viewport: " + w.getMessage());
    for (int a = 0; a < widgets.size(); a++)
      for (int b = a + 1; b < widgets.size(); b++) {
        var x = widgets.get(a);
        var y = widgets.get(b);
        check(
            !(x.getX() < y.getX() + y.getWidth()
                && x.getX() + x.getWidth() > y.getX()
                && x.getY() < y.getY() + y.getHeight()
                && x.getY() + x.getHeight() > y.getY()),
            "Overlapping controls: " + x.getMessage() + " / " + y.getMessage());
      }
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 2, 2);
    mc.options.pauseOnLostFocus = false;
    if (++age > 6000 || error != null) {
      try {
        Files.createDirectories(OUT);
        Files.writeString(
            OUT.resolve("failed-metis.txt"), error == null ? "Timeout " + phase : error);
      } catch (Exception ignored) {
      }
      mc.stop();
      return;
    }
    if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
    if (!mc.getSingleplayerServer()
        .getWorldPath(LevelResource.ROOT)
        .toString()
        .contains("Wayfarer Review")) {
      error = "Refusing non-review save";
      return;
    }
    if (mc.screen != previous) {
      previous = mc.screen;
      wait = 0;
    }
    if (phase > 0
        && phase < 9
        && !(mc.screen instanceof dev.heavenlyrestriction.client.MetisScreen)) {
      wait = 0;
      return;
    }
    if (++wait < 15) return;
    try {
      if (phase == 0) {
        guiScale = mc.options.guiScale().get();
        hideGui = mc.options.hideGui;
        mc.options.hideGui = true;
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 5, 5);
        server(
            p -> {
              var storage = SkillAPI.getSkillsFrom(p);
              for (var i : List.copyOf(storage.getLearnedSkills()))
                if (i.getSkill() instanceof io.github.manasmods.tensura.ability.magic.Magic
                    || i.getSkill() == HeavenlyRestriction.METIS.get()) storage.forgetSkill(i);
              EnergyHelper.setBaseMaxEP(p, 20000000);
              TensuraStorages.getExistenceFrom(p).setTrueHero(true);
              TensuraStorages.getExistenceFrom(p).setMagicule(EnergyHelper.getMaxMagicule(p));
              TensuraStorages.getExistenceFrom(p).setAura(EnergyHelper.getMaxAura(p));
              learn(p, HeavenlyRestriction.SPLIT_MIND.get());
              learn(p, UniqueSkills.GREAT_SAGE.get());
              for (var skill :
                  List.of(
                      AspectualMagics.FIRE.get(),
                      AspectualMagics.WIND_CUTTER.get(),
                      AspectualMagics.HEALING.get(),
                      AspectualMagics.FIRE_BALL.get(),
                      AspectualMagics.FIRE_LANCE.get(),
                      AspectualMagics.AGILITY.get(),
                      AspectualMagics.STONE_SHOT.get())) learn(p, skill);
              learn(p, HeavenlyRestriction.CRITICAL_MERCHANT.get());
              learn(p, HeavenlyRestriction.PROSPECTOR.get());
              learn(p, UniqueSkills.HEALER.get());
              learn(p, io.github.manasmods.tensura.registry.skill.ExtraSkills.SPATIAL_MOTION.get());
              MetisSynthesis.discover(p);
              ((io.github.manasmods.manascore.storage.api.StorageHolder) p).manasCore$sync();
              MetisNetwork.open(p);
              prepared = true;
            });
        phase = 1;
        wait = 0;
        return;
      }
      if (phase == 9) {
        shot("metis-ultimate-menu");
        Files.writeString(
            OUT.resolve("passed-metis.txt"),
            "Native synthesis confirmation, real assignment packet, modes, mastery/four threads,"
                + " native Ultimate selection, normal/320x180 bounds, skill assignment, menu-mode"
                + " exclusion, background container guards and ordinary manual menus passed.");
        mc.options.hideGui = hideGui;
        mc.stop();
        phase = 10;
        return;
      }
      if (!prepared || !(mc.screen instanceof dev.heavenlyrestriction.client.MetisScreen)) return;
      if (phase == 1) {
        bounds();
        check(state().getBoolean("ready"), "Synthesis not ready");
        shot("metis-synthesis");
        click("Synthesize Metis");
        phase = 2;
        wait = 0;
        return;
      }
      if (phase == 2) {
        shot("metis-confirmation");
        int w = Math.min(450, mc.screen.width - 16),
            h = Math.min(286, mc.screen.height - 16),
            x = (mc.screen.width - w) / 2,
            y = (mc.screen.height - h) / 2;
        mc.screen.mouseClicked(x + w * .73, y + 57 + 68, 0);
        phase = 3;
        wait = 0;
        return;
      }
      if (phase == 3) {
        if (!state().getBoolean("learned")) {
          wait = 0;
          return;
        }
        bounds();
        shot("metis-matrix");
        var field = dev.heavenlyrestriction.client.MetisScreen.class.getDeclaredField("selected");
        field.setAccessible(
            true); // Select through the actual visible Fire button, then send the real Assign
        // request.
        click("Fire");
        click("Assign");
        phase = 4;
        wait = 0;
        return;
      }
      if (phase == 4) {
        check(state().getList("slots", 10).size() == 1, "Actual Assign request did not persist");
        bounds();
        shot("metis-assigned");
        click("Mode: Default");
        phase = 5;
        wait = 0;
        return;
      }
      if (phase == 5) {
        shot("metis-mode-dropdown");
        mc.screen.keyPressed(256, 0, 0);
        server(
            p -> {
              var i = MetisMatrix.owned(p).orElseThrow();
              i.setMastery(i.getMaxMastery());
              MetisMatrix.assign(p, 1, AspectualMagics.WIND_CUTTER.getId().toString(), 0, 2, true);
              MetisMatrix.assign(p, 2, AspectualMagics.HEALING.getId().toString(), 0, 0, false);
              MetisMatrix.assign(p, 3, AspectualMagics.AGILITY.getId().toString(), 0, 1, false);
              i.markDirty();
              var spell =
                  SkillAPI.getSkillsFrom(p).getSkill(AspectualMagics.FIRE.get()).orElseThrow();
              var provider =
                  new net.minecraft.world.SimpleMenuProvider(
                      (id, inv, owner) -> new net.minecraft.world.inventory.CraftingMenu(id, inv),
                      net.minecraft.network.chat.Component.literal("Review crafting"));
              var beforeMenu = p.containerMenu;
              dev.heavenlyrestriction.splitmind.SplitMindCasting.autoInvoke(
                  p,
                  spell,
                  0,
                  p,
                  () -> {
                    check(p.openMenu(provider).isEmpty(), "Automatic container opened");
                    check(
                        p.openMenu(provider, data -> {}).isEmpty(),
                        "Automatic extended container opened");
                    return true;
                  });
              check(p.containerMenu == beforeMenu, "Background menu changed container");
              check(p.openMenu(provider).isPresent(), "Ordinary manual container blocked");
              p.closeContainer();
              MetisNetwork.open(p);
            });
        phase = 6;
        wait = 0;
        return;
      }
      if (phase == 6) {
        check(
            state().getInt("capacity") == 4 && state().getList("slots", 10).size() == 4,
            "Mastered matrix incomplete");
        bounds();
        shot("metis-mastered-matrix");
        click("All");
        phase = 16;
        wait = 0;
        return;
      }
      if (phase == 16) {
        click("Aspectual");
        phase = 17;
        wait = 0;
        return;
      }
      if (phase == 17) {
        click("Spiritual");
        phase = 18;
        wait = 0;
        return;
      }
      if (phase == 18) {
        check(state().getInt("category") == 3, "Skills filter did not persist");
        click("4 · Agility");
        click("Critical Merchant");
        click("Assign");
        phase = 19;
        wait = 0;
        return;
      }
      if (phase == 19) {
        check(
            state().getList("slots", 10).stream()
                .anyMatch(
                    t ->
                        ((CompoundTag) t).getInt("slot") == 3
                            && ((CompoundTag) t)
                                .getString("id")
                                .equals("heavenlyrestriction:critical_merchant")),
            "Real skill assignment packet failed");
        shot("metis-skill-assignment");
        var spatial =
            state().getList("rows", 10).stream()
                .map(t -> (CompoundTag) t)
                .filter(t -> t.getString("id").equals("tensura:spatial_motion"))
                .findFirst()
                .orElseThrow();
        check(
            spatial.getList("modes", 10).size() == 1
                && spatial.getList("modes", 10).getCompound(0).getInt("id") == 0,
            "Warp menu appeared among assignable modes");
        click("Spatial Motion");
        click("Mode: Blink");
        phase = 20;
        wait = 0;
        return;
      }
      if (phase == 20) {
        shot("metis-safe-skill-modes");
        mc.screen.keyPressed(256, 0, 0);
        mc.options.guiScale().set(4);
        mc.resizeDisplay();
        phase = 7;
        wait = 0;
        return;
      }
      if (phase == 7) {
        bounds();
        shot("metis-compact-matrix");
        click("Synthesis");
        phase = 8;
        wait = 0;
        return;
      }
      if (phase == 8) {
        bounds();
        shot("metis-compact-synthesis");
        mc.options.guiScale().set(guiScale);
        mc.resizeDisplay();
        var nativeMenu =
            new io.github.manasmods.tensura.client.screen.AbilitySelectionScreen(
                io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen.AbilityType.SKILL,
                io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE,
                null);
        mc.setScreen(nativeMenu);
        nativeMenu.selectedAbility =
            SkillAPI.getSkillsFrom(mc.player)
                .getSkill(HeavenlyRestriction.METIS.get())
                .orElseThrow();
        wait = 0;
        phase = 9;
      }
    } catch (Throwable ex) {
      error = ex.toString();
      ex.printStackTrace();
    }
  }
}
