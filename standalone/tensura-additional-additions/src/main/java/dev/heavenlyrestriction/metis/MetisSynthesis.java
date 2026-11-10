package dev.heavenlyrestriction.metis;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.forge.ForgeSynthesis;
import dev.heavenlyrestriction.splitmind.SplitMindCasting;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.world.TensuraGameRules;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Explicit, post-awakening synthesis. A discoverable unfinished entry cannot bypass the bill. */
public final class MetisSynthesis {
  private static final Set<UUID> BUSY = new HashSet<>();

  public static int learnedMagic(ServerPlayer p) {
    return (int)
        SkillAPI.getSkillsFrom(p).getLearnedSkills().stream()
            .filter(
                i -> !i.isTemporarySkill() && !i.isSubInstance() && SplitMindCasting.eligible(i))
            .count();
  }

  public static double cost(ServerPlayer p) {
    return p.hasInfiniteMaterials()
        ? 0
        : MetisConfig.ACQUISITION.get()
            * p.level().getGameRules().getInt(TensuraGameRules.MP_SKILL_COST)
            / 100d;
  }

  public static boolean affordable(ServerPlayer p) {
    var a = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
    return a != null && a.getBaseValue() > cost(p) && a.getValue() > cost(p);
  }

  public static boolean ready(ServerPlayer p) {
    return MetisMatrix.owned(p).isEmpty()
        && ForgeSynthesis.settled(p)
        && ForgeSynthesis.awakened(p)
        && ForgeSynthesis.mastered(p, HeavenlyRestriction.SPLIT_MIND.get())
        && ForgeSynthesis.mastered(p, UniqueSkills.GREAT_SAGE.get())
        && learnedMagic(p) >= MetisConfig.MAGIC_ACHIEVEMENT.get()
        && affordable(p);
  }

  public static void discover(ServerPlayer p) {
    var skills = SkillAPI.getSkillsFrom(p);
    if (skills.getSkill(HeavenlyRestriction.METIS.get()).isPresent()
        || skills.getSkill(HeavenlyRestriction.SPLIT_MIND.get()).isEmpty()
            && skills.getSkill(UniqueSkills.GREAT_SAGE.get()).isEmpty()) return;
    var entry = HeavenlyRestriction.METIS.get().createLearningInstance(p);
    entry.setMastery(-1);
    SkillHelper.learnSkill(p, entry);
  }

  public static boolean synthesize(ServerPlayer p) {
    if (!ready(p) || !BUSY.add(p.getUUID())) return false;
    try {
      var skills = SkillAPI.getSkillsFrom(p);
      var mind = skills.getSkill(HeavenlyRestriction.SPLIT_MIND.get()).orElseThrow();
      var sage = skills.getSkill(UniqueSkills.GREAT_SAGE.get()).orElseThrow();
      if (p.containerMenu != p.inventoryMenu) p.closeContainer();
      var tag = sage.getOrCreateTag().copy();
      for (String key :
          List.of("Repeating", "Brewing", "RepeatBrewing", "startRefine", "Activated"))
        tag.remove(key);
      var assignment = SplitMindCasting.assigned(p, mind);
      if (assignment.isPresent()) {
        var rows = new ListTag();
        var row = new CompoundTag();
        row.putInt("slot", 0);
        row.putString("id", assignment.get().getSkillId().toString());
        row.putInt("mode", SplitMindCasting.assignedMode(mind));
        row.putInt("priority", 2);
        row.putBoolean("target", true);
        rows.add(row);
        tag.put(MetisMatrix.SLOTS, rows);
      }
      tag.putBoolean(MetisMatrix.AUTO, false);
      tag.putBoolean(MetisMatrix.OPTIMAL, true);
      var result = HeavenlyRestriction.METIS.get().createDefaultInstance();
      result.setMastery(0);
      result.setParentSkill(UniqueSkills.GREAT_SAGE.get());
      result.getOrCreateTag().putBoolean("NoMagiculeCost", true);
      try (var scope =
          ChainedContext.ability(
              p, ChainedState.inherited(p, sage.getSkill()) ? sage.getSkill() : mind.getSkill())) {
        if (!SkillHelper.learnSkill(p, result)) return false;
      }
      var actual = MetisMatrix.owned(p).orElseThrow();
      actual.setTag(tag);
      actual.setParentSkill(null);
      if (ChainedState.inherited(p, mind.getSkill()) || ChainedState.inherited(p, sage.getSkill()))
        ChainedState.remember(p, actual.getSkill());
      double bill = cost(p);
      var attr = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
      attr.setBaseValue(attr.getBaseValue() - bill);
      var energy = TensuraStorages.getExistenceFrom(p);
      energy.setMagicule(Math.max(0, Math.min(energy.getMagicule() - bill, attr.getValue())));
      energy.markDirty();
      var slots = new ArrayList<int[]>();
      var ability = TensuraStorages.getAbilityFrom(p);
      for (int preset = 0; preset < 9; preset++)
        for (int slot = 0; slot < 3; slot++) {
          var original = ability.getAbilitySlot(preset, slot);
          if (original.getSkill() == mind.getSkill())
            slots.add(new int[] {preset, slot, original.getMode() == 1 ? 1 : 0});
          else if (original.getSkill() == sage.getSkill())
            slots.add(
                new int[] {
                  preset,
                  slot,
                  switch (original.getMode()) {
                    case 1 -> 3;
                    case 2 -> 4;
                    default -> 2;
                  }
                });
        }
      boolean toggled = mind.isToggled() || sage.isToggled();
      SplitMindCasting.cancel(p);
      skills.forgetSkill(mind);
      skills.forgetSkill(sage);
      for (var slot : slots) ability.setAbilitySlot(slot[0], slot[1], actual.getSkill(), slot[2]);
      ability.markDirty();
      actual.setToggled(toggled);
      MetisIntellect.refresh(p);
      skills.checkAndMarkDirty(actual);
      ((io.github.manasmods.manascore.storage.api.StorageHolder) p).manasCore$sync();
      return true;
    } finally {
      BUSY.remove(p.getUUID());
    }
  }

  private MetisSynthesis() {}
}
