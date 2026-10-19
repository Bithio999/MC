package dev.wildhunt.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.manasmods.tensura.client.screen.EvolutionScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native navigation now occupies the old Evolution title position. */
@Mixin(EvolutionScreen.class)
public abstract class MythsEvolutionMenuMixin {
    @ModifyExpressionValue(method="renderBackground",at=@At(value="FIELD",target="Lio/github/manasmods/tensura/client/screen/EvolutionScreen;TITLE:Lnet/minecraft/network/chat/Component;"),remap=false)
    private Component myths$navigationTitle(Component original){return Component.empty();}
}
