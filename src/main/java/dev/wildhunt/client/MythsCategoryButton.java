package dev.wildhunt.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MythsCategoryButton extends Button {
    private final ResourceLocation icon;
    private final boolean godly;
    public MythsCategoryButton(int x,int y,ResourceLocation icon,Component title,boolean godly,OnPress press) {
        super(x,y,24,24,title,press,DEFAULT_NARRATION);this.icon=icon;this.godly=godly;setTooltip(Tooltip.create(title));
    }
    @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial) {
        g.fill(getX()-1,getY()-1,getX()+25,getY()+25,isHoveredOrFocused()?0xffe8c56f:0xff44546c);
        if(godly) {
            g.fill(getX(),getY(),getX()+24,getY()+24,0xff191d32);
            g.blit(icon,getX(),getY(),24,24,0,0,64,64,64,64);
        } else g.blit(icon,getX(),getY(),24,24,0,0,32,32,32,64);
    }
}
