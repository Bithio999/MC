package dev.heavenlyrestriction.client.cartography;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MapHelpScreen extends Screen {
  final MaraudersMapScreen parent;
  private int x, y, w, h;

  MapHelpScreen(MaraudersMapScreen parent) {
    super(Component.literal("Map guide"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    w = Math.min(390, width - 24);
    h = 218;
    x = (width - w) / 2;
    y = (height - h) / 2;
    addRenderableWidget(
        Button.builder(Component.literal("Back to map"), b -> onClose())
            .bounds(x + 14, y + h - 33, w - 28, 20)
            .build());
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    parent.render(g, -100, -100, partial);
    g.fill(0, 0, width, height, 0xc010171d);
    g.fill(x, y, x + w, y + h, 0xff202d34);
    g.renderOutline(x, y, w, h, 0xff65726b);
    g.drawString(font, "MAP GUIDE", x + 14, y + 14, MaraudersMapScreen.GOLD, false);
    g.drawWordWrap(
        font,
        Component.literal(
            "Drag to pan · Scroll to zoom at the cursor\n"
                + "Right-click to add a pin · Home to recenter\n"
                + "F to search markers · N for a new pin\n"
                + "Go to… moves the map to coordinates."),
        x + 14,
        y + 37,
        w - 28,
        MaraudersMapScreen.TEXT);
    g.drawString(font, "STRUCTURE COLORS", x + 14, y + 89, MaraudersMapScreen.MUTED, false);
    String[] labels = {
      "Village",
      "Temple / pyramid",
      "Fortress / outpost",
      "Ocean structures",
      "Underground",
      "Other / addon"
    };
    int[] colors = {0xffe6c78a, 0xffedb175, 0xffeb8c83, 0xff8dccea, 0xffc2a2e5, 0xff79cfc2};
    for (int n = 0; n < 6; n++) {
      int xx = x + 14 + (n % 2) * (w - 28) / 2, yy = y + 106 + (n / 2) * 15;
      g.fill(xx, yy, xx + 5, yy + 5, colors[n]);
      g.drawString(font, labels[n], xx + 10, yy - 1, MaraudersMapScreen.TEXT, false);
    }
    g.drawWordWrap(
        font,
        Component.literal(
            "Pins are private. Dark areas have not been recorded. Locate can find structures"
                + " without revealing terrain."),
        x + 14,
        y + 155,
        w - 28,
        MaraudersMapScreen.MUTED);
    super.render(g, mx, my, partial);
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
