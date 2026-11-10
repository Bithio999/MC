package dev.heavenlyrestriction;

import dev.heavenlyrestriction.forge.*;
import dev.heavenlyrestriction.highrealm.HighRealm;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.tensura.ability.skill.Skill.SkillType;
import io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen.AbilityType;
import io.github.manasmods.tensura.client.screen.AbilitySelectionScreen;
import io.github.manasmods.tensura.entity.human.golem.TrainingDummyEntity;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.block.TensuraBlocks;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
public final class HephaestusReview {
  private static final Path OUT = Path.of("/workspace/validation-wayfarer/capture");
  private static int phase, wait, age;
  private static volatile int acceptedHits;
  private static volatile String error;
  private static volatile boolean initialized, converted, equipped, damaged, recalled;
  private static volatile TrainingDummyEntity dummy;

  private static void check(boolean value, String message) {
    if (!value) throw new IllegalStateException(message);
  }

  private static void server(java.util.function.Consumer<ServerPlayer> task) {
    var s = Minecraft.getInstance().getSingleplayerServer();
    s.execute(
        () -> {
          try {
            task.accept(s.getPlayerList().getPlayers().getFirst());
          } catch (Throwable e) {
            error = e.toString();
            e.printStackTrace();
          }
        });
  }

  private static void shot(String name) throws Exception {
    try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      Files.createDirectories(OUT);
      image.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  private static void click(String label) {
    var mc = Minecraft.getInstance();
    var button =
        mc.screen.children().stream()
            .filter(
                w ->
                    w instanceof AbstractButton b
                        && b.getMessage() != null
                        && b.getMessage().getString().equals(label))
            .map(w -> (AbstractButton) w)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Button not found: " + label));
    check(button.active, "Button disabled: " + label);
    float scale =
        mc.screen instanceof dev.heavenlyrestriction.client.ForgeScreen f ? f.uiScale() : 1;
    check(
        mc.screen.mouseClicked(
            (button.getX() + button.getWidth() / 2d) * scale,
            (button.getY() + button.getHeight() / 2d) * scale,
            0),
        "Click not accepted: " + label);
  }

  private static void checkBounds() {
    var screen = Minecraft.getInstance().screen;
    for (var child : screen.children())
      if (child instanceof net.minecraft.client.gui.components.AbstractWidget w && w.visible) {
        check(
            w.getX() >= 0
                && w.getY() >= 0
                && w.getX() + w.getWidth() <= screen.width
                && w.getY() + w.getHeight() <= screen.height,
            "Widget outside viewport: " + w.getMessage());
      }
    var widgets =
        screen.children().stream()
            .filter(
                c ->
                    c instanceof net.minecraft.client.gui.components.AbstractWidget w
                        && w.visible
                        && w.active)
            .map(c -> (net.minecraft.client.gui.components.AbstractWidget) c)
            .toList();
    for (int a = 0; a < widgets.size(); a++)
      for (int b = a + 1; b < widgets.size(); b++) {
        var one = widgets.get(a);
        var two = widgets.get(b);
        check(
            one.getX() + one.getWidth() <= two.getX()
                || two.getX() + two.getWidth() <= one.getX()
                || one.getY() + one.getHeight() <= two.getY()
                || two.getY() + two.getHeight() <= one.getY(),
            "Overlapping controls: " + one.getMessage() + " / " + two.getMessage());
      }
  }

  private static void select(String name) {
    var screen = Minecraft.getInstance().screen;
    for (int n = 0; n < 12; n++) {
      boolean found =
          screen.children().stream()
              .anyMatch(
                  w ->
                      w instanceof AbstractButton b
                          && b.visible
                          && b.getMessage() != null
                          && b.getMessage().getString().equals("Select " + name));
      if (found) {
        click("Select " + name);
        return;
      }
      click("Next page");
    }
    throw new IllegalStateException("Missing equipment " + name);
  }

  private static void grant(ServerPlayer p, ManasSkill skill) {
    RestrictionData.learnReward(p, skill);
    var i = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow();
    i.setMastery(i.getMaxMastery());
  }

  @SubscribeEvent
  public static void damage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post e) {
    if (e.getEntity() == dummy && e.getNewDamage() > 0) acceptedHits++;
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    try {
      step();
    } catch (Throwable e) {
      error = e.toString();
      e.printStackTrace();
    }
  }

  private static void step() throws Exception {
    var mc = Minecraft.getInstance();
    mc.options.pauseOnLostFocus = false;
    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 4, 4);
    // Park the fixture pointer without relying on desktop focus / native motion callbacks.
    for (String name : List.of("xpos", "ypos")) {
      var field = net.minecraft.client.MouseHandler.class.getDeclaredField(name);
      field.setAccessible(true);
      field.setDouble(mc.mouseHandler, 4);
    }
    if (error != null || ++age > 6000) {
      Files.createDirectories(OUT);
      Files.writeString(
          OUT.resolve("failed-hephaestus.txt"), error == null ? "Timeout phase " + phase : error);
      mc.stop();
      return;
    }
    if (mc.player == null
        || mc.level == null
        || mc.getSingleplayerServer() == null
        || mc.getOverlay() != null) return;
    wait++;
    if (phase == 0 && wait > 75) {
      phase = 1;
      wait = 0;
      mc.setScreen(null);
      mc.options.guiScale().set(2);
      mc.options.fov().set(60);
      mc.options.hideGui = false;
      mc.resizeDisplay();
      server(
          p -> {
            check(
                p.server
                    .getWorldPath(LevelResource.ROOT)
                    .normalize()
                    .getFileName()
                    .toString()
                    .equals("Wayfarer Review"),
                "Refusing a personal save");
            ForgeArsenal.recall(p);
            var level = p.server.getLevel(HighRealm.LEVEL);
            check(level != null, "Missing review dimension");
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
            var root = new Vec3(1000.5, 161, 1000.5);
            for (var e : level.getEntitiesOfClass(Mob.class, new AABB(root, root).inflate(35)))
              e.discard();
            for (int x = -9; x <= 9; x++)
              for (int z = -9; z <= 9; z++) {
                level.setBlock(
                    new BlockPos(1000 + x, 160, 1000 + z),
                    Math.abs(x) == 9 || Math.abs(z) == 9
                        ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    2);
                for (int y = 161; y <= 170; y++)
                  level.setBlock(
                      new BlockPos(1000 + x, y, 1000 + z), Blocks.AIR.defaultBlockState(), 2);
              }
            level.setBlock(new BlockPos(1003, 161, 1000), Blocks.ANVIL.defaultBlockState(), 2);
            level.setBlock(
                new BlockPos(997, 161, 1000), Blocks.COPPER_BLOCK.defaultBlockState(), 2);
            level.setBlock(new BlockPos(997, 162, 1000), Blocks.LANTERN.defaultBlockState(), 2);
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.setNoGravity(true);
            p.teleportTo(level, 1000.5, 161, 1004.5, 155, 5);
            p.setDeltaMovement(Vec3.ZERO);
            p.getInventory().clearContent();
            p.removeAllEffects();
            RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(), false);
            EnergyHelper.setBaseMaxEP(p, 2000000);
            p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(1000000);
            p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(1000000);
            TensuraStorages.getExistenceFrom(p).setTrueHero(true);
            TensuraStorages.getExistenceFrom(p).setMagicule(1000000);
            var skills = SkillAPI.getSkillsFrom(p);
            for (var i : List.copyOf(skills.getLearnedSkills())) i.setToggled(false);
            skills
                .getSkill(HeavenlyRestriction.CRITICAL_MERCHANT.get())
                .ifPresent(dev.heavenlyrestriction.critical.CriticalMerchant::clear);
            skills.getSkill(HeavenlyRestriction.HEPHAESTUS.get()).ifPresent(skills::forgetSkill);
            grant(p, HeavenlyRestriction.PROSPECTOR.get());
            grant(p, UniqueSkills.GODLY_CRAFTSMAN.get());
            var proof = new ItemStack(Items.NETHERITE_SWORD);
            proof.onCraftedBy(p.level(), p, 1);
            ForgeSynthesis.discover(p);
            ((StorageHolder) p).manasCore$sync();
            initialized = true;
          });
      return;
    }
    if (!initialized) return;
    mc.player.getAbilities().flying = true;
    mc.player.setNoGravity(true);
    if (phase == 1 && wait > 35) {
      var i =
          SkillAPI.getSkillsFrom(mc.player)
              .getSkill(HeavenlyRestriction.HEPHAESTUS.get())
              .orElseThrow();
      check(i.getMastery() < 0, "Expected synthesis entry");
      mc.setScreen(new AbilitySelectionScreen(AbilityType.SKILL, SkillType.ULTIMATE, null));
      ((AbilitySelectionScreen) mc.screen).selectedAbility = i;
      phase = 2;
      wait = 0;
      return;
    }
    if (phase == 2 && wait > 15) {
      shot("hephaestus-ultimate");
      server(
          p -> {
            var i =
                SkillAPI.getSkillsFrom(p)
                    .getSkill(HeavenlyRestriction.HEPHAESTUS.get())
                    .orElseThrow();
            var ability = TensuraStorages.getAbilityFrom(p);
            ability.setActivePreset(0);
            ability.setAbilitySlot(0, 0, i.getSkill(), 0);
            ability.markDirty();
            SkillEvents.ACTIVATE_SKILL
                .invoker()
                .activateSkill(
                    io.github.manasmods.manascore.network.api.util.Changeable.of(i), p, 0, 0);
          });
      phase = 3;
      wait = 0;
      return;
    }
    if (phase == 3
        && wait > 20
        && mc.screen instanceof dev.heavenlyrestriction.client.ForgeScreen) {
      shot("hephaestus-synthesis");
      click("Primary action");
      phase = 4;
      wait = 0;
      return;
    }
    if (phase == 4 && wait > 24) {
      shot("hephaestus-confirm-synthesis");
      click("Confirm");
      phase = 5;
      wait = 0;
      return;
    }
    if (phase == 5 && wait > 25) {
      server(
          p -> {
            check(ForgeSynthesis.owned(p).isPresent(), "Native menu synthesis failed");
            check(
                SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PROSPECTOR.get()).isEmpty()
                    && SkillAPI.getSkillsFrom(p)
                        .getSkill(UniqueSkills.GODLY_CRAFTSMAN.get())
                        .isEmpty(),
                "Source skills remained");
            check(
                p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue() == 750000,
                "Acquisition was not charged once");
            var i = ForgeSynthesis.owned(p).orElseThrow();
            check(i.getMastery() == 0, "Ultimate was not unmastered");
            i.setToggled(true);
            Item[] weapons = {
              Items.NETHERITE_SWORD,
              HeavenlyArsenal.VESPER.get(),
              HeavenlyArsenal.SPEAR.get(),
              Items.DIAMOND_SWORD,
              Items.IRON_SWORD,
              Items.GOLDEN_SWORD
            };
            for (int n = 0; n < weapons.length; n++) {
              var item = new ItemStack(weapons[n]);
              item.onCraftedBy(p.level(), p, 1);
              p.getInventory().setItem(n, item);
            }
            p.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 32));
            p.getInventory().setItem(10, new ItemStack(Items.NETHERITE_INGOT, 8));
            var ability = TensuraStorages.getAbilityFrom(p);
            ability.setActivePreset(0);
            ability.setAbilitySlot(0, 0, HeavenlyRestriction.HEPHAESTUS.get(), 0);
            ability.markDirty();
            p.getInventory().setChanged();
            ((StorageHolder) p).manasCore$sync();
            ForgeService.open(p, "forge");
            converted = true;
          });
      phase = 6;
      wait = 0;
      return;
    }
    if (phase == 6
        && converted
        && wait > 25
        && mc.screen instanceof dev.heavenlyrestriction.client.ForgeScreen) {
      shot("hephaestus-forge");
      mc.options.guiScale().set(3);
      mc.resizeDisplay();
      phase = 60;
      wait = 0;
      return;
    }
    if (phase == 60 && wait > 10) {
      shot("hephaestus-forge-compact");
      checkBounds();
      mc.getWindow().setGuiScale(4);
      mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
      phase = 601;
      wait = 0;
      return;
    }
    if (phase == 601 && wait > 8) {
      checkBounds();
      shot("hephaestus-forge-small");
      mc.options.guiScale().set(2);
      mc.resizeDisplay();
      phase = 61;
      wait = 0;
      return;
    }
    if (phase == 61 && wait > 5) {
      checkBounds();
      click("Action");
      phase = 610;
      wait = 0;
      return;
    }
    if (phase == 610 && wait > 5) {
      checkBounds();
      shot("hephaestus-forge-actions");
      click("Temper · Keen");
      click("Material");
      phase = 611;
      wait = 0;
      return;
    }
    if (phase == 611 && wait > 5) {
      checkBounds();
      shot("hephaestus-forge-materials");
      click("Diamond · 32 owned");
      click("Primary action");
      phase = 7;
      wait = 0;
      return;
    }
    if (phase == 7 && wait > 20) {
      click("Utilities");
      phase = 701;
      wait = 0;
      return;
    }
    if (phase == 701 && wait > 8) {
      checkBounds();
      shot("hephaestus-forge-utilities");
      mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0);
      click("Magnum Opus");
      phase = 8;
      wait = 0;
      return;
    }
    if (phase == 8 && wait > 5) {
      click("Primary action");
      phase = 9;
      wait = 0;
      return;
    }
    if (phase == 9 && wait > 25) {
      checkBounds();
      shot("hephaestus-confirm-masterpiece");
      click("Confirm");
      phase = 10;
      wait = 0;
      return;
    }
    if (phase == 10 && wait > 25) {
      server(
          p -> {
            check(
                ForgeItems.masterpiece(p.getInventory().getItem(0)),
                "Masterpiece designation failed");
            check(
                ForgeItems.temper(p.getInventory().getItem(0)) == ForgeItems.Temper.KEEN,
                "Native UI temper failed");
            equipped = true;
          });
      shot("hephaestus-masterpiece");
      phase = 11;
      wait = 0;
      return;
    }
    if (phase == 11 && equipped && wait > 5) {
      click("Living Arsenal");
      phase = 12;
      wait = 0;
      return;
    }
    if (phase == 12 && wait > 8) {
      for (String name :
          List.of(
              "Netherite Sword",
              "Vesper",
              "Inverted Spear of Heaven",
              "Diamond Sword",
              "Iron Sword",
              "Golden Sword")) select(name);
      click("Primary action");
      phase = 13;
      wait = 0;
      return;
    }
    if (phase == 13 && wait > 70) {
      checkBounds();
      shot("hephaestus-arsenal-menu");
      mc.getWindow().setGuiScale(4);
      mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
      phase = 1300;
      wait = 0;
      return;
    }
    if (phase == 1300 && wait > 8) {
      checkBounds();
      shot("hephaestus-arsenal-small");
      mc.options.guiScale().set(2);
      mc.resizeDisplay();
      phase = 1301;
      wait = 0;
      return;
    }
    if (phase == 1301 && wait > 8) {
      click("Recall blade 1");
      phase = 130;
      wait = 0;
      return;
    }
    if (phase == 130 && wait > 20) {
      server(
          p ->
              check(ForgeArsenal.count(p) == 5, "Individual menu recall did not keep five blades"));
      phase = 131;
      wait = 0;
      return;
    }
    if (phase == 131 && wait > 5) {
      select("Netherite Sword");
      click("Primary action");
      phase = 132;
      wait = 0;
      return;
    }
    if (phase == 132 && wait > 25) {
      server(
          p ->
              check(
                  ForgeArsenal.count(p) == 6,
                  "Incremental menu deployment did not restore six blades"));
      mc.setScreen(null);
      mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      phase = 14;
      wait = 0;
      return;
    }
    if (phase == 14 && wait > 20) {
      shot("hephaestus-arsenal");
      server(
          p -> {
            check(ForgeArsenal.active(p), "Arsenal not deployed");
            for (int n = 0; n < 6; n++)
              check(p.getInventory().getItem(n).isEmpty(), "Weapon still usable while deployed");
            dummy =
                (TrainingDummyEntity)
                    BuiltInRegistries.ENTITY_TYPE
                        .get(ResourceLocation.parse("tensura:training_dummy"))
                        .create(p.level());
            dummy.setPos(1000.5, 161.25, 999.5);
            dummy.setNoAi(true);
            dummy.setNoGravity(true);
            dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            p.serverLevel()
                .setBlock(
                    dummy.blockPosition(),
                    TensuraBlocks.TRAINING_DUMMY.get().defaultBlockState(),
                    3);
            p.serverLevel().addFreshEntity(dummy);
            acceptedHits = 0;
            p.setLastHurtMob(dummy);
          });
      phase = 15;
      wait = 0;
      return;
    }
    if (phase == 15 && wait > 30) {
      int moving = 0;
      boolean far = false;
      for (var e : mc.level.entitiesForRendering())
        if (e instanceof ArsenalBlade b && b.phase() == 1) {
          moving++;
          far |= b.getZ() < mc.player.getZ() - 2;
        }
      if ((!far || moving < 1) && wait < 120) return;
      check(far, "Client blades did not leave formation");
      shot("hephaestus-arsenal-strike");
      phase = 16;
      wait = 0;
      return;
    }
    if (phase == 16 && wait > 20) {
      server(
          p -> {
            check(
                acceptedHits > 0,
                "No real autonomous damage: last="
                    + dummy.lastDamageTaken
                    + " alive="
                    + dummy.isAlive()
                    + " removed="
                    + dummy.isRemoved());
            damaged = true;
            ForgeArsenal.recall(p);
            int count = 0;
            for (var s : p.getInventory().items) if (ForgeItems.weapon(s)) count++;
            check(count == 6 && !ForgeArsenal.active(p), "Recall lost or duplicated weapons");
            recalled = true;
          });
      phase = 17;
      wait = 0;
      return;
    }
    if (phase == 17 && damaged && recalled && wait > 20) {
      Files.writeString(
          OUT.resolve("passed-hephaestus.txt"),
          "PASS: native Ultimate learning entry; explicit synthesis confirmation surviving"
              + " refresh; 250000 permanent magicules charged once; both parents replaced,"
              + " learned/unmastered Ultimate; native Forge UI tempering; one masterpiece; six real"
              + " weapons escrowed, individual menu recall and incremental redeployment, autonomous"
              + " strikes and exact six-item recall.\n");
      mc.options.setCameraType(CameraType.FIRST_PERSON);
      mc.stop();
    }
  }
}
