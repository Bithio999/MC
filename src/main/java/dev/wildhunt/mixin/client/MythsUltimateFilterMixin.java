package dev.wildhunt.mixin.client;

import dev.wildhunt.GodlySkill;
import dev.wildhunt.client.GodlySelectionScreen;
import dev.wildhunt.client.GodlyAscensionScreen;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.AbilitySelectionScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(AbilitySelectionScreen.class)
public abstract class MythsUltimateFilterMixin implements dev.wildhunt.client.SkillMenuAccess {
    @Override public Skill.SkillType myths$filter(){return skillFilter;}
    @Shadow @Final protected Skill.SkillType skillFilter;
    @Shadow @Final public List<ManasSkillInstance> filtered;
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="renderBackground",at=@At(value="FIELD",target="Lio/github/manasmods/tensura/client/screen/AbilitySelectionScreen;TITLE:Lnet/minecraft/network/chat/Component;"),remap=false)
    private net.minecraft.network.chat.Component myths$title(net.minecraft.network.chat.Component original) {
        return (Object)this instanceof GodlySelectionScreen ? net.minecraft.network.chat.Component.literal("Godly") : (Object)this instanceof GodlyAscensionScreen ? net.minecraft.network.chat.Component.literal("Ascension") : (Object)this instanceof dev.wildhunt.client.SkillSynthesisScreen ? net.minecraft.network.chat.Component.literal("Synthesis") : skillFilter==Skill.SkillType.ULTIMATE ? net.minecraft.network.chat.Component.literal("Ultimate") : original;
    }
    @Inject(method="updateFilteredAbilities",at=@At("RETURN"),remap=false)
    private void myths$excludeGodly(CallbackInfo ci) {
        filtered.removeIf(instance->dev.wildhunt.MythicEvolution.retired(instance.getSkill()));
        if(!((Object)this instanceof GodlySelectionScreen) && skillFilter==Skill.SkillType.ULTIMATE)
            filtered.removeIf(instance->dev.wildhunt.myths.MythsTiers.isGodly(instance.getSkill()));
    }
}
