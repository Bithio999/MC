package dev.heavenlyrestriction.laplace;

import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.skill.api.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Profiles live with the actual owned skill and are revalidated against live native constants. */
public final class Laplace {
  public static final String PROFILES = "LaplaceEquations";

  public static Optional<ManasSkillInstance> owned(LivingEntity e) {
    return SkillAPI.getSkillsFrom(e)
        .getSkill(HeavenlyRestriction.LAPLACE.get())
        .filter(i -> i.getMastery() >= 0 && i.getRemoveTime() != -3);
  }

  public static Optional<ManasSkillInstance> active(LivingEntity e) {
    return owned(e)
        .filter(i -> e.isAlive() && !e.isSpectator() && i.isToggled() && i.canInteractSkill(e));
  }

  public static int capacity(LivingEntity e) {
    return owned(e).filter(i -> i.isMastered(e)).isPresent() ? 4 : 3;
  }

  public static Map<String, String> equations(ManasSkillInstance i, LaplaceActions.Action a) {
    var row = i.getOrCreateTag().getCompound(PROFILES).getCompound(a.key());
    if (!row.contains("equations")) return new LinkedHashMap<>(a.model().defaults());
    var t = row.getCompound("equations");
    var result = new LinkedHashMap<String, String>();
    for (String key : a.model().defaults().keySet())
      if (t.contains(key, Tag.TAG_STRING)) result.put(key, t.getString(key));
    t.getAllKeys().stream()
        .sorted()
        .filter(k -> !result.containsKey(k))
        .forEach(k -> result.put(k, t.getString(k)));
    return result;
  }

  public static boolean enabled(ManasSkillInstance i, String key) {
    return i.getOrCreateTag().getCompound(PROFILES).getCompound(key).getBoolean("enabled");
  }

  public static boolean effectiveEnabled(LivingEntity e, ManasSkillInstance i, String key) {
    var rows = i.getOrCreateTag().getCompound(PROFILES);
    return rows.getAllKeys().stream()
        .filter(k -> enabled(i, k))
        .sorted()
        .limit(capacity(e))
        .anyMatch(key::equals);
  }

  public static long remaining(LivingEntity e, ManasSkillInstance i) {
    long delta = i.getOrCreateTag().getLong("EditUntil") - e.level().getGameTime();
    // World Jump can restore a different game clock; an old world's timestamp must not lock
    // editing.
    return delta > 0 && delta <= 100 ? delta : 0;
  }

  public static String save(
      ServerPlayer p, String key, Map<String, String> equations, boolean enable) {
    var own = owned(p);
    if (own.isEmpty()) return "Learn Laplace first.";
    var i = own.get();
    var a = LaplaceActions.byKey(p, key);
    if (a == null) return "You must own and have learned this action.";
    if (remaining(p, i) > 0)
      return "Wait " + ((remaining(p, i) + 19) / 20) + " seconds before changing another formula.";
    if (!SkillAPI.getSkillsFrom(p).getHeldSkills().isEmpty())
      return "Finish casting before changing equations.";
    a.model().evaluate(equations);
    var profiles = i.getOrCreateTag().getCompound(PROFILES);
    long enabled =
        profiles.getAllKeys().stream()
            .filter(k -> profiles.getCompound(k).getBoolean("enabled") && !k.equals(key))
            .count();
    if (enable && enabled >= capacity(p))
      return "Disable another edited action first (limit " + capacity(p) + ").";
    if (!profiles.contains(key) && profiles.size() >= 32)
      return "At most 32 saved actions are allowed.";
    var row = new CompoundTag();
    var values = new CompoundTag();
    equations.forEach(values::putString);
    row.put("equations", values);
    row.putBoolean("enabled", enable);
    profiles.put(key, row);
    i.getOrCreateTag().put(PROFILES, profiles);
    i.getOrCreateTag().putLong("EditUntil", p.level().getGameTime() + 100);
    i.markDirty();
    return "Saved. " + (enable ? "Edited formula is enabled." : "Native formula is active.");
  }

  public static Optional<EquationModel.Result> result(
      LivingEntity e, ManasSkillInstance i, int mode) {
    var own = active(e);
    if (own.isEmpty()) return Optional.empty();
    var a = LaplaceActions.find(e, i, mode);
    if (a == null || !effectiveEnabled(e, own.get(), a.key())) return Optional.empty();
    try {
      return Optional.of(a.model().evaluate(equations(own.get(), a)));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private Laplace() {}
}
