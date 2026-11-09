package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.forge.ForgeNetwork;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Native Tensura buttons, a clipped equipment ledger and stable confirmations during live refresh.
 */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class ForgeScreen extends Screen {
  private static final int GOLD = 0xffedbf72,
      TEXT = 0xffe8e3db,
      MUTED = 0xffaaa6a1,
      LINE = 0xff66584a;
  private float uiScale = 1;

  public float uiScale() {
    return uiScale;
  }

  private CompoundTag state;
  private String tab, query = "", notice = "";
  private int x, y, w, h, page, selected = -1, material = 1, temper = 1, age;
  private boolean waiting, pending, confirm;
  private ForgeNetwork.Request queued;
  private EditBox search;
  private final List<Button> rows = new ArrayList<>(), controls = new ArrayList<>();

  public ForgeScreen(CompoundTag state) {
    super(Component.translatable("heavenlyrestriction.forge.title"));
    this.state = state.copy();
    tab = state.getString("mode");
    if (tab.equals("masterpiece")) tab = "opus";
    if (!state.getBoolean("learned")) tab = "synthesis";
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> ForgeNetwork.receiver = ForgeScreen::receive);
  }

  public static void receive(CompoundTag tag) {
    var mc = Minecraft.getInstance();
    if (tag.contains("ledger")) {
      ForgeNetwork.clientWorks.clear();
      var data = tag.getCompound("ledger");
      for (var key : data.getAllKeys())
        try {
          ForgeNetwork.clientWorks.put(UUID.fromString(key), data.getUUID(key));
        } catch (IllegalArgumentException ignored) {
        }
    }
    if (tag.getBoolean("open")) {
      mc.setScreen(new ForgeScreen(tag));
      return;
    }
    if (!(mc.screen instanceof ForgeScreen s)
        || !tag.hasUUID("session")
        || !s.state.getUUID("session").equals(tag.getUUID("session"))) return;
    boolean changed = s.state.getBoolean("learned") != tag.getBoolean("learned");
    s.state = tag.copy();
    if (!tag.getString("message").isEmpty()) s.notice = tag.getString("message");
    s.waiting = false;
    if (s.queued != null) {
      var q = s.queued;
      s.queued = null;
      s.send(q);
    } else s.pending = false;
    if (changed) {
      s.tab = "forge";
      s.confirm = false;
      s.rebuildWidgets();
    }
    s.refresh();
  }

  private void request(String action) {
    if (pending) return;
    pending = !action.equals("refresh");
    var r =
        new ForgeNetwork.Request(
            state.getUUID("session"), action, selected, temper * 10 + material);
    if (waiting) queued = r;
    else send(r);
    refresh();
  }

  private void send(ForgeNetwork.Request r) {
    waiting = true;
    PacketDistributor.sendToServer(r);
  }

  private class ForgeButton extends SimpleButton {
    private final boolean materialControl;

    ForgeButton(int x, int y, int width, String label, Runnable call) {
      super(x, y, width, 18, Component.literal(label), Component.literal(label), b -> call.run());
      materialControl = label.equals("Material");
      setActiveCheck(() -> active);
      setClickedCheck(() -> active && visible && isHovered());
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      if (materialControl) {
        var mats = state.getList("materials", Tag.TAG_COMPOUND);
        if (!mats.isEmpty()) {
          var m = mats.getCompound(material % mats.size());
          text =
              Component.literal(
                  m.getString("name")
                      + " ("
                      + m.getInt("count")
                      + ") · "
                      + Math.round(m.getDouble("quality") * 100)
                      + "%");
        }
      }
      super.renderWidget(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
      if (active && visible && b == 0 && isMouseOver(mx, my)) {
        onPress();
        playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
      }
      return false;
    }
  }

  private Button button(int bx, int by, int bw, String label, Runnable call) {
    var b = addRenderableWidget(new ForgeButton(bx, by, bw, label, call));
    controls.add(b);
    return b;
  }

  private List<CompoundTag> items() {
    var list = new ArrayList<CompoundTag>();
    for (var tag : state.getList("items", Tag.TAG_COMPOUND)) {
      var r = (CompoundTag) tag;
      if ((!tab.equals("arsenal") || r.getBoolean("weapon"))
          && r.getString("name").toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
        list.add(r);
    }
    return list;
  }

  private CompoundTag chosen() {
    for (var r : items()) if (r.getInt("slot") == selected) return r;
    return null;
  }

  private ItemStack stack(CompoundTag r) {
    return r == null
        ? ItemStack.EMPTY
        : ItemStack.parseOptional(minecraft.level.registryAccess(), r.getCompound("stack"));
  }

  private void switchTab(String next) {
    tab = next;
    query = "";
    page = 0;
    confirm = false;
    notice = "";
    rebuildWidgets();
  }

  @Override
  protected void init() {
    controls.clear();
    rows.clear();
    w = 450;
    h = 324;
    uiScale = Math.max(.1f, Math.min(1f, Math.min((width - 12) / 450f, (height - 12) / 324f)));
    x = (int) (width / uiScale - w) / 2;
    y = (int) (height / uiScale - h) / 2;
    int bw = (w - 28) / 4;
    String[] tabs = {"forge", "arsenal", "opus", "synthesis"},
        names = {"Divine Forge", "Living Arsenal", "Magnum Opus", "Synthesis"};
    for (int n = 0; n < 4; n++) {
      String next = tabs[n];
      var b = button(x + 8 + n * (bw + 4), y + 39, bw, names[n], () -> switchTab(next));
      b.active = state.getBoolean("learned") || next.equals("synthesis");
    }
    if (tab.equals("synthesis")) {
      button(
          x + 12,
          y + h - 31,
          w - 24,
          "Synthesize Hephaestus",
          () -> {
            if (!confirm) {
              confirm = true;
              return;
            }
            confirm = false;
            request("synthesize");
          });
      refresh();
      return;
    }
    if (tab.equals("forge")) {
      int b = (w - 31) / 5;
      String[] actions = {"craft", "refine", "engrave", "storage", "recover"},
          labels = {"Craft", "Refine", "Engrave", "Storage", "Recover"};
      for (int n = 0; n < 5; n++) {
        String a = actions[n];
        button(x + 9 + n * (b + 3), y + 64, b, labels[n], () -> request(a));
      }
    }
    search =
        addRenderableWidget(
            new EditBox(font, x + 10, y + 87, 197, 16, Component.literal("Search equipment")));
    search.setMaxLength(60);
    search.setHint(Component.literal("Search equipment…"));
    search.setValue(query);
    search.setResponder(
        q -> {
          query = q;
          page = 0;
          refresh();
        });
    for (int n = 0; n < 6; n++) {
      int index = n;
      var b =
          addRenderableWidget(
              new ForgeButton(
                  x + 9,
                  y + 108 + n * 24,
                  199,
                  "",
                  () -> {
                    var data = items();
                    int at = page * 6 + index;
                    if (at < data.size()) {
                      selected = data.get(at).getInt("slot");
                      confirm = false;
                      refresh();
                    }
                  }) {
                @Override
                protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                  var data = items();
                  int at = page * 6 + index;
                  if (at >= data.size()) return;
                  var r = data.get(at);
                  boolean chosen = r.getInt("slot") == selected;
                  g.fill(
                      getX(),
                      getY(),
                      getX() + width,
                      getY() + 22,
                      chosen ? GOLD : isHovered() ? 0xffa58b69 : LINE);
                  g.fill(
                      getX() + 1,
                      getY() + 1,
                      getX() + width - 1,
                      getY() + 21,
                      chosen ? 0xff3c342c : 0xff292827);
                  g.renderItem(stack(r), getX() + 3, getY() + 3);
                  g.drawString(
                      font,
                      fit(r.getString("name"), width - 29),
                      getX() + 25,
                      getY() + 7,
                      chosen ? GOLD : TEXT,
                      false);
                }
              });
      rows.add(b);
    }
    button(
        x + 10,
        y + 257,
        26,
        "‹",
        () -> {
          page = Math.max(0, page - 1);
          refresh();
        });
    button(
        x + 181,
        y + 257,
        26,
        "›",
        () -> {
          page = Math.min(Math.max(0, (items().size() - 1) / 6), page + 1);
          refresh();
        });
    if (!tab.equals("arsenal")) {
      button(
          x + 221,
          y + 179,
          w - 232,
          "Material",
          () -> {
            material = (material + 1) % state.getList("materials", Tag.TAG_COMPOUND).size();
            confirm = false;
          });
    }
    if (tab.equals("forge")) {
      String[] labels = {"Keen", "Piercing", "Conductor", "Enduring"};
      for (int n = 0; n < 4; n++) {
        int pick = n + 1;
        button(
            x + 221 + (n % 2) * ((w - 236) / 2 + 4),
            y + 203 + (n / 2) * 22,
            (w - 236) / 2,
            labels[n],
            () -> {
              temper = pick;
              confirm = false;
            });
      }
      int b = (w - 240) / 3;
      button(x + 221, y + 253, b, "Reforge", () -> request("reforge"));
      button(x + 225 + b, y + 253, b, "Temper", () -> request("temper"));
      button(x + 229 + b * 2, y + 253, b, "Repair", () -> request("repair"));
    }
    if (tab.equals("opus"))
      button(
          x + 221,
          y + 223,
          w - 232,
          "Designate masterpiece",
          () -> {
            if (!confirm) {
              confirm = true;
              return;
            }
            confirm = false;
            request("opus");
          });
    if (tab.equals("arsenal")) {
      button(x + 221, y + 195, w - 232, "Deploy selected", () -> request("deploy"));
      button(x + 221, y + 219, w - 232, "Deploy up to six", () -> request("deploy_all"));
      button(x + 221, y + 243, w - 232, "Recall all", () -> request("recall"));
    }
    button(x + 10, y + h - 28, 197, "Close", this::onClose);
    refresh();
  }

  private String fit(String text, int max) {
    return font.width(text) <= max
        ? text
        : font.plainSubstrByWidth(text, Math.max(0, max - 8)) + "…";
  }

  private void refresh() {
    var data = items();
    if (!data.isEmpty() && chosen() == null) selected = data.getFirst().getInt("slot");
    for (int n = 0; n < rows.size(); n++) rows.get(n).visible = page * 6 + n < data.size();
    for (var b : controls) {
      String label = b.getMessage().getString();
      b.active =
          !pending
              && (state.getBoolean("learned")
                  || label.equals("Synthesis")
                  || label.equals("Synthesize Hephaestus"));
      if (label.equals("Synthesize Hephaestus")) b.active = !pending && state.getBoolean("ready");
    }
  }

  @Override
  public void tick() {
    if (++age % 20 == 0 && !waiting && !pending) request("refresh");
  }

  private void line(GuiGraphics g, String text, int bx, int by, int width, int color) {
    g.drawWordWrap(font, Component.literal(text), bx, by, width, color);
  }

  private void gate(GuiGraphics g, String text, boolean ready, int yy) {
    g.drawString(
        font, (ready ? "✓  " : "•  ") + text, x + 21, yy, ready ? 0xffa3d6a1 : MUTED, false);
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xff11100f);
    g.fill(x, y, x + w, y + h, LINE);
    g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xff242322);
    g.fill(x + 1, y + 1, x + w - 1, y + 34, 0xff302a24);
    g.blit(
        HeavenlyRestriction.id("textures/skill/hephaestus.png"),
        x + 9,
        y + 5,
        0,
        0,
        25,
        25,
        25,
        25);
    g.drawString(font, "Hephaestus", x + 41, y + 7, GOLD, false);
    g.drawString(font, "LORD OF THE FORGE", x + 41, y + 20, MUTED, false);
    g.drawString(
        font,
        fit(
            tab.equals("synthesis")
                ? "Mastery · Awakening · Craftsmanship"
                : "Materials shape the masterpiece",
            w - 190),
        x + 183,
        y + 15,
        MUTED,
        false);
    int activeTab = List.of("forge", "arsenal", "opus", "synthesis").indexOf(tab);
    int tabWidth = (w - 28) / 4;
    g.fill(
        x + 8 + activeTab * (tabWidth + 4),
        y + 58,
        x + 8 + activeTab * (tabWidth + 4) + tabWidth,
        y + 60,
        GOLD);
    if (tab.equals("synthesis")) {
      g.fill(x + 10, y + 69, x + w - 10, y + h - 40, 0xff1d1d1c);
      g.drawString(font, "Combine mastered skills into one Ultimate", x + 21, y + 80, GOLD, false);
      gate(g, "Prospector fully mastered", state.getBoolean("prospector"), y + 106);
      gate(g, "Godly Craftsman fully mastered", state.getBoolean("craftsman"), y + 126);
      gate(g, "True Demon Lord or True Hero", state.getBoolean("awakened"), y + 146);
      gate(
          g,
          "Personally forge qualifying high-tier equipment",
          state.getBoolean("crafted"),
          y + 166);
      gate(g, "Awakening bonuses fully applied", state.getBoolean("settled"), y + 186);
      gate(
          g,
          String.format(
              Locale.ROOT, "Acquisition: %,.0f permanent magicules", state.getDouble("cost")),
          state.getBoolean("affordable"),
          y + 206);
      g.drawString(font, "No EP requirement.", x + 21, y + 229, GOLD, false);
      line(
          g,
          confirm
              ? "Confirm synthesis: both component skills will be replaced. Hephaestus starts"
                    + " learned, unmastered."
              : notice.isEmpty()
                  ? "The cost is paid once, after awakening. Stored ingredients and engraving"
                        + " knowledge are retained."
                  : notice,
          x + 21,
          y + 245,
          w - 42,
          confirm ? GOLD : MUTED);
      return;
    }
    g.fill(x + 8, y + 85, x + 210, y + 280, 0xff1b1b1b);
    g.fill(x + 215, y + 85, x + w - 8, y + 280, 0xff1d1d1c);
    g.drawCenteredString(
        font, (page + 1) + " / " + Math.max(1, (items().size() + 5) / 6), x + 108, y + 262, MUTED);
    var row = chosen();
    g.enableScissor(x + 219, y + 87, x + w - 11, y + 176);
    if (row != null) {
      g.renderItem(stack(row), x + 223, y + 91);
      g.drawString(font, fit(row.getString("name"), w - 269), x + 246, y + 96, GOLD, false);
      String info =
          tab.equals("arsenal")
              ? "Actual weapon, enchantments and engravings. 1,000 magicules to deploy each; 10/sec"
                    + " upkeep each. Up to six blades."
              : tab.equals("opus")
                  ? "One active masterpiece per smith. +50% item stats, 75% reduced wear. Replaces"
                        + " the ordinary forged bonuses. Cost: 50,000 MP + two selected materials;"
                        + " Diamond or better."
                  : "Forged: +20% attack / armor / toughness; 50% reduced wear. Reforge / temper:"
                        + " 2,000 MP + one material. Repair: 500 MP + one material.";
      line(g, info, x + 223, y + 115, w - 240, MUTED);
    } else line(g, "No matching equipment in your inventory.", x + 223, y + 102, w - 240, MUTED);
    g.disableScissor();
    if (!tab.equals("arsenal")) {
      var mats = state.getList("materials", Tag.TAG_COMPOUND);
      if (!mats.isEmpty()) {
        var m = mats.getCompound(material % mats.size());
        g.drawString(
            font,
            fit(
                m.getString("name")
                    + " · "
                    + m.getInt("count")
                    + " owned · "
                    + Math.round(m.getDouble("quality") * 100)
                    + "%",
                w - 240),
            x + 225,
            y + 184,
            GOLD,
            false);
      }
    }
    if (tab.equals("forge"))
      g.drawString(
          font,
          "Temper: " + new String[] {"", "Keen", "Piercing", "Conductor", "Enduring"}[temper],
          x + 223,
          y + 153,
          GOLD,
          false);
    if (tab.equals("opus") && confirm)
      line(
          g,
          "Press again to confirm. Your previous masterpiece loses its designation.",
          x + 223,
          y + 248,
          w - 240,
          GOLD);
    if (!notice.isEmpty())
      g.drawString(font, fit(notice, w - 231), x + 221, y + h - 23, GOLD, false);
  }

  private final class ScaledGraphics extends GuiGraphics {
    ScaledGraphics(GuiGraphics original) {
      super(Minecraft.getInstance(), original.bufferSource());
      pose().mulPose(original.pose().last().pose());
      pose().scale(uiScale, uiScale, 1);
    }

    @Override
    public void enableScissor(int a, int b, int c, int d) {
      super.enableScissor(
          (int) (a * uiScale),
          (int) (b * uiScale),
          (int) Math.ceil(c * uiScale),
          (int) Math.ceil(d * uiScale));
    }

    @Override
    public int guiWidth() {
      return (int) (ForgeScreen.this.width / uiScale);
    }

    @Override
    public int guiHeight() {
      return (int) (ForgeScreen.this.height / uiScale);
    }
  }

  @Override
  public void render(GuiGraphics original, int mouseX, int mouseY, float partial) {
    original.flush();
    var g = new ScaledGraphics(original);
    int mx = (int) (mouseX / uiScale), my = (int) (mouseY / uiScale);
    renderBackground(g, mx, my, partial);
    super.render(g, mx, my, partial);
    for (int n = 0; n < rows.size(); n++)
      if (rows.get(n).isHovered() && page * 6 + n < items().size())
        g.renderTooltip(font, stack(items().get(page * 6 + n)), mx, my);
    g.flush();
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    return super.mouseClicked(x / uiScale, y / uiScale, button);
  }

  @Override
  public boolean mouseReleased(double x, double y, int button) {
    return super.mouseReleased(x / uiScale, y / uiScale, button);
  }

  @Override
  public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
    return super.mouseDragged(x / uiScale, y / uiScale, button, dx / uiScale, dy / uiScale);
  }

  @Override
  public boolean mouseScrolled(double x, double y, double dx, double dy) {
    return super.mouseScrolled(x / uiScale, y / uiScale, dx, dy);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void onClose() {
    PacketDistributor.sendToServer(
        new ForgeNetwork.Request(state.getUUID("session"), "close", 0, 0));
    super.onClose();
  }
}
