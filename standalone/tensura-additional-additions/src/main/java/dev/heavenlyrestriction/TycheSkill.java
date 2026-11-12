package dev.heavenlyrestriction;

import dev.heavenlyrestriction.critical.CriticalMerchant;
import dev.heavenlyrestriction.tyche.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class TycheSkill extends Skill {
  public TycheSkill() {
    super(SkillType.ULTIMATE);
  }

  @Override
  public MutableComponent getName() {
    return Component.translatable("heavenlyrestriction.skill.tyche");
  }

  @Override
  public MutableComponent getColoredName() {
    return getName().withStyle(getType().getChatFormatting());
  }

  @Override
  public MutableComponent getSkillDescription() {
    return Component.translatable("heavenlyrestriction.skill.tyche.description");
  }

  @Override
  public ResourceLocation getSkillIcon() {
    return HeavenlyRestriction.id("textures/skill/tyche.png");
  }

  @Override
  public double getDefaultAcquiringMagiculeCost() {
    return TycheConfig.ACQUISITION.get();
  }

  @Override
  public int getAcquirementMastery(LivingEntity e) {
    return -1;
  }

  @Override
  public boolean addLearnPoint(ManasSkillInstance i, LivingEntity e, int mode, double point) {
    if (e instanceof ServerPlayer p) TycheNetwork.open(p);
    return false;
  }

  @Override
  public int getModes(ManasSkillInstance i) {
    return 3;
  }

  @Override
  public int nextMode(LivingEntity e, ManasSkillInstance i, int m, boolean reverse) {
    return 0; // Dividend is the only castable combat mode.
  }

  @Override
  public String getModeId(ManasSkillInstance i, int m) {
    return "tyche." + (m == 1 ? "odds" : m == 2 ? "insurance" : "dividend");
  }

  @Override
  public Component getModeName(ManasSkillInstance i, int m) {
    return Component.translatable(
        "heavenlyrestriction.skill.mode." + getModeId(i, m),
        m == 0 ? CriticalMerchant.remaining(i) : (Tyche.oddsTicks(i) + 19) / 20);
  }

  @Override
  public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) {
    return i.getMastery() >= 0;
  }

  @Override
  public void onToggleOn(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(true);
    Tyche.checkOdds(e);
    Tyche.refresh(e);
  }

  @Override
  public void onToggleOff(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(false);
    Tyche.clearOdds(i, e);
  }

  @Override
  public boolean canIgnoreCoolDown(ManasSkillInstance i, LivingEntity e, int mode) {
    return i.getMastery() < 0
        || mode == FateInsurance.MODE
        || mode == Tyche.ODDS_MODE
        || mode == 0 && e.isShiftKeyDown();
  }

  @Override
  public double getMagiculeCost(LivingEntity e, ManasSkillInstance i, int m) {
    return m == 0 && e.isShiftKeyDown() ? 0 : m == 0 ? TycheConfig.DIVIDEND_COST.get() : 0;
  }

  @Override
  public double getAuraCost(LivingEntity e, ManasSkillInstance i, int m) {
    return 0;
  }

  @Override
  public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
    if (i.getMastery() < 0) {
      if (e instanceof ServerPlayer p) TycheNetwork.open(p);
      return;
    }
    if ((mode == FateInsurance.MODE || mode == Tyche.ODDS_MODE || mode == 0 && e.isShiftKeyDown())
        && e instanceof ServerPlayer p) {
      TycheNetwork.open(p);
      return;
    }
    if (mode == 0) CriticalMerchant.press(i, e);
  }

  @Override
  public boolean onDeath(ManasSkillInstance i, LivingEntity e, DamageSource source) {
    return !FateInsurance.revive(i, e, source);
  }

  @Override
  public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
    Tyche.clear(i, e);
    super.onForgetSkill(i, e);
  }

  @Override
  public void onRespawn(ManasSkillInstance i, ServerPlayer p, boolean end) {
    super.onRespawn(i, p, end);
    Tyche.clear(i, p);
  }
}
