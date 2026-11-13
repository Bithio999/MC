package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.client.laplace.EquationCanvas;
import dev.heavenlyrestriction.laplace.*;
import dev.heavenlyrestriction.laplace.EquationBoard.*;
import java.util.*;
import java.util.function.BooleanSupplier;
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
import org.lwjgl.glfw.GLFW;

/** A live equation board: structural blocks, named links, undo and explicit application state. */
@EventBusSubscriber(
    modid = HeavenlyRestriction.ID,
    bus = EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class LaplaceScreen extends Screen implements EquationCanvas.Host {
  private static final int TEXT = 0xffdeedf2,
      MUTED = 0xff8eaab7,
      CYAN = 0xff8bdcdb,
      GOLD = 0xffefd296,
      EDGE = 0xff365a67;
  private CompoundTag state;
  private EquationModel liveModel;
  private PaymentPlan draftPlan = PaymentPlan.MAGICULES, savedPlan = PaymentPlan.MAGICULES;
  private final EditBox[] shares = new EditBox[4];
  private PaymentPlan modalPlan;
  private String paymentError = "";
  private EquationBoard board;
  private EquationCanvas canvas;
  private EquationCanvas.Viewport viewport;
  private Address selection;
  private String selected = "",
      view = "actions",
      query = "",
      library = "Inputs",
      modal = "",
      notice = "",
      error = "",
      awaiting = "",
      queuedOperation = "",
      switchTo = "",
      customNumber = "2",
      armedLabel = "";
  private Expr armed;
  private Drag drag;
  private EquationModel.Result preview, temporaryPreview;
  private Map<String, String> savedEquations = Map.of();
  private final List<BoardButton> controls = new ArrayList<>();
  private final List<AbstractWidget> libraryWidgets = new ArrayList<>();
  private final List<Label> labels = new ArrayList<>();
  private EditBox search, modalInput;
  private boolean dirty, pending, libraryOpen = true, pan, compact;
  private int x,
      y,
      w,
      h,
      top,
      foot,
      previewY,
      previewHeight,
      listScroll,
      libraryScroll,
      age,
      stateAge,
      noticeAge,
      helpScroll;
  private double downX, downY, lastX, lastY;
  private Address numberTarget;
  private String modalError = "";
  private String hoverKey = "";
  private long hoverSince;

  private record Label(String text, int x, int y, int color) {}

  private static final class Drag {
    final Expr tool;
    final Address source;
    final String label;
    final double x, y;
    boolean moved;

    Drag(Expr t, Address s, String label, double x, double y) {
      tool = t;
      source = s;
      this.label = label;
      this.x = x;
      this.y = y;
    }
  }

  public LaplaceScreen(CompoundTag state) {
    super(Component.literal("Laplace · Equation Board"));
    this.state = state.copy();
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent e) {
    e.enqueueWork(() -> LaplaceNetwork.receiver = LaplaceScreen::receive);
  }

  public static void receive(CompoundTag tag) {
    var mc = Minecraft.getInstance();
    if (tag.getBoolean("open") && tag.getBoolean("allowed")) {
      mc.setScreen(new LaplaceScreen(tag));
      return;
    }
    if (!(mc.screen instanceof LaplaceScreen s)
        || !tag.hasUUID("session")
        || !tag.getUUID("session").equals(s.state.getUUID("session"))) return;
    if (!tag.getBoolean("allowed")) {
      mc.setScreen(null);
      return;
    }
    String operation = tag.getString("operation");
    if (operation.isEmpty()) operation = s.awaiting;
    s.state = tag.copy();
    s.stateAge = s.age;
    s.pending = false;
    s.awaiting = "";
    String message = tag.getString("notice");
    if (!message.isEmpty()) {
      s.notice = message;
      s.noticeAge = s.age;
    }
    boolean learned = s.state.getBoolean("learned");
    if (!s.selected.isEmpty() && !s.chosen().isEmpty())
      s.liveModel = LaplaceNetwork.decode(s.chosen().getCompound("model"));
    if ((operation.equals("apply") || operation.equals("save"))
        && (message.startsWith("Applied.") || message.startsWith("Saved."))) {
      s.load(s.selected);
      s.rebuildWidgets();
    } else if (operation.equals("synthesize") && learned) {
      s.view = "actions";
      s.modal = "";
      s.rebuildWidgets();
    } else {
      s.evaluate();
      s.refreshControls();
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

  private boolean live() {
    return chosen().getBoolean("enabled") && state.getBoolean("passive");
  }

  private boolean savedEnabled() {
    return chosen().getBoolean("enabled");
  }

  private long waitTicks() {
    return Math.max(0, state.getLong("wait") - (age - stateAge));
  }

  private void load(String key) {
    selected = key;
    viewport = null;
    canvas = null;
    var row = chosen();
    if (row.isEmpty()) return;
    liveModel = LaplaceNetwork.decode(row.getCompound("model"));
    var formulas = new LinkedHashMap<String, String>();
    var eq = row.getCompound("equations");
    for (String k : liveModel.defaults().keySet()) formulas.put(k, eq.getString(k));
    eq.getAllKeys().stream()
        .sorted()
        .filter(k -> !formulas.containsKey(k))
        .forEach(k -> formulas.put(k, eq.getString(k)));
    savedEquations = new LinkedHashMap<>(formulas);
    draftPlan = savedPlan = liveModel.payment().plan();
    try {
      board = new EquationBoard(formulas);
    } catch (IllegalArgumentException ex) {
      board = new EquationBoard(liveModel.defaults());
      notice = "Saved formula needs repair. Native equations loaded; Apply to replace it.";
      noticeAge = age;
    }
    if (selection == null || !board.names().contains(selection.equation()))
      selection = new Address(board.names().getFirst(), List.of());
    else selection = new Address(selection.equation(), List.of());
    dirty = false;
    armed = null;
    drag = null;
    evaluate();
  }

  @Override
  public EquationBoard document() {
    return board;
  }

  @Override
  public EquationModel model() {
    return liveModel == null ? null : liveModel.withPayment(draftPlan);
  }

  @Override
  public EquationModel.Result result() {
    return temporaryPreview != null ? temporaryPreview : preview;
  }

  @Override
  public Address selection() {
    return selection;
  }

  private boolean busy() {
    return pending && !awaiting.equals("refresh") || !queuedOperation.isEmpty();
  }

  @Override
  public boolean ready() {
    return !busy() && modal.isEmpty();
  }

  private void evaluate() {
    preview = null;
    error = "";
    if (board == null || liveModel == null) return;
    if (board.holes() > 0) {
      error =
          board.holes()
              + " empty input"
              + (board.holes() == 1 ? "" : "s")
              + " — drop a number or variable into each dashed slot.";
      return;
    }
    try {
      preview = model().evaluate(board.formulas());
    } catch (IllegalArgumentException ex) {
      error = ex.getMessage();
    }
  }

  private void edit(Runnable action) {
    try {
      var before = board.names();
      action.run();
      try {
        dirty = !board.formulas().equals(savedEquations) || !draftPlan.equals(savedPlan);
      } catch (IllegalArgumentException ex) {
        dirty = true;
      }
      notice = "";
      temporaryPreview = null;
      if (!board.names().contains(selection.equation()))
        selection = new Address(board.names().getFirst(), List.of());
      else
        try {
          board.at(selection);
        } catch (IllegalArgumentException ex) {
          selection = new Address(selection.equation(), List.of());
        }
      evaluate();
      if (canvas != null) canvas.layout();
      if (!before.equals(board.names())) rebuildWidgets();
      else refreshControls();
    } catch (IllegalArgumentException ex) {
      message(ex.getMessage());
    }
  }

  private void message(String text) {
    notice = text;
    noticeAge = age;
  }

  private void select(Address at) {
    selection = at;
    refreshControls();
  }

  private void request(String operation) {
    if (pending) {
      if (!operation.equals("refresh")) {
        queuedOperation = operation;
        refreshControls();
      }
      return;
    }
    Map<String, String> formulas = Map.of();
    if (operation.equals("apply") || operation.equals("save")) {
      if (!error.isEmpty()) return;
      formulas = board.formulas();
    }
    pending = true;
    awaiting = operation;
    PacketDistributor.sendToServer(
        new LaplaceNetwork.Request(
            state.getUUID("session"), operation, selected, true, formulas, draftPlan));
    refreshControls();
  }

  private String fit(String text, int pixels) {
    return font.width(text) <= pixels
        ? text
        : font.plainSubstrByWidth(text, Math.max(0, pixels - 8)) + "…";
  }

  private BoardButton button(String label, int xx, int yy, int width, int height, Runnable action) {
    return button(label, xx, yy, width, height, action, () -> !busy());
  }

  private BoardButton button(
      String label,
      int xx,
      int yy,
      int width,
      int height,
      Runnable action,
      BooleanSupplier allowed) {
    var b = new BoardButton(xx, yy, width, height, label, action, allowed);
    controls.add(b);
    addRenderableWidget(b);
    return b;
  }

  private void tip(AbstractWidget widget, String text) {
    widget.setTooltip(Tooltip.create(Component.literal(text)));
  }

  private void label(String text, int xx, int yy, int color) {
    labels.add(new Label(text, xx, yy, color));
  }

  private void refreshControls() {
    for (var b : controls) b.active = b.allowed.getAsBoolean();
    for (var widget : libraryWidgets)
      widget.visible =
          view.equals("board")
              && modal.isEmpty()
              && (!compact || libraryOpen)
              && !(compact && drag != null && drag.moved);
  }

  @Override
  protected void init() {
    x = Math.max(6, (width - 850) / 2);
    y = Math.max(6, (height - 470) / 2);
    w = width - 2 * x;
    h = Math.min(470, height - 2 * y);
    boolean wasCompact = compact;
    compact = w < 500;
    if (compact && !wasCompact) libraryOpen = false;
    top = y + (compact ? 47 : 62);
    foot = y + h - 27;
    previewHeight = compact ? 34 : 58;
    previewY = foot - previewHeight - 5;
    build();
  }

  private void build() {
    if (canvas != null) viewport = canvas.viewport();
    controls.clear();
    libraryWidgets.clear();
    labels.clear();
    search = null;
    modalInput = null;
    canvas = null;
    button("×", x + w - 25, y + 7, 18, 18, this::onClose, () -> true);
    if (!state.getBoolean("learned")) {
      synthesis();
      refreshControls();
      return;
    }
    if (view.equals("board") && board != null) {
      workbench();
    } else actions();
    if (!modal.isEmpty()) {
      clearWidgets();
      controls.clear();
      libraryWidgets.clear();
      modalControls();
    }
    refreshControls();
  }

  private void actions() {
    int yy = y + 40;
    search = new EditBox(font, x + 12, yy, w - 24, 19, Component.literal("Search learned actions"));
    search.setMaxLength(64);
    search.setHint(Component.literal("Search your learned actions…"));
    search.setValue(query);
    search.setResponder(
        s -> {
          query = s;
          listScroll = 0;
          rebuildWidgets();
          if (search != null) {
            setFocused(search);
            search.setCursorPosition(s.length());
          }
        });
    addRenderableWidget(search);
    var items =
        rows().stream()
            .filter(
                t ->
                    t.getString("title")
                        .toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT)))
            .toList();
    int count = Math.max(1, (foot - yy - 29) / 29);
    listScroll = Math.clamp(listScroll, 0, Math.max(0, items.size() - count));
    for (int n = listScroll; n < Math.min(items.size(), listScroll + count); n++) {
      var row = items.get(n);
      String key = row.getString("key");
      String status =
          row.getBoolean("enabled") ? (state.getBoolean("passive") ? "LIVE" : "PAUSED") : "NATIVE";
      var b =
          button(
              fit(row.getString("title"), w - 105) + "   ·   " + status,
              x + 12,
              yy + 26 + (n - listScroll) * 29,
              w - 24,
              25,
              () -> {
                if (dirty && !key.equals(selected)) {
                  switchTo = key;
                  openModal("switch");
                  return;
                }
                if (!key.equals(selected)) load(key);
                view = "board";
                libraryOpen = !compact;
                rebuildWidgets();
                if (canvas != null) canvas.start();
              });
      tip(
          b,
          row.getString("detail")
              + "\n"
              + "LIVE: saved edit is running. NATIVE: original equations are running. PAUSED: turn"
              + " Universal Formula on, or Apply & Enable.");
    }
    if (items.isEmpty())
      label("Learn a supported skill to open its board.", x + 14, yy + 32, MUTED);
    label(
        fit(
            items.size()
                + " actions · "
                + (items.size() > count ? "scroll for more · " : "")
                + state.getInt("capacity")
                + " edits can run at once",
            w - 24),
        x + 12,
        foot + 9,
        MUTED);
  }

  private void workbench() {
    int by = y + (compact ? 27 : 36), bh = compact ? 17 : 20;
    int xx = x + 10;
    button(
        "‹ Actions",
        xx,
        by,
        compact ? 60 : 66,
        bh,
        () -> {
          view = "actions";
          armed = null;
          drag = null;
          rebuildWidgets();
        });
    xx += compact ? 64 : 70;
    if (compact) {
      button(
          "Blocks",
          xx,
          by,
          42,
          bh,
          () -> {
            libraryOpen = !libraryOpen;
            refreshControls();
          });
      xx += 46;
    }
    tip(
        button(
            "Undo",
            xx,
            by,
            compact ? 32 : 38,
            bh,
            () -> edit(board::undo),
            () -> !busy() && board.canUndo()),
        "Undo (Ctrl+Z)");
    xx += compact ? 36 : 42;
    tip(
        button(
            "Redo",
            xx,
            by,
            compact ? 32 : 38,
            bh,
            () -> edit(board::redo),
            () -> !busy() && board.canRedo()),
        "Redo (Ctrl+Y / Ctrl+Shift+Z)");
    xx += compact ? 36 : 42;
    button(
        "Reset",
        xx,
        by,
        compact ? 38 : 42,
        bh,
        () -> edit(() -> board.reset(liveModel.defaults())));
    xx += compact ? 42 : 46;
    button("Fit", xx, by, 30, bh, () -> canvas.fit());
    xx += 34;
    button("More", xx, by, compact ? 38 : 40, bh, () -> openModal("more"));
    int libraryWidth = 128, cx = compact ? x + 10 : x + libraryWidth + 18, cw = x + w - 10 - cx;
    canvas = new EquationCanvas(font, cx, top, cw, Math.max(28, previewY - top - 5), this);
    addRenderableWidget(canvas);
    if (viewport == null) canvas.start();
    else canvas.restore(viewport);
    libraryWidgets(libraryWidth);
    var paymentButton =
        button(
            "Payment…",
            x + w - (compact ? 72 : 90) - 10,
            previewY + 3,
            compact ? 72 : 90,
            14,
            () -> openModal("payment"));
    tip(
        paymentButton,
        "Choose magicules, aura, HP, SHP, or a percentage mix. The budget equation stays the same."
            + " Healing and regeneration use magicules/aura only.");
    int applyWidth = compact ? 112 : 145;
    var apply =
        button(
            "Apply & Enable",
            x + w - applyWidth - 10,
            foot,
            applyWidth,
            20,
            () -> request("apply"),
            () ->
                !busy()
                    && error.isEmpty()
                    && waitTicks() == 0
                    && !chosen().getBoolean("requiresStop"));
    tip(
        apply,
        (chosen().getBoolean("requiresStop")
                ? "Turn Self-Regeneration off before applying changes.\n"
                : "")
            + "Save these equations and payment, enable this action and turn on Universal Formula."
            + " Other previously enabled actions resume too. The server checks ownership, costs and"
            + " limits.");
    if (savedEnabled()) {
      var disable =
          button(
              "Disable",
              x + w - applyWidth - (compact ? 70 : 78),
              foot,
              compact ? 56 : 64,
              20,
              () -> request("disable"),
              () -> !busy() && waitTicks() == 0);
      tip(
          disable,
          "Use this action's original native formula. Your board edits stay available; this does"
              + " not turn off Universal Formula or other actions.");
    }
  }

  private int libraryBottom() {
    return compact ? foot - 4 : previewY - 5;
  }

  private void libraryWidgets(int width) {
    int lx = x + 10, ly = top + 20;
    for (String category : List.of("Inputs", "Math", "Links")) {
      int i = List.of("Inputs", "Math", "Links").indexOf(category);
      var b =
          button(
              category,
              lx + i * 43,
              ly,
              41,
              18,
              () -> {
                library = category;
                libraryScroll = 0;
                rebuildWidgets();
              });
      libraryWidgets.add(b);
    }
    ly += 23;
    for (int n = 0; n < 2; n++) {
      String v = n == 0 ? "1" : customNumber;
      var tile = tile(v, Expr.number(v), lx + n * 30, ly, 27, 21);
      libraryWidgets.add(tile);
    }
    var number =
        button(
            "Number…",
            lx + 62,
            ly,
            66,
            21,
            () -> {
              numberTarget = null;
              openModal("number");
            });
    tip(number, "Choose a numeric value, then drag or click it into an input.");
    libraryWidgets.add(number);
    ly += 26;
    var tools = new ArrayList<Map.Entry<String, Expr>>();
    if (library.equals("Math")) {
      for (String op : List.of("*", "+", "-", "/", "^", "min", "max", "clamp", "abs", "neg"))
        tools.add(Map.entry(EquationCanvas.operation(op), Expr.operation(op)));
    } else if (library.equals("Links")) {
      for (String key : board.names())
        tools.add(Map.entry(EquationCanvas.name(key, liveModel), Expr.variable(key)));
    } else {
      var names = new ArrayList<>(liveModel.inputs().keySet());
      names.sort(
          Comparator.comparingInt(
                  (String k) -> k.startsWith("base_") ? 0 : k.equals("missing_hp_fraction") ? 1 : 2)
              .thenComparing(k -> k));
      for (String key : names)
        tools.add(Map.entry(EquationCanvas.name(key, liveModel), Expr.variable(key)));
    }
    int count = Math.max(1, (libraryBottom() - ly - 7) / 25);
    libraryScroll = Math.clamp(libraryScroll, 0, Math.max(0, tools.size() - count));
    for (int n = libraryScroll; n < Math.min(tools.size(), libraryScroll + count); n++) {
      var tool = tools.get(n);
      var b = tile(tool.getKey(), tool.getValue(), lx, ly + (n - libraryScroll) * 25, width, 21);
      libraryWidgets.add(b);
    }
  }

  private Tile tile(String label, Expr expr, int x, int y, int w, int h) {
    var tile = new Tile(x, y, w, h, label, expr);
    controls.add(tile);
    addRenderableWidget(tile);
    tip(tile, describe(expr));
    return tile;
  }

  private String describe(Expr e) {
    if (e.kind() == Kind.NUMBER)
      return "Drag "
          + e.value()
          + " into a slot. Click a number on the board to change it. You can also click this block,"
          + " then click a slot.";
    if (!e.children().isEmpty())
      return EquationCanvas.operation(e.value())
          + "\n"
          + "Drop onto an expression to keep it as the first input and add empty slots. Drag values"
          + " into the remaining slots.\n"
          + "Right-click an operation to unwrap or clear it.";
    String k = e.value();
    if (liveModel.inputs().containsKey(k))
      return EquationCanvas.name(k, liveModel)
          + "\n"
          + k
          + " = "
          + EquationModel.pretty(liveModel.inputs().get(k))
          + "\n"
          + (k.startsWith("base_")
              ? "Read-only native configuration value."
              : k.endsWith("_fraction")
                  ? "A fraction between 0 and 1. Live preview refreshes from the server."
                  : "Read-only input.")
          + "\nDrag or click to place.";
    return "Reference the result of "
        + EquationCanvas.name(k, liveModel)
        + ". Changes to that equation flow into this input. Circular links are rejected.";
  }

  private void openModal(String kind) {
    modal = kind;
    if (kind.equals("payment")) {
      modalPlan = draftPlan;
      paymentError = "";
    }
    modalError = "";
    helpScroll = 0;
    temporaryPreview = null;
    drag = null;
    armed = null;
    rebuildWidgets();
  }

  private void closeModal() {
    modal = "";
    temporaryPreview = null;
    rebuildWidgets();
  }

  private void modalControls() {
    int mw = Math.min(w - 18, 430),
        mh =
            Math.min(
                h - 14,
                modal.equals("payment")
                    ? 260
                    : modal.equals("more") ? 230 : modal.equals("help") ? 240 : 150),
        mx = x + (w - mw) / 2,
        my = y + (h - mh) / 2;
    button("×", mx + mw - 24, my + 6, 17, 17, this::closeModal, () -> true);
    if (modal.equals("payment")) {
      paymentControls(mx, my, mw, mh);
      return;
    }
    if (modal.equals("number") || modal.equals("text")) {
      String initial =
          modal.equals("number")
              ? (numberTarget == null ? customNumber : board.at(numberTarget).formula())
              : safeFormula(selection.equation());
      modalInput =
          new EditBox(
              font,
              mx + 14,
              my + 38,
              mw - 28,
              20,
              Component.literal(modal.equals("number") ? "Number value" : "Advanced equation"));
      modalInput.setMaxLength(modal.equals("number") ? 36 : 160);
      modalInput.setValue(initial);
      modalInput.setResponder(this::modalPreview);
      addRenderableWidget(modalInput);
      setFocused(modalInput);
      modalInput.setCursorPosition(initial.length());
      button(
          modal.equals("number") ? "Use number" : "Use formula",
          mx + 14,
          my + mh - 29,
          mw - 28,
          20,
          this::acceptModal);
      modalPreview(initial);
      return;
    }
    if (modal.equals("more")) {
      String[] names = {
        "Add helper equation",
        "Remove selected helper",
        "Advanced formula…",
        "Balance cost formula",
        "How the board works"
      };
      Runnable[] actions = {
        () -> {
          if (board.names().size() >= liveModel.defaults().size() + 4) {
            message("At most four helper equations per action.");
            return;
          }
          int n = 1;
          while (board.names().contains("helper_" + n)) n++;
          String key = "helper_" + n;
          edit(() -> board.addHelper(key));
          selection = new Address(key, List.of());
          closeModal();
          canvas.reveal(selection);
        },
        () -> {
          if (liveModel.defaults().containsKey(selection.equation())) {
            message("Select a helper equation first.");
            return;
          }
          edit(() -> board.deleteHelper(selection.equation(), liveModel.defaults().keySet()));
          selection = new Address(board.names().getFirst(), List.of());
          closeModal();
        },
        () -> openModal("text"),
        () -> {
          edit(() -> board.balance(liveModel));
          closeModal();
        },
        () -> openModal("help")
      };
      int step = Math.min(32, (mh - 42) / 5);
      for (int n = 0; n < 5; n++) {
        int choice = n;
        var b =
            button(
                names[n],
                mx + 14,
                my + 31 + n * step,
                mw - 28,
                step - 4,
                actions[n],
                () ->
                    !busy()
                        && (choice != 0 || board.names().size() < liveModel.defaults().size() + 4)
                        && (choice != 1
                            || !liveModel.defaults().containsKey(selection.equation())));
        if (n == 0)
          tip(
              b,
              "Add up to four helper equations. Drag a helper's output into another equation to use"
                  + " it.");
        if (n == 1) tip(b, "Select a helper on the board first. Native outputs cannot be removed.");
      }

      return;
    }
    if (modal.equals("node")) {
      boolean number = board.at(selection).kind() == Kind.NUMBER;
      int yy = my + 32;
      button(
          number ? "Change number" : "Advanced formula…",
          mx + 14,
          yy,
          mw - 28,
          20,
          () -> {
            if (number) {
              numberTarget = selection;
              openModal("number");
            } else openModal("text");
          });
      button(
          "Unwrap (keep first input)",
          mx + 14,
          yy + 26,
          mw - 28,
          20,
          () -> {
            edit(() -> board.unwrap(selection));
            closeModal();
          },
          () -> !busy() && !board.at(selection).children().isEmpty());
      button(
          "Clear this input",
          mx + 14,
          yy + 52,
          mw - 28,
          20,
          () -> {
            edit(() -> board.remove(selection));
            closeModal();
          });
      return;
    }
    if (modal.equals("switch") || modal.equals("close")) {
      button(
          "Keep editing",
          mx + 14,
          my + mh - 29,
          (mw - 34) / 2,
          20,
          () -> {
            view = "board";
            closeModal();
          });
      button(
          "Discard draft",
          mx + 20 + (mw - 34) / 2,
          my + mh - 29,
          (mw - 34) / 2,
          20,
          () -> {
            dirty = false;
            if (modal.equals("close")) {
              modal = "";
              onClose();
            } else {
              modal = "";
              load(switchTo);
              view = "board";
              rebuildWidgets();
            }
          });
      return;
    }
    button("Back to board", mx + 14, my + mh - 29, mw - 28, 20, this::closeModal);
  }

  private String safeFormula(String name) {
    try {
      return board.root(name).formula();
    } catch (IllegalArgumentException ex) {
      return liveModel.defaults().getOrDefault(name, "1");
    }
  }

  private void modalPreview(String value) {
    modalError = "";
    helpScroll = 0;
    temporaryPreview = null;
    try {
      if (modal.equals("number")) {
        Expr n = Expr.number(value);
        if (numberTarget != null) {
          var test = new EquationBoard(board.formulas());
          test.drop(numberTarget, n);
          temporaryPreview = model().evaluate(test.formulas());
        }
      } else {
        var test = new EquationBoard(board.formulas());
        test.text(selection.equation(), value);
        temporaryPreview = model().evaluate(test.formulas());
      }
    } catch (IllegalArgumentException ex) {
      modalError = ex.getMessage();
    }
  }

  private void acceptModal() {
    if (modalInput == null) return;
    String value = modalInput.getValue();
    try {
      if (modal.equals("number")) {
        Expr n = Expr.number(value);
        if (numberTarget == null) {
          customNumber = value;
          modal = "";
          rebuildWidgets();
          armed = n;
          armedLabel = value;
          message("Number selected — click an input slot, or drag a number from Blocks.");
          return;
        }
        edit(() -> board.drop(numberTarget, n));
      } else {
        EquationBoard.parse(value);
        edit(() -> board.text(selection.equation(), value));
      }
      closeModal();
    } catch (IllegalArgumentException ex) {
      modalError = ex.getMessage();
    }
  }

  @Override
  public void press(EquationCanvas.NodeBox node, double mx, double my, int button) {
    if (node == null) {
      if (button == 0 || button == 2) {
        pan = true;
        lastX = mx;
        lastY = my;
      }
      return;
    }
    if (button == -1) {
      select(node.address());
      return;
    }
    if (button == 1) {
      selection = node.address();
      if (node.output()) {
        message("Drag this result into another equation to link them.");
        return;
      }
      openModal("node");
      return;
    }
    if (button == 2) {
      pan = true;
      lastX = mx;
      lastY = my;
      return;
    }
    if (button != 0) return;
    select(node.address());
    if (armed != null && !node.output()) {
      Expr tool = armed;
      armed = null;
      edit(() -> board.drop(node.address(), tool));
      return;
    }
    drag =
        new Drag(
            node.output() ? Expr.variable(node.address().equation()) : node.expression(),
            node.output() ? null : node.address(),
            node.output()
                ? EquationCanvas.name(node.address().equation(), liveModel)
                : canvas.label(node.expression()),
            mx,
            my);
    downX = mx;
    downY = my;
    setFocused(canvas);
  }

  @Override
  public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
    if (pan && canvas != null) {
      canvas.pan(mx - lastX, my - lastY);
      lastX = mx;
      lastY = my;
      return true;
    }
    if (drag != null) {
      if (Math.hypot(mx - drag.x, my - drag.y) > 3) {
        drag.moved = true;
        if (compact) libraryOpen = false;
        refreshControls();
      }
      return true;
    }
    return super.mouseDragged(mx, my, button, dx, dy);
  }

  @Override
  public boolean mouseReleased(double mx, double my, int button) {
    if (pan) {
      setDragging(false);
      pan = false;
      return true;
    }
    if (drag == null) return super.mouseReleased(mx, my, button);
    setDragging(false);
    var d = drag;
    drag = null;
    refreshControls();
    if (d.moved) {
      var target = canvas == null ? null : canvas.hit(mx, my);
      if (target == null || target.output()) {
        message("Drop onto an input block or empty slot.");
        return true;
      }
      select(target.address());
      if (d.source == null) edit(() -> board.drop(target.address(), d.tool));
      else edit(() -> board.transfer(d.source, target.address(), hasShiftDown()));
      return true;
    }
    if (d.source == null) {
      armed = d.tool;
      armedLabel = d.label;
      message("Selected " + d.label + " — click a board input to place it.");
    } else if (d.tool.kind() == Kind.NUMBER) {
      numberTarget = d.source;
      openModal("number");
    }
    return true;
  }

  @Override
  public boolean mouseClicked(double mx, double my, int button) {
    if (!modal.isEmpty()) return super.mouseClicked(mx, my, button);
    if (view.equals("board")
        && compact
        && libraryOpen
        && mx >= x + 8
        && mx < x + 140
        && my >= top
        && my < libraryBottom()) {
      for (var widget : libraryWidgets)
        if (widget.visible && widget.mouseClicked(mx, my, button)) {
          setFocused(widget);
          return true;
        }
      return true;
    }
    return super.mouseClicked(mx, my, button);
  }

  @Override
  public boolean mouseScrolled(double mx, double my, double dx, double dy) {
    if (!modal.isEmpty()) {
      if (modal.equals("help")) helpScroll = Math.max(0, helpScroll - (int) Math.signum(dy) * 2);
      return true;
    }
    if (view.equals("actions")) {
      listScroll = Math.max(0, listScroll - (int) Math.signum(dy));
      rebuildWidgets();
      return true;
    }
    if (canvas != null) {
      if ((!compact || libraryOpen)
          && mx >= x + 8
          && mx < x + 140
          && my >= top
          && my < libraryBottom()) {
        libraryScroll = Math.max(0, libraryScroll - (int) Math.signum(dy));
        rebuildWidgets();
        return true;
      }
      if (canvas.isMouseOver(mx, my)) {
        canvas.scroll(mx, my, dy, hasControlDown(), hasShiftDown());
        return true;
      }
    }
    return super.mouseScrolled(mx, my, dx, dy);
  }

  @Override
  public boolean keyPressed(int key, int scan, int mods) {
    if (!modal.isEmpty()) {
      if (key == GLFW.GLFW_KEY_ESCAPE) {
        closeModal();
        return true;
      }
      if (key == GLFW.GLFW_KEY_ENTER && (modal.equals("number") || modal.equals("text"))) {
        acceptModal();
        return true;
      }
      return super.keyPressed(key, scan, mods);
    }
    if (view.equals("board") && !busy()) {
      if (hasControlDown() && key == GLFW.GLFW_KEY_Z) {
        edit(hasShiftDown() ? board::redo : board::undo);
        return true;
      }
      if (hasControlDown() && key == GLFW.GLFW_KEY_Y) {
        edit(board::redo);
        return true;
      }
      if (hasControlDown() && key == GLFW.GLFW_KEY_ENTER) {
        if (error.isEmpty() && waitTicks() == 0 && !chosen().getBoolean("requiresStop"))
          request("apply");
        return true;
      }
      if (getFocused() == canvas) {
        if (key == GLFW.GLFW_KEY_LEFT
            || key == GLFW.GLFW_KEY_RIGHT
            || key == GLFW.GLFW_KEY_UP
            || key == GLFW.GLFW_KEY_DOWN) {
          canvas.navigate(key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP ? -1 : 1);
          return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
          edit(() -> board.remove(selection));
          return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
          if (armed != null) {
            Expr tool = armed;
            armed = null;
            edit(() -> board.drop(selection, tool));
          } else if (board.at(selection).kind() == Kind.NUMBER) {
            numberTarget = selection;
            openModal("number");
          }
          return true;
        }
      }
    }
    return super.keyPressed(key, scan, mods);
  }

  @Override
  public void tick() {
    age++;
    if (pending) return;
    if (!queuedOperation.isEmpty()) {
      String op = queuedOperation;
      queuedOperation = "";
      request(op);
      return;
    }
    refreshControls();
    if (age - stateAge >= 40 && modal.isEmpty() && state.getBoolean("learned") && drag == null)
      request("refresh");
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float dt) {}

  @Override
  public void render(GuiGraphics g, int mx, int my, float dt) {
    g.fill(0, 0, width, height, 0xb0071118);
    g.fill(x, y, x + w, y + h, 0xff0d222c);
    g.renderOutline(x, y, w, h, EDGE);
    g.fill(x + 1, y + 1, x + w - 1, y + 27, 0xff14323c);
    g.blit(
        HeavenlyRestriction.id("textures/skill/laplace.png"), x + 5, y + 1, 0, 0, 24, 24, 24, 24);
    g.drawString(font, "LAPLACE", x + 33, y + 7, GOLD, false);
    g.drawString(
        font,
        fit(
            state.getBoolean("learned")
                ? view.equals("board")
                    ? chosen().getString("title") + (compact ? "" : " · EQUATION BOARD")
                    : "CHOOSE AN ACTION"
                : "ULTIMATE SYNTHESIS",
            w - 161),
        x + 117,
        y + 7,
        MUTED,
        false);
    for (var l : labels) g.drawString(font, fit(l.text, w - 24), l.x, l.y, l.color, false);
    if (view.equals("board") && board != null) {
      if (!compact || libraryOpen) {
        g.fill(x + 8, top, x + 140, libraryBottom(), 0xff102d37);
        g.renderOutline(x + 8, top, 132, libraryBottom() - top, EDGE);
        g.drawString(font, "BLOCK LIBRARY", x + 15, top + 6, GOLD, false);
      }
      if (!modal.isEmpty() && canvas != null) canvas.render(g, mx, my, dt);
      renderPreview(g);
    }
    super.render(g, mx, my, dt);
    // The small-screen library is an intentional drawer, drawn over the board and hit-tested first.
    if (view.equals("board") && compact && libraryOpen && modal.isEmpty()) {
      g.fill(x + 8, top, x + 140, libraryBottom(), 0xff102d37);
      g.renderOutline(x + 8, top, 132, libraryBottom() - top, EDGE);
      g.drawString(font, "BLOCK LIBRARY", x + 15, top + 6, GOLD, false);
      for (var widget : libraryWidgets) if (widget.visible) widget.render(g, mx, my, dt);
    }
    if (!modal.isEmpty() && state.getBoolean("learned")) renderModal(g, mx, my, dt);
    if (drag != null && drag.moved) {
      int ww = Math.min(180, font.width(drag.label) + 22);
      g.pose().pushPose();
      g.pose().translate(0, 0, 400);
      g.fill(mx + 9, my + 9, mx + 9 + ww, my + 34, 0xf02c4754);
      g.renderOutline(mx + 9, my + 9, ww, 25, GOLD);
      g.drawString(font, fit(drag.label, ww - 12), mx + 15, my + 17, TEXT, false);
      g.pose().popPose();
    } else if (view.equals("board")
        && modal.isEmpty()
        && !error.isEmpty()
        && canvas != null
        && mx >= canvas.getX()
        && mx < canvas.getRight()
        && my >= canvas.getBottom() - 16
        && my < canvas.getBottom())
      g.renderTooltip(font, font.split(Component.literal(error), Math.min(300, w - 22)), mx, my);
    else if (view.equals("board")
        && modal.isEmpty()
        && canvas != null
        && canvas.isMouseOver(mx, my)) {
      var node = canvas.hit(mx, my);
      String key = node == null ? "" : node.address().toString() + node.output();
      long now = net.minecraft.Util.getMillis();
      if (!key.equals(hoverKey)) {
        hoverKey = key;
        hoverSince = now;
      }
      if (now - hoverSince < 600) return;
      if (node != null && node.output())
        g.renderTooltip(
            font,
            font.split(
                Component.literal(
                    "Drag this named result into another equation to create a live link."),
                220),
            mx,
            my);
      else if (node != null && node.expression().kind() == Kind.VARIABLE)
        g.renderTooltip(
            font,
            font.split(
                Component.literal(
                    describe(node.expression())
                        + "\nDrag between occupied slots to swap; Shift-drag copies."),
                Math.min(280, w - 24)),
            mx,
            my);
    }
  }

  private String billText(PaymentPlan.Bill b) {
    var parts = new ArrayList<String>();
    if (b.magicules() > 0) parts.add(EquationModel.pretty(b.magicules()) + " MP");
    if (b.aura() > 0) parts.add(EquationModel.pretty(b.aura()) + " aura");
    if (b.hp() > 0) parts.add(EquationModel.pretty(b.hp()) + " HP");
    if (b.shp() > 0) parts.add(EquationModel.pretty(b.shp()) + " SHP");
    return parts.isEmpty() ? "No cost" : String.join(" + ", parts);
  }

  private double[] percentages(PaymentPlan plan) {
    return new double[] {
      plan.magicules() * 100, plan.aura() * 100, plan.hp() * 100, plan.shp() * 100
    };
  }

  private void paymentControls(int mx, int my, int mw, int mh) {
    String[] names = {"Magicules", "Aura", "HP", "SHP"};
    PaymentPlan[] presets = {
      PaymentPlan.MAGICULES, PaymentPlan.AURA, PaymentPlan.HP, PaymentPlan.SHP
    };
    int col = (mw - 28) / 4, fieldY = my + (mh < 220 ? 65 : 72);
    for (int n = 0; n < 4; n++) {
      int choice = n;
      var b =
          button(
              names[n],
              mx + 14 + n * col,
              my + 31,
              col - 4,
              18,
              () -> {
                var v = percentages(presets[choice]);
                for (int k = 0; k < 4; k++) shares[k].setValue(EquationModel.pretty(v[k]));
              },
              () -> !busy() && (choice < 2 || liveModel.payment().allowVital()));
      tip(
          b,
          choice < 2
              ? "Pay the full balanced budget with " + names[n] + "."
              : "Leaves at least 1 " + names[n] + ". Not available for healing or regeneration.");
      shares[n] =
          new EditBox(
              font,
              mx + 14 + n * col,
              fieldY,
              col - 5,
              18,
              Component.literal(names[n] + " percentage"));
      shares[n].setMaxLength(12);
      shares[n].setValue(EquationModel.pretty(percentages(modalPlan)[n]));
      shares[n].setEditable(n < 2 || liveModel.payment().allowVital());
      addRenderableWidget(shares[n]);
      tip(
          shares[n],
          "Percentage of the cost budget paid in "
              + names[n]
              + ". All four shares must total 100%.");
    }
    for (var box : shares) box.setResponder(value -> paymentPreview());
    var use =
        button(
            "Use payment mix",
            mx + 14,
            my + mh - 29,
            mw - 28,
            20,
            () -> {
              if (!paymentError.isEmpty() || modalPlan == null) return;
              draftPlan = modalPlan;
              edit(() -> {});
              closeModal();
            },
            () -> !busy() && paymentError.isEmpty() && modalPlan != null);
    tip(
        use,
        "Updates this draft. Apply & Enable commits equations and payment together. Closing this"
            + " dialog discards its changes.");
    paymentPreview();
  }

  private void paymentPreview() {
    paymentError = "";
    try {
      double[] v = new double[4];
      for (int n = 0; n < 4; n++) {
        v[n] = Double.parseDouble(shares[n].getValue()) / 100;
      }
      modalPlan = new PaymentPlan(v[0], v[1], v[2], v[3]);
      var m = liveModel.withPayment(modalPlan);
      m.payment().validate();
      if (board.holes() == 0) m.evaluate(board.formulas());
    } catch (NumberFormatException ex) {
      paymentError = "Enter a percentage in every field.";
      modalPlan = null;
    } catch (IllegalArgumentException ex) {
      paymentError = ex.getMessage();
      modalPlan = null;
    }
    refreshControls();
  }

  private void renderPayment(GuiGraphics g, int mx, int my, int mw, int mh) {
    boolean small = mh < 220;
    int col = (mw - 28) / 4;
    String[] names = {"Magicules %", "Aura %", "HP %", "SHP %"};
    for (int n = 0; n < 4; n++)
      g.drawString(
          font,
          fit(names[n], col - 5),
          mx + 14 + n * col,
          my + (small ? 53 : 59),
          n >= 2 && !liveModel.payment().allowVital() ? MUTED : TEXT,
          false);
    var rates = liveModel.payment().rates();
    String rate =
        "1 aura = "
            + EquationModel.pretty(rates.magiculesPerAura())
            + " MP eq; 1 HP/SHP = "
            + EquationModel.pretty(rates.magiculesPerHP())
            + " / "
            + EquationModel.pretty(rates.magiculesPerSHP())
            + " MP eq";
    g.drawString(
        font,
        fit(small ? "Shares must total 100%" : rate, mw - 28),
        mx + 14,
        my + (small ? 91 : 102),
        MUTED,
        false);
    String bill = "Fill board inputs to preview payment";
    double budget = 0;
    if (modalPlan != null && board.holes() == 0)
      try {
        var m = liveModel.withPayment(modalPlan);
        var r = m.evaluate(board.formulas());
        budget = r.values().get("magicule_cost");
        bill = billText(m.bill(r));
      } catch (IllegalArgumentException ignored) {
      }
    if (!small)
      g.drawString(
          font,
          "Cost budget: " + EquationModel.pretty(budget) + " MP eq",
          mx + 14,
          my + 118,
          GOLD,
          false);
    g.drawString(
        font,
        fit(paymentError.isEmpty() ? bill : paymentError, mw - 28),
        mx + 14,
        my + (small ? 105 : 134),
        paymentError.isEmpty() ? CYAN : 0xffffb0a1,
        false);
    if (!small) {
      if (modalPlan != null) {
        var v = percentages(modalPlan);
        String[] res = {"Magicules", "Aura", "HP", "SHP"};
        double[] div = {
          1, rates.magiculesPerAura(), rates.magiculesPerHP(), rates.magiculesPerSHP()
        };
        for (int n = 0; n < 4; n++)
          g.drawString(
              font,
              fit(
                  res[n]
                      + " = budget × "
                      + EquationModel.pretty(v[n])
                      + "% / "
                      + EquationModel.pretty(div[n]),
                  mw - 28),
              mx + 14,
              my + 154 + n * 12,
              MUTED,
              false);
      }
      g.drawString(
          font,
          fit(
              liveModel.payment().allowVital()
                  ? "All-or-nothing payment · HP/SHP retain at least 1"
                  : "Healing & regeneration: magicules / aura only",
              mw - 28),
          mx + 14,
          my + 208,
          GOLD,
          false);
    }
  }

  private void renderPreview(GuiGraphics g) {
    var r = result();
    int pw = w - 20, xx = x + 10;
    g.fill(xx, previewY, xx + pw, foot - 4, 0xff102f39);
    g.renderOutline(xx, previewY, pw, previewHeight + 1, EDGE);
    String key = selection.equation();
    int split = compact ? pw / 2 : pw * 53 / 100;
    g.fill(xx + split, previewY + 6, xx + split + 1, foot - 10, EDGE);
    g.drawString(
        font,
        fit(EquationCanvas.name(key, liveModel), split - 16),
        xx + 8,
        previewY + 5,
        GOLD,
        false);
    String value =
        r == null
            ? "Fill inputs to preview"
            : (r.nativeValues().containsKey(key)
                    ? EquationModel.pretty(r.nativeValues().get(key)) + " → "
                    : "")
                + EquationModel.pretty(r.values().getOrDefault(key, 0d))
                + " "
                + liveModel.units().getOrDefault(key, "");
    g.drawString(
        font, fit(value, split - 16), xx + 8, previewY + 18, r == null ? MUTED : CYAN, false);
    String cost = liveModel.costOutputs().getFirst();
    g.drawString(
        font, compact ? "PAYMENT" : "RESOURCE PAYMENT", xx + split + 8, previewY + 5, GOLD, false);
    g.drawString(
        font,
        r == null ? "—" : fit(billText(model().bill(r)), pw - split - 16),
        xx + split + 8,
        previewY + 18,
        CYAN,
        false);
    if (!compact) {
      String detail =
          r == null
              ? error
              : "Budget "
                  + EquationModel.pretty(r.values().get(cost))
                  + " MP eq · before native cost modifiers";
      g.drawString(font, fit(detail, pw - split - 16), xx + split + 8, previewY + 34, MUTED, false);
      g.drawString(
          font,
          fit("Formula outputs · combat/healing modifiers apply afterward", split - 16),
          xx + 8,
          previewY + 34,
          MUTED,
          false);
    }
    String status =
        pending && !awaiting.equals("refresh")
            ? "Applying…"
            : !error.isEmpty()
                ? error
                : armed != null
                    ? "PLACE " + armedLabel + ": click a board input"
                    : dirty
                        ? live()
                            ? "DRAFT · previous formula is still live"
                            : "DRAFT · changes are not applied"
                        : live()
                            ? "LIVE · saved formula is active"
                            : savedEnabled()
                                ? "PAUSED · Universal Formula is off"
                                : "NATIVE · edit is not enabled";
    if (age - noticeAge < 160 && !notice.isEmpty()) status = notice;
    if (waitTicks() > 0) status = "Next change in " + ((waitTicks() + 19) / 20) + "s · " + status;
    int room = w - (compact ? 132 : 171) - (savedEnabled() ? (compact ? 64 : 74) : 0) - 20;
    g.drawString(
        font,
        fit(status, Math.max(20, room)),
        x + 10,
        foot + 7,
        dirty || !live() ? GOLD : CYAN,
        false);
    // Full status and validation explanations are always available, including on small screens.
    if (!error.isEmpty())
      g.drawString(
          font,
          fit(error, canvas.getWidth() - 14),
          canvas.getX() + 7,
          canvas.getY() + canvas.getHeight() - 13,
          0xffedb58c,
          false);
  }

  private void renderModal(GuiGraphics g, int mouseX, int mouseY, float dt) {
    int mw = Math.min(w - 18, 430),
        mh =
            Math.min(
                h - 14,
                modal.equals("payment")
                    ? 260
                    : modal.equals("more") ? 230 : modal.equals("help") ? 240 : 150),
        mx = x + (w - mw) / 2,
        my = y + (h - mh) / 2;
    g.fill(x + 1, y + 28, x + w - 1, y + h - 1, 0xd0071620);
    g.fill(mx - 2, my - 2, mx + mw + 2, my + mh + 2, 0xff07161e);
    g.fill(mx, my, mx + mw, my + mh, 0xff17333f);
    g.renderOutline(mx, my, mw, mh, EDGE);
    String heading =
        switch (modal) {
          case "payment" -> "Choose resource payment";
          case "number" -> "Choose a number";
          case "text" -> "Advanced formula";
          case "more" -> "Board tools";
          case "node" -> "Edit this block";
          case "switch", "close" -> "Unsaved changes";
          default -> "How the equation board works";
        };
    g.drawString(font, heading, mx + 14, my + 11, GOLD, false);
    if (modal.equals("payment")) {
      renderPayment(g, mx, my, mw, mh);
      for (var widget : renderables) widget.render(g, mouseX, mouseY, dt);
      return;
    }
    String body =
        switch (modal) {
          case "number", "text" ->
              modalError.isEmpty()
                  ? (temporaryPreview == null
                      ? "Press Enter to use this value."
                      : "Preview updated. Cost budget: "
                          + EquationModel.pretty(
                              temporaryPreview.values().get(liveModel.costOutputs().getFirst()))
                          + " MP eq")
                  : modalError;
          case "switch", "close" ->
              "Your board has unapplied changes. Keep editing, or discard this draft.";
          case "help" ->
              "Drag an input or number onto a slot. Drop an operation onto an expression to keep it"
                  + " as its first input. Fill the new empty slots.\n\n"
                  + "Drag existing blocks between slots to swap them. Shift-drag copies. Click a"
                  + " library tile, then a slot, if you prefer not to drag. Click numbers to change"
                  + " them.\n\n"
                  + "Scroll pans; Shift-scroll pans sideways; Ctrl-scroll zooms. Drag blank board"
                  + " space to pan. Fit shows all equations. Ctrl+Z undoes.\n\n"
                  + "Apply & Enable saves the action and turns Universal Formula on. Preview shows"
                  + " formula values before other modifiers. Payment chooses a resource mix; HP/SHP"
                  + " costs must leave 1 point. Healing/regeneration cannot pay HP/SHP. Native"
                  + " balance limits still apply.";
          default -> "";
        };
    int yy = my + (modal.equals("number") || modal.equals("text") ? 68 : 34), limit = my + mh - 36;
    g.enableScissor(mx + 8, my + 28, mx + mw - 8, limit);
    var helpLines = font.split(Component.literal(body), mw - 28);
    int visible = Math.max(1, (limit - yy) / 12);
    helpScroll = Math.min(helpScroll, Math.max(0, helpLines.size() - visible));
    for (var line : helpLines.subList(modal.equals("help") ? helpScroll : 0, helpLines.size())) {
      if (yy + 9 > limit) break;
      g.drawString(font, line, mx + 14, yy, modalError.isEmpty() ? TEXT : 0xffffb0a1, false);
      yy += 12;
    }
    g.disableScissor();
    for (var widget : renderables) widget.render(g, mouseX, mouseY, dt);
  }

  private void synthesis() {
    int yy = y + 42;
    String[] rows = {
      "Exponentiation fully mastered",
      "Mathematician fully mastered",
      "True Hero / True Demon Lord awakening complete",
      "Mastered supported skills: "
          + state.getInt("achievement")
          + " / "
          + state.getInt("required"),
      "Permanent magicules: " + String.format(Locale.ROOT, "%,.0f", state.getDouble("cost"))
    };
    boolean[] yes = {
      state.getBoolean("parent1"),
      state.getBoolean("parent2"),
      state.getBoolean("awakened"),
      state.getInt("achievement") >= state.getInt("required"),
      state.getBoolean("affordable")
    };
    int step = Math.min(29, Math.max(13, (foot - yy - 20) / 6));
    for (int n = 0; n < rows.length; n++)
      label((yes[n] ? "✓ " : "◇ ") + rows[n], x + 14, yy + n * step, yes[n] ? CYAN : MUTED);
    label("Replaces both parents. No EP requirement.", x + 14, yy + 5 * step, GOLD);
    button(
        modal.equals("synthesis-confirm") ? "Confirm permanent payment" : "Synthesize Laplace",
        x + 12,
        foot,
        w - 24,
        20,
        () -> {
          if (modal.equals("synthesis-confirm")) {
            modal = "";
            request("synthesize");
          } else {
            modal = "synthesis-confirm";
            rebuildWidgets();
          }
        },
        () -> !busy() && state.getBoolean("ready"));
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void onClose() {
    if (!modal.isEmpty()) {
      closeModal();
      return;
    }
    if (dirty) {
      openModal("close");
      return;
    }
    if (state.hasUUID("session"))
      PacketDistributor.sendToServer(
          new LaplaceNetwork.Request(state.getUUID("session"), "close", "", false, Map.of()));
    super.onClose();
  }

  private class BoardButton extends Button {
    final BooleanSupplier allowed;
    final Runnable action;

    BoardButton(
        int x, int y, int w, int h, String label, Runnable action, BooleanSupplier allowed) {
      super(x, y, w, h, Component.literal(label), b -> action.run(), DEFAULT_NARRATION);
      this.allowed = allowed;
      this.action = action;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
      boolean selected =
          List.of("Inputs", "Math", "Links").contains(getMessage().getString())
              && library.equals(getMessage().getString());
      boolean primary = getMessage().getString().equals("Apply & Enable");
      g.fill(
          getX(),
          getY(),
          getRight(),
          getBottom(),
          primary ? 0xff28514f : isHoveredOrFocused() || selected ? 0xff244653 : 0xff142f3a);
      g.renderOutline(
          getX(),
          getY(),
          width,
          height,
          active ? (primary ? GOLD : isHoveredOrFocused() || selected ? CYAN : EDGE) : 0xff34444f);
      g.drawCenteredString(
          font,
          fit(getMessage().getString(), width - 8),
          getX() + width / 2,
          getY() + (height - 8) / 2,
          active ? (primary ? GOLD : TEXT) : 0xff667d88);
    }
  }

  private final class Tile extends BoardButton {
    final Expr payload;

    Tile(int x, int y, int w, int h, String label, Expr payload) {
      super(
          x,
          y,
          w,
          h,
          label,
          () -> {
            armed = payload;
            armedLabel = label;
            message("Selected " + label + " — click an input to place it.");
          },
          () -> !busy());
      this.payload = payload;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
      if (button != 0 || !active || !visible || !isMouseOver(mx, my)) return false;
      drag = new Drag(payload, null, getMessage().getString(), mx, my);
      return true;
    }
  }
}
