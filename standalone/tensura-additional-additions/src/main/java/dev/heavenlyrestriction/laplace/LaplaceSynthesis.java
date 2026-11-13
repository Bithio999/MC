package dev.heavenlyrestriction.laplace;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.forge.ForgeSynthesis;
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
public final class LaplaceSynthesis {
  private static final Set<UUID> BUSY = new HashSet<>();

  public static int learnedMagic(ServerPlayer p) {
    return (int)
        LaplaceActions.learned(p).stream()
            .filter(
                a ->
                    !a.skill().isTemporarySkill()
                        && !a.skill().isSubInstance()
                        && a.skill().isMastered(p))
            .map(a -> a.skill().getSkillId())
            .distinct()
            .count();
  }

  public static double cost(ServerPlayer p) {
    return p.hasInfiniteMaterials()
        ? 0
        : LaplaceConfig.ACQUISITION.get()
            * p.level().getGameRules().getInt(TensuraGameRules.MP_SKILL_COST)
            / 100d;
  }

  public static boolean affordable(ServerPlayer p) {
    var a = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
    return a != null && a.getBaseValue() > cost(p) && a.getValue() > cost(p);
  }

  public static boolean ready(ServerPlayer p) {
    return Laplace.owned(p).isEmpty()
        && ForgeSynthesis.settled(p)
        && ForgeSynthesis.awakened(p)
        && ForgeSynthesis.mastered(p, HeavenlyRestriction.EXPONENTIATION.get())
        && ForgeSynthesis.mastered(p, UniqueSkills.MATHEMATICIAN.get())
        && learnedMagic(p) >= LaplaceConfig.ACHIEVEMENT.get()
        && affordable(p);
  }

  public static void discover(ServerPlayer p) {
    var skills = SkillAPI.getSkillsFrom(p);
    if (skills.getSkill(HeavenlyRestriction.LAPLACE.get()).isPresent()
        || skills.getSkill(HeavenlyRestriction.EXPONENTIATION.get()).isEmpty()
            && skills.getSkill(UniqueSkills.MATHEMATICIAN.get()).isEmpty()) return;
    var entry = HeavenlyRestriction.LAPLACE.get().createLearningInstance(p);
    entry.setMastery(-1);
    SkillHelper.learnSkill(p, entry);
  }

  public static boolean synthesize(ServerPlayer p) {
    if (!ready(p) || !BUSY.add(p.getUUID())) return false;
    try {
      var skills = SkillAPI.getSkillsFrom(p);
      var mind = skills.getSkill(HeavenlyRestriction.EXPONENTIATION.get()).orElseThrow();
      var sage = skills.getSkill(UniqueSkills.MATHEMATICIAN.get()).orElseThrow();
      if (p.containerMenu != p.inventoryMenu) p.closeContainer();
      var tag = new CompoundTag();
      var result = HeavenlyRestriction.LAPLACE.get().createDefaultInstance();
      result.setMastery(0);
      result.setParentSkill(UniqueSkills.MATHEMATICIAN.get());
      result.getOrCreateTag().putBoolean("NoMagiculeCost", true);
      try (var scope =
          ChainedContext.ability(
              p, ChainedState.inherited(p, sage.getSkill()) ? sage.getSkill() : mind.getSkill())) {
        if (!SkillHelper.learnSkill(p, result)) return false;
      }
      var actual = Laplace.owned(p).orElseThrow();
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
          if (original.getSkill() == mind.getSkill() || original.getSkill() == sage.getSkill())
            slots.add(new int[] {preset, slot, 0});
        }
      boolean toggled =
          mind.isToggled()
              || sage.isToggled()
              || HeavenlyRestriction.EXPONENTIATION.get().isInSlot(p, mind);
      skills.forgetSkill(mind);
      skills.forgetSkill(sage);
      for (var slot : slots) ability.setAbilitySlot(slot[0], slot[1], actual.getSkill(), slot[2]);
      ability.markDirty();
      actual.setToggled(toggled);
      LaplaceIntellect.refresh(p);
      skills.checkAndMarkDirty(actual);
      ((io.github.manasmods.manascore.storage.api.StorageHolder) p).manasCore$sync();
      return true;
    } finally {
      BUSY.remove(p.getUUID());
    }
  }

  private LaplaceSynthesis() {}
}
