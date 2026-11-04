package dev.heavenlyrestriction.client.cartography;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Flat atlas controls. Dialogs own an opaque background, so parent labels cannot bleed through. */
public final class MapUi {
  public static final int BACK = 0xff10191e, PANEL = 0xff19272e, EDGE = 0xff3c555c;

  public static Builder button(Component label, Button.OnPress action) {
    return new Builder(label, action);
  }

  public static final class Builder {
    private final Component label;
    private final Button.OnPress action;
    private int x, y, w, h = 20;

    private Builder(Component label, Button.OnPress action) {
      this.label = label;
      this.action = action;
    }

    public Builder bounds(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      return this;
    }

    public Button build() {
      return new AtlasButton(x, y, w, h, label, action);
    }
  }

  public static class AtlasButton extends Button {
    public AtlasButton(int x, int y, int w, int h, Component text, OnPress action) {
      super(x, y, w, h, text, action, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      boolean hover = isHoveredOrFocused() && active;
      int x = getX(), y = getY(), w = getWidth(), h = getHeight();
      g.fill(x + 1, y, x + w - 1, y + h, hover ? 0xff30454c : active ? 0xff22343c : 0xff1b282e);
      g.fill(x, y + 1, x + w, y + h - 1, hover ? 0xff30454c : active ? 0xff22343c : 0xff1b282e);
      g.fill(x + 1, y + h - 1, x + w - 1, y + h, hover ? MaraudersMapScreen.GOLD : EDGE);
      if (isFocused()) g.renderOutline(x, y, w, h, MaraudersMapScreen.GOLD);
      var font = Minecraft.getInstance().font;
      String label = getMessage().getString();
      if (font.width(label) > w - 10)
        label = font.plainSubstrByWidth(label, Math.max(0, w - 16)) + "…";
      g.drawString(
          font,
          label,
          x + (w - font.width(label)) / 2,
          y + (h - 8) / 2,
          active ? hover ? 0xfff7dfac : MaraudersMapScreen.TEXT : 0xff72838a,
          false);
    }
  }

  public static void backdrop(GuiGraphics g, int width, int height) {
    g.fill(0, 0, width, height, BACK);
    for (int x = 0; x < width; x += 32) g.fill(x, 0, x + 1, height, 0xff142128);
    for (int y = 0; y < height; y += 32) g.fill(0, y, width, y + 1, 0xff142128);
  }

  public static void panel(GuiGraphics g, int x, int y, int w, int h) {
    g.fill(x - 3, y + 4, x + w + 3, y + h + 5, 0xff0c1317);
    g.fill(x, y, x + w, y + h, PANEL);
    g.renderOutline(x, y, w, h, EDGE);
    g.fill(x + 1, y + 1, x + w - 1, y + 2, MaraudersMapScreen.GOLD);
  }

  private MapUi() {}
}
