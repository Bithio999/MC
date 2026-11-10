package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.forge.*;
import io.github.manasmods.tensura.client.screen.templates.SimpleScreen;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** A purpose-built Additions workshop, using Tensura's type, bevels and cyan panel language. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class ForgeScreen extends SimpleScreen {
  private static final int TEXT = 0xffe1eff1,
      MUTED = 0xff86a7af,
      CYAN = 0xff70bdcc,
      GOLD = 0xffe9be78,
      EDGE = 0xff305763,
      INK = 0xff0b1820,
      PANEL = 0xff102630;
  private static final String[] TEMPERS = {"", "Keen", "Piercing", "Conductor", "Enduring"};
  private CompoundTag state;
  private String tab, query = "", notice = "", dropdown = "", action = "temper", confirmation = "";
  private int x,
      y,
      w,
      h,
      leftW,
      rightX,
      rightW,
      bodyY,
      bodyBottom,
      rowY,
      rowHeight,
      rowCount,
      footerY;
  private int page, selected = -1, material = 1, temper = 1, age, noticeAge = 1000;
  private boolean compact, waiting, pending;
  private ForgeNetwork.Request queued;
  private EditBox search;
  private final Set<Integer> staged = new LinkedHashSet<>();
  private final List<WorkshopButton> controls = new ArrayList<>(),
      rows = new ArrayList<>(),
      rack = new ArrayList<>(),
      popup = new ArrayList<>();
  private int popX, popY, popW, popH;
  private String popTitle = "";

  public ForgeScreen(CompoundTag state) {
    super(Component.translatable("heavenlyrestriction.forge.title"), 374, 246);
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
      var ledger = tag.getCompound("ledger");
      for (String key : ledger.getAllKeys())
        try {
          ForgeNetwork.clientWorks.put(UUID.fromString(key), ledger.getUUID(key));
        } catch (IllegalArgumentException ignored) {
        }
    }
    if (tag.getBoolean("open")) {
      mc.setScreen(new ForgeScreen(tag));
      return;
    }
    if (!(mc.screen instanceof ForgeScreen s)
        || !tag.hasUUID("session")
        || !tag.getUUID("session").equals(s.state.getUUID("session"))) return;
    boolean learned = s.state.getBoolean("learned") != tag.getBoolean("learned");
    s.state = tag.copy();
    if (!tag.getString("message").isEmpty()) {
      s.notice = tag.getString("message");
      s.noticeAge = 0;
    }
    s.waiting = false;
    if (s.queued != null) {
      var q = s.queued;
      s.queued = null;
      s.send(q);
    } else s.pending = false;
    s.staged.removeIf(slot -> s.inventory().stream().noneMatch(r -> r.getInt("slot") == slot));
    if (learned) {
      s.tab = "forge";
      s.confirmation = "";
      s.dropdown = "";
      s.rebuildWidgets();
    }
    s.refresh();
  }

  private void request(String action) {
    request(action, selected);
  }

  private void request(String action, int slot) {
    if (pending) return;
    pending = !action.equals("refresh");
    int value = temper * 10 + material;
    if (action.equals("deploy_set")) {
      long mask = 0;
      for (int n : staged) if (n >= 0 && n < 36) mask |= 1L << n;
      slot = (int) mask;
      value = (int) (mask >>> 32);
    }
    var request = new ForgeNetwork.Request(state.getUUID("session"), action, slot, value);
    if (waiting) queued = request;
    else send(request);
    refresh();
  }

  private void send(ForgeNetwork.Request request) {
    waiting = true;
    PacketDistributor.sendToServer(request);
  }

  private List<CompoundTag> inventory() {
    var out = new ArrayList<CompoundTag>();
    for (var tag : state.getList("items", 10)) out.add((CompoundTag) tag);
    return out;
  }

  private List<CompoundTag> items() {
    return inventory().stream()
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

  private CompoundTag deployed(int index) {
    for (var t : state.getList("armaments", 10)) {
      var r = (CompoundTag) t;
      if (r.getInt("slot") == -index - 2) return r;
    }
    return null;
  }

  private int deployed() {
    return state.getList("armaments", 10).size();
  }

  private ItemStack stack(CompoundTag row) {
    return row == null
        ? ItemStack.EMPTY
        : ItemStack.parseOptional(minecraft.level.registryAccess(), row.getCompound("stack"));
  }

  private CompoundTag metal() {
    var mats = state.getList("materials", 10);
    return mats.isEmpty()
        ? new CompoundTag()
        : mats.getCompound(Math.floorMod(material, mats.size()));
  }

  private double price() {
    return tab.equals("opus")
        ? state.getDouble("opusCost")
        : state.getDouble(action.equals("repair") ? "repairCost" : "workCost");
  }

  private String amount(double n) {
    return String.format(Locale.ROOT, "%,.0f", n);
  }

  private String operation() {
    return action.equals("temper")
        ? "Temper · " + TEMPERS[temper]
        : action.equals("repair") ? "Repair" : "Reforge";
  }

  private String primaryCaption() {
    if (pending) return "Working…";
    return switch (tab) {
      case "arsenal" -> "Deploy selected (" + staged.size() + ")";
      case "opus" -> "Designate masterpiece";
      case "synthesis" -> "Synthesize Hephaestus";
      default -> operation();
    };
  }

  private boolean canWork() {
    var row = chosen();
    if (row == null
        || metal().getInt("count") < (tab.equals("opus") ? 2 : 1)
        || state.getDouble("magicules") < price()) return false;
    if (tab.equals("opus"))
      return row.getBoolean("own") && material != 0 && !row.getBoolean("opus");
    return action.equals("repair") || !ForgeItems.forged(stack(row)) || row.getBoolean("own");
  }

  private boolean canDeploy() {
    return !staged.isEmpty()
        && staged.size() + deployed() <= 6
        && state.getDouble("magicules") >= state.getDouble("deployCost") * staged.size();
  }

  private boolean modal() {
    return !dropdown.isEmpty() || !confirmation.isEmpty();
  }

  private void switchTab(String next) {
    if (tab.equals(next)) return;
    tab = next;
    page = 0;
    query = "";
    notice = "";
    clearOverlay();
    rebuildWidgets();
  }

  private class WorkshopButton extends SimpleButton {
    final String key;
    Supplier<String> caption;
    BooleanSupplier allowed = () -> true, chosen = () -> false;
    boolean primary, arrow;
    String lastHint = "";
    ItemStack icon = ItemStack.EMPTY;

    WorkshopButton(
        int bx, int by, int bw, int bh, String key, Supplier<String> caption, Runnable press) {
      super(
          bx,
          by,
          bw,
          bh,
          (net.minecraft.resources.ResourceLocation) null,
          Component.literal(key),
          Component.literal(key),
          b -> press.run());
      this.key = key;
      this.caption = caption;
      setActiveCheck(() -> active);
    }

    @Override
    public Component getMessage() {
      return Component.literal(key);
    }

    @Override
    public void updateWidgetNarration(
        net.minecraft.client.gui.narration.NarrationElementOutput output) {
      String spoken = caption.get();
      output.add(
          net.minecraft.client.gui.narration.NarratedElementType.TITLE,
          spoken.isBlank() || key.equals("Close") ? getMessage() : Component.literal(spoken));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
      boolean lit = chosen.getAsBoolean();
      panel(
          g,
          getX(),
          getY(),
          getWidth(),
          getHeight(),
          primary ? 0xff183942 : isHoveredOrFocused() ? 0xff1d3c46 : PANEL,
          lit || primary ? GOLD : isHoveredOrFocused() ? CYAN : EDGE);
      if (lit)
        g.fill(
            getX() + 3,
            getY() + getHeight() - 3,
            getX() + getWidth() - 3,
            getY() + getHeight() - 2,
            GOLD);
      int pad = icon.isEmpty() ? 7 : 26;
      if (!icon.isEmpty()) g.renderItem(icon, getX() + 5, getY() + (getHeight() - 16) / 2);
      int available = getWidth() - (icon.isEmpty() ? (arrow ? 24 : 8) : (arrow ? 43 : 33));
      String label = fit(caption.get(), available);
      if (arrow || !icon.isEmpty())
        g.drawString(
            font,
            label,
            getX() + pad,
            getY() + (getHeight() - 8) / 2,
            active ? TEXT : MUTED,
            false);
      else
        g.drawString(
            font,
            label,
            getX() + (getWidth() - font.width(label)) / 2,
            getY() + (getHeight() - 8) / 2,
            active ? (primary ? GOLD : TEXT) : MUTED,
            false);
      if (arrow)
        chevron(g, getX() + getWidth() - 11, getY() + getHeight() / 2, active ? CYAN : EDGE);
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

  private WorkshopButton button(
      int bx, int by, int bw, int bh, String key, Supplier<String> caption, Runnable press) {
    var b = addRenderableWidget(new WorkshopButton(x + bx, y + by, bw, bh, key, caption, press));
    controls.add(b);
    return b;
  }

  private WorkshopButton button(
      int bx, int by, int bw, String key, Supplier<String> caption, Runnable press) {
    return button(bx, by, bw, compact ? 17 : 20, key, caption, press);
  }

  private WorkshopButton button(int bx, int by, int bw, String key, Runnable press) {
    return button(bx, by, bw, compact ? 17 : 20, key, () -> key, press);
  }

  private void hint(AbstractWidget button, String text) {
    if (button instanceof WorkshopButton b) {
      if (b.lastHint.equals(text)) return;
      b.lastHint = text;
    }
    button.setTooltip(text.isEmpty() ? null : Tooltip.create(Component.literal(text)));
    button.setTooltipDelay(java.time.Duration.ofMillis(450));
  }

  @Override
  protected void init() {
    w = Math.min(374, width - 16);
    h = Math.min(246, height - 16);
    compact = h < 205;
    imageWidth = w;
    imageHeight = h;
    super.init();
    x = (width - w) / 2;
    y = (height - h) / 2;
    leftW = (int) ((w - 28) * .44);
    rightX = 18 + leftW;
    rightW = w - rightX - 10;
    bodyY = compact ? 52 : 66;
    bodyBottom = h - 33;
    footerY = h - 25;
    rowHeight = compact ? 18 : 22;
    rowY = bodyY + (compact ? 23 : 29);
    rowCount = Math.max(1, (bodyBottom - rowY - 2) / rowHeight);
    controls.clear();
    rows.clear();
    rack.clear();
    popup.clear();
    search = null;
    dropdown = "";
    confirmation = "";
    hint(
        button(w - 26, compact ? 7 : 10, 16, 16, "Close", () -> "×", this::onClose),
        "Close the workshop");
    String[] tabs = {"forge", "arsenal", "opus", "synthesis"},
        keys = {"Divine Forge", "Living Arsenal", "Magnum Opus", "Synthesis"},
        labels = {"Forge", "Arsenal", "Masterpiece", "Synthesis"};
    int tabW = (w - 32) / 4;
    for (int n = 0; n < 4; n++) {
      String next = tabs[n], label = labels[n];
      var b =
          button(
              10 + n * (tabW + 4),
              compact ? 30 : 40,
              tabW,
              compact ? 16 : 19,
              keys[n],
              () -> label,
              () -> switchTab(next));
      b.chosen = () -> tab.equals(next);
      b.allowed = () -> state.getBoolean("learned") || next.equals("synthesis");
    }
    if (!tab.equals("synthesis")) {
      search =
          addRenderableWidget(
              new EditBox(
                  font,
                  x + 17,
                  y + bodyY + 8,
                  leftW - 43,
                  9,
                  Component.literal("Search equipment")));
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
      for (int n = 0; n < rowCount; n++) {
        final int index = n;
        var row =
            new WorkshopButton(
                x + 14,
                y + rowY + n * rowHeight,
                leftW - 8,
                rowHeight - 2,
                "Equipment row " + n,
                () -> "",
                () -> select(index)) {
              @Override
              public Component getMessage() {
                int at = page * rowCount + index;
                return Component.literal(
                    at < items().size()
                        ? "Select " + items().get(at).getString("name")
                        : "Equipment row " + index);
              }

              @Override
              protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                int at = page * rowCount + index;
                if (at >= items().size()) return;
                var r = items().get(at);
                int slot = r.getInt("slot");
                boolean marked = tab.equals("arsenal") ? staged.contains(slot) : selected == slot;
                if (marked || isHoveredOrFocused())
                  panel(
                      g,
                      getX(),
                      getY(),
                      getWidth(),
                      getHeight(),
                      marked ? 0xff193640 : 0xff142f39,
                      marked ? GOLD : EDGE);
                g.renderItem(stack(r), getX() + 3, getY() + (getHeight() - 16) / 2);
                g.drawString(
                    font,
                    fit(r.getString("name"), getWidth() - (tab.equals("arsenal") ? 41 : 26)),
                    getX() + 23,
                    getY() + (getHeight() - 8) / 2,
                    marked ? GOLD : TEXT,
                    false);
                if (tab.equals("arsenal")) {
                  int xx = getX() + getWidth() - 13, yy = getY() + getHeight() / 2 - 4;
                  g.fill(xx, yy, xx + 8, yy + 8, marked ? GOLD : EDGE);
                  g.fill(xx + 1, yy + 1, xx + 7, yy + 7, INK);
                  if (marked) g.drawString(font, "✓", xx + 1, yy - 1, GOLD, false);
                }
              }
            };
        rows.add(addRenderableWidget(row));
      }
      var previous =
          button(
              10 + leftW - 40,
              footerY,
              18,
              compact ? 17 : 20,
              "Previous page",
              () -> "‹",
              () -> {
                page = Math.max(0, page - 1);
                refresh();
              });
      previous.allowed = () -> page > 0;
      var next =
          button(
              10 + leftW - 18,
              footerY,
              18,
              compact ? 17 : 20,
              "Next page",
              () -> "›",
              () -> {
                page = Math.min(pages() - 1, page + 1);
                refresh();
              });
      next.allowed = () -> page + 1 < pages();
      if (tab.equals("arsenal")) {
        button(
                    10,
                    footerY,
                    leftW - 45,
                    compact ? 17 : 20,
                    "Clear selection",
                    () -> "Clear (" + staged.size() + ")",
                    () -> {
                      staged.clear();
                      refresh();
                    })
                .allowed =
            () -> !staged.isEmpty();
        button(
                    rightX + rightW - 69,
                    bodyY + (compact ? 4 : 5),
                    61,
                    compact ? 13 : 16,
                    "Recall all",
                    () -> "Recall all",
                    () -> request("recall"))
                .allowed =
            () -> deployed() > 0;
        int cellW = (rightW - 24) / 3,
            cellH =
                Math.min(
                    compact ? 20 : 35,
                    (bodyBottom - bodyY - (compact ? 19 : 29) - (compact ? 13 : 27) - 4) / 2),
            gy = bodyY + (compact ? 19 : 29);
        for (int n = 0; n < 6; n++) {
          int index = n;
          var slot =
              new WorkshopButton(
                  x + rightX + 8 + n % 3 * (cellW + 4),
                  y + gy + n / 3 * (cellH + 4),
                  cellW,
                  cellH,
                  "Recall blade " + (n + 1),
                  () -> "",
                  () -> request("recall_one", -index - 2)) {
                @Override
                protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                  var item = stack(deployed(index));
                  panel(
                      g,
                      getX(),
                      getY(),
                      getWidth(),
                      getHeight(),
                      item.isEmpty() ? INK : PANEL,
                      item.isEmpty() ? EDGE : isHoveredOrFocused() ? GOLD : CYAN);
                  if (item.isEmpty())
                    g.drawCenteredString(
                        font, "·", getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, EDGE);
                  else
                    g.renderItem(
                        item, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2);
                  if (!compact)
                    g.drawString(
                        font, Integer.toString(index + 1), getX() + 4, getY() + 3, MUTED, false);
                }
              };
          rack.add(addRenderableWidget(slot));
        }

      } else {
        button(10, footerY, leftW - 45, "Utilities", () -> drop("utilities"));
        if (tab.equals("forge")) {
          int opY = bodyY + (compact ? 27 : 45), matY = opY + (compact ? 21 : 28);
          var op =
              button(
                  rightX + 8,
                  opY,
                  rightW - 16,
                  compact ? 17 : 20,
                  "Action",
                  this::operation,
                  () -> drop("action"));
          op.arrow = true;
          var materialButton =
              button(
                  rightX + 8,
                  matY,
                  rightW - 16,
                  compact ? 17 : 20,
                  "Material",
                  () -> metal().getString("name") + " · " + metal().getInt("count") + " owned",
                  () -> drop("metal"));
          materialButton.arrow = true;
        } else {
          var b =
              button(
                  rightX + 8,
                  bodyBottom - (compact ? 35 : 45),
                  rightW - 16,
                  "Material",
                  () -> metal().getString("name") + " · " + metal().getInt("count") + " owned",
                  () -> drop("metal"));
          b.arrow = true;
        }
      }
    }
    var main =
        button(
            rightX,
            footerY,
            rightW,
            compact ? 17 : 20,
            "Primary action",
            this::primaryCaption,
            () -> {
              if (tab.equals("opus") || tab.equals("synthesis")) confirm(tab);
              else request(tab.equals("arsenal") ? "deploy_set" : action);
            });
    main.primary = true;
    main.allowed =
        () ->
            switch (tab) {
              case "synthesis" -> state.getBoolean("ready");
              case "arsenal" -> canDeploy();
              default -> canWork();
            };

    refresh();
  }

  private void select(int index) {
    int at = page * rowCount + index;
    if (at >= items().size()) return;
    selected = items().get(at).getInt("slot");
    if (tab.equals("arsenal") && !staged.remove(selected) && staged.size() + deployed() < 6)
      staged.add(selected);
    refresh();
  }

  private int pages() {
    return Math.max(1, (items().size() + rowCount - 1) / rowCount);
  }

  private void refresh() {
    if (chosen() == null) selected = items().isEmpty() ? -1 : items().getFirst().getInt("slot");
    page = Math.clamp(page, 0, pages() - 1);
    for (int n = 0; n < rows.size(); n++) {
      var b = rows.get(n);
      b.visible = page * rowCount + n < items().size();
      b.active = !pending && !modal();
    }
    for (int n = 0; n < rack.size(); n++)
      rack.get(n).active = !pending && !modal() && deployed(n) != null;
    for (var b : controls) {
      b.active = !pending && !modal() && b.allowed.getAsBoolean();
      if (b.key.equals("Primary action")) hint(b, requirementHint());
      if (b.key.equals("Action"))
        hint(
            b,
            action.equals("temper") && chosen() != null
                ? ForgeItems.temperDescription(
                        stack(chosen()),
                        ForgeItems.Temper.values()[temper],
                        metal().getDouble("quality"))
                    .getString()
                : "");
    }
    if (search != null) {
      search.active = !modal();
      search.setEditable(!modal());
    }
    for (var b : popup) b.active = !pending && b.allowed.getAsBoolean();
  }

  private String requirementHint() {
    if (tab.equals("synthesis")) {
      if (!state.getBoolean("prospector")) return "Fully master Prospector.";
      if (!state.getBoolean("craftsman")) return "Fully master Godly Craftsman.";
      if (!state.getBoolean("awakened")) return "Become a True Demon Lord or True Hero.";
      if (!state.getBoolean("crafted"))
        return "Personally forge Netherite or Hihiirokane equipment. Tensura's Smithing Bench"
            + " counts.";
      if (!state.getBoolean("settled")) return "Finish awakening before synthesis.";
      return state.getBoolean("affordable")
          ? ""
          : "Requires " + amount(state.getDouble("cost")) + " permanent magicules.";
    }
    if (tab.equals("arsenal")) {
      if (staged.isEmpty()) return "Select inventory weapons using the checkboxes.";
      return canDeploy() ? "" : "Recall a blade to free space, or restore enough magicules.";
    }
    var row = chosen();
    if (row == null) return "Select equipment in the inventory list.";
    if (tab.equals("opus")) {
      if (!row.getBoolean("own"))
        return "A masterpiece must be equipment you personally forged. Unmarked equipment can be"
            + " reforged in the Forge tab.";
      if (row.getBoolean("opus")) return "This is already your active masterpiece.";
      if (material == 0) return "Choose Diamond, Netherite or Hihiirokane.";
    } else if (!action.equals("repair") && ForgeItems.forged(stack(row)) && !row.getBoolean("own"))
      return "Equipment signed by another smith cannot be reforged or tempered.";
    if (metal().getInt("count") < (tab.equals("opus") ? 2 : 1))
      return "Not enough " + metal().getString("name") + " in your inventory.";
    return state.getDouble("magicules") < price()
        ? "Requires "
            + amount(price())
            + " magicules. You have "
            + amount(state.getDouble("magicules"))
            + "."
        : "";
  }

  private void clearOverlay() {
    setFocused(null);
    for (var b : popup) removeWidget(b);
    popup.clear();
    dropdown = "";
    confirmation = "";
    refresh();
  }

  private WorkshopButton overlayButton(
      int px, int py, int pw, int ph, String key, Supplier<String> text, Runnable press) {
    var b = addRenderableWidget(new WorkshopButton(px, py, pw, ph, key, text, press));
    popup.add(b);
    return b;
  }

  private void drop(String name) {
    clearOverlay();
    dropdown = name;
    popTitle =
        switch (name) {
          case "utilities" -> "Workshop utilities";
          case "metal" -> "Choose material";
          default -> "Choose a forge action";
        };
    var names = new ArrayList<String>();
    var runs = new ArrayList<Runnable>();
    var icons = new ArrayList<ItemStack>();
    if (name.equals("utilities")) {
      names.addAll(List.of("Craft", "Refine", "Engrave", "Storage", "Recover ingredients"));
      Item[] items = {
        Items.CRAFTING_TABLE,
        Items.BLAST_FURNACE,
        Items.ENCHANTED_BOOK,
        Items.ENDER_CHEST,
        Items.BUNDLE
      };
      var actions = List.of("craft", "refine", "engrave", "storage", "recover");
      for (int n = 0; n < 5; n++) {
        String action = actions.get(n);
        runs.add(() -> request(action));
        icons.add(new ItemStack(items[n]));
      }
    } else if (name.equals("metal")) {
      var mats = state.getList("materials", 10);
      for (int n = 0; n < mats.size(); n++) {
        int pick = n;
        var m = mats.getCompound(n);
        names.add(m.getString("name") + " · " + m.getInt("count") + " owned");
        icons.add(stack(m));
        runs.add(() -> material = pick);
      }
    } else {
      names.add("Reforge");
      icons.add(new ItemStack(Items.ANVIL));
      runs.add(() -> action = "reforge");
      for (int n = 1; n <= 4; n++) {
        int pick = n;
        names.add("Temper · " + TEMPERS[n]);
        icons.add(ItemStack.EMPTY);
        runs.add(
            () -> {
              action = "temper";
              temper = pick;
            });
      }
      names.add("Repair");
      icons.add(new ItemStack(Items.IRON_INGOT));
      runs.add(() -> action = "repair");
    }
    int ph = compact ? 18 : 21;
    popW = Math.min(w - 24, 210);
    popH = names.size() * ph + 29;
    popX = x + (w - popW) / 2;
    popY = y + (h - popH) / 2;
    for (int n = 0; n < names.size(); n++) {
      var run = runs.get(n);
      String label = names.get(n);
      var b =
          overlayButton(
              popX + 7,
              popY + 22 + n * ph,
              popW - 14,
              ph - 1,
              label,
              () -> label,
              () -> {
                clearOverlay();
                run.run();
                refresh();
              });
      b.icon = icons.get(n);
      if (name.equals("action") && n >= 1 && n <= 4 && chosen() != null)
        hint(
            b,
            ForgeItems.temperDescription(
                    stack(chosen()), ForgeItems.Temper.values()[n], metal().getDouble("quality"))
                .getString());
      if (name.equals("metal")) {
        int pick = n;
        b.chosen = () -> material == pick;
        hint(
            b,
            Math.round(state.getList("materials", 10).getCompound(n).getDouble("quality") * 100)
                + "% temper strength. Masterpieces require Diamond or better.");
      }
    }
    setFocused(popup.getFirst());
    refresh();
  }

  private void confirm(String mode) {
    clearOverlay();
    confirmation = mode;
    popW = Math.min(w - 24, 278);
    popH = compact ? 125 : 142;
    popX = x + (w - popW) / 2;
    popY = y + (h - popH) / 2;
    var cancel =
        overlayButton(
            popX + 10,
            popY + popH - 28,
            (popW - 26) / 2,
            19,
            "Cancel",
            () -> "Cancel",
            this::clearOverlay);
    var commit =
        overlayButton(
            popX + 16 + (popW - 26) / 2,
            popY + popH - 28,
            (popW - 26) / 2,
            19,
            "Confirm",
            () -> "Confirm",
            () -> {
              clearOverlay();
              request(mode.equals("opus") ? "opus" : "synthesize");
            });
    commit.primary = true;
    commit.allowed = () -> mode.equals("opus") ? canWork() : state.getBoolean("ready");
    setFocused(cancel);
    refresh();
  }

  @Override
  public void tick() {
    noticeAge++;
    if (++age % 20 == 0 && !waiting && !pending) request("refresh");
  }

  private String fit(String text, int width) {
    return font.width(text) <= width
        ? text
        : font.plainSubstrByWidth(text, Math.max(0, width - 6)) + "…";
  }

  private void label(GuiGraphics g, String text, int px, int py, int color) {
    g.drawString(font, text, x + px, y + py, color, false);
  }

  private void wrap(
      GuiGraphics g, String text, int px, int py, int width, int maxLines, int color) {
    var lines = font.split(Component.literal(text), width);
    for (int n = 0; n < Math.min(maxLines, lines.size()); n++)
      g.drawString(font, lines.get(n), px, py + n * 10, color, false);
  }

  static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int edge) {
    g.fill(x + 2, y, x + w - 2, y + h, edge);
    g.fill(x, y + 2, x + w, y + h - 2, edge);
    g.fill(x + 2, y + 1, x + w - 2, y + h - 1, fill);
    g.fill(x + 1, y + 2, x + w - 1, y + h - 2, fill);
    g.fill(x + 3, y + 2, x + w - 3, y + 3, 0x2448a9c5);
  }

  private static void chevron(GuiGraphics g, int x, int y, int color) {
    g.fill(x, y - 1, x + 1, y, color);
    g.fill(x + 1, y, x + 2, y + 1, color);
    g.fill(x + 2, y + 1, x + 3, y + 2, color);
    g.fill(x + 3, y, x + 4, y + 1, color);
    g.fill(x + 4, y - 1, x + 5, y, color);
  }

  private void itemHeader(GuiGraphics g) {
    var row = chosen();
    int px = x + rightX + 8, py = y + bodyY + (compact ? 5 : 7);
    if (row == null) {
      wrap(g, "No matching equipment.", px, py + 4, rightW - 16, 2, MUTED);
      return;
    }
    int icon = compact ? 16 : 24;
    panel(g, px, py, icon + 4, icon + 4, INK, row.getBoolean("opus") ? GOLD : EDGE);
    g.pose().pushPose();
    g.pose().translate(px + 2, py + 2, 0);
    g.pose().scale(icon / 16f, icon / 16f, 1);
    g.renderItem(stack(row), 0, 0);
    g.pose().popPose();
    wrap(
        g,
        row.getString("name"),
        px + icon + 10,
        py + 3,
        rightW - icon - 29,
        compact ? 1 : 2,
        row.getBoolean("opus") ? GOLD : TEXT);
  }

  private void cost(GuiGraphics g, double cost, int quantity) {
    int px = x + rightX + 8, py = y + bodyBottom - 13;
    String text = amount(cost) + " MP" + (quantity > 0 ? " + " + quantity + "×" : "");
    g.drawString(font, text, px, py, state.getDouble("magicules") >= cost ? MUTED : GOLD, false);
    if (quantity > 0) {
      var icon = stack(metal());
      int xx = px + font.width(text) + 3;
      g.pose().pushPose();
      g.pose().translate(xx, py - 1, 0);
      g.pose().scale(.65f, .65f, 1);
      g.renderItem(icon, 0, 0);
      g.pose().popPose();
    }
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    g.fill(0, 0, width, height, 0x88040b11);
    panel(g, x - 2, y - 2, w + 4, h + 4, 0xff071119, 0xff173b49);
    panel(g, x, y, w, h, INK, CYAN);
    g.fill(x + 3, y + 3, x + w - 3, y + (compact ? 27 : 35), 0xff142b35);
    g.fill(x + 10, y + (compact ? 27 : 35), x + w - 10, y + (compact ? 28 : 36), EDGE);
    int icon = compact ? 18 : 24;
    g.blit(
        HeavenlyRestriction.id("textures/skill/hephaestus.png"),
        x + 10,
        y + 7,
        0,
        0,
        icon,
        icon,
        icon,
        icon);
    label(g, "HEPHAESTUS", compact ? 36 : 42, compact ? 7 : 8, TEXT);
    label(g, "TENSURA ADDITIONS", compact ? 36 : 42, compact ? 18 : 21, MUTED);
    if (w > 320) label(g, "FORGEWORKS", w - 103, 16, GOLD);
    // Small copper corner brackets are the Additions workshop's signature.
    for (int side = 0; side < 2; side++) {
      int xx = side == 0 ? x + 5 : x + w - 6;
      g.fill(xx, y + 5, xx + 1, y + 13, GOLD);
      g.fill(side == 0 ? xx : xx - 7, y + 5, side == 0 ? xx + 8 : xx + 1, y + 6, GOLD);
    }
    panel(g, x + 10, y + bodyY, leftW, bodyBottom - bodyY, 0xff0d1e27, EDGE);
    panel(g, x + rightX, y + bodyY, rightW, bodyBottom - bodyY, PANEL, EDGE);
    if (tab.equals("synthesis")) {
      synthesis(g);
      return;
    }
    panel(g, x + 14, y + bodyY + 4, leftW - 8, compact ? 16 : 19, INK, EDGE);
    String count = (page + 1) + "/" + pages();
    g.drawString(font, count, x + 10 + leftW - 7 - font.width(count), y + bodyY + 8, MUTED, false);
    if (items().isEmpty())
      wrap(
          g,
          tab.equals("arsenal")
              ? (deployed() > 0
                  ? "Recall a blade to put it back in your inventory."
                  : "No inventory weapons.")
              : "No equipment found.",
          x + 20,
          y + rowY + 8,
          leftW - 20,
          3,
          MUTED);
    if (tab.equals("arsenal")) {
      label(g, "ARSENAL", rightX + 8, bodyY + (compact ? 6 : 8), TEXT);
      if (!compact) label(g, "Click a blade to recall it", rightX + 8, bodyY + 19, MUTED);
      if (!staged.isEmpty()) cost(g, state.getDouble("deployCost") * staged.size(), 0);
      if (!compact || staged.isEmpty())
        label(
            g,
            deployed()
                + "/6 active · "
                + amount(deployed() * state.getDouble("upkeepCost"))
                + " MP/s",
            rightX + 8,
            bodyBottom - (staged.isEmpty() ? 13 : 26),
            MUTED);
    } else {
      itemHeader(g);
      if (tab.equals("forge")) {
        if (!compact && bodyBottom - bodyY > 125)
          label(g, "Forged: +20% stats · 50% less wear", rightX + 8, bodyBottom - 30, MUTED);
      } else {
        if (!compact) {
          label(g, "+50% item stats", rightX + 8, bodyY + 47, GOLD);
          label(g, "75% less wear", rightX + 8, bodyY + 60, GOLD);
        } else label(g, "+50% stats · -75% wear", rightX + 8, bodyY + 30, GOLD);
      }
      cost(g, price(), tab.equals("opus") ? 2 : 1);
    }
  }

  private void synthesis(GuiGraphics g) {
    String[] keys = {"prospector", "craftsman", "awakened", "crafted"},
        names =
            {
              "Master Prospector",
              "Master Godly Craftsman",
              "True awakening",
              "Forge high-tier gear"
            };
    int step = compact ? 15 : 29;
    for (int n = 0; n < 4; n++) {
      int yy = y + bodyY + 9 + n * step;
      boolean ready = state.getBoolean(keys[n]);
      g.drawString(font, ready ? "✓" : "○", x + 18, yy, ready ? 0xff92d6b4 : MUTED, false);
      wrap(
          g,
          compact
              ? new String[] {
                    "Prospector mastery", "Craftsman mastery", "True awakening", "High-tier forging"
                  }
                  [n]
              : names[n],
          x + 31,
          yy,
          leftW - 29,
          compact ? 1 : 2,
          ready ? TEXT : MUTED);
    }
    int xx = x + rightX + 9, yy = y + bodyY + 9;
    if (!compact) {
      g.blit(HeavenlyRestriction.id("textures/skill/hephaestus.png"), xx, yy, 0, 0, 32, 32, 32, 32);
      wrap(g, "Lord of the Forge", xx + 41, yy + 5, rightW - 61, 2, GOLD);
      wrap(
          g,
          "Combine both skills. Keep stored items and engraving knowledge.",
          xx,
          yy + 43,
          rightW - 18,
          3,
          TEXT);
    } else wrap(g, "Combine both skills. Storage is retained.", xx, yy, rightW - 18, 3, TEXT);
    String price = amount(state.getDouble("cost"));
    wrap(
        g,
        price + " permanent MP",
        xx,
        y + bodyBottom - (compact ? 27 : 30),
        rightW - 18,
        2,
        state.getBoolean("affordable") ? GOLD : MUTED);
    if (!compact) label(g, "No EP requirement", rightX + 9, bodyBottom - 12, MUTED);
  }

  @Override
  public void renderWidgets(GuiGraphics g, int mx, int my, float pt) {
    // Keep popovers above the dim layer and all underlying widgets inert.
    for (var widget : renderables)
      if (!popup.contains(widget)) {
        if (modal() && widget instanceof AbstractWidget underlying) {
          var savedTooltip = underlying.getTooltip();
          underlying.setTooltip(null);
          underlying.render(g, -10000, -10000, pt);
          underlying.setTooltip(savedTooltip);
        } else widget.render(g, mx, my, pt);
      }
    if (modal()) {
      g.pose().pushPose();
      g.pose().translate(0, 0, 200);
      g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xc0061018);
      panel(g, popX, popY, popW, popH, PANEL, GOLD);
      if (!confirmation.isEmpty()) {
        g.drawString(
            font,
            confirmation.equals("opus") ? "Designate this masterpiece?" : "Synthesize Hephaestus?",
            popX + 10,
            popY + 10,
            GOLD,
            false);
        String message =
            confirmation.equals("opus")
                ? "Your previous masterpiece loses its designation. This piece gains +50% stats and"
                    + " 75% less wear."
                : "Prospector and Godly Craftsman will be replaced. Stored items and engraving"
                    + " knowledge are kept.";
        wrap(g, message, popX + 10, popY + 28, popW - 20, compact ? 4 : 5, TEXT);
        String price =
            confirmation.equals("opus")
                ? amount(price()) + " MP + 2 " + metal().getString("name")
                : amount(state.getDouble("cost")) + " permanent MP";
        g.drawString(font, fit(price, popW - 20), popX + 10, popY + popH - 43, MUTED, false);
      } else g.drawString(font, popTitle, popX + 10, popY + 8, GOLD, false);
      for (var widget : popup) widget.render(g, mx, my, pt);
      g.pose().popPose();
    } else if (!notice.isEmpty() && noticeAge < 65) {
      g.pose().pushPose();
      g.pose().translate(0, 0, 200);
      var lines = font.split(Component.literal(notice), w - 40);
      int height = Math.min(2, lines.size()) * 10 + 12;
      panel(g, x + 13, y + footerY - height - 5, w - 26, height, 0xff152e35, CYAN);
      for (int n = 0; n < Math.min(2, lines.size()); n++)
        g.drawString(font, lines.get(n), x + 21, y + footerY - height + 1 + n * 10, TEXT, false);
      g.pose().popPose();
    }
  }

  @Override
  public void renderTooltip(GuiGraphics g, int mx, int my) {
    if (modal()) return;
    for (int n = 0; n < rows.size(); n++)
      if (rows.get(n).isHovered() && page * rowCount + n < items().size()) {
        g.renderTooltip(font, stack(items().get(page * rowCount + n)), mx, my);
        return;
      }
    for (int n = 0; n < rack.size(); n++)
      if (rack.get(n).isHovered() && deployed(n) != null) {
        g.renderTooltip(font, stack(deployed(n)), mx, my);
        return;
      }
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (modal()) {
      for (int n = popup.size() - 1; n >= 0; n--)
        if (popup.get(n).mouseClicked(mx, my, button)) return true;
      if (mx < popX || mx >= popX + popW || my < popY || my >= popY + popH) clearOverlay();
      return true;
    }
    return super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    if (modal()) return true;
    if (!tab.equals("synthesis")
        && mx >= x + 10
        && mx < x + 10 + leftW
        && my >= y + bodyY
        && my < y + bodyBottom) {
      page = Math.clamp(page + (dy < 0 ? 1 : -1), 0, pages() - 1);
      refresh();
      return true;
    }
    return super.mouseScrolled(mx, my, dx, dy);
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (key == GLFW.GLFW_KEY_ESCAPE && modal()) {
      clearOverlay();
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }

  @Override
  public void onClose() {
    PacketDistributor.sendToServer(
        new ForgeNetwork.Request(state.getUUID("session"), "close", 0, 0));
    super.onClose();
  }
}
