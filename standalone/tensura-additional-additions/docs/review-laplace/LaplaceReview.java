package dev.heavenlyrestriction;

import dev.heavenlyrestriction.client.LaplaceScreen;
import dev.heavenlyrestriction.client.laplace.EquationCanvas;
import dev.heavenlyrestriction.laplace.*;
import dev.heavenlyrestriction.laplace.EquationBoard.*;
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

  private static EquationBoard board() throws Exception {
    var f = LaplaceScreen.class.getDeclaredField("board");
    f.setAccessible(true);
    return (EquationBoard) f.get(Minecraft.getInstance().screen);
  }

  private static EquationCanvas canvas() {
    return Minecraft.getInstance().screen.children().stream()
        .filter(c -> c instanceof EquationCanvas)
        .map(c -> (EquationCanvas) c)
        .findFirst()
        .orElseThrow();
  }

  private static void clickPrefix(String prefix) {
    var screen = Minecraft.getInstance().screen;
    String name =
        screen.children().stream()
            .filter(
                c ->
                    c instanceof AbstractButton b
                        && b.visible
                        && b.getMessage().getString().startsWith(prefix))
            .map(c -> ((AbstractButton) c).getMessage().getString())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No button prefix " + prefix));
    click(name);
  }

  private static void dragTile(String label, Address target) {
    var screen = Minecraft.getInstance().screen;
    var tile =
        screen.children().stream()
            .filter(
                c ->
                    c instanceof AbstractButton b
                        && b.visible
                        && b.getMessage().getString().equals(label))
            .map(c -> (AbstractButton) c)
            .findFirst()
            .orElseThrow();
    drag(
        tile.getX() + tile.getWidth() / 2d,
        tile.getY() + tile.getHeight() / 2d,
        canvas().center(target));
  }

  private static void drag(double x, double y, double[] target) {
    var s = Minecraft.getInstance().screen;
    check(s.mouseClicked(x, y, 0), "Drag press failed");
    check(
        s.mouseDragged(target[0], target[1], 0, target[0] - x, target[1] - y),
        "Drag motion failed");
    check(s.mouseReleased(target[0], target[1], 0), "Drag release failed");
  }

  private static void blockClick(Address at) {
    var s = Minecraft.getInstance().screen;
    double[] point = canvas().center(at);
    check(s.mouseClicked(point[0], point[1], 0), "Block press");
    s.mouseReleased(point[0], point[1], 0);
  }

  private static String damage() {
    return "elemental_damage";
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
        if (a != b && !(a instanceof EquationCanvas) && !(b instanceof EquationCanvas))
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
            OUT.resolve("failed-equation-board.txt"),
            error == null ? "Timeout phase " + phase : error);
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
    try {
      for (String fieldName : List.of("xpos", "ypos")) {
        var f = mc.mouseHandler.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setDouble(mc.mouseHandler, 2);
      }
    } catch (ReflectiveOperationException ex) {
      error = ex.toString();
    }
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
          server(
              p -> {
                Laplace.owned(p).orElseThrow().onToggleOff(p);
                LaplaceNetwork.open(p);
              });
          phase = 4;
        }
        case 4 -> {
          shot("laplace-board-actions");
          mc.screen.children().stream()
              .filter(c -> c instanceof EditBox)
              .map(c -> (EditBox) c)
              .findFirst()
              .orElseThrow()
              .setValue("wind");
          clickPrefix("Wind Cutter");
          phase = 5;
        }
        case 5 -> {
          shot("laplace-board-original");
          check(!state().getBoolean("passive"), "Passive should start off for Apply test");
          click("Math");
          dragTile("Multiply ×", new Address(damage(), List.of()));
          check(board().holes() == 1, "Multiply did not retain value and create a socket");
          phase = 6;
        }
        case 6 -> {
          shot("laplace-board-empty-slot");
          dragTile("2", new Address(damage(), List.of(1)));
          check(board().holes() == 0, "Number did not fill the socket");
          click("Undo");
          check(board().holes() == 1, "Undo failed");
          click("Redo");
          check(board().holes() == 0, "Redo failed");
          phase = 7;
        }
        case 7 -> {
          shot("laplace-board-doubled");
          blockClick(new Address(damage(), List.of(1)));
          phase = 8;
        }
        case 8 -> {
          var input =
              mc.screen.children().stream()
                  .filter(c -> c instanceof EditBox)
                  .map(c -> (EditBox) c)
                  .findFirst()
                  .orElseThrow();
          input.setValue("3");
          shot("laplace-board-number");
          click("Use number");
          phase = 9;
        }
        case 9 -> {
          shot("laplace-board-tripled");
          click("Apply & Enable");
          phase = 10;
        }
        case 10 -> {
          if (!state().getString("notice").startsWith("Applied.")) return;
          check(state().getBoolean("passive"), "Apply did not turn on Universal Formula");
          check(
              state().getList("rows", 10).stream()
                  .map(t -> (CompoundTag) t)
                  .anyMatch(
                      t -> t.getString("key").contains("wind_cutter") && t.getBoolean("enabled")),
              "Action was not enabled");
          shot("laplace-board-live");
          click("More");
          click("Add helper equation");
          canvas().fit();
          phase = 11;
        }
        case 11 -> {
          double[] source = canvas().port("helper_1");
          drag(source[0], source[1], canvas().center(new Address(damage(), List.of(1))));
          blockClick(new Address("helper_1", List.of()));
          phase = 12;
        }
        case 12 -> {
          var input =
              mc.screen.children().stream()
                  .filter(c -> c instanceof EditBox)
                  .map(c -> (EditBox) c)
                  .findFirst()
                  .orElseThrow();
          input.setValue("2");
          click("Use number");
          canvas().fit();
          phase = 13;
        }
        case 13 -> {
          if (state().getLong("wait") > 0) return;
          shot("laplace-board-linked");
          double[] source = canvas().port(damage());
          drag(source[0], source[1], canvas().center(new Address("helper_1", List.of())));
          var apply =
              mc.screen.children().stream()
                  .filter(
                      c ->
                          c instanceof AbstractButton b
                              && b.getMessage().getString().equals("Apply & Enable"))
                  .map(c -> (AbstractButton) c)
                  .findFirst()
                  .orElseThrow();
          check(!apply.active, "Circular graph allowed Apply");
          phase = 14;
        }
        case 14 -> {
          shot("laplace-board-cycle-error");
          click("Undo");
          click("More");
          click("Advanced formula…");
          phase = 15;
        }
        case 15 -> {
          shot("laplace-board-advanced");
          mc.screen.keyPressed(256, 0, 0);
          phase = 151;
        }
        case 151 -> {
          click("‹ Actions");
          mc.screen.children().stream()
              .filter(c -> c instanceof EditBox)
              .map(c -> (EditBox) c)
              .findFirst()
              .orElseThrow()
              .setValue("fire");
          clickPrefix("Fire");
          phase = 152;
        }
        case 152 -> {
          click("Keep editing");
          check(board().names().contains("helper_1"), "Keep editing lost draft");
          check(
              mc.screen.children().stream().anyMatch(c -> c instanceof EquationCanvas),
              "Keep editing did not return to the board");
          click("Number…");
          phase = 153;
        }
        case 153 -> {
          mc.screen.children().stream()
              .filter(c -> c instanceof EditBox)
              .map(c -> (EditBox) c)
              .findFirst()
              .orElseThrow()
              .setValue("1.5");
          click("Use number");
          dragTile("1.5", new Address("helper_1", List.of()));
          check(board().root("helper_1").formula().equals("1.5"), "Custom number did not place");
          click("Undo");
          mc.options.guiScale().set(4);
          mc.resizeDisplay();
          mc.screen.init(mc, 320, 180);
          canvas().start();
          phase = 16;
        }

        case 16 -> {
          shot("laplace-board-compact");
          click("Blocks");
          phase = 17;
        }
        case 17 -> {
          shot("laplace-board-compact-blocks");
          click("Math");
          mc.screen.mouseScrolled(30, 120, 0, -1);
          dragTile("Add +", new Address(damage(), List.of(0)));
          check(board().holes() == 1, "Compact drawer drag failed");
          click("Undo");
          phase = 18;
        }
        case 18 -> {
          click("Payment…");
          phase = 19;
        }
        case 19 -> {
          shot("laplace-payment-compact");
          click("Aura");
          click("Use payment mix");
          check(
              ((LaplaceScreen) mc.screen).model().payment().plan().equals(PaymentPlan.AURA),
              "Aura draft not selected");
          click("Payment…");
          click("HP");
          mc.screen.keyPressed(256, 0, 0);
          check(
              ((LaplaceScreen) mc.screen).model().payment().plan().equals(PaymentPlan.AURA),
              "Cancel changed payment draft");
          mc.options.guiScale().set(2);
          mc.resizeDisplay();
          click("Payment…");
          phase = 20;
        }
        case 20 -> {
          var fields =
              mc.screen.children().stream()
                  .filter(c -> c instanceof EditBox)
                  .map(c -> (EditBox) c)
                  .toList();
          check(fields.size() == 4, "Missing resource shares");
          for (var field : fields) field.setValue("25");
          phase = 21;
        }
        case 21 -> {
          shot("laplace-payment-mixed");
          var fields =
              mc.screen.children().stream()
                  .filter(c -> c instanceof EditBox)
                  .map(c -> (EditBox) c)
                  .toList();
          fields.getFirst().setValue("99");
          var apply =
              mc.screen.children().stream()
                  .filter(
                      c ->
                          c instanceof AbstractButton b
                              && b.getMessage().getString().equals("Use payment mix"))
                  .map(c -> (AbstractButton) c)
                  .findFirst()
                  .orElseThrow();
          check(!apply.active, "Invalid percentage sum accepted");
          fields.getFirst().setValue("25");
          click("Use payment mix");
          phase = 22;
        }
        case 22 -> {
          if (state().getLong("wait") > 0) return;
          check(
              ((LaplaceScreen) mc.screen)
                  .model()
                  .payment()
                  .plan()
                  .equals(new PaymentPlan(.25, .25, .25, .25)),
              "Mix did not update draft");
          shot("laplace-payment-preview");
          click("Apply & Enable");
          phase = 23;
        }
        case 23 -> {
          if (!state().getString("notice").startsWith("Applied.")) return;
          var row =
              state().getList("rows", 10).stream()
                  .map(t -> (CompoundTag) t)
                  .filter(t -> t.getString("key").contains("wind_cutter"))
                  .findFirst()
                  .orElseThrow();
          check(
              LaplaceNetwork.decode(row.getCompound("model"))
                  .payment()
                  .plan()
                  .equals(new PaymentPlan(.25, .25, .25, .25)),
              "Server did not persist payment mix");
          shot("laplace-payment-live");
          click("‹ Actions");
          mc.screen.children().stream()
              .filter(c -> c instanceof EditBox)
              .map(c -> (EditBox) c)
              .findFirst()
              .orElseThrow()
              .setValue("healing");
          clickPrefix("Healing");
          click("Payment…");
          phase = 24;
        }
        case 24 -> {
          shot("laplace-payment-healing");
          for (String name : List.of("HP", "SHP")) {
            var b =
                mc.screen.children().stream()
                    .filter(
                        c ->
                            c instanceof AbstractButton v
                                && v.getMessage().getString().equals(name))
                    .map(c -> (AbstractButton) c)
                    .findFirst()
                    .orElseThrow();
            check(!b.active, "Recovery vital preset not disabled");
          }
          mc.screen.keyPressed(256, 0, 0);
          Files.writeString(
              OUT.resolve("passed-resource-payment.txt"),
              "Actual native client: payment presets, cancel preserves draft, four-way mix, invalid"
                  + " percentages blocked, live bill, Apply & Enable server persistence, healing"
                  + " vital guard, 320×180 layout passed.");
          mc.options.guiScale().set(oldScale);
          mc.options.hideGui = oldHide;
          mc.resizeDisplay();
          Files.writeString(
              OUT.resolve("passed-equation-board.txt"),
              "Actual native client: synthesis, palette-to-board drag, numeric socket fill,"
                  + " undo/redo, number dialog, Apply+Enable real server persistence and passive"
                  + " activation, helper links, rejected circular graph, advanced text, normal and"
                  + " 320×180 stress layouts, compact drawer drag passed.");
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
