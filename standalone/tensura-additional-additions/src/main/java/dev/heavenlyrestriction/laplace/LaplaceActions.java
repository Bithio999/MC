package dev.heavenlyrestriction.laplace;

import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.common.SelfRegenerationSkill;
import io.github.manasmods.tensura.config.ability.magic.AspectualMagicConfig;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.magic.AspectualMagics;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;

/** Verified adapters expose actual native constants, including separate recovery/upkeep clocks. */
public final class LaplaceActions {
  public record Action(
      String key,
      String title,
      String detail,
      ManasSkillInstance skill,
      int mode,
      EquationModel model) {}

  public static List<Action> learned(LivingEntity owner) {
    var result = new ArrayList<Action>();
    for (var i : SkillAPI.getSkillsFrom(owner).getLearnedSkills()) {
      if (i.getMastery() < 0 || i.getRemoveTime() == -3) continue;
      for (int mode = 0;
          mode
              < (i.getSkill() == AspectualMagics.WATER_CUTTER.get() && i.isMastered(owner) ? 2 : 1);
          mode++) {
        var action = find(owner, i, mode);
        if (action != null) result.add(action);
      }
    }
    result.sort(Comparator.comparing(Action::title, String.CASE_INSENSITIVE_ORDER));
    return result;
  }

  public static Action byKey(LivingEntity owner, String key) {
    if (key == null || key.length() > 180) return null;
    return learned(owner).stream().filter(a -> a.key.equals(key)).findFirst().orElse(null);
  }

  private static double field(Object cfg, String name) {
    try {
      return ((Number) cfg.getClass().getField(name).get(cfg)).doubleValue();
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Native equation constant unavailable: " + name, ex);
    }
  }

  public static Action find(LivingEntity owner, ManasSkillInstance i, int mode) {
    if (i == null || i.getMastery() < 0) return null;
    String id = nativeId(i);
    boolean master = i.isMastered(owner);
    if (mode < 0 || mode > 0 && !(id.equals("tensura:water_cutter") && mode == 1 && master))
      return null;
    var magic = ConfigRegistry.getConfig(AspectualMagicConfig.class);
    Object cfg;
    String detail;
    var b = new Builder(owner);
    switch (id) {
      case "tensura:self_regeneration" -> {
        cfg = SelfRegenerationSkill.CONFIG;
        double level = field(cfg, master ? "regenLevelMastered" : "regenLevel");
        b.amount("healing", "HP per pulse", "HP", field(cfg, "regenHP") * level, "base_healing");
        if (level > 1)
          b.amount(
              "shp_healing",
              "SHP per pulse",
              "SHP",
              field(cfg, "regenSHP") * level,
              "base_shp_healing");
        b.cost(field(cfg, "magiculeCost"), "base_cost");
        b.input("pulse_seconds", 1);
        b.input("upkeep_seconds", 5);
        detail =
            "Healing pulses every 1 second; upkeep is paid on the native skill tick (normally every"
                + " 5 seconds). Native suppression and recovery limits apply.";
      }
      case "tensura:fire" -> {
        cfg = magic.Fire;
        b.damage(cfg, "fireDamage", "Fire");
        b.cost(field(cfg, "magiculeCost"), "base_cost");
        detail =
            "Damage per native fire bolt; mastery keeps its native projectile count and casting"
                + " time. Payment follows the chosen resource mix.";
      }
      case "tensura:wind_cutter" -> {
        cfg = magic.WindCutter;
        b.damage(cfg, "windDamage", "Wind");
        b.cost(field(cfg, "magiculeCost"), "base_cost");
        detail =
            "Separate Wind and Magic damage per blade; native mastery and projectile count remain.";
      }
      case "tensura:water_cutter" -> {
        cfg = magic.WaterCutter;
        // Water blades provide their own native primary damage; only the spell's explicit Magic
        // component is edited.
        b.amount(
            "magic_damage",
            "Magic damage",
            "damage",
            field(cfg, "magicDamage"),
            "base_magic_damage");
        b.cost(field(cfg, mode == 1 ? "magiculeCostRepeat" : "magiculeCost"), "base_cost");
        detail =
            mode == 1
                ? "Magic damage and cost per repeated water cut. Native Water damage and repeat"
                    + " timing remain."
                : "Magic damage per water cut. The projectile's native Water damage remains"
                    + " unchanged.";
      }
      case "tensura:healing" -> {
        cfg = magic.Healing;
        double maximum = Exponentiation.capacity(owner, owner::getMaxHealth);
        double heal =
            master
                ? Math.max(field(cfg, "hpHealMastered"), field(cfg, "hpHealPercentage") * maximum)
                : field(cfg, "hpHeal");
        b.amount("healing", "Healing limit", "HP", heal, "base_healing");
        b.input("cost_per_hp", field(cfg, "magiculeCost"));
        b.cost(field(cfg, "minCost"), "max(base_cost, cost_per_hp * healing)");
        detail =
            "Healing is capped to the target's missing HP. Cost follows actual healing. Mastered"
                + " percentage healing uses the actual target's native HP pool.";
      }
      case "tensura:strength" -> {
        cfg = magic.Strength;
        b.duration(cfg, master, "strengthDuration");
        b.cost(field(cfg, "magiculeCost"), "base_cost");
        detail =
            "Strength duration in seconds. Native effect level, targeting and cast time remain.";
      }
      case "tensura:protection" -> {
        cfg = magic.Protection;
        b.duration(cfg, master, "protectionDuration");
        b.cost(field(cfg, "magiculeCost"), "base_cost");
        detail = "Protection duration in seconds. Native effect strength and defenses remain.";
      }
      case "tensura:reinforcement" -> {
        cfg = magic.Reinforcement;
        b.duration(cfg, master, "reinforcementDuration");
        b.cost(field(cfg, "magiculeCost"), "base_cost");
        detail = "Reinforcement duration in seconds. Native effect strength and targeting remain.";
      }
      default -> {
        return null;
      }
    }
    String title = i.getSkill().getName().getString() + (mode == 1 ? " · Repeat" : "");
    String key = i.getSkillId() + "#" + mode;
    var raw = b.model();
    boolean recovery = id.equals("tensura:self_regeneration") || id.equals("tensura:healing");
    var policy =
        new PaymentPlan.Policy(
            Laplace.payment(owner, key),
            LaplaceConfig.rates(),
            !recovery,
            id.equals("tensura:healing"));
    var model =
        new EquationModel(
            raw.inputs(),
            raw.bounds(),
            raw.defaults(),
            raw.labels(),
            raw.units(),
            raw.powerOutputs(),
            raw.costOutputs(),
            raw.powerLimit(),
            policy);
    return new Action(key, title, detail, i, mode, model);
  }

  private static String nativeId(ManasSkillInstance i) {
    if (i.getSkill() == AspectualMagics.FIRE.get()) return "tensura:fire";
    if (i.getSkill() == AspectualMagics.WIND_CUTTER.get()) return "tensura:wind_cutter";
    if (i.getSkill() == AspectualMagics.WATER_CUTTER.get()) return "tensura:water_cutter";
    if (i.getSkill() == AspectualMagics.HEALING.get()) return "tensura:healing";
    if (i.getSkill() == AspectualMagics.STRENGTH.get()) return "tensura:strength";
    if (i.getSkill() == AspectualMagics.PROTECTION.get()) return "tensura:protection";
    if (i.getSkill() == AspectualMagics.REINFORCEMENT.get()) return "tensura:reinforcement";
    return i.getSkillId().toString();
  }

  private static class Builder {
    final Map<String, Double> inputs = new LinkedHashMap<>();
    final Map<String, FormulaSet.Range> bounds = new LinkedHashMap<>();
    final LinkedHashMap<String, String> defaults = new LinkedHashMap<>();
    final Map<String, String> labels = new LinkedHashMap<>(), units = new LinkedHashMap<>();
    final List<String> power = new ArrayList<>(), costs = new ArrayList<>();
    final LivingEntity owner;

    Builder(LivingEntity owner) {
      this.owner = owner;
      double max = Exponentiation.capacity(owner, owner::getMaxHealth);
      var energy = TensuraStorages.getExistenceFrom(owner);
      input("missing_hp", Math.max(0, max - owner.getHealth()));
      bounds.put("missing_hp", new FormulaSet.Range(0, Math.max(0, max)));
      fraction("missing_hp_fraction", max > 0 ? Math.max(0, 1 - owner.getHealth() / max) : 0);
      fraction("hp_fraction", max > 0 ? owner.getHealth() / max : 0);
      fraction(
          "aura_fraction",
          owner.getAttributeValue(TensuraAttributes.MAX_AURA) > 0
              ? energy.getAura() / owner.getAttributeValue(TensuraAttributes.MAX_AURA)
              : 0);
      fraction(
          "magicule_fraction",
          owner.getAttributeValue(TensuraAttributes.MAX_MAGICULE) > 0
              ? energy.getMagicule() / owner.getAttributeValue(TensuraAttributes.MAX_MAGICULE)
              : 0);
    }

    void input(String key, double n) {
      inputs.put(key, n);
      bounds.put(key, FormulaSet.Range.fixed(n));
    }

    void fraction(String key, double n) {
      inputs.put(key, Math.max(0, Math.min(n, 1)));
      bounds.put(key, new FormulaSet.Range(0, 1));
    }

    void amount(String key, String label, String unit, double base, String formula) {
      String input = "base_" + key;
      input(input, base);
      defaults.put(key, formula);
      labels.put(key, label);
      units.put(key, unit);
      power.add(key);
    }

    void cost(double base, String formula) {
      input("base_cost", base);
      defaults.put("magicule_cost", formula);
      labels.put("magicule_cost", "Cost budget");
      units.put("magicule_cost", "MP eq");
      costs.add("magicule_cost");
    }

    void damage(Object cfg, String elemental, String label) {
      amount(
          "elemental_damage",
          label + " damage",
          "damage",
          field(cfg, elemental),
          "base_elemental_damage");
      amount(
          "magic_damage", "Magic damage", "damage", field(cfg, "magicDamage"), "base_magic_damage");
    }

    void duration(Object cfg, boolean master, String name) {
      amount(
          "duration",
          "Effect duration",
          "seconds",
          field(cfg, name + (master ? "Mastered" : "")) / 20,
          "base_duration");
    }

    EquationModel model() {
      return new EquationModel(
          Map.copyOf(inputs),
          Map.copyOf(bounds),
          defaults,
          Map.copyOf(labels),
          Map.copyOf(units),
          List.copyOf(power),
          List.copyOf(costs),
          Laplace.active(owner).map(i -> i.isMastered(owner) ? 4d : 3d).orElse(3d));
    }
  }

  private LaplaceActions() {}
}
