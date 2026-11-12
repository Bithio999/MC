package dev.heavenlyrestriction;

import dev.heavenlyrestriction.critical.CriticalMerchant;
import dev.heavenlyrestriction.tyche.*;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class TycheReview {
  static final Path OUT = Path.of("/workspace/validation-tyche");
  static int phase, age, wait, guiScale;
  static volatile boolean ready;
  static volatile String error;
  static double mp;

  static void check(boolean ok, String text) {
    if (!ok) throw new IllegalStateException(text);
  }

  static void server(java.util.function.Consumer<ServerPlayer> task) {
    ready = false;
    Minecraft.getInstance()
        .getSingleplayerServer()
        .execute(
            () -> {
              try {
                task.accept(
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

  static void look(ServerPlayer p, Vec3 eye, Vec3 target) {
    var d = target.subtract(eye.add(0, p.getEyeHeight(), 0));
    float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90,
        pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
    p.teleportTo(p.server.overworld(), eye.x, eye.y, eye.z, Set.of(), yaw, pitch);
  }

  static void shot(String name) throws Exception {
    Files.createDirectories(OUT);
    try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      image.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  static void click(String name) {
    var s = Minecraft.getInstance().screen;
    var b =
        s.children().stream()
            .filter(c -> c instanceof AbstractButton a && a.getMessage().getString().equals(name))
            .map(c -> (AbstractButton) c)
            .findFirst()
            .orElseThrow();
    check(b.active, "Disabled " + name);
    // Native SimpleButton uses its last rendered hover state for click acceptance.
    int mx = b.getX() + b.getWidth() / 2, my = b.getY() + 8;
    b.render(
        new net.minecraft.client.gui.GuiGraphics(
            Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource()),
        mx,
        my,
        0);
    check(s.mouseClicked(mx, my, 0), "Click not handled " + name);
    s.mouseReleased(mx, my, 0);
  }

  static void bounds() {
    var s = Minecraft.getInstance().screen;
    for (var c : s.children())
      if (c instanceof AbstractWidget w)
        check(
            w.getX() >= 0
                && w.getY() >= 0
                && w.getX() + w.getWidth() <= s.width
                && w.getY() + w.getHeight() <= s.height,
            "Widget outside screen");
  }

  @SubscribeEvent
  public static void hud(RenderGuiEvent.Pre e) {
    if (Minecraft.getInstance().options.hideGui) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    var mc = Minecraft.getInstance();
    mc.options.pauseOnLostFocus = false;
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
    if (++wait < (phase == 6 ? 8 : 50)) return;
    try {
      if (phase == 0) {
        guiScale = mc.options.guiScale().get();
        server(
            p -> {
              var level = p.server.overworld();
              p.setGameMode(GameType.SURVIVAL);
              // Earlier visual previews used the entity invulnerability flag; creative mode does
              // not clear that separately persisted flag in this disposable review save.
              p.setInvulnerable(false);
              p.getAbilities().mayfly = true;
              p.getAbilities().flying = true;
              p.onUpdateAbilities();
              p.removeAllEffects();
              p.getInventory().clearContent();
              for (var i : List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))
                SkillAPI.getSkillsFrom(p).forgetSkill(i);
              RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(), false);
              p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(8_000_000);
              p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(2_000_000);
              var energy = TensuraStorages.getExistenceFrom(p);
              energy.setTrueHero(true);
              energy.setAura(1_000_000);
              energy.setMagicule(1_000_000);
              energy.setSpiritualHealth(
                  p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));
              p.setHealth(p.getMaxHealth());
              for (int x = -4045; x <= -3995; x++)
                for (int z = -4105; z <= -4055; z++)
                  level.setBlock(
                      new BlockPos(x, 100, z),
                      (x == -4045 || x == -3995 || z == -4105 || z == -4055
                              ? Blocks.POLISHED_DEEPSLATE
                              : Blocks.LIGHT_GRAY_CONCRETE)
                          .defaultBlockState(),
                      2);
              look(p, new Vec3(-4020, 101, -4080), new Vec3(-4020, 102, -4075));
              level.setDayTime(6000);
              level.setWeatherParameters(999999, 0, false, false);
              level.getWorldBorder().setWarningBlocks(0);
              for (var skill :
                  List.of(HeavenlyRestriction.CRITICAL_MERCHANT.get(), UniqueSkills.TUNER.get())) {
                RestrictionData.learnReward(p, skill);
                var i = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();
                i.setMastery(i.getMaxMastery());
                i.setToggled(true);
              }
              RestrictionData.of(p).putInt(TycheSynthesis.CRITS, TycheConfig.CRITICALS.get());
              for (int n = 0; n < TycheConfig.SURVIVALS.get(); n++)
                TycheSynthesis.survived(p, "review_type_" + n);
              TycheSynthesis.discover(p);
              check(TycheSynthesis.ready(p), "Synthesis not ready");
              mp = p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
              TycheNetwork.open(p);
            });
        phase = 1;
        wait = 0;
        return;
      }
      if (!ready) return;
      if (phase == 1) {
        if (!(mc.screen instanceof dev.heavenlyrestriction.client.TycheScreen)) return;
        mc.options.hideGui = true;
        mc.getToasts().clear();
        bounds();
        shot("tyche-synthesis");
        mc.options.guiScale().set(4);
        mc.resizeDisplay();
        phase = 11;
        wait = 0;
        return;
      }
      if (phase == 11) {
        bounds();
        shot("tyche-synthesis-compact");
        mc.options.guiScale().set(guiScale);
        mc.resizeDisplay();
        phase = 12;
        wait = 0;
        return;
      }
      if (phase == 12) {
        shot("tyche-synthesis");
        click("Review synthesis");
        check(
            mc.screen.children().stream()
                .anyMatch(
                    c ->
                        c instanceof AbstractButton b
                            && b.getMessage().getString().equals("Confirm synthesis")),
            "Review did not open confirmation");
        phase = 2;
        wait = 0;
        return;
      }
      if (phase == 2) {
        check(
            mc.screen.children().stream()
                .anyMatch(
                    c ->
                        c instanceof AbstractButton b
                            && b.getMessage().getString().equals("Confirm synthesis")),
            "Heartbeat canceled confirmation");
        shot("tyche-confirmation");
        click("Confirm synthesis");
        phase = 3;
        wait = 0;
        return;
      }
      if (phase == 3) {
        shot("tyche-insurance-menu");
        mc.setScreen(null);
        server(
            p -> {
              check(Tyche.owned(p).isPresent(), "No Tyche after purchase");
              check(
                  Math.abs(
                          mp
                              - p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue()
                              - TycheConfig.ACQUISITION.get())
                      < .01,
                  "Wrong charge");
              var i = Tyche.owned(p).orElseThrow();
              CriticalMerchant.press(i, p);
              check(CriticalMerchant.remaining(i) == 10, "Dividend failed");
              p.setHealth(p.getMaxHealth() * .5f);
              Tyche.checkOdds(p);
              check(
                  Tyche.oddsTicks(i) > 0 && i.getCoolDown(1) == TycheConfig.ODDS_COOLDOWN.get(),
                  "Timed Against All Odds failed");
              p.setHealth(p.getMaxHealth());
              Tyche.refresh(p);
              check(Tyche.oddsTicks(i) > 0, "Healing canceled buff");
            });
        mc.options.hideGui = true;
        phase = 4;
        wait = 0;
        return;
      }
      if (phase == 4) {
        shot("tyche-dividend-first-person");
        server(TycheNetwork::open);
        phase = 14;
        wait = 0;
        return;
      }
      if (phase == 14) {
        bounds();
        shot("tyche-odds-status");
        mc.setScreen(null);
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        server(p -> p.connection.teleport(p.getX(), p.getY(), p.getZ(), p.getYRot(), 28));
        phase = 5;
        wait = 0;
        return;
      }
      if (phase == 5) {
        shot("tyche-odds");
        server(
            p -> {
              var i = Tyche.owned(p).orElseThrow();
              p.setHealth(1);
              var energy = TensuraStorages.getExistenceFrom(p);
              energy.setAura(1);
              energy.setMagicule(1);
              energy.setSpiritualHealth(1);
              p.invulnerableTime = 0;
              check(!p.isInvulnerableTo(p.damageSources().generic()), "Review player is protected");
              boolean hit = p.hurt(p.damageSources().generic(), 100_000_000);
              check(
                  p.isAlive() && p.getHealth() == p.getMaxHealth(),
                  "Real player failed revival: hit="
                      + hit
                      + " hp="
                      + p.getHealth()
                      + " max="
                      + p.getMaxHealth()
                      + " timer="
                      + i.getCoolDown(2)
                      + " interact="
                      + i.canInteractSkill(p)
                      + " tag="
                      + i.getOrCreateTag());
              check(
                  i.getCoolDown(2) > 0 && i.getCoolDown(0) == 0 && i.getCoolDown(1) == 0,
                  "Revival cooldown reset incorrect");
              check(
                  energy.getAura() == io.github.manasmods.tensura.util.EnergyHelper.getMaxAura(p)
                      && energy.getMagicule()
                          == io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(p)
                      && energy.getSpiritualHealth()
                          == p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),
                  "Real player resources not restored");
            });
        phase = 6;
        wait = 0;
        return;
      }
      if (phase == 6) {
        shot("tyche-revival");
        server(TycheNetwork::open);
        phase = 7;
        wait = 0;
        return;
      }
      if (phase == 7) {
        bounds();
        shot("tyche-insurance-history");
        var nativeMenu =
            new io.github.manasmods.tensura.client.screen.AbilitySelectionScreen(
                io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen.AbilityType.SKILL,
                io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE,
                null);
        mc.setScreen(nativeMenu);
        nativeMenu.selectedAbility =
            SkillAPI.getSkillsFrom(mc.player)
                .getSkill(HeavenlyRestriction.TYCHE.get())
                .orElseThrow();
        phase = 8;
        wait = 0;
        return;
      }
      if (phase == 8) {
        shot("tyche-native-ultimate-menu");
        Files.writeString(
            OUT.resolve("passed.txt"),
            "Actual client: synthesis, compact bounds, stable confirmation heartbeat, real cost,"
                + " Dividend, timed Against All Odds and full player revival/cooldown reset"
                + " passed.\n");
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.hideGui = false;
        mc.options.guiScale().set(guiScale);
        mc.resizeDisplay();
        mc.stop();
        phase = 9;
      }
    } catch (Throwable e) {
      error = e.toString();
      e.printStackTrace();
    }
  }
}
