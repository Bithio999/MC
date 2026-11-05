package dev.heavenlyrestriction.client.cartography;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.cartography.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Quaternionf;

/** A north-up atlas: terrain is the canvas; controls and markers stay readable at every zoom. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class MaraudersMapScreen extends Screen {
  public static final int GOLD = 0xffe6c78a,
      TEXT = 0xffede8dc,
      MUTED = 0xffaaa99f,
      TEAL = 0xff81cbbb;
  public static final int[] COLORS = {
    0xffedc575, 0xffeb7976, 0xff88c7e4, 0xff96d79b, 0xffc3a2ed, 0xffefab72, 0xffeee9db, 0xff72d6d1
  };
  public static final String[] ICONS = {"Pin", "Home", "Danger", "Treasure", "Portal", "Camp"};
  public final MapView view = new MapView();
  private CompoundTag state;
  private UUID token;
  private int left, top, mapW, mapH, side, listTop, listBottom, scroll, age, refreshAt;
  private boolean structures = true, pins = true, players = true, dragging, moved, returned;
  private double downX, downY;
  private String selected = "";
  private EditBox search;
  private MapPopup popup;
  private String pendingDimension = "";
  private final Map<String, double[]> dimensionViews = new HashMap<>();
  private String lastFound = "";
  private List<CompoundTag> entries = List.of();
  private final List<Hit> hits = new ArrayList<>();

  private record Hit(int x, int y, CompoundTag marker, int count) {}

  public MaraudersMapScreen(CompoundTag state) {
    super(Component.literal("Marauder's Map"));
    this.state = state.copy();
    token = state.getUUID("session");
    MapTerrainAtlas.context(state);
    MapTerrainAtlas.session(token);
    var p = Minecraft.getInstance().player;
    view.center(p == null ? 0 : p.getX(), p == null ? 0 : p.getZ());
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(() -> MapNetwork.receiver = MaraudersMapScreen::receive);
  }

  @EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
  public static final class Recorder {
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
      MapAtlas.tick();
      MapTerrainAtlas.tick();
    }
  }

  public static void receive(CompoundTag data) {
    if (data.getBoolean("terrain")) {
      MapTerrainAtlas.receive(data);
      return;
    }
    if (data.hasUUID("world"))
      MapAtlas.context(
          data.getUUID("world"),
          data.contains("playerDimension")
              ? data.getString("playerDimension")
              : data.getString("dimension"));
    var mc = Minecraft.getInstance();
    MaraudersMapScreen screen = null;
    if (mc.screen instanceof MaraudersMapScreen m) screen = m;
    else if (mc.screen instanceof MapPinScreen p) screen = p.parent;
    else if (mc.screen instanceof MapStructureScreen s) screen = s.parent;
    else if (mc.screen instanceof MapHelpScreen h) screen = h.parent;
    else if (mc.screen instanceof MapTravelScreen t) screen = t.parent;
    if (screen != null
        && data.getBoolean("dimensionChanged")
        && data.hasUUID("previousSession")
        && screen.token.equals(data.getUUID("previousSession"))) {
      screen.token = data.getUUID("session");
      screen.state = data.copy();
      screen.pendingDimension = "";
      screen.selected = "";
      screen.scroll = 0;
      screen.lastFound = "";
      screen.popup = null;
      double[] saved = screen.dimensionViews.get(data.getString("dimension"));
      screen.view.center(data.getInt("viewX"), data.getInt("viewZ"));
      if (saved != null) screen.view.scale = saved[2];
      MapTerrainAtlas.context(data);
      MapTerrainAtlas.session(screen.token);
      if (mc.screen != screen) mc.setScreen(screen);
      else screen.rebuildWidgets();
      return;
    }
    if (screen != null && data.hasUUID("session") && screen.token.equals(data.getUUID("session"))) {
      if (data.getBoolean("dimensionFailed")) screen.pendingDimension = "";
      if (data.contains("travelOptions")) {
        mc.setScreen(
            new MapTravelScreen(
                screen,
                data.getCompound("target"),
                data.getList("travelOptions", Tag.TAG_COMPOUND)));
        return;
      }
      if (data.getBoolean("openTravel")) {
        var key = net.minecraft.resources.ResourceLocation.tryParse(data.getString("skill"));
        var skill =
            key == null
                ? null
                : io.github.manasmods.manascore.skill.api.SkillAPI.getSkillRegistry().get(key);
        if (skill instanceof io.github.manasmods.tensura.ability.subclass.ISpatialMovement) {
          screen.returned = true;
          MapAtlas.flush();
          var dimensions =
              data.getList("dimensions", Tag.TAG_STRING).stream()
                  .map(net.minecraft.nbt.Tag::getAsString)
                  .toList();
          mc.setScreen(
              new MapWarpScreen(
                  (io.github.manasmods.manascore.skill.api.ManasSkill
                          & io.github.manasmods.tensura.ability.subclass.ISpatialMovement)
                      skill,
                  dimensions,
                  data));
        }
        return;
      }
    }
    if (data.getBoolean("closed")) {
      if (screen != null) {
        screen.returned = true;
        mc.setScreen(null);
      }
      return;
    }
    if (data.getBoolean("open")) {
      mc.setScreen(new MaraudersMapScreen(data));
      return;
    }
    if (screen != null && data.hasUUID("session") && screen.token.equals(data.getUUID("session"))) {
      if (data.getBoolean("playersOnly"))
        screen.state.put("players", data.getList("players", Tag.TAG_COMPOUND).copy());
      else {
        var catalog = screen.state.getList("types", Tag.TAG_COMPOUND);
        screen.state = data.copy();
        if (!data.contains("types")) screen.state.put("types", catalog);
        MapTerrainAtlas.context(screen.state);
        MapTerrainAtlas.session(screen.token);
        if (data.contains("found")) {
          var found = data.getCompound("found");
          if (!found.getString("id").equals(screen.lastFound)) {
            screen.lastFound = found.getString("id");
            screen.selected = screen.lastFound;
            screen.search.setValue("");
            screen.center(found.getInt("x"), found.getInt("z"));
          }
        }
      }
      screen.filter();
    }
  }

  @Override
  protected void setInitialFocus() {
    // Native skill activation is a keyboard input: keep the canvas ready for navigation.
  }

  private Button button(int x, int y, int w, String label, Runnable action) {
    var b =
        MapUi.button(Component.literal(label), ignore -> action.run()).bounds(x, y, w, 20).build();
    addRenderableWidget(b);
    return b;
  }

  @Override
  protected void init() {
    String savedQuery = search == null ? "" : search.getValue();
    side = Math.clamp(width / 4, 148, 210);
    left = side + 12;
    top = 49;
    mapW = Math.max(40, width - left - 10);
    mapH = Math.max(40, height - top - 39);
    listTop = 103;
    listBottom = height - 48;
    popup = null;
    search =
        new EditBox(font, 10, 49, side - 8, 18, Component.literal("Search structures and pins"));
    search.setMaxLength(96);
    search.setValue(savedQuery);
    search.setHint(Component.literal("Search markers…"));
    search.setResponder(
        q -> {
          scroll = 0;
          filter();
        });
    addRenderableWidget(search);
    button(10, 74, (side - 12) / 2, "Layers ▾", this::layersMenu);
    button(12 + (side - 12) / 2, 74, (side - 12) / 2, "Map tools ▾", this::toolsMenu);
    String[] dimensions = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};
    String[] labels = {"Overworld", "The Nether", "The End"};
    net.minecraft.world.item.Item[] art = {
      net.minecraft.world.item.Items.GRASS_BLOCK,
      net.minecraft.world.item.Items.CRIMSON_NYLIUM,
      net.minecraft.world.item.Items.END_STONE
    };
    int[] accent = {0xff9acb83, 0xffef9a72, 0xffd0b0f0};
    for (int i = 0; i < 3; i++) {
      String id = dimensions[i];
      var tab =
          new MapDimensionTab(
              left + i * 34,
              10,
              labels[i],
              art[i],
              dimension().equals(id),
              accent[i],
              () -> switchDimension(id));
      tab.active = hasDimension(id);
      addRenderableWidget(tab);
    }
    boolean other = !Arrays.asList(dimensions).contains(dimension());
    addRenderableWidget(
        new MapDimensionTab(
            left + 102,
            10,
            other ? "Other · " + state.getString("dimensionName") : "Other dimensions",
            net.minecraft.world.item.Items.ENDER_EYE,
            other,
            TEAL,
            this::otherDimensions));
    var recenter = button(left + mapW - 48, 14, 22, "⌖", this::centerPlayer);
    recenter.setTooltip(Tooltip.create(Component.literal("Your location · Home")));
    var close = button(left + mapW - 22, 14, 22, "×", this::onClose);
    close.setTooltip(Tooltip.create(Component.literal("Close map")));
    button(
        left + mapW - 49,
        top + mapH - 25,
        20,
        "−",
        () -> zoom(-1, left + mapW / 2d, top + mapH / 2d));
    button(
        left + mapW - 26,
        top + mapH - 25,
        20,
        "+",
        () -> zoom(1, left + mapW / 2d, top + mapH / 2d));
    button(
        10,
        height - 34,
        side - 8,
        "Locate structure…",
        () ->
            minecraft.setScreen(
                new MapStructureScreen(this, state.getList("types", Tag.TAG_COMPOUND))));
    filter();
    refreshAt = age + 1;
  }

  public String dimension() {
    return state.getString("dimension");
  }

  int minY() {
    return state.contains("minY") ? state.getInt("minY") : minecraft.level.getMinBuildHeight();
  }

  int maxY() {
    return state.contains("maxY") ? state.getInt("maxY") : minecraft.level.getMaxBuildHeight();
  }

  private boolean localDimension() {
    return minecraft.level != null
        && dimension().equals(minecraft.level.dimension().location().toString());
  }

  private boolean hasDimension(String id) {
    return state.getList("dimensions", Tag.TAG_COMPOUND).stream()
        .anyMatch(t -> ((CompoundTag) t).getString("id").equals(id));
  }

  private void menu(String title, List<MapPopup.Action> actions, int x, int y, boolean search) {
    dragging = false;
    this.search.setFocused(false);
    clearFocus();
    popup = new MapPopup(title, actions, x, y, width, height, search);
  }

  private void layersMenu() {
    menu(
        "Map layers",
        List.of(
            new MapPopup.Action(
                (structures ? "✓ " : "  ") + "Structures",
                () -> {
                  structures = !structures;
                  filter();
                  refresh();
                }),
            new MapPopup.Action(
                (pins ? "✓ " : "  ") + "Custom pins",
                () -> {
                  pins = !pins;
                  filter();
                }),
            new MapPopup.Action(
                (players ? "✓ " : "  ") + "Players",
                () -> {
                  players = !players;
                  filter();
                })),
        10,
        96,
        false);
  }

  private void toolsMenu() {
    var actions = new ArrayList<MapPopup.Action>();
    actions.add(
        new MapPopup.Action(
            "Go to coordinates…", () -> minecraft.setScreen(MapPinScreen.gotoPoint(this))));
    if (state.contains("found"))
      actions.add(
          new MapPopup.Action(
              "Show located structure",
              () -> {
                var found = state.getCompound("found");
                search.setValue("");
                selected = found.getString("id");
                center(found.getInt("x"), found.getInt("z"));
              }));
    actions.add(
        new MapPopup.Action("Map guide", () -> minecraft.setScreen(new MapHelpScreen(this))));
    menu("Map tools", actions, 12 + (side - 12) / 2, 96, false);
  }

  private void otherDimensions() {
    var actions = new ArrayList<MapPopup.Action>();
    state.getList("dimensions", Tag.TAG_COMPOUND).stream()
        .map(t -> (CompoundTag) t)
        .filter(
            t ->
                !Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end")
                    .contains(t.getString("id")))
        .sorted(Comparator.comparing(t -> t.getString("name")))
        .forEach(
            t ->
                actions.add(
                    new MapPopup.Action(
                        t.getString("name") + " · " + t.getString("id"),
                        () -> switchDimension(t.getString("id")))));
    menu("Other dimensions", actions, left + 102, 43, true);
  }

  public void switchDimension(String id) {
    if (!pendingDimension.isEmpty() || !hasDimension(id) || dimension().equals(id)) return;
    dimensionViews.put(dimension(), new double[] {view.x, view.z, view.scale});
    double[] saved = dimensionViews.get(id);
    double x = saved == null ? view.x : saved[0], z = saved == null ? view.z : saved[1];
    if (saved == null && minecraft.level != null) {
      String actual = minecraft.level.dimension().location().toString();
      if (id.equals(actual) && minecraft.player != null) {
        x = minecraft.player.getX();
        z = minecraft.player.getZ();
      } else if (dimension().equals("minecraft:overworld") && id.equals("minecraft:the_nether")) {
        x /= 8;
        z /= 8;
      } else if (dimension().equals("minecraft:the_nether") && id.equals("minecraft:overworld")) {
        x *= 8;
        z *= 8;
      } else {
        x = 0;
        z = 0;
      }
    }
    var t = new CompoundTag();
    t.putString("dimension", id);
    t.putInt("x", Mth.floor(Math.clamp(x, -MapView.LIMIT, MapView.LIMIT)));
    t.putInt("z", Mth.floor(Math.clamp(z, -MapView.LIMIT, MapView.LIMIT)));
    pendingDimension = id;
    popup = null;
    request("dimension", t);
  }

  private void markerMenu(CompoundTag marker, int x, int y) {
    selected = marker.getString("id");
    var m = marker.copy();
    var actions = new ArrayList<MapPopup.Action>();
    actions.add(
        new MapPopup.Action(
            "Focus on map",
            () -> {
              if (!m.getString("dimension").isEmpty()
                  && !m.getString("dimension").equals(dimension())) {
                dimensionViews.put(
                    m.getString("dimension"),
                    new double[] {m.getInt("x"), m.getInt("z"), view.scale});
                switchDimension(m.getString("dimension"));
              } else center(m.getInt("x"), m.getInt("z"));
            }));
    if (m.getBoolean("pin")) {
      actions.add(
          new MapPopup.Action("Edit pin…", () -> minecraft.setScreen(new MapPinScreen(this, m))));
      actions.add(
          new MapPopup.Action(
              "Delete pin…", () -> minecraft.setScreen(MapPinScreen.delete(this, m))));
    } else if (!m.getBoolean("player")) {
      actions.add(
          new MapPopup.Action(
              "Travel…",
              state.getInt("travelSkills") > 0
                  && !state.getBoolean("preparingTravel")
                  && localDimension(),
              () -> {
                var t = new CompoundTag();
                t.putString("id", m.getString("id"));
                request("routes", t);
              }));
      actions.add(
          new MapPopup.Action("Save as custom pin…", () -> pinAt(m.getInt("x"), m.getInt("z"))));
    }
    menu(m.getString("name"), actions, x, y, false);
    filter();
  }

  private void canvasMenu(double mx, double my) {
    double x = view.worldX(mx, left, mapW), z = view.worldZ(my, top, mapH);
    menu(
        "X " + Mth.floor(x) + " · Z " + Mth.floor(z),
        List.of(
            new MapPopup.Action("Add pin here…", () -> pinAt(x, z)),
            new MapPopup.Action("Center here", () -> center(x, z))),
        (int) mx,
        (int) my,
        false);
  }

  void request(String action, CompoundTag data) {
    MapNetwork.send(token, action, data);
  }

  void center(double x, double z) {
    view.center(x, z);
    refreshAt = age + 1;
  }

  void refresh() {
    refreshAt = age + 6;
  }

  private void centerPlayer() {
    if (minecraft.player != null) {
      if (!localDimension()) {
        String id = minecraft.player.level().dimension().location().toString();
        dimensionViews.put(
            id, new double[] {minecraft.player.getX(), minecraft.player.getZ(), view.scale});
        switchDimension(id);
      } else center(minecraft.player.getX(), minecraft.player.getZ());
    }
  }

  private void zoom(double step, double mx, double my) {
    view.zoom(step, mx, my, left, top, mapW, mapH);
    refreshAt = age + 6;
  }

  private void pinAt(double x, double z) {
    var p = new CompoundTag();
    p.putInt("x", (int) Math.floor(x));
    p.putInt("z", (int) Math.floor(z));
    int y = minecraft.player == null ? 64 : minecraft.player.getBlockY();
    if (localDimension()
        && minecraft.level.hasChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4))
      y =
          minecraft.level.getHeight(
              net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
              (int) Math.floor(x),
              (int) Math.floor(z));
    p.putInt("y", Mth.clamp(y, minY(), maxY() - 1));
    p.putString("name", "");
    minecraft.setScreen(new MapPinScreen(this, p));
  }

  private CompoundTag chosen() {
    return entries.stream()
        .filter(m -> m.getString("id").equals(selected))
        .findFirst()
        .orElse(null);
  }

  private CompoundTag travelTarget() {
    var m = chosen();
    if (m != null) return !m.getBoolean("pin") && !m.getBoolean("player") ? m : null;
    return state.contains("found") ? state.getCompound("found") : null;
  }

  private void filter() {
    String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
    var all = new ArrayList<CompoundTag>();
    if (pins) for (var t : state.getList("pins", Tag.TAG_COMPOUND)) all.add((CompoundTag) t);
    if (players) for (var t : state.getList("players", Tag.TAG_COMPOUND)) all.add((CompoundTag) t);
    if (structures)
      for (var t : state.getList("markers", Tag.TAG_COMPOUND)) all.add((CompoundTag) t);
    var p = Minecraft.getInstance().player;
    all.removeIf(
        m ->
            !(m.getString("name")
                    + " "
                    + m.getString("type")
                    + " "
                    + m.getString("dimensionName")
                    + " "
                    + m.getInt("x")
                    + " "
                    + m.getInt("z"))
                .toLowerCase(Locale.ROOT)
                .contains(q));
    all.sort(
        Comparator.comparing((CompoundTag m) -> !m.getBoolean("pin"))
            .thenComparingDouble(
                m -> p == null ? 0 : Math.hypot(m.getInt("x") - p.getX(), m.getInt("z") - p.getZ()))
            .thenComparing(m -> m.getString("name")));
    entries = List.copyOf(all);
    scroll =
        Math.clamp(
            scroll, 0, Math.max(0, entries.size() - Math.max(1, (listBottom - listTop) / 32)));
  }

  @Override
  public void tick() {
    age++;
    if (age % 100 == 0 && refreshAt < 0) refreshAt = age;
    if (age % 20 == 0) filter();
    if (refreshAt > 0 && age >= refreshAt && pendingDimension.isEmpty()) {
      var t = new CompoundTag();
      t.putInt("x", (int) view.x);
      t.putInt("z", (int) view.z);
      t.putInt("radius", (int) Math.min(65536, Math.max(mapW, mapH) / (2 * view.scale) + 128));
      t.putBoolean("survey", structures);
      t.putInt("minX", (int) Math.floor(view.worldX(left, left, mapW)));
      t.putInt("maxX", (int) Math.ceil(view.worldX(left + mapW, left, mapW)));
      t.putInt("minZ", (int) Math.floor(view.worldZ(top, top, mapH)));
      t.putInt("maxZ", (int) Math.ceil(view.worldZ(top + mapH, top, mapH)));
      request("view", t);
      refreshAt = -1;
    }
  }

  private String fit(String s, int max) {
    return font.width(s) <= max ? s : font.plainSubstrByWidth(s, Math.max(0, max - 8)) + "…";
  }

  public boolean terrainViewReady() {
    return pendingDimension.isEmpty()
        && state.getInt("viewX") == (int) view.x
        && state.getInt("viewZ") == (int) view.z
        && state.getInt("viewRadius")
            >= Math.min(65536, Math.max(mapW, mapH) / (2 * view.scale) + 96);
  }

  public boolean visibleChunk(int x, int z) {
    return view.screenX(x * 16d + 16, left, mapW) >= left
        && view.screenX(x * 16d, left, mapW) <= left + mapW
        && view.screenZ(z * 16d + 16, top, mapH) >= top
        && view.screenZ(z * 16d, top, mapH) <= top + mapH;
  }

  private boolean inside(double x, double y) {
    return x >= left && x < left + mapW && y >= top && y < top + mapH;
  }

  public static int markerColor(CompoundTag m) {
    if (m.getBoolean("player")) return 0xff91e3ed;
    if (m.getBoolean("pin")) return COLORS[Math.floorMod(m.getInt("color"), COLORS.length)];
    String id = m.getString("type");
    if (id.contains("village")) return 0xffe6c78a;
    if (id.contains("fortress") || id.contains("outpost") || id.contains("stronghold"))
      return 0xffeb8c83;
    if (id.contains("ocean") || id.contains("shipwreck") || id.contains("monument"))
      return 0xff8dccea;
    if (id.contains("temple") || id.contains("pyramid")) return 0xffedb175;
    if (id.contains("ancient_city") || id.contains("trial") || id.contains("mineshaft"))
      return 0xffc2a2e5;
    return 0xff79cfc2;
  }

  private void marker(GuiGraphics g, int x, int y, CompoundTag m, int count) {
    int color = markerColor(m);
    boolean active = m.getString("id").equals(selected);
    if (active) {
      g.fill(x - 7, y - 7, x + 8, y + 8, 0xc0332b1c);
      g.renderOutline(x - 8, y - 8, 17, 17, GOLD);
    }
    g.fill(x - 5, y - 5, x + 6, y + 6, 0xee131c20);
    if (m.getBoolean("pin")) {
      int icon = m.getInt("icon");
      if (icon == 0) {
        g.fill(x - 2, y - 4, x + 3, y + 1, color);
        g.fill(x, y + 1, x + 1, y + 6, color);
      } else {
        String[] glyph = {"", "H", "!", "$", "O", "^"};
        g.drawCenteredString(font, glyph[Math.floorMod(icon, glyph.length)], x + 1, y - 4, color);
      }
    } else {
      for (int dy = -3; dy <= 3; dy++)
        g.fill(x - (3 - Math.abs(dy)), y + dy, x + (3 - Math.abs(dy)) + 1, y + dy + 1, color);
    }
    if (count > 1) {
      String text = String.valueOf(count);
      g.fill(x + 4, y - 8, x + 7 + font.width(text), y + 2, 0xef172126);
      g.drawString(font, text, x + 6, y - 7, GOLD, false);
    }
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    g.fill(0, 0, width, height, 0xff10171d);
    g.fill(0, 0, side + 4, height, 0xff19232a);
    g.fill(side + 4, 0, side + 5, height, 0xff3c4b50);
    g.drawString(font, fit("MARAUDER'S MAP", side - 14), 10, 12, GOLD, false);
    g.drawString(font, fit(state.getString("dimensionName"), side - 12), 10, 29, MUTED, false);
    g.fill(left - 1, top - 1, left + mapW + 1, top + mapH + 1, 0xff526165);
    g.fill(left, top, left + mapW, top + mapH, 0xff202c32);
    g.enableScissor(left, top, left + mapW, top + mapH);
    for (int gy = top; gy < top + mapH; gy += 24)
      for (int gx = left; gx < left + mapW; gx += 24)
        if (((gx - left) / 24 + (gy - top) / 24) % 2 == 0)
          g.fill(gx, gy, Math.min(left + mapW, gx + 24), Math.min(top + mapH, gy + 24), 0xff253139);
    MapTerrainAtlas.render(g, view, left, top, mapW, mapH, false);
    if (localDimension()) MapAtlas.render(g, view, left, top, mapW, mapH);
    MapTerrainAtlas.render(g, view, left, top, mapW, mapH, true);
    if (view.scale >= .5) {
      int grid = view.scale < 1 ? 128 : 64;
      int x0 = (int) Math.floor(view.worldX(left, left, mapW) / grid) * grid,
          z0 = (int) Math.floor(view.worldZ(top, top, mapH) / grid) * grid;
      for (int x = x0; x <= view.worldX(left + mapW, left, mapW); x += grid) {
        int sx = (int) view.screenX(x, left, mapW);
        g.fill(sx, top, sx + 1, top + mapH, 0x224f6b70);
      }
      for (int z = z0; z <= view.worldZ(top + mapH, top, mapH); z += grid) {
        int sy = (int) view.screenZ(z, top, mapH);
        g.fill(left, sy, left + mapW, sy + 1, 0x224f6b70);
      }
    }
    hits.clear();
    var clusters = new LinkedHashMap<Long, List<CompoundTag>>();
    for (var m : entries) {
      int x = (int) view.screenX(m.getInt("x"), left, mapW),
          y = (int) view.screenZ(m.getInt("z"), top, mapH);
      if (!inside(x, y)) continue;
      if (!m.getBoolean("pin") && !m.getBoolean("player")) {
        long cell = ((long) Math.floorDiv(x, 18) << 32) | (Math.floorDiv(y, 18) & 0xffffffffL);
        clusters.computeIfAbsent(cell, k -> new ArrayList<>()).add(m);
      }
    }
    for (var group : clusters.values()) {
      var m =
          group.stream()
              .filter(a -> a.getString("id").equals(selected))
              .findFirst()
              .orElse(group.getFirst());
      int x = (int) view.screenX(m.getInt("x"), left, mapW),
          y = (int) view.screenZ(m.getInt("z"), top, mapH);
      int count = group.stream().mapToInt(a -> Math.max(1, a.getInt("count"))).sum();
      marker(g, x, y, m, count);
      hits.add(new Hit(x, y, m, count));
    }
    for (var m : entries)
      if (m.getBoolean("pin")) {
        int x = (int) view.screenX(m.getInt("x"), left, mapW),
            y = (int) view.screenZ(m.getInt("z"), top, mapH);
        if (inside(x, y)) {
          marker(g, x, y, m, 1);
          hits.add(0, new Hit(x, y, m, 1));
        }
      }
    for (var m : entries)
      if (m.getBoolean("player")
          && m.getString("dimension").equals(state.getString("dimension"))
          && (minecraft.player == null || !m.getUUID("uuid").equals(minecraft.player.getUUID()))) {
        int px = (int) view.screenX(m.getDouble("px"), left, mapW),
            pz = (int) view.screenZ(m.getDouble("pz"), top, mapH);
        if (!inside(px, pz)) continue;
        g.fill(px - 5, pz - 5, px + 6, pz + 6, 0xf0101c25);
        g.fill(px - 3, pz - 3, px + 4, pz + 4, markerColor(m));
        g.renderOutline(px - 6, pz - 6, 13, 13, 0xffe1f6f5);
        String label = fit(m.getString("name"), Math.min(120, mapW / 2));
        int labelWidth = font.width(label);
        g.fill(px - labelWidth / 2 - 3, pz + 9, px + labelWidth / 2 + 4, pz + 21, 0xd5101b24);
        g.drawCenteredString(font, label, px, pz + 11, markerColor(m));
        hits.add(0, new Hit(px, pz, m, 1));
      }
    if (minecraft.player != null && localDimension()) {
      int px = (int) view.screenX(minecraft.player.getX(), left, mapW),
          pz = (int) view.screenZ(minecraft.player.getZ(), top, mapH);
      if (inside(px, pz)) {
        g.pose().pushPose();
        g.pose().translate(px, pz, 0);
        g.pose()
            .mulPose(
                new Quaternionf()
                    .rotationZ((float) Math.toRadians(minecraft.player.getYRot() + 180)));
        for (int yy = -6; yy <= 4; yy++) {
          int half = yy < 0 ? (yy + 6) / 2 + 1 : 1;
          g.fill(-half - 1, yy, half + 2, yy + 1, 0xff1a1712);
          g.fill(-half, yy, half + 1, yy + 1, GOLD);
        }
        g.pose().popPose();
      }
    }
    g.disableScissor();
    g.fill(left + mapW - 27, top + 7, left + mapW - 6, top + 30, 0xdc141e24);
    g.drawCenteredString(font, "N", left + mapW - 16, top + 11, GOLD);
    g.drawCenteredString(font, "↑", left + mapW - 16, top + 21, GOLD);
    int scaleBlocks = 1;
    double target = 90 / view.scale;
    while (scaleBlocks * 10 < target) scaleBlocks *= 10;
    if (scaleBlocks * 5 <= target) scaleBlocks *= 5;
    else if (scaleBlocks * 2 <= target) scaleBlocks *= 2;
    int bar = (int) (scaleBlocks * view.scale);
    g.fill(left + 9, top + mapH - 23, left + bar + 19, top + mapH - 4, 0xcd10191f);
    g.fill(left + 14, top + mapH - 10, left + bar + 14, top + mapH - 8, TEXT);
    g.fill(left + 14, top + mapH - 13, left + 15, top + mapH - 7, TEXT);
    g.fill(left + bar + 13, top + mapH - 13, left + bar + 14, top + mapH - 7, TEXT);
    g.drawString(font, scaleBlocks + " blocks", left + 14, top + mapH - 21, TEXT, false);
    int visible = Math.max(1, (listBottom - listTop) / 32);
    g.enableScissor(7, listTop, side + 1, Math.max(listTop, listBottom));
    for (int n = 0; n < visible && scroll + n < entries.size(); n++) {
      var m = entries.get(scroll + n);
      int yy = listTop + n * 32;
      boolean chosen = m.getString("id").equals(selected);
      g.fill(8, yy, side, yy + 30, chosen ? 0xff36484a : 0xff202e34);
      if (chosen) g.fill(8, yy, 10, yy + 30, GOLD);
      g.drawString(font, fit(m.getString("name"), side - 24), 16, yy + 5, markerColor(m), false);
      String detail = m.getInt("x") + ", " + m.getInt("z");
      boolean otherDimension =
          m.getBoolean("player") && !m.getString("dimension").equals(state.getString("dimension"));
      if (otherDimension) detail = m.getString("dimensionName") + " · " + detail;
      if (minecraft.player != null && !otherDimension && localDimension())
        detail +=
            " · "
                + (int)
                    Math.hypot(
                        m.getInt("x") - minecraft.player.getX(),
                        m.getInt("z") - minecraft.player.getZ())
                + "m";
      g.drawString(font, fit(detail, side - 24), 16, yy + 17, MUTED, false);
    }
    if (entries.size() > visible) {
      int bh = Math.max(1, listBottom - listTop),
          thumb = Math.max(8, bh * visible / entries.size()),
          yy = listTop + (bh - thumb) * scroll / Math.max(1, entries.size() - visible);
      g.fill(side - 3, listTop, side - 1, listBottom, 0xff122028);
      g.fill(side - 3, yy, side - 1, yy + thumb, TEAL);
    }
    if (entries.isEmpty()) {
      g.drawWordWrap(
          font,
          Component.literal(
              search.getValue().isEmpty()
                  ? "Markers appear as this area is surveyed. Right-click the map to add a pin."
                  : "No matching markers or players."),
          12,
          listTop + 8,
          side - 16,
          MUTED);
    }
    g.disableScissor();
    String location =
        inside(mx, my)
            ? "X "
                + (int) Math.floor(view.worldX(mx, left, mapW))
                + "  Z "
                + (int) Math.floor(view.worldZ(my, top, mapH))
            : "Center  " + (int) view.x + ", " + (int) view.z;
    g.drawString(
        font,
        fit(
            location
                + "  ·  "
                + (state.getInt("plane") == Integer.MAX_VALUE
                    ? "Surface terrain"
                    : "Below roof · Y " + state.getInt("plane")),
            mapW),
        left,
        height - 35,
        TEXT,
        false);
    String note =
        pendingDimension.isEmpty()
            ? state.getString("message")
            : "Opening " + MapService.name(pendingDimension) + "…";
    if (state.getBoolean("searching"))
      note =
          "Searching "
              + MapService.name(state.getString("searchType"))
              + " · "
              + state.getInt("candidates")
              + " candidates  [Esc cancels]";
    if (state.getBoolean("preparingTravel")) note = "Preparing safe arrival coordinates…";
    if (note.isEmpty() && state.getBoolean("surveying"))
      note =
          "Discovering structures · "
              + state.getInt("surveyDone")
              + "/"
              + state.getInt("surveyTotal")
              + " areas";
    if (note.isEmpty() && state.getBoolean("surveyError"))
      note = "Some structures could not be read. Reopen the map to retry.";
    if (note.isEmpty()) note = MapTerrainAtlas.progress();
    if (note.isEmpty() && !MapAtlas.notice.isEmpty()) note = MapAtlas.notice;
    if (note.isEmpty())
      note = "Drag to pan · Scroll to zoom · Right-click to pin · Home centers on you";
    g.drawString(font, fit(note, mapW), left, height - 20, MUTED, false);
    if (MapAtlas.knownChunks() == 0 && MapTerrainAtlas.tileCount() == 0)
      g.drawCenteredString(font, "Mapping world terrain…", left + mapW / 2, top + mapH / 2, MUTED);
    super.render(g, popup == null ? mx : -100, popup == null ? my : -100, partial);
    if (popup != null) popup.render(g, mx, my);
    if (popup == null && inside(mx, my)) {
      var hit =
          hits.stream()
              .filter(h -> Math.abs(h.x - mx) < 9 && Math.abs(h.y - my) < 9)
              .min(Comparator.comparingDouble(h -> Math.hypot(h.x - mx, h.y - my)))
              .orElse(null);
      if (hit != null) {
        var m = hit.marker;
        var lines = new ArrayList<Component>();
        lines.add(Component.literal(m.getString("name")).withColor(GOLD));
        lines.add(
            Component.literal(
                "X " + m.getInt("x") + "  Y " + m.getInt("y") + "  Z " + m.getInt("z")));
        if (m.getBoolean("player"))
          lines.add(Component.literal("Player · " + m.getString("dimensionName")).withColor(TEAL));
        else if (!m.getBoolean("pin"))
          lines.add(Component.literal(m.getString("type")).withColor(MUTED));
        if (m.getBoolean("predicted"))
          lines.add(Component.literal("World-generation location").withColor(TEAL));
        if (hit.count > 1)
          lines.add(Component.literal(hit.count + " nearby structures · zoom in").withColor(TEAL));
        lines.add(
            Component.literal(
                    m.getBoolean("player")
                        ? "Click to select · Live location"
                        : m.getBoolean("pin")
                            ? "Click for pin actions"
                            : "Click for structure actions")
                .withColor(MUTED));
        g.renderTooltip(
            font,
            lines.stream()
                .flatMap(line -> font.split(line, Math.min(240, width - 16)).stream())
                .toList(),
            mx,
            my);
      }
    }
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (popup != null) {
      var current = popup;
      if (current.click(mx, my, button) && popup == current) popup = null;
      return true;
    }
    if (!pendingDimension.isEmpty()) return true;
    // Floating controls are inside the canvas; they must receive clicks before pan/selection.
    if (children().stream()
        .anyMatch(w -> w instanceof AbstractWidget a && a.visible && a.isMouseOver(mx, my)))
      return super.mouseClicked(mx, my, button);
    if (inside(mx, my)) {
      search.setFocused(false);
      clearFocus();
      if (button == 1) {
        canvasMenu(mx, my);
        return true;
      }
      if (button == 0) {
        dragging = true;
        moved = false;
        downX = mx;
        downY = my;
        return true;
      }
    }
    if (button == 0 && mx >= 8 && mx < side && my >= listTop && my < listBottom) {
      search.setFocused(false);
      clearFocus();
      int n = scroll + (int) (my - listTop) / 32;
      if (n < entries.size()) {
        markerMenu(entries.get(n), (int) mx + 8, (int) my + 4);
        return true;
      }
    }
    return super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    if (dragging && button == 0) {
      if (Math.hypot(mx - downX, my - downY) > 3) moved = true;
      if (moved) {
        view.drag(dx, dy);
        refreshAt = age + 7;
      }
      return true;
    }
    return super.mouseDragged(mx, my, button, dx, dy);
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    if (dragging && button == 0) {
      dragging = false;
      if (!moved) {
        var hit =
            hits.stream()
                .filter(h -> Math.abs(h.x - mx) < 10 && Math.abs(h.y - my) < 10)
                .findFirst()
                .orElse(null);
        if (hit != null) {
          markerMenu(hit.marker, (int) mx + 8, (int) my + 4);
        }
      }
      return true;
    }
    return super.mouseReleased(mx, my, button);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    if (popup != null) {
      popup.scroll(dy);
      return true;
    }
    if (inside(mx, my)) {
      zoom(dy, mx, my);
      return true;
    }
    if (mx < side) {
      int count = Math.max(1, (listBottom - listTop) / 32);
      scroll =
          Math.clamp(scroll - (int) Math.signum(dy) * 2, 0, Math.max(0, entries.size() - count));
      return true;
    }
    return super.mouseScrolled(mx, my, dx, dy);
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (popup != null) {
      var current = popup;
      if (current.key(key) && popup == current) popup = null;
      return true;
    }
    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && state.getBoolean("searching")) {
      search.setFocused(false);
      clearFocus();
      request("cancel", new CompoundTag());
      return true;
    }
    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && search.isFocused()) {
      search.setFocused(false);
      clearFocus();
      return true;
    }
    if (search.isFocused()) return super.keyPressed(key, scan, modifiers);
    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_HOME) {
      centerPlayer();
      return true;
    }
    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_F) {
      search.setFocused(true);
      setFocused(search);
      return true;
    }
    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_N) {
      pinAt(view.x, view.z);
      return true;
    }
    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD) {
      zoom(1, left + mapW / 2d, top + mapH / 2d);
      return true;
    }
    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS
        || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT) {
      zoom(-1, left + mapW / 2d, top + mapH / 2d);
      return true;
    }
    int dx = key == 263 || key == 65 ? -1 : key == 262 || key == 68 ? 1 : 0,
        dz = key == 265 || key == 87 ? -1 : key == 264 || key == 83 ? 1 : 0;
    if (dx != 0 || dz != 0) {
      view.center(view.x + dx * 48 / view.scale, view.z + dz * 48 / view.scale);
      refreshAt = age + 7;
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }

  @Override
  public boolean charTyped(char c, int modifiers) {
    if (popup != null) {
      popup.type(c);
      return true;
    }
    return super.charTyped(c, modifiers);
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    // This opaque atlas is already the background; never blur its terrain or labels.
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void onClose() {
    if (!returned) {
      request("close", new CompoundTag());
      MapAtlas.flush();
      returned = true;
    }
    super.onClose();
  }
}
