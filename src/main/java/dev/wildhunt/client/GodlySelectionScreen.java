package dev.wildhunt.client;

import dev.wildhunt.GodlySkill;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class GodlySelectionScreen extends AbilitySelectionScreen {
    public GodlySelectionScreen() { super(AbilityCategoriesScreen.AbilityType.SKILL,Skill.SkillType.ULTIMATE,null); }
    @Override protected void updateFilteredAbilities() {
        int start=listStartIndex; float offset=scrollOffset;
        super.updateFilteredAbilities();
        filtered.removeIf(i->!dev.wildhunt.myths.MythsTiers.isGodly(i.getSkill()));
        listStartIndex=Math.clamp(start,0,Math.max(0,filtered.size()-getScrollBarRenderCount()));scrollOffset=offset;
        if(selectedAbility!=null && !filtered.contains(selectedAbility))selectedAbility=null;
    }
}
