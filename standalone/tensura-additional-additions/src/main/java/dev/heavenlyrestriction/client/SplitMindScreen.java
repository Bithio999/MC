package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.splitmind.SplitMindNetwork;
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

/** Fitted Tensura-style controls, learned-only search and category filters. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class SplitMindScreen extends Screen {
  private static final int TEXT = 0xffe4e0eb,
      MUTED = 0xffaba2b7,
      GOLD = 0xffe7c27a,
      CYAN = 0xff8cdee6;
  private CompoundTag state;
  private String selected = "", query = "", notice = "";
  private int x, y, w, h, delay = -1;
  private boolean pending;
  private EditBox search;
  private SimpleButton assign, previous, next;
  private final List<SimpleButton> rows = new ArrayList<>();

  public SplitMindScreen(CompoundTag state) {
    super(Component.translatable("heavenlyrestriction.split_mind.menu"));
    this.state = state.copy();
    query = state.getString("query");
    selected = state.getString("assignment");
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(() -> SplitMindNetwork.receiver = SplitMindScreen::receive);
  }

  private static void receive(CompoundTag data) {
    var mc = Minecraft.getInstance();
    if (data.getBoolean("open") && data.getBoolean("allowed"))
      mc.setScreen(new SplitMindScreen(data));
    else if (mc.screen instanceof SplitMindScreen s) {
      if (!data.getBoolean("allowed")) {
        mc.setScreen(null);
        return;
      }
      s.state = data.copy();
      s.pending = false;
      s.notice = data.getString("message");
      s.refresh();
    }
  }

  private ListTag data() {
    return state.getList("rows", Tag.TAG_COMPOUND);
  }

  private CompoundTag chosen() {
    for (var row : data())
      if (((CompoundTag) row).getString("id").equals(selected)) return (CompoundTag) row;
    return null;
  }

  private String fit(String s, int pixels) {
    return font.width(s) <= pixels ? s : font.plainSubstrByWidth(s, Math.max(0, pixels - 9)) + "…";
  }

  private SimpleButton button(int bx, int by, int bw, String label, Runnable action) {
    return addRenderableWidget(
        new SimpleButton(
            bx, by, bw, 18, Component.literal(label), Component.literal(label), b -> action.run()));
  }

  @Override
  protected void init() {
    w = Math.min(360, width - 16);
    h = Math.min(270, height - 16);
    x = (width - w) / 2;
    y = (height - h) / 2;
    rows.clear();
    search =
        new EditBox(
            font,
            x + 9,
            y + 27,
            w - 18,
            18,
            Component.translatable("heavenlyrestriction.split_mind.search"));
    search.setMaxLength(64);
    search.setValue(query);
    search.setHint(Component.translatable("heavenlyrestriction.split_mind.search"));
    search.setResponder(
        v -> {
          query = v;
          delay = 6;
        });
    addRenderableWidget(search);
    for (int n = 0; n < 3; n++) {
      int type = n;
      button(
          x + 9 + n * (w - 18) / 3,
          y + 50,
          (w - 22) / 3,
          List.of("All magic", "Aspectual", "Spiritual").get(n),
          () -> request("list", "", type, 0));
    }
    int rowH = Math.max(14, Math.min(25, (h - 144) / 6));
    for (int n = 0; n < 6; n++)
      rows.add(addRenderableWidget(new EntryButton(x + 9, y + 74 + n * rowH, w - 18, rowH - 2, n)));
    previous =
        button(
            x + 9,
            y + h - 43,
            24,
            "‹",
            () -> request("list", "", state.getInt("category"), state.getInt("page") - 1));
    next =
        button(
            x + 36,
            y + h - 43,
            24,
            "›",
            () -> request("list", "", state.getInt("category"), state.getInt("page") + 1));
    button(
        x + w - 190,
        y + h - 43,
        65,
        "Clear",
        () -> request("assign", "", state.getInt("category"), state.getInt("page")));
    assign =
        button(
            x + w - 120,
            y + h - 43,
            111,
            "Assign magic",
            () -> request("assign", selected, state.getInt("category"), state.getInt("page")));
    button(x + w - 63, y + h - 22, 54, "Close", this::onClose);
    refresh();
  }

  private void refresh() {
    for (int n = 0; n < rows.size(); n++) {
      rows.get(n).visible = n < data().size();
      rows.get(n).active = !pending;
      if (n < data().size()) {
        var row = data().getCompound(n);
        rows.get(n)
            .setTooltip(
                Tooltip.create(
                    Component.literal(
                        row.getString("name")
                            + "\n"
                            + row.getString("type")
                            + " · "
                            + (row.getBoolean("mastered") ? "Mastered" : "Learned")
                            + String.format(
                                Locale.ROOT,
                                "\n"
                                    + "Base magicules %.1f · Aura %.1f\n"
                                    + "Cast %.2fs · Current cooldown %ds",
                                row.getDouble("mp"),
                                row.getDouble("ap"),
                                row.getInt("castTicks") / 20d,
                                row.getInt("cooldown")))));
      }
    }
    if (assign != null) {
      assign.active = !pending && chosen() != null;
      previous.active = !pending && state.getInt("page") > 0;
      next.active = !pending && state.getInt("page") + 1 < state.getInt("pages");
    }
  }

  private void request(String action, String id, int category, int page) {
    if (pending) return;
    pending = true;
    delay = -1;
    notice = "";
    refresh();
    PacketDistributor.sendToServer(new SplitMindNetwork.Request(action, id, query, category, page));
  }

  @Override
  public void tick() {
    if (delay > 0 && --delay == 0) {
      if (pending) delay = 1;
      else request("list", "", state.getInt("category"), 0);
    }
  }

  private final class EntryButton extends SimpleButton {
    private final int index;

    EntryButton(int bx, int by, int bw, int bh, int index) {
      super(
          bx,
          by,
          bw,
          bh,
          Component.empty(),
          Component.empty(),
          b -> {
            selected = data().getCompound(index).getString("id");
            refresh();
          });
      this.index = index;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      if (index >= data().size()) return;
      var row = data().getCompound(index);
      boolean on = row.getString("id").equals(selected),
          assigned = row.getString("id").equals(state.getString("assignment"));
      g.fill(
          getX(),
          getY(),
          getX() + width,
          getY() + height,
          on ? GOLD : isHoveredOrFocused() ? 0xffa89cb5 : 0xff655c70);
      g.fill(
          getX() + 1,
          getY() + 1,
          getX() + width - 1,
          getY() + height - 1,
          on ? 0xff3d3443 : 0xff28252e);
      int size = Math.min(20, height - 2);
      var icon = ResourceLocation.tryParse(row.getString("icon"));
      if (icon != null) g.blit(icon, getX() + 3, getY() + 1, 0, 0, size, size, size, size);
      g.drawString(
          font,
          fit(row.getString("name"), width - size - 96),
          getX() + size + 8,
          getY() + Math.max(2, (height - 8) / 2),
          on ? GOLD : TEXT,
          false);
      String kind = assigned ? "ASSIGNED" : row.getString("type");
      g.drawString(
          font,
          kind,
          getX() + width - 76,
          getY() + Math.max(2, (height - 8) / 2),
          assigned ? CYAN : MUTED,
          false);
    }
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    g.fill(0, 0, width, height, 0xa0100e18);
    g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xff17131e);
    g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xff716379);
    g.fill(x, y, x + w, y + h, 0xff302c34);
    g.fill(x + 2, y + 2, x + w - 2, y + 21, 0xff211b29);
    g.drawString(font, title, x + 9, y + 7, GOLD, false);
    String status = "AUTO-CAST " + (state.getBoolean("auto") ? "ON" : "OFF");
    g.drawString(font, status, x + w - 103, y + 7, state.getBoolean("auto") ? CYAN : MUTED, false);
    int category = state.getInt("category"), cw = (w - 18) / 3;
    g.fill(x + 9 + category * cw, y + 69, x + 9 + category * cw + (w - 22) / 3, y + 71, CYAN);
    if (data().isEmpty())
      g.drawCenteredString(
          font,
          Component.translatable("heavenlyrestriction.split_mind.empty"),
          x + w / 2,
          y + 96,
          MUTED);
    String current = state.getString("assignedName");
    g.drawString(
        font,
        fit(current.isEmpty() ? "Assigned: None" : "Assigned: " + current, w - 120),
        x + 9,
        y + h - 58,
        CYAN,
        false);
    g.drawString(
        font,
        (state.getInt("page") + 1) + " / " + Math.max(1, state.getInt("pages")),
        x + 66,
        y + h - 37,
        MUTED,
        false);
    g.drawString(
        font,
        fit(
            pending
                ? "Updating…"
                : notice.isEmpty()
                    ? "Nearest hostile / combat opponent · 32 blocks"
                    : Component.translatable(notice).getString(),
            w - 78),
        x + 9,
        y + h - 16,
        MUTED,
        false);
    super.render(g, mx, my, partial);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
