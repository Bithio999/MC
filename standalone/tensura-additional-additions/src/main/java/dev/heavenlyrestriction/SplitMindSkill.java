package dev.heavenlyrestriction;

import dev.heavenlyrestriction.splitmind.*;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.skill.extra.ThoughtAccelerationSkill;
import io.github.manasmods.tensura.config.ability.skill.ExtraSkillConfig;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

/** One passive toggle, with a separate persisted auto-cast switch. */
public final class SplitMindSkill extends Skill {
  public static final ResourceLocation BONUS = HeavenlyRestriction.id("split_mind");

  public SplitMindSkill() {
    super(SkillType.UNIQUE);
  }

  @Override
  public MutableComponent getName() {
    return Component.translatableWithFallback("heavenlyrestriction.skill.split_mind", "Split Mind");
  }

  @Override
  public MutableComponent getColoredName() {
    return getName().withStyle(getType().getChatFormatting());
  }

  @Override
  public MutableComponent getSkillDescription() {
    return Component.translatable("heavenlyrestriction.skill.split_mind.description");
  }

  @Override
  public ResourceLocation getSkillIcon() {
    return HeavenlyRestriction.id("textures/skill/split_mind.png");
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
  public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) {
    return i.getMastery() >= 0;
  }

  @Override
  public int getModes(ManasSkillInstance i) {
    return 2;
  }

  @Override
  public int nextMode(LivingEntity e, ManasSkillInstance i, int m, boolean reverse) {
    return m == 0 ? 1 : 0;
  }

  @Override
  public String getModeId(ManasSkillInstance i, int mode) {
    return mode == 0 ? "split_mind.magic_assignment" : "split_mind.auto_cast";
  }

  @Override
  public Component getModeName(ManasSkillInstance i, int mode) {
    return Component.translatable("heavenlyrestriction.skill.mode." + getModeId(i, mode));
  }

  @Override
  public void onToggleOn(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(true);
    apply(i, e);
  }

  @Override
  public void onToggleOff(ManasSkillInstance i, LivingEntity e) {
    i.setToggled(false);
    remove(e);
  }

  @Override
  public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
    remove(e);
    SplitMindCasting.cancel(e);
    super.onForgetSkill(i, e);
  }

  @Override
  public void onSkillMastered(ManasSkillInstance i, LivingEntity e) {
    super.onSkillMastered(i, e);
    if (i.isToggled()) {
      remove(e);
      apply(i, e);
    }
  }

  @Override
  public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
    if (!(e instanceof ServerPlayer p)
        || i.getMastery() < 0
        || !canInteractSkill(i, e)
        || !canActivateSkill(i, e, mode)) return;
    if (mode == 0) SplitMindNetwork.open(p);
    else if (mode == 1) {
      boolean enabled = !i.getOrCreateTag().getBoolean(SplitMindCasting.AUTO);
      if (enabled && SplitMindCasting.assigned(p, i).isEmpty()) {
        p.displayClientMessage(
            Component.translatable("heavenlyrestriction.split_mind.assign_first"), true);
        return;
      }
      i.getOrCreateTag().putBoolean(SplitMindCasting.AUTO, enabled);
      i.markDirty();
      if (!enabled) SplitMindCasting.cancel(p);
      p.displayClientMessage(
          Component.translatable(
              enabled
                  ? "heavenlyrestriction.split_mind.auto_on"
                  : "heavenlyrestriction.split_mind.auto_off"),
          true);
    }
  }

  public static boolean active(LivingEntity e) {
    return SkillAPI.getSkillsFrom(e)
        .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
        .filter(i -> i.getMastery() >= 0 && i.isToggled() && i.canInteractSkill(e))
        .isPresent();
  }

  public static void refresh(LivingEntity e) {
    var i = SkillAPI.getSkillsFrom(e).getSkill(HeavenlyRestriction.SPLIT_MIND.get());
    if (i.isPresent() && active(e)) apply(i.get(), e);
    else remove(e);
  }

  private static void apply(ManasSkillInstance i, LivingEntity e) {
    if (e.level().isClientSide()) return;
    var c =
        ((ExtraSkillConfig) ConfigRegistry.getConfig(ExtraSkillConfig.class)).ThoughtAcceleration;
    ThoughtAccelerationSkill.onToggle(i, e, BONUS, true);
    modifier(e, TensuraAttributes.ABILITY_LEARNING_GAIN, 9);
    modifier(e, TensuraAttributes.ABILITY_MASTERY_GAIN, 9);
    modifier(e, TensuraAttributes.CHANT_SPEED, c.chantSpeed);
    modifier(
        e, TensuraAttributes.DODGE_INVULNERABILITY, i.isMastered(e) ? c.dodgeInvulnerability : 0);
  }

  private static void modifier(LivingEntity e, Holder<Attribute> a, double value) {
    var at = e.getAttribute(a);
    if (at == null) return;
    var old = at.getModifier(BONUS);
    if (value == 0) {
      if (old != null) at.removeModifier(BONUS);
    } else if (old == null || old.amount() != value)
      at.addOrReplacePermanentModifier(
          new AttributeModifier(BONUS, value, AttributeModifier.Operation.ADD_VALUE));
  }

  private static void remove(LivingEntity e) {
    if (e.level().isClientSide()) return;
    for (var a :
        java.util.List.of(
            Attributes.MOVEMENT_SPEED,
            Attributes.ATTACK_SPEED,
            TensuraAttributes.CHANT_SPEED,
            TensuraAttributes.DODGE_INVULNERABILITY,
            TensuraAttributes.ABILITY_LEARNING_GAIN,
            TensuraAttributes.ABILITY_MASTERY_GAIN)) {
      var at = e.getAttribute(a);
      if (at != null && at.hasModifier(BONUS)) at.removeModifier(BONUS);
    }
  }
}
