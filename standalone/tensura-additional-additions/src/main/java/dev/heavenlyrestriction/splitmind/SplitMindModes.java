package dev.heavenlyrestriction.splitmind;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Magic;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;

/** Read-only native mode traversal; never calls learnMode or spends learning resources. */
public final class SplitMindModes {
  public static int count(ManasSkillInstance i) {
    return Math.clamp(i.getModes(), 0, 128);
  }

  public static Set<Integer> available(LivingEntity owner, ManasSkillInstance i) {
    if (!SplitMindCasting.eligible(i)) return Set.of();
    int count = count(i);
    if (count == 0) return Set.of();
    var seen = new HashSet<Integer>();
    var modes = new TreeSet<Integer>();
    var todo = new ArrayDeque<Integer>();
    todo.add(0);
    while (!todo.isEmpty() && seen.size() <= count + 1) {
      int mode = todo.removeFirst();
      if (!seen.add(mode)) continue;
      if (mode >= 0 && mode < count) modes.add(mode);
      for (boolean reverse : new boolean[] {false, true}) {
        int next = ((Magic) i.getSkill()).nextMode(owner, i, mode, reverse);
        if (next >= -1 && next < count && !seen.contains(next)) todo.add(next);
      }
    }
    var magic = (Magic) i.getSkill();
    modes.removeIf(
        mode ->
            magic.getModeLearningList(i).contains(mode)
                && (i.getTag() == null ? 0 : i.getTag().getDouble(magic.getModeId(i, mode)))
                    < magic.getLearningPointRequirement(i, owner, mode));
    return Collections.unmodifiableSet(modes);
  }

  public static boolean allowed(LivingEntity owner, ManasSkillInstance i, int mode) {
    return mode >= 0 && mode < count(i) && available(owner, i).contains(mode);
  }

  /** Native modes that fire from onHeld rather than onRelease. Their own cadence is retained. */
  public static boolean sustained(ManasSkillInstance i, int mode) {
    if (mode != 1 || !i.getSkillId().getNamespace().equals("tensura")) return false;
    return Set.of("fire_lance", "icicle_lance", "icicle_rain", "water_cutter", "search_enemy")
        .contains(i.getSkillId().getPath());
  }

  private SplitMindModes() {}
}
