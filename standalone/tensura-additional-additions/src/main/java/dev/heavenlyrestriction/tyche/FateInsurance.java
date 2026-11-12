package dev.heavenlyrestriction.tyche;

import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.effect.template.TensuraMobEffect;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.*;
import java.util.List;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** The native cooldown slot is only a history-reset clock, never a global revival lock. */
public final class FateInsurance {
  public static final int MODE = 2, RESET_SECONDS = 300;
  public static final String HISTORY = "TycheDamageHistory";

  public static void expire(ManasSkillInstance i) {
    if (!i.onCoolDown(MODE) && i.getOrCreateTag().contains(HISTORY)) {
      i.getOrCreateTag().remove(HISTORY);
      i.markDirty();
    }
  }

  public static boolean revive(ManasSkillInstance i, LivingEntity e, DamageSource source) {
    if (e.level().isClientSide()
        || i.getMastery() < 0
        || !i.canInteractSkill(e)
        || e.isAlive()
        || source.is(DamageTypes.FELL_OUT_OF_WORLD)
        || source.is(DamageTypes.GENERIC_KILL)
        || ((TensuraDamageSource) source).tensura$getBarrierBypassLevel() >= 3) return false;
    var attacker = source.getEntity();
    if (attacker == e
        || attacker != null
            && attacker.getUUID().equals(SubordinateHelper.getSubordinateOwnerUUID(e)))
      return false;
    expire(i);
    var history = i.getOrCreateTag().getCompound(HISTORY);
    String key = source.getMsgId(); // Same grouping used by native Tuner.
    if (history.contains(key)) return false;
    boolean first = !i.onCoolDown(MODE);
    history.putBoolean(key, true);
    i.getOrCreateTag().put(HISTORY, history);
    if (first) i.setCoolDown(RESET_SECONDS, MODE);
    e.setHealth(e.getMaxHealth());
    e.invulnerableTime = Math.max(60, e.invulnerableTime);
    TensuraMobEffect.removePredicateEffect(
        e, h -> h.value().getCategory() == MobEffectCategory.HARMFUL);
    TensuraStorages.resetEffect(e);
    var energy = TensuraStorages.getExistenceFrom(e);
    energy.setSpiritualHealth(e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));
    energy.setAura(EnergyHelper.getMaxAura(e));
    energy.setMagicule(EnergyHelper.getMaxMagicule(e));
    energy.markDirty();
    resetOtherCooldowns(e, i);
    i.addMasteryPoint(e);
    i.markDirty();
    SkillAPI.getSkillsFrom(e).checkAndMarkDirty(i);
    Tyche.refresh(e);
    Tyche.visual(e, ArtVisualEntity.TYCHE_REBIRTH, 2, 40);
    return true;
  }

  public static void resetOtherCooldowns(LivingEntity e, ManasSkillInstance insurance) {
    for (var s : List.copyOf(SkillAPI.getSkillsFrom(e).getLearnedSkills())) {
      if (s == insurance || s.getSkill() == HeavenlyRestriction.TYCHE.get()) {
        for (int mode = 0; mode < s.getModes(); mode++) if (mode != MODE) s.setCoolDown(0, mode);
      } else s.setCoolDowns(0);
      s.markDirty();
      SkillAPI.getSkillsFrom(e).checkAndMarkDirty(s);
    }
  }

  private FateInsurance() {}
}
