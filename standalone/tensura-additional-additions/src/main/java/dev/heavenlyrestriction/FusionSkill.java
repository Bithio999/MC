package dev.heavenlyrestriction;
import dev.heavenlyrestriction.fusion.Fusion;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
public final class FusionSkill extends Skill {
    public FusionSkill(){super(SkillType.UNIQUE);}
    @Override public MutableComponent getName(){return Component.translatableWithFallback("heavenlyrestriction.skill.fusion","Fusion");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(getType().getChatFormatting());}
    @Override public ResourceLocation getSkillIcon(){return HeavenlyRestriction.id("textures/skill/fusion.png");}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Fuse with your subordinate or a consenting player for 3 minutes. Fused EP = 1.5 × both participants' combined EP; HP and SHP combine by addition, including remaining health; aura and magicule regeneration ×10; final damage ×2. Temporarily share skills. Control your subordinate's fused form; a receiving player controls their own body. Press Fuse again to separate. Both receive Paralysis II and Fragility II for 5 minutes after separation. No resource cost.");}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "fusion.fuse";}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.translatableWithFallback("heavenlyrestriction.skill.mode.fusion.fuse","Fuse");}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int m){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int m){return 0;}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){
        if(e instanceof ServerPlayer p && i.getMastery()>=0 && canInteractSkill(i,p) && canActivateSkill(i,p,mode))Fusion.press(p);
    }
}
