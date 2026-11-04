package dev.heavenlyrestriction.client.cartography;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Compact XYZ editor with an explicit confirmation for deletion. Fits Minecraft's 240px GUI. */
public final class MapPinScreen extends Screen {
  final MaraudersMapScreen parent;
  private final CompoundTag pin;
  private final boolean jump, removing;
  private EditBox name, x, y, z;
  private int left, top, w, h, color, icon;
  private String error = "";

  public MapPinScreen(MaraudersMapScreen parent, CompoundTag pin) {
    this(parent, pin, false, false);
  }

  private MapPinScreen(MaraudersMapScreen parent, CompoundTag pin, boolean jump, boolean removing) {
    super(
        Component.literal(
            jump
                ? "Go to coordinates"
                : removing ? "Delete pin?" : pin.hasUUID("uuid") ? "Edit pin" : "New pin"));
    this.parent = parent;
    this.pin = pin.copy();
    this.jump = jump;
    this.removing = removing;
    color = Math.floorMod(pin.getInt("color"), 8);
    icon = Math.floorMod(pin.getInt("icon"), 6);
  }

  static MapPinScreen gotoPoint(MaraudersMapScreen parent) {
    var t = new CompoundTag();
    t.putInt("x", (int) parent.view.x);
    t.putInt("z", (int) parent.view.z);
    return new MapPinScreen(parent, t, true, false);
  }

  static MapPinScreen delete(MaraudersMapScreen parent, CompoundTag pin) {
    return new MapPinScreen(parent, pin, false, true);
  }

  private EditBox field(int xx, int yy, int width, String label, String value, int max) {
    var f = new EditBox(font, xx, yy, width, 20, Component.literal(label));
    f.setMaxLength(max);
    f.setValue(value);
    f.setResponder(text -> error = "");
    addRenderableWidget(f);
    return f;
  }

  private Button button(int xx, int yy, int ww, String text, Runnable action) {
    return addRenderableWidget(
        Button.builder(Component.literal(text), b -> action.run()).bounds(xx, yy, ww, 20).build());
  }

  private void remember() {
    if (x == null || z == null) return;
    pin.putString("fieldX", x.getValue());
    pin.putString("fieldZ", z.getValue());
    if (name != null) pin.putString("name", name.getValue());
    if (y != null) pin.putString("fieldY", y.getValue());
  }

  @Override
  protected void init() {
    w = Math.min(350, width - 24);
    h = removing ? 146 : jump ? 156 : 218;
    left = (width - w) / 2;
    top = (height - h) / 2;
    if (removing) {
      button(
          left + 16,
          top + h - 37,
          (w - 38) / 2,
          "Delete pin",
          () -> {
            parent.request("delete", pin);
            onClose();
          });
      button(left + 22 + (w - 38) / 2, top + h - 37, (w - 38) / 2, "Keep pin", this::onClose);
      return;
    }
    if (!jump) name = field(left + 16, top + 43, w - 32, "Pin name", pin.getString("name"), 64);
    int count = jump ? 2 : 3,
        fw = (w - 32 - (count - 1) * 8) / count,
        yy = jump ? top + 53 : top + 89;
    x =
        field(
            left + 16,
            yy,
            fw,
            "X",
            pin.contains("fieldX") ? pin.getString("fieldX") : String.valueOf(pin.getInt("x")),
            10);
    if (jump)
      z =
          field(
              left + 24 + fw,
              yy,
              fw,
              "Z",
              pin.contains("fieldZ") ? pin.getString("fieldZ") : String.valueOf(pin.getInt("z")),
              10);
    else {
      y =
          field(
              left + 24 + fw,
              yy,
              fw,
              "Y",
              pin.contains("fieldY") ? pin.getString("fieldY") : String.valueOf(pin.getInt("y")),
              6);
      z =
          field(
              left + 32 + 2 * fw,
              yy,
              fw,
              "Z",
              pin.contains("fieldZ") ? pin.getString("fieldZ") : String.valueOf(pin.getInt("z")),
              10);
      button(
          left + 16,
          top + 144,
          (w - 38) / 2,
          "Color: " + (color + 1),
          () -> {
            remember();
            color = (color + 1) % 8;
            rebuildWidgets();
          });
      button(
          left + 22 + (w - 38) / 2,
          top + 144,
          (w - 38) / 2,
          "Icon: " + MaraudersMapScreen.ICONS[icon],
          () -> {
            remember();
            icon = (icon + 1) % 6;
            rebuildWidgets();
          });
    }
    button(left + 16, top + h - 35, (w - 38) / 2, jump ? "Center map" : "Save pin", this::commit);
    button(left + 22 + (w - 38) / 2, top + h - 35, (w - 38) / 2, "Cancel", this::onClose);
    setInitialFocus(jump ? x : name);
  }

  private void commit() {
    try {
      int xx = Integer.parseInt(x.getValue().strip()), zz = Integer.parseInt(z.getValue().strip());
      if (Math.abs((long) xx) > 29999984 || Math.abs((long) zz) > 29999984)
        throw new IllegalArgumentException("Coordinates exceed the world border.");
      if (jump) {
        parent.center(xx, zz);
        onClose();
        return;
      }
      int yy = Integer.parseInt(y.getValue().strip());
      if (name.getValue().isBlank()) throw new IllegalArgumentException("Give this pin a name.");
      if (yy < minecraft.level.getMinBuildHeight() || yy >= minecraft.level.getMaxBuildHeight())
        throw new IllegalArgumentException("Y must be inside this dimension's build range.");
      var data = pin.copy();
      data.putString("name", name.getValue().strip());
      data.putInt("x", xx);
      data.putInt("y", yy);
      data.putInt("z", zz);
      data.putInt("color", color);
      data.putInt("icon", icon);
      parent.request("pin", data);
      onClose();
    } catch (NumberFormatException e) {
      error = "Enter whole-number coordinates.";
    } catch (IllegalArgumentException e) {
      error = e.getMessage();
    }
  }

  @Override
  public void resize(Minecraft mc, int width, int height) {
    remember();
    super.resize(mc, width, height);
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    parent.render(g, -100, -100, partial);
    g.fill(0, 0, width, height, 0xc010171d);
    g.fill(left, top, left + w, top + h, 0xff202d34);
    g.renderOutline(left, top, w, h, 0xff65726b);
    g.drawString(font, title, left + 16, top + 14, MaraudersMapScreen.GOLD, false);
    if (removing)
      g.drawWordWrap(
          font,
          Component.literal(
              "Delete “" + pin.getString("name") + "”? This removes only your saved pin."),
          left + 16,
          top + 45,
          w - 32,
          MaraudersMapScreen.TEXT);
    else {
      if (!jump) {
        g.drawString(font, "NAME", left + 16, top + 32, MaraudersMapScreen.MUTED, false);
        g.fill(left + w - 28, top + 13, left + w - 16, top + 25, MaraudersMapScreen.COLORS[color]);
      }
      int yy = jump ? top + 42 : top + 78, fw = (w - 32 - (jump ? 8 : 16)) / (jump ? 2 : 3);
      g.drawString(font, "X", left + 16, yy, MaraudersMapScreen.MUTED, false);
      g.drawString(font, jump ? "Z" : "Y", left + 24 + fw, yy, MaraudersMapScreen.MUTED, false);
      if (!jump) g.drawString(font, "Z", left + 32 + 2 * fw, yy, MaraudersMapScreen.MUTED, false);
      g.drawString(
          font,
          jump ? "Center the map at these coordinates." : "X east · Y height · Z south",
          left + 16,
          yy + 40,
          MaraudersMapScreen.MUTED,
          false);
      if (!error.isEmpty())
        g.drawString(
            font,
            font.plainSubstrByWidth(error, w - 32),
            left + 16,
            top + h - 49,
            0xffffa296,
            false);
    }
    super.render(g, mx, my, partial);
  }

  @Override
  public boolean keyPressed(int key, int scan, int mods) {
    if (key == 257 && !removing) {
      commit();
      return true;
    }
    return super.keyPressed(key, scan, mods);
  }

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
    parent.refresh();
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
