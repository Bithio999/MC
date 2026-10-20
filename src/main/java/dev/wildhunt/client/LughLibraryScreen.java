package dev.wildhunt.client;

import dev.wildhunt.*;
import dev.wildhunt.myths.MythsNetwork;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.client.screen.templates.*;
import io.github.manasmods.tensura.client.screen.widgets.*;
import io.github.manasmods.tensura.util.client.RenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Pride's native sub-ability panel, search, six-row list, icons and scrolling detail area. */
public final class LughLibraryScreen extends SimpleScreen implements IScrollBar {
    private static final ResourceLocation BACKGROUND=ResourceLocation.fromNamespaceAndPath("tensura","textures/gui/sub_ability_selection/sub_ability_selection.png");
    private static final ResourceLocation ABILITY_BAR=ResourceLocation.fromNamespaceAndPath("tensura","textures/gui/ability_button.png");
    /** Native SimpleButton keeps a separate label field; update it with narration text. */
    private static final class LibraryButton extends SimpleButton {
        LibraryButton(int x,int y,int w,int h,Component label,Component tooltip,net.minecraft.client.gui.components.Button.OnPress press){super(x,y,w,h,label,tooltip,press);}
        @Override public void setMessage(Component label){super.setMessage(label);text=label;}
    }
    private CompoundTag state;
    private final List<CompoundTag> filtered=new ArrayList<>();
    private ExpandedEditBox search;
    private String query="",selected="";
    private int start,detailStart;
    private float offset;
    private boolean scrolling;
    private int syncTicks;
    public LughLibraryScreen(CompoundTag state){super(Component.literal("Appraised Skills"),233,145);this.state=state;filter();if(!filtered.isEmpty())selected=filtered.getFirst().getString("entry");}
    public void update(CompoundTag tag){state=tag;if(!tag.getBoolean("owned")){onClose();return;}filter();}
    private void request(String action,String argument){PacketDistributor.sendToServer(new MythsNetwork.Request(action,argument));}
    private int slot(){return Math.clamp(state.getInt("selected"),0,2);}
    private static String numeral(int slot){return switch(slot){case 0->"I";case 1->"II";default->"III";};}
    private CompoundTag record(String id){for(var value:state.getList("entries",Tag.TAG_COMPOUND)){var row=(CompoundTag)value;if(row.getString("entry").equals(id))return row;}return new CompoundTag();}
    private CompoundTag bound(int slot){return record(state.getList("bindings",Tag.TAG_STRING).getString(slot));}
    private static String name(CompoundTag row){return LughArts.techniqueName(row);}
    private void filter(){
        filtered.clear();for(var value:state.getList("entries",Tag.TAG_COMPOUND)){var row=(CompoundTag)value;
            if((row.getString("name")+" "+name(row)+" "+row.getString("type")+" "+LughArts.formName(row.getInt("form"))).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))filtered.add(row);
        }
        if(!selected.isEmpty() && filtered.stream().noneMatch(row->row.getString("entry").equals(selected))){selected="";detailStart=0;}
        start=Math.clamp(start,0,Math.max(0,filtered.size()-6));offset=filtered.size()>6?(float)start/(filtered.size()-6):0;
    }
    @Override protected void init(){
        super.init();guiLeft--;
        search=new ExpandedEditBox(font,guiLeft+19,guiTop+27,79,9,Component.literal("Search appraised skills"));search.setBordered(false);search.setMaxLength(128);
        search.setHint(Component.literal("Search").withColor(0xAAAAAA));search.setValue(query);
        search.setResponder(text->{query=text;start=0;filter();});addRenderableWidget(search);
        for(int n=0;n<3;n++){
            final int index=n;
            var button=new LibraryButton(guiLeft+6+n*30,guiTop+129,28,11,Component.literal(numeral(n)),Component.empty(),b->request("lugh_select",Integer.toString(index)));
            button.setUvOffsetCheck(()->slot()==index || button.isHovered());addRenderableWidget(button);
        }
        addRenderableWidget(new LibraryButton(guiLeft+96,guiTop+129,13,11,Component.literal("›"),Component.literal("Next Art slot"),b->request("lugh_select",Integer.toString((slot()+1)%3))));
        var bind=new LibraryButton(guiLeft+119,guiTop+128,106,11,Component.literal("Bind selected skill"),Component.literal("Bind the highlighted library entry to the selected Art slot.\nGrand Synthesis uses all three bindings.\nScroll the right panel to inspect all parameters."),b->{if(!record(selected).isEmpty())request("lugh_bind",slot()+"|"+selected);});
        bind.setActiveCheck(()->!record(selected).isEmpty());addRenderableWidget(bind);
        addRenderableWidget(new LibraryButton(guiLeft+197,guiTop+11,20,20,Component.literal("×"),Component.literal("Close appraisal library"),b->onClose()));
    }
    @Override public void tick(){super.tick();if(++syncTicks%20==0)request("lugh_sync","");}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        g.blit(BACKGROUND,guiLeft,guiTop,0,0,imageWidth,imageHeight);
        RenderHelper.drawCenteredText(g,font,title,guiLeft+57,guiTop+7,0xFFFFFF,true);
        for(var widget:renderables)if(widget instanceof SimpleButton button){
            if(button.getY()==guiTop+129 && button.getX()<guiLeft+96){int n=(button.getX()-guiLeft-6)/30;button.setMessage(Component.literal(numeral(n)).withColor(slot()==n?0x83EC80:0xD7CFB8));}
            else if(button.getX()==guiLeft+119)button.setMessage(Component.literal("Bind to Art "+numeral(slot())));
        }
        for(int n=start;n<Math.min(start+6,filtered.size());n++){
            var row=filtered.get(n);int x=guiLeft+6,y=guiTop+43+(n-start)*13;boolean hover=inside(mx,my,6,43+(n-start)*13,89,13),chosen=row.getString("entry").equals(selected);
            g.blit(ABILITY_BAR,x,y,0,hover || chosen?13:0,89,13,89,26);
            RenderHelper.drawShortenedText(g,font,Component.literal(name(row)).withColor(chosen?0xF4E7A1:0xD7CFB8),x+3,y+3,80,0,true);
            if(hover)setTooltipForNextRenderPass(Component.literal(row.getString("name")+"\n"+name(row)+"\n"+row.getString("source")));
        }
        if(filtered.isEmpty())g.drawString(font,"No matches",guiLeft+9,guiTop+47,0xAAAAAA,false);
        renderScrollBar(g,mx,my);
        var row=record(selected);
        if(!row.isEmpty()){
            var source=ResourceLocation.tryParse(row.getString("source"));var skill=source==null?null:SkillAPI.getSkillRegistry().get(source);var icon=skill==null?WildHunt.id("textures/skill/lugh.png"):skill.getSkillIcon();
            if(icon!=null)g.blit(icon,guiLeft+156,guiTop+6,0,0,32,32,32,32);
            if(inside(mx,my,156,6,32,32))setTooltipForNextRenderPass(Component.literal(row.getString("name")+"\n"+name(row)));
        }
        // Keep the clipping/render state identical for empty and selected detail panels.
        g.enableScissor(guiLeft+123,guiTop+48,guiLeft+221,guiTop+117);
        if(!row.isEmpty()){
            var lines=details(row);detailStart=Math.clamp(detailStart,0,Math.max(0,lines.size()-7));
            for(int n=detailStart;n<Math.min(detailStart+7,lines.size());n++)g.drawString(font,lines.get(n),guiLeft+123,guiTop+48+(n-detailStart)*9,0xD7CFB8,false);
        }
        g.disableScissor();
        // Native preset strip is repurposed as the three reconstruction bindings.
        for(int n=0;n<3;n++)if(inside(mx,my,6+n*30,129,28,11))setTooltipForNextRenderPass(Component.literal("Art "+numeral(n)+(slot()==n?" (active)":"")+"\n"+name(bound(n))+"\nSelect this slot, then Bind selected skill."));
        if(inside(mx,my,96,129,13,11))setTooltipForNextRenderPass(Component.literal(state.getList("entries",Tag.TAG_COMPOUND).size()+" / "+LughArts.LIBRARY_LIMIT+" techniques\nNext Art slot. Shift + Reconstructed Art also cycles the three bindings."));
        // Flush the background even with no selected record, before native button batches.
        g.flush();
    }
    private static String number(float value){return String.format(Locale.ROOT,"%,.1f",value).replace(".0","");}
    private List<FormattedCharSequence> details(CompoundTag row){
        String geometry=switch(row.getInt("form")){case 0->"Range: 16 blocks; forward fan";case 1->"Range: 64 blocks; width 1.4";default->"Range: 48 blocks; radius 9";};
        String type=row.getString("type"),simple=type.substring(type.indexOf(':')+1).replace('_',' ');
        var text="Damage: "+number(row.getFloat("damage"))+" raw\nAt mastery: "+number(row.getFloat("damage")*state.getFloat("mastery_scale"))+"\nType: "+simple+"\nForm: "+LughArts.formName(row.getInt("form"))+"\n"+geometry+"\nTargets: 8\nBase cost: 10,000 MP\nCooldown: 4 s\nSynthesis: 1.6× raw / phase\n";
        if(row.getBoolean("observed"))text+="Observed damage: "+number(row.getFloat("observed_damage"))+"\nSource type: "+row.getString("source_type")+"\n";
        else text+="Origin / legacy record\n";
        text+="Skill: "+row.getString("name")+"\nTechnique: "+name(row)+"\nID: "+row.getString("source")+"\nDamage ID: "+type+"\nFinal damage also uses native modifiers and Godly traits. Reconstructs damage and form; does not grant the original skill's utility.";
        return font.split(Component.literal(text),98);
    }
    private boolean inside(double x,double y,int dx,int dy,int w,int h){return x>=guiLeft+dx && x<guiLeft+dx+w && y>=guiTop+dy && y<guiTop+dy+h;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0 && inside(x,y,6,43,89,78)){int n=start+((int)y-guiTop-43)/13;if(n<filtered.size()){selected=filtered.get(n).getString("entry");detailStart=0;}search.setFocused(false);return true;}
        if(button==0 && isScrollBarActive() && clickedScrollBar(x,y)){draggedScrollBar(y);return true;}
        if(!inside(x,y,19,27,79,9))search.setFocused(false);return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
        if(inside(x,y,119,43,106,78)){detailStart=Math.max(0,detailStart-(int)Math.signum(dy));return true;}
        return inside(x,y,4,41,106,82)?scrolledScrollBar(dy):super.mouseScrolled(x,y,dx,dy);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return button==0 && draggedScrollBar(y) || super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){scrolling=false;return super.mouseReleased(x,y,button);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(search.keyPressed(key,scan,modifiers))return true;if(search.isFocused() && key!=256)return true;return super.keyPressed(key,scan,modifiers);}
    @Override public int getScrollBarX(){return guiLeft+98;}
    @Override public int getScrollBarY(){return guiTop+43;}
    @Override public int getScrollBarTotalSpace(){return 78;}
    @Override public int getScrollBarListSize(){return filtered.size();}
    @Override public int getScrollBarRenderCount(){return 6;}
    @Override public float getScrollOffset(){return offset;}
    @Override public boolean isScrolling(){return scrolling;}
    @Override public void setListStartIndex(int value){start=value;}
    @Override public void setScrolling(boolean value){scrolling=value;}
    @Override public void setScrollOffset(float value){offset=value;}
}
