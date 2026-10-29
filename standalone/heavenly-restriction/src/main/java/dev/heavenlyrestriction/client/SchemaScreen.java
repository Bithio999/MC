package dev.heavenlyrestriction.client;

import dev.heavenlyrestriction.*;
import io.github.manasmods.tensura.client.screen.templates.SimpleScreen;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Native Tensura screen/button controls with a fitted, six-node Schema panel. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SchemaScreen extends SimpleScreen {
    private CompoundTag state;
    private int[] draft;
    private int selected;
    private String message="";
    public SchemaScreen(CompoundTag state){super(Component.literal("Schema Tree"),233,225);read(state);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->SchemaNetwork.receiver=SchemaScreen::receive);}
    public static void receive(CompoundTag state){
        var mc=Minecraft.getInstance();if(state.getBoolean("open") && state.getBoolean("allowed"))mc.setScreen(new SchemaScreen(state));
        else if(mc.screen instanceof SchemaScreen screen){screen.read(state);screen.message=state.getString("message");}
    }
    private final class NodeButton extends SimpleButton {
        private final int slot;
        NodeButton(int slot){super(guiLeft+8,guiTop+32+slot*17,144,15,Component.literal("Slot "+(slot+1)),Component.literal("Select Schema slot "+(slot+1)),b->selected=slot);this.slot=slot;}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            int x=getX(),y=getY();g.fill(x-1,y-1,x+145,y+15,slot==selected?0xffaa716a:0xff614267);g.fill(x,y,x+144,y+14,0xff26192e);
            if(draft[slot]==0){g.drawString(font,(slot+1)+"  Empty slot",x+4,y+3,0x93869f,false);return;}
            int ordinal=0;for(int n=0;n<slot;n++)if(draft[n]!=0)ordinal++;
            double mastery=state.getDouble("mastery");var local=io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(player).getSkill(HeavenlyRestriction.SCHEMA.get());if(local.isPresent())mastery=local.get().getMastery();
            double w=SchemaArt.weight(ordinal,mastery);String name=draft[slot]==SchemaArt.STRENGTH?"Strength":"Mitochondrial";g.drawString(font,(slot+1)+"  "+name+" "+percent(w*100),x+4,y+3,w==1?0x9ce5a1:0xe4c27f,false);
        }
    }
    private void read(CompoundTag data){state=data.copy();draft=Arrays.copyOf(data.getIntArray("tree"),6);}
    private void button(int x,int y,int w,String label,String tooltip,Runnable action){addRenderableWidget(new SimpleButton(guiLeft+x,guiTop+y,w,15,Component.literal(label),Component.literal(tooltip),b->{action.run();message="";}));}
    private void add(int effect){
        int at=selected;if(draft[at]!=0){at=-1;for(int n=0;n<6;n++)if(draft[n]==0){at=n;break;}}if(at>=0){draft[at]=effect;selected=at;}
    }
    @Override protected void init(){
        super.init();
        for(int n=0;n<6;n++)addRenderableWidget(new NodeButton(n));
        button(160,33,65,"Add Mito","Mitochondrial Build: +500% aura regeneration; -5% all outgoing damage. Repeated stacks are allowed.",()->add(SchemaArt.MITOCHONDRIAL));
        button(160,53,65,"Add Str.","Strength Build: +5 base attack damage; +2% damage taken. Positive strength follows slot weight; the drawback remains full.",()->add(SchemaArt.STRENGTH));
        button(160,73,65,"Remove","Remove the selected effect",()->{for(int n=selected;n<5;n++)draft[n]=draft[n+1];draft[5]=0;});
        button(160,93,30,"Up","Move selected effect earlier",()->{if(selected>0){int v=draft[selected-1];draft[selected-1]=draft[selected];draft[selected--]=v;}});
        button(193,93,32,"Down","Move selected effect later",()->{if(selected<5){int v=draft[selected+1];draft[selected+1]=draft[selected];draft[selected++]=v;}});
        button(160,113,65,"Clear","Clear the draft tree",()->{Arrays.fill(draft,0);selected=0;});
        button(49,207,65,"Apply","Apply this build on the server",()->PacketDistributor.sendToServer(new SchemaNetwork.Save(draft.clone(),state.getInt("revision"))));
        button(121,207,65,"Close","Close; unapplied edits are discarded",this::onClose);
    }
    private static String percent(double value){return String.format(Locale.ROOT,value==Math.rint(value)?"%.0f%%":"%.1f%%",value);}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        g.fill(guiLeft-2,guiTop-2,guiLeft+235,guiTop+227,0xff0a080e);g.fill(guiLeft,guiTop,guiLeft+233,guiTop+225,0xff4b315b);g.fill(guiLeft+2,guiTop+2,guiLeft+231,guiTop+223,0xff1b1028);
        g.fill(guiLeft+3,guiTop+3,guiLeft+230,guiTop+20,0xff291333);
        g.drawCenteredString(font,title,guiLeft+116,guiTop+7,0xff7770);
        double mastery=Math.max(0,state.getDouble("mastery"));var local=io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(player).getSkill(HeavenlyRestriction.SCHEMA.get());if(local.isPresent())mastery=Math.max(0,local.get().getMastery());
        g.drawString(font,String.format(Locale.ROOT,"Mastery %.0f/2000 | Full slots %d/6",mastery,SchemaArt.fullSlots(mastery)),guiLeft+8,guiTop+23,0xbcb0c8,false);
        g.drawString(font,"Mito: regen",guiLeft+160,guiTop+138,0xe5d8ee,false);
        g.drawString(font,"+500% / -5%",guiLeft+160,guiTop+149,0xe5d8ee,false);
        g.drawString(font,"Str: +5 atk",guiLeft+160,guiTop+165,0xe5d8ee,false);
        g.drawString(font,"+2% taken",guiLeft+160,guiTop+176,0xf19c9c,false);
        double bonus=SchemaArt.bonus(draft,mastery,state.getDouble("boost"));
        g.drawString(font,"Aura regen: +"+percent(bonus*100),guiLeft+8,guiTop+138,0x9ce5a1,false);
        g.drawString(font,String.format(Locale.ROOT,"Base attack: +%.2f",SchemaArt.strengthBonus(draft,mastery,state.getDouble("boost"))),guiLeft+8,guiTop+151,0x9ce5a1,false);
        g.drawString(font,"Damage dealt: -"+percent(SchemaArt.penalty(draft)*100),guiLeft+8,guiTop+164,0xf19c9c,false);
        g.drawString(font,"Damage taken: +"+percent(SchemaArt.vulnerability(draft)*100),guiLeft+8,guiTop+177,0xf19c9c,false);
        if(!message.isEmpty())g.drawCenteredString(font,Component.literal(message),guiLeft+116,guiTop+194,0xeee2b4);
    }
    @Override public void renderWidgets(GuiGraphics g,int mx,int my,float partial){super.renderWidgets(g,mx,my,partial);}
    @Override public boolean isPauseScreen(){return false;}
}
