package dev.wildhunt.client;

import dev.wildhunt.myths.MythsNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Server snapshots drive this screen. Disabled buttons are convenience, never authority. */
public final class MythsScreen extends Screen {
    private CompoundTag data;
    private final Screen parent;
    private int tab,scroll,selected=-1,left,top,panelWidth,panelHeight,rows;
    private boolean confirming;
    private int detailScroll;
    public MythsScreen(CompoundTag data,Screen parent) { super(Component.literal("Tensura Myths"));this.data=data;this.parent=parent; }
    public void update(CompoundTag tag) { data=tag;selected=-1;confirming=false;rebuildWidgets(); }
    private ListTag entries() { return data.getList(tab==0?"choices":"ascensions",Tag.TAG_COMPOUND); }
    private void send(String action,String argument) { PacketDistributor.sendToServer(new MythsNetwork.Request(action,argument)); }
    private Button button(int x,int y,int w,String title,Button.OnPress click) { return addRenderableWidget(Button.builder(Component.literal(title),click).bounds(x,y,w,18).build()); }
    @Override protected void init() {
        panelWidth=Math.min(width-16,450);panelHeight=Math.min(height-16,285);left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        rows=Math.max(2,(panelHeight-137)/20);scroll=Math.clamp(scroll,0,Math.max(0,entries().size()-rows));
        button(left+10,top+26,110,"Duo Race",b->{tab=0;scroll=0;selected=-1;confirming=false;rebuildWidgets();});
        button(left+124,top+26,110,"Godly",b->{tab=1;scroll=0;selected=-1;confirming=false;rebuildWidgets();});
        button(left+panelWidth-30,top+7,20,"×",b->onClose());
        var list=entries();int listWidth=Math.min(184,(panelWidth-32)/2);
        for(int row=0;row<rows && row+scroll<list.size();row++) {
            int index=row+scroll;var entry=list.getCompound(index);
            var name=font.plainSubstrByWidth(entry.getString("name"),listWidth-12);
            if(selected==index)name="› "+font.plainSubstrByWidth(entry.getString("name"),listWidth-24);
            var btn=button(left+10,top+83+row*20,listWidth,name,b->{selected=index;confirming=false;detailScroll=0;rebuildWidgets();});
            btn.setTooltip(Tooltip.create(Component.literal(entry.getString("name")+(entry.contains("detail")?"\n"+entry.getString("detail"):""))));
        }
        int y=top+panelHeight-46;
        if(tab==0) {
            boolean has=data.getString("choice_action").equals("evolve");
            var select=button(left+10,y,Math.min(184,(panelWidth-32)/2),data.getBoolean("converged")?"Lineages unified":has?"Evolve secondary":confirming?"Confirm permanent choice":"Choose lineage",b->{
                if(!has && !confirming) {confirming=true;rebuildWidgets();return;}
                if(selected>=0 && selected<entries().size())send(data.getString("choice_action"),entries().getCompound(selected).getString("id"));
            });
            select.active=selected>=0 && !data.getBoolean("converged") && (has || data.getBoolean("primary_final") && data.getBoolean("awakened") && data.getDouble("ep")>=data.getDouble("secondary_ep"));
            var awaken=button(left+panelWidth/2+4,y,panelWidth/2-14,data.getBoolean("converged")?"Convergence complete":"Convergent Awakening",b->send("awaken",""));
            awaken.active=!data.getBoolean("converged") && data.getBoolean("primary_final") && data.getBoolean("secondary_final") && data.getBoolean("awakened") && data.getDouble("ep")>=data.getDouble("convergence_ep");
        } else {
            var ascend=button(left+10,y,panelWidth-20,data.getBoolean("has_godly")?"Aion is already learned":"Evolve selected Ultimate into Aion",b->{if(selected>=0)send("godly",entries().getCompound(selected).getString("id"));});
            ascend.active=selected>=0 && entries().getCompound(selected).getBoolean("mastered") && !data.getBoolean("has_godly") && data.getBoolean("awakened") && data.getDouble("ep")>=data.getDouble("godly_ep");
        }
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy) {
        if(x>left+panelWidth/2 && y>top+83 && y<top+panelHeight-51) {detailScroll=Math.max(0,detailScroll+(dy>0?-2:2));return true;}
        int next=Math.clamp(scroll+(dy>0?-1:1),0,Math.max(0,entries().size()-rows));
        if(scroll!=next){scroll=next;rebuildWidgets();return true;}return super.mouseScrolled(x,y,dx,dy);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g,mx,my,partial);
        g.fill(left,top,left+panelWidth,top+panelHeight,0xf2161e30);g.renderOutline(left,top,panelWidth,panelHeight,0xffd4b572);
        g.drawString(font,"TENSURA MYTHS",left+11,top+11,0xffedcf89,false);
        String status=tab==0?(data.getBoolean("converged")?data.getString("primary")+" · Convergent race":data.getString("primary")+" / "+data.getString("secondary")):"Aion, God of Causality · Godly / Ultimate";
        g.drawString(font,font.plainSubstrByWidth(status,panelWidth-22),left+11,top+53,0xffd5e5fa,false);
        g.drawString(font,String.format(java.util.Locale.ROOT,"Base EP: %,.0f · Awakened: %s",data.getDouble("ep"),data.getBoolean("awakened")?"Yes":"No"),left+11,top+67,0xff9aacbf,false);
        int textX=left+panelWidth/2+8,available=panelWidth/2-20;
        String details;
        if(tab==0) {
            details="Selection: "+String.format(java.util.Locale.ROOT,"%,.0f",data.getDouble("secondary_ep"))+" EP\nConvergence: "+String.format(java.util.Locale.ROOT,"%,.0f",data.getDouble("convergence_ep"))+" EP\nPrimary final: "+(data.getBoolean("primary_final")?"Yes":"No")+"\nSecondary final: "+(data.getBoolean("secondary_final")?"Yes":"No")+"\nGain weights: "+String.format(java.util.Locale.ROOT,"%.3f aura / %.3f magicules",data.getDouble("aura"),data.getDouble("magicules"));
            if(data.getBoolean("converged"))details="Convergence complete.\nYour two lineages are unified.\n\n"+data.getString("fusion_description");
            if(data.contains("fusion_name"))details+="\n\n"+(data.getBoolean("converged")?"Converged as ":"Awakens as ")+data.getString("fusion_name")+"\n"+data.getDouble("fusion_aura")+" aura / "+data.getDouble("fusion_magicules")+" magicules";
            if(selected>=0 && selected<entries().size())details+="\n\n"+entries().getCompound(selected).getString("detail");
            if(confirming)details="PERMANENT CHOICE\n\nThis lineage cannot be swapped.\n\n"+details;
        } else details="Four arts: Worldline Sever; Event Horizon; Causal Reversal; Genesis Verdict.\n\nNative passive: Unbroken Continuum.\n\nRequires an awakened soul, a mastered Myths Ultimate and "+String.format(java.util.Locale.ROOT,"%,.0f",data.getDouble("godly_ep"))+" base EP.";
        int yy=top+84;
        var detailLines=font.split(Component.literal(details),available);
        int visible=Math.max(1,(panelHeight-135)/10);detailScroll=Math.min(detailScroll,Math.max(0,detailLines.size()-visible));
        for(var line:detailLines.subList(detailScroll,detailLines.size())) {if(yy>=top+panelHeight-51)break;g.drawString(font,line,textX,yy,0xffc9d6e6,false);yy+=10;}
        String message=data.getString("message");
        if(!message.isEmpty())g.drawString(font,font.plainSubstrByWidth(message,panelWidth-22),left+11,top+panelHeight-18,0xffffd8a0,false);
        else if(entries().size()>rows || detailLines.size()>visible)g.drawString(font,"Scroll either column to browse",left+11,top+panelHeight-18,0xff8c9eb8,false);
        for(var widget:renderables)widget.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
