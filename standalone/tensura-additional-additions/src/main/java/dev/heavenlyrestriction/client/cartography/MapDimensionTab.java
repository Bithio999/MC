package dev.heavenlyrestriction.client.cartography;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Vanilla item art follows resource packs, with a visible selected tab and narrated hover name. */
final class MapDimensionTab extends MapUi.AtlasButton {
  private final ItemStack art;
  private final boolean selected;
  private final int accent;

  MapDimensionTab(
      int x, int y, String name, Item art, boolean selected, int accent, Runnable action) {
    super(x, y, 30, 28, Component.literal(name), b -> action.run());
    this.art = new ItemStack(art);
    this.selected = selected;
    this.accent = accent;
    setTooltip(Tooltip.create(Component.literal(name)));
  }

  @Override
  protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
    int x = getX(), y = getY();
    boolean hover = isHoveredOrFocused();
    g.fill(x + 1, y, x + 29, y + 28, selected ? 0xff31464c : hover ? 0xff263c43 : 0xff1a2930);
    g.fill(x, y + 1, x + 30, y + 27, selected ? 0xff31464c : hover ? 0xff263c43 : 0xff1a2930);
    g.fill(x + 2, y + 26, x + 28, y + 28, selected ? accent : MapUi.EDGE);
    if (isFocused()) g.renderOutline(x, y, 30, 28, MaraudersMapScreen.GOLD);
    g.renderItem(art, x + 7, y + 5);
    if (selected) g.fill(x + 13, y + 29, x + 17, y + 31, accent);
  }
}
