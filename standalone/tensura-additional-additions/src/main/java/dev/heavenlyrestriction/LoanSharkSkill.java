package dev.heavenlyrestriction;
import dev.heavenlyrestriction.loans.LoanShark;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
public final class LoanSharkSkill extends Skill {
    public LoanSharkSkill(){super(SkillType.UNIQUE);}
    @Override public MutableComponent getName(){return Component.translatableWithFallback("heavenlyrestriction.skill.loan_shark","Loan Shark");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(getType().getChatFormatting());}
    @Override public ResourceLocation getSkillIcon(){return HeavenlyRestriction.id("textures/skill/loan_shark.png");}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Loan: aim at a subordinate or player to lend a copy of a learned skill, retaining mastery. While the loan is in use, half of all permanent EP gains go to you. Report: inspect loans, EP income, use time and idle intervals. Claim always recalls the copy; after 10 uninterrupted minutes unused, also steal a random eligible skill you do not own, retaining its mastery. No resource cost.");}
    @Override public int getModes(ManasSkillInstance i){return 2;}
    @Override public String getModeId(ManasSkillInstance i,int mode){return mode==0?"loan_shark.loan":"loan_shark.report";}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal(mode==0?"Loan":"Report");}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int m){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int m){return 0;}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){if(e instanceof ServerPlayer p && mode>=0 && mode<2 && i.getMastery()>=0 && canInteractSkill(i,p) && canActivateSkill(i,p,mode))LoanShark.open(p,mode);}
}
