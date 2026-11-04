package dev.heavenlyrestriction.client.cartography;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.subclass.ISpatialMovement;
import io.github.manasmods.tensura.client.screen.SpatialMovementScreen;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;

/** The actual native coordinate menu, with a server-prepared destination prefilled once. */
public final class MapWarpScreen<T extends ManasSkill & ISpatialMovement>
    extends SpatialMovementScreen<T> {
  private final CompoundTag destination;
  private boolean seeded;

  public MapWarpScreen(T skill, List<String> dimensions, CompoundTag destination) {
    super(skill, dimensions);
    this.destination = destination.copy();
  }

  @Override
  protected void init() {
    viewingWarpPads = false;
    super.init();
    if (!seeded) {
      fieldX.setValue(Double.toString(destination.getDouble("x")));
      fieldY.setValue(Double.toString(destination.getDouble("y")));
      fieldZ.setValue(Double.toString(destination.getDouble("z")));
      tooltipFieldD = destination.getString("dimension");
      fieldD.setValue(getDimensionName(tooltipFieldD));
      fieldD.setTooltip(tooltipFieldD);
      seeded = true;
    }
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    super.render(g, mx, my, partial);
    String label = "Surface arrival · " + destination.getString("name");
    g.drawCenteredString(
        font,
        font.plainSubstrByWidth(label, width - 20),
        width / 2,
        Math.max(5, guiTop - 17),
        MaraudersMapScreen.GOLD);
  }
}
