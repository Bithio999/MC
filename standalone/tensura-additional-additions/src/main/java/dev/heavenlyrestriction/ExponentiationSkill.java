package dev.heavenlyrestriction;
import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
/** A learned passive activated by the currently selected native ability preset, not a toggle. */
public final class ExponentiationSkill extends Skill {
    public ExponentiationSkill(){super(SkillType.UNIQUE);}
    @Override public MutableComponent getName(){return Component.translatableWithFallback("heavenlyrestriction.skill.exponentiation","Exponentiation");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(getType().getChatFormatting());}
    @Override public ResourceLocation getSkillIcon(){return HeavenlyRestriction.id("textures/skill/exponentiation.png");}
    @Override public MutableComponent getSkillDescription(){return Component.translatable("heavenlyrestriction.skill.exponentiation.description");}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public boolean canTick(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.translatable("heavenlyrestriction.skill.mode.exponentiation.exponentiation");}
}
