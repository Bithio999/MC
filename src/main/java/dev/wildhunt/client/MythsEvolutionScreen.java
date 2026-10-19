package dev.wildhunt.client;

import dev.wildhunt.myths.MythsNetwork;
import io.github.manasmods.manascore.race.api.*;
import io.github.manasmods.tensura.client.screen.EvolutionScreen;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import io.github.manasmods.tensura.util.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Native EvolutionScreen with server-provided secondary/convergence candidates. */
public final class MythsEvolutionScreen extends EvolutionScreen {
    private CompoundTag state;
    public final boolean convergence;
    private boolean confirming;
    public MythsEvolutionScreen(CompoundTag state,boolean convergence) {this.state=state;this.convergence=convergence;loadChoices();}
    public void update(CompoundTag state) {this.state=state;confirming=false;loadChoices();rebuildWidgets();}
    public void refreshState(CompoundTag state) {
        if(!this.state.getString("secondary").equals(state.getString("secondary")) || !this.state.getString("choice_action").equals(state.getString("choice_action")) || this.state.getBoolean("converged")!=state.getBoolean("converged"))update(state);
        else this.state=state; // Preserve search, scroll position and pending confirmation during read-only updates.
    }
    private void loadChoices() {
        evolutions=new ArrayList<>();
        if(convergence) {
            var id=ResourceLocation.tryParse(state.getString("fusion_id"));
            if(id!=null)Optional.ofNullable(RaceAPI.getRaceRegistry().get(id)).ifPresent(evolutions::add);
        } else for(var raw:state.getList("choices",Tag.TAG_COMPOUND)) {
            var id=ResourceLocation.tryParse(((CompoundTag)raw).getString("race"));
            if(id!=null)Optional.ofNullable(RaceAPI.getRaceRegistry().get(id)).ifPresent(evolutions::add);
        }
        copyOfEvolutions=new ArrayList<>(evolutions);
        if(selectedEvolution==null || !evolutions.contains(selectedEvolution))selectedEvolution=evolutions.isEmpty()?null:evolutions.getFirst();
        startIndex=Math.clamp(startIndex,0,Math.max(0,evolutions.size()-3));
    }
    private CompoundTag row() {
        if(selectedEvolution!=null)for(var raw:state.getList("choices",Tag.TAG_COMPOUND)) {
            var tag=(CompoundTag)raw;if(tag.getString("race").equals(selectedEvolution.getRegistryName().toString()))return tag;
        }
        return new CompoundTag();
    }
    private boolean ready() {
        if(state.getBoolean("converged"))return false;
        if(convergence)return selectedEvolution!=null && state.getBoolean("primary_final") && state.getBoolean("secondary_final") && state.getBoolean("awakened") && state.getDouble("ep")>=state.getDouble("convergence_ep");
        if(selectedEvolution==null)return false;
        if(state.getString("choice_action").equals("evolve"))return row().getBoolean("requirements_met");
        return state.getBoolean("primary_final") && state.getBoolean("awakened") && state.getDouble("ep")>=state.getDouble("secondary_ep");
    }
    @Override protected void init() {
        super.init();
        // Remove only the native primary-race actions. Navigation, search and scrolling remain native.
        for(var child:List.copyOf(children()))if(child instanceof AbstractWidget w && w.getY()==guiTop+142 && (w.getX()==guiLeft+98 || w.getX()==guiLeft+160))removeWidget(w);
        // Native Evolution uses the status tab as selected; replace it with our side tab.
        for(var child:List.copyOf(children()))if(child instanceof AbstractWidget w && w.getX()==guiLeft+233 && w.getY()==guiTop+30+((convergence?6:5)-4)*26)removeWidget(w);
        addRenderableWidget(new SimpleButton(guiLeft+233,guiTop+30,21,24,ScreenHelper.STATUS_TAB,null,ScreenHelper.getMenuName(4),b->ScreenHelper.openScreen(4)));
        var back=new SimpleButton(guiLeft+98,guiTop+142,43,13,BUTTON,Component.literal("Back"),Component.literal("Return to primary evolution"),b->minecraft.setScreen(new EvolutionScreen()));
        // Evolution's native return button opens Status; keep that same navigation here.
        addRenderableWidget(back);
        var action=new SimpleButton(guiLeft+160,guiTop+142,43,13,BUTTON,Component.literal(state.getBoolean("converged")?"Unified":confirming?"Confirm":convergence?"Awaken":state.getString("choice_action").equals("evolve")?"Evolve":"Choose"),Component.literal(requirements()),b->{
            if(!ready())return;
            if(!convergence && state.getString("choice_action").equals("select") && !confirming){confirming=true;rebuildWidgets();return;}
            PacketDistributor.sendToServer(new MythsNetwork.Request(convergence?"awaken":state.getString("choice_action"),convergence?"":row().getString("id")));
        });
        action.setActiveCheck(this::ready);action.setClickedCheck(()->action.isHovered() && ready());addRenderableWidget(action);
    }
    private String requirements() {
        if(state.getBoolean("converged"))return "Convergence complete. Your lineages are unified.\n"+state.getString("fusion_description")+"\nGrowth: "+state.getDouble("aura")+" aura / "+state.getDouble("magicules")+" magicules.";
        if(!convergence && state.getString("choice_action").equals("evolve"))return row().getString("detail");
        String text=(confirming?"Confirm permanent ancestry: "+selectedEvolution.getName().getString()+"\n":"")+"Primary final: "+yes(state.getBoolean("primary_final"))+"\n";
        if(convergence)text+="Secondary final: "+yes(state.getBoolean("secondary_final"))+"\n";
        text+="True Hero / True Demon Lord: "+yes(state.getBoolean("awakened"))+"\nBase EP: "+String.format(Locale.ROOT,"%,.0f / %,.0f",state.getDouble("ep"),state.getDouble(convergence?"convergence_ep":"secondary_ep"));
        return text+(convergence?"\nResult: "+state.getString("fusion_name")+"\nGrowth: "+state.getDouble("fusion_aura")+" aura / "+state.getDouble("fusion_magicules")+" magicules, +10% EP gain":"\n"+row().getString("detail"));
    }
    private static String yes(boolean b){return b?"Yes":"No";}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial) {
        // Exclude the baked Status selection first; transparent pixels cannot erase an already drawn tab.
        g.blit(BACKGROUND,guiLeft,guiTop,0,0,imageWidth,30);
        g.blit(BACKGROUND,guiLeft,guiTop+30,0,30,230,24);
        g.blit(BACKGROUND,guiLeft,guiTop+54,0,54,imageWidth,imageHeight-54);
        g.blit(BACKGROUND,guiLeft+230,guiTop+30,230,56,26,24);
        // Use the exact native connected frame, including the two columns of the panel's bevel.
        g.blit(BACKGROUND,guiLeft+230,guiTop+30+((convergence?6:5)-4)*26,230,30,26,24);
        g.drawString(font,Component.literal(convergence?"Awakening":"Secondary Race"),guiLeft+105,guiTop+10,0xffffff);
        ScreenHelper.renderTabIcon(g,this,convergence?6:5,guiLeft,guiTop,mx,my);
        float size=player.getScale()>1?1/player.getScale():1;
        RenderHelper.renderEntityInInventoryFollowsMouse(g,guiLeft+6,guiTop+55,guiLeft+54,guiTop+126,30*size,mx,my,false,player);
        renderPlayerStats(g,mx,my);renderScrollBar(g,mx,my);renderEvolutionsList(g,mx,my);
        var description=Component.literal(requirements());var lines=font.split(description,107);
        textStartIndex=Math.clamp(textStartIndex,0,Math.max(0,lines.size()-3));
        RenderHelper.drawCenteredText(g,font,Component.literal(convergence?"Convergence":"Secondary"),guiLeft+95,guiTop+94,111,0xffffff,false);
        boolean hover=mx>=guiLeft+97 && mx<guiLeft+204 && my>=guiTop+105 && my<guiTop+136;
        RenderHelper.drawScrollableTextInAreaSetHighlight(g,font,lines,guiLeft+97,guiTop+105,107,31,1,textStartIndex,Math.min(textStartIndex+3,lines.size()),hover,0xffffff,863042);
        if(hover)setTooltipForNextRenderPass(description);
        int fill=!convergence && state.getString("choice_action").equals("evolve")?Math.clamp((int)Math.round(58*row().getDouble("progress")/100),0,58):ready()?58:0;if(fill>0)g.blit(BACKGROUND,guiLeft+213,guiTop+93,13,164,6,fill);
        if(!state.getString("message").isEmpty())setTooltipForNextRenderPass(Component.literal(state.getString("message")));
        renderWidgets(g,mx,my,partial);
    }
    @Override protected void renderPlayerStats(GuiGraphics g,int mx,int my){
        String current=state.getBoolean("converged")?state.getString("primary"):state.getString("secondary");
        var name=Component.literal(current.isBlank()?"Select ancestry":current);
        RenderHelper.drawScrollingText(g,font,name,guiLeft+7,guiTop+35,70,0xffe8c56f);
        if(mx>=guiLeft+6 && mx<guiLeft+79 && my>=guiTop+34 && my<guiTop+47)setTooltipForNextRenderPass(name);
        double ep=state.getDouble("ep");
        RenderHelper.drawTextWithTooltip(g,font,"EP: "+RenderHelper.getShortenedNumber(ep).getString(),guiLeft+13,guiTop+144,0xffffaa00,mx,my,Component.literal(String.format(Locale.ROOT,"Base EP: %,.0f",ep)),this);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {var before=selectedEvolution;boolean result=super.mouseClicked(x,y,button);if(before!=selectedEvolution){confirming=false;rebuildWidgets();}return result;}
}
