package dev.heavenlyrestriction.client;
import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.loans.LoanNetwork;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Native Tensura buttons, understated grey panels, skill icons and a searchable six-row ledger. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class LoanScreen extends Screen {
    private static final int GOLD=0xffe7c27a,TEXT=0xffe4e0eb,MUTED=0xffa6a0b0,GREEN=0xff91d8b7;
    private CompoundTag state;private String selected="",query="";private int x,y,w,h,left,rowH,age;private boolean pending,confirm;private EditBox search;private Button action,previous,next;private final List<Button> rows=new ArrayList<>();
    public LoanScreen(CompoundTag state){super(Component.literal("Loan Shark"));this.state=state.copy();query=state.getString("query");}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->LoanNetwork.receiver=LoanScreen::receive);}
    public static void receive(CompoundTag state){var mc=Minecraft.getInstance();if(state==null)return;if(state.getBoolean("offer")){mc.setScreen(new GiftOffer(mc.screen,state));return;}if(state.getBoolean("open"))mc.setScreen(new LoanScreen(state));else if(mc.screen instanceof LoanScreen screen && screen.state.getUUID("session").equals(state.getUUID("session"))){screen.state=state.copy();screen.pending=false;screen.refresh();}}
    private boolean report(){return state.getInt("mode")==1;}
    private ListTag data(){return state.getList("rows",Tag.TAG_COMPOUND);}
    private CompoundTag chosen(){for(var t:data())if(((CompoundTag)t).getString("id").equals(selected))return (CompoundTag)t;return null;}
    private String fit(String text,int pixels){return font.width(text)<=pixels?text:font.plainSubstrByWidth(text,Math.max(0,pixels-9))+"…";}
    private void request(String action,String value,int page){if(pending)return;pending=true;confirm=false;PacketDistributor.sendToServer(new LoanNetwork.Request(state.getUUID("session"),action,value,query,page));refresh();}
    private static class LoanButton extends SimpleButton {
        LoanButton(int x,int y,int w,String label,Runnable action){super(x,y,w,18,Component.literal(label),Component.literal(label),b->action.run());setActiveCheck(()->active);setClickedCheck(()->active && visible && isHovered());}
        @Override public void setMessage(Component message){super.setMessage(message);this.text=message;}
        @Override public boolean mouseClicked(double mx,double my,int button){return active && visible && super.mouseClicked(mx,my,button);}
    }
    private Button button(int bx,int by,int bw,String label,Runnable call){return addRenderableWidget(new LoanButton(bx,by,bw,label,call));}
    @Override protected void init(){rows.clear();w=Math.min(438,width-12);h=Math.min(300,height-12);x=(width-w)/2;y=(height-h)/2;left=w*55/100;rowH=Math.max(13,(h-108)/6);
        button(x+8,y+22,66,"Loan",()->request("loan_tab","",0));button(x+78,y+22,66,"Report",()->request("report_tab","",0));
        search=addRenderableWidget(new EditBox(font,x+9,y+47,left-17,16,Component.literal("Search skills or recipients")));search.setMaxLength(80);search.setHint(Component.literal(report()?"Search skill or recipient…":"Search owned skills…"));search.setValue(query);search.setResponder(value->{query=value;age=-5;confirm=false;});
        for(int n=0;n<6;n++){final int index=n;rows.add(addRenderableWidget(new EntryButton(x+8,y+69+n*rowH,left-15,rowH-2,index)));}
        previous=button(x+8,y+h-27,26,"‹",()->request("refresh","",state.getInt("page")-1));next=button(x+left-34,y+h-27,26,"›",()->request("refresh","",state.getInt("page")+1));
        action=button(x+left+10,y+h-51,w-left-20,report()?"Claim":"Send gift",()->{var row=chosen();if(row==null)return;if(report() && !confirm){confirm=true;refresh();return;}request(report()?"claim":"lend",row.getString("id"),state.getInt("page"));});
        button(x+left+10,y+h-27,w-left-20,"Close",this::onClose);refresh();
    }
    private void refresh(){if(action==null)return;var data=data();for(int n=0;n<rows.size();n++){rows.get(n).visible=n<data.size();if(n<data.size()){var r=data.getCompound(n);rows.get(n).setTooltip(Tooltip.create(Component.literal(r.getString("name")+(report()?"\n"+r.getString("borrower")+"\nEP "+String.format(Locale.ROOT,"%,.2f",r.getDouble("ep"))+"\nUse "+duration(r.getLong("used"))+"\nIdle "+duration(r.getLong("idle"))+"\nLast / longest gap "+duration(r.getLong("gap"))+" / "+duration(r.getLong("longestGap")):"\n"+r.getString("status")))));}}if(chosen()==null){selected=data.isEmpty()?"":data.getCompound(0).getString("id");confirm=false;}previous.active=!pending && state.getInt("page")>0;next.active=!pending && state.getInt("page")+1<state.getInt("pages");var row=chosen();action.active=!pending && row!=null && row.getBoolean("enabled");action.setMessage(Component.literal(confirm?"Confirm claim":report()?"Claim":"Send gift"));}
    @Override public void tick(){if(++age==6 && !pending)request("refresh","",0);else if(age>0 && age%20==0 && !pending)request("refresh","",state.getInt("page"));}
    private final class EntryButton extends SimpleButton {
        private final int index;
        EntryButton(int bx,int by,int bw,int bh,int index){super(bx,by,bw,bh,Component.empty(),Component.literal("Select a skill"),b->{selected=data().getCompound(index).getString("id");confirm=false;refresh();});this.index=index;}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){if(index>=data().size())return;var row=data().getCompound(index);boolean chosen=row.getString("id").equals(selected);int color=chosen?GOLD:isHoveredOrFocused()?0xffa69cb5:0xff5b5662;g.fill(getX(),getY(),getX()+width,getY()+height,color);g.fill(getX()+1,getY()+1,getX()+width-1,getY()+height-1,chosen?0xff38333d:0xff28262c);int size=Math.min(24,height-4);icon(g,row.getString("skill"),getX()+3,getY()+2,size);int textX=getX()+size+7;g.drawString(font,fit(row.getString("name"),width-size-13),textX,getY()+3,chosen?GOLD:TEXT,false);if(height>=25)g.drawString(font,fit(report()?row.getString("borrower")+" · "+row.getString("status"):row.getString("status"),width-size-13),textX,getY()+15,MUTED,false);}
    }
    private static void icon(GuiGraphics g,String skillId,int x,int y,int size){var id=ResourceLocation.tryParse(skillId);if(id==null)return;var skill=SkillAPI.getSkillRegistry().get(id);if(skill!=null)g.blit(skill.getSkillIcon(),x,y,0,0,size,size,size,size);}
    private static String duration(long ticks){long sec=Math.max(0,ticks)/20;return sec>=3600?String.format(Locale.ROOT,"%dh %02dm",sec/3600,sec/60%60):String.format(Locale.ROOT,"%dm %02ds",sec/60,sec%60);}
    private void line(GuiGraphics g,String label,String value,int py,int color){int px=x+left+10,rw=w-left-20;g.drawString(font,fit(label,rw),px,py,MUTED,false);g.drawString(font,fit(value,rw),px,py+10,color,false);}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){g.fill(0,0,width,height,0xa0100e18);g.fill(x-2,y-2,x+w+2,y+h+2,0xff17131e);g.fill(x-1,y-1,x+w+1,y+h+1,0xff716379);g.fill(x,y,x+w,y+h,0xff302c34);g.fill(x+2,y+2,x+w-2,y+18,0xff211b29);g.drawString(font,"LOAN SHARK",x+9,y+6,GOLD,false);g.drawString(font,report()?"REPORT":"SKILL GIFT",x+w-90,y+6,MUTED,false);g.fill(x+left,y+44,x+left+1,y+h-7,0xff64556d);g.fill(x+(report()?78:8),y+41,x+(report()?144:74),y+42,GOLD);
        int rx=x+left+10,rw=w-left-20;var row=chosen();if(row!=null){int size=h<240?20:30;icon(g,row.getString("skill"),rx,y+48,size);g.drawString(font,fit(row.getString("name"),rw-size-6),rx+size+5,y+50,GOLD,false);g.drawString(font,fit(report()?row.getString("status"):state.getString("target"),rw-size-6),rx+size+5,y+62,MUTED,false);
            int top=y+(h<240?78:88),gap=24;
            if(report() && h<230){String[] compact={row.getString("borrower"),String.format(Locale.ROOT,"EP %,.1f",row.getDouble("ep")),"Use "+duration(row.getLong("used")),"Idle "+duration(row.getLong("idle"))};for(int n=0;n<compact.length;n++)g.drawString(font,fit(compact[n],rw),rx,y+75+n*10,n==1?GREEN:TEXT,false);}
            else if(report()){line(g,"RECIPIENT",row.getString("borrower"),top,TEXT);line(g,"EP TRANSFERRED",String.format(Locale.ROOT,"%,.2f EP",row.getDouble("ep")),top+gap,GREEN);line(g,"TOTAL USE",duration(row.getLong("used")),top+gap*2,TEXT);line(g,"CURRENT IDLE",duration(row.getLong("idle")),top+gap*3,row.getBoolean("steal")?GOLD:TEXT);if(h>=270)line(g,"LAST / LONGEST IDLE GAP",duration(row.getLong("gap"))+" / "+duration(row.getLong("longestGap")),top+gap*4,TEXT);if(h>=290)g.drawString(font,fit(row.getString("stolen").isEmpty()?"Pending credit: "+String.format(Locale.ROOT,"%,.1f",state.getDouble("pending"))+" EP":(row.getString("status").equals("Copied")?"Copied: ":"Claimed: ")+row.getString("stolen"),rw),rx,y+h-86,MUTED,false);}
            else {line(g,"MASTERY RETAINED",String.format(Locale.ROOT,"%.0f / %d",row.getDouble("mastery"),row.getInt("maxMastery")),top,GREEN);int barY=top+24;g.fill(rx,barY,rx+rw,barY+4,0xff17131e);double frac=Math.clamp(row.getDouble("mastery")/Math.max(1,row.getInt("maxMastery")),0,1);g.fill(rx,barY,rx+(int)(rw*frac),barY+4,GOLD);if(h>=245)g.drawWordWrap(font,Component.literal("The recipient receives a skill copy. Your original stays yours."),rx,barY+14,rw,TEXT);if(h>=280)g.drawWordWrap(font,Component.literal("25% EP while used; extra gifts add diminishing returns. After 10 minutes idle, Claim may take or copy a skill."),rx,barY+52,rw,MUTED);}
            String note=confirm?(row.getBoolean("steal")?(row.getBoolean("copy")?"Recall + copy a random unowned skill?":"Recall + take a random unowned skill?"):"Recall only; no skill will be taken."):state.getString("message");if(note.isEmpty() && report())note=row.getBoolean("steal")?(row.getBoolean("copy")?"Skill copy available":"Skill claim available"):"Claim recalls the gifted copy";g.drawString(font,fit(pending?"Updating…":note,rw),rx,y+h-65,confirm?GOLD:MUTED,false);
        }else g.drawString(font,"No matching entries",x+14,y+80,MUTED,false);
        g.drawCenteredString(font,(state.getInt("page")+1)+" / "+Math.max(1,state.getInt("pages"))+" · "+state.getInt("count"),x+left/2,y+h-22,MUTED);super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_F && hasControlDown()){setFocused(search);search.setFocused(true);return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){if(mx>=x && mx<x+left && my>=y+68 && my<y+h-30 && dy!=0){request("refresh","",state.getInt("page")-(int)Math.signum(dy));return true;}return super.mouseScrolled(mx,my,dx,dy);}
    public static final class GiftOffer extends Screen {
        private final Screen parent;private final CompoundTag gift;private int age;private int x,y,w,h;private boolean sent;
        public GiftOffer(Screen parent,CompoundTag gift){super(Component.literal("Skill gift"));this.parent=parent;this.gift=gift.copy();}
        private void reply(boolean accept){if(!sent){sent=true;PacketDistributor.sendToServer(new LoanNetwork.Reply(gift.getUUID("nonce"),accept));}Minecraft.getInstance().setScreen(parent);}
        @Override protected void init(){w=Math.min(280,width-16);h=Math.min(154,height-12);x=(width-w)/2;y=(height-h)/2;addRenderableWidget(new SimpleButton(x+10,y+h-27,(w-25)/2,18,Component.literal("Accept"),Component.literal("Accept skill gift"),b->reply(true)));addRenderableWidget(new SimpleButton(x+15+(w-25)/2,y+h-27,(w-25)/2,18,Component.literal("Decline"),Component.literal("Decline skill gift"),b->reply(false)));}
        @Override public void tick(){if(++age>=600)reply(false);}
        @Override public void onClose(){reply(false);}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){g.fill(0,0,width,height,0xa0100e18);g.fill(x-1,y-1,x+w+1,y+h+1,0xff716379);g.fill(x,y,x+w,y+h,0xff302c34);g.fill(x+2,y+2,x+w-2,y+20,0xff211b29);g.drawCenteredString(font,"SKILL GIFT",width/2,y+7,GOLD);String sender=font.plainSubstrByWidth(gift.getString("sender"),w-30);g.drawCenteredString(font,sender+" offers you",width/2,y+30,TEXT);icon(g,gift.getString("skill"),x+12,y+51,30);g.drawString(font,font.plainSubstrByWidth(gift.getString("name"),w-64),x+49,y+55,GOLD,false);g.drawString(font,String.format(Locale.ROOT,"Mastery %.0f / %d",gift.getDouble("mastery"),gift.getInt("maxMastery")),x+49,y+70,GREEN,false);super.render(g,mx,my,partial);}
    }
}
