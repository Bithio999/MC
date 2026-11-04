package dev.heavenlyrestriction;

import dev.heavenlyrestriction.cartography.MapService;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class MaraudersMapSkill extends Skill {
  public MaraudersMapSkill() {
    super(SkillType.UNIQUE);
  }

  @Override
  public MutableComponent getName() {
    return Component.translatableWithFallback(
        "heavenlyrestriction.skill.marauders_map", "Marauder's Map");
  }

  @Override
  public MutableComponent getColoredName() {
    return getName().withStyle(getType().getChatFormatting());
  }

  @Override
  public MutableComponent getSkillDescription() {
    return Component.translatable("heavenlyrestriction.skill.marauders_map.description");
  }

  @Override
  public ResourceLocation getSkillIcon() {
    return HeavenlyRestriction.id("textures/skill/marauders_map.png");
  }

  @Override
  public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) {
    return false;
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
    return 0;
  }

  @Override
  public String getModeId(ManasSkillInstance i, int mode) {
    return "marauders_map.open_map";
  }

  @Override
  public Component getModeName(ManasSkillInstance i, int mode) {
    return Component.translatable("heavenlyrestriction.skill.mode.marauders_map.open_map");
  }

  @Override
  public void onPressed(ManasSkillInstance i, LivingEntity e, int slot, int mode) {
    if (e instanceof ServerPlayer p
        && i.getMastery() >= 0
        && i.canInteractSkill(p)
        && i.canActivateSkill(p, mode)) MapService.open(p);
  }
}
