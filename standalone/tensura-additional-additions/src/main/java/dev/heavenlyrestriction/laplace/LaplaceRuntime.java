package dev.heavenlyrestriction.laplace;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.entity.projectile.TensuraFlyingProjectile;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * A cast freezes its inputs before payment. In-flight shots retain the values they were paid for.
 */
public final class LaplaceRuntime {
  public record Frame(
      LivingEntity owner,
      LaplaceActions.Action action,
      Map<String, String> equations,
      EquationModel.Result result) {}

  private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

  public static final class Scope implements AutoCloseable {
    private final Frame before;

    Scope(Frame f) {
      before = CURRENT.get();
      CURRENT.set(f);
    }

    public void close() {
      if (before == null) CURRENT.remove();
      else CURRENT.set(before);
    }
  }

  public static Scope cast(LivingEntity e, ManasSkillInstance i, int mode) {
    Frame f = null;
    var own = Laplace.active(e);
    var a = own.isEmpty() ? null : LaplaceActions.find(e, i, mode);
    if (a != null && Laplace.effectiveEnabled(e, own.get(), a.key()))
      try {
        var eq = Laplace.equations(own.get(), a);
        f = new Frame(e, a, eq, a.model().evaluate(eq));
      } catch (IllegalArgumentException ignored) {
      }
    return new Scope(f);
  }

  public static Frame current() {
    return CURRENT.get();
  }

  public static double cost(double nativeCost, LivingEntity e, ManasSkillInstance i, int mode) {
    var f = CURRENT.get();
    if (f != null && f.owner == e && f.action.skill() == i && f.action.mode() == mode)
      return f.result.values().get("magicule_cost");
    return Laplace.result(e, i, mode).map(v -> v.values().get("magicule_cost")).orElse(nativeCost);
  }

  public static void projectile(EntityJoinLevelEvent event) {
    var f = CURRENT.get();
    if (f == null
        || event.getLevel().isClientSide()
        || !(event.getEntity() instanceof TensuraFlyingProjectile p)
        || p.getOwner() != f.owner) return;
    var v = f.result.values();
    if (v.containsKey("elemental_damage"))
      p.setDamage((float) Math.min(1e30, v.get("elemental_damage")));
    if (v.containsKey("magic_damage"))
      p.setSecondaryDamage((float) Math.min(1e30, v.get("magic_damage")));
  }

  public static int duration(int original) {
    var f = CURRENT.get();
    return f != null && f.result.values().containsKey("duration")
        ? (int)
            Math.min(
                Integer.MAX_VALUE - 1,
                Math.max(1, Math.round(f.result.values().get("duration") * 20)))
        : original;
  }

  public static EquationModel withBase(EquationModel m, String key, double value) {
    var inputs = new LinkedHashMap<>(m.inputs());
    var bounds = new LinkedHashMap<>(m.bounds());
    inputs.put(key, value);
    bounds.put(key, FormulaSet.Range.fixed(value));
    return new EquationModel(
        inputs,
        bounds,
        m.defaults(),
        m.labels(),
        m.units(),
        m.powerOutputs(),
        m.costOutputs(),
        m.powerLimit());
  }

  public static float regeneration(LivingEntity e, String output, float original) {
    var own = Laplace.active(e);
    if (own.isEmpty()) return original;
    var i =
        SkillAPI.getSkillsFrom(e)
            .getSkill(
                io.github.manasmods.tensura.registry.skill.CommonSkills.SELF_REGENERATION.get())
            .orElse(null);
    var a = i == null ? null : LaplaceActions.find(e, i, 0);
    if (a == null
        || !i.isToggled()
        || !a.model().defaults().containsKey(output)
        || !Laplace.effectiveEnabled(e, own.get(), a.key())) return original;
    try {
      var m = withBase(a.model(), "base_" + output, original);
      return (float)
          Math.min(1e30, m.evaluate(Laplace.equations(own.get(), a)).values().get(output));
    } catch (IllegalArgumentException ex) {
      return original;
    }
  }

  private static final ThreadLocal<double[]> HEALING = new ThreadLocal<>();

  public static void clearHealing() {
    HEALING.remove();
  }

  public static float healingLimit(float missing, float nativeLimit) {
    var f = CURRENT.get();
    if (f == null
        || f.action.skill().getSkill()
            != io.github.manasmods.tensura.registry.magic.AspectualMagics.HEALING.get())
      return Math.min(missing, nativeLimit);
    try {
      var m = withBase(f.action.model(), "base_healing", nativeLimit);
      double desired = m.evaluate(f.equations).values().get("healing");
      double actual = Math.min(missing, desired);
      var result =
          m.evaluate(
              f.equations,
              Map.of("healing", actual),
              Map.of("healing", (double) Math.min(missing, nativeLimit)));
      double cost = result.values().get("magicule_cost");
      if (cost > Integer.MAX_VALUE - 1d)
        throw new IllegalArgumentException("Healing bill exceeds the native payment limit.");
      HEALING.set(new double[] {cost, actual, 1});
      return (float) actual;
    } catch (IllegalArgumentException ex) {
      HEALING.remove();
      return Math.min(missing, nativeLimit);
    }
  }

  public static double healingShortfall(double nativeShortfall, double bill) {
    var v = HEALING.get();
    if (v != null)
      v[2] =
          bill > 0
              ? Math.max(0, Math.min(1, 1 - nativeShortfall / bill))
              : nativeShortfall <= 0 ? 1 : 0;
    return nativeShortfall;
  }

  public static float paidHealing(float nativeHeal) {
    var v = HEALING.get();
    return v == null ? nativeHeal : (float) (v[1] * v[2]);
  }

  public static double healingCost(double a, double b) {
    var v = HEALING.get();
    return v == null ? Math.max(a, b) : Math.min(Integer.MAX_VALUE - 1d, v[0]);
  }

  private LaplaceRuntime() {}
}
