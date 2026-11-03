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
  public static final int VERSION = 3;

  public record Loadout(List<ManasSkill> skills, List<Magic> spells) {}

  private static Loadout pack(ManasSkill... extra) {
    var skills =
        new LinkedHashSet<ManasSkill>(
            List.of(
                ResistanceSkills.PHYSICAL_ATTACK_RESISTANCE.get(),
                ResistanceSkills.MAGIC_RESISTANCE.get(),
                ResistanceSkills.PAIN_RESISTANCE.get(),
                ResistanceSkills.SPIRITUAL_ATTACK_RESISTANCE.get(),
                ExtraSkills.MAGIC_SENSE.get(),
                ExtraSkills.DANGER_SENSE.get(),
                ExtraSkills.ULTRASPEED_REGENERATION.get()));
    skills.addAll(List.of(extra));
    if (skills.contains(ExtraSkills.STEEL_STRENGTH.get()))
      skills.remove(CommonSkills.STRENGTH.get());
    // A nullification replaces its lower tier instead of stacking redundant defenses.
    for (var skill : List.copyOf(skills))
      if (skill.getRegistryName().getPath().endsWith("_nullification")) {
        var lower =
            SkillAPI.getSkillRegistry()
                .get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        "tensura",
                        skill
                            .getRegistryName()
                            .getPath()
                            .replace("_nullification", "_resistance")));
        if (lower != null) skills.remove(lower);
      }
    return new Loadout(List.copyOf(skills), List.of());
  }

  private static Loadout caster(int tier) {
    var base =
        tier >= 2
            ? pack(
                ExtraSkills.MANA_MANIPULATION.get(),
                ExtraSkills.FLAME_MANIPULATION.get(),
                ExtraSkills.WATER_MANIPULATION.get(),
                ExtraSkills.WIND_MANIPULATION.get(),
                ExtraSkills.LIGHTNING_MANIPULATION.get(),
                ExtraSkills.EARTH_MANIPULATION.get(),
                ResistanceSkills.FLAME_ATTACK_NULLIFICATION.get(),
                ResistanceSkills.ELECTRICITY_NULLIFICATION.get(),
                ResistanceSkills.DARKNESS_ATTACK_RESISTANCE.get())
            : pack(
                ExtraSkills.MANA_MANIPULATION.get(),
                ExtraSkills.FLAME_MANIPULATION.get(),
                ExtraSkills.WATER_MANIPULATION.get(),
                ExtraSkills.WIND_MANIPULATION.get(),
                ExtraSkills.EARTH_MANIPULATION.get(),
                ResistanceSkills.FLAME_ATTACK_NULLIFICATION.get(),
                ResistanceSkills.WATER_ATTACK_RESISTANCE.get());
    if (tier == 1) {
      var skills = new java.util.ArrayList<ManasSkill>(base.skills());
      skills.add(ExtraSkills.LIGHTNING_MANIPULATION.get());
      base = new Loadout(List.copyOf(skills), List.of());
    }
    var spells =
        switch (tier) {
          case 2 ->
              List.<Magic>of(
                  AspectualMagics.FIRE_BALL.get(),
                  AspectualMagics.ACID_SHELL.get(),
                  AspectualMagics.ICE_BREAKER.get(),
                  AspectualMagics.TORNADO_BLADE.get(),
                  AspectualMagics.WATER_JAIL.get(),
                  AspectualMagics.MUD_SPEARS.get(),
                  AspectualMagics.STONE_SHOT.get(),
                  AspectualMagics.THUNDER_ORB.get());
          case 1 ->
              List.<Magic>of(
                  AspectualMagics.FIRE_LANCE.get(),
                  AspectualMagics.ACID_SHELL.get(),
                  AspectualMagics.ICICLE_LANCE.get(),
                  AspectualMagics.TORNADO_BLADE.get(),
                  AspectualMagics.THUNDER_ORB.get(),
                  AspectualMagics.STONE_SHOT.get(),
                  AspectualMagics.SLEEP_MIST.get());
          default ->
              List.<Magic>of(
                  AspectualMagics.FIRE.get(),
                  AspectualMagics.WATER_CUTTER.get(),
                  AspectualMagics.WIND_CUTTER.get(),
                  AspectualMagics.STONE_SHOT.get(),
                  AspectualMagics.FIRE_BALL.get());
        };
    return new Loadout(base.skills(), spells);
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
              ResistanceSkills.PAIN_NULLIFICATION.get(),
              CommonSkills.STRENGTH.get(),
              ResistanceSkills.FLAME_ATTACK_RESISTANCE.get(),
              ResistanceSkills.PARALYSIS_RESISTANCE.get());
      case "armorsaurus" ->
          pack(
              UniqueSkills.SURVIVOR.get(),
              IntrinsicSkills.BODY_ARMOR.get(),
              IntrinsicSkills.SCALE_ARMOR.get(),
              ResistanceSkills.PIERCE_RESISTANCE.get(),
              ResistanceSkills.PHYSICAL_ATTACK_NULLIFICATION.get(),
              CommonSkills.STRENGTH.get(),
              ResistanceSkills.GRAVITY_ATTACK_RESISTANCE.get());
      case "horned_bear" ->
          pack(
              UniqueSkills.FIGHTER.get(),
              CommonSkills.STRENGTH.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              ResistanceSkills.PAIN_NULLIFICATION.get(),
              ResistanceSkills.COLD_RESISTANCE.get(),
              ResistanceSkills.PARALYSIS_RESISTANCE.get());
      case "direwolf" ->
          pack(
              ExtraSkills.GODWOLF_SENSE.get(),
              ExtraSkills.BLACK_LIGHTNING.get(),
              ExtraSkills.ULTRA_INSTINCT.get(),
              ExtraSkills.SHADOW_MOTION.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              ResistanceSkills.ELECTRICITY_NULLIFICATION.get(),
              ResistanceSkills.PARALYSIS_RESISTANCE.get(),
              CommonSkills.STRENGTH.get(),
              ResistanceSkills.WIND_ATTACK_RESISTANCE.get(),
              ResistanceSkills.POISON_RESISTANCE.get());
      case "blade_tiger" ->
          pack(
              UniqueSkills.SEER.get(),
              CommonSkills.STRENGTH.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              ResistanceSkills.WIND_ATTACK_NULLIFICATION.get(),
              ResistanceSkills.PIERCE_RESISTANCE.get(),
              ResistanceSkills.PARALYSIS_RESISTANCE.get());
      case "knight_spider" ->
          pack(
              IntrinsicSkills.BODY_ARMOR.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              CommonSkills.POISON.get(),
              ResistanceSkills.POISON_NULLIFICATION.get(),
              ResistanceSkills.PARALYSIS_RESISTANCE.get(),
              CommonSkills.STRENGTH.get(),
              ResistanceSkills.PIERCE_RESISTANCE.get());
      case "lizardman" ->
          pack(
              IntrinsicSkills.SCALE_ARMOR.get(),
              IntrinsicSkills.WATER_BREATHING.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              ResistanceSkills.WATER_ATTACK_NULLIFICATION.get(),
              CommonSkills.STRENGTH.get(),
              ResistanceSkills.CORROSION_RESISTANCE.get());
      case "basilisk" ->
          pack(
              IntrinsicSkills.SCALE_ARMOR.get(),
              CommonSkills.POISON.get(),
              CommonSkills.PARALYSIS.get(),
              ResistanceSkills.POISON_NULLIFICATION.get(),
              ResistanceSkills.PARALYSIS_NULLIFICATION.get(),
              ExtraSkills.STEEL_STRENGTH.get(),
              CommonSkills.STRENGTH.get());
      case "lesser_daemon" -> caster(0);
      case "greater_daemon" -> caster(1);
      case "arch_daemon" -> caster(2);
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
    double oldBaseEP = EnergyHelper.getBaseMaxEP(mob);
    HighRealmEvolutions.apply(mob);
    if (EnergyHelper.getBaseMaxEP(mob) < oldBaseEP) EnergyHelper.setBaseMaxEP(mob, oldBaseEP);
    // Retire Fighter where the previous addon loadout granted it, and superseded resistances.
    if (tag.getInt(VERSION_KEY) > 0 && !loadout.skills().contains(UniqueSkills.FIGHTER.get()))
      remove(mob, UniqueSkills.FIGHTER.get());
    for (var skill : loadout.skills()) grant(mob, skill, true);
    for (var spell : loadout.spells()) grant(mob, spell, false);
    // Native nullifications may keep lower-tier entries; do not dismantle that hierarchy.
    if (loadout.skills().contains(ExtraSkills.STEEL_STRENGTH.get()))
      remove(mob, CommonSkills.STRENGTH.get());
    mob.setHealth(Math.min(hp, mob.getMaxHealth()));
    pool.setAura(Math.min(ap, EnergyHelper.getMaxAura(mob)));
    pool.setMagicule(Math.min(mp, EnergyHelper.getMaxMagicule(mob)));
    pool.setSpiritualHealth(shp);
    pool.markDirty();
    HighRealmEvolutions.refreshName(mob);
    tag.putInt(VERSION_KEY, VERSION);
    return true;
  }

  private static void remove(Mob mob, ManasSkill skill) {
    var storage = SkillAPI.getSkillsFrom(mob);
    storage.getLearnedSkills().stream()
        .filter(i -> i.getSkill() == skill)
        .findFirst()
        .ifPresent(
            i -> {
              if (i.isToggled()) {
                i.setToggled(false);
                i.onToggleOff(mob);
              }
              var presets = TensuraStorages.getAbilityFrom(mob);
              for (int preset = 0; preset < 9; preset++)
                for (int slot = 0; slot < 3; slot++)
                  if (presets.getAbilitySlot(preset, slot).getSkill() == skill)
                    presets.setAbilitySlot(preset, slot, null, 0);
              i.markDirty();
              storage.forgetSkill(i);
            });
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
