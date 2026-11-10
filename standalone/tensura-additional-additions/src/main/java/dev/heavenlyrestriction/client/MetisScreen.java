package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.metis.MetisNetwork;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import java.util.*;
import java.util.function.*;
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
import org.lwjgl.glfw.GLFW;

/** Dark cyan Tensura panels, a readable four-slot matrix and deliberate synthesis confirmation. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class MetisScreen extends Screen {
  private static final int TEXT = 0xffe0eff3,
      MUTED = 0xff89a8b7,
      CYAN = 0xff88deec,
      GOLD = 0xffefd49b,
      EDGE = 0xff315866,
      PANEL = 0xff102731;
  private CompoundTag state;
  private int listScroll, visibleRows;
  private int x,
      y,
      w,
      h,
      left,
      right,
      rw,
      top,
      bottom,
      slot,
      mode,
      priority = 2,
      category,
      page,
      delay = -1,
      scroll;
  private boolean current = true, pending, confirmation;
  private String selected = "", query = "", notice = "", popup = "", tab;
  private EditBox search;
  private final List<Control> controls = new ArrayList<>();

  public MetisScreen(CompoundTag data) {
    super(Component.translatable("heavenlyrestriction.metis.title"));
    state = data.copy();
    tab = data.getBoolean("learned") ? "matrix" : "synthesis";
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(() -> MetisNetwork.receiver = MetisScreen::receive);
  }

  public static void receive(CompoundTag data) {
    var mc = Minecraft.getInstance();
    if (data.getBoolean("open") && data.getBoolean("allowed")) {
      mc.setScreen(new MetisScreen(data));
      return;
    }
    if (mc.screen instanceof MetisScreen s
        && data.hasUUID("session")
        && data.getUUID("session").equals(s.state.getUUID("session"))) {
      if (!data.getBoolean("allowed")) {
        mc.setScreen(null);
        return;
      }
      boolean learned = !s.state.getBoolean("learned") && data.getBoolean("learned");
      s.state = data.copy();
      s.pending = false;
      s.notice = data.getString("notice");
      s.page = data.getInt("page");
      s.listScroll = Math.clamp(s.listScroll, 0, Math.max(0, s.spells().size() - s.visibleRows));
      if (learned) {
        s.tab = "matrix";
        s.confirmation = false;
        s.rebuildWidgets();
      } else s.refresh();
    }
  }

  private ListTag spells() {
    return state.getList("rows", Tag.TAG_COMPOUND);
  }

  private CompoundTag assigned(int index) {
    for (var t : state.getList("slots", Tag.TAG_COMPOUND)) {
      var r = (CompoundTag) t;
      if (r.getInt("slot") == index) return r;
    }
    return new CompoundTag();
  }

  private CompoundTag chosen() {
    for (var t : spells()) {
      var r = (CompoundTag) t;
      if (r.getString("id").equals(selected)) return r;
    }
    var r = assigned(slot);
    return r.getString("id").equals(selected) ? r : new CompoundTag();
  }

  private ListTag modes() {
    return chosen().getList("modes", Tag.TAG_COMPOUND);
  }

  private CompoundTag option() {
    for (var t : modes()) {
      var r = (CompoundTag) t;
      if (r.getInt("id") == mode) return r;
    }
    return new CompoundTag();
  }

  private String fit(String value, int pixels) {
    return font.width(value) <= pixels
        ? value
        : font.plainSubstrByWidth(value, Math.max(0, pixels - 7)) + "…";
  }

  private Control control(
      int bx, int by, int bw, String label, Runnable run, BooleanSupplier enabled) {
    var b = new Control(bx, by, bw, 18, () -> label, run, enabled, () -> false);
    controls.add(b);
    return addRenderableWidget(b);
  }

  private void chooseSlot(int index) {
    slot = index;
    var row = assigned(index);
    selected = row.getString("id");
    mode = row.getInt("mode");
    priority = row.isEmpty() ? 2 : row.getInt("priority");
    current = row.isEmpty() || row.getBoolean("target");
    popup = "";
    refresh();
  }

  @Override
  protected void init() {
    controls.clear();
    w = Math.min(450, width - 16);
    h = Math.min(286, height - 16);
    x = (width - w) / 2;
    y = (height - h) / 2;
    left = Math.max(112, (w - 30) * 36 / 100);
    right = x + left + 16;
    rw = w - left - 24;
    top = y + (h < 220 ? 56 : 64);
    bottom = y + h - (h < 220 ? 60 : 74);
    popup = "";
    control(
        x + 8,
        y + 35,
        Math.min(90, (w - 25) / 3),
        "Matrix",
        () -> switchTab("matrix"),
        () -> state.getBoolean("learned"));
    control(
        x + 104,
        y + 35,
        Math.min(106, (w - 25) / 3),
        "Synthesis",
        () -> switchTab("synthesis"),
        () -> true);
    control(x + w - 29, y + 7, 21, "×", this::onClose, () -> true);
    if (tab.equals("synthesis")) {
      control(
          x + 10,
          y + h - 34,
          w - 20,
          "Synthesize Metis",
          () -> {
            confirmation = true;
            refresh();
          },
          () -> state.getBoolean("ready") && !state.getBoolean("learned"));
      return;
    }
    search = new EditBox(font, right, top, rw, 18, Component.literal("Search learned magic"));
    search.setMaxLength(64);
    search.setValue(query);
    search.setHint(Component.literal("Search learned magic…"));
    search.setResponder(
        v -> {
          query = v;
          delay = 6;
        });
    addRenderableWidget(search);
    int sh = Math.max(12, Math.min(31, (bottom - top) / 4));
    for (int n = 0; n < 4; n++) {
      int index = n;
      var b =
          new Control(
              x + 8,
              top + n * sh,
              left,
              sh - (h < 220 ? 1 : 3),
              () -> {
                var row = assigned(index);
                return (index + 1)
                    + " · "
                    + (index >= state.getInt("capacity")
                        ? "Mastery unlock"
                        : row.isEmpty() ? "Empty" : row.getString("name"));
              },
              () -> chooseSlot(index),
              () -> index < state.getInt("capacity"),
              () -> slot == index);
      controls.add(b);
      addRenderableWidget(b);
    }
    int rowTop = top + 23;
    visibleRows = Math.clamp((bottom - rowTop) / 12, 1, 5);
    int rowH = Math.max(12, Math.min(21, (bottom - rowTop) / visibleRows));
    for (int n = 0; n < 5; n++) {
      int index = n;
      var b =
          new Control(
              right,
              rowTop + n * rowH,
              rw,
              rowH - 1,
              () ->
                  index + listScroll < spells().size() && index < visibleRows
                      ? spells().getCompound(index + listScroll).getString("name")
                      : "",
              () -> {
                var row = spells().getCompound(index + listScroll);
                selected = row.getString("id");
                mode = 0;
                for (var t : row.getList("modes", Tag.TAG_COMPOUND))
                  if (((CompoundTag) t).getBoolean("available")) {
                    mode = ((CompoundTag) t).getInt("id");
                    break;
                  }
                refresh();
              },
              () -> index + listScroll < spells().size() && index < visibleRows,
              () ->
                  index + listScroll < spells().size()
                      && index < visibleRows
                      && spells().getCompound(index + listScroll).getString("id").equals(selected));
      b.shown = () -> index + listScroll < spells().size() && index < visibleRows;
      b.icon =
          () ->
              index + listScroll < spells().size() && index < visibleRows
                  ? spells().getCompound(index + listScroll).getString("icon")
                  : "";
      controls.add(b);
      addRenderableWidget(b);
    }
    var modeButton =
        new Control(
            x + 8,
            bottom + 4,
            left,
            18,
            () -> "Mode: " + option().getString("name"),
            () -> {
              popup = "mode";
              scroll = 0;
              refresh();
            },
            () -> !chosen().isEmpty(),
            () -> false);
    controls.add(modeButton);
    addRenderableWidget(modeButton);
    var priorities =
        new Control(
            right,
            bottom + 4,
            rw / 2 - 2,
            18,
            () -> "Priority: " + List.of("Healing", "Support", "Offense").get(priority),
            () -> {
              popup = "priority";
              scroll = 0;
              refresh();
            },
            () -> true,
            () -> false);
    controls.add(priorities);
    addRenderableWidget(priorities);
    var target =
        new Control(
            right + rw / 2 + 2,
            bottom + 4,
            rw / 2 - 2,
            18,
            () -> current ? "Target: current" : "Target: nearest",
            () -> {
              current = !current;
              refresh();
            },
            () -> true,
            () -> false);
    controls.add(target);
    addRenderableWidget(target);
    var optimal =
        new Control(
            x + 8,
            bottom + 26,
            left,
            18,
            () -> "Optimal Solution: " + (state.getBoolean("optimal") ? "ON" : "OFF"),
            () -> request("optimal", "", !state.getBoolean("optimal")),
            () -> true,
            () -> state.getBoolean("optimal"));
    controls.add(optimal);
    addRenderableWidget(optimal);
    control(
        right,
        bottom + 26,
        24,
        "‹",
        () -> {
          page--;
          request("list", "", false);
        },
        () -> state.getInt("page") > 0);
    control(
        right + 28,
        bottom + 26,
        24,
        "›",
        () -> {
          page++;
          request("list", "", false);
        },
        () -> state.getInt("page") + 1 < state.getInt("pages"));
    control(
        right + 56,
        bottom + 26,
        Math.max(38, (rw - 60) / 2),
        "Clear",
        () -> request("assign", "", current),
        () -> !assigned(slot).isEmpty());
    control(
        right + 60 + (rw - 60) / 2,
        bottom + 26,
        (rw - 60) / 2,
        "Assign",
        () -> request("assign", selected, current),
        () -> !chosen().isEmpty() && option().getBoolean("available"));
    chooseSlot(slot);
    refresh();
  }

  private void switchTab(String value) {
    search = null;
    listScroll = 0;
    tab = value;
    confirmation = false;
    popup = "";
    rebuildWidgets();
  }

  private void request(String action, String id, boolean value) {
    if (pending || !state.hasUUID("session")) return;
    pending = true;
    popup = "";
    delay = -1;
    refresh();
    PacketDistributor.sendToServer(
        new MetisNetwork.Request(
            state.getUUID("session"),
            action,
            id,
            query,
            page,
            category,
            slot,
            id.isEmpty() ? 0 : mode,
            priority,
            value));
  }

  private void refresh() {
    if (search != null) search.active = !pending && !confirmation && popup.isEmpty();
    for (var b : controls) {
      b.setMessage(Component.literal(b.label.get()));
      b.setTooltip(
          confirmation || !popup.isEmpty()
              ? null
              : Tooltip.create(Component.literal(b.label.get())));
      b.active = !pending && !confirmation && popup.isEmpty() && b.enabled.getAsBoolean();
      b.visible = b.shown.getAsBoolean();
    }
  }

  @Override
  public void tick() {
    if (delay > 0 && --delay == 0) {
      if (pending) delay = 1;
      else {
        page = 0;
        request("list", "", false);
      }
    }
    refresh();
  }

  private final class Control extends SimpleButton {
    private final Supplier<String> label;
    private final Runnable action;
    private final BooleanSupplier enabled, lit;
    private BooleanSupplier shown = () -> true;
    private Supplier<String> icon = () -> "";

    private Control(
        int x,
        int y,
        int w,
        int h,
        Supplier<String> label,
        Runnable action,
        BooleanSupplier enabled,
        BooleanSupplier lit) {
      super(
          x,
          y,
          w,
          h,
          Component.literal(label.get()),
          Component.literal(label.get()),
          b -> action.run());
      this.label = label;
      this.action = action;
      this.enabled = enabled;
      this.lit = lit;
      setTooltip(Tooltip.create(Component.literal(label.get())));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
      if (active && visible && button == 0 && isMouseOver(mx, my)) {
        playDownSound(Minecraft.getInstance().getSoundManager());
        action.run();
        return true;
      }
      return false;
    }

    @Override
    public void setMessage(Component value) {
      this.text = value;
      super.setMessage(value);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      boolean on = lit.getAsBoolean();
      g.fill(
          getX(),
          getY(),
          getX() + width,
          getY() + height,
          on ? GOLD : active && isHoveredOrFocused() ? CYAN : EDGE);
      g.fill(
          getX() + 1,
          getY() + 1,
          getX() + width - 1,
          getY() + height - 1,
          on ? 0xff243c46 : 0xff122c36);
      g.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + 3, on ? 0xff789396 : 0xff28505a);
      int pad = 5;
      var texture = icon.get().isBlank() ? null : ResourceLocation.tryParse(icon.get());
      if (texture != null) {
        int size = Math.min(18, height - 2);
        g.blit(texture, getX() + 2, getY() + 1, 0, 0, size, size, size, size);
        pad = size + 5;
      }
      g.drawString(
          font,
          fit(label.get(), width - pad - 4),
          getX() + pad,
          getY() + Math.max(1, (height - 8) / 2),
          active ? (on ? GOLD : TEXT) : MUTED,
          false);
    }
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    g.fill(0, 0, width, height, 0xb0081119);
    g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xff060f16);
    g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
    g.fill(x, y, x + w, y + h, PANEL);
    g.fill(x + 2, y + 2, x + w - 2, y + 29, 0xff0a1d28);
    g.blit(HeavenlyRestriction.id("textures/skill/metis.png"), x + 7, y + 4, 0, 0, 24, 24, 24, 24);
    g.drawString(font, title, x + 36, y + 8, GOLD, false);
    g.drawString(
        font,
        state.getBoolean("learned") ? "PARALLEL COGNITION" : "ULTIMATE SYNTHESIS",
        x + 36,
        y + 19,
        MUTED,
        false);
    if (tab.equals("synthesis")) renderSynthesis(g);
    else {
      if (h >= 220)
        g.drawString(
            font, "MATRIX  " + state.getInt("capacity") + " THREADS", x + 9, top - 9, CYAN, false);
      if (h >= 220) g.drawString(font, "LEARNED MAGIC", right, top - 9, CYAN, false);
      if (spells().isEmpty())
        g.drawString(font, "No learned magic found", right + 5, top + 31, MUTED, false);
      String summary =
          String.format(
              Locale.ROOT,
              "Auto-cast %s · Magicule cost −%.0f%% · %d analyses",
              state.getBoolean("auto") ? "ON" : "OFF",
              (1 - state.getDouble("factor")) * 100,
              state.getInt("dossiers"));
      if (h >= 220) g.drawString(font, fit(summary, w - 18), x + 9, y + h - 25, CYAN, false);
    }
    super.render(g, mx, my, partial);
    g.drawString(
        font,
        fit(
            pending
                ? "Updating…"
                : notice.isEmpty()
                    ? (tab.equals("matrix")
                        ? "Choose a thread, then a spell and mode."
                        : "Your components and stored items are preserved until confirmation.")
                    : Component.translatable(notice).getString(),
            w - 18),
        x + 9,
        y + h - 13,
        MUTED,
        false);
    if (confirmation || !popup.isEmpty()) {
      g.pose().pushPose();
      g.pose().translate(0, 0, 300);
      renderPopup(g, mx, my);
      g.pose().popPose();
    }
  }

  private void line(GuiGraphics g, int row, String label, boolean met) {
    g.drawString(
        font,
        (met ? "✓ " : "• ") + label,
        x + 12,
        top + row * (h < 220 ? 15 : 17),
        met ? CYAN : MUTED,
        false);
  }

  private void renderSynthesis(GuiGraphics g) {
    line(g, 0, "Split Mind fully mastered", state.getBoolean("split"));
    line(g, 1, "Great Sage fully mastered", state.getBoolean("sage"));
    line(
        g,
        2,
        "True Demon Lord / True Hero awakening complete",
        state.getBoolean("awakened") && state.getBoolean("settled"));
    line(
        g,
        3,
        state.getInt("magicCount")
            + " / "
            + state.getInt("magicRequired")
            + " learned aspectual / spiritual magics",
        state.getInt("magicCount") >= state.getInt("magicRequired"));
    line(
        g,
        4,
        String.format(Locale.ROOT, "Cost: %,.0f permanent magicules", state.getDouble("cost")),
        state.getBoolean("affordable"));
    if (h > 220)
      g.drawString(
          font,
          "No EP requirement · Awakening bonuses apply before the bill",
          x + 12,
          top + 91,
          GOLD,
          false);
  }

  private int px() {
    return x + 8;
  }

  private int py() {
    return y + 57;
  }

  private int pw() {
    return w - 16;
  }

  private void renderPopup(GuiGraphics g, int mx, int my) {
    int px = px(), py = py(), pw = pw();
    g.fill(x + 1, y + 30, x + w - 1, y + h - 1, 0xe0061119);
    if (confirmation) {
      int ph = 88;
      g.fill(px, py, px + pw, py + ph, GOLD);
      g.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xff112a35);
      g.drawString(font, "Confirm synthesis", px + 8, py + 8, GOLD, false);
      g.drawString(
          font,
          fit("Replace both component skills with Metis?", pw - 16),
          px + 8,
          py + 26,
          TEXT,
          false);
      g.drawString(
          font,
          String.format(Locale.ROOT, "Permanent cost: %,.0f magicules", state.getDouble("cost")),
          px + 8,
          py + 40,
          CYAN,
          false);
      popupButton(g, px + 8, py + 61, (pw - 24) / 2, "Cancel");
      popupButton(g, px + 16 + (pw - 24) / 2, py + 61, (pw - 24) / 2, "Synthesize");
      return;
    }
    var labels = new ArrayList<String>();
    if (popup.equals("priority"))
      labels.addAll(List.of("Healing priority", "Support priority", "Offense priority"));
    else
      for (var t : modes()) {
        var r = (CompoundTag) t;
        labels.add(r.getString("name") + (r.getBoolean("available") ? "" : " [Locked]"));
      }
    int count = Math.min(Math.max(1, (h - 80) / 18), labels.size());
    scroll = Math.clamp(scroll, 0, Math.max(0, labels.size() - count));
    g.fill(px, py, px + pw, py + count * 18 + 22, EDGE);
    g.fill(px + 1, py + 1, px + pw - 1, py + count * 18 + 21, 0xff0c202a);
    g.drawString(
        font,
        popup.equals("priority") ? "Casting priority" : "Learned modes · scroll for more",
        px + 6,
        py + 6,
        GOLD,
        false);
    for (int n = 0; n < count; n++)
      popupButton(g, px + 3, py + 20 + n * 18, pw - 6, fit(labels.get(scroll + n), pw - 18));
  }

  private void popupButton(GuiGraphics g, int px, int py, int pw, String label) {
    g.fill(px, py, px + pw, py + 16, EDGE);
    g.fill(px + 1, py + 1, px + pw - 1, py + 15, 0xff173745);
    g.drawString(font, label, px + 5, py + 4, TEXT, false);
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (confirmation) {
      if (button == 0 && my >= py() + 61 && my < py() + 77) {
        if (mx >= px() + 8 && mx < px() + pw() / 2) {
          confirmation = false;
          refresh();
        } else if (mx >= px() + pw() / 2 && mx < px() + pw() - 8) {
          confirmation = false;
          request("synthesize", "", false);
        }
      }
      return true;
    }
    if (!popup.isEmpty()) {
      if (button == 0
          && mx >= px() + 3
          && mx < px() + pw() - 3
          && my >= py() + 20
          && my < py() + 20 + Math.max(1, (h - 80) / 18) * 18) {
        int index = scroll + (int) ((my - py() - 20) / 18);
        if (popup.equals("priority") && index < 3) {
          priority = index;
          popup = "";
        } else if (popup.equals("mode")
            && index < modes().size()
            && index < scroll + Math.max(1, (h - 80) / 18)) {
          var r = modes().getCompound(index);
          if (r.getBoolean("available")) {
            mode = r.getInt("id");
            popup = "";
          }
        } else popup = "";
      } else popup = "";
      refresh();
      return true;
    }
    return super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    if (!popup.isEmpty()) {
      scroll = Math.max(0, scroll - (int) Math.signum(dy));
      return true;
    }
    if (tab.equals("matrix") && mx >= right && mx < right + rw && my >= top + 20 && my < bottom) {
      listScroll =
          Math.clamp(
              listScroll - (int) Math.signum(dy), 0, Math.max(0, spells().size() - visibleRows));
      refresh();
      return true;
    }
    return super.mouseScrolled(mx, my, dx, dy);
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (confirmation || !popup.isEmpty()) {
      if (key == GLFW.GLFW_KEY_ESCAPE) {
        confirmation = false;
        popup = "";
        refresh();
      }
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }

  @Override
  public void onClose() {
    if (state.hasUUID("session"))
      PacketDistributor.sendToServer(
          new MetisNetwork.Request(
              state.getUUID("session"), "close", "", "", 0, 0, 0, 0, 0, false));
    super.onClose();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
