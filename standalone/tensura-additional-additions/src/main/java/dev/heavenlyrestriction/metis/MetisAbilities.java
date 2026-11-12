package dev.heavenlyrestriction.metis;

import dev.heavenlyrestriction.splitmind.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.magic.Magic;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Learned-only, per-mode assignment policy shared by catalogue, packets and execution. */
public final class MetisAbilities {
  public static boolean eligible(ManasSkillInstance i) {
    if (i.getMastery() < 0 || i.getRemoveTime() == -3 || !(i.getSkill() instanceof TensuraSkill))
      return false;
    if (i.getSkill() instanceof Magic) return SplitMindCasting.eligible(i);
    if (Set.of(
            "heavenlyrestriction:metis",
            "heavenlyrestriction:split_mind",
            "heavenlyrestriction:parallel_world_jumper",
            "tensura:pride")
        .contains(i.getSkillId().toString())) return false;
    for (String name : List.of("onPressed", "onHeld", "onRelease")) {
      try {
        var args =
            name.equals("onRelease")
                ? new Class<?>[] {
                  ManasSkillInstance.class, LivingEntity.class, int.class, int.class, int.class
                }
                : new Class<?>[] {
                  ManasSkillInstance.class, LivingEntity.class, int.class, int.class
                };
        if (i.getSkill().getClass().getMethod(name, args).getDeclaringClass() != ManasSkill.class)
          return true;
      } catch (ReflectiveOperationException ignored) {
        return false;
      }
    }
    return false;
  }

  public static Set<Integer> available(LivingEntity owner, ManasSkillInstance i) {
    if (!eligible(i)) return Set.of();
    var skill = (TensuraSkill) i.getSkill();
    var probe = i.copy();
    int count = SplitMindModes.count(i);
    var seen = new HashSet<Integer>();
    var result = new TreeSet<Integer>();
    var todo = new ArrayDeque<Integer>();
    todo.add(0);
    while (!todo.isEmpty() && seen.size() <= count + 1) {
      int mode = todo.removeFirst();
      if (!seen.add(mode)) continue;
      if (mode >= 0
          && mode < count
          && skill.canBeSlotted(probe, owner, mode)
          && (!skill.getModeLearningList(probe).contains(mode)
              || probe.getOrCreateTag().getDouble(skill.getModeId(probe, mode))
                  >= skill.getLearningPointRequirement(probe, owner, mode))
          && !MetisMenuInspection.blocks(skill.getClass(), mode, i.isMastered(owner)))
        result.add(mode);
      for (boolean reverse : new boolean[] {false, true}) {
        int next = skill.nextMode(owner, probe, mode, reverse);
        if (next >= -1 && next < count && !seen.contains(next)) todo.add(next);
      }
    }
    return Collections.unmodifiableSet(result);
  }

  public static boolean allowed(LivingEntity owner, ManasSkillInstance i, int mode) {
    return mode >= 0 && mode < SplitMindModes.count(i) && available(owner, i).contains(mode);
  }

  public static boolean held(ManasSkillInstance i) {
    if (i.getSkill() instanceof Magic) return true;
    try {
      return i.getSkill()
              .getClass()
              .getMethod(
                  "onHeld", ManasSkillInstance.class, LivingEntity.class, int.class, int.class)
              .getDeclaringClass()
          != ManasSkill.class;
    } catch (ReflectiveOperationException ex) {
      return false;
    }
  }

  public static int releaseAfter(ManasSkillInstance i, LivingEntity owner) {
    return i.getSkill() instanceof Magic magic
        ? magic.getCastingTime(i, owner)
        : i.getMaxHeldTime(owner);
  }

  public static ListTag modes(ServerPlayer p, ManasSkillInstance i) {
    var modes = new ListTag();
    var skill = (TensuraSkill) i.getSkill();
    for (int mode : available(p, i)) {
      var option = new CompoundTag();
      option.putInt("id", mode);
      option.putString("name", skill.getModeName(i, mode).getString());
      option.putBoolean("available", true);
      option.putDouble(
          "mp",
          skill.getMagiculeCost(p, i, mode)
              * (skill instanceof Magic ? MetisMatrix.costFactor(p) : 1));
      option.putDouble("ap", skill.getAuraCost(p, i, mode));
      option.putInt("cooldown", i.getCoolDown(mode));
      modes.add(option);
    }
    return modes;
  }

  public static CompoundTag catalog(ServerPlayer p, String query, int category, int page) {
    query = query.substring(0, Math.min(64, query.length()));
    category = Math.clamp(category, 0, 4);
    final String search = query.toLowerCase(Locale.ROOT);
    final int filter = category;
    var list =
        SkillAPI.getSkillsFrom(p).getLearnedSkills().stream()
            .filter(MetisAbilities::eligible)
            .filter(
                i ->
                    switch (filter) {
                      case 1 ->
                          i.getSkill() instanceof Magic m
                              && m.getType() == Magic.MagicType.ASPECTUAL;
                      case 2 ->
                          i.getSkill() instanceof Magic m
                              && m.getType() == Magic.MagicType.SPIRITUAL;
                      case 3 ->
                          i.getSkill() instanceof io.github.manasmods.tensura.ability.skill.Skill;
                      case 4 ->
                          !(i.getSkill() instanceof Magic)
                              && !(i.getSkill()
                                  instanceof io.github.manasmods.tensura.ability.skill.Skill);
                      default -> true;
                    })
            .filter(
                i ->
                    i.getSkill().getName().getString().toLowerCase(Locale.ROOT).contains(search)
                        || i.getSkillId().toString().contains(search))
            .filter(i -> !available(p, i).isEmpty())
            .sorted(
                Comparator.comparing(
                        (ManasSkillInstance i) -> i.getSkill().getName().getString(),
                        String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(i -> i.getSkillId().toString()))
            .toList();
    int pages = Math.max(1, (list.size() + 4) / 5);
    page = Math.clamp(page, 0, pages - 1);
    var rows = new ListTag();
    for (var i : list.subList(page * 5, Math.min(list.size(), page * 5 + 5))) {
      var row = new CompoundTag();
      row.putString("id", i.getSkillId().toString());
      row.putString("name", i.getSkill().getName().getString());
      row.putString("icon", i.getSkill().getSkillIcon().toString());
      row.putBoolean("mastered", i.isMastered(p));
      row.put("modes", modes(p, i));
      rows.add(row);
    }
    var out = new CompoundTag();
    out.putString("query", query);
    out.putInt("category", category);
    out.putInt("page", page);
    out.putInt("pages", pages);
    out.putInt("total", list.size());
    out.put("rows", rows);
    return out;
  }

  private MetisAbilities() {}
}
