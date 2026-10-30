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
    @Override public MutableComponent getSkillDescription(){return Component.literal("Single-player only. Press World Hop to enter a fresh-seed world with every dimension regenerated. Keep your character, inventory and progression. Your previous world, including its builds and entities, remains in the world list as a recovery copy. The game saves, closes and reloads during the transition. Arrive on the nearest safe ground around your previous coordinates; if none is available nearby, arrive safely in the Overworld. Unavailable on dedicated servers or while your world is open to LAN. No aura or magicule cost.");}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public boolean canLearnSkill(ManasSkillInstance i,LivingEntity e){return WorldHop.available(e) && super.canLearnSkill(i,e);}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "parallel_world_jumper.world_hop";}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal("World Hop");}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){
        if(e instanceof ServerPlayer p && mode==0 && i.getMastery()>=0 && !i.onCoolDown(mode) && canInteractSkill(i,p) && canActivateSkill(i,p,mode))WorldHop.request(p,i);
    }
}
