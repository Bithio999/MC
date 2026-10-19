package dev.wildhunt.client;

import dev.wildhunt.*;
import dev.wildhunt.myths.MythsNetwork;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Uses the native skill list, details, search, presets and selection behavior. */
public final class GodlyAscensionScreen extends AbilitySelectionScreen {
    public GodlyAscensionScreen(){super(AbilityCategoriesScreen.AbilityType.SKILL,Skill.SkillType.ULTIMATE,null);}
    public void refresh(){rebuildWidgets();}
    private SimpleButton action;
    @Override protected void init(){
        super.init();
        action=new SimpleButton(guiLeft+119,guiTop+146,102,12,Component.literal("Ascend"),Component.literal("Evolve the selected mastered Ultimate into Aion.\nRequires true awakening and "+String.format(java.util.Locale.ROOT,"%,.0f",MythsClient.state.getDouble("godly_ep"))+" base EP."),b->{
            if(ready())PacketDistributor.sendToServer(new MythsNetwork.Request("godly",selectedAbility.getSkillId().toString()));
        });
        action.setActiveCheck(this::ready);action.setClickedCheck(()->action.isHovered() && ready());addRenderableWidget(action);
    }
    private boolean ready(){
        return selectedAbility!=null && selectedAbility.isMastered(player) && MythsClient.state.getBoolean("awakened") && MythsClient.state.getDouble("ep")>=MythsClient.state.getDouble("godly_ep") && !MythsClient.state.getBoolean("has_godly");
    }
    @Override protected void updateFilteredAbilities(){
        super.updateFilteredAbilities();filtered.removeIf(i->WildHunt.ULTIMATE.values().stream().noneMatch(s->s.get()==i.getSkill()));
        if(selectedAbility!=null && !filtered.contains(selectedAbility))selectedAbility=null;
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){
        super.renderBackground(g,x,y,partial);
        g.blit(BACKGROUND,guiLeft+117,guiTop+145,117,145,106,14);
        action.render(g,x,y,partial);
    }
}
