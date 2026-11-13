package dev.heavenlyrestriction.client.laplace;

import dev.heavenlyrestriction.laplace.*;
import dev.heavenlyrestriction.laplace.EquationBoard.*;
import java.util.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Scissored, pannable equation board. Logical block addresses survive zoom and layout changes. */
public final class EquationCanvas extends AbstractWidget {
  public interface Host {
    EquationBoard document();

    EquationModel model();

    EquationModel.Result result();

    Address selection();

    boolean ready();

    void press(NodeBox node, double x, double y, int button);
  }

  public record NodeBox(
      Address address, Expr expression, int x, int y, int width, int height, boolean output) {
    boolean contains(double xx, double yy) {
      return xx >= x && xx < x + width && yy >= y && yy < y + height;
    }
  }

  private final Host host;
  private final Font font;
  private final List<NodeBox> nodes = new ArrayList<>(), groups = new ArrayList<>();
  private double zoom = 1, panX, panY;
  private int contentWidth, contentHeight;

  public EquationCanvas(Font font, int x, int y, int width, int height, Host host) {
    super(
        x,
        y,
        width,
        height,
        Component.literal(
            "Equation board. Select an input with arrow keys. Enter places a selected block or"
                + " edits a number. Delete clears the slot."));
    this.font = font;
    this.host = host;
    layout();
  }

  public static String name(String key, EquationModel model) {
    if (model.labels().containsKey(key)) return model.labels().get(key);
    if (key.startsWith("base_")) {
      String output = key.substring(5);
      return output.equals("cost")
          ? "Base cost"
          : "Base " + model.labels().getOrDefault(output, output.replace('_', ' '));
    }
    return switch (key) {
      case "missing_hp_fraction" -> "Missing HP %";
      case "hp_fraction" -> "Current HP %";
      case "aura_fraction" -> "Current aura %";
      case "magicule_fraction" -> "Current MP %";
      case "missing_hp" -> "Missing HP";
      case "cost_per_hp" -> "Cost per HP";
      case "pulse_seconds" -> "Pulse seconds";
      case "upkeep_seconds" -> "Upkeep seconds";
      default -> key.replace('_', ' ');
    };
  }

  public static String operation(String key) {
    return switch (key) {
      case "+" -> "Add +";
      case "-" -> "Subtract −";
      case "*" -> "Multiply ×";
      case "/" -> "Divide ÷";
      case "^" -> "Power ^";
      case "min" -> "Minimum";
      case "max" -> "Maximum";
      case "clamp" -> "Clamp";
      case "abs" -> "Absolute";
      case "neg" -> "Negate";
      default -> key;
    };
  }

  public String label(Expr e) {
    return switch (e.kind()) {
      case HOLE -> "Drop input";
      case OPERATOR, FUNCTION -> operation(e.value());
      case VARIABLE -> name(e.value(), host.model());
      case NUMBER -> e.value();
    };
  }

  private int nodeWidth(Expr e) {
    if (e.children().isEmpty()) return Math.clamp(font.width(label(e)) + 18, 55, 112);
    return Math.max(
        font.width(label(e)) + 28,
        e.children().stream().mapToInt(this::nodeWidth).sum()
            + 10 * (e.children().size() - 1)
            + 16);
  }

  private int nodeHeight(Expr e) {
    if (e.children().isEmpty()) return e.kind() == Kind.VARIABLE ? 31 : 25;
    return e.children().stream().mapToInt(this::nodeHeight).max().orElse(25) + 25;
  }

  private void place(Address address, Expr e, int x, int y) {
    int w = nodeWidth(e), h = nodeHeight(e);
    if (!e.children().isEmpty()) {
      groups.add(new NodeBox(address, e, x, y, w, h, false));
      nodes.add(new NodeBox(address, e, x, y, w, 19, false));
      int xx = x + 8;
      for (int n = 0; n < e.children().size(); n++) {
        Expr c = e.children().get(n);
        place(address.child(n), c, xx, y + 20);
        xx += nodeWidth(c) + 10;
      }
    } else nodes.add(new NodeBox(address, e, x, y, w, h, false));
  }

  public void layout() {
    nodes.clear();
    groups.clear();
    int y = 16;
    contentWidth = 0;
    for (String key : host.document().names()) {
      Expr root = host.document().root(key);
      nodes.add(new NodeBox(new Address(key, List.of()), Expr.variable(key), 12, y, 108, 33, true));
      place(new Address(key, List.of()), root, 146, y);
      contentWidth = Math.max(contentWidth, 146 + nodeWidth(root) + 16);
      y += Math.max(53, nodeHeight(root) + 19);
    }
    contentHeight = y;
  }

  public record Viewport(double zoom, double panX, double panY) {}

  public Viewport viewport() {
    return new Viewport(zoom, panX, panY);
  }

  public void restore(Viewport v) {
    zoom = v.zoom;
    panX = v.panX;
    panY = v.panY;
  }

  public void fit() {
    layout();
    zoom =
        Math.clamp(
            Math.min(
                (width - 20d) / Math.max(1, contentWidth),
                (height - 20d) / Math.max(1, contentHeight)),
            .5,
            1);
    panX = panY = 0;
  }

  public void start() {
    fit();
    if (zoom < .76) {
      zoom = .76;
      panX = 0;
      panY = height < 80 ? 16 : 0;
    }
  }

  private double worldX(double x) {
    return (x - getX() - 8) / zoom + panX;
  }

  private double worldY(double y) {
    return (y - getY() - 8) / zoom + panY;
  }

  public NodeBox hit(double x, double y) {
    if (!isMouseOver(x, y)) return null;
    double xx = worldX(x), yy = worldY(y);
    for (int n = nodes.size() - 1; n >= 0; n--)
      if (nodes.get(n).contains(xx, yy)) return nodes.get(n);
    for (int n = groups.size() - 1; n >= 0; n--)
      if (groups.get(n).contains(xx, yy)) return groups.get(n);
    return null;
  }

  public double[] port(String name) {
    layout();
    var b =
        nodes.stream()
            .filter(n -> n.output && n.address.equation().equals(name))
            .findFirst()
            .orElseThrow();
    return new double[] {
      getX() + 8 + (b.x + b.width / 2d - panX) * zoom,
      getY() + 8 + (b.y + b.height / 2d - panY) * zoom
    };
  }

  public double[] center(Address a) {
    layout();
    var box =
        nodes.stream().filter(n -> !n.output && n.address.equals(a)).findFirst().orElseThrow();
    return new double[] {
      getX() + 8 + (box.x + box.width / 2d - panX) * zoom,
      getY() + 8 + (box.y + box.height / 2d - panY) * zoom
    };
  }

  public void reveal(Address a) {
    layout();
    var b = nodes.stream().filter(n -> !n.output && n.address.equals(a)).findFirst().orElse(null);
    if (b == null) return;
    if (b.y < panY + 5) panY = Math.max(0, b.y - 5);
    if (b.y + b.height > panY + (height - 15) / zoom)
      panY = Math.max(0, b.y + b.height - (height - 15) / zoom);
    if (b.x + b.width > panX + (width - 15) / zoom)
      panX = Math.max(0, b.x + b.width - (width - 15) / zoom);
    if (b.x < panX) panX = Math.max(0, b.x - 5);
  }

  public void navigate(int step) {
    var all = nodes.stream().filter(n -> !n.output).map(NodeBox::address).toList();
    if (all.isEmpty()) return;
    int i = all.indexOf(host.selection());
    host.press(
        nodes.stream()
            .filter(
                n -> !n.output && n.address.equals(all.get(Math.floorMod(i + step, all.size()))))
            .findFirst()
            .orElseThrow(),
        0,
        0,
        -1);
    reveal(host.selection());
  }

  public void pan(double dx, double dy) {
    panX = Math.clamp(panX - dx / zoom, 0, Math.max(0, contentWidth - (width - 20) / zoom));
    panY = Math.clamp(panY - dy / zoom, 0, Math.max(0, contentHeight - (height - 20) / zoom));
  }

  public void scroll(double mx, double my, double delta, boolean control, boolean shift) {
    if (control) {
      double wx = worldX(mx), wy = worldY(my);
      zoom = Math.clamp(zoom * Math.pow(1.12, delta), .45, 1.75);
      panX = Math.max(0, wx - (mx - getX() - 8) / zoom);
      panY = Math.max(0, wy - (my - getY() - 8) / zoom);
    } else if (shift) pan(-delta * 28, 0);
    else pan(0, delta * 28);
  }

  private String fit(String s, int w) {
    return font.width(s) <= w ? s : font.plainSubstrByWidth(s, Math.max(0, w - 7)) + "…";
  }

  private void line(GuiGraphics g, int x, int y, int xx, int yy, int color) {
    g.fill(Math.min(x, xx), y, Math.max(x, xx) + 1, y + 1, color);
    g.fill(xx, Math.min(y, yy), xx + 1, Math.max(y, yy) + 1, color);
  }

  private int color(Expr e) {
    return switch (e.kind()) {
      case NUMBER -> 0xffdfc58b;
      case VARIABLE -> 0xff78cbd4;
      case OPERATOR, FUNCTION -> 0xffbaace3;
      case HOLE -> 0xff5b899b;
    };
  }

  @Override
  protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
    layout();
    g.fill(getX(), getY(), getRight(), getBottom(), 0xff0a2029);
    g.renderOutline(getX(), getY(), width, height, 0xff42616c);
    g.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1);
    g.pose().pushPose();
    g.pose().translate(getX() + 8 - panX * zoom, getY() + 8 - panY * zoom, 0);
    g.pose().scale((float) zoom, (float) zoom, 1);
    int left = (int) panX - 16,
        top = (int) panY - 16,
        right = (int) (panX + width / zoom + 16),
        bottom = (int) (panY + height / zoom + 16);
    for (int xx = Math.floorDiv(left, 16) * 16; xx < right; xx += 16)
      g.fill(xx, top, xx + 1, bottom, xx % 64 == 0 ? 0xff183841 : 0xff102b34);
    for (int yy = Math.floorDiv(top, 16) * 16; yy < bottom; yy += 16)
      g.fill(left, yy, right, yy + 1, yy % 64 == 0 ? 0xff183841 : 0xff102b34);
    for (NodeBox node : nodes)
      if (node.output) {
        line(g, node.x + node.width, node.y + 16, 139, node.y + 16, 0xff638b91);
        g.drawString(font, "=", 132, node.y + 12, 0xffb1c6c9, false);
      }
    // Named results remain visibly linked to their source equation.
    for (NodeBox node : nodes)
      if (!node.output
          && node.expression.kind() == Kind.VARIABLE
          && host.document().names().contains(node.expression.value())) {
        var source =
            nodes.stream()
                .filter(n -> n.output && n.address.equation().equals(node.expression.value()))
                .findFirst()
                .orElse(null);
        if (source != null) {
          int yy = node.y + node.height / 2;
          line(g, source.x + source.width, source.y + 16, 127, yy, 0xff3a7f82);
          line(g, 127, yy, node.x, yy, 0xff3a7f82);
        }
      }
    for (NodeBox b : groups) {
      g.fill(b.x, b.y, b.x + b.width, b.y + b.height, 0xff102b36);
      g.renderOutline(b.x, b.y, b.width, b.height, 0xff425268);
    }
    NodeBox hover = hit(mx, my);
    for (NodeBox b : nodes) {
      if (b.output) {
        g.fill(b.x, b.y, b.x + b.width, b.y + b.height, 0xff133a40);
        g.renderOutline(b.x, b.y, b.width, b.height, 0xff4f8386);
        g.drawString(
            font,
            fit(name(b.address.equation(), host.model()), b.width - 12),
            b.x + 6,
            b.y + 5,
            0xffd4eee8,
            false);
        var r = host.result();
        String k = b.address.equation();
        String value =
            r == null
                ? "waiting for inputs"
                : r.nativeValues().containsKey(k)
                    ? EquationModel.pretty(r.nativeValues().get(k))
                        + " → "
                        + EquationModel.pretty(r.values().get(k))
                    : "= " + EquationModel.pretty(r.values().getOrDefault(k, 0d));
        g.drawString(font, fit(value, b.width - 12), b.x + 6, b.y + 19, 0xff86c9c4, false);
        continue;
      }
      boolean selected = b.address.equals(host.selection()), over = b == hover;
      int c = color(b.expression);
      if (b.expression.children().isEmpty())
        g.fill(
            b.x,
            b.y,
            b.x + b.width,
            b.y + b.height,
            b.expression.kind() == Kind.HOLE ? 0xff122e37 : 0xff15313d);
      if (b.expression.kind() == Kind.HOLE && !selected && !over) {
        for (int d = 0; d < b.width; d += 8) {
          g.fill(b.x + d, b.y, Math.min(b.x + d + 4, b.x + b.width), b.y + 1, c);
          g.fill(
              b.x + d, b.y + b.height - 1, Math.min(b.x + d + 4, b.x + b.width), b.y + b.height, c);
        }
        for (int d = 0; d < b.height; d += 8) {
          g.fill(b.x, b.y + d, b.x + 1, Math.min(b.y + d + 4, b.y + b.height), c);
          g.fill(
              b.x + b.width - 1, b.y + d, b.x + b.width, Math.min(b.y + d + 4, b.y + b.height), c);
        }
      } else
        g.renderOutline(
            b.x, b.y, b.width, b.height, selected || over ? 0xffe8d19b : c & 0xff7f7f7f);
      if (over) g.renderOutline(b.x - 1, b.y - 1, b.width + 2, b.height + 2, 0xff91dcdb);
      g.drawString(
          font,
          fit(label(b.expression), b.width - 12),
          b.x + 6,
          b.y + (b.expression.kind() == Kind.NUMBER || b.expression.kind() == Kind.HOLE ? 8 : 5),
          c,
          false);
      if (b.expression.kind() == Kind.VARIABLE) {
        Double n = host.model().inputs().get(b.expression.value());
        if (n == null && host.result() != null)
          n = host.result().values().get(b.expression.value());
        String val =
            n == null
                ? "named result"
                : b.expression.value().endsWith("_fraction")
                    ? EquationModel.pretty(n * 100) + "%"
                    : EquationModel.pretty(n);
        g.drawString(font, fit(val, b.width - 12), b.x + 6, b.y + 19, 0xff86a7b6, false);
      }
    }
    g.pose().popPose();
    g.disableScissor();
    if (height > 110) {
      g.fill(getX() + 2, getY() + 2, getRight() - 2, getY() + 16, 0xe00a2029);
      g.drawString(
          font,
          fit("Drag blocks onto inputs · click numbers to edit", width - 16),
          getX() + 7,
          getY() + 5,
          0xff7899a8,
          false);
    }
    String status = Math.round(zoom * 100) + "%";
    g.drawString(
        font, status, getRight() - font.width(status) - 6, getBottom() - 12, 0xff7595a5, false);
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    if (!host.ready() || !isMouseOver(x, y)) return false;
    host.press(hit(x, y), x, y, button);
    return true;
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput output) {
    defaultButtonNarrationText(output);
  }
}
