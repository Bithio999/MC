package dev.wildhunt.client.guide;

import dev.wildhunt.WildHunt;
import dev.wildhunt.guide.GuideCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Searchable two-panel guide, entirely client-side; no browser or server request. */
public final class GuideScreen extends Screen {
    private static final int INK=0xff101721,PANEL=0xff182330,LINE=0xff344554,ACCENT=0xffe4bc78,TEXT=0xffe6e9e9,MUTED=0xffa6b7c2;
    private static String last="welcome";
    private final Screen parent;
    private final GuideCatalog catalog;
    private final Deque<String> back=new ArrayDeque<>(),forward=new ArrayDeque<>();
    private String selected=last,category="All",query="";
    private List<GuideCatalog.Entry> results=List.of();
    private EditBox search;
    private final List<GuideButton> rows=new ArrayList<>();
    private final List<Line> content=new ArrayList<>();
    private int drag;
    private GuideButton previous,next;
    private int x,y,w,h,side,listTop,listBottom,articleX,articleY,articleW,articleBottom,listStart,pageScroll,totalHeight;
    private record Line(FormattedCharSequence text,int color,int top,int height,String link){}
    public GuideScreen(Screen parent){super(Component.literal("Tensura Myths Guide"));this.parent=parent;catalog=load();selected=catalog.get(selected).id();}
    private static GuideCatalog load(){
        try(var in=Minecraft.getInstance().getResourceManager().getResourceOrThrow(WildHunt.id("guide/entries.json")).open()){
            return GuideCatalog.parse(new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }catch(Exception e){return GuideCatalog.parse("{\"entries\":[{\"id\":\"welcome\",\"title\":\"Guide unavailable\",\"category\":\"Start\",\"summary\":\"The guide resource could not be read.\",\"sections\":[{\"title\":\"Try again\",\"lines\":[\"Reload resources with F3 + T, then reopen the guide. If using a resource pack, check its guide file.\"]}]}]}");}
    }
    private final class GuideButton extends Button {
        private final Runnable action;private final BooleanSupplier chosen;
        GuideButton(int bx,int by,int bw,int bh,String label,Runnable action,BooleanSupplier chosen){super(bx,by,bw,bh,Component.literal(label),b->action.run(),DEFAULT_NARRATION);this.action=action;this.chosen=chosen;}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            boolean on=chosen.getAsBoolean();g.fill(getX(),getY(),getX()+width,getY()+height,on?0xff354551:isHoveredOrFocused()?0xff2b3b49:PANEL);
            if(on)g.fill(getX(),getY(),getX()+2,getY()+height,ACCENT);
            String label=getMessage().getString();if(font.width(label)>width-10)label=font.plainSubstrByWidth(label,width-16)+"…";
            g.drawString(font,label,getX()+6,getY()+(height-8)/2,on?ACCENT:active?TEXT:MUTED,false);
            if(isFocused())g.renderOutline(getX(),getY(),width,height,ACCENT);
        }
    }
    private GuideButton button(int bx,int by,int bw,int bh,String label,Runnable action,BooleanSupplier chosen){return addRenderableWidget(new GuideButton(bx,by,bw,bh,label,action,chosen));}
    @Override protected void init(){
        w=Math.min(600,width-20);h=Math.min(340,height-20);x=(width-w)/2;y=(height-h)/2;side=Math.clamp(w/3,100,184);
        listTop=y+90;listBottom=y+h-23;articleX=x+side+22;articleY=y+112;articleW=w-side-36;articleBottom=y+h-25;
        previous=button(x+9,y+9,23,20,"<",()->history(false),()->false);previous.setTooltip(Tooltip.create(Component.literal("Previous page")));
        next=button(x+35,y+9,23,20,">",()->history(true),()->false);next.setTooltip(Tooltip.create(Component.literal("Next page")));
        button(x+w-29,y+9,20,20,"×",this::onClose,()->false).setTooltip(Tooltip.create(Component.literal("Close guide")));
        search=new EditBox(font,x+12,y+38,w-24,20,Component.literal("Search guide"));search.setMaxLength(128);search.setHint(Component.literal("Search names, abilities or requirements…"));search.setValue(query);
        search.setResponder(value->{query=value;listStart=0;filter();});addRenderableWidget(search);
        var cats=new ArrayList<String>();cats.add("All");cats.addAll(catalog.categories());int cw=(w-24)/cats.size();
        for(int n=0;n<cats.size();n++){String c=cats.get(n);button(x+12+n*cw,y+64,cw-2,19,c,()->{category=c;listStart=0;filter();},()->category.equals(c));}
        rows.clear();int count=Math.max(1,(listBottom-listTop)/23);
        for(int n=0;n<count;n++){final int slot=n;rows.add(button(x+10,listTop+n*23,side,21,"",()->{int index=listStart+slot;if(index<results.size())select(results.get(index).id(),true);},()->{int index=listStart+slot;return index<results.size() && results.get(index).id().equals(selected);}));}
        filter();layout();
    }
    private void filter(){results=catalog.search(query,category);listStart=Math.clamp(listStart,0,Math.max(0,results.size()-rows.size()));updateRows();if(!results.isEmpty() && results.stream().noneMatch(e->e.id().equals(selected)))select(results.getFirst().id(),false);}
    private void updateRows(){for(int n=0;n<rows.size();n++){int at=listStart+n;var b=rows.get(n);b.visible=at<results.size();b.active=b.visible;if(b.visible){var e=results.get(at);b.setMessage(Component.literal(e.title()));b.setTooltip(Tooltip.create(Component.literal(e.title()+" · "+e.category())));}}}
    public void select(String id,boolean remember){String next=catalog.get(id).id();if(remember && !selected.equals(next)){back.push(selected);forward.clear();}selected=next;last=next;pageScroll=0;layout();if(previous!=null){previous.active=!back.isEmpty();this.next.active=!forward.isEmpty();}}
    private void history(boolean next){var from=next?forward:back;var to=next?back:forward;if(from.isEmpty())return;to.push(selected);select(from.pop(),false);}
    private void paragraph(String text,int color,String link){for(var line:font.split(Component.literal(text),articleW-12)){content.add(new Line(line,color,totalHeight,11,link));totalHeight+=11;}totalHeight+=5;}
    private void layout(){if(font==null)return;content.clear();totalHeight=0;var e=catalog.get(selected);paragraph(e.summary(),TEXT,null);
        for(var section:e.sections()){totalHeight+=7;paragraph(section.title().toUpperCase(Locale.ROOT),ACCENT,null);for(String text:section.lines())paragraph(text,TEXT,null);}
        if(!e.related().isEmpty()){totalHeight+=8;paragraph("RELATED",ACCENT,null);for(String id:e.related())paragraph("› "+catalog.get(id).title(),0xff9cd6cb,id);}
        pageScroll=Math.clamp(pageScroll,0,Math.max(0,totalHeight-(articleBottom-articleY)));
    }
    private void scrollPage(int amount){pageScroll=Math.clamp(pageScroll+amount,0,Math.max(0,totalHeight-(articleBottom-articleY)));}
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){
        if(mx>=x && mx<x+side+14 && my>=listTop && my<=listBottom){listStart=Math.clamp(listStart-(int)Math.signum(dy)*3,0,Math.max(0,results.size()-rows.size()));updateRows();return true;}
        if(mx>=articleX && mx<x+w && my>=articleY && my<=articleBottom){scrollPage(-(int)(dy*30));return true;}return super.mouseScrolled(mx,my,dx,dy);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0 && my>=articleY && my<=articleBottom && mx>=x+w-16 && mx<=x+w-8){drag=2;dragTo(my);return true;}
        if(button==0 && my>=listTop && my<=listBottom && mx>=x+side+7 && mx<=x+side+14){drag=1;dragTo(my);return true;}
        if(button==0 && mx>=articleX && mx<articleX+articleW && my>=articleY && my<articleBottom){for(var line:content)if(line.link()!=null && my>=articleY+line.top()-pageScroll && my<articleY+line.top()-pageScroll+line.height()){
            query="";category="All";search.setValue("");filter();select(line.link(),true);int index=results.indexOf(catalog.get(selected));listStart=Math.clamp(index,0,Math.max(0,results.size()-rows.size()));updateRows();return true;}}
        return super.mouseClicked(mx,my,button);
    }
    private void dragTo(double my){if(drag==1){listStart=(int)Math.round(Math.clamp((my-listTop)/(listBottom-listTop),0,1)*Math.max(0,results.size()-rows.size()));updateRows();}else if(drag==2)pageScroll=(int)Math.round(Math.clamp((my-articleY)/(articleBottom-articleY),0,1)*Math.max(0,totalHeight-(articleBottom-articleY)));}
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(drag!=0){dragTo(my);return true;}return super.mouseDragged(mx,my,button,dx,dy);}
    @Override public boolean mouseReleased(double mx,double my,int button){if(drag!=0){drag=0;return true;}return super.mouseReleased(mx,my,button);}
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_F && hasControlDown()){setFocused(search);search.setFocused(true);return true;}
        if(key==GLFW.GLFW_KEY_PAGE_DOWN){scrollPage(articleBottom-articleY-12);return true;}if(key==GLFW.GLFW_KEY_PAGE_UP){scrollPage(-(articleBottom-articleY-12));return true;}
        if(key==GLFW.GLFW_KEY_LEFT && hasAltDown()){history(false);return true;}if(key==GLFW.GLFW_KEY_RIGHT && hasAltDown()){history(true);return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        previous.active=!back.isEmpty();next.active=!forward.isEmpty();g.fill(0,0,width,height,0xc8070c12);g.fill(x-1,y-1,x+w+1,y+h+1,LINE);g.fill(x,y,x+w,y+h,INK);g.fill(x,y,x+w,y+2,ACCENT);
        String label=w<420?"Tensura Myths":"Tensura Myths  /  Field Guide";g.drawString(font,label,x+67,y+15,ACCENT,false);
        g.fill(x+8,listTop-2,x+side+12,listBottom+1,PANEL);g.fill(articleX-8,listTop-2,x+w-9,articleBottom+5,PANEL);
        var e=catalog.get(selected);int hx=articleX+26;try{g.blit(ResourceLocation.parse(e.icon()),articleX,y+86,0,0,20,20,20,20);}catch(Exception ignored){}
        String title=font.plainSubstrByWidth(e.title(),Math.max(20,articleW-30));g.drawString(font,title,hx,y+87,TEXT,false);
        // Header occupies a separate strip; the body begins below it.
        articleY=y+112;g.enableScissor(articleX,articleY,articleX+articleW,articleBottom);
        for(var line:content){int ly=articleY+line.top()-pageScroll;if(ly+11>=articleY && ly<articleBottom)g.drawString(font,line.text(),articleX,ly,line.color(),false);}g.disableScissor();
        if(totalHeight>articleBottom-articleY){int vh=articleBottom-articleY,thumb=Math.max(14,vh*vh/totalHeight),bar=y+112+(vh-thumb)*pageScroll/Math.max(1,totalHeight-vh);g.fill(x+w-13,articleY,x+w-11,articleBottom,LINE);g.fill(x+w-13,bar,x+w-11,bar+thumb,ACCENT);}
        if(results.size()>rows.size()){int vh=listBottom-listTop,thumb=Math.max(12,vh*rows.size()/results.size());int bar=listTop+(vh-thumb)*listStart/Math.max(1,results.size()-rows.size());g.fill(x+side+10,bar,x+side+12,bar+thumb,MUTED);}
        if(results.isEmpty())g.drawWordWrap(font,Component.literal("No matches. Try another search or category."),x+17,listTop+10,side-14,MUTED);
        g.drawString(font,results.size()==1?"1 page":results.size()+" pages",x+12,y+h-15,MUTED,false);String hint="Ctrl+F search  ·  Esc back";g.drawString(font,hint,articleX,y+h-15,MUTED,false);
        super.render(g,mx,my,partial);
    }
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return true;}
}
