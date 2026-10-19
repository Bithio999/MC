package dev.wildhunt.mixin.client;

import dev.wildhunt.WildHunt;
import dev.wildhunt.client.GodlySelectionScreen;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.client.screen.templates.SimpleScreen;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import io.github.manasmods.tensura.util.client.ScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbilityCategoriesScreen.class)
public abstract class MythsCategoryMixin extends SimpleScreen {
    @Shadow @Final protected AbilityCategoriesScreen.AbilityType abilityType;
    protected MythsCategoryMixin(Component title) { super(title,254,163); }
    @Inject(method="addSkillCategories",at=@At("TAIL"),remap=false)
    private void myths$category(CallbackInfo ci) {
        int index=0;
        for(var child:children())if(child instanceof SimpleButton b && b.getWidth()==32 && b.getHeight()==32) {
            b.setX(guiLeft+6+(index%3)*36);b.setY(guiTop+45+(index/3)*38);index++;
        }
        addRenderableWidget(new SimpleButton(guiLeft+42,guiTop+121,32,32,WildHunt.id("textures/gui/godly_category.png"),null,Component.literal("Godly"),
            b->ScreenHelper.clicked(()->Minecraft.getInstance().setScreen(new GodlySelectionScreen()))));
    }
    @Inject(method="renderBackground",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/client/screen/templates/SimpleScreen;renderWidgets(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",shift=At.Shift.BEFORE),remap=false)
    private void myths$fitBackground(GuiGraphics g,int mx,int my,float partial,CallbackInfo ci) {
        if(abilityType!=AbilityCategoriesScreen.AbilityType.SKILL)return;
        // Reuse the native panel's blue texture, clearing only its six old socket positions.
        var background=ResourceLocation.fromNamespaceAndPath("tensura","textures/gui/skill/skill_gui.png");
        for(int x=4;x<111;x+=13)g.blit(background,guiLeft+x,guiTop+42,4,42,Math.min(13,111-x),117);
    }
}
