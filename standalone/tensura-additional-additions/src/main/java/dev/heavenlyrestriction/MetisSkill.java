package dev.heavenlyrestriction;

import dev.heavenlyrestriction.metis.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.skill.unique.GreatSageSkill;
import io.github.manasmods.tensura.ability.subclass.*;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Native Ultimate; inherited utilities use the same actual skill instance and containers. */
public final class MetisSkill extends Skill
    implements IRefining<MetisSkill>, IRepeatCrafting<MetisSkill> {
  public MetisSkill() {
    super(SkillType.ULTIMATE);
  }

  @Override
  public MutableComponent getName() {
    return Component.translatable("heavenlyrestriction.skill.metis");
  }

  @Override
  public MutableComponent getColoredName() {
    return getName().withStyle(getType().getChatFormatting());
  }

  @Override
  public MutableComponent getSkillDescription() {
    return Component.translatable("heavenlyrestriction.skill.metis.description");
  }

  @Override
  public ResourceLocation getSkillIcon() {
    return HeavenlyRestriction.id("textures/skill/metis.png");
  }

  @Override
  public double getDefaultAcquiringMagiculeCost() {
    return MetisConfig.ACQUISITION.get();
  }

  @Override
  public int getAcquirementMastery(LivingEntity e) {
    return -1;
  }

  @Override
  public boolean addLearnPoint(ManasSkillInstance i, LivingEntity e, int mode, double point) {
    if (e instanceof ServerPlayer p) MetisNetwork.open(p);
    return false;
  }

  @Override
  public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) {
    return i.getMastery() >= 0;
  }

  @Override
  public int getModes(ManasSkillInstance i) {
    return 5;
  }

  @Override
  public int nextMode(LivingEntity e, ManasSkillInstance i, int m, boolean reverse) {
    return Math.floorMod(m + (reverse ? -1 : 1), 5);
  }

  @Override
  public String getModeId(ManasSkillInstance i, int mode) {
    return "metis."
        + switch (mode) {
          case 1 -> "autonomous_sage";
          case 2 -> "appraisal";
          case 3 -> "analysis";
          case 4 -> "refine";
          default -> "parallel_matrix";
        };
  }

  @Override
  public Component getModeName(ManasSkillInstance i, int mode) {
    return Component.translatable("heavenlyrestriction.skill.mode." + getModeId(i, mode));
  }

  @Override
  public void onToggleOn(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(true);
    MetisIntellect.refresh(e);
  }

  @Override
  public void onToggleOff(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(false);
    MetisIntellect.refresh(e);
  }

  @Override
  public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
    MetisCasting.cancel(e);
    MetisIntellect.remove(e);
    ((GreatSageSkill) UniqueSkills.GREAT_SAGE.get()).onForgetSkill(i, e);
    super.onForgetSkill(i, e);
  }

  @Override
  public boolean canTick(ManasSkillInstance i, LivingEntity e) {
    return i.getMastery() >= 0 && i.canInteractSkill(e);
  }

  @Override
  public void onTick(ManasSkillInstance i, LivingEntity e) {
    if (e instanceof Player p && !e.level().isClientSide()) {
      boolean refining = onRefiningTick(i, p), crafting = onRepeatCraftingTick(i, p);
      if (refining || crafting) i.addMasteryPoint(e);
    }
  }

  @Override
  public void onSkillMastered(ManasSkillInstance i, LivingEntity e) {
    super.onSkillMastered(i, e);
    MetisIntellect.remove(e);
    MetisIntellect.refresh(e);
  }

  @Override
  public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
    if (!(e instanceof ServerPlayer p)) return;
    if (i.getMastery() < 0) {
      MetisNetwork.open(p);
      return;
    }
    if (!i.canInteractSkill(e) || !i.canActivateSkill(e, mode)) return;
    switch (mode) {
      case 0 -> MetisNetwork.open(p);
      case 1 -> {
        boolean enabled = !i.getOrCreateTag().getBoolean(MetisMatrix.AUTO);
        if (enabled && MetisMatrix.slots(p, i).isEmpty()) {
          p.displayClientMessage(
              Component.translatable("heavenlyrestriction.split_mind.assign_first"), true);
          return;
        }
        i.getOrCreateTag().putBoolean(MetisMatrix.AUTO, enabled);
        i.markDirty();
        if (!enabled) MetisCasting.cancel(p);
        p.displayClientMessage(
            Component.translatable(
                enabled
                    ? "heavenlyrestriction.metis.auto_on"
                    : "heavenlyrestriction.metis.auto_off"),
            true);
      }
      case 2 -> ((GreatSageSkill) UniqueSkills.GREAT_SAGE.get()).onPressed(i, p, key, 0);
      case 3 -> { // Native Analysis bills its own mode 1. Move that cooldown to Metis' Analysis
        // mode.
        ((GreatSageSkill) UniqueSkills.GREAT_SAGE.get()).onPressed(i, p, key, 1);
        int cd = i.getCoolDown(1);
        i.setCoolDown(0, 1);
        i.setCoolDown(cd, 3);
      }
      case 4 -> {
        if (p.isShiftKeyDown()) openRefiningMenu(p, i);
        else openRepeatCraftingMenu(p, i);
      }
    }
  }

  @Override
  public boolean onDeath(
      ManasSkillInstance i,
      LivingEntity owner,
      net.minecraft.world.damagesource.DamageSource source) {
    return ((GreatSageSkill) UniqueSkills.GREAT_SAGE.get()).onDeath(i, owner, source);
  }

  @Override
  public void onRespawn(ManasSkillInstance i, ServerPlayer p, boolean conqueredEnd) {
    ((GreatSageSkill) UniqueSkills.GREAT_SAGE.get()).onRespawn(i, p, conqueredEnd);
    MetisIntellect.refresh(p);
  }

  @Override
  public int getSpatialStorageIdOffset() {
    return 11;
  }

  @Override
  public boolean isAutoRefiningAllowed() {
    return true;
  }

  @Override
  public boolean hasAutoCraftingTab() {
    return true;
  }

  @Override
  public SpatialStorageContainer getSpatialStorage(
      ManasSkillInstance i, HolderLookup.Provider lookup) {
    var c = new SpatialStorageContainer(20, 128);
    c.fromTag(i.getOrCreateTag().getList("SpatialStorage", 10), lookup);
    return c;
  }

  @Override
  public void openSpatialStoragePage(
      ServerPlayer p, LivingEntity owner, ManasSkillInstance i, int page) {
    openRepeatCraftingMenu(p, owner, i);
  }
}
