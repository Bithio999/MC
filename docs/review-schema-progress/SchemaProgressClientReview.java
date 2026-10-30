package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.client.screen.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;

/** Read-only client view of detached skill instances; run in a disposable world. */
@EventBusSubscriber(modid=WildHunt.ID,value=Dist.CLIENT)
public final class SchemaProgressClientReview {
    private static final double[] VALUES={-1000,-500,-1,0,1000,1999,2000,-50};
    private static final String[] EXPECTED={"0 / 1,000","500 / 1,000","999 / 1,000","0 / 2,000","1000 / 2,000","1999 / 2,000","2000 / 2,000","50"};
    private static ReviewScreen screen;private static int stage,frames,age;
    public static final class ReviewScreen extends AbilitySelectionScreen {
        String tooltip="";ManasSkillInstance sample;
        ReviewScreen(){super(AbilityCategoriesScreen.AbilityType.BATTLEWILL,null,null);shouldFade=false;}
        @Override protected void updateAllAbilities(){if(sample==null)super.updateAllAbilities();else{abilities.clear();abilities.add(sample);}}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,guiLeft+150,guiTop+150,partial);}
        @Override public void setTooltipForNextRenderPass(Component text){String value=text.getString();if(value.contains("Learn Points") || value.contains("Mastery"))tooltip=value;super.setTooltipForNextRenderPass(text);}
    }
    private static void select(Minecraft mc){
        ManasSkillInstance i=stage==7 ? SkillAPI.getSkillRegistry().get(net.minecraft.resources.ResourceLocation.parse("tensura:aura_slash")).createDefaultInstance() : WildHunt.SCHEMA.get().createDefaultInstance();
        i.setMastery(VALUES[stage]);screen.sample=i;screen.selectedAbility=i;screen.tooltip="";frames=0;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;if(mc.player==null || mc.getOverlay()!=null || mc.player.tickCount<100)return;
        if(++age>1200)throw new IllegalStateException("Schema progress review timeout");
        if(screen==null){screen=new ReviewScreen();mc.setScreen(screen);select(mc);return;}
        if(++frames<18)return;
        String tooltip=screen.tooltip;
        if(!tooltip.contains(EXPECTED[stage]) || stage==7 && tooltip.contains(" / "))throw new IllegalStateException("Wrong native tooltip stage "+stage+": "+tooltip);
        try{
            Files.createDirectories(Path.of("schema-progress-review"));
            Files.writeString(Path.of("schema-progress-review/results.txt"),stage+": "+tooltip+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            try(var pic=Screenshot.takeScreenshot(mc.getMainRenderTarget())){pic.writeToFile(Path.of("schema-progress-review/stage-"+stage+".png"));}
            if(++stage==VALUES.length){Files.writeString(Path.of("schema-progress-review/passed.txt"),"Native Schema learning/mastery tooltips verified at all 7 boundaries; Aura Slash display unchanged.\n");mc.stop();return;}
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
        select(mc);
    }
}
