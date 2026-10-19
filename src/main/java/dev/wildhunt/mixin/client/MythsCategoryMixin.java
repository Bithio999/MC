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
    @Inject(method="addSkillCategories",at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$categories(CallbackInfo ci) {
        var types=Skill.SkillType.values();
        for(int i=0;i<7;i++) {
            int index=i; boolean godly=i==6;
            var icon=godly?WildHunt.id("textures/skill/aion.png"):ResourceLocation.fromNamespaceAndPath("tensura","textures/gui/skill/"+types[i].getNamespace()+".png");
            var name=godly?Component.literal("Godly (Ultimate compatibility)"):types[i].getName();
            addRenderableWidget(new MythsCategoryButton(guiLeft+19+(i%2)*44,guiTop+45+(i/2)*28,icon,name,godly,b->{
                if(index==6)Minecraft.getInstance().setScreen(new GodlySelectionScreen());
                else ScreenHelper.openAbilityScreen(AbilityCategoriesScreen.AbilityType.SKILL,types[index],null);
            }));
        }
        ci.cancel();
    }
}
