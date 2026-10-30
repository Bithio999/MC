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
    @Override public MutableComponent getName(){return Component.literal("Parallel World Jumper");}
    @Override public ResourceLocation getSkillIcon(){return ResourceLocation.fromNamespaceAndPath("tensura","textures/skill/extra/spatial_motion.png");}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Single-player only. World Jump enters a fresh-seed world with every dimension regenerated, keeping your character, inventory and progression. World Select (requires learning) opens your visited worlds: choose one as the next jump's destination, or leave New World selected for a fresh seed. A selected destination is used once. Returning preserves that world's terrain and brings your current character. The game saves and reloads; preceding saves remain recoverable. Land on safe ground, with an Overworld fallback. Unavailable on servers or open-to-LAN worlds. No aura or magicule cost.");}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public boolean canLearnSkill(ManasSkillInstance i,LivingEntity e){return WorldHop.available(e) && super.canLearnSkill(i,e);}
    @Override public int getModes(ManasSkillInstance i){return i.getMastery()<0?1:2;}
    @Override public int nextMode(LivingEntity e,ManasSkillInstance i,int mode,boolean reverse){return Math.floorMod(mode+(reverse?-1:1),getModes(i));}
    @Override public String getModeId(ManasSkillInstance i,int mode){return mode==1?"parallel_world_jumper.world_select":"parallel_world_jumper.world_hop";}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal(mode==1?"World Select":"World Jump");}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){
        if(e instanceof ServerPlayer p && mode>=0 && mode<getModes(i) && i.getMastery()>=0 && !i.onCoolDown(mode) && canInteractSkill(i,p) && canActivateSkill(i,p,mode)){
            if(mode==1)dev.heavenlyrestriction.worldhop.WorldSelect.send(p,true,"");else WorldHop.request(p,i);
        }
    }
}
