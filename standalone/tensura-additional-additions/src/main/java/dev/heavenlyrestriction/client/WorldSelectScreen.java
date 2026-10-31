package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.worldhop.WorldHopNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Searchable, keyboard-accessible atlas; choosing a card never starts a transfer. */
public final class WorldSelectScreen extends Screen {
    private record World(String id,String name,long seed,boolean available){}
    private final List<World> worlds=new ArrayList<>();
    private final List<Button> rows=new ArrayList<>();
    private String selected="",picked="",message="",query="";
    private boolean error,pending;
    private int x,y,w,h,listW,page,pageSize,worldCount=1,limit=128;
    private EditBox search;
    private Button previous,next,apply;
    private static final int TEXT=0xffe2eafa,MUTED=0xff9aaac6,GOLD=0xffe9c985;
    public WorldSelectScreen(CompoundTag state){super(Component.literal("World Select"));read(state);picked=selected;}
    public static void receive(CompoundTag state){
        var mc=Minecraft.getInstance();if(state==null || mc.getSingleplayerServer()==null || mc.getSingleplayerServer().isPublished())return;
        if(state.getBoolean("open"))mc.setScreen(new WorldSelectScreen(state));
        else if(mc.screen instanceof WorldSelectScreen screen){screen.read(state);screen.pending=false;screen.refresh();}
    }
    private void read(CompoundTag state){
        worlds.clear();worlds.add(new World("","New World",0,true));
        for(var tag:state.getList("worlds",Tag.TAG_COMPOUND)){var row=(CompoundTag)tag;worlds.add(new World(row.getString("id"),row.getString("name"),row.getLong("seed"),row.getBoolean("available")));}
        selected=state.getString("selected");message=state.getString("message");error=state.getBoolean("error");worldCount=state.getInt("count");limit=state.contains("limit")?state.getInt("limit"):128;
    }
    private List<World> filtered(){String q=query.strip().toLowerCase(Locale.ROOT);return worlds.stream().filter(e->e.name.toLowerCase(Locale.ROOT).contains(q) || !e.id.isEmpty() && Long.toString(e.seed).contains(q)).toList();}
    private World chosen(){return worlds.stream().filter(e->e.id.equals(picked)).findFirst().orElse(null);}
    private String fit(String text,int pixels){return font.width(text)<=pixels?text:font.plainSubstrByWidth(text,Math.max(0,pixels-9))+"…";}
    @Override protected void init(){
        rows.clear();
        w=Math.min(490,width-16);h=Math.min(306,height-16);x=(width-w)/2;y=(height-h)/2;listW=(int)(w*.55);pageSize=Math.max(1,(h-116)/43);
        search=addRenderableWidget(new EditBox(font,x+12,y+49,listW-20,18,Component.literal("Search visited worlds")));search.setMaxLength(128);search.setHint(Component.literal("Search name or seed…"));search.setValue(query);search.setResponder(value->{query=value;page=0;refresh();});
        for(int n=0;n<pageSize;n++){final int index=n;rows.add(addRenderableWidget(new AtlasButton(x+10,y+76+n*43,listW-16,38,"",()->{var list=filtered();int at=page*pageSize+index;if(at<list.size()){picked=list.get(at).id;refresh();}},index)));}
        previous=addRenderableWidget(new AtlasButton(x+10,y+h-29,24,19,"‹",()->{page--;refresh();},-1));
        next=addRenderableWidget(new AtlasButton(x+listW-30,y+h-29,24,19,"›",()->{page++;refresh();},-1));
        int right=x+listW+10,rw=w-listW-20;
        apply=addRenderableWidget(new AtlasButton(right,y+h-53,rw,20,"Set next jump",()->{pending=true;apply.active=false;message="Saving destination…";PacketDistributor.sendToServer(new WorldHopNetwork.Select(picked));},-1));
        addRenderableWidget(new AtlasButton(right,y+h-28,rw,18,"Done",this::onClose,-1));refresh();
    }
    private void refresh(){
        if(search==null)return;var list=filtered();int pages=Math.max(1,(list.size()+pageSize-1)/pageSize);page=Math.clamp(page,0,pages-1);
        for(int n=0;n<rows.size();n++){var button=rows.get(n);int at=page*pageSize+n;button.visible=at<list.size();if(button.visible){var world=list.get(at);button.setMessage(Component.literal(world.name+(world.available?"":" (missing)")));button.setTooltip(Tooltip.create(Component.literal(world.id.isEmpty()?"Generate every dimension with a fresh seed.":world.name+"\nSeed: "+world.seed+(world.available?"":"\nSave is unavailable."))));}}
        if(previous!=null){previous.active=page>0;next.active=page+1<pages;var choice=chosen();apply.active=!pending && !error && choice!=null && choice.available && !picked.equals(selected);apply.setMessage(Component.literal(picked.equals(selected)?"Destination set":"Set next jump"));}
    }
    private final class AtlasButton extends Button {
        private final int row;
        AtlasButton(int bx,int by,int bw,int bh,String label,Runnable action,int row){super(bx,by,bw,bh,Component.literal(label),b->action.run(),DEFAULT_NARRATION);this.row=row;}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            World world=null;if(row>=0){var list=filtered();int index=page*pageSize+row;if(index<list.size())world=list.get(index);}
            boolean chosen=world!=null && world.id.equals(picked);int border=chosen?GOLD:isHoveredOrFocused()?0xff7798c5:0xff344866;
            g.fill(getX(),getY(),getX()+width,getY()+height,border);g.fill(getX()+1,getY()+1,getX()+width-1,getY()+height-1,chosen?0xff24324a:0xff131f34);
            if(world==null){g.drawCenteredString(font,getMessage(),getX()+width/2,getY()+(height-8)/2,active?TEXT:0xff65738b);return;}
            int cx=getX()+18,cy=getY()+18;sigil(g,cx,cy,world.id.isEmpty()?0x67c6d1:0x8e9be9);
            g.drawString(font,fit(world.name,width-47),getX()+38,getY()+7,world.available?TEXT:0xffbc8181,false);
            String detail=world.id.isEmpty()?"Fresh seed · All dimensions":world.available?"Visited world · Return":"Save unavailable";
            g.drawString(font,fit(detail,width-47),getX()+38,getY()+22,MUTED,false);
            if(world.id.equals(selected))g.fill(getX()+1,getY()+1,getX()+3,getY()+height-1,GOLD);
        }
    }
    private static void sigil(GuiGraphics g,int cx,int cy,int color){
        for(int n=0;n<13;n++){int half=6-Math.abs(6-n);g.fill(cx-half,cy-6+n,cx+half+1,cy-5+n,0xff000000|color);}
        g.fill(cx-2,cy-3,cx+3,cy+4,0xff122139);g.fill(cx-9,cy,cx-6,cy+1,0xffcadcf8);g.fill(cx+7,cy,cx+10,cy+1,0xffcadcf8);
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xb5091020);g.fill(x-1,y-1,x+w+1,y+h+1,0xff506582);g.fillGradient(x,y,x+w,y+h,0xff14243a,0xff090f20);
        g.fill(x+1,y+1,x+w-1,y+2,GOLD);g.drawString(font,"WORLD SELECT",x+12,y+12,GOLD,false);
        String target=worlds.stream().filter(e->e.id.equals(selected)).map(e->e.name).findFirst().orElse("Unavailable world");
        g.drawString(font,fit("Next jump: "+target,w-24),x+12,y+29,MUTED,false);g.fill(x+listW,y+47,x+listW+1,y+h-9,0xff34435e);
        int right=x+listW+10,rw=w-listW-20;var world=chosen();
        g.drawString(font,"DESTINATION",right,y+51,GOLD,false);
        if(world!=null){
            g.drawString(font,fit(world.name,rw),right,y+69,TEXT,false);
            if(!world.id.isEmpty()){g.drawString(font,"WORLD SEED",right,y+89,MUTED,false);g.drawString(font,fit(Long.toString(world.seed),rw),right,y+102,0xffbfcdf4,false);}
            String detail=world.id.isEmpty()?"Create a fresh world. All dimensions regenerate. Your current character travels with you.":"Return to saved terrain, builds and entities. Bring your current inventory, skills and progression.";
            if(h<270)detail=world.id.isEmpty()?"Fresh seed. All dimensions regenerate.":"Saved terrain. Your current character travels with you.";
            if(world.id.isEmpty() && worldCount>=limit)detail="Library full. Choose a visited world to return.";
            int textY=y+(world.id.isEmpty()?92:122);g.drawWordWrap(font,Component.literal(detail),right,textY,rw,MUTED);
        }
        int noteY=y+h-89;
        if(h>=270)g.drawWordWrap(font,Component.literal("Used once, on your next World Jump."),right,noteY,rw,0xffb9a581);
        String status=world!=null && !world.available?"This save is unavailable.":message.isEmpty()?"Selecting here does not jump.":message;
        g.drawString(font,fit(status,rw),right,y+h-66,error?0xffe9a29a:0xffa6cbbc,false);
        var filtered=filtered();int pages=Math.max(1,(filtered.size()+pageSize-1)/pageSize);
        g.drawCenteredString(font,(page+1)+"/"+pages+" · "+worldCount+"/"+limit+" worlds",x+listW/2,y+h-23,MUTED);
        if(filtered.isEmpty())g.drawCenteredString(font,"No matching worlds",x+listW/2,y+91,MUTED);
        super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){
        if(mx>=x && mx<x+listW && my>=y+76 && my<y+h-30 && dy!=0){page-=Math.signum(dy);refresh();return true;}return super.mouseScrolled(mx,my,dx,dy);
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_F && hasControlDown()){setFocused(search);search.setFocused(true);return true;}
        return super.keyPressed(key,scan,mods);
    }
}
