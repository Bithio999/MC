package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.forge.ForgeNetwork;
import io.github.manasmods.tensura.client.screen.templates.SimpleScreen;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import io.github.manasmods.tensura.util.client.ScreenHelper;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The native Tensura frame, search, list rows and side tabs, with server-owned item actions. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class ForgeScreen extends SimpleScreen {
  private static final ResourceLocation
      FRAME =
          ResourceLocation.parse(
              "tensura:textures/gui/ability_selection/ability_selection_gui.png"),
      ROW = ResourceLocation.parse("tensura:textures/gui/ability_button.png");
  private static final int TEXT = 0xffdceff2, MUTED = 0xff91b4bd, GOLD = 0xffffd277;
  private CompoundTag state;
  private String tab, query = "", notice = "", dropdown = "", action = "temper";
  private int detailScroll;
  private int page, selected = -1, material = 1, temper = 1, age;
  private boolean waiting, pending, confirm;
  private ForgeNetwork.Request queued;
  private final Set<Integer> staged = new LinkedHashSet<>();
  private final List<ForgeButton> rows = new ArrayList<>(), controls = new ArrayList<>();
  private final List<AbstractWidget> popup = new ArrayList<>();

  public ForgeScreen(CompoundTag state) {
    super(Component.translatable("heavenlyrestriction.forge.title"), 254, 163);
    this.state = state.copy();
    tab = state.getString("mode");
    if (tab.equals("masterpiece")) tab = "opus";
    if (!state.getBoolean("learned")) tab = "synthesis";
  }

  public float uiScale() {
    return 1;
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(() -> ForgeNetwork.receiver = ForgeScreen::receive);
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
    s.staged.removeIf(slot -> s.inventory().stream().noneMatch(row -> row.getInt("slot") == slot));
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
    int slot = selected, value = temper * 10 + material;
    if (action.equals("deploy_set")) {
      long mask = 0;
      for (int n : staged) if (n >= 0 && n < 36) mask |= 1L << n;
      slot = (int) mask;
      value = (int) (mask >>> 32);
    }
    var r = new ForgeNetwork.Request(state.getUUID("session"), action, slot, value);
    if (waiting) queued = r;
    else send(r);
    refresh();
  }

  private void send(ForgeNetwork.Request r) {
    waiting = true;
    PacketDistributor.sendToServer(r);
  }

  private class ForgeButton extends SimpleButton {
    private final String label;
    private final ItemStack icon;
    private java.util.function.BooleanSupplier allowed = () -> true;

    ForgeButton(
        int x,
        int y,
        int width,
        int height,
        ResourceLocation texture,
        String label,
        ItemStack icon,
        Runnable call) {
      super(
          x,
          y,
          width,
          height,
          texture,
          icon.isEmpty() ? Component.literal(label) : Component.empty(),
          Component.literal(label),
          b -> call.run());
      this.label = label;
      this.icon = icon;
      setActiveCheck(() -> active);
      setClickedCheck(() -> active && visible && isHovered());
    }

    @Override
    public Component getMessage() {
      return Component.literal(label);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      var saved = text;
      text = null;
      super.renderWidget(g, mx, my, partial);
      text = saved;
      if (!icon.isEmpty())
        g.renderItem(icon, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2);
      else {
        g.blit(
            ROW, getX(), getY(), getWidth(), getHeight(), 0, isHovered() ? 13 : 0, 89, 13, 89, 26);
        String caption =
            switch (label) {
              case "Synthesize Hephaestus" -> confirm ? "Confirm synthesis" : "Synthesize";
              case "Designate masterpiece" -> confirm ? "Confirm masterpiece" : "Designate";
              case "Temper" -> "Type";
              case "Deploy selected" -> "Deploy (" + staged.size() + ")";
              case "‹ page" -> "‹";
              case "› page" -> "›";
              case "Action" ->
                  action.substring(0, 1).toUpperCase(Locale.ROOT) + action.substring(1);
              default -> label;
            };
        g.drawCenteredString(
            font,
            fit(caption, getWidth() - 4),
            getX() + getWidth() / 2,
            getY() + 3,
            active ? TEXT : MUTED);
      }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
      if (active && visible && button == 0 && isMouseOver(mx, my)) {
        onPress();
        playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
      }
      return false;
    }
  }

  private ForgeButton button(int x, int y, int width, String label, Runnable call) {
    var b =
        addRenderableWidget(
            new ForgeButton(
                guiLeft + x, guiTop + y, width, 13, null, label, ItemStack.EMPTY, call));
    controls.add(b);
    return b;
  }

  private List<CompoundTag> inventory() {
    var out = new ArrayList<CompoundTag>();
    for (var t : state.getList("items", Tag.TAG_COMPOUND)) out.add((CompoundTag) t);
    return out;
  }

  private List<CompoundTag> items() {
    var out = new ArrayList<CompoundTag>();
    if (tab.equals("arsenal"))
      for (var t : state.getList("armaments", Tag.TAG_COMPOUND)) out.add((CompoundTag) t);
    out.addAll(inventory());
    return out.stream()
        .filter(
            r ->
                (!tab.equals("arsenal") || r.getBoolean("weapon"))
                    && r.getString("name")
                        .toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT)))
        .toList();
  }

  private CompoundTag chosen() {
    return items().stream().filter(r -> r.getInt("slot") == selected).findFirst().orElse(null);
  }

  private ItemStack stack(CompoundTag row) {
    return row == null
        ? ItemStack.EMPTY
        : ItemStack.parseOptional(minecraft.level.registryAccess(), row.getCompound("stack"));
  }

  private void switchTab(String next) {
    tab = next;
    detailScroll = 0;
    page = 0;
    query = "";
    dropdown = "";
    confirm = false;
    notice = "";
    rebuildWidgets();
  }

  @Override
  protected void init() {
    super.init();
    controls.clear();
    rows.clear();
    popup.clear();
    for (var nativeTab : ScreenHelper.getMenuTabs(0, guiLeft, guiTop))
      if (nativeTab.getY() == guiTop + 2) addRenderableWidget(nativeTab);
    String[] tabs = {"forge", "arsenal", "opus", "synthesis"},
        labels = {"Divine Forge", "Living Arsenal", "Magnum Opus", "Synthesis"};
    Item[] icons = {Items.ANVIL, Items.DIAMOND_SWORD, Items.NETHER_STAR, Items.END_CRYSTAL};
    for (int n = 0; n < 4; n++) {
      String next = tabs[n];
      var b =
          addRenderableWidget(
              new ForgeButton(
                  guiLeft + 233,
                  guiTop + 30 + n * 26,
                  21,
                  24,
                  ScreenHelper.getTabTexture(5),
                  labels[n],
                  new ItemStack(icons[n]),
                  () -> switchTab(next)));
      b.setUvOffsetCheck(() -> tab.equals(next) || b.isHovered());
      b.allowed = () -> state.getBoolean("learned") || next.equals("synthesis");
      controls.add(b);
    }
    button(5, 28, 18, "‹", this::onClose);
    if (tab.equals("synthesis")) {
      button(
                  116,
                  142,
                  111,
                  "Synthesize Hephaestus",
                  () -> {
                    if (!confirm) {
                      confirm = true;
                      return;
                    }
                    confirm = false;
                    request("synthesize");
                  })
              .allowed =
          () -> state.getBoolean("ready");
      refresh();
      return;
    }
    var search =
        addRenderableWidget(
            new EditBox(
                font, guiLeft + 28, guiTop + 31, 78, 9, Component.literal("Search equipment")));
    search.setBordered(false);
    search.setMaxLength(60);
    search.setHint(Component.literal("Search…"));
    search.setValue(query);
    search.setResponder(
        q -> {
          query = q;
          page = 0;
          refresh();
        });
    for (int n = 0; n < 7; n++) {
      int index = n;
      var row =
          new ForgeButton(
              guiLeft + 6,
              guiTop + 46 + n * 13,
              89,
              13,
              ROW,
              "",
              ItemStack.EMPTY,
              () -> {
                int at = page * 7 + index;
                if (at >= items().size()) return;
                selected = items().get(at).getInt("slot");
                confirm = false;
                if (tab.equals("arsenal") && selected >= 0) {
                  if (!staged.remove(selected)
                      && staged.size() + state.getList("armaments", 10).size() < 6)
                    staged.add(selected);
                }
                refresh();
              }) {
            @Override
            protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
              int at = page * 7 + index;
              if (at >= items().size()) return;
              var row = items().get(at);
              int slot = row.getInt("slot");
              g.blit(
                  ROW, getX(), getY(), 0, slot == selected || isHovered() ? 13 : 0, 89, 13, 89, 26);
              g.pose().pushPose();
              g.pose().translate(getX() + 1, getY() + 1, 0);
              g.pose().scale(.65f, .65f, 1);
              g.renderItem(stack(row), 0, 0);
              g.pose().popPose();
              String prefix =
                  tab.equals("arsenal")
                      ? (slot < 0 ? "↑ " : staged.contains(slot) ? "✓ " : "+ ")
                      : "";
              g.drawString(
                  font,
                  fit(prefix + row.getString("name"), 73),
                  getX() + 13,
                  getY() + 3,
                  slot < 0 ? GOLD : TEXT,
                  false);
            }
          };
      rows.add(addRenderableWidget(row));
    }
    button(
        6,
        145,
        17,
        "‹ page",
        () -> {
          page = Math.max(0, page - 1);
          refresh();
        });
    button(
        78,
        145,
        17,
        "› page",
        () -> {
          page = Math.min(Math.max(0, (items().size() - 1) / 7), page + 1);
          refresh();
        });
    button(116, 30, 37, "Tools", () -> drop("tools"));
    if (tab.equals("forge")) {
      button(156, 30, 34, "Metal", () -> drop("metal"));
      button(193, 30, 34, "Temper", () -> drop("temper"));
      button(116, 142, 54, "Action", () -> drop("action"));
      button(173, 142, 54, "Apply", () -> request(action)).allowed = () -> chosen() != null;
    } else if (tab.equals("opus")) {
      button(156, 30, 71, "Material", () -> drop("metal"));
      button(
                  116,
                  142,
                  111,
                  "Designate masterpiece",
                  () -> {
                    if (!confirm) {
                      confirm = true;
                      return;
                    }
                    confirm = false;
                    request("opus");
                  })
              .allowed =
          () -> chosen() != null;
    } else {
      button(116, 142, 64, "Deploy selected", () -> request("deploy_set")).allowed =
          () -> !staged.isEmpty();
      button(183, 142, 44, "Recall", () -> request("recall_one")).allowed = () -> selected <= -2;
    }
    refresh();
  }

  private void drop(String name) {
    boolean close = dropdown.equals(name);
    for (var w : popup) removeWidget(w);
    popup.clear();
    dropdown = close ? "" : name;
    if (close) return;
    var labels = new ArrayList<String>();
    var actions = new ArrayList<Runnable>();
    if (name.equals("tools")) {
      if (tab.equals("arsenal")) {
        labels.addAll(List.of("Clear selection", "Recall all"));
        actions.add(
            () -> {
              staged.clear();
              refresh();
            });
        actions.add(() -> request("recall"));
      } else {
        labels.addAll(List.of("Craft", "Refine", "Engrave", "Storage", "Recover"));
        for (String a : List.of("craft", "refine", "engrave", "storage", "recover"))
          actions.add(() -> request(a));
      }
    } else if (name.equals("metal")) {
      var mats = state.getList("materials", 10);
      for (int n = 0; n < mats.size(); n++) {
        int pick = n;
        labels.add(mats.getCompound(n).getString("name"));
        actions.add(
            () -> {
              material = pick;
              confirm = false;
            });
      }
    } else if (name.equals("temper")) {
      for (int n = 0; n < 4; n++) {
        int pick = n + 1;
        labels.add(new String[] {"Keen", "Piercing", "Conductor", "Enduring"}[n]);
        actions.add(
            () -> {
              temper = pick;
              confirm = false;
            });
      }
    } else {
      for (String a : List.of("reforge", "temper", "repair")) {
        labels.add(a.substring(0, 1).toUpperCase(Locale.ROOT) + a.substring(1));
        actions.add(() -> action = a);
      }
    }
    int by = name.equals("action") ? 142 - labels.size() * 13 : 44;
    for (int n = 0; n < labels.size(); n++) {
      var run = actions.get(n);
      var b =
          new ForgeButton(
              guiLeft + 116,
              guiTop + by + n * 13,
              111,
              13,
              ROW,
              labels.get(n),
              ItemStack.EMPTY,
              () -> {
                run.run();
                drop(name);
              });
      addRenderableWidget(b);
      popup.add(b);
    }
  }

  private void refresh() {
    if (chosen() == null) selected = items().isEmpty() ? -1 : items().getFirst().getInt("slot");
    page = Math.min(page, Math.max(0, (items().size() - 1) / 7));
    for (int n = 0; n < rows.size(); n++) rows.get(n).visible = page * 7 + n < items().size();
    for (var b : controls) b.active = !pending && b.allowed.getAsBoolean();
    for (var b : popup) b.active = !pending;
  }

  @Override
  public void tick() {
    if (++age % 20 == 0 && !waiting && !pending) request("refresh");
  }

  private String fit(String s, int w) {
    return font.width(s) <= w ? s : font.plainSubstrByWidth(s, Math.max(0, w - 6)) + "…";
  }

  private void text(GuiGraphics g, String s, int x, int y, int width, int color) {
    g.drawWordWrap(font, Component.literal(s), guiLeft + x, guiTop + y, width, color);
  }

  private String material() {
    var list = state.getList("materials", 10);
    if (list.isEmpty()) return "";
    var r = list.getCompound(material % list.size());
    return r.getString("name") + " ×" + r.getInt("count");
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    g.blit(FRAME, guiLeft, guiTop, 0, 0, 254, 163);
    g.drawString(font, "Hephaestus", guiLeft + 105, guiTop + 10, TEXT, false);
    // Clear the baked preset boxes and unused cooldown/mode decorations of the native frame.
    g.fill(guiLeft + 5, guiTop + 145, guiLeft + 110, guiTop + 159, 0xff33343a);
    g.fill(guiLeft + 116, guiTop + 28, guiLeft + 154, guiTop + 68, 0xff4c4c4c);
    g.fill(guiLeft + 189, guiTop + 28, guiLeft + 228, guiTop + 68, 0xff4c4c4c);
    if (tab.equals("synthesis")) {
      g.blit(
          HeavenlyRestriction.id("textures/skill/hephaestus.png"),
          guiLeft + 155,
          guiTop + 30,
          0,
          0,
          32,
          32,
          32,
          32);
      String[] keys = {"prospector", "craftsman", "awakened", "crafted", "settled", "affordable"},
          names =
              {
                "Prospector mastery",
                "Craftsman mastery",
                "True awakening",
                "High-tier forging",
                "Awakening settled",
                "Magicules available"
              };
      for (int n = 0; n < keys.length; n++)
        g.drawString(
            font,
            fit((state.getBoolean(keys[n]) ? "✓ " : "• ") + names[n], 85),
            guiLeft + 6,
            guiTop + 48 + n * 14,
            state.getBoolean(keys[n]) ? 0xff88dfbd : MUTED,
            false);
      g.enableScissor(guiLeft + 121, guiTop + 71, guiLeft + 222, guiTop + 130);
      detail(
          g,
          confirm
              ? "Confirm replacement of both mastered skills. Ingredients and knowledge are kept."
              : "Cost: "
                  + String.format(Locale.ROOT, "%,.0f", state.getDouble("cost"))
                  + " permanent MP. Paid after awakening. No EP gate.",
          confirm ? GOLD : TEXT);
      g.disableScissor();
      return;
    }
    g.drawCenteredString(
        font,
        (page + 1) + " / " + Math.max(1, (items().size() + 6) / 7),
        guiLeft + 51,
        guiTop + 147,
        MUTED);
    var row = chosen();
    if (row != null) {
      g.renderItem(stack(row), guiLeft + 163, guiTop + 42);
    }
    String detail =
        tab.equals("arsenal")
            ? state.getList("armaments", 10).size()
                + "/6 deployed · "
                + staged.size()
                + " selected. Click + to select. 1,000 MP each; 10 MP/sec each. 24-tick round."
            : tab.equals("opus")
                ? "+50% item stats; 75% less wear. One masterpiece. 50,000 MP +2 "
                    + material()
                    + "."
                : (action.equals("repair")
                        ? "Repair: 500 MP +1 "
                        : action.equals("reforge")
                            ? "Reforge: 2,000 MP +1 "
                            : "Temper: 2,000 MP +1 ")
                    + material()
                    + ". +20% forged stats; 50% less wear. "
                    + new String[] {"", "Keen", "Piercing", "Conductor", "Enduring"}[temper]
                    + ".";
    if (confirm) detail = "Press again to confirm. Previous masterpiece loses its designation.";
    g.enableScissor(guiLeft + 121, guiTop + 71, guiLeft + 222, guiTop + 130);
    detail(g, detail, confirm ? GOLD : TEXT);
    g.disableScissor();
    if (!notice.isEmpty()) {
      /* Long action feedback belongs in a hover tooltip, not across other panes. */
      g.drawString(font, fit(notice, 108), guiLeft + 116, guiTop + 134, GOLD, false);
    }
  }

  private void detail(GuiGraphics g, String text, int color) {
    var lines = font.split(Component.literal(text), 98);
    detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() - 6));
    for (int n = 0; n < 6 && n + detailScroll < lines.size(); n++)
      g.drawString(
          font, lines.get(n + detailScroll), guiLeft + 122, guiTop + 72 + n * 9, color, false);
    if (lines.size() > 6) {
      g.fill(guiLeft + 220, guiTop + 72, guiLeft + 222, guiTop + 127, 0xff173c48);
      int top = guiTop + 72 + (int) (detailScroll / (double) (lines.size() - 6) * 39);
      g.fill(guiLeft + 220, top, guiLeft + 222, top + 16, 0xff6b9ca6);
    }
  }

  @Override
  public void renderTooltip(GuiGraphics g, int mx, int my) {
    if (!dropdown.isEmpty()) return;
    for (int n = 0; n < rows.size(); n++)
      if (rows.get(n).isHovered() && page * 7 + n < items().size())
        g.renderTooltip(font, stack(items().get(page * 7 + n)), mx, my);
    if (!notice.isEmpty()
        && mx >= guiLeft + 116
        && mx < guiLeft + 227
        && my >= guiTop + 132
        && my < guiTop + 141)
      g.renderTooltip(font, font.split(Component.literal(notice), 180), mx, my);
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (!dropdown.isEmpty()) {
      for (int n = popup.size() - 1; n >= 0; n--)
        if (popup.get(n).mouseClicked(mx, my, button)) return true;
      drop(dropdown);
      return true;
    }
    return super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    if (mx >= guiLeft + 5 && mx <= guiLeft + 109 && my >= guiTop + 45 && my <= guiTop + 139) {
      page = Math.clamp(page + (dy < 0 ? 1 : -1), 0, Math.max(0, (items().size() - 1) / 7));
      refresh();
      return true;
    }
    if (mx >= guiLeft + 119 && mx <= guiLeft + 224 && my >= guiTop + 69 && my <= guiTop + 132) {
      detailScroll = Math.max(0, detailScroll + (dy < 0 ? 1 : -1));
      return true;
    }
    return super.mouseScrolled(mx, my, dx, dy);
  }

  @Override
  public void onClose() {
    PacketDistributor.sendToServer(
        new ForgeNetwork.Request(state.getUUID("session"), "close", 0, 0));
    super.onClose();
  }
}
