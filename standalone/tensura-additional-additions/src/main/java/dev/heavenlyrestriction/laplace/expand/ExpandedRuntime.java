package dev.heavenlyrestriction.laplace.expand;

import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;

/** Freeze per-player native parameters around the actual ManasCore callback. */
public final class ExpandedRuntime {
  public record Frame(
      LivingEntity owner,
      ManasSkillInstance skill,
      int mode,
      LaplaceActions.Action action,
      Map<String, String> equations,
      EquationModel.Result result) {}

  private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

  public static final class Scope implements AutoCloseable {
    private final Frame before;
    private final ParameterRuntime.Scope parameters;

    private Scope() {
      before = null;
      parameters = null;
    }

    private Scope(Frame frame) {
      this(frame, frame == null ? null : frame.result.values());
    }

    private Scope(Frame frame, Map<String, Double> values) {
      before = CURRENT.get();
      CURRENT.set(frame);
      parameters = new ParameterRuntime.Scope(values);
    }

    public void close() {
      if (parameters == null) return;
      parameters.close();
      if (before == null) CURRENT.remove();
      else CURRENT.set(before);
    }
  }

  private static final Scope NOOP = new Scope();

  public static Scope open(LivingEntity owner, ManasSkillInstance skill, int mode) {
    var inherited = CURRENT.get();
    if (mode < 0
        && inherited != null
        && inherited.owner() == owner
        && inherited.skill().getSkillId().equals(skill.getSkillId())) return new Scope(inherited);
    Frame frame = null;
    if (!owner.level().isClientSide() && !LaplaceActions.legacy(skill)) {
      var own = Laplace.active(owner);
      if (own.isEmpty() && !owner.isAlive() && mode < 0)
        own =
            Laplace.owned(owner)
                .filter(i -> i.isToggled() && !owner.isSpectator() && i.canInteractSkill(owner));
      String key = skill.getSkillId() + "#" + mode;
      if (mode < 0
          && own.isPresent()
          && !Laplace.effectiveEnabled(owner, own.get(), key)
          && skill.isToggled())
        for (int m = 0; m < Math.clamp(skill.getModes(), 0, 128); m++) {
          String candidate = skill.getSkillId() + "#" + m;
          if (Laplace.effectiveEnabled(owner, own.get(), candidate)) {
            mode = m;
            key = candidate;
            break;
          }
        }
      if (own.isPresent() && Laplace.effectiveEnabled(owner, own.get(), key))
        try {
          var action = ExpandedActions.find(owner, skill, mode);
          if (action != null) {
            var equations = Laplace.equations(own.get(), action);
            frame =
                new Frame(
                    owner, skill, mode, action, equations, action.model().evaluate(equations));
          }
        } catch (IllegalArgumentException ignored) {
        }
    }
    return frame == null && CURRENT.get() == null && !ParameterRuntime.active()
        ? NOOP
        : new Scope(frame);
  }

  public static Scope frozen(
      LivingEntity owner,
      ManasSkillInstance skill,
      int mode,
      EquationModel model,
      Map<String, String> equations,
      EquationModel.Result result) {
    return new Scope(
        new Frame(
            owner,
            skill,
            mode,
            new LaplaceActions.Action(
                skill.getSkillId() + "#" + mode, "Frozen cast", "", skill, mode, model),
            equations,
            result));
  }

  public static Scope parameters(Map<String, Double> values) {
    return new Scope(null, values);
  }

  public static Frame current() {
    return CURRENT.get();
  }

  /** Preserve native cost multipliers, but price the edited action as one shared budget. */
  public static double budget(
      LivingEntity owner, ManasSkillInstance skill, int mode, double nativeBudget) {
    if (LaplaceActions.legacy(skill)) return Double.NaN;
    var frame = CURRENT.get();
    if (frame != null
        && frame.owner == owner
        && frame.skill.getSkillId().equals(skill.getSkillId())) {
      try {
        return LaplaceRuntime.withBase(frame.action.model(), "base_cost", nativeBudget)
            .evaluate(frame.equations)
            .values()
            .get("magicule_cost");
      } catch (IllegalArgumentException ex) {
        return -1;
      }
    }
    var own = Laplace.active(owner);
    if (own.isEmpty()) return Double.NaN;
    var action = ExpandedActions.find(owner, skill, mode);
    if (action == null || !Laplace.effectiveEnabled(owner, own.get(), action.key()))
      return Double.NaN;
    try {
      return LaplaceRuntime.withBase(action.model(), "base_cost", nativeBudget)
          .evaluate(Laplace.equations(own.get(), action))
          .values()
          .get("magicule_cost");
    } catch (IllegalArgumentException ex) {
      return -1;
    }
  }

  private ExpandedRuntime() {}
}
