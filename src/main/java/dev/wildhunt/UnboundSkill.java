package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** The completed Heavenly Restriction: all original vows remain, with a stronger martial body. */
public final class UnboundSkill extends ChainedSkill {
    public UnboundSkill(){super(SkillType.ULTIMATE);}
    @Override public MutableComponent getName(){return Component.literal("Unbound");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(SkillType.ULTIMATE.getChatFormatting());}
    @Override public ResourceLocation getSkillIcon(){return WildHunt.id("textures/skill/unbound.png");}
    @Override public int getModes(ManasSkillInstance i){return i.getMastery()<0?1:2;}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal(i.getMastery()<0?"Evolve Chained into Unbound":mode==1?"Counter":"Severance Rush");}
    @Override public String getModeId(ManasSkillInstance i,int mode){return mode==1?"unbound.counter":"chained.severance_rush";}
    @Override public int nextMode(LivingEntity e,ManasSkillInstance i,int mode,boolean reverse){return i.getMastery()<0?0:mode==0?1:0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return i.getMastery()<0 || mode==1 && UnboundCounter.active(e)?0:mode==1?Math.max(0,io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(e).getAura()*.1):super.getAuraCost(e,i,mode);}
    @Override public boolean canIgnoreCoolDown(ManasSkillInstance i,LivingEntity e,int mode){return mode==1 && UnboundCounter.active(e);}
    @Override public boolean addLearnPoint(ManasSkillInstance i,LivingEntity e,int mode,double point){return ChainedEvolution.ready(e) && super.addLearnPoint(i,e,mode,Math.max(1,point));}
    @Override public void onLearnSkill(ManasSkillInstance i,LivingEntity e){if(i.getMastery()>=0){ChainedEvolution.finish(e,i);super.onLearnSkill(i,e);}}
    @Override public void onForgetSkill(ManasSkillInstance i,LivingEntity e){UnboundCounter.clear(e);super.onForgetSkill(i,e);}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){if(mode==1)UnboundCounter.press(i,e);else super.onPressed(i,e,slot,mode);}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Ultimate evolution of mastered Chained: requires Heavenbreaker and True Demon Lord or True Hero. All Heavenly Restriction vows, aura growth, regeneration conversion, training and Severance Rush remain. Martial Supremacy toggle: 8x Battlewill ability damage, otherwise 5x Physical damage; 4x numeric Battlewill buffs except Aura Sword. Broken Limit, true mastered passive: one-tier resistance/nullification degradation, dodge bypass, and automatic critical hits for all your attacks. Unerring Body, in slot: 30% incoming damage reduction, 100% projectile dodge, 50% other dodge, subject to native bypass rules. Counter: 3-second defensive stance with zero movement speed. The first eligible enemy hit is negated and releases a massive 64-block Physical slash through terrain. Base damage = 10x negated incoming damage + 6x square root of aura paid. Costs 10% current aura on entry; 10-second cooldown. Press again to cancel freely. Evolution retains mastery, toggle, capacity ceiling, aura exemption, presets and dash cooldown.");}
}
