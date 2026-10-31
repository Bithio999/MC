package dev.heavenlyrestriction;

import dev.heavenlyrestriction.worldhop.WorldHop;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ParallelWorldJumperSkill extends Skill {
    public ParallelWorldJumperSkill(){super(SkillType.UNIQUE);}
    @Override public MutableComponent getName(){return Component.translatableWithFallback("heavenlyrestriction.skill.parallel_world_jumper","Parallel World Jumper");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(getType().getChatFormatting());}
    @Override public ResourceLocation getSkillIcon(){return ResourceLocation.fromNamespaceAndPath("tensura","textures/skill/extra/spatial_motion.png");}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Single-player only. World Jump creates a fresh-seed world with every dimension regenerated. World Select (requires learning) sets a one-use return destination, or choose New World for a fresh seed. Your current character, inventory and progression travel with you; visited worlds retain their builds and terrain. Up to 128 worlds, including the active world. Visited worlds are hidden inside one stable Singleplayer save and accessed through World Select. Deleting that save deletes the entire library. Travel saves and reloads with safe-ground arrival. Unavailable on servers or open-to-LAN worlds. World Jump cooldown: 5 minutes. No aura or magicule cost.");}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public boolean canLearnSkill(ManasSkillInstance i,LivingEntity e){return WorldHop.available(e) && super.canLearnSkill(i,e);}
    @Override public int getModes(ManasSkillInstance i){return i.getMastery()<0?1:2;}
    @Override public java.util.List<Integer> getModeLearningList(ManasSkillInstance i){return java.util.List.of(1);}
    public boolean isWorldSelectLearned(ManasSkillInstance i,LivingEntity owner){return i.getMastery()>=0 && (i.getTag()==null?0:i.getTag().getDouble(getModeId(i,1)))>=getLearningPointRequirement(i,owner,1);}
    @Override public int nextMode(LivingEntity e,ManasSkillInstance i,int mode,boolean reverse){return Math.floorMod(mode+(reverse?-1:1),getModes(i));}
    @Override public String getModeId(ManasSkillInstance i,int mode){return mode==1?"parallel_world_jumper.world_select":"parallel_world_jumper.world_hop";}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.translatableWithFallback("heavenlyrestriction.skill.mode."+getModeId(i,mode),mode==1?"World Select":"World Jump").withStyle(net.minecraft.ChatFormatting.GRAY);}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){
        if(e instanceof ServerPlayer p && mode>=0 && mode<getModes(i) && i.getMastery()>=0 && !i.onCoolDown(mode) && canInteractSkill(i,p) && canActivateSkill(i,p,mode)){
            if(!WorldHop.available(p)){p.displayClientMessage(Component.literal("Parallel World Jumper is only available in a private single-player world."),true);return;}
            if(mode==1){if(learnMode(i,p,mode))return;dev.heavenlyrestriction.worldhop.WorldSelect.send(p,true,"");}else WorldHop.request(p,i);
        }
    }
}
