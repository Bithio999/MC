package dev.heavenlyrestriction.highrealm.boss;

import dev.heavenlyrestriction.RestrictionData;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.battlewill.*;
import io.github.manasmods.tensura.registry.magic.*;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;

/**
 * Native stored instances, mastery and toggles; the humanoid's actual preset enables its Uniques.
 */
public final class WayfarerLoadout {
  public static List<ManasSkill> uniques() {
    return List.of(
        UniqueSkills.MARTIAL_MASTER.get(),
        UniqueSkills.MATHEMATICIAN.get(),
        UniqueSkills.SEER.get());
  }

  public static List<ManasSkill> support() {
    return List.of(
        ExtraSkills.STEEL_STRENGTH.get(),
        ExtraSkills.STRENGTHEN_BODY.get(),
        ExtraSkills.ULTRASPEED_REGENERATION.get(),
        ExtraSkills.UNIVERSAL_PERCEPTION.get(),
        ExtraSkills.THOUGHT_ACCELERATION.get(),
        ExtraSkills.CHANT_ANNULMENT.get(),
        ExtraSkills.MANA_MANIPULATION.get(),
        ExtraSkills.WIND_MANIPULATION.get(),
        ResistanceSkills.PHYSICAL_ATTACK_RESISTANCE.get(),
        ResistanceSkills.MAGIC_RESISTANCE.get(),
        ResistanceSkills.SPIRITUAL_ATTACK_RESISTANCE.get(),
        ResistanceSkills.FLAME_ATTACK_RESISTANCE.get(),
        ResistanceSkills.EARTH_ATTACK_RESISTANCE.get(),
        ResistanceSkills.HOLY_ATTACK_RESISTANCE.get(),
        ResistanceSkills.DARKNESS_ATTACK_RESISTANCE.get(),
        ResistanceSkills.WATER_ATTACK_RESISTANCE.get(),
        ResistanceSkills.SPATIAL_ATTACK_NULLIFICATION.get(),
        ResistanceSkills.WIND_ATTACK_NULLIFICATION.get(),
        ResistanceSkills.POISON_NULLIFICATION.get(),
        ResistanceSkills.PARALYSIS_NULLIFICATION.get(),
        ResistanceSkills.PAIN_NULLIFICATION.get(),
        MeleeArts.AURA_SWORD.get(),
        MeleeArts.AURA_SLASH.get(),
        UtilityArts.INSTANT_MOVE.get());
  }

  public static List<ManasSkill> magic() {
    return List.of(
        AspectualMagics.WIND_CUTTER.get(),
        AspectualMagics.TORNADO_BLADE.get(),
        SpiritualMagics.AERIAL_BLADE.get());
  }

  public static void apply(KaelVeyrEntity boss) {
    for (var skill : uniques()) grant(boss, skill, true);
    for (var skill : support()) grant(boss, skill, true);
    for (var skill : magic()) grant(boss, skill, false);
    for (int slot = 0; slot < uniques().size(); slot++)
      TensuraStorages.getAbilityFrom(boss).setAbilitySlot(0, slot, uniques().get(slot), 0);
  }

  private static void grant(KaelVeyrEntity boss, ManasSkill skill, boolean toggle) {
    var storage = SkillAPI.getSkillsFrom(boss);
    if (storage.getSkill(skill).isEmpty()) RestrictionData.learnReward(boss, skill);
    var instance = storage.getSkill(skill).orElseThrow();
    instance.setMastery(instance.getMaxMastery());
    if (toggle && instance.canBeToggled(boss) && !instance.isToggled()) {
      instance.setToggled(true);
      instance.onToggleOn(boss);
    }
    instance.markDirty();
  }

  private WayfarerLoadout() {}
}
