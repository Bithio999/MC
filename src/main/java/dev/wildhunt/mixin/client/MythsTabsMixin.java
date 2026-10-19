package dev.wildhunt.mixin.client;

import dev.wildhunt.WildHunt;
import dev.wildhunt.client.MythsClient;
import io.github.manasmods.tensura.util.client.ScreenHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Activate native right-side menu slots 5 and 6 everywhere they are used. */
@Mixin(ScreenHelper.class)
public abstract class MythsTabsMixin {
    @Inject(method="getMenuTabs",at=@At("RETURN"),remap=false)
    private static void myths$buttons(int current,int left,int top,CallbackInfoReturnable<java.util.List<io.github.manasmods.tensura.client.screen.widgets.SimpleButton>> ci) {
        var tabs=ci.getReturnValue();
        for(int index=0;index<tabs.size();index++) {
            var button=tabs.get(index);
            if(button.getX()!=left+233)continue;
            int id=4+(button.getY()-top-30)/26;
            if(id==5 || id==6)tabs.set(index,new dev.wildhunt.client.MythsSideTab(button.getX(),button.getY(),id));
        }
    }
    @Inject(method="openScreen(I)V",at=@At("HEAD"),cancellable=true,remap=false)
    private static void myths$open(int id,CallbackInfo ci) {
        if(id==5 || id==6){MythsClient.open(id==5?"secondary":"convergence");ci.cancel();}
    }
    @Inject(method="getMenuName",at=@At("HEAD"),cancellable=true,remap=false)
    private static void myths$name(int id,CallbackInfoReturnable<Component> ci) {
        if(id==5 || id==6)ci.setReturnValue(Component.literal(id==5?"Secondary Race":"Convergent Awakening"));
    }
    @Inject(method="getTabTexture",at=@At("HEAD"),cancellable=true,remap=false)
    private static void myths$texture(int id,CallbackInfoReturnable<ResourceLocation> ci) {
        if(id==5 || id==6)ci.setReturnValue(ScreenHelper.RIGHT_TAB);
    }
    @Inject(method="getTabIcon",at=@At("HEAD"),cancellable=true,remap=false)
    private static void myths$icon(int id,CallbackInfoReturnable<ResourceLocation> ci) {
        if(id==5 || id==6)ci.setReturnValue(WildHunt.id("textures/gui/"+(id==5?"secondary_tab":"awakening_tab")+".png"));
    }
}
