package dev.heavenlyrestriction.highrealm;

import dev.heavenlyrestriction.RestrictionData;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.registry.magic.AspectualMagics;
import io.github.manasmods.tensura.registry.skill.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;

/**
 * Native, usable abilities: passives are mastered/enabled, daemon spells drive their existing
 * brains.
 */
public final class HighRealmLoadouts {
  public static final String VERSION_KEY = "heavenlyrestriction:elite_loadout_version";
  public static final int VERSION = 1;

  public record Loadout(List<ManasSkill> skills, List<Magic> spells) {}

  private static Loadout pack(ManasSkill... extra) {
    var skills =
        new LinkedHashSet<ManasSkill>(
            List.of(
                ResistanceSkills.PHYSICAL_ATTACK_RESISTANCE.get(),
                ResistanceSkills.MAGIC_RESISTANCE.get(),
                ExtraSkills.MAGIC_SENSE.get(),
                ExtraSkills.DANGER_SENSE.get(),
                ExtraSkills.ULTRASPEED_REGENERATION.get()));
    skills.addAll(List.of(extra));
    return new Loadout(List.copyOf(skills), List.of());
  }

  private static Loadout caster(boolean greater) {
    var base =
        greater
            ? pack(
                ExtraSkills.MANA_MANIPULATION.get(),
                ExtraSkills.FLAME_MANIPULATION.get(),
                ExtraSkills.WATER_MANIPULATION.get(),
                ExtraSkills.WIND_MANIPULATION.get(),
                ExtraSkills.LIGHTNING_MANIPULATION.get())
            : pack(
                ExtraSkills.MANA_MANIPULATION.get(),
                ExtraSkills.FLAME_MANIPULATION.get(),
                ExtraSkills.WATER_MANIPULATION.get(),
                ExtraSkills.WIND_MANIPULATION.get());
    return new Loadout(
        base.skills(),
        greater
            ? List.of(
                AspectualMagics.FIRE_LANCE.get(),
                AspectualMagics.ACID_SHELL.get(),
                AspectualMagics.ICICLE_LANCE.get(),
                AspectualMagics.TORNADO_BLADE.get(),
                AspectualMagics.THUNDER_ORB.get(),
                AspectualMagics.STONE_SHOT.get(),
                AspectualMagics.SLEEP_MIST.get())
            : List.of(
                AspectualMagics.FIRE.get(),
                AspectualMagics.WATER_CUTTER.get(),
                AspectualMagics.WIND_CUTTER.get(),
                AspectualMagics.STONE_SHOT.get(),
                AspectualMagics.FIRE_BALL.get()));
  }

  public static Loadout forMob(Mob mob) {
    var id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
    if (!id.getNamespace().equals("tensura")) return null;
    return switch (id.getPath()) {
      case "orc" ->
          pack(
              UniqueSkills.BERSERKER.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              ExtraSkills.STRENGTHEN_BODY.get(),
              ResistanceSkills.PAIN_RESISTANCE.get());
      case "armorsaurus" ->
          pack(
              UniqueSkills.SURVIVOR.get(),
              IntrinsicSkills.BODY_ARMOR.get(),
              IntrinsicSkills.SCALE_ARMOR.get(),
              ResistanceSkills.PIERCE_RESISTANCE.get());
      case "horned_bear" ->
          pack(
              UniqueSkills.FIGHTER.get(),
              CommonSkills.STRENGTH.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              ResistanceSkills.PAIN_RESISTANCE.get());
      case "direwolf" ->
          pack(
              ExtraSkills.GODWOLF_SENSE.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              ResistanceSkills.ELECTRICITY_RESISTANCE.get(),
              ResistanceSkills.PARALYSIS_RESISTANCE.get());
      case "blade_tiger" ->
          pack(
              UniqueSkills.SEER.get(),
              UniqueSkills.FIGHTER.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              ResistanceSkills.WIND_ATTACK_RESISTANCE.get());
      case "knight_spider" ->
          pack(
              IntrinsicSkills.BODY_ARMOR.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              CommonSkills.POISON.get(),
              ResistanceSkills.POISON_RESISTANCE.get(),
              ResistanceSkills.PARALYSIS_RESISTANCE.get());
      case "lizardman" ->
          pack(
              IntrinsicSkills.SCALE_ARMOR.get(),
              IntrinsicSkills.WATER_BREATHING.get(),
              UniqueSkills.FIGHTER.get(),
              ResistanceSkills.WATER_ATTACK_RESISTANCE.get());
      case "basilisk" ->
          pack(
              IntrinsicSkills.SCALE_ARMOR.get(),
              CommonSkills.POISON.get(),
              CommonSkills.PARALYSIS.get(),
              ResistanceSkills.POISON_NULLIFICATION.get(),
              ResistanceSkills.PARALYSIS_NULLIFICATION.get());
      case "lesser_daemon" -> caster(false);
      case "greater_daemon" -> caster(true);
      default -> null;
    };
  }

  public static boolean apply(Mob mob) {
    var tag = mob.getPersistentData();
    if (tag.getInt(VERSION_KEY) >= VERSION
        || !HighRealmElites.elite(mob)
        || !HighRealmElites.unowned(mob)) return false;
    var loadout = forMob(mob);
    if (loadout == null) return false;
    // Upgrading a saved elite must not refill a damaged enemy or reset its EP.
    float hp = mob.getHealth();
    var pool = TensuraStorages.getExistenceFrom(mob);
    double ap = pool.getAura(), mp = pool.getMagicule(), shp = pool.getSpiritualHealth();
    for (var skill : loadout.skills()) grant(mob, skill, true);
    for (var spell : loadout.spells()) grant(mob, spell, false);
    mob.setHealth(Math.min(hp, mob.getMaxHealth()));
    pool.setAura(Math.min(ap, EnergyHelper.getMaxAura(mob)));
    pool.setMagicule(Math.min(mp, EnergyHelper.getMaxMagicule(mob)));
    pool.setSpiritualHealth(shp);
    pool.markDirty();
    tag.putInt(VERSION_KEY, VERSION);
    return true;
  }

  private static void grant(Mob mob, ManasSkill skill, boolean enable) {
    var storage = SkillAPI.getSkillsFrom(mob);
    if (storage.getSkill(skill).isEmpty()) RestrictionData.learnReward(mob, skill);
    storage
        .getSkill(skill)
        .ifPresent(
            i -> {
              i.setMastery(i.getMaxMastery());
              // These native Unique passives explicitly require the active preset.
              if (skill == UniqueSkills.FIGHTER.get() || skill == UniqueSkills.BERSERKER.get())
                TensuraStorages.getAbilityFrom(mob).setAbilitySlot(0, 0, skill, 0);
              if (enable && i.canBeToggled(mob) && !i.isToggled()) {
                i.setToggled(true);
                i.onToggleOn(mob);
              }
              i.markDirty();
            });
  }

  private HighRealmLoadouts() {}
}
