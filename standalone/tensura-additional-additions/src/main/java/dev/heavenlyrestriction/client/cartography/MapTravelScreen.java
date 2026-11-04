package dev.heavenlyrestriction.client.cartography;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;

/** Compact spatial-skill chooser, using the same atlas dialog layout as structure discovery. */
public final class MapTravelScreen extends Screen {
  final MaraudersMapScreen parent;
  private final CompoundTag target;
  private final List<CompoundTag> skills = new ArrayList<>();
  private int x, y, w, h, offset, count;

  public MapTravelScreen(MaraudersMapScreen parent, CompoundTag target, ListTag options) {
    super(Component.literal("Choose a teleport skill"));
    this.parent = parent;
    this.target = target.copy();
    for (var option : options) skills.add((CompoundTag) option);
  }

  @Override
  protected void init() {
    w = Math.min(370, width - 24);
    h = Math.min(290, height - 24);
    x = (width - w) / 2;
    y = (height - h) / 2;
    count = Math.max(1, (h - 115) / 27);
    for (int n = 0; n < count && offset + n < skills.size(); n++) {
      var skill = skills.get(offset + n);
      addRenderableWidget(
          MapUi.button(
                  Component.literal(font.plainSubstrByWidth(skill.getString("name"), w - 44)),
                  b -> {
                    var t = new CompoundTag();
                    t.putString("id", target.getString("id"));
                    t.putString("skill", skill.getString("id"));
                    parent.request("travel", t);
                    onClose();
                  })
              .bounds(x + 14, y + 72 + n * 27, w - 28, 23)
              .build());
    }
    addRenderableWidget(
        MapUi.button(Component.literal("Back to map"), b -> onClose())
            .bounds(x + 14, y + h - 33, w - 28, 20)
            .build());
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    MapUi.backdrop(g, width, height);
    MapUi.panel(g, x, y, w, h);
    g.drawString(
        font,
        "TRAVEL TO " + font.plainSubstrByWidth(target.getString("name"), w - 88),
        x + 14,
        y + 14,
        MaraudersMapScreen.GOLD,
        false);
    g.drawWordWrap(
        font,
        Component.literal(
            "Choose a learned skill. Confirm the prefilled coordinates in Tensura's teleport"
                + " menu."),
        x + 14,
        y + 34,
        w - 28,
        MaraudersMapScreen.TEXT);
    super.render(g, mx, my, partial);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    int next = Math.clamp(offset - (int) Math.signum(dy), 0, Math.max(0, skills.size() - count));
    if (next != offset) {
      offset = next;
      rebuildWidgets();
    }
    return true;
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }
}
