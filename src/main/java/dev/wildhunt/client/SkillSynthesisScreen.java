package dev.wildhunt.client;

import dev.wildhunt.*;
import dev.wildhunt.myths.MythsNetwork;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;

/** Reuses native Unique entries, art, scrolling and details. The footer previews recipes. */
public final class SkillSynthesisScreen extends AbilitySelectionScreen {
    private int recipe;
    private SimpleButton choose, synthesize;
    public SkillSynthesisScreen(){super(AbilityCategoriesScreen.AbilityType.SKILL,Skill.SkillType.UNIQUE,null);}
    private MythicKind result(){return MythicEvolution.ultimates().get(recipe);}
    private CompoundTag row(){for(var value:MythsClient.state.getList("recipes",net.minecraft.nbt.Tag.TAG_COMPOUND)){var row=(CompoundTag)value;if(row.getString("id").equals(result().id()))return row;}return new CompoundTag();}
    public void refresh(){rebuildWidgets();}
    @Override protected void init(){
        super.init();String title=result().title.split(",")[0];
        choose=new SimpleButton(guiLeft+119,guiTop+146,48,12,Component.literal(title),Component.literal("Choose recipe: Vritra / Demeter / Janus / Anansi / Aegir.\n"+result().title+"\n"+row().getString("ingredients")+"\nRequires 2M base EP. Both mastered ingredients are combined."),b->{recipe=(recipe+1)%MythicEvolution.ultimates().size();refresh();});
        synthesize=new SimpleButton(guiLeft+169,guiTop+146,52,12,Component.literal("Combine"),Component.literal(result().title+"\n"+row().getString("ingredients")+"\nRequires 2M base EP. Presets and remaining cooldowns carry over."),b->{if(ready())PacketDistributor.sendToServer(new MythsNetwork.Request("synthesize",result().id()));});
        synthesize.setActiveCheck(this::ready);synthesize.setClickedCheck(()->synthesize.isHovered() && ready());addRenderableWidget(choose);addRenderableWidget(synthesize);
    }
    private boolean ready(){return row().getBoolean("ready");}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){super.renderBackground(g,x,y,partial);g.blit(BACKGROUND,guiLeft+117,guiTop+145,117,145,106,14);choose.render(g,x,y,partial);synthesize.render(g,x,y,partial);}
}
