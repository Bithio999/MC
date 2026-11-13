package dev.heavenlyrestriction.laplace.expand;

import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;

/**
 * Explicit bridges for Additions' authored effects; inventory/world operations are not equations.
 */
public final class AddonParameters {
  public record Spec(String name, String label, double base, boolean whole) {
    public String key(String id, int mode) {
      return ParameterKey.key("addon:" + id + "#" + mode + "." + name);
    }
  }

  public static List<Spec> specs(ManasSkillInstance i, int mode, LivingEntity owner) {
    if (!i.getSkillId().getNamespace().equals("heavenlyrestriction")) return List.of();
    boolean master = i.isMastered(owner);
    String id = i.getSkillId().getPath();
    if (mode == 0 && (id.equals("chained") || id.equals("unbound")))
      return List.of(
          new Spec("rush_damage", "Rush damage multiplier", 1, false),
          new Spec("rush_reach", "Rush reach", 16, false));
    if (mode == 1 && id.equals("unbound"))
      return List.of(new Spec("counter_damage", "Counter damage multiplier", 1, false));
    if (mode < 0 && (id.equals("critical_merchant") || id.equals("tyche")))
      return List.of(new Spec("critical_payout", "Critical payout multiplier", 1, false));
    if (mode < 0 && (id.equals("prospector") || id.equals("hephaestus")))
      return List.of(
          new Spec("mining_bonus", "Mining speed bonus", id.equals("hephaestus") ? 1 : .5, false),
          new Spec(
              "fortune_bonus", "Additional Fortune levels", id.equals("hephaestus") ? 5 : 3, true));
    if (mode < 0 && id.equals("schema"))
      return List.of(
          new Spec("aura_regeneration", "Mitochondrial regeneration bonus", 5, false),
          new Spec("strength", "Strength attack bonus", 5, false),
          new Spec("healing", "Regenerative healing bonus", .2, false));
    if (mode == 1 && id.equals("hephaestus"))
      return List.of(new Spec("arsenal_damage", "Living Arsenal damage multiplier", 1, false));
    if (mode == 0 && id.equals("fusion"))
      return List.of(
          new Spec("duration", "Fusion duration (seconds)", master ? 270 : 180, true),
          new Spec("regeneration", "Fusion regeneration multiplier", 10, false),
          new Spec("damage", "Fusion damage multiplier", 2, false));
    if (mode < 0 && (id.equals("split_mind") || id.equals("metis")))
      return List.of(new Spec("scan_radius", "Auto-cast targeting radius", 32, false));
    if (mode < 0 && Set.of("hunters_recovery", "apex_renewal", "sovereign_rebirth").contains(id))
      return List.of(new Spec("recovery", "Healing output multiplier", 1, false));
    return List.of();
  }

  private record Cached(int tick, int revision, Map<String, Map<String, Double>> actions) {}

  private static final Map<LivingEntity, Cached> CACHE = new WeakHashMap<>();

  public static double value(
      LivingEntity owner, String id, int mode, String name, double original) {
    if (owner == null || owner.level().isClientSide()) return original;
    String skillId = id.contains(":") ? id : "heavenlyrestriction:" + id;
    String key = ParameterKey.key("addon:" + skillId + "#" + mode + "." + name);
    double scoped = ParameterRuntime.decimal(Double.NaN, key);
    if (!Double.isNaN(scoped)) return scoped;
    var own = Laplace.active(owner);
    String actionKey = skillId + "#" + mode;
    if (own.isEmpty() || !Laplace.effectiveEnabled(owner, own.get(), actionKey)) return original;
    int revision = own.get().getOrCreateTag().getInt("EquationRevision");
    var cached = CACHE.get(owner);
    if (cached == null || cached.tick() != owner.tickCount || cached.revision() != revision) {
      cached = new Cached(owner.tickCount, revision, new HashMap<>());
      CACHE.put(owner, cached);
    }
    var values = cached.actions().get(actionKey);
    if (values == null) {
      var action = LaplaceActions.byKey(owner, actionKey);
      if (action == null) return original;
      try {
        values = action.model().evaluate(Laplace.equations(own.get(), action)).values();
        cached.actions().put(actionKey, values);
      } catch (IllegalArgumentException ex) {
        return original;
      }
    }
    return values.getOrDefault(key, original);
  }

  private AddonParameters() {}
}
