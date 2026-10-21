package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** A real Ultimate source and native cooldown/resource authority for Ananke's four rites. */
public final class MoiraSkill extends Skill {
    public static final String[] NAMES={"Fatebreaker","Starfall Canticle","Eventide Procession","The Last Dawn"};
    public static final int[] COOLDOWNS={4,10,12,24};
    public static final double[] COSTS={4000,10000,14000,50000};
    public MoiraSkill(){super(SkillType.ULTIMATE);}
    @Override public MutableComponent getName(){return Component.literal("Moira, Lord of Finality");}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Ananke's intrinsic Ultimate. Fatebreaker cleaves a warned fan; Starfall Canticle rains individually warned stellar lances; Eventide Procession expands a hollow shock front; The Last Dawn judges a 32-block sphere except its golden 7–12-block refuge. Native aura-slash, light, darkness and space damage. Costs magicules; no EP damage scaling. Not obtainable through player synthesis.");}
    @Override public ResourceLocation getSkillIcon(){return WildHunt.id("textures/skill/moira.png");}
    @Override public int getMaxMastery(){return 10000;}
    @Override public int getModes(ManasSkillInstance i){return 4;}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "moira."+Math.clamp(mode,0,3);}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal(NAMES[Math.clamp(mode,0,3)]);}
    @Override public boolean canBeSlotted(ManasSkillInstance i,LivingEntity e,int mode){return e instanceof AnankeEntity;}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return mode>=0 && mode<4?COSTS[mode]:0;}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity caster,int key,int mode){
        if(!(caster instanceof AnankeEntity boss) || !(caster.level() instanceof ServerLevel level) || mode<0 || mode>3 || boss.isCasting() || i.onCoolDown(mode)
            || !canInteractSkill(i,caster) || !canActivateSkill(i,caster,mode) || boss.getTarget()==null || !boss.eligible(boss.getTarget()))return;
        if(EnergyHelper.isOutOfEnergy(caster,i,mode))return;
        AnankeRites.begin(level,boss,mode);i.setCoolDown(COOLDOWNS[mode],mode);i.markDirty();
    }
    void hit(AnankeEntity boss,LivingEntity target,int mode){
        if(!boss.eligible(target))return;
        var i=io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(boss).getSkill(this).orElseThrow();
        var type=switch(mode){case 0->TensuraDamageTypes.AURA_SLASH;case 1->TensuraDamageTypes.LIGHT_ELEMENTAL;case 2->TensuraDamageTypes.DARKNESS_ELEMENTAL;default->TensuraDamageTypes.SPACE_ELEMENTAL;};
        float damage=switch(mode){case 0->1800;case 1->1000;case 2->2200;default->4800;};
        // Values are fixed encounter damage; native defenses and Godly ownership traits remain separate.
        target.hurt(createSource(i,boss,type,mode),damage);
    }
}
