package dev.heavenlyrestriction.client.cartography;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** A single modal input layer for contextual actions, layers and searchable dimension choices. */
final class MapPopup {
  record Action(String label, boolean enabled, Runnable run) {
    Action(String label, Runnable run) {
      this(label, true, run);
    }
  }

  final String title;
  private final List<Action> all;
  private final boolean searchable;
  private final int x, y, w, maxRows;
  private int offset, selected = -1;
  private String query = "";

  MapPopup(
      String title,
      List<Action> actions,
      int mx,
      int my,
      int screenW,
      int screenH,
      boolean searchable) {
    this.title = title;
    all = List.copyOf(actions);
    this.searchable = searchable;
    w = Math.min(searchable ? 204 : 160, screenW - 16);
    maxRows = Math.max(1, Math.min(actions.size(), (screenH - 52 - (searchable ? 22 : 0)) / 21));
    int h = header() + maxRows * 21 + 6;
    x = Math.clamp(mx, 8, screenW - w - 8);
    y = Math.clamp(my, 8, screenH - h - 8);
    Minecraft.getInstance()
        .getNarrator()
        .sayNow(
            net.minecraft.network.chat.Component.literal(
                title
                    + ". "
                    + (searchable ? "Type to filter; " : "")
                    + "Use arrow keys and Enter. Escape closes."));
  }

  private int header() {
    return searchable ? 46 : 24;
  }

  private List<Action> items() {
    return all.stream()
        .filter(a -> a.label.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
        .toList();
  }

  private int rows() {
    return Math.max(1, Math.min(maxRows, items().size()));
  }

  private int height() {
    return header() + rows() * 21 + 6;
  }

  void render(GuiGraphics g, int mx, int my) {
    var font = Minecraft.getInstance().font;
    g.flush();
    g.pose().pushPose();
    g.pose().translate(0, 0, 450);
    MapUi.panel(g, x, y, w, height());
    g.drawString(
        font,
        font.plainSubstrByWidth(title, w - 20),
        x + 10,
        y + 9,
        MaraudersMapScreen.GOLD,
        false);
    if (searchable) {
      g.fill(x + 7, y + 25, x + w - 7, y + 42, 0xff101d24);
      g.drawString(
          font,
          font.plainSubstrByWidth(query.isEmpty() ? "Type to filter…" : query + "_", w - 24),
          x + 12,
          y + 30,
          query.isEmpty() ? MaraudersMapScreen.MUTED : MaraudersMapScreen.TEXT,
          false);
    }
    var list = items();
    g.enableScissor(x + 3, y + header(), x + w - 3, y + height() - 3);
    for (int i = 0; i < rows(); i++) {
      int at = offset + i, yy = y + header() + i * 21;
      if (at >= list.size()) {
        g.drawString(font, "No dimensions found", x + 11, yy + 6, MaraudersMapScreen.MUTED, false);
        break;
      }
      var action = list.get(at);
      boolean hover = mx >= x + 4 && mx < x + w - 4 && my >= yy && my < yy + 21;
      if (hover || selected == at) {
        g.fill(x + 4, yy, x + w - 4, yy + 20, 0xff30464d);
        g.fill(x + 4, yy, x + 6, yy + 20, MaraudersMapScreen.TEAL);
      }
      String text = action.label;
      if (font.width(text) > w - 24) text = font.plainSubstrByWidth(text, w - 32) + "…";
      g.drawString(
          font, text, x + 12, yy + 6, action.enabled ? MaraudersMapScreen.TEXT : 0xff71838a, false);
    }
    g.disableScissor();
    if (list.size() > rows()) {
      int h = rows() * 21,
          thumb = Math.max(8, h * rows() / list.size()),
          yy = y + header() + (h - thumb) * offset / Math.max(1, list.size() - rows());
      g.fill(x + w - 4, yy, x + w - 2, yy + thumb, MaraudersMapScreen.TEAL);
    }
    g.flush();
    g.pose().popPose();
  }

  /** True means dismiss; every click is consumed to prevent actions behind a dropdown. */
  boolean click(double mx, double my, int button) {
    if (button != 0 || mx < x || mx >= x + w || my < y || my >= y + height()) return true;
    if (my < y + header()) return false;
    int at = offset + (int) (my - y - header()) / 21;
    var list = items();
    if (at < list.size() && list.get(at).enabled) {
      list.get(at).run.run();
      return true;
    }
    return false;
  }

  void scroll(double dy) {
    offset = Math.clamp(offset - (int) Math.signum(dy), 0, Math.max(0, items().size() - rows()));
    selected = -1;
  }

  boolean key(int key) {
    if (key == GLFW.GLFW_KEY_ESCAPE) return true;
    var list = items();
    if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
      selected =
          Math.clamp(
              selected + (key == GLFW.GLFW_KEY_UP ? -1 : 1), 0, Math.max(0, list.size() - 1));
      offset =
          Math.clamp(
              selected < offset
                  ? selected
                  : selected >= offset + rows() ? selected - rows() + 1 : offset,
              0,
              Math.max(0, list.size() - rows()));
    }
    if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
      int at = selected >= 0 ? selected : offset;
      if (at < list.size() && list.get(at).enabled) {
        list.get(at).run.run();
        return true;
      }
    }
    if (searchable && key == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
      query = query.substring(0, query.length() - 1);
      offset = 0;
      selected = -1;
    }
    return false;
  }

  void type(char c) {
    if (searchable && !Character.isISOControl(c) && query.length() < 96) {
      query += c;
      offset = 0;
      selected = -1;
    }
  }
}
