package dev.heavenlyrestriction;

import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Exactly one inherited passive and one editor mode. */
public final class LaplaceSkill extends Skill {
  public LaplaceSkill() {
    super(SkillType.ULTIMATE);
  }

  @Override
  public MutableComponent getName() {
    return Component.translatable("heavenlyrestriction.skill.laplace");
  }

  @Override
  public MutableComponent getColoredName() {
    return getName().withStyle(getType().getChatFormatting());
  }

  @Override
  public MutableComponent getSkillDescription() {
    return Component.translatable("heavenlyrestriction.skill.laplace.description");
  }

  @Override
  public ResourceLocation getSkillIcon() {
    return HeavenlyRestriction.id("textures/skill/laplace.png");
  }

  @Override
  public double getDefaultAcquiringMagiculeCost() {
    return LaplaceConfig.ACQUISITION.get();
  }

  @Override
  public int getAcquirementMastery(LivingEntity e) {
    return -1;
  }

  @Override
  public boolean addLearnPoint(ManasSkillInstance i, LivingEntity e, int mode, double point) {
    if (e instanceof ServerPlayer p) LaplaceNetwork.open(p);
    return false;
  }

  @Override
  public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) {
    return i.getMastery() >= 0;
  }

  @Override
  public String getModeId(ManasSkillInstance i, int mode) {
    return "laplace.equation_editor";
  }

  @Override
  public Component getModeName(ManasSkillInstance i, int mode) {
    return Component.translatable("heavenlyrestriction.skill.mode.laplace.equation_editor");
  }

  @Override
  public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
    if (e instanceof ServerPlayer p
        && (i.getMastery() < 0 || i.canInteractSkill(e) && i.canActivateSkill(e, 0)))
      LaplaceNetwork.open(p);
  }

  @Override
  public void onToggleOn(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(true);
    LaplaceIntellect.refresh(e);
    dev.heavenlyrestriction.laplace.expand.ExpandedStates.refreshAttributes(e);
  }

  @Override
  public void onToggleOff(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(false);
    LaplaceIntellect.remove(e);
    dev.heavenlyrestriction.laplace.expand.ExpandedStates.stopped(e);
  }

  @Override
  public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(false);
    LaplaceIntellect.remove(e);
    dev.heavenlyrestriction.laplace.expand.ExpandedStates.stopped(e);
    super.onForgetSkill(i, e);
  }

  @Override
  public boolean canTick(ManasSkillInstance i, LivingEntity e) {
    return i.getMastery() >= 0 && i.isToggled() && i.canInteractSkill(e);
  }

  @Override
  public void onTick(ManasSkillInstance i, LivingEntity e) {
    var tag = i.getOrCreateTag();
    int count = tag.getInt("FormulaTicks");
    if (count % BASE_CONFIG.Mastery.masteryActivateTime == 0) i.addMasteryPoint(e);
    tag.putInt("FormulaTicks", count == Integer.MAX_VALUE ? 0 : count + 1);
  }
}
