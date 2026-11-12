package dev.heavenlyrestriction.tyche;

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
import net.minecraft.world.entity.LivingEntity;

public final class TycheSynthesis {
  public static final String CRITS = "tyche_critical_hits", SURVIVED = "tyche_tuner_types";
  private static final Set<UUID> BUSY = new HashSet<>();

  public static void critical(LivingEntity e) {
    if (!e.level().isClientSide()) {
      var t = RestrictionData.of(e);
      t.putInt(CRITS, Math.min(1000000, t.getInt(CRITS) + 1));
    }
  }

  public static void survived(LivingEntity e, String type) {
    if (!e.level().isClientSide()) {
      var t = RestrictionData.of(e);
      var types = t.getCompound(SURVIVED);
      types.putBoolean(type, true);
      t.put(SURVIVED, types);
    }
  }

  public static int types(LivingEntity e) {
    return RestrictionData.of(e).getCompound(SURVIVED).size();
  }

  public static double cost(ServerPlayer p) {
    return p.hasInfiniteMaterials()
        ? 0
        : TycheConfig.ACQUISITION.get()
            * p.level().getGameRules().getInt(TensuraGameRules.MP_SKILL_COST)
            / 100d;
  }

  public static boolean affordable(ServerPlayer p) {
    var a = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
    return a != null && a.getBaseValue() > cost(p) && a.getValue() > cost(p);
  }

  public static boolean ready(ServerPlayer p) {
    return Tyche.owned(p).isEmpty()
        && ForgeSynthesis.settled(p)
        && ForgeSynthesis.awakened(p)
        && ForgeSynthesis.mastered(p, HeavenlyRestriction.CRITICAL_MERCHANT.get())
        && ForgeSynthesis.mastered(p, UniqueSkills.TUNER.get())
        && RestrictionData.of(p).getInt(CRITS) >= TycheConfig.CRITICALS.get()
        && types(p) >= TycheConfig.SURVIVALS.get()
        && affordable(p);
  }

  public static void discover(ServerPlayer p) {
    var skills = SkillAPI.getSkillsFrom(p);
    if (skills.getSkill(HeavenlyRestriction.TYCHE.get()).isPresent()
        || skills.getSkill(HeavenlyRestriction.CRITICAL_MERCHANT.get()).isEmpty()
            && skills.getSkill(UniqueSkills.TUNER.get()).isEmpty()) return;
    var i = HeavenlyRestriction.TYCHE.get().createLearningInstance(p);
    i.setMastery(-1);
    SkillHelper.learnSkill(p, i);
  }

  public static boolean synthesize(ServerPlayer p) {
    if (!ready(p) || !BUSY.add(p.getUUID())) return false;
    try {
      var skills = SkillAPI.getSkillsFrom(p);
      var merchant = skills.getSkill(HeavenlyRestriction.CRITICAL_MERCHANT.get()).orElseThrow();
      var tuner = skills.getSkill(UniqueSkills.TUNER.get()).orElseThrow();
      var result = HeavenlyRestriction.TYCHE.get().createDefaultInstance();
      result.setMastery(0);
      result.setParentSkill(tuner.getSkill());
      result.getOrCreateTag().putBoolean("NoMagiculeCost", true);
      try (var context =
          ChainedContext.ability(
              p,
              ChainedState.inherited(p, tuner.getSkill())
                  ? tuner.getSkill()
                  : merchant.getSkill())) {
        if (!SkillHelper.learnSkill(p, result)) return false;
      }
      var actual = Tyche.owned(p).orElseThrow();
      actual.setTag(merchant.getOrCreateTag().copy());
      actual.setParentSkill(null);
      actual.setToggled(merchant.isToggled() || tuner.isToggled());
      var history = new CompoundTag();
      for (var item : tuner.getOrCreateTag().getList("deaths", Tag.TAG_COMPOUND))
        if (item instanceof CompoundTag row)
          for (String key : row.getAllKeys()) history.putBoolean(key, true);
      if (!history.isEmpty()) {
        actual.getOrCreateTag().put(FateInsurance.HISTORY, history);
        actual.setCoolDown(Math.min(300, Math.max(1, tuner.getCoolDown(0))), FateInsurance.MODE);
      }
      actual.setCoolDown(merchant.getCoolDown(0), 0);
      if (ChainedState.inherited(p, merchant.getSkill())
          || ChainedState.inherited(p, tuner.getSkill()))
        ChainedState.remember(p, actual.getSkill());
      double bill = cost(p);
      var a = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
      a.setBaseValue(a.getBaseValue() - bill);
      var energy = TensuraStorages.getExistenceFrom(p);
      energy.setMagicule(Math.max(0, Math.min(energy.getMagicule() - bill, a.getValue())));
      energy.markDirty();
      var ability = TensuraStorages.getAbilityFrom(p);
      var slots = new ArrayList<int[]>();
      for (int preset = 0; preset < 9; preset++)
        for (int slot = 0; slot < 3; slot++) {
          var old = ability.getAbilitySlot(preset, slot);
          if (old.getSkill() == merchant.getSkill() || old.getSkill() == tuner.getSkill())
            slots.add(new int[] {preset, slot});
        }
      skills.forgetSkill(merchant);
      skills.forgetSkill(tuner);
      p.removeEffect(
          io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(
              io.github.manasmods.tensura.registry.effect.TensuraMobEffects.FATE_CHANGE));
      for (var slot : slots) ability.setAbilitySlot(slot[0], slot[1], actual.getSkill(), 0);
      ability.markDirty();
      actual.markDirty();
      Tyche.refresh(p);
      skills.checkAndMarkDirty(actual);
      ((io.github.manasmods.manascore.storage.api.StorageHolder) p).manasCore$sync();
      return true;
    } finally {
      BUSY.remove(p.getUUID());
    }
  }

  private TycheSynthesis() {}
}
