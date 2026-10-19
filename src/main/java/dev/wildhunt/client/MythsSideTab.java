package dev.wildhunt.client;

import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import io.github.manasmods.tensura.util.client.ScreenHelper;
import net.minecraft.client.gui.GuiGraphics;

/** Native tab frame, hover behavior and tooltip with the lineage/awakening glyph. */
public final class MythsSideTab extends SimpleButton {
    private final int id;
    public MythsSideTab(int x,int y,int id) {
        super(x,y,21,24,ScreenHelper.RIGHT_TAB,null,ScreenHelper.getMenuName(id),b->ScreenHelper.clicked(()->ScreenHelper.openScreen(id)));this.id=id;
    }
    @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial) {
        super.renderWidget(g,mx,my,partial);g.blit(ScreenHelper.getTabIcon(id),getX()+1,getY()+4,0,0,16,16,16,16);
    }
}
