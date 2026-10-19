package dev.wildhunt.client;

import dev.wildhunt.*;
import dev.wildhunt.myths.MythsNetwork;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Native skill list and detail pane, with two Godly destinations in the existing footer. */
public final class GodlyAscensionScreen extends AbilitySelectionScreen {
    private boolean thoth;
    private SimpleButton action, destination;
    public GodlyAscensionScreen(){super(AbilityCategoriesScreen.AbilityType.SKILL,Skill.SkillType.ULTIMATE,null);thoth=MythsClient.state.getBoolean("has_godly") && !MythsClient.state.getBoolean("has_thoth");}
    public boolean destinationOwned(){return MythsClient.state.getBoolean(thoth?"has_thoth":"has_godly");}
    public void refresh(){rebuildWidgets();}
    @Override protected void init(){
        super.init();
        destination=new SimpleButton(guiLeft+119,guiTop+146,39,12,Component.literal(thoth?"Thoth":"Aion"),Component.literal("Choose Godly destination: Aion / Thoth.\nThoth additionally combines a mastered Rune Smith."),b->{thoth=!thoth;refresh();});
        action=new SimpleButton(guiLeft+160,guiTop+146,61,12,Component.literal("Ascend"),Component.literal("Evolve the selected mastered combination Ultimate into "+(thoth?"Thoth":"Aion")+".\nRequires true awakening and "+String.format(java.util.Locale.ROOT,"%,.0f",MythsClient.state.getDouble("godly_ep"))+" base EP."+(thoth?"\nAlso consumes a mastered Rune Smith.":"")),b->{
            if(ready())PacketDistributor.sendToServer(new MythsNetwork.Request("godly",selectedAbility.getSkillId()+"|"+(thoth?"thoth":"aion")));
        });
        action.setActiveCheck(this::ready);action.setClickedCheck(()->action.isHovered() && ready());addRenderableWidget(destination);addRenderableWidget(action);
    }
    private boolean ready(){return selectedAbility!=null && selectedAbility.isMastered(player) && MythsClient.state.getBoolean("awakened") && MythsClient.state.getDouble("ep")>=MythsClient.state.getDouble("godly_ep") && !destinationOwned() && (!thoth || MythsClient.state.getBoolean("rune_mastered"));}
    @Override protected void updateFilteredAbilities(){super.updateFilteredAbilities();filtered.removeIf(i->MythicEvolution.ultimates().stream().noneMatch(k->WildHunt.MYTHIC.get(k).get()==i.getSkill()));if(selectedAbility!=null && !filtered.contains(selectedAbility))selectedAbility=null;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){super.renderBackground(g,x,y,partial);g.blit(BACKGROUND,guiLeft+117,guiTop+145,117,145,106,14);destination.render(g,x,y,partial);action.render(g,x,y,partial);}
}
