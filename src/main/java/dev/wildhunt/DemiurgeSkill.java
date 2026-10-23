package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Boss-exclusive Godly, carried as a native Ultimate for damage/cooldown compatibility. */
public final class DemiurgeSkill extends Skill {
    public static final String[] NAMES={"Crownless Edge","Astral Loom","Sevenfold Genesis","Null Testament","World-Eater's Wake","Primordial Exchange","Thronefall","The End of All Myths"};
    public static final int[] COOLDOWNS={6,12,14,18,18,24,16,42};
    public static final double[] AURA={400_000,0,0,0,1_200_000,20_000_000,2_000_000,0};
    public static final double[] MAGICULES={0,900_000,1_800_000,4_000_000,0,0,0,25_000_000};
    public DemiurgeSkill(){super(SkillType.ULTIMATE);}
    @Override public MutableComponent getName(){return Component.literal("Demiurge, God of Genesis");}
    @Override public MutableComponent getColoredName(){return getName().withColor(0x9DF7ED);}
    @Override public MutableComponent getSkillDescription(){return Component.literal("[Godly] Eschaton's intrinsic authority over the birth and end of worlds. Eight real native modes: a threefold cleave, rotating stellar rays, seven elemental impacts, silencing spatial cuts, gapped world-eater fronts, paid aura conversion, a warned thronefall and an interruptible apocalypse. Creation seals can be broken to drain his native resources and open refuges. Encounter-exclusive; not a reincarnation roll or player synthesis."+GodlyPassives.DESCRIPTION);}
    @Override public ResourceLocation getSkillIcon(){return WildHunt.id("textures/skill/demiurge.png");}
    @Override public int getMaxMastery(){return 100_000;}
    @Override public int getModes(ManasSkillInstance i){return NAMES.length;}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "demiurge."+Math.clamp(mode,0,7);}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal(NAMES[Math.clamp(mode,0,7)]);}
    @Override public boolean canBeSlotted(ManasSkillInstance i,LivingEntity e,int mode){return e instanceof EschatonEntity;}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return mode>=0 && mode<8?AURA[mode]:0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return mode>=0 && mode<8?MAGICULES[mode]:0;}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int key,int mode){
        if(!(e instanceof EschatonEntity boss) || !(e.level() instanceof ServerLevel level) || mode<0 || mode>=8 || boss.isCasting() || boss.isStaggered() || i.onCoolDown(mode)
            || !canInteractSkill(i,e) || !canActivateSkill(i,e,mode) || !EschatonRites.canBegin(boss,mode) || EnergyHelper.isOutOfEnergy(e,i,mode))return;
        EschatonRites.begin(level,boss,mode);i.setCoolDown(COOLDOWNS[mode],mode);i.markDirty();
    }
    void hit(EschatonEntity boss,LivingEntity target,int mode,int chapter){
        if(!boss.eligible(target))return;
        var i=SkillAPI.getSkillsFrom(boss).getSkill(this).orElseThrow();
        var type=switch(mode){case 0->TensuraDamageTypes.AURA_SLASH;case 1->TensuraDamageTypes.LIGHT_ELEMENTAL;case 2->ThothElement.at(chapter).damage;case 3,7->TensuraDamageTypes.SPACE_ELEMENTAL;case 4->TensuraDamageTypes.DARKNESS_ELEMENTAL;default->TensuraDamageTypes.EARTH_ELEMENTAL;};
        float amount=switch(mode){case 0->14_000;case 1->6_500;case 2->18_000;case 3->28_000;case 4->18_000;case 6->36_000;default->120_000;};
        var source=createSource(i,boss,type,mode);((IndependentHitSource)source).myths$independentHit();
        // Native attributes, defenses and Godly ownership apply afterwards. No EP damage multiplier.
        if(target.hurt(source,amount) && mode==3)EschatonRites.silence(boss,target,i);
    }
}
