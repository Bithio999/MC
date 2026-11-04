package dev.heavenlyrestriction.client.cartography;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;

public final class MapStructureScreen extends Screen {
  final MaraudersMapScreen parent;
  private final List<CompoundTag> all = new ArrayList<>(), filtered = new ArrayList<>();
  private EditBox search;
  private int left, top, w, h, offset;
  private String selected = "";
  private final List<Button> rows = new ArrayList<>();
  private Button locate;

  public MapStructureScreen(MaraudersMapScreen parent, ListTag types) {
    super(Component.literal("Locate a structure"));
    this.parent = parent;
    for (var t : types) all.add((CompoundTag) t);
  }

  @Override
  protected void init() {
    w = Math.min(420, width - 24);
    h = Math.min(340, height - 20);
    left = (width - w) / 2;
    top = (height - h) / 2;
    search =
        new EditBox(
            font, left + 14, top + 38, w - 28, 20, Component.literal("Find a structure type"));
    search.setMaxLength(96);
    search.setHint(Component.literal("Search name or mod, e.g. village or tensura"));
    search.setResponder(
        q -> {
          offset = 0;
          filter();
        });
    addRenderableWidget(search);
    rows.clear();
    int count = Math.max(1, (h - 150) / 28);
    for (int n = 0; n < count; n++) {
      int index = n;
      rows.add(
          addRenderableWidget(
              Button.builder(
                      Component.empty(),
                      b -> {
                        int at = offset + index;
                        if (at < filtered.size()) {
                          selected = filtered.get(at).getString("id");
                          update();
                        }
                      })
                  .bounds(left + 14, top + 66 + n * 28, w - 28, 25)
                  .build()));
    }
    locate =
        addRenderableWidget(
            Button.builder(
                    Component.literal("Start search"),
                    b -> {
                      var t = new CompoundTag();
                      t.putString("type", selected);
                      parent.request("locate", t);
                      onClose();
                    })
                .bounds(left + 14, top + h - 37, (w - 34) / 2, 20)
                .build());
    addRenderableWidget(
        Button.builder(Component.literal("Cancel"), b -> onClose())
            .bounds(left + 20 + (w - 34) / 2, top + h - 37, (w - 34) / 2, 20)
            .build());
    setInitialFocus(search);
    filter();
  }

  private void filter() {
    filtered.clear();
    String q = search.getValue().toLowerCase(Locale.ROOT);
    for (var t : all)
      if ((t.getString("name") + " " + t.getString("id")).toLowerCase(Locale.ROOT).contains(q))
        filtered.add(t);
    update();
  }

  private void update() {
    for (int n = 0; n < rows.size(); n++) {
      int i = offset + n;
      var b = rows.get(n);
      b.visible = i < filtered.size();
      if (b.visible) {
        var t = filtered.get(i);
        String label =
            t.getString("id").equals(selected) ? "> " + t.getString("name") : t.getString("name");
        b.setMessage(Component.literal(font.plainSubstrByWidth(label, w - 52)));
        b.setTooltip(Tooltip.create(Component.literal(t.getString("id"))));
      }
    }
    if (locate != null)
      locate.active =
          !selected.isEmpty()
              && filtered.stream().anyMatch(t -> t.getString("id").equals(selected));
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    parent.render(g, -100, -100, partial);
    g.fill(0, 0, width, height, 0xc010171d);
    g.fill(left, top, left + w, top + h, 0xff202d34);
    g.renderOutline(left, top, w, h, 0xff65726b);
    g.drawString(font, "LOCATE A STRUCTURE", left + 14, top + 14, MaraudersMapScreen.GOLD, false);
    g.drawWordWrap(
        font,
        Component.literal(
            "Searches up to 100,000 blocks around the map center. Addon structures are included."
                + " Esc cancels."),
        left + 14,
        top + h - 80,
        w - 28,
        MaraudersMapScreen.MUTED);
    if (filtered.isEmpty())
      g.drawString(
          font,
          "No matching structure types",
          left + 14,
          top + 73,
          MaraudersMapScreen.MUTED,
          false);
    super.render(g, mx, my, partial);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    offset =
        Math.clamp(
            offset - (int) Math.signum(dy) * 2, 0, Math.max(0, filtered.size() - rows.size()));
    update();
    return true;
  }

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    // This opaque atlas is already the background; never blur its terrain or labels.
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
