package dev.heavenlyrestriction;

import dev.heavenlyrestriction.client.LaplaceScreen;
import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.magic.AspectualMagics;
import io.github.manasmods.tensura.registry.skill.*;
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

/** Opt-in real-client interaction test, restricted to the disposable review save. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class LaplaceReview {
  private static final Path OUT = Path.of("/workspace/validation-wayfarer/capture");
  private static int phase, age, wait, oldScale;
  private static volatile String error;
  private static boolean oldHide;

  private static void check(boolean ok, String message) {
    if (!ok) throw new IllegalStateException(message);
  }

  private static void server(java.util.function.Consumer<ServerPlayer> work) {
    var s = Minecraft.getInstance().getSingleplayerServer();
    s.execute(
        () -> {
          try {
            work.accept(s.getPlayerList().getPlayers().getFirst());
          } catch (Throwable ex) {
            error = ex.toString();
            ex.printStackTrace();
          }
        });
  }

  private static void learn(ServerPlayer p, ManasSkill skill) {
    RestrictionData.learnReward(p, skill);
    var i = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();
    i.setMastery(i.getMaxMastery());
    i.markDirty();
  }

  private static CompoundTag state() throws Exception {
    var f = LaplaceScreen.class.getDeclaredField("state");
    f.setAccessible(true);
    return (CompoundTag) f.get(Minecraft.getInstance().screen);
  }

  private static void click(String label) {
    var s = Minecraft.getInstance().screen;
    var b =
        s.children().stream()
            .filter(
                c ->
                    c instanceof AbstractButton w
                        && w.visible
                        && w.getMessage().getString().equals(label))
            .map(c -> (AbstractButton) c)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing button " + label));
    check(b.active, "Inactive " + label);
    check(
        s.mouseClicked(b.getX() + b.getWidth() / 2d, b.getY() + b.getHeight() / 2d, 0),
        "Click failed " + label);
  }

  private static void bounds() {
    var s = Minecraft.getInstance().screen;
    var widgets =
        s.children().stream()
            .filter(c -> c instanceof AbstractWidget w && w.visible)
            .map(c -> (AbstractWidget) c)
            .toList();
    for (var a : widgets) {
      check(
          a.getX() >= 0 && a.getY() >= 0 && a.getRight() <= s.width && a.getBottom() <= s.height,
          "Offscreen widget " + a.getMessage());
      for (var b : widgets)
        if (a != b)
          check(
              a.getRight() <= b.getX()
                  || a.getX() >= b.getRight()
                  || a.getBottom() <= b.getY()
                  || a.getY() >= b.getBottom(),
              "Overlapping widgets");
    }
  }

  private static void shot(String name) throws Exception {
    if (Minecraft.getInstance().screen instanceof LaplaceScreen) bounds();
    Files.createDirectories(OUT);
    try (var im = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      im.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    if (++age > 5000 || error != null) {
      try {
        Files.createDirectories(OUT);
        Files.writeString(
            OUT.resolve("failed-laplace.txt"), error == null ? "Timeout phase " + phase : error);
      } catch (Exception ignored) {
      }
      mc.stop();
      return;
    }
    if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
    if (!mc.getSingleplayerServer()
        .getWorldPath(LevelResource.ROOT)
        .toString()
        .contains("Wayfarer Review")) {
      error = "Refusing non-review save";
      return;
    }
    mc.options.pauseOnLostFocus = false;
    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 2, 2);
    if (++wait < 25) return;
    try {
      switch (phase) {
        case 0 -> {
          oldScale = mc.options.guiScale().get();
          oldHide = mc.options.hideGui;
          mc.options.guiScale().set(2);
          mc.options.hideGui = true;
          mc.resizeDisplay();
          server(
              p -> {
                var store = SkillAPI.getSkillsFrom(p);
                for (var i : List.copyOf(store.getLearnedSkills()))
                  if (i.getSkill() == HeavenlyRestriction.LAPLACE.get()
                      || i.getSkill() == HeavenlyRestriction.EXPONENTIATION.get()
                      || i.getSkill() == UniqueSkills.MATHEMATICIAN.get()
                      || i.getSkill() instanceof io.github.manasmods.tensura.ability.magic.Magic)
                    store.forgetSkill(i);
                EnergyHelper.setBaseMaxEP(p, 20000000);
                TensuraStorages.getExistenceFrom(p).setTrueHero(true);
                TensuraStorages.getExistenceFrom(p).setMagicule(EnergyHelper.getMaxMagicule(p));
                learn(p, HeavenlyRestriction.EXPONENTIATION.get());
                learn(p, UniqueSkills.MATHEMATICIAN.get());
                for (var s :
                    List.of(
                        CommonSkills.SELF_REGENERATION.get(),
                        AspectualMagics.FIRE.get(),
                        AspectualMagics.WIND_CUTTER.get(),
                        AspectualMagics.WATER_CUTTER.get(),
                        AspectualMagics.HEALING.get(),
                        AspectualMagics.STRENGTH.get(),
                        AspectualMagics.PROTECTION.get(),
                        AspectualMagics.REINFORCEMENT.get())) learn(p, s);
                LaplaceSynthesis.discover(p);
                ((io.github.manasmods.manascore.storage.api.StorageHolder) p).manasCore$sync();
                LaplaceNetwork.open(p);
              });
          phase = 1;
        }
        case 1 -> {
          if (!(mc.screen instanceof LaplaceScreen)) return;
          check(state().getBoolean("ready"), "Synthesis not ready");
          shot("laplace-synthesis");
          click("Synthesize Laplace");
          phase = 2;
        }
        case 2 -> {
          shot("laplace-confirmation");
          click("Confirm permanent payment");
          phase = 3;
        }
        case 3 -> {
          if (!state().getBoolean("learned")) return;
          click("Continue editing");
          server(
              p -> {
                Laplace.owned(p).orElseThrow().onToggleOn(p);
                LaplaceNetwork.open(p);
              });
          phase = 4;
        }
        case 4 -> {
          shot("laplace-actions");
          var title =
              state().getList("rows", 10).stream()
                  .map(t -> (CompoundTag) t)
                  .filter(t -> t.getString("key").contains("self_regeneration"))
                  .findFirst()
                  .orElseThrow()
                  .getString("title");
          click("◇ " + title);
          phase = 5;
        }
        case 5 -> {
          shot("laplace-editor");
          var input =
              mc.screen.children().stream()
                  .filter(c -> c instanceof EditBox)
                  .map(c -> (EditBox) c)
                  .findFirst()
                  .orElseThrow();
          input.setValue("base_healing * 2");
          click("Disabled");
          click("Balance cost");
          click("Save");
          phase = 6;
        }
        case 6 -> {
          check(
              state().getString("notice").startsWith("Saved."),
              "Actual save did not persist: " + state().getString("notice"));
          click("Continue editing");
          phase = 7;
        }
        case 7 -> {
          shot("laplace-saved");
          var selector =
              mc.screen.children().stream()
                  .filter(
                      c ->
                          c instanceof AbstractButton b
                              && b.getMessage().getString().startsWith("HP per pulse"))
                  .map(c -> (AbstractButton) c)
                  .findFirst()
                  .orElseThrow();
          click(selector.getMessage().getString());
          phase = 12;
        }
        case 12 -> {
          shot("laplace-aspects");
          var cost =
              mc.screen.children().stream()
                  .filter(
                      c ->
                          c instanceof AbstractButton b
                              && b.getMessage().getString().startsWith("Magicule cost ="))
                  .map(c -> (AbstractButton) c)
                  .findFirst()
                  .orElseThrow();
          click(cost.getMessage().getString());
          phase = 13;
        }
        case 13 -> {
          shot("laplace-cost");
          click("3  Parameters");
          phase = 8;
        }
        case 8 -> {
          shot("laplace-parameters");
          click("Back to equation");
          click("+");
          phase = 9;
        }
        case 9 -> {
          shot("laplace-helper");
          mc.options.guiScale().set(4);
          mc.resizeDisplay();
          mc.screen.init(mc, 320, 180);
          phase = 10;
        }
        case 10 -> {
          check(mc.screen.width == 320 && mc.screen.height == 180, "Unexpected compact size");
          shot("laplace-compact-editor");
          click("3  Parameters");
          phase = 11;
        }
        case 11 -> {
          shot("laplace-compact-parameters");
          mc.options.guiScale().set(oldScale);
          mc.options.hideGui = oldHide;
          mc.resizeDisplay();
          Files.writeString(
              OUT.resolve("passed-laplace.txt"),
              "Real synthesis confirmation and server payment, native action catalogue, editing and"
                  + " Balance cost, enabled Save packet and persisted profile, parameter palette,"
                  + " helper equation, normal and 320×180 bounds passed.");
          var menu =
              new io.github.manasmods.tensura.client.screen.AbilitySelectionScreen(
                  io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen.AbilityType
                      .SKILL,
                  io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE,
                  null);
          mc.setScreen(menu);
          menu.selectedAbility =
              SkillAPI.getSkillsFrom(mc.player)
                  .getSkill(HeavenlyRestriction.LAPLACE.get())
                  .orElseThrow();
          phase = 14;
        }
        case 14 -> {
          shot("laplace-ultimate-menu");
          mc.stop();
          phase = 99;
        }
        default -> {}
      }
      wait = 0;
    } catch (Throwable ex) {
      error = ex.toString();
      ex.printStackTrace();
    }
  }
}
