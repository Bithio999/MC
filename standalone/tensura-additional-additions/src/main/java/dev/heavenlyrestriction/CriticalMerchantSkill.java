package dev.heavenlyrestriction;

import dev.heavenlyrestriction.critical.CriticalMerchant;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class CriticalMerchantSkill extends Skill {
  public CriticalMerchantSkill() {
    super(SkillType.UNIQUE);
  }

  @Override
  public MutableComponent getName() {
    return Component.translatableWithFallback(
        "heavenlyrestriction.skill.critical_merchant", "Critical Merchant");
  }

  @Override
  public MutableComponent getColoredName() {
    return getName().withStyle(getType().getChatFormatting());
  }

  @Override
  public ResourceLocation getSkillIcon() {
    return HeavenlyRestriction.id("textures/skill/critical_merchant.png");
  }

  @Override
  public MutableComponent getSkillDescription() {
    return Component.translatable("heavenlyrestriction.skill.critical_merchant.description");
  }

  @Override
  public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) {
    return i.getMastery() >= 0;
  }

  @Override
  public void onToggleOn(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(true);
  }

  @Override
  public void onToggleOff(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(false);
  }

  @Override
  public boolean canTick(ManasSkillInstance i, LivingEntity e) {
    return false;
  }

  @Override
  public String getModeId(ManasSkillInstance i, int mode) {
    return "critical_merchant.guaranteed_profit";
  }

  @Override
  public Component getModeName(ManasSkillInstance i, int mode) {
    return Component.translatable(
        "heavenlyrestriction.skill.mode.critical_merchant.guaranteed_profit",
        CriticalMerchant.remaining(i));
  }

  @Override
  public double getDefaultAcquiringMagiculeCost() {
    return 0;
  }

  @Override
  public double getAuraCost(LivingEntity e, ManasSkillInstance i, int mode) {
    return 0;
  }

  @Override
  public double getMagiculeCost(LivingEntity e, ManasSkillInstance i, int mode) {
    return CriticalMerchant.COST;
  }

  @Override
  public void onPressed(ManasSkillInstance i, LivingEntity e, int slot, int mode) {
    if (mode == 0) CriticalMerchant.press(i, e);
  }

  @Override
  public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
    CriticalMerchant.clear(i);
    super.onForgetSkill(i, e);
  }
}
