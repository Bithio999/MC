package dev.heavenlyrestriction;

import dev.heavenlyrestriction.forge.*;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.skill.unique.GodlyCraftsmanSkill;
import io.github.manasmods.tensura.ability.subclass.*;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A native Ultimate, preserving the source skills' refining and engraving protocols. */
public final class HephaestusSkill extends Skill
    implements IRefining<HephaestusSkill>, IResearcherEnchanter {
  public HephaestusSkill() {
    super(SkillType.ULTIMATE);
  }

  @Override
  public MutableComponent getName() {
    return Component.translatableWithFallback(
        "heavenlyrestriction.skill.hephaestus", "Hephaestus, Lord of the Forge");
  }

  @Override
  public MutableComponent getColoredName() {
    return getName().withStyle(getType().getChatFormatting());
  }

  @Override
  public MutableComponent getSkillDescription() {
    return Component.translatable("heavenlyrestriction.skill.hephaestus.description");
  }

  @Override
  public ResourceLocation getSkillIcon() {
    return HeavenlyRestriction.id("textures/skill/hephaestus.png");
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
  public int getModes(ManasSkillInstance i) {
    return 3;
  }

  @Override
  public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) {
    return Math.floorMod(mode + (reverse ? -1 : 1), 3);
  }

  @Override
  public String getModeId(ManasSkillInstance i, int mode) {
    return "hephaestus."
        + switch (mode) {
          case 1 -> "living_arsenal";
          case 2 -> "magnum_opus";
          default -> "divine_forge";
        };
  }

  @Override
  public Component getModeName(ManasSkillInstance i, int mode) {
    return Component.translatable("heavenlyrestriction.skill.mode." + getModeId(i, mode));
  }

  @Override
  public double getDefaultAcquiringMagiculeCost() {
    return ForgeConfig.ACQUISITION.get();
  }

  @Override
  public int getAcquirementMastery(LivingEntity e) {
    return -1;
  }

  @Override
  public boolean addLearnPoint(ManasSkillInstance i, LivingEntity e, int mode, double point) {
    if (e instanceof ServerPlayer p) ForgeService.open(p, "synthesis");
    return false;
  }

  @Override
  public void onPressed(ManasSkillInstance i, LivingEntity e, int slot, int mode) {
    if (!(e instanceof ServerPlayer p)) return;
    if (i.getMastery() < 0) {
      ForgeService.open(p, "synthesis");
      return;
    }
    if (!canInteractSkill(i, e) || !canActivateSkill(i, e, mode)) return;
    if (mode == 1) {
      if (ForgeArsenal.active(p)) ForgeArsenal.recall(p);
      else ForgeService.open(p, "arsenal");
    } else ForgeService.open(p, mode == 2 ? "masterpiece" : "forge");
  }

  @Override
  public boolean canTick(ManasSkillInstance i, LivingEntity e) {
    return i.getMastery() >= 0 && canInteractSkill(i, e);
  }

  @Override
  public void onTick(ManasSkillInstance i, LivingEntity e) {
    if (e instanceof Player p && !e.level().isClientSide() && onRefiningTick(i, p))
      i.addMasteryPoint(e);
  }

  @Override
  public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
    if (e instanceof ServerPlayer p) ForgeArsenal.recall(p);
    super.onForgetSkill(i, e);
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
  public SpatialStorageContainer getSpatialStorage(
      ManasSkillInstance i, HolderLookup.Provider lookup) {
    var c = new SpatialStorageContainer(54, 256);
    c.fromTag(i.getOrCreateTag().getList("SpatialStorage", 10), lookup);
    return c;
  }

  private GodlyCraftsmanSkill craftsman() {
    return (GodlyCraftsmanSkill) UniqueSkills.GODLY_CRAFTSMAN.get();
  }

  @Override
  public int getMaximumBonusLevel() {
    return craftsman().getMaximumBonusLevel();
  }

  @Override
  public List<String> getBlacklistEnchantments() {
    return craftsman().getBlacklistEnchantments();
  }

  @Override
  public List<String> getBlackListBonusLevelEnchantments() {
    return craftsman().getBlackListBonusLevelEnchantments();
  }

  @Override
  public float getCurseChancePerEngraving() {
    return craftsman().getCurseChancePerEngraving();
  }

  @Override
  public boolean isAllowedToCopyEnchantments(LivingEntity e, ItemStack s) {
    return craftsman().isAllowedToCopyEnchantments(e, s);
  }
}
