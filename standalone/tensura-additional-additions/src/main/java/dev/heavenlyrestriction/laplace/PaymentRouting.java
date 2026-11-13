package dev.heavenlyrestriction.laplace;

import dev.heavenlyrestriction.exponentiation.ExponentiationContext;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.ep.IExistence;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/** Scoped at the native payment boundary, after native discounts and casting modifiers. */
public final class PaymentRouting {
  private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

  public static final class Scope implements AutoCloseable {
    private final Scope before;
    private final LivingEntity owner;
    private final PaymentPlan.Policy policy;
    private double budget, reserve;
    private boolean failed, paid;

    private Scope(LivingEntity owner, PaymentPlan.Policy policy) {
      before = CURRENT.get();
      this.owner = owner;
      this.policy = policy;
      CURRENT.set(this);
    }

    public boolean failed() {
      return failed;
    }

    public void close() {
      if (before == null) CURRENT.remove();
      else CURRENT.set(before);
    }

    public void reserve(double budget) {
      if (!Double.isFinite(budget) || budget < 0)
        throw new IllegalArgumentException("Invalid resource reserve.");
      reserve = budget;
    }

    private PaymentPlan.Debit prepare(IExistence energy) {
      var debit =
          PaymentPlan.prepare(
              new PaymentPlan.Pools(
                  energy.getMagicule(),
                  energy.getAura(),
                  owner.getHealth(),
                  energy.getSpiritualHealth()),
              policy.bill(budget));
      var held = policy.bill(reserve);
      var after = debit.after();
      return new PaymentPlan.Debit(
          after,
          debit.affordable()
              && after.magicules() >= held.magicules()
              && after.aura() >= held.aura()
              && (held.hp() == 0 || after.hp() >= Math.max(1, held.hp()))
              && (held.shp() == 0 || after.shp() >= Math.max(1, held.shp())));
    }
  }

  public static Scope open(LivingEntity owner, ManasSkillInstance skill, int mode) {
    return open(owner, skill, mode, false);
  }

  public static Scope open(LivingEntity owner, ManasSkillInstance skill, int mode, boolean force) {
    PaymentPlan.Policy policy = null;
    if (!owner.level().isClientSide()) {
      var expanded = dev.heavenlyrestriction.laplace.expand.ExpandedRuntime.current();
      if (expanded != null
          && expanded.owner() == owner
          && expanded.skill().getSkillId().equals(skill.getSkillId()))
        policy = expanded.action().model().payment();
      var frame = LaplaceRuntime.current();
      if (frame != null
          && frame.owner() == owner
          && frame.action().skill() == skill
          && frame.action().mode() == mode) {
        policy = frame.action().model().payment();
      } else if (policy == null) {
        var own = Laplace.active(owner);
        var action = own.isEmpty() ? null : LaplaceActions.find(owner, skill, mode);
        if (action != null && Laplace.effectiveEnabled(owner, own.get(), action.key())) {
          try {
            action.model().evaluate(Laplace.equations(own.get(), action));
            policy = action.model().payment();
          } catch (IllegalArgumentException ignored) {
          }
        }
      }
    }
    return new Scope(owner, policy != null && (force || policy.plan().converted()) ? policy : null);
  }

  public static Scope healing(LivingEntity owner) {
    var frame = LaplaceRuntime.current();
    return LaplaceRuntime.hasHealing() && frame != null && frame.owner() == owner
        ? open(owner, frame.action().skill(), frame.action().mode())
        : new Scope(owner, null);
  }

  private static Scope current(LivingEntity owner) {
    var scope = CURRENT.get();
    return scope != null && scope.owner == owner && scope.policy != null ? scope : null;
  }

  public static boolean scoped(LivingEntity owner) {
    var scope = CURRENT.get();
    return scope != null && scope.owner == owner;
  }

  public static boolean converted(LivingEntity owner) {
    return current(owner) != null;
  }

  /** Virtual funding equals the actual final bill, avoiding cancellation at very large reserves. */
  public static double funding(
      LivingEntity owner, IExistence energy, double budget, double nativeValue) {
    var scope = current(owner);
    if (scope == null) return nativeValue;
    scope.budget = budget;
    try {
      if (scope.paid || scope.failed || !scope.prepare(energy).affordable()) {
        scope.failed = true;
        return 0;
      }
      return budget;
    } catch (IllegalArgumentException ex) {
      scope.failed = true;
      return 0;
    }
  }

  /** Suppresses the native MP write. The whole mixed payment is committed once or not at all. */
  public static boolean debit(LivingEntity owner, IExistence energy) {
    var scope = current(owner);
    if (scope == null) return false;
    if (scope.failed || scope.paid) return true;
    try {
      var debit = scope.prepare(energy);
      if (!debit.affordable()) {
        scope.failed = true;
        return true;
      }
      scope.paid = true;
      var after = debit.after();
      try (var raw = ExponentiationContext.raw(owner, ExponentiationContext.RESTORE)) {
        energy.setMagicule(after.magicules());
        energy.setAura(after.aura());
        if (scope.policy.plan().hp() > 0) owner.setHealth((float) after.hp());
        if (scope.policy.plan().shp() > 0) energy.setSpiritualHealth(after.shp());
      }
      energy.markDirty();
    } catch (IllegalArgumentException ex) {
      scope.failed = true;
    }
    return true;
  }

  public static Component message(LivingEntity owner, Component original) {
    var scope = current(owner);
    if (scope == null) return original;
    try {
      var bill = scope.policy.bill(scope.budget);
      var parts = new java.util.ArrayList<String>();
      if (bill.magicules() > 0) parts.add(number(bill.magicules()) + " magicules");
      if (bill.aura() > 0) parts.add(number(bill.aura()) + " aura");
      if (bill.hp() > 0) parts.add(number(bill.hp()) + " HP");
      if (bill.shp() > 0) parts.add(number(bill.shp()) + " SHP");
      return Component.literal(
              "Cannot pay " + String.join(" + ", parts) + ". HP and SHP must retain 1 point.")
          .withStyle(ChatFormatting.RED);
    } catch (IllegalArgumentException ex) {
      return Component.literal("This resource payment is invalid.").withStyle(ChatFormatting.RED);
    }
  }

  private static String number(double n) {
    return String.format(Locale.ROOT, n >= 1000 ? "%.0f" : "%.2f", n);
  }

  private PaymentRouting() {}
}
