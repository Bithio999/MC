package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.laplace.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** A single focused equation, with progressive disclosure instead of a wall of controls. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class LaplaceScreen extends Screen {
  private static final int TEXT = 0xffdeedf2,
      MUTED = 0xff93adba,
      CYAN = 0xff88deec,
      GOLD = 0xffedce92,
      EDGE = 0xff345460;
  private CompoundTag state;
  private String selected = "",
      aspect = "",
      query = "",
      notice = "",
      page = "actions",
      confirm = "";
  private LinkedHashMap<String, String> draft = new LinkedHashMap<>();
  private boolean enabled, dirty, pending;
  private int x, y, w, h, top, bottom, scroll, caret = -1;
  private EditBox input, search;
  private Button save;
  private String error = "";
  private EquationModel.Result result;
  private final List<Line> lines = new ArrayList<>();

  private record Line(String text, int x, int y, int color) {}

  public LaplaceScreen(CompoundTag t) {
    super(Component.literal("Laplace · Lord of Equations"));
    state = t.copy();
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(() -> LaplaceNetwork.receiver = LaplaceScreen::receive);
  }

  public static void receive(CompoundTag t) {
    var mc = Minecraft.getInstance();
    if (t.getBoolean("open") && t.getBoolean("allowed")) {
      mc.setScreen(new LaplaceScreen(t));
      return;
    }
    if (mc.screen instanceof LaplaceScreen s
        && t.hasUUID("session")
        && t.getUUID("session").equals(s.state.getUUID("session"))) {
      if (!t.getBoolean("allowed")) {
        mc.setScreen(null);
        return;
      }
      s.state = t.copy();
      s.pending = false;
      s.notice = t.getString("notice");
      if (s.notice.startsWith("Saved.")) {
        s.dirty = false;
        s.load(s.selected);
      }
      s.confirm = s.notice.isEmpty() ? "" : "notice";
      s.rebuildWidgets();
    }
  }

  private List<CompoundTag> rows() {
    return state.getList("rows", 10).stream().map(t -> (CompoundTag) t).toList();
  }

  private CompoundTag chosen() {
    return rows().stream()
        .filter(t -> t.getString("key").equals(selected))
        .findFirst()
        .orElse(new CompoundTag());
  }

  private EquationModel model() {
    return LaplaceNetwork.decode(chosen().getCompound("model"));
  }

  private void load(String key) {
    selected = key;
    caret = -1;
    var row = chosen();
    draft.clear();
    if (!row.isEmpty()) {
      var m = LaplaceNetwork.decode(row.getCompound("model"));
      var eq = row.getCompound("equations");
      for (String k : m.defaults().keySet()) draft.put(k, eq.getString(k));
      eq.getAllKeys().stream()
          .sorted()
          .filter(k -> !draft.containsKey(k))
          .forEach(k -> draft.put(k, eq.getString(k)));
      enabled = row.getBoolean("enabled");
      if (!draft.containsKey(aspect)) aspect = draft.keySet().iterator().next();
    }
    dirty = false;
  }

  private void send(String op) {
    if (pending) return;
    pending = true;
    PacketDistributor.sendToServer(
        new LaplaceNetwork.Request(
            state.getUUID("session"), op, selected, enabled, new LinkedHashMap<>(draft)));
  }

  private void text(String s, int xx, int yy, int color) {
    lines.add(new Line(s, xx, yy, color));
  }

  private String fit(String s, int width) {
    return font.width(s) <= width ? s : font.plainSubstrByWidth(s, Math.max(0, width - 9)) + "…";
  }

  private Button button(String label, int xx, int yy, int ww, int hh, Runnable action) {
    var b = new FormulaButton(xx, yy, ww, hh, label, action);
    addRenderableWidget(b);
    return b;
  }

  private void tip(AbstractWidget w, String s) {
    w.setTooltip(Tooltip.create(Component.literal(s)));
  }

  @Override
  protected void init() {
    x = Math.max(8, (width - 660) / 2);
    y = Math.max(8, (height - 420) / 2);
    w = width - 2 * x;
    h = Math.min(420, height - 2 * y);
    top = y + 62;
    bottom = y + h - 29;
    build();
  }

  private void build() {
    lines.clear();
    input = null;
    save = null;
    search = null;
    button("×", x + w - 25, y + 8, 18, 18, this::onClose);
    if (!state.getBoolean("learned")) {
      synthesis();
      return;
    }
    int tw = (w - 20) / 3;
    button(
        "1  Actions",
        x + 8,
        y + 34,
        tw,
        20,
        () -> {
          page = "actions";
          confirm = "";
          scroll = 0;
          rebuildWidgets();
        });
    button(
                "2  Equation" + (dirty ? " *" : ""),
                x + 10 + tw,
                y + 34,
                tw,
                20,
                () -> {
                  if (!selected.isEmpty()) {
                    page = "edit";
                    confirm = "";
                    rebuildWidgets();
                  }
                })
            .active =
        !selected.isEmpty();
    button(
                "3  Parameters",
                x + 12 + 2 * tw,
                y + 34,
                tw,
                20,
                () -> {
                  if (!selected.isEmpty()) {
                    if (input != null) caret = input.getCursorPosition();
                    page = "reference";
                    confirm = "";
                    scroll = 0;
                    rebuildWidgets();
                  }
                })
            .active =
        !selected.isEmpty();
    if (!confirm.isEmpty()) {
      confirmation();
      return;
    }
    switch (page) {
      case "edit" -> editor();
      case "reference" -> reference();
      case "aspects" -> aspects();
      default -> actions();
    }
  }

  private void actions() {
    search = new EditBox(font, x + 12, top, w - 24, 18, Component.literal("Search actions"));
    search.setMaxLength(64);
    search.setHint(Component.literal("Search learned actions…"));
    search.setValue(query);
    search.setResponder(
        s -> {
          query = s;
          scroll = 0;
          rebuildWidgets();
          if (search != null) {
            setFocused(search);
            search.setCursorPosition(s.length());
          }
        });
    addRenderableWidget(search);
    var list =
        rows().stream()
            .filter(
                t ->
                    t.getString("title")
                        .toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT)))
            .toList();
    int count = Math.max(1, (bottom - top - 28) / 27);
    scroll = Math.clamp(scroll, 0, Math.max(0, list.size() - count));
    for (int n = scroll; n < Math.min(list.size(), scroll + count); n++) {
      var row = list.get(n);
      String key = row.getString("key");
      var b =
          button(
              (row.getBoolean("enabled") ? "◆ " : "◇ ") + fit(row.getString("title"), w - 65),
              x + 12,
              top + 25 + (n - scroll) * 27,
              w - 24,
              23,
              () -> {
                if (dirty && !key.equals(selected)) {
                  confirm = key;
                  rebuildWidgets();
                  return;
                }
                if (!key.equals(selected)) load(key);
                page = "edit";
                rebuildWidgets();
              });
      tip(b, row.getString("detail"));
    }
    if (list.isEmpty())
      text("Learn a supported skill to edit its equations.", x + 14, top + 29, MUTED);
    text(
        fit(
            list.size()
                + " actions · Formula "
                + (state.getBoolean("passive") ? "ON" : "OFF")
                + " · "
                + state.getInt("capacity")
                + " edited actions can be enabled · scroll for more",
            w - 24),
        x + 12,
        bottom + 10,
        MUTED);
  }

  private void editor() {
    if (chosen().isEmpty()) {
      page = "actions";
      actions();
      return;
    }
    var m = model();
    var keys = new ArrayList<>(draft.keySet());
    int n = keys.indexOf(aspect);
    var prev =
        button(
            "‹",
            x + 12,
            top,
            20,
            19,
            () -> {
              aspect = keys.get(Math.floorMod(n - 1, keys.size()));
              caret = -1;
              rebuildWidgets();
            });
    tip(prev, "Previous equation");
    var selector =
        button(
            fit(
                m.labels().getOrDefault(aspect, aspect) + " (" + (n + 1) + "/" + keys.size() + ")",
                w - 116),
            x + 35,
            top,
            w - 104,
            19,
            () -> {
              page = "aspects";
              scroll = 0;
              rebuildWidgets();
            });
    tip(
        selector,
        chosen().getString("title")
            + "\n"
            + aspect
            + " = "
            + m.defaults().getOrDefault(aspect, "Your helper equation")
            + "\nClick to choose an aspect.");
    var helper =
        button(
            "+",
            x + w - 64,
            top,
            22,
            19,
            () -> {
              if (draft.size() >= m.defaults().size() + 4) return;
              int k = 1;
              while (draft.containsKey("helper_" + k)) k++;
              aspect = "helper_" + k;
              draft.put(aspect, "1");
              dirty = true;
              rebuildWidgets();
            });
    tip(helper, "Add a helper equation; insert its name into another equation to use it.");
    helper.active = draft.size() < m.defaults().size() + 4;
    var del =
        button(
            "−",
            x + w - 39,
            top,
            22,
            19,
            () -> {
              draft.remove(aspect);
              aspect = keys.getFirst();
              dirty = true;
              rebuildWidgets();
            });
    del.active = !m.defaults().containsKey(aspect);
    tip(del, "Remove this helper (native outputs cannot be removed)");
    input =
        new EditBox(
            font, x + 12, top + 24, w - 24, 20, Component.literal("Equation for " + aspect));
    input.setMaxLength(160);
    input.setValue(draft.getOrDefault(aspect, ""));
    if (caret >= 0) input.setCursorPosition(Math.min(caret, input.getValue().length()));
    input.setResponder(
        s -> {
          draft.put(aspect, s);
          dirty = true;
          preview();
        });
    addRenderableWidget(input);
    preview();
    if (bottom - top >= 144) {
      text(
          fit(chosen().getString("title") + " · " + aspect + " =", w - 24), x + 12, top + 52, GOLD);
      text(
          "Select Parameters to insert inputs, operators or helper names.",
          x + 12,
          top + 116,
          MUTED);
    }
    int by = bottom;
    int bw = (w - 34) / 4;
    button(
        enabled ? "Enabled ✓" : "Disabled",
        x + 12,
        by,
        bw,
        20,
        () -> {
          enabled = !enabled;
          dirty = true;
          rebuildWidgets();
        });
    tip(
        button(
            "Original",
            x + 16 + bw,
            by,
            bw,
            20,
            () -> {
              draft = new LinkedHashMap<>(m.defaults());
              aspect = draft.keySet().iterator().next();
              dirty = true;
              rebuildWidgets();
            }),
        "Restore every equation for this action. Save to apply.");
    tip(
        button(
            "Balance cost",
            x + 20 + 2 * bw,
            by,
            bw,
            20,
            () -> {
              try {
                draft = m.balanceCost(draft);
                dirty = true;
                rebuildWidgets();
              } catch (IllegalArgumentException ex) {
                notice = ex.getMessage();
              }
            }),
        "Fill cost equations using the maximum possible output. The server also enforces this"
            + " minimum bill.");
    save = button(pending ? "Saving…" : "Save", x + 24 + 3 * bw, by, bw, 20, () -> send("save"));
    save.active = error.isEmpty() && !pending;
  }

  private void aspects() {
    var m = model();
    var keys = new ArrayList<>(draft.keySet());
    int count = Math.max(1, (bottom - top) / 26);
    scroll = Math.clamp(scroll, 0, Math.max(0, keys.size() - count));
    for (int n = scroll; n < Math.min(keys.size(), scroll + count); n++) {
      String k = keys.get(n);
      var b =
          button(
              fit(m.labels().getOrDefault(k, k) + " = " + draft.get(k), w - 40),
              x + 12,
              top + (n - scroll) * 26,
              w - 24,
              22,
              () -> {
                aspect = k;
                caret = -1;
                page = "edit";
                rebuildWidgets();
              });
      tip(b, k + " = " + draft.get(k));
    }
    button(
        "Back to equation",
        x + 12,
        bottom,
        w - 24,
        20,
        () -> {
          page = "edit";
          rebuildWidgets();
        });
  }

  private void preview() {
    result = null;
    error = "";
    try {
      result = model().evaluate(draft);
    } catch (IllegalArgumentException ex) {
      error = ex.getMessage();
    }
    if (save != null) save.active = error.isEmpty() && !pending;
  }

  private void reference() {
    var m = model();
    int cols = w >= 500 ? 3 : 2, cw = (w - 32) / cols;
    int available = Math.max(1, (bottom - top - 27) / 24);
    var tokens = new ArrayList<String>();
    tokens.addAll(List.of(" + ", " - ", " * ", " / ", " ^ ", "min(", "max(", "clamp(", "abs("));
    tokens.addAll(m.inputs().keySet().stream().sorted().toList());
    tokens.addAll(draft.keySet());
    int total = (tokens.size() + cols - 1) / cols;
    scroll = Math.clamp(scroll, 0, Math.max(0, total - available));
    for (int row = 0; row < available; row++)
      for (int col = 0; col < cols; col++) {
        int at = (row + scroll) * cols + col;
        if (at >= tokens.size()) continue;
        String token = tokens.get(at);
        String title = token.startsWith("base_") ? token : token.trim();
        var b =
            button(
                fit(title, cw - 12),
                x + 12 + col * (cw + 4),
                top + row * 24,
                cw,
                20,
                () -> {
                  int cursor =
                      caret < 0
                          ? draft.get(aspect).length()
                          : Math.min(caret, draft.get(aspect).length());
                  String s = draft.get(aspect);
                  s =
                      s.substring(0, Math.min(cursor, s.length()))
                          + token
                          + s.substring(Math.min(cursor, s.length()));
                  if (s.length() <= 160) {
                    draft.put(aspect, s);
                    caret = cursor + token.length();
                    dirty = true;
                  }
                  page = "edit";
                  rebuildWidgets();
                  if (input != null) {
                    setFocused(input);
                    input.setCursorPosition(Math.min(cursor + token.length(), s.length()));
                  }
                });
        String description = describe(token, m);
        tip(b, description);
      }
    button(
        "Injury example",
        x + 12,
        bottom,
        w / 2 - 18,
        20,
        () -> {
          String key = m.powerOutputs().getFirst();
          draft.put(key, "base_" + key + " * (1 + missing_hp_fraction)");
          aspect = key;
          dirty = true;
          page = "edit";
          rebuildWidgets();
        });
    button(
        "Back to equation",
        x + w / 2 + 2,
        bottom,
        w / 2 - 14,
        20,
        () -> {
          page = "edit";
          rebuildWidgets();
        });
  }

  private String describe(String token, EquationModel m) {
    if (m.inputs().containsKey(token))
      return token
          + " = "
          + EquationModel.pretty(m.inputs().get(token))
          + "\n"
          + (token.endsWith("_fraction")
              ? "A fraction from 0 to 1 (1 = 100%). Snapshotted before payment."
              : token.startsWith("base_")
                  ? "Read-only native value from the server configuration."
                  : "Read-only input.")
          + "\nClick to insert. Scroll for more inputs.";
    if (draft.containsKey(token))
      return token
          + " = "
          + draft.get(token)
          + "\nA named equation. Its result updates dependent equations automatically.";
    return switch (token.trim()) {
      case "^" -> "Power: base_healing ^ 1.2. Constant exponent from 0 to 3.";
      case "min(" -> "min(a, b) · use the smaller result";
      case "max(" -> "max(a, b) · use the larger result";
      case "clamp(" -> "clamp(value, minimum, maximum) · keep a result within bounds";
      case "abs(" -> "abs(value) · positive magnitude";
      default -> "Arithmetic operator. Use parentheses to group operations.";
    };
  }

  private void synthesis() {
    int yy = top - 13;
    boolean[] yes = {
      state.getBoolean("parent1"),
      state.getBoolean("parent2"),
      state.getBoolean("awakened"),
      state.getInt("achievement") >= state.getInt("required"),
      state.getBoolean("affordable")
    };
    String[] names = {
      "Exponentiation fully mastered",
      "Mathematician fully mastered",
      "True Hero / True Demon Lord awakening completed",
      "Mastered native skills: " + state.getInt("achievement") + " / " + state.getInt("required"),
      "Permanent magicules: " + String.format(Locale.ROOT, "%,.0f", state.getDouble("cost"))
    };
    int step = Math.min(25, Math.max(13, (bottom - yy - 20) / 6));
    for (int j = 0; j < names.length; j++)
      text(
          (yes[j] ? "✓ " : "◇ ") + fit(names[j], w - 45),
          x + 16,
          yy + j * step,
          yes[j] ? CYAN : MUTED);
    text(
        fit("Replaces both parent skills. No EP requirement.", w - 30),
        x + 16,
        yy + 5 * step,
        GOLD);
    button(
                confirm.equals("synthesis") ? "Confirm permanent payment" : "Synthesize Laplace",
                x + 12,
                bottom,
                w - 24,
                20,
                () -> {
                  if (!confirm.equals("synthesis")) {
                    confirm = "synthesis";
                    rebuildWidgets();
                  } else send("synthesize");
                })
            .active =
        state.getBoolean("ready") && !pending;
  }

  private void confirmation() {
    if (confirm.equals("notice")) {
      int yy = top + 8;
      String rest = notice;
      while (!rest.isEmpty() && yy + 9 < bottom) {
        String line = font.plainSubstrByWidth(rest, w - 32);
        if (line.isEmpty()) break;
        text(line, x + 16, yy, GOLD);
        rest = rest.substring(line.length());
        yy += 12;
      }
      button(
          "Continue editing",
          x + 12,
          bottom,
          w - 24,
          20,
          () -> {
            confirm = "";
            rebuildWidgets();
          });
      return;
    }
    text("Discard your unsaved changes?", x + 16, top + 10, GOLD);
    text("Choose Equation to keep editing.", x + 16, top + 26, MUTED);
    button(
        "Discard and open action",
        x + 12,
        bottom,
        w - 24,
        20,
        () -> {
          String next = confirm;
          confirm = "";
          if (next.equals("close")) {
            dirty = false;
            onClose();
            return;
          }
          load(next);
          page = "edit";
          rebuildWidgets();
        });
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float dt) {}

  @Override
  public void render(GuiGraphics g, int mx, int my, float dt) {
    g.fill(0, 0, width, height, 0xb0081119);
    g.fill(x, y, x + w, y + h, 0xf4091923);
    g.renderOutline(x, y, w, h, EDGE);
    g.fill(x + 1, y + 1, x + w - 1, y + 28, 0xff13313c);
    g.blit(
        HeavenlyRestriction.id("textures/skill/laplace.png"), x + 6, y + 2, 0, 0, 24, 24, 24, 24);
    g.drawString(font, "LAPLACE", x + 34, y + 8, GOLD, false);
    g.drawString(font, fit("Lord of Equations", w - 175), x + 117, y + 8, MUTED, false);
    for (var l : lines) g.drawString(font, fit(l.text, w - 24), l.x, l.y, l.color, false);
    if (page.equals("edit")
        && confirm.isEmpty()
        && state.getBoolean("learned")
        && !chosen().isEmpty()) {
      int yy = top + (bottom - top >= 144 ? 70 : 51);
      if (!error.isEmpty()) {
        g.drawString(font, fit(error, w - 24), x + 12, yy, 0xffffa6a0, false);
        if (mx >= x + 12 && mx < x + w - 12 && my >= yy && my < yy + 18)
          g.renderTooltip(
              font, font.split(Component.literal(error), Math.min(340, w - 24)), mx, my);
      } else if (result != null) {
        String key = aspect;
        var m = model();
        double v = result.values().getOrDefault(key, 0d);
        String preview =
            (m.costOutputs().contains(key) ? "Charged: " : "Result: ")
                + EquationModel.pretty(v)
                + " "
                + m.units().getOrDefault(key, "")
                + (m.defaults().containsKey(key)
                    ? " · native "
                        + EquationModel.pretty(result.nativeValues().getOrDefault(key, 0d))
                    : " · helper equation");
        g.drawString(font, fit(preview, w - 24), x + 12, yy, CYAN, false);
        if (yy + 22 < bottom) {
          String cost = m.costOutputs().getFirst();
          double requested = new FormulaSet(draft, m.bounds()).evaluate(m.inputs()).get(cost);
          g.drawString(
              font,
              fit(
                  "MP · requested "
                      + EquationModel.pretty(requested)
                      + " / minimum "
                      + EquationModel.pretty(result.minimumCosts().get(cost))
                      + " / charged "
                      + EquationModel.pretty(result.values().get(cost)),
                  w - 24),
              x + 12,
              yy + 14,
              MUTED,
              false);
        }
        if (mx >= x + 12 && mx < x + w - 12 && my >= yy && my < yy + 25)
          g.renderTooltip(
              font,
              font.split(
                  Component.literal(
                      "Native and edited values are shown before Universal Formula and other"
                          + " modifiers.\n"
                          + "Maximum edited output: "
                          + EquationModel.pretty(m.powerLimit())
                          + "× native. Costs use the strongest possible formula state: doubled"
                          + " output costs at least 3×; tripled costs at least 9×. Cost discounts"
                          + " are limited to 25%.\n"
                          + "Native caps, cooldowns, defenses, cast times and projectile count"
                          + " remain.\n"
                          + chosen().getString("detail")),
                  Math.min(340, w - 24)),
              mx,
              my);
      }
    }
    if (page.equals("edit") && confirm.isEmpty() && result != null && bottom - top > 190) {
      var m = model();
      int yy = top + 145;
      g.drawString(font, "ASPECT", x + 14, yy, MUTED, false);
      g.drawString(font, "NATIVE", x + w / 2, yy, MUTED, false);
      g.drawString(font, "EDITED", x + w * 3 / 4, yy, CYAN, false);
      yy += 15;
      for (String k : m.defaults().keySet()) {
        if (yy + 14 >= bottom - 20) break;
        g.fill(x + 10, yy - 2, x + w - 10, yy + 11, k.equals(aspect) ? 0xff193b48 : 0xff0e2632);
        g.drawString(font, fit(m.labels().get(k), w / 2 - 24), x + 14, yy, TEXT, false);
        g.drawString(
            font, EquationModel.pretty(result.nativeValues().get(k)), x + w / 2, yy, MUTED, false);
        g.drawString(
            font,
            EquationModel.pretty(result.values().get(k)) + " " + m.units().get(k),
            x + w * 3 / 4,
            yy,
            CYAN,
            false);
        yy += 18;
      }
    }
    if (!notice.isEmpty() && h > 240)
      g.drawString(font, fit(notice, w - 24), x + 12, bottom - 15, GOLD, false);
    super.render(g, mx, my, dt);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    if (page.equals("actions") || page.equals("reference") || page.equals("aspects")) {
      scroll = Math.max(0, scroll - (int) Math.signum(dy));
      rebuildWidgets();
      return true;
    }
    return super.mouseScrolled(mx, my, dx, dy);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void onClose() {
    if (dirty) {
      if (confirm.equals("close")) {
        confirm = "";
        rebuildWidgets();
        return;
      }
      confirm = "close";
      page = "edit";
      rebuildWidgets();
      return;
    }
    if (state.hasUUID("session"))
      PacketDistributor.sendToServer(
          new LaplaceNetwork.Request(state.getUUID("session"), "close", "", false, Map.of()));
    super.onClose();
  }

  private final class FormulaButton extends Button {
    FormulaButton(int xx, int yy, int ww, int hh, String label, Runnable click) {
      super(xx, yy, ww, hh, Component.literal(label), b -> click.run(), DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
      int xx = getX(), yy = getY();
      boolean selectedTab =
          getMessage()
              .getString()
              .startsWith(
                  page.equals("actions") ? "1  " : page.equals("reference") ? "3  " : "2  ");
      g.fill(
          xx,
          yy,
          xx + getWidth(),
          yy + getHeight(),
          isHoveredOrFocused() || selectedTab ? 0xff234955 : 0xff112d38);
      g.renderOutline(
          xx, yy, getWidth(), getHeight(), isHoveredOrFocused() || selectedTab ? CYAN : EDGE);
      g.drawCenteredString(
          font,
          fit(getMessage().getString(), getWidth() - 10),
          xx + getWidth() / 2,
          yy + (getHeight() - 8) / 2,
          active ? TEXT : 0xff59717b);
    }
  }
}
