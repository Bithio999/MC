package dev.wildhunt.client;

import dev.wildhunt.*;
import dev.wildhunt.myths.MythsNetwork;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Native list, details and footer; destinations share the server's progression requirements. */
public final class GodlyAscensionScreen extends AbilitySelectionScreen {
    private static final String[] IDS={"aion","thoth","lugh"},NAMES={"Aion","Thoth","Lugh"};
    private int choice;
    private SimpleButton action,destination;
    public GodlyAscensionScreen(){super(AbilityCategoriesScreen.AbilityType.SKILL,Skill.SkillType.ULTIMATE,null);while(choice<2 && destinationOwned())choice++;}
    public boolean destinationOwned(){return MythsClient.state.getBoolean(choice==0?"has_godly":"has_"+IDS[choice]);}
    public void refresh(){rebuildWidgets();}
    private String ingredient(){return choice==1?"Rune Smith":choice==2?"Threadweaver":"";}
    @Override protected void init(){
        super.init();
        destination=new SimpleButton(guiLeft+119,guiTop+146,39,12,Component.literal(NAMES[choice]),Component.literal("Choose Godly destination: Aion / Thoth / Lugh.\nThoth combines a mastered Rune Smith.\nLugh combines a mastered Threadweaver."),b->{choice=(choice+1)%IDS.length;refresh();});
        action=new SimpleButton(guiLeft+160,guiTop+146,61,12,Component.literal("Ascend"),Component.literal("Evolve the selected mastered combination Ultimate into "+NAMES[choice]+".\nRequires true awakening and "+String.format(java.util.Locale.ROOT,"%,.0f",MythsClient.state.getDouble("godly_ep"))+" base EP."+(choice>0?"\nAlso consumes a mastered "+ingredient()+".":"")),b->{
            if(ready())PacketDistributor.sendToServer(new MythsNetwork.Request("godly",selectedAbility.getSkillId()+"|"+IDS[choice]));
        });
        action.setActiveCheck(this::ready);action.setClickedCheck(()->action.isHovered() && ready());addRenderableWidget(destination);addRenderableWidget(action);
    }
    private boolean ready(){return selectedAbility!=null && selectedAbility.isMastered(player) && MythsClient.state.getBoolean("awakened") && MythsClient.state.getDouble("ep")>=MythsClient.state.getDouble("godly_ep") && !destinationOwned() && (choice==0 || MythsClient.state.getBoolean(choice==1?"rune_mastered":"thread_mastered"));}
    @Override protected void updateFilteredAbilities(){super.updateFilteredAbilities();filtered.removeIf(i->MythicEvolution.ultimates().stream().noneMatch(k->WildHunt.MYTHIC.get(k).get()==i.getSkill()));if(selectedAbility!=null && !filtered.contains(selectedAbility))selectedAbility=null;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){super.renderBackground(g,x,y,partial);g.blit(BACKGROUND,guiLeft+117,guiTop+145,117,145,106,14);destination.render(g,x,y,partial);action.render(g,x,y,partial);}
}
