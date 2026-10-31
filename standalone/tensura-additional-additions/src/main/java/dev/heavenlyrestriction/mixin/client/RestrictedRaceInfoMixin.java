package dev.heavenlyrestriction.mixin.client;

import dev.heavenlyrestriction.RestrictedRace;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.tensura.client.screen.ReincarnationScreen;
import io.github.manasmods.tensura.util.client.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.awt.Color;

/** Native reincarnation only renders its information panel for DefaultRace subclasses. */
@Mixin(ReincarnationScreen.class)
public abstract class RestrictedRaceInfoMixin {
    @Shadow(remap=false) protected abstract ManasRace getSelectedRace();

    @Inject(method="renderRace",at=@At("TAIL"),remap=false)
    private void myths$restrictedInformation(GuiGraphics graphics,int mouseX,int mouseY,CallbackInfo ci){
        if(!(getSelectedRace() instanceof RestrictedRace race))return;
        var screen=(ReincarnationScreen)(Object)this;
        var font=Minecraft.getInstance().font;
        int x=screen.getGuiLeft()+screen.getXSize()-95,y=screen.getGuiTop();
        // Keep the native panel positions, sizing, labels and difficulty/skill colors.
        var stats=Component.translatable("tensura.reincarnation.infobox.1",race.getDifficulty().asText(),
            race.getMinBaseAura(),race.getMaxBaseAura(),race.getMinBaseMagicule(),race.getMaxBaseMagicule());
        RenderHelper.drawScaledTextInArea(graphics,font,stats,x,y+26,91,35,Color.WHITE,4);
        var skills=Component.empty();
        for(var skill:race.getIntrinsicSkills(race.createDefaultInstance(),screen.getMenu().getPlayer())){
            if(!skills.getSiblings().isEmpty())skills.append(", ");
            skills.append(skill.getChatDisplayName(true));
        }
        RenderHelper.drawScaledTextInArea(graphics,font,Component.translatable("tensura.reincarnation.infobox.2",skills),x,y+60,90,35,Color.WHITE,4);
        RenderHelper.drawScaledTextInArea(graphics,font,race.getRaceDescription(),x,y+96,90,70,Color.WHITE,3);
    }
}
