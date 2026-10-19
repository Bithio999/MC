package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.LivingEntity;

/** Load-only save adapters. No retired Ultimate can be activated or toggled. */
final class RetiredApexSkill extends ApexSkill {
    RetiredApexSkill(Apex apex){super(apex,true);}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Retired skill. Existing ownership migrates automatically to a combination Ultimate.");}
    @Override public boolean canActivateSkill(ManasSkillInstance i,LivingEntity e,int mode){return false;}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){}
    @Override public void onToggleOn(ManasSkillInstance i,LivingEntity e){onToggleOff(i,e);}
    @Override public boolean tickActive(ManasSkillInstance i,LivingEntity e){onToggleOff(i,e);return false;}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
}
