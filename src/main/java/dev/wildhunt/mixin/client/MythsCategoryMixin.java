package dev.wildhunt.mixin.client;

import dev.wildhunt.WildHunt;
import dev.wildhunt.client.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.client.screen.templates.SimpleScreen;
import io.github.manasmods.tensura.util.client.ScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbilityCategoriesScreen.class)
public abstract class MythsCategoryMixin extends SimpleScreen {
    protected MythsCategoryMixin(Component title) { super(title,254,163); }
    @Inject(method="addSkillCategories",at=@At("TAIL"),remap=false)
    private void myths$category(CallbackInfo ci) {
        addRenderableWidget(new io.github.manasmods.tensura.client.screen.widgets.SimpleButton(guiLeft+54,guiTop+27,43,13,
            EvolutionScreen.BUTTON,Component.literal("Godly"),Component.literal("Godly"),
            b->ScreenHelper.clicked(()->Minecraft.getInstance().setScreen(new GodlySelectionScreen()))));
    }
}
