package dev.heavenlyrestriction;

import dev.heavenlyrestriction.cartography.*;
import dev.heavenlyrestriction.client.cartography.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in native client exercise in a disposable normal-generation world. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class MapReview {
  private static final Path OUT = Path.of("/workspace/validation-marauders-map/capture");
  private static volatile int phase;
  private static int age, wait;
  private static volatile String error;
  private static double originalX, originalZ;
  private static volatile int farX = 120000, farZ = -160000;
  private static double warpX, warpY, warpZ;
  private static net.minecraft.nbt.CompoundTag autoMarker;
  private static volatile boolean autoVerified;
  private static Set<String> surveyRegionsBefore = Set.of();

  private static Set<String> surveyRegions(net.minecraft.server.level.ServerPlayer p) {
    var dir = p.server.getWorldPath(LevelResource.ROOT).resolve("region");
    var out = new HashSet<String>();
    try (var paths = Files.list(dir)) {
      paths.forEach(
          path -> {
            String[] parts = path.getFileName().toString().split("\\.");
            if (parts.length == 4 && parts[0].equals("r")) {
              try {
                int x = Integer.parseInt(parts[1]) * 512, z = Integer.parseInt(parts[2]) * 512;
                if (Math.abs(x - 300000) < 10000 && Math.abs(z + 310000) < 10000)
                  out.add(path.getFileName().toString());
              } catch (NumberFormatException ignored) {
              }
            }
          });
    } catch (java.io.IOException ex) {
      throw new IllegalStateException(ex);
    }
    return out;
  }

  private static net.minecraft.client.gui.components.EditBox nativeField(String name) {
    try {
      var f =
          io.github.manasmods.tensura.client.screen.SpatialMovementScreen.class.getDeclaredField(
              name);
      f.setAccessible(true);
      return (net.minecraft.client.gui.components.EditBox) f.get(Minecraft.getInstance().screen);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static String nativeSkill() {
    try {
      var f =
          io.github.manasmods.tensura.client.screen.SpatialMovementScreen.class.getDeclaredField(
              "skill");
      f.setAccessible(true);
      return ((io.github.manasmods.manascore.skill.api.ManasSkill)
              f.get(Minecraft.getInstance().screen))
          .getRegistryName()
          .toString();
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static volatile UUID savedPin;
  private static net.minecraft.server.level.ServerPlayer companion, traveler;

  private static net.minecraft.nbt.CompoundTag state() {
    try {
      var f = MaraudersMapScreen.class.getDeclaredField("state");
      f.setAccessible(true);
      return (net.minecraft.nbt.CompoundTag) f.get(map());
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static final class Menu extends AbilitySelectionScreen {
    Menu() {
      super(AbilityCategoriesScreen.AbilityType.SKILL, Skill.SkillType.UNIQUE, null);
    }

    void search() {
      searchField.setValue("Marauder");
    }

    void select() {
      int at = -1;
      for (int n = 0; n < filtered.size(); n++)
        if (filtered.get(n).getSkill() == HeavenlyRestriction.MARAUDERS_MAP.get()) {
          at = n;
          break;
        }
      check(at >= 0, "Map missing from native Unique menu");
      mouseClicked(guiLeft + 12, guiTop + 46 + 13 * (at - listStartIndex) + 5, 0);
      check(
          selectedAbility != null
              && selectedAbility.getSkill() == HeavenlyRestriction.MARAUDERS_MAP.get(),
          "Native Map selection failed");
    }
  }

  private static void fakeConnection(net.minecraft.server.level.ServerPlayer p) {
    try {
      var f = net.minecraft.network.Connection.class.getDeclaredField("channel");
      f.setAccessible(true);
      f.set(p.connection.getConnection(), new io.netty.channel.embedded.EmbeddedChannel());
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void check(boolean value, String message) {
    if (!value) {
      error = message;
      throw new IllegalStateException(message);
    }
  }

  private static void server(
      java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> task) {
    var s = Minecraft.getInstance().getSingleplayerServer();
    s.execute(
        () -> {
          try {
            task.accept(s.getPlayerList().getPlayers().getFirst());
          } catch (Throwable e) {
            e.printStackTrace();
            error = e.toString();
          }
        });
  }

  private static void shot(String name) {
    try {
      Files.createDirectories(OUT);
      try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
        image.writeToFile(OUT.resolve(name + ".png"));
      }
    } catch (Exception e) {
      error = e.toString();
    }
  }

  private static Button button(String text) {
    return Minecraft.getInstance().screen.children().stream()
        .filter(w -> w instanceof Button b && b.getMessage().getString().equals(text))
        .map(w -> (Button) w)
        .findFirst()
        .orElseThrow();
  }

  private static void press(String text) {
    var b = button(text);
    check(b.active && b.visible, "Inactive button: " + text);
    Minecraft.getInstance()
        .screen
        .mouseClicked(b.getX() + b.getWidth() / 2d, b.getY() + b.getHeight() / 2d, 0);
  }

  private static EditBox field(String label) {
    return Minecraft.getInstance().screen.children().stream()
        .filter(w -> w instanceof EditBox b && b.getMessage().getString().equals(label))
        .map(w -> (EditBox) w)
        .findFirst()
        .orElseThrow();
  }

  private static MaraudersMapScreen map() {
    check(Minecraft.getInstance().screen instanceof MaraudersMapScreen, "Map screen not open");
    return (MaraudersMapScreen) Minecraft.getInstance().screen;
  }

  private static void fits() {
    var screen = Minecraft.getInstance().screen;
    for (var w : screen.children())
      if (w instanceof AbstractWidget a && a.visible)
        check(
            a.getX() >= 0
                && a.getY() >= 0
                && a.getX() + a.getWidth() <= screen.width
                && a.getY() + a.getHeight() <= screen.height,
            "Control clipped: "
                + a.getMessage().getString()
                + " "
                + a.getX()
                + ","
                + a.getY()
                + " "
                + a.getWidth()
                + "x"
                + a.getHeight()
                + " / "
                + screen.width
                + "x"
                + screen.height);
  }

  private static void open() {
    server(
        p -> {
          var i =
              SkillAPI.getSkillsFrom(p)
                  .getSkill(HeavenlyRestriction.MARAUDERS_MAP.get())
                  .orElseThrow();
          TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, i.getSkill(), 0);
          check(
              SkillAPI.getSkillsFrom(p).startHoldSkill(i, 0, 0), "Native map activation rejected");
          SkillAPI.getSkillsFrom(p).handleSkillRelease(i, 0, 0, false);
        });
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    try {
      exercise();
    } catch (Throwable e) {
      error = e.toString();
      e.printStackTrace();
    }
  }

  private static void exercise() {
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
    wait++;
    if (phase == 0) {
      phase = 1;
      wait = 0;
      mc.options.guiScale().set(2);
      mc.options.renderDistance().set(12);
      mc.resizeDisplay();
      server(
          p -> {
            check(
                p.server
                    .getWorldPath(LevelResource.ROOT)
                    .normalize()
                    .getFileName()
                    .toString()
                    .equals("Marauders Map Review"),
                "Refusing non-review world");
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            p.removeAllEffects();
            io.github.manasmods.manascore.race.api.RaceAPI.getRaceFrom(p)
                .setRace(io.github.manasmods.tensura.registry.race.TensuraRaces.HUMAN.get(), false);
            var existingPins = MapData.get(p.server).pins(p.getUUID());
            existingPins.forEach(pin -> MapData.get(p.server).removePin(p.getUUID(), pin.id()));
            p.setNoGravity(false);
            for (var i : List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills())) {
              if (i.isToggled()) i.onToggleOff(p);
              SkillAPI.getSkillsFrom(p).forgetSkill(i);
            }
            RestrictionData.learnReward(p, HeavenlyRestriction.MARAUDERS_MAP.get());
            SkillAPI.getSkillsFrom(p)
                .getSkill(HeavenlyRestriction.MARAUDERS_MAP.get())
                .orElseThrow()
                .setMastery(0);
            var level = p.server.overworld();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
            level.setDayTime(6000);
            level.setWeatherParameters(6000, 0, false, false);
            var village =
                level.findNearestMapStructure(
                    StructureTags.VILLAGE, new BlockPos(0, 64, 0), 96, false);
            check(village != null, "Review world has no nearby native village");
            level.getChunk(village.getX() >> 4, village.getZ() >> 4);
            int y =
                level.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    village.getX(),
                    village.getZ());
            p.teleportTo(level, village.getX() + .5, y + 1, village.getZ() + .5, 180, 0);
            originalX = p.getX();
            originalZ = p.getZ();
            MapService.login(p);
            phase = 2;
          });
      return;
    }
    if (phase == 2 && wait > 100 && Boolean.getBoolean("heavenlyrestriction.review.surveyOnly")) {
      open();
      phase = 500;
      wait = 0;
      return;
    }
    if (phase == 2 && wait > 100 && Boolean.getBoolean("heavenlyrestriction.review.travelOnly")) {
      open();
      phase = 50;
      wait = 0;
      return;
    }
    if (phase == 50 && mc.screen instanceof MaraudersMapScreen && wait > 20) {
      phase = 29;
      wait = 16;
      return;
    }
    if (phase == 2 && wait > 100) {
      check(MapAtlas.knownChunks() > 20, "Explored terrain recorder did not run");
      var menu = new Menu();
      mc.setScreen(menu);
      menu.search();
      phase = 3;
      wait = 0;
      return;
    }
    if (phase == 3 && wait > 25) {
      ((Menu) mc.screen).select();
      shot("marauders-map-unique-menu");
      mc.setScreen(null);
      phase = 4;
      wait = 0;
      open();
      return;
    }
    if (phase == 4 && mc.screen instanceof MaraudersMapScreen && wait > 90) {
      fits();
      press("?");
      phase = 41;
      wait = 0;
      return;
    }
    if (phase == 41 && wait > 10) {
      fits();
      shot("marauders-map-help");
      press("Back to map");
      phase = 42;
      wait = 0;
      return;
    }
    if (phase == 42 && wait > 10) {
      var m = map();
      m.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_F, 0, 0);
      check(field("Search structures and pins").isFocused(), "F did not focus marker search");
      m.mouseClicked(m.width - 90, m.height / 2, 0);
      m.mouseReleased(m.width - 90, m.height / 2, 0);
      check(
          !field("Search structures and pins").isFocused(),
          "Canvas click did not release search focus");
      double x = m.view.x, z = m.view.z, scale = m.view.scale;
      int mx = m.width - 90, my = m.height / 2;
      m.mouseClicked(mx, my, 0);
      m.mouseDragged(mx + 40, my + 18, 0, 40, 18);
      m.mouseReleased(mx + 40, my + 18, 0);
      check(
          Math.abs(m.view.x - (x - 40 / scale)) < .01
              && Math.abs(m.view.z - (z - 18 / scale)) < .01,
          "Drag did not follow cursor");
      m.mouseScrolled(mx, my, 0, 1);
      check(m.view.scale > scale, "Mouse zoom did not work");
      press("Recenter");
      phase = 5;
      wait = 0;
      return;
    }
    if (phase == 5 && wait > 40) {
      shot("marauders-map");
      var m = map();
      m.mouseClicked(m.width - 110, m.height / 2, 1);
      phase = 6;
      wait = 0;
      return;
    }
    if (phase == 6 && mc.screen instanceof MapPinScreen && wait > 10) {
      fits();
      press("Save pin");
      check(mc.screen instanceof MapPinScreen, "Blank pin saved");
      field("Pin name").setValue("Camp Willow");
      press("Color: 1");
      press("Icon: Pin");
      phase = 61;
      wait = 0;
      return;
    }
    if (phase == 61 && wait > 8) {
      shot("marauders-map-pin-editor");
      press("Save pin");
      phase = 7;
      wait = 0;
      return;
    }
    if (phase == 7 && wait > 25) {
      server(
          p -> {
            var pins = MapData.get(p.server).pins(p.getUUID());
            check(
                pins.size() == 1 && pins.getFirst().name().equals("Camp Willow"),
                "C2S pin save failed");
            check(
                pins.getFirst().color() == 1 && pins.getFirst().icon() == 1,
                "Custom pin appearance lost");
            savedPin = pins.getFirst().id();
          });
      var m = map();
      field("Search structures and pins").setValue("Camp Willow");
      m.mouseClicked(24, 110, 0);
      phase = 8;
      wait = 0;
      return;
    }
    if (phase == 8 && wait > 12) {
      shot("marauders-map-custom-pin");
      press("Edit pin");
      field("Pin name").setValue("Camp Willow Home");
      field("X").setValue(String.valueOf(Integer.parseInt(field("X").getValue()) + 8));
      press("Save pin");
      phase = 9;
      wait = 0;
      return;
    }
    if (phase == 9 && wait > 20) {
      server(
          p -> {
            var pins = MapData.get(p.server).pins(p.getUUID());
            check(
                pins.size() == 1
                    && pins.getFirst().id().equals(savedPin)
                    && pins.getFirst().name().equals("Camp Willow Home"),
                "Pin edit changed identity or failed");
            var d = MapData.get(p.server);
            var restored =
                MapData.load(
                    d.save(new net.minecraft.nbt.CompoundTag(), p.registryAccess()),
                    p.registryAccess());
            check(restored.pins(p.getUUID()).equals(pins), "Pin NBT roundtrip failed");
            // Exercise incremental native discovery, not the known-marker shortcut.
            d.structures
                .entrySet()
                .removeIf(e -> e.getValue().type().equals("minecraft:village_plains"));
            d.setDirty();
          });
      field("Search structures and pins").setValue("");
      press("Locate structure…");
      phase = 10;
      wait = 0;
      return;
    }
    if (phase == 10 && wait > 10) {
      fits();
      field("Find a structure type").setValue("village_plains");
      phase = 11;
      wait = 0;
      return;
    }
    if (phase == 11 && wait > 10) {
      press("Village Plains");
      phase = 111;
      wait = 0;
      return;
    }
    if (phase == 111 && wait > 8) {
      shot("marauders-map-structure-search");
      press("Start search");
      phase = 12;
      wait = 0;
      return;
    }
    if (phase == 12 && wait > 30) {
      if (!button("Show result").active) {
        check(wait < 1200, "Real village was not located within 60 seconds");
        return;
      }
      server(
          p -> {
            var search = MapService.session(p).search;
            check(
                MapService.session(p).found != null
                    && (search == null
                        || (search.done && search.checked > 0 && search.candidates > 0)),
                "Incremental native village discovery did not run");
          });
      press("Show result");
      phase = 13;
      wait = 0;
      return;
    }
    if (phase == 13 && wait > 25) {
      shot("marauders-map-structures");
      press("Go to…");
      field("X").setValue(String.valueOf((int) originalX + 128));
      field("Z").setValue(String.valueOf((int) originalZ + 64));
      press("Center map");
      phase = 14;
      wait = 0;
      return;
    }
    if (phase == 14 && wait > 15) {
      check(Math.abs(map().view.x - ((int) originalX + 128)) < .01, "Go-to did not center map");
      server(
          p ->
              check(
                  Math.abs(p.getX() - originalX) < 4 && Math.abs(p.getZ() - originalZ) < 4,
                  "Go-to teleported player"));
      press("Recenter");
      mc.options.guiScale().set(4);
      mc.resizeDisplay();
      phase = 15;
      wait = 0;
      return;
    }
    if (phase == 15 && wait > 25) {
      fits();
      shot("marauders-map-small");
      press("Add pin");
      phase = 16;
      wait = 0;
      return;
    }
    if (phase == 16 && wait > 10) {
      fits();
      shot("marauders-map-pin-small");
      press("Cancel");
      mc.options.guiScale().set(2);
      mc.resizeDisplay();
      field("Search structures and pins").setValue("Camp Willow");
      map().mouseClicked(24, 110, 0);
      phase = 17;
      wait = 0;
      return;
    }
    if (phase == 17 && wait > 10) {
      press("Delete pin");
      press("Keep pin");
      phase = 18;
      wait = 0;
      return;
    }
    if (phase == 18 && wait > 10) {
      server(
          p ->
              check(
                  MapData.get(p.server).pins(p.getUUID()).size() == 1,
                  "Delete cancellation removed pin"));
      MapAtlas.flush();
      map().onClose();
      phase = 19;
      wait = 0;
      return;
    }
    if (phase == 19 && wait > 25) {
      open();
      phase = 20;
      wait = 0;
      return;
    }
    if (phase == 20 && mc.screen instanceof MaraudersMapScreen && wait > 20) {
      field("Search structures and pins").setValue("Camp Willow");
      map().mouseClicked(24, 110, 0);
      press("Edit pin");
      check(field("Pin name").getValue().equals("Camp Willow Home"), "Pin did not survive reopen");
      press("Cancel");
      phase = 21;
      wait = 0;
      return;
    }
    if (phase == 21 && wait > 10) {
      press("Delete pin");
      press("Delete pin");
      phase = 22;
      wait = 0;
      return;
    }
    if (phase == 22 && wait > 25) {
      server(
          p -> {
            check(
                MapData.get(p.server).pins(p.getUUID()).isEmpty(), "Confirmed pin deletion failed");
            check(
                MapService.snapshot(p, MapService.session(p))
                    .getList("types", net.minecraft.nbt.Tag.TAG_COMPOUND)
                    .stream()
                    .anyMatch(
                        t ->
                            ((net.minecraft.nbt.CompoundTag) t)
                                .getString("id")
                                .startsWith("tensura:")),
                "Addon structures missing from search types");
          });
      MapAtlas.flush();
      phase = 23;
      wait = 0;
      return;
    }
    if (phase == 23 && wait > 30) {
      try {
        Path root = mc.gameDirectory.toPath().resolve("tensura-additions/maps");
        try (var paths = Files.walk(root)) {
          check(
              paths.anyMatch(p -> p.toString().endsWith(".png") && p.toFile().length() > 200),
              "Terrain tiles did not persist to disk");
        }
        check(MapAtlas.knownChunks() > 80, "Too little explored terrain recorded");
        Files.writeString(
            OUT.resolve("passed.txt"),
            "PASS: real native Unique menu/name/icon; actual normal-world village terrain; native"
                + " skill activation opens map; drag/zoom/recenter; C2S private pin creation,"
                + " custom color/icon, edit, invalid label, NBT persistence/reopen and confirmed"
                + " deletion; registry structure search includes Tensura; actual native village"
                + " marker; Go-to centers without teleporting; fitted map and pin editor at GUI"
                + " scales 2 and 4; terrain PNG cache persisted. Known chunks="
                + MapAtlas.knownChunks()
                + "\n");
      } catch (Exception e) {
        error = e.toString();
        return;
      }
      field("Search structures and pins").setValue("");
      press("Recenter");
      map().view.scale = 2;
      server(
          p -> {
            companion =
                net.neoforged.neoforge.common.util.FakePlayerFactory.get(
                    p.serverLevel(),
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), "Cartographer"));
            fakeConnection(companion);
            companion.setPos(originalX + 28, p.getY(), originalZ + 36);
            TestFixtures.reviewRoster(p).add(companion);
            var nether = p.server.getLevel(net.minecraft.world.level.Level.NETHER);
            traveler =
                net.neoforged.neoforge.common.util.FakePlayerFactory.get(
                    nether, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "RealmWalker"));
            fakeConnection(traveler);
            traveler.setPos(-40.5, 70, 120.5);
            TestFixtures.reviewRoster(p).add(traveler);
            var level = p.serverLevel();
            var generator = level.getChunkSource().getGenerator();
            for (int n = 0; n < 32; n++) {
              int x = 120000 + n * 2048, z = -160000 + n * 1024;
              int y =
                  generator.getFirstOccupiedHeight(
                      x,
                      z,
                      net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                      level,
                      level.getChunkSource().randomState());
              if (y > generator.getSeaLevel() + 12) {
                farX = x;
                farZ = z;
                break;
              }
            }
            System.out.println("Map review remote terrain: " + farX + "," + farZ);
          });
      phase = 24;
      wait = 0;
      return;
    }
    if (phase == 24 && wait > 100) {
      check(
          state().getList("players", net.minecraft.nbt.Tag.TAG_COMPOUND).stream()
              .anyMatch(
                  t ->
                      ((net.minecraft.nbt.CompoundTag) t).getString("name").equals("Cartographer")),
          "Live player marker missing");
      check(
          state().getList("players", net.minecraft.nbt.Tag.TAG_COMPOUND).stream()
              .anyMatch(
                  t ->
                      ((net.minecraft.nbt.CompoundTag) t)
                          .getString("dimension")
                          .equals("minecraft:the_nether")),
          "Other-dimension player missing");
      shot("marauders-map-players");
      press("Go to…");
      field("X").setValue(String.valueOf(farX));
      field("Z").setValue(String.valueOf(farZ));
      press("Center map");
      phase = 25;
      wait = 0;
      return;
    }
    if (phase == 25 && wait > 60) {
      if (!MapTerrainAtlas.hasPreviewAt(farX, farZ) || !MapTerrainAtlas.hasDetailAt(farX, farZ)) {
        if (wait % 200 == 0)
          System.out.println(
              "Map remote wait "
                  + wait
                  + " tiles="
                  + MapTerrainAtlas.tileCount()
                  + " preview="
                  + MapTerrainAtlas.hasPreviewAt(farX, farZ)
                  + " detail="
                  + MapTerrainAtlas.hasDetailAt(farX, farZ));
        check(wait < 1800, "Unexplored native terrain/detail did not arrive within 90 seconds");
        return;
      }
      check(
          mc.level
                  .getChunkSource()
                  .getChunk(
                      farX >> 4,
                      farZ >> 4,
                      net.minecraft.world.level.chunk.status.ChunkStatus.FULL,
                      false)
              == null,
          "Distant preview became client-loaded terrain");
      check(Math.abs(mc.player.getX() - originalX) < 4, "Mapping distant terrain moved the player");
      phase = 26;
      wait = 0;
      return;
    }
    if (phase == 26 && wait > 300) {
      shot("marauders-map-unexplored");
      map().view.scale = .2;
      map().keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0);
      server(p -> companion.setPos(originalX + 5000, companion.getY(), originalZ + 5000));
      phase = 27;
      wait = 0;
      return;
    }
    if (phase == 27 && wait > 300) {
      shot("marauders-map-overview");
      var marker =
          state().getList("players", net.minecraft.nbt.Tag.TAG_COMPOUND).stream()
              .map(t -> (net.minecraft.nbt.CompoundTag) t)
              .filter(t -> t.getString("name").equals("Cartographer"))
              .findFirst()
              .orElseThrow();
      check(
          marker.getInt("x") == (int) Math.floor(originalX + 5000),
          "Out-of-range player location did not update");
      field("Search structures and pins").setValue("RealmWalker");
      map().mouseClicked(24, 110, 0);
      check(!button("Focus").active, "Other-dimension location incorrectly centers current map");
      phase = 28;
      wait = 0;
      return;
    }
    if (phase == 28 && wait > 15) {
      shot("marauders-map-dimensions");
      try {
        Files.writeString(
            OUT.resolve("passed-world-map.txt"),
            "PASS: prior map/menu/pin/search regressions; native generator preview and 4x4-textured"
                + " native FEATURES detail at unvisited remote coordinates; no client chunk"
                + " load/teleport; distant live players update beyond tracking distance;"
                + " other-dimension players visible and cannot miscenter map; zoomed-out terrain"
                + " preview.\n");
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
      server(
          p -> {
            TestFixtures.reviewRoster(p).remove(companion);
            TestFixtures.reviewRoster(p).remove(traveler);
          });
      phase = 29;
      wait = 0;
      return;
    }
    if (phase == 29 && wait > 15) {
      field("Search structures and pins").setValue("");
      press("Go to…");
      field("X").setValue("70000");
      field("Z").setValue("-70000");
      press("Center map");
      map().view.scale = .2;
      server(
          p -> {
            RestrictionData.learnReward(
                p, io.github.manasmods.tensura.registry.skill.ExtraSkills.SPATIAL_MOTION.get());
            RestrictionData.learnReward(p, HeavenlyRestriction.INSTANT_TRANSMISSION.get());
            var i =
                SkillAPI.getSkillsFrom(p)
                    .getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get())
                    .orElseThrow();
            i.setMastery(i.getMaxMastery());
            MapData.get(p.server)
                .structures
                .entrySet()
                .removeIf(e -> e.getValue().type().equals("minecraft:stronghold"));
          });
      phase = 30;
      wait = 0;
      return;
    }
    if (phase == 30 && wait > 30) {
      press("Locate structure…");
      field("Find a structure type").setValue("stronghold");
      phase = 31;
      wait = 0;
      return;
    }
    if (phase == 31 && wait > 10) {
      press("Stronghold");
      shot("marauders-map-long-search");
      press("Start search");
      phase = 32;
      wait = 0;
      return;
    }
    if (phase == 32 && wait > 30) {
      if (!button("Show result").active) {
        check(wait < 1800, "100k native ring search did not find a stronghold within 90 seconds");
        return;
      }
      server(
          p -> {
            var session = MapService.session(p);
            var found = session.found;
            check(
                found != null
                    && session.search != null
                    && session.search.done
                    && session.search.checked > 0
                    && session.search.candidates > 0,
                "Long-range search used no native worker/candidates");
            double distance = Math.hypot(found.x() - 70000d, found.z() + 70000d);
            check(
                distance > 2048 && distance <= 100000,
                "Search did not discover a structure beyond the old range");
            System.out.println(
                "Map review 100k search: "
                    + found.x()
                    + ","
                    + found.z()
                    + " distance="
                    + distance
                    + " checked="
                    + session.search.checked
                    + " candidates="
                    + session.search.candidates);
          });
      press("Show result");
      phase = 33;
      wait = 0;
      return;
    }
    if (phase == 33 && wait > 30) {
      fits();
      check(button("Travel…").active, "Travel button not enabled with owned spatial skills");
      press("Travel…");
      phase = 34;
      wait = 0;
      return;
    }
    if (phase == 34 && wait > 15) {
      check(mc.screen instanceof MapTravelScreen, "Spatial skill chooser did not open");
      fits();
      check(
          button("Spatial Motion").active && button("Instant Transmission").active,
          "Owned spatial skills missing");
      shot("marauders-map-travel-skills");
      press("Spatial Motion");
      phase = 35;
      wait = 0;
      return;
    }
    if (phase == 35 && wait > 15) {
      if (!(mc.screen instanceof MapWarpScreen)) {
        check(wait < 1800, "Native Spatial Motion menu handoff timed out");
        return;
      }
      check(nativeSkill().equals("tensura:spatial_motion"), "Wrong native menu skill");
      warpX = Double.parseDouble(nativeField("fieldX").getValue());
      warpY = Double.parseDouble(nativeField("fieldY").getValue());
      warpZ = Double.parseDouble(nativeField("fieldZ").getValue());
      check(
          Math.abs(warpX - originalX) > 1000 || Math.abs(warpZ - originalZ) > 1000,
          "Destination not prefilled");
      check(
          Math.abs(mc.player.getX() - originalX) < 4, "Opening native menu teleported the player");
      mc.gui.getChat().clearMessages(false);
      phase = 36;
      wait = 0;
      return;
    }
    if (phase == 36 && wait > 15) {
      shot("marauders-map-spatial-motion");
      mc.screen.onClose();
      open();
      phase = 37;
      wait = 0;
      return;
    }
    if (phase == 37 && wait > 20) {
      var m = map();
      press("Go to…");
      field("X").setValue("70000");
      field("Z").setValue("-70000");
      press("Center map");
      phase = 38;
      wait = 0;
      return;
    }
    if (phase == 38 && wait > 20) {
      press("Locate structure…");
      field("Find a structure type").setValue("stronghold");
      phase = 39;
      wait = 0;
      return;
    }
    if (phase == 39 && wait > 10) {
      press("Stronghold");
      press("Start search");
      phase = 40;
      wait = 0;
      return;
    }
    if (phase == 40 && wait > 20) {
      if (!button("Show result").active) {
        check(wait < 1200, "Recorded long-range result missing");
        return;
      }
      press("Show result");
      phase = 401;
      wait = 0;
      return;
    }
    if (phase == 401 && wait > 15) {
      press("Travel…");
      phase = 402;
      wait = 0;
      return;
    }
    if (phase == 402 && wait > 15) {
      press("Instant Transmission");
      phase = 403;
      wait = 0;
      return;
    }
    if (phase == 403 && wait > 15) {
      if (!(mc.screen instanceof MapWarpScreen)) {
        check(wait < 1200, "Instant Transmission native menu did not open");
        return;
      }
      check(
          nativeSkill().equals("heavenlyrestriction:instant_transmission"),
          "Wrong aura teleport skill");
      check(
          Math.abs(Double.parseDouble(nativeField("fieldX").getValue()) - warpX) < .01
              && Math.abs(Double.parseDouble(nativeField("fieldZ").getValue()) - warpZ) < .01,
          "Reopened route lost coordinates");
      server(
          p -> {
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            p.getAttribute(
                    io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_AURA)
                .setBaseValue(20000000);
            p.getAttribute(
                    io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_MAGICULE)
                .setBaseValue(20000000);
            var e = io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(p);
            e.setAura(10000000);
            e.setMagicule(1000000);
          });
      phase = 404;
      wait = 0;
      return;
    }
    if (phase == 404 && wait > 20) {
      mc.gui.getChat().clearMessages(false);
      var x = nativeField("fieldX");
      double gx = x.getX() - 49 + 80, gy = x.getY() - 38 + 110;
      org.lwjgl.glfw.GLFW.glfwSetCursorPos(
          mc.getWindow().getWindow(),
          gx * mc.getWindow().getScreenWidth() / (double) mc.screen.width,
          gy * mc.getWindow().getScreenHeight() / (double) mc.screen.height);
      phase = 407;
      wait = 0;
      return;
    }
    if (phase == 407 && wait > 12) {
      shot("marauders-map-instant-transmission");
      var x = nativeField("fieldX");
      var warpButton =
          mc.screen.children().stream()
              .filter(
                  w ->
                      w instanceof Button b
                          && b.getX() == x.getX() + 21
                          && b.getY() == x.getY() + 62)
              .map(w -> (Button) w)
              .findFirst()
              .orElseThrow();
      mc.screen.setFocused(warpButton);
      check(
          mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0),
          "Native Warp keyboard confirmation was not accepted");
      server(
          p ->
              System.out.println(
                  "Map warp confirmation: aura="
                      + io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(p)
                          .getAura()
                      + " max="
                      + io.github.manasmods.tensura.util.EnergyHelper.getMaxAura(p)
                      + " mastered="
                      + SkillAPI.getSkillsFrom(p)
                          .getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get())
                          .orElseThrow()
                          .isMastered(p)));
      phase = 405;
      wait = 0;
      return;
    }
    if (phase == 405 && wait > 30) {
      if (Math.hypot(mc.player.getX() - warpX, mc.player.getZ() - warpZ) > 2) {
        check(wait < 600, "Native Warp confirmation did not teleport to found structure");
        return;
      }
      check(
          Math.abs(mc.player.getY() - warpY) < 4,
          "Arrival Y did not match the prefilled safe location");
      phase = 406;
      wait = 0;
      return;
    }
    if (phase == 406 && wait > 25) {
      shot("marauders-map-arrival");
      try {
        Files.writeString(
            OUT.resolve("passed-travel.txt"),
            "PASS: actual cold native stronghold search beyond 2,048 and within 100,000 blocks;"
                + " owned-only spatial skill chooser; prefilled native Spatial Motion and Instant"
                + " Transmission menus; no warp before confirmation; actual native Warp button"
                + " keyboard confirmation and mastered aura-based teleport to a safe surface"
                + " arrival.\n");
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
      mc.stop();
    }
    if (phase == 500 && mc.screen instanceof MaraudersMapScreen && wait > 20) {
      mc.options.guiScale().set(4);
      mc.resizeDisplay();
      fits();
      server(
          p -> {
            check(
                p.serverLevel().structureManager().shouldGenerateStructures(),
                "Review world disables structures");
            MapData.get(p.server).structures.clear();
            MapData.get(p.server).setDirty();
            surveyRegionsBefore = surveyRegions(p);
          });
      map().view.scale = .125;
      press("Go to…");
      field("X").setValue("300000");
      field("Z").setValue("-310000");
      press("Center map");
      phase = 501;
      wait = 0;
      return;
    }
    if (phase == 501 && wait > 30) {
      if (state().getInt("viewX") != 300000
          || state().getBoolean("surveying")
          || state().getInt("surveyTotal") == 0) {
        if (wait % 200 == 0)
          System.out.println(
              "Auto structures: "
                  + state().getInt("surveyDone")
                  + "/"
                  + state().getInt("surveyTotal"));
        check(wait < 6000, "Automatic unexplored-area discovery timed out");
        return;
      }
      check(!state().getBoolean("surveyError"), "Automatic discovery reported an error");
      var markers = state().getList("markers", net.minecraft.nbt.Tag.TAG_COMPOUND);
      check(
          markers.size() > 15,
          "Too few automatic markers in unexplored wide view: " + markers.size());
      check(
          markers.stream()
              .anyMatch(t -> ((net.minecraft.nbt.CompoundTag) t).getBoolean("predicted")),
          "No ungenerated-area locations found");
      server(
          p -> {
            check(MapService.session(p).search == null, "Automatic view required Locate");
            check(
                surveyRegions(p).equals(surveyRegionsBefore),
                "Survey created distant region files");
          });
      System.out.println("Auto structures first view markers=" + markers.size());
      phase = 502;
      wait = 0;
      return;
    }
    if (phase == 502 && wait > 15) {
      shot("marauders-map-auto-structures");
      press("Go to…");
      field("X").setValue("302048");
      field("Z").setValue("-310000");
      press("Center map");
      phase = 503;
      wait = 0;
      return;
    }
    if (phase == 503 && wait > 30) {
      if (state().getInt("viewX") != 302048 || state().getBoolean("surveying")) {
        check(wait < 6000, "Panned automatic survey did not finish");
        return;
      }
      check(!state().getBoolean("surveyError"), "Panned survey failed");
      var markers = state().getList("markers", net.minecraft.nbt.Tag.TAG_COMPOUND);
      check(markers.size() > 15, "Panned map did not reveal structures");
      autoMarker =
          markers.stream()
              .map(t -> (net.minecraft.nbt.CompoundTag) t)
              .filter(t -> t.getBoolean("predicted"))
              .findFirst()
              .orElseThrow()
              .copy();
      server(
          p ->
              check(
                  surveyRegions(p).equals(surveyRegionsBefore),
                  "Panning generated terrain regions"));
      phase = 504;
      wait = 0;
      return;
    }
    if (phase == 504 && wait > 15) {
      shot("marauders-map-auto-pan");
      map().view.scale = .5;
      press("Go to…");
      field("X").setValue(String.valueOf(autoMarker.getInt("x")));
      field("Z").setValue(String.valueOf(autoMarker.getInt("z")));
      press("Center map");
      phase = 505;
      wait = 0;
      return;
    }
    if (phase == 505 && wait > 30) {
      if (state().getBoolean("surveying")) {
        check(wait < 1800, "Zoomed automatic survey stalled");
        return;
      }
      shot("marauders-map-auto-zoom");
      server(
          p -> {
            check(surveyRegions(p).equals(surveyRegionsBefore), "Zooming created terrain regions");
            String key = autoMarker.getString("id");
            long packed = Long.parseLong(key.substring(key.lastIndexOf('/') + 1));
            var pos = new net.minecraft.world.level.ChunkPos(packed);
            check(
                p.serverLevel().getChunkSource().getChunkNow(pos.x, pos.z) == null,
                "Survey loaded unseen terrain");
            var access =
                p.serverLevel()
                    .getChunk(
                        pos.x,
                        pos.z,
                        net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
            var type =
                p.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                    .get(
                        net.minecraft.resources.ResourceLocation.parse(
                            autoMarker.getString("type")));
            var start = access.getStartForStructure(type);
            check(
                start != null && start.isValid(),
                "Automatic marker picked wrong native structure variant");
            var box = start.getBoundingBox();
            check(
                box.getCenter().getX() == autoMarker.getInt("x")
                    && box.getCenter().getZ() == autoMarker.getInt("z")
                    && box.minY() == autoMarker.getInt("y"),
                "Automatic coordinates differed from actual generation");
            MapService.capture(p.serverLevel(), access);
            check(
                !MapData.get(p.server).structures.get(key).predicted(),
                "Native confirmation did not replace prediction");
            System.out.println("Auto structures verified native start: " + autoMarker);
            autoVerified = true;
          });
      phase = 506;
      wait = 0;
      return;
    }
    if (phase == 506 && wait > 20 && autoVerified) {
      try {
        Files.writeString(
            OUT.resolve("passed-auto-structures.txt"),
            "PASS: automatic markers in an unexplored wide view, independent panning and zoom"
                + " discovery without Locate; no distant region-file creation or full terrain"
                + " loads; exact native structure variant/bounds comparison; generated locations"
                + " upgraded to confirmed starts.\n");
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
      phase = 999;
      map().onClose();
      mc.stop();
    }
  }
}
