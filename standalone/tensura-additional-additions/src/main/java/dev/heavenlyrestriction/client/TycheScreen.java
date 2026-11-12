package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.tyche.TycheNetwork;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class TycheScreen extends Screen {
  private CompoundTag state;
  private boolean confirm, pending;
  private int x, y, w, h, historyScroll, refresh;

  public TycheScreen(CompoundTag state) {
    super(Component.literal("Tyche · Lord of Fortune"));
    this.state = state.copy();
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(() -> TycheNetwork.receiver = TycheScreen::receive);
  }

  private static void receive(CompoundTag t) {
    var mc = Minecraft.getInstance();
    if (t.getBoolean("open") && t.getBoolean("allowed")) mc.setScreen(new TycheScreen(t));
    else if (mc.screen instanceof TycheScreen s) {
      boolean changed =
          s.state.getBoolean("learned") != t.getBoolean("learned")
              || s.state.getBoolean("ready") != t.getBoolean("ready")
              || Double.compare(s.state.getDouble("cost"), t.getDouble("cost")) != 0
              || s.state.hasUUID("session")
                  && t.hasUUID("session")
                  && !s.state.getUUID("session").equals(t.getUUID("session"));
      boolean response = s.pending || !t.getString("notice").isEmpty();
      s.state = t.copy();
      if (changed || response) {
        s.pending = false;
        s.confirm = false;
        s.rebuildWidgets();
      }
    }
  }

  @Override
  protected void init() {
    w = Math.min(350, width - 16);
    h = Math.min(228, height - 16);
    x = (width - w) / 2;
    y = (height - h) / 2;
    if (!state.getBoolean("learned")) {
      var b =
          new SimpleButton(
              x + 10,
              y + h - 27,
              w - 100,
              18,
              Component.literal(confirm ? "Confirm synthesis" : "Review synthesis"),
              Component.empty(),
              q -> {
                if (pending || !state.getBoolean("ready") || !state.hasUUID("session")) return;
                if (confirm) {
                  pending = true;
                  PacketDistributor.sendToServer(
                      new TycheNetwork.Request(state.getUUID("session"), true));
                } else {
                  confirm = true;
                  rebuildWidgets();
                }
              });
      b.active = !pending && state.getBoolean("ready") && state.hasUUID("session");
      b.setActiveCheck(() -> !pending && state.getBoolean("ready") && state.hasUUID("session"));
      b.setClickedCheck(() -> b.isActive() && b.isHoveredOrFocused());
      addRenderableWidget(b);
    }
    addRenderableWidget(
        new SimpleButton(
            x + w - 80,
            y + h - 27,
            70,
            18,
            Component.literal(confirm ? "Cancel" : "Close"),
            Component.empty(),
            b -> {
              if (confirm) {
                confirm = false;
                rebuildWidgets();
              } else onClose();
            }));
  }

  private void row(GuiGraphics g, int line, String label, boolean good) {
    g.drawString(
        font,
        (good ? "✓ " : "• ") + label,
        x + 12,
        y + (h < 210 ? 35 : 42) + line * (h < 210 ? 14 : 18),
        good ? 0xffa4dfb5 : 0xffb0c3cd,
        false);
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    g.fill(0, 0, width, height, 0x99061119);
    g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xff416975);
    g.fill(x, y, x + w, y + h, 0xff102731);
    g.fill(x + 1, y + 1, x + w - 1, y + 31, 0xff0a1b25);
    g.blit(HeavenlyRestriction.id("textures/skill/tyche.png"), x + 7, y + 4, 0, 0, 24, 24, 24, 24);
    g.drawString(font, title, x + 38, y + 10, 0xffe6c687, false);
    if (!state.getBoolean("learned")) {
      row(g, 0, "Critical Merchant mastered", state.getBoolean("merchant"));
      row(g, 1, "Tuner mastered", state.getBoolean("tuner"));
      row(g, 2, "Awakening completed", state.getBoolean("awakened") && state.getBoolean("settled"));
      row(
          g,
          3,
          "Compounded criticals: "
              + state.getInt("criticals")
              + " / "
              + state.getInt("criticalRequired"),
          state.getInt("criticals") >= state.getInt("criticalRequired"));
      row(
          g,
          4,
          "Tuner damage types survived: "
              + state.getInt("survivals")
              + " / "
              + state.getInt("survivalRequired"),
          state.getInt("survivals") >= state.getInt("survivalRequired"));
      var cost = NumberFormat.getIntegerInstance(Locale.ROOT).format(state.getDouble("cost"));
      g.drawString(
          font,
          "Cost: " + cost + " permanent magicules",
          x + 12,
          y + (h < 210 ? 107 : 140),
          state.getBoolean("affordable") ? 0xffe6c687 : 0xffffa290,
          false);
      String explanation =
          confirm
              ? "Consumes both parents and pays the cost."
              : "No EP gate. Awakening bonuses apply first.";
      g.drawString(
          font,
          font.plainSubstrByWidth(explanation, w - 24),
          x + 12,
          y + (h < 210 ? 121 : 158),
          0xffa6bdc7,
          false);
    } else {
      g.drawWordWrap(
          font,
          Component.literal(
              "Fate Insurance · " + state.getInt("timer") + "s until damage history resets."),
          x + 12,
          y + 43,
          w - 24,
          0xffe6c687);
      String history =
          "Recorded damage types: "
              + (state.getString("history").isEmpty() ? "None" : state.getString("history"));
      g.drawString(
          font,
          "Against All Odds: "
              + state.getInt("oddsSeconds")
              + "s active · "
              + state.getInt("oddsCooldown")
              + "s cooldown",
          x + 12,
          y + 74,
          0xffa4dfb5,
          false);
      g.enableScissor(x + 12, y + 91, x + w - 12, y + h - 55);
      g.drawWordWrap(
          font, Component.literal(history), x + 12, y + 94 - historyScroll, w - 26, 0xffb8d1dc);
      g.disableScissor();
      if (state.getString("notice").isEmpty())
        g.drawString(
            font, "Shift + Dividend opens this page.", x + 12, y + h - 43, 0xffa6bdc7, false);
    }
    String notice = state.getString("notice");
    if (!notice.isEmpty())
      g.drawString(
          font, font.plainSubstrByWidth(notice, w - 24), x + 12, y + h - 43, 0xfff0d49c, false);
    super.render(g, mx, my, partial);
  }

  @Override
  public void tick() {
    if (++refresh >= 20) {
      refresh = 0;
      if (state.hasUUID("session"))
        PacketDistributor.sendToServer(new TycheNetwork.Request(state.getUUID("session"), false));
    }
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    if (state.getBoolean("learned")) {
      int max =
          Math.max(
              0,
              font.split(
                              Component.literal(
                                  "Recorded damage types: " + state.getString("history")),
                              w - 26)
                          .size()
                      * 9
                  - (h - 149));
      historyScroll = Math.clamp(historyScroll - (int) (sy * 18), 0, max);
      return true;
    }
    return super.mouseScrolled(mx, my, sx, sy);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
