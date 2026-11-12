package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.mentor.MentorNetwork;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** A quiet library palette with native Tensura controls, bounded lists and clipped descriptions. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class MentorScreen extends Screen {
  private static final int TEXT = 0xffe5f0f2,
      MUTED = 0xff95a9b8,
      GOLD = 0xffe4ca8c,
      EDGE = 0xff385367;
  private CompoundTag state;
  private String selected = "", query = "";
  private int x, y, w, h, left, right, bodyTop, bodyBottom, listScroll, detailScroll, delay = -1;
  private boolean confirmation, pending;
  private EditBox search;

  public MentorScreen(CompoundTag t) {
    super(Component.translatable("heavenlyrestriction.mentor.title"));
    state = t.copy();
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(() -> MentorNetwork.receiver = MentorScreen::receive);
  }

  public static void receive(CompoundTag t) {
    var mc = Minecraft.getInstance();
    if (t.getBoolean("open") && t.getBoolean("allowed")) {
      mc.setScreen(new MentorScreen(t));
      return;
    }
    if (mc.screen instanceof MentorScreen s
        && t.hasUUID("session")
        && t.getUUID("session").equals(s.state.getUUID("session"))) {
      if (!t.getBoolean("allowed")) {
        mc.setScreen(null);
        return;
      }
      boolean typing = s.search != null && s.search.isFocused();
      int cursor = typing ? s.search.getCursorPosition() : 0;
      s.state = t.copy();
      s.pending = false;
      s.confirmation = false;
      s.rebuildWidgets();
      if (typing && s.search != null) {
        s.setFocused(s.search);
        s.search.setFocused(true);
        s.search.setCursorPosition(Math.min(cursor, s.search.getValue().length()));
      }
    }
  }

  private ListTag rows() {
    return state.getList("rows", Tag.TAG_COMPOUND);
  }

  private CompoundTag chosen() {
    for (var raw : rows()) {
      var r = (CompoundTag) raw;
      if (r.getString("id").equals(selected)) return r;
    }
    return new CompoundTag();
  }

  private boolean canBuy() {
    return !pending
        && !state.getBoolean("used")
        && state.getBoolean("affordable")
        && chosen().getBoolean("learnable");
  }

  private String fit(String text, int width) {
    return font.width(text) <= width
        ? text
        : font.plainSubstrByWidth(text, Math.max(0, width - 7)) + "…";
  }

  private void send(String action, int page) {
    if (!state.hasUUID("session") || pending) return;
    pending = true;
    PacketDistributor.sendToServer(
        new MentorNetwork.Request(state.getUUID("session"), action, selected, query, page));
  }

  private SimpleButton button(
      int bx, int by, int bw, String label, Runnable action, boolean enabled) {
    var b =
        new MentorButton(
            bx, by, bw, 18, Component.literal(label), Component.literal(label), v -> action.run());
    b.active = enabled;
    return addRenderableWidget(b);
  }

  @Override
  protected void init() {
    w = Math.min(480, width - 16);
    h = Math.min(300, height - 16);
    x = (width - w) / 2;
    y = (height - h) / 2;
    left = Math.max(122, (w - 34) * 43 / 100);
    right = x + left + 22;
    bodyTop = y + 59;
    bodyBottom = y + h - 79;
    button(x + w - 29, y + 6, 21, "×", this::onClose, true);
    if (confirmation) {
      int pw = Math.min(330, w - 16), px = x + (w - pw) / 2, py = y + (h - 112) / 2;
      button(
          px + 10,
          py + 84,
          (pw - 28) / 2,
          "Cancel",
          () -> {
            confirmation = false;
            rebuildWidgets();
          },
          true);
      button(
          px + 18 + (pw - 28) / 2,
          py + 84,
          (pw - 28) / 2,
          "Accept lesson",
          () -> {
            confirmation = false;
            send("buy", state.getInt("page"));
            rebuildWidgets();
          },
          canBuy());
      return;
    }
    search = new EditBox(font, x + 10, y + 34, left, 18, Component.literal("Search unique skills"));
    search.setMaxLength(64);
    search.setValue(query);
    search.setHint(Component.literal("Search skills or mod…"));
    search.setResponder(
        s -> {
          query = s;
          delay = 6;
          listScroll = 0;
          detailScroll = 0;
        });
    addRenderableWidget(search);
    int visible = Math.max(1, (bodyBottom - bodyTop) / 23);
    listScroll = Math.clamp(listScroll, 0, Math.max(0, rows().size() - visible));
    for (int n = 0; n < visible && n + listScroll < rows().size(); n++) {
      var row = rows().getCompound(n + listScroll);
      var b = new SkillRow(x + 10, bodyTop + n * 23, left, row);
      addRenderableWidget(b);
    }
    button(
        x + 10,
        y + h - 72,
        26,
        "‹",
        () -> {
          listScroll = 0;
          send("list", state.getInt("page") - 1);
        },
        !pending && state.getInt("page") > 0);
    button(
        x + left - 16,
        y + h - 72,
        26,
        "›",
        () -> {
          listScroll = 0;
          send("list", state.getInt("page") + 1);
        },
        !pending && state.getInt("page") + 1 < state.getInt("pages"));
    button(
        x + w - 127,
        y + h - 25,
        117,
        state.getBoolean("used") ? "Lesson completed" : "Learn this skill",
        () -> {
          confirmation = true;
          rebuildWidgets();
        },
        canBuy());
  }

  @Override
  public void tick() {
    if (delay > 0 && --delay == 0) {
      if (pending) delay = 2;
      else {
        delay = -1;
        send("list", 0);
      }
    }
  }

  private void panel(GuiGraphics g, int px, int py, int pw, int ph, int edge) {
    g.fill(px, py, px + pw, py + ph, edge);
    g.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xff101f2d);
  }

  private void lines(GuiGraphics g, String text, int px, int py, int width, int bottom, int color) {
    for (var line : font.split(Component.literal(text), width)) {
      if (py + 9 > bottom) break;
      g.drawString(font, line, px, py, color, false);
      py += 11;
    }
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    g.fill(0, 0, width, height, 0x99040b13);
    panel(g, x, y, w, h, EDGE);
    g.fillGradient(x + 1, y + 1, x + w - 1, y + 29, 0xff28394c, 0xff152535);
    g.fill(x + 9, y + 9, x + 12, y + 22, GOLD);
    g.drawString(font, "ARCHIVE OF ACCORD", x + 19, y + 8, GOLD, false);
    g.drawString(font, "Elias · One extraordinary lesson", x + 19, y + 20, MUTED, false);
    g.fill(x + left + 16, y + 34, x + left + 17, y + h - 52, EDGE);
    if (!confirmation) {
      if (rows().isEmpty())
        g.drawString(font, "No matching skills", x + 12, bodyTop + 8, MUTED, false);
      g.drawCenteredString(
          font,
          (state.getInt("page") + 1) + " / " + Math.max(1, state.getInt("pages")),
          x + 10 + left / 2,
          y + h - 66,
          MUTED);
      var row = chosen();
      int rw = x + w - 10 - right;
      if (row.isEmpty()) {
        g.drawString(font, fit("A choice that stays with you", rw), right, y + 37, GOLD, false);
        lines(
            g,
            "Choose a Unique Skill from the catalogue. Elias teaches it permanently, starting"
                + " unmastered.",
            right,
            bodyTop,
            rw,
            bodyBottom,
            TEXT);
      } else {
        g.drawString(font, fit(row.getString("name"), rw), right, y + 35, GOLD, false);
        String id = row.getString("id");
        g.drawString(
            font,
            fit(id.substring(0, Math.max(0, id.indexOf(':'))) + " · Unique", rw),
            right,
            y + 47,
            MUTED,
            false);
        var all = font.split(Component.literal(row.getString("description")), Math.max(20, rw - 5));
        int shown = Math.max(1, (bodyBottom - bodyTop) / 11);
        detailScroll = Math.clamp(detailScroll, 0, Math.max(0, all.size() - shown));
        g.enableScissor(right, bodyTop, right + rw, bodyBottom);
        for (int n = 0; n < shown && n + detailScroll < all.size(); n++)
          g.drawString(font, all.get(n + detailScroll), right, bodyTop + n * 11, TEXT, false);
        g.disableScissor();
        if (all.size() > shown)
          g.drawString(font, "Scroll for details", right, bodyBottom + 3, MUTED, false);
      }
      g.fill(x + 9, y + h - 49, x + w - 9, y + h - 48, EDGE);
      g.drawString(
          font,
          fit(
              String.format(
                  Locale.ROOT, "%,.0f permanent EP + 1 Chromatic Orb", state.getDouble("cost")),
              w - 20),
          x + 10,
          y + h - 40,
          GOLD,
          false);
      String notice = state.getString("notice");
      String status =
          !notice.isEmpty()
              ? Component.translatable("heavenlyrestriction.mentor." + notice).getString()
              : state.getBoolean("used")
                  ? "Your one lesson has been used."
                  : !state.getBoolean("orb")
                      ? "Bring a Chromatic Orb."
                      : !state.getBoolean("affordable")
                          ? "Insufficient permanent EP."
                          : row.getBoolean("owned")
                              ? "You already know this skill."
                              : !row.isEmpty() && !row.getBoolean("learnable")
                                  ? "This skill is unavailable."
                                  : "One lesson per player.";
      g.drawString(font, fit(status, w - 153), x + 10, y + h - 20, MUTED, false);
    } else {
      g.fill(x + 1, y + 30, x + w - 1, y + h - 1, 0xcc08121e);
      int pw = Math.min(330, w - 16), px = x + (w - pw) / 2, py = y + (h - 112) / 2;
      panel(g, px, py, pw, 112, GOLD);
      g.drawString(font, "Accept your only lesson?", px + 10, py + 10, GOLD, false);
      g.drawString(font, fit(chosen().getString("name"), pw - 20), px + 10, py + 27, TEXT, false);
      lines(
          g,
          String.format(
              Locale.ROOT,
              "Spend %,.0f permanent EP and one Chromatic Orb. This choice cannot be changed.",
              state.getDouble("cost")),
          px + 10,
          py + 45,
          pw - 20,
          py + 79,
          MUTED);
    }
    super.render(g, mx, my, partial);
  }

  private final class MentorButton extends SimpleButton {
    MentorButton(
        int x,
        int y,
        int w,
        int h,
        Component name,
        Component hint,
        net.minecraft.client.gui.components.Button.OnPress action) {
      super(x, y, w, h, name, hint, action);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      panel(
          g, getX(), getY(), getWidth(), getHeight(), active && isHoveredOrFocused() ? GOLD : EDGE);
      g.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + 3, 0xff2d4658);
      g.drawCenteredString(
          font,
          fit(getMessage().getString(), getWidth() - 8),
          getX() + getWidth() / 2,
          getY() + 5,
          active ? TEXT : MUTED);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
      if (active
          && visible
          && button == 0
          && mx >= getX()
          && mx < getX() + getWidth()
          && my >= getY()
          && my < getY() + getHeight()) {
        playDownSound(Minecraft.getInstance().getSoundManager());
        onPress();
        return true;
      }
      return false;
    }
  }

  private final class SkillRow extends SimpleButton {
    private final CompoundTag row;

    SkillRow(int bx, int by, int bw, CompoundTag row) {
      super(
          bx,
          by,
          bw,
          21,
          Component.literal(row.getString("name")),
          Component.literal(row.getString("name")),
          b -> {});
      this.row = row;
      setTooltip(
          Tooltip.create(
              Component.literal(
                  row.getString("id") + (row.getBoolean("owned") ? " · Already learned" : ""))));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
      if (active
          && visible
          && button == 0
          && mx >= getX()
          && mx < getX() + getWidth()
          && my >= getY()
          && my < getY() + getHeight()) {
        playDownSound(Minecraft.getInstance().getSoundManager());
        onPress();
        return true;
      }
      return false;
    }

    @Override
    public void onPress() {
      selected = row.getString("id");
      detailScroll = 0;
      rebuildWidgets();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      boolean chosen = row.getString("id").equals(selected);
      panel(
          g,
          getX(),
          getY(),
          width,
          height,
          chosen ? GOLD : isHoveredOrFocused() ? 0xff79b7c5 : EDGE);
      var texture = ResourceLocation.tryParse(row.getString("icon"));
      if (texture != null) g.blit(texture, getX() + 3, getY() + 3, 0, 0, 15, 15, 15, 15);
      g.drawString(
          font,
          fit(row.getString("name"), width - 25),
          getX() + 23,
          getY() + 6,
          row.getBoolean("owned") ? MUTED : TEXT,
          false);
    }
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    if (confirmation) return true;
    if (my >= bodyTop && my <= bodyBottom) {
      if (mx >= right) {
        detailScroll = Math.max(0, detailScroll - (int) Math.signum(dy));
        return true;
      }
      if (mx >= x + 10 && mx <= x + 10 + left) {
        listScroll = Math.max(0, listScroll - (int) Math.signum(dy));
        rebuildWidgets();
        return true;
      }
    }
    return super.mouseScrolled(mx, my, dx, dy);
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (confirmation && key == 256) {
      confirmation = false;
      rebuildWidgets();
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }

  @Override
  public void onClose() {
    if (state.hasUUID("session"))
      PacketDistributor.sendToServer(
          new MentorNetwork.Request(state.getUUID("session"), "close", "", "", 0));
    super.onClose();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
