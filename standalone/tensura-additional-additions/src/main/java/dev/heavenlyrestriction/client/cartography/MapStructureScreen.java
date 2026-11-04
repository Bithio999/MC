package dev.heavenlyrestriction.client.cartography;

import dev.heavenlyrestriction.cartography.MapService;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/** Full registry browser with stable selection, live filtering and explicit name/mod sorting. */
public final class MapStructureScreen extends Screen {
  final MaraudersMapScreen parent;

  private record Entry(String id, String name, String mod, String namespace) {}

  private enum Sort {
    NAME,
    MOD
  }

  private static final int ROW_STEP = 24;
  private final List<Entry> all = new ArrayList<>(), filtered = new ArrayList<>();
  private final List<StructureRow> rows = new ArrayList<>();
  private EditBox search;
  private int left, top, w, h, offset, listTop;
  private String selected = "";
  private Sort sort = Sort.NAME;
  private Button locate;
  private boolean draggingScrollbar;

  public MapStructureScreen(MaraudersMapScreen parent, ListTag types) {
    super(Component.literal("Locate a structure"));
    this.parent = parent;
    for (var tag : types) {
      var t = (CompoundTag) tag;
      String id = t.getString("id"),
          namespace = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
      String mod =
          ModList.get()
              .getModContainerById(namespace)
              .map(c -> c.getModInfo().getDisplayName())
              .orElseGet(() -> MapService.name(namespace));
      all.add(new Entry(id, t.getString("name"), mod, namespace));
    }
  }

  @Override
  protected void init() {
    draggingScrollbar = false;
    String savedQuery = search == null ? "" : search.getValue();
    w = Math.min(480, width - 24);
    h = Math.min(400, height - 20);
    left = (width - w) / 2;
    top = (height - h) / 2;
    listTop = top + 84;
    search =
        new EditBox(
            font, left + 14, top + 34, w - 28, 18, Component.literal("Find a structure type"));
    search.setMaxLength(96);
    search.setHint(Component.literal("Search structures, mods or IDs…"));
    search.setValue(savedQuery);
    search.setResponder(
        q -> {
          offset = 0;
          filter();
        });
    addRenderableWidget(search);
    addRenderableWidget(new SortButton(left + 45, top + 58, "Name A–Z", Sort.NAME));
    addRenderableWidget(new SortButton(left + 145, top + 58, "Mod A–Z", Sort.MOD));
    rows.clear();
    int count = Math.max(1, (h - 64 - 84) / ROW_STEP);
    for (int n = 0; n < count; n++) rows.add(addRenderableWidget(new StructureRow(n)));
    locate =
        addRenderableWidget(
            MapUi.button(
                    Component.literal("Start search"),
                    b -> {
                      var t = new CompoundTag();
                      t.putString("type", selected);
                      parent.request("locate", t);
                      onClose();
                    })
                .bounds(left + 14, top + h - 32, (w - 34) / 2, 20)
                .build());
    addRenderableWidget(
        MapUi.button(Component.literal("Cancel"), b -> onClose())
            .bounds(left + 20 + (w - 34) / 2, top + h - 32, (w - 34) / 2, 20)
            .build());
    setInitialFocus(search);
    filter();
  }

  private final class SortButton extends MapUi.AtlasButton {
    private final Sort mode;

    SortButton(int x, int y, String label, Sort mode) {
      super(x, y, 94, 20, Component.literal(label), b -> {});
      this.mode = mode;
      setTooltip(
          Tooltip.create(
              Component.literal(
                  mode == Sort.NAME
                      ? "Sort all matching structures alphabetically"
                      : "Group by mod, then sort structure names alphabetically")));
    }

    @Override
    public void onPress() {
      sort = mode;
      offset = 0;
      filter();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      super.renderWidget(g, mx, my, partial);
      if (sort == mode)
        g.fill(
            getX() + 1,
            getY() + getHeight() - 2,
            getX() + getWidth() - 1,
            getY() + getHeight(),
            MaraudersMapScreen.GOLD);
    }
  }

  private String fit(String text, int max) {
    return font.width(text) <= max
        ? text
        : font.plainSubstrByWidth(text, Math.max(0, max - 8)) + "…";
  }

  private final class StructureRow extends MapUi.AtlasButton {
    private final int row;
    private Entry entry;

    StructureRow(int row) {
      super(left + 14, listTop + row * ROW_STEP, w - 34, 22, Component.empty(), b -> {});
      this.row = row;
    }

    @Override
    public void onPress() {
      if (entry != null) {
        selected = entry.id;
        update();
      }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      if (entry == null) return;
      int x = getX(), y = getY(), width = getWidth();
      boolean chosen = entry.id.equals(selected);
      g.fill(
          x,
          y,
          x + width,
          y + 22,
          chosen ? 0xff35494c : isHoveredOrFocused() ? 0xff2a4048 : 0xff20323a);
      if (chosen) g.fill(x, y, x + 2, y + 22, MaraudersMapScreen.GOLD);
      if (isFocused()) g.renderOutline(x, y, width, 22, MaraudersMapScreen.GOLD);
      g.drawString(
          font,
          fit(entry.name, width - 16),
          x + 8,
          y + 3,
          chosen ? MaraudersMapScreen.GOLD : MaraudersMapScreen.TEXT,
          false);
      g.drawString(
          font, fit(entry.mod, width - 16), x + 8, y + 12, MaraudersMapScreen.MUTED, false);
    }
  }

  private void filter() {
    filtered.clear();
    String q = search.getValue().strip().toLowerCase(Locale.ROOT);
    for (var e : all)
      if ((e.name + " " + e.id + " " + e.mod).toLowerCase(Locale.ROOT).contains(q)) filtered.add(e);
    Comparator<Entry> names =
        Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::id);
    filtered.sort(
        sort == Sort.NAME
            ? names
            : Comparator.comparing(Entry::mod, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Entry::namespace)
                .thenComparing(names));
    update();
  }

  private void update() {
    offset = Math.clamp(offset, 0, Math.max(0, filtered.size() - rows.size()));
    for (var row : rows) {
      int at = offset + row.row;
      row.entry = at < filtered.size() ? filtered.get(at) : null;
      row.visible = row.entry != null;
      if (row.entry != null) {
        row.setMessage(Component.literal(row.entry.name));
        row.setTooltip(
            Tooltip.create(
                Component.literal(row.entry.name + "\n" + row.entry.mod + "\n" + row.entry.id)));
      }
    }
    if (locate != null) locate.active = filtered.stream().anyMatch(e -> e.id.equals(selected));
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    MapUi.backdrop(g, width, height);
    MapUi.panel(g, left, top, w, h);
    g.drawString(font, "LOCATE STRUCTURES", left + 14, top + 14, MaraudersMapScreen.GOLD, false);
    String count =
        search.getValue().isBlank()
            ? all.size() + " structures"
            : filtered.size() + " of " + all.size();
    g.drawString(
        font, count, left + w - 14 - font.width(count), top + 14, MaraudersMapScreen.MUTED, false);
    g.drawString(font, "Sort", left + 14, top + 64, MaraudersMapScreen.MUTED, false);
    if (filtered.isEmpty())
      g.drawString(
          font, "No matching structures.", left + 22, listTop + 8, MaraudersMapScreen.MUTED, false);
    if (filtered.size() > rows.size()) {
      int track = rows.size() * ROW_STEP,
          thumb = Math.max(10, track * rows.size() / filtered.size()),
          y = listTop + (track - thumb) * offset / Math.max(1, filtered.size() - rows.size());
      g.fill(left + w - 16, listTop, left + w - 14, listTop + track, 0xff102028);
      g.fill(left + w - 16, y, left + w - 14, y + thumb, MaraudersMapScreen.TEAL);
    }
    String selection =
        all.stream()
            .filter(e -> e.id.equals(selected))
            .map(e -> "Selected: " + e.name)
            .findFirst()
            .orElse("Select a structure to locate it.");
    g.drawString(
        font, fit(selection, w - 28), left + 14, top + h - 59, MaraudersMapScreen.TEXT, false);
    g.drawString(
        font,
        fit("100,000-block radius · Viewed map center", w - 28),
        left + 14,
        top + h - 46,
        MaraudersMapScreen.MUTED,
        false);
    super.render(g, mx, my, partial);
  }

  private void dragScrollbar(double mouseY) {
    int track = rows.size() * ROW_STEP, thumb = Math.max(10, track * rows.size() / filtered.size());
    double fraction =
        Math.clamp((mouseY - listTop - thumb / 2d) / Math.max(1, track - thumb), 0, 1);
    offset = (int) Math.round(fraction * (filtered.size() - rows.size()));
    update();
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (button == 0
        && filtered.size() > rows.size()
        && mx >= left + w - 20
        && mx < left + w - 10
        && my >= listTop
        && my < listTop + rows.size() * ROW_STEP) {
      draggingScrollbar = true;
      search.setFocused(false);
      clearFocus();
      dragScrollbar(my);
      return true;
    }
    return super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    if (draggingScrollbar && button == 0) {
      dragScrollbar(my);
      return true;
    }
    return super.mouseDragged(mx, my, button, dx, dy);
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    if (draggingScrollbar && button == 0) {
      draggingScrollbar = false;
      return true;
    }
    return super.mouseReleased(mx, my, button);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    if (mx >= left && mx < left + w && my >= listTop && my < listTop + rows.size() * ROW_STEP) {
      offset -= (int) Math.signum(dy) * 2;
      update();
      return true;
    }
    return super.mouseScrolled(mx, my, dx, dy);
  }

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
