package dev.heavenlyrestriction.laplace.expand;

import dev.heavenlyrestriction.exponentiation.Exponentiation;
import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;

/** Per-mode native parameter boards, including passive callbacks. */
public final class ExpandedActions {
  private static final Set<String> PROTECTED = Set.of("heavenlyrestriction:laplace");

  public static boolean namespace(ManasSkillInstance i) {
    return Set.of("tensura", "heavenlyrestriction").contains(i.getSkillId().getNamespace())
        && !PROTECTED.contains(i.getSkillId().toString())
        && i.getSkill() instanceof TensuraSkill;
  }

  public static boolean overrides(ManasSkillInstance i, String name) {
    for (var m : i.getSkill().getClass().getMethods())
      if (m.getName().equals(name) && m.getDeclaringClass() != ManasSkill.class) return true;
    return false;
  }

  public static boolean attributeEffects(ManasSkillInstance i) {
    return i.getSkill() instanceof io.github.manasmods.tensura.ability.skill.Skill
        && !((AttributeParameters) i.getSkill()).additions$attributeTemplates().isEmpty();
  }

  public static boolean passive(ManasSkillInstance i) {
    if (attributeEffects(i)) return true;
    var base =
        Set.of(
            ManasSkill.class.getName(),
            "io.github.manasmods.tensura.ability.TensuraSkill",
            "io.github.manasmods.tensura.ability.skill.Skill",
            "io.github.manasmods.tensura.ability.magic.Magic",
            "io.github.manasmods.tensura.ability.magic.aspectual.AspectualMagic",
            "io.github.manasmods.tensura.ability.magic.spiritual.SpiritualMagic",
            "io.github.manasmods.tensura.ability.battlewill.Battlewill");
    for (var method : i.getSkill().getClass().getMethods())
      if (List.of(
                  "onTick",
                  "onToggleOn",
                  "onBeingDamaged",
                  "onDamageEntity",
                  "onTakenDamage",
                  "onEffectAdded",
                  "onDeath")
              .contains(method.getName())
          && !base.contains(method.getDeclaringClass().getName())) return true;
    return Set.of(
                "prospector",
                "hephaestus",
                "critical_merchant",
                "tyche",
                "split_mind",
                "metis",
                "schema",
                "hunters_recovery",
                "apex_renewal",
                "sovereign_rebirth")
            .contains(i.getSkillId().getPath())
        && i.getSkillId().getNamespace().equals("heavenlyrestriction");
  }

  public static boolean active(ManasSkillInstance i) {
    return List.of("onPressed", "onHeld", "onRelease").stream().anyMatch(n -> overrides(i, n));
  }

  public static Set<Integer> modes(LivingEntity e, ManasSkillInstance i) {
    if (!namespace(i) || i.getMastery() < 0 || i.getRemoveTime() == -3) return Set.of();
    var result = new TreeSet<Integer>();
    var skill = (TensuraSkill) i.getSkill();
    if (passive(i)) result.add(-1);
    if (active(i)) {
      int count = Math.clamp(i.getModes(), 0, 128);
      var seen = new HashSet<Integer>();
      var todo = new ArrayDeque<Integer>();
      todo.add(0);
      while (!todo.isEmpty() && seen.size() <= count + 1) {
        int mode = todo.removeFirst();
        if (!seen.add(mode) || mode < 0 || mode >= count) continue;
        var probe = i.copy();
        boolean learned =
            !skill.getModeLearningList(probe).contains(mode)
                || probe.getOrCreateTag().getDouble(skill.getModeId(probe, mode))
                    >= skill.getLearningPointRequirement(probe, e, mode);
        if (skill.canBeSlotted(probe, e, mode) && learned) result.add(mode);
        for (boolean reverse : new boolean[] {false, true}) {
          int next = skill.nextMode(e, probe, mode, reverse);
          if (next >= 0 && next < count && !seen.contains(next)) todo.add(next);
        }
      }
    }
    if (result.isEmpty()) result.add(-1);
    return result;
  }

  public static LaplaceActions.Action find(LivingEntity owner, ManasSkillInstance i, int mode) {
    if (!namespace(i) || !modes(owner, i).contains(mode)) return null;
    var params = ParameterInspection.fields(i.getSkill().getClass(), mode, i.isMastered(owner));
    var inputs = new LinkedHashMap<String, Double>();
    var bounds = new LinkedHashMap<String, FormulaSet.Range>();
    var defaults = new LinkedHashMap<String, String>();
    var labels = new LinkedHashMap<String, String>();
    var units = new LinkedHashMap<String, String>();
    var power = new ArrayList<String>();
    for (var p : params) {
      double value = NativeParameters.read(p);
      if (!Double.isFinite(value) || value <= 0 || value > 1e30) continue;
      if (defaults.size() >= 120) break;
      String key = p.key();
      inputs.put("base_" + key, value);
      bounds.put("base_" + key, FormulaSet.Range.fixed(value));
      defaults.put(key, "base_" + key);
      labels.put(key, capitalize(ParameterKey.label(p.name())));
      units.put(key, Set.of("I", "J").contains(p.descriptor()) ? "whole units" : "units");
      power.add(key);
    }
    var authored = new ArrayList<>(AddonParameters.specs(i, mode, owner));
    if (mode < 0 && attributeEffects(i))
      authored.add(
          new AddonParameters.Spec(
              "attribute_power", "Native attribute bonus multiplier", 1, false));
    for (var spec : authored) {
      String key = spec.key(i.getSkillId().toString(), mode);
      inputs.put("base_" + key, spec.base());
      bounds.put("base_" + key, FormulaSet.Range.fixed(spec.base()));
      defaults.put(key, "base_" + key);
      labels.put(key, spec.label());
      units.put(key, spec.whole() ? "whole units" : "units");
      power.add(key);
    }
    double mp, ap;
    try (var raw = new ParameterRuntime.Scope(null)) {
      var skill = (TensuraSkill) i.getSkill();
      mp = skill.getMagiculeCost(owner, i, Math.max(0, mode));
      ap = skill.getAuraCost(owner, i, Math.max(0, mode));
    }
    double declaredMP = mp, declaredAP = ap;
    boolean payment =
        ParameterInspection.paying(i.getSkill().getClass(), mode, i.isMastered(owner));
    if (!payment) {
      mp = 0;
      ap = 0;
    }
    if (!Double.isFinite(mp) || !Double.isFinite(ap) || mp < 0 || ap < 0) return null;
    var rates = LaplaceConfig.rates();
    double budget = mp + ap * rates.magiculesPerAura();
    if (budget > 1e30) return null;
    inputs.put("base_cost", budget);
    bounds.put("base_cost", FormulaSet.Range.fixed(budget));
    inputs.put("__parameter_board", 1d);
    bounds.put("__parameter_board", FormulaSet.Range.fixed(1));
    boolean deferred =
        i.getSkill()
                instanceof io.github.manasmods.tensura.ability.subclass.ISpatialMovement spatial
            && mode == spatial.getSpatialMovementModeIndex();
    if (deferred) {
      inputs.put("__deferred_cost", 1d);
      bounds.put("__deferred_cost", FormulaSet.Range.fixed(1));
    }
    defaults.put("magicule_cost", "base_cost");
    labels.put("magicule_cost", "Cost budget");
    units.put("magicule_cost", "MP eq");
    var energy = TensuraStorages.getExistenceFrom(owner);
    double max = Exponentiation.capacity(owner, owner::getMaxHealth);
    putFraction(inputs, bounds, "missing_hp_fraction", max > 0 ? 1 - owner.getHealth() / max : 0);
    putFraction(inputs, bounds, "hp_fraction", max > 0 ? owner.getHealth() / max : 0);
    putFraction(
        inputs,
        bounds,
        "aura_fraction",
        energy.getAura()
            / Math.max(1, io.github.manasmods.tensura.util.EnergyHelper.getMaxAura(owner)));
    putFraction(
        inputs,
        bounds,
        "magicule_fraction",
        energy.getMagicule()
            / Math.max(1, io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(owner)));
    String key = i.getSkillId() + "#" + mode;
    PaymentPlan nativePlan =
        (declaredMP + declaredAP * rates.magiculesPerAura()) > 0
            ? new PaymentPlan(
                declaredMP / (declaredMP + declaredAP * rates.magiculesPerAura()),
                declaredAP
                    * rates.magiculesPerAura()
                    / (declaredMP + declaredAP * rates.magiculesPerAura()),
                0,
                0)
            : Set.of(
                        "hunters_recovery",
                        "apex_renewal",
                        "sovereign_rebirth",
                        "instant_transmission",
                        "air_sovereignty",
                        "chained",
                        "unbound")
                    .contains(i.getSkillId().getPath())
                ? PaymentPlan.AURA
                : PaymentPlan.MAGICULES;
    var plan = Laplace.payment(owner, key, nativePlan);
    boolean restorative =
        ParameterInspection.restorative(i.getSkill().getClass(), mode, i.isMastered(owner))
            || i.getSkillId().getPath().matches(".*(recover|regen|rebirth|healing).*")
            || i.getSkill() instanceof dev.heavenlyrestriction.AuraRegenerationSkill
            || params.stream()
                .anyMatch(
                    p ->
                        p.name()
                            .toLowerCase(Locale.ROOT)
                            .matches(".*(heal|regen|recover|restore).*"));
    var policy = new PaymentPlan.Policy(plan, rates, !restorative, false);
    var model =
        new EquationModel(
            inputs,
            bounds,
            defaults,
            labels,
            units,
            power,
            List.of("magicule_cost"),
            Laplace.owned(owner).filter(l -> l.isMastered(owner)).isPresent() ? 4 : 3,
            policy);
    String title =
        i.getSkill().getName().getString()
            + " · "
            + (mode < 0
                ? "Passive"
                : ((TensuraSkill) i.getSkill()).getModeName(i, mode).getString());
    String detail =
        deferred
            ? "Coordinate selection is free. The selected payment and cost equation apply to the"
                  + " actual destination bill when confirmed."
            : power.isEmpty()
                ? (budget == 0
                    ? "Fixed native mechanic: there are no numerical effects or resource charges to"
                        + " edit. Menus, world operations and progression remain native."
                    : "This action has fixed native effects. Its existing payment can be edited;"
                        + " inventory and progression rules stay native.")
                : "Live native parameters for this mode. Values are native units; whole-number"
                    + " parameters round to the nearest whole unit. Combined increases share the"
                    + " output limit. Progression and inventory rules stay native.";
    return new LaplaceActions.Action(key, title, detail, i, mode, model);
  }

  private static String capitalize(String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private static void putFraction(
      Map<String, Double> v, Map<String, FormulaSet.Range> b, String key, double n) {
    v.put(key, Math.max(0, Math.min(1, n)));
    b.put(key, new FormulaSet.Range(0, 1));
  }

  private ExpandedActions() {}
}
