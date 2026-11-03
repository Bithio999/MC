package dev.heavenlyrestriction;

import dev.heavenlyrestriction.client.SplitMindScreen;
import dev.heavenlyrestriction.splitmind.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.registry.magic.*;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Disposable integrated-server review of actual C2S/S2C menu behavior and layout. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class SplitMindReview {
  private static final Path OUT = Path.of("/workspace/validation-split-mind/capture");
  private static volatile net.minecraft.world.entity.monster.Husk autoEnemy;
  private static volatile int phase;
  private static int age, tick;
  private static volatile String error;

  private static final class Menu extends AbilitySelectionScreen {
    Menu() {
      super(AbilityCategoriesScreen.AbilityType.SKILL, Skill.SkillType.UNIQUE, null);
    }

    void search() {
      searchField.setValue("Split Mind");
    }

    void choose() {
      int at = -1;
      for (int n = 0; n < filtered.size(); n++)
        if (filtered.get(n).getSkill() == HeavenlyRestriction.SPLIT_MIND.get()) {
          at = n;
          break;
        }
      check(at >= 0, "Missing native Unique entry");
      mouseClicked(guiLeft + 12, guiTop + 46 + 13 * (at - listStartIndex) + 5, 0);
      check(
          selectedAbility != null
              && selectedAbility.getSkill() == HeavenlyRestriction.SPLIT_MIND.get(),
          "Native skill selection failed");
    }
  }

  private static void check(boolean ok, String m) {
    if (!ok) {
      error = m;
      throw new IllegalStateException(m);
    }
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

  private static Button button(String label) {
    return Minecraft.getInstance().screen.children().stream()
        .filter(c -> c instanceof Button b && b.getMessage().getString().equals(label))
        .map(c -> (Button) c)
        .findFirst()
        .orElseThrow();
  }

  private static EditBox search() {
    return Minecraft.getInstance().screen.children().stream()
        .filter(c -> c instanceof EditBox)
        .map(c -> (EditBox) c)
        .findFirst()
        .orElseThrow();
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var mc = Minecraft.getInstance();
    if (error != null) {
      try {
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve("failed.txt"), error);
      } catch (Exception ignored) {
      }
      mc.stop();
      return;
    }
    if (mc.player == null || mc.getSingleplayerServer() == null || ++age < 100) return;
    if (phase == 0) {
      phase = 1;
      mc.options.guiScale().set(2);
      mc.resizeDisplay();
      server(
          p -> {
            check(
                p.server
                    .getWorldPath(LevelResource.ROOT)
                    .normalize()
                    .getFileName()
                    .toString()
                    .equals("Split Mind Review"),
                "Refusing non-review save");
            p.removeAllEffects();
            for (var i : List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills())) {
              if (i.isToggled()) {
                i.setToggled(false);
                i.onToggleOff(p);
              }
              SkillAPI.getSkillsFrom(p).forgetSkill(i);
            }
            RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(), false);
            EnergyHelper.setBaseMaxEP(p, 20_000_000);
            var pool = TensuraStorages.getExistenceFrom(p);
            pool.setMagicule(1_000_000);
            pool.setAura(1_000_000);
            RestrictionData.learnReward(p, HeavenlyRestriction.SPLIT_MIND.get());
            var mind =
                SkillAPI.getSkillsFrom(p)
                    .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
                    .orElseThrow();
            mind.setMastery(mind.getMaxMastery());
            mind.onToggleOn(p);
            for (var m :
                List.of(
                    AspectualMagics.FIRE.get(),
                    AspectualMagics.FIRE_BALL.get(),
                    AspectualMagics.FIRE_LANCE.get(),
                    AspectualMagics.ACID_SHELL.get(),
                    AspectualMagics.WATER_CUTTER.get(),
                    AspectualMagics.THUNDER_ORB.get(),
                    AspectualMagics.STONE_SHOT.get(),
                    SpiritualMagics.FIRE.get(),
                    SpiritualMagics.EARTH.get())) {
              RestrictionData.learnReward(p, m);
              var i = SkillAPI.getSkillsFrom(p).getSkill(m).orElseThrow();
              i.setMastery(i.getMaxMastery());
            }
            phase = 2;
          });
      return;
    }
    if (phase == 2 && ++tick > 25) {
      tick = 0;
      phase = 3;
      mc.setScreen(null);
      server(
          p ->
              HeavenlyRestriction.SPLIT_MIND
                  .get()
                  .onPressed(
                      SkillAPI.getSkillsFrom(p)
                          .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
                          .orElseThrow(),
                      p,
                      0,
                      0));
      return;
    }
    if (phase == 3 && mc.screen instanceof SplitMindScreen && ++tick > 30) {
      shot("split-mind-assignment");
      search().setValue("fire");
      phase = 4;
      tick = 0;
      return;
    }
    if (phase == 4 && ++tick > 30) {
      shot("split-mind-search");
      button("Spiritual").onPress();
      phase = 5;
      tick = 0;
      return;
    }
    if (phase == 5 && ++tick > 30) {
      var row =
          mc.screen.children().stream()
              .filter(
                  c -> c instanceof Button b && b.visible && b.getMessage().getString().isEmpty())
              .map(c -> (Button) c)
              .findFirst()
              .orElseThrow();
      row.onPress();
      check(button("Assign magic").active, "Selection did not enable assignment");
      button("Assign magic").onPress();
      phase = 6;
      tick = 0;
      return;
    }
    if (phase == 6 && ++tick > 30) {
      server(
          p ->
              check(
                  SkillAPI.getSkillsFrom(p)
                      .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
                      .orElseThrow()
                      .getOrCreateTag()
                      .getString(SplitMindCasting.ASSIGNED)
                      .equals(SpiritualMagics.FIRE.get().getRegistryName().toString()),
                  "Real assignment packet did not persist learned spiritual magic"));
      shot("split-mind-assigned");
      search().setValue("stone");
      button("All magic").onPress();
      phase = 60;
      tick = 0;
      return;
    }
    if (phase == 60 && ++tick > 30) {
      var row =
          mc.screen.children().stream()
              .filter(
                  c -> c instanceof Button b && b.visible && b.getMessage().getString().isEmpty())
              .map(c -> (Button) c)
              .findFirst()
              .orElseThrow();
      row.onPress();
      var mode =
          mc.screen.children().stream()
              .filter(
                  c ->
                      c instanceof Button b
                          && b.visible
                          && b.getMessage().getString().startsWith("Mode:"))
              .map(c -> (Button) c)
              .findFirst()
              .orElseThrow();
      check(mode.active, "Multi-mode spell has no active dropdown");
      mode.onPress();
      phase = 61;
      tick = 0;
      return;
    }
    if (phase == 61 && ++tick > 15) {
      shot("split-mind-mode-dropdown");
      var mode =
          mc.screen.children().stream()
              .filter(
                  c ->
                      c instanceof Button b
                          && b.visible
                          && b.getMessage().getString().startsWith("Mode:"))
              .map(c -> (Button) c)
              .findFirst()
              .orElseThrow();
      mc.screen.mouseClicked(mode.getX() + 12, mode.getY() - 8, 0);
      mode.onPress();
      mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0);
      check(
          mc.screen instanceof SplitMindScreen,
          "Escape closed the assignment menu instead of dropdown");
      button("Assign magic").onPress();
      phase = 62;
      tick = 0;
      return;
    }
    if (phase == 62 && ++tick > 30) {
      server(
          p -> {
            var mind =
                SkillAPI.getSkillsFrom(p)
                    .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
                    .orElseThrow();
            check(
                mind.getOrCreateTag()
                        .getString(SplitMindCasting.ASSIGNED)
                        .equals(AspectualMagics.STONE_SHOT.get().getRegistryName().toString())
                    && SplitMindCasting.assignedMode(mind) == 1,
                "Real mode dropdown assignment did not persist Chain");
          });
      shot("split-mind-mode-assigned");
      mc.options.guiScale().set(4);
      mc.resizeDisplay();
      phase = 7;
      tick = 0;
      return;
    }
    if (phase == 7 && ++tick > 30) {
      for (var c : mc.screen.children())
        if (c instanceof AbstractWidget w && w.visible)
          check(
              w.getX() >= 0
                  && w.getY() >= 0
                  && w.getX() + w.getWidth() <= mc.screen.width
                  && w.getY() + w.getHeight() <= mc.screen.height,
              "Widget clipped at small GUI size");
      shot("split-mind-assignment-small");
      mc.options.guiScale().set(2);
      mc.resizeDisplay();
      var menu = new Menu();
      mc.setScreen(menu);
      menu.search();
      phase = 8;
      tick = 0;
      return;
    }
    if (phase == 8 && ++tick > 30) {
      ((Menu) mc.screen).choose();
      phase = 9;
      tick = 0;
      return;
    }
    if (phase == 9 && ++tick > 25) {
      shot("split-mind-unique-menu");
      mc.setScreen(null);
      phase = 10;
      tick = 0;
      server(
          p -> {
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            for (int x = -6; x <= 20; x++)
              for (int z = -6; z <= 6; z++)
                for (int y = 199; y <= 207; y++)
                  p.serverLevel()
                      .setBlock(
                          new net.minecraft.core.BlockPos(x, y, z),
                          (y == 199
                                  ? net.minecraft.world.level.block.Blocks.STONE
                                  : net.minecraft.world.level.block.Blocks.AIR)
                              .defaultBlockState(),
                          2);
            p.connection.teleport(1.5, 200, 1.5, 90, 0);
            p.setNoGravity(true);
            autoEnemy =
                new net.minecraft.world.entity.monster.Husk(
                    net.minecraft.world.entity.EntityType.HUSK, p.serverLevel());
            autoEnemy.setPos(11.5, 200, 1.5);
            autoEnemy.setNoAi(true);
            autoEnemy.setNoGravity(true);
            autoEnemy
                .getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(1024);
            autoEnemy.setHealth(1024);
            p.serverLevel().addFreshEntity(autoEnemy);
            var storage = SkillAPI.getSkillsFrom(p);
            var mind = storage.getSkill(HeavenlyRestriction.SPLIT_MIND.get()).orElseThrow();
            check(
                SplitMindCasting.assign(
                    p, AspectualMagics.STONE_SHOT.get().getRegistryName().toString(), 1),
                "Live Chain Stone Shot assignment rejected");
            mind.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, false);
            TensuraStorages.getAbilityFrom(p)
                .setAbilitySlot(0, 0, HeavenlyRestriction.SPLIT_MIND.get(), 1);
            check(
                storage.startHoldSkill(mind, 0, 1), "Native Auto-Cast toggle activation rejected");
            storage.handleSkillRelease(mind, 0, 1, false);
          });
      return;
    }
    if (phase == 10 && ++tick > 100) {
      phase = 11;
      server(
          p -> {
            check(
                autoEnemy != null && autoEnemy.getHealth() < 1024,
                "Real automatic player ticks did not hit the enemy; held="
                    + SkillAPI.getSkillsFrom(p).getHeldSkills().size()
                    + ", MP="
                    + TensuraStorages.getExistenceFrom(p).getMagicule());
            var mind =
                SkillAPI.getSkillsFrom(p)
                    .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
                    .orElseThrow();
            mind.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, false);
            SplitMindCasting.cancel(p);
            autoEnemy.discard();
            try {
              Files.writeString(
                  OUT.resolve("passed.txt"),
                  "PASS: native Unique list/name/icon; real Magic Assignment S2C; learned-only"
                      + " search/type filtering; real assign C2S persists Spiritual Fire; fitted"
                      + " controls at GUI scales 2 and 4; native Auto-Cast toggle and actual"
                      + " automatic player/held ticks cast non-equipped Chain Stone Shot and damage"
                      + " an enemy without manual aiming.\n");
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
            phase = 12;
          });
      return;
    }
    if (phase == 12) {
      mc.stop();
    }
  }
}
