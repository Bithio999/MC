package dev.wildhunt.mixin.client;

import dev.wildhunt.client.MythsClient;
import io.github.manasmods.tensura.client.screen.MainScreen;
import io.github.manasmods.tensura.client.screen.templates.SimpleScreen;
import io.github.manasmods.tensura.util.client.RenderHelper;
import net.minecraft.client.gui.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MainScreen.class)
public abstract class MythsStatusMixin extends SimpleScreen {
    protected MythsStatusMixin(Component title){super(title,254,163);}
    private static String myths$race(String original) {
        var state=MythsClient.state;
        if(state.getBoolean("converged") || state.getString("secondary").isBlank() || state.getString("secondary").equals("Not selected"))return original;
        // Wait for the matching primary snapshot after a reincarnation or race change.
        return state.getString("primary").equals(original)?original+"/"+state.getString("secondary"):original;
    }
    @Redirect(method="renderRaceStats",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"),remap=false)
    private int myths$label(GuiGraphics g,Font font,String text,int x,int y,int color) {
        RenderHelper.drawScrollingText(g,font,Component.literal(myths$race(text)),x,y,125,color);return x;
    }
    @Inject(method="renderRaceStats",at=@At("TAIL"),remap=false)
    private void myths$tooltip(GuiGraphics g,int mx,int my,CallbackInfo ci) {
        var state=MythsClient.state;
        if(mx>=guiLeft+95 && mx<guiLeft+220 && my>=guiTop+35 && my<guiTop+47)
            setTooltipForNextRenderPass(Component.literal(myths$race(state.getString("primary"))));
    }
}
