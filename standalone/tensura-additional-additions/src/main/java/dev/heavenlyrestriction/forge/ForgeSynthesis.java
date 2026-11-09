package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.effect.debuff.SleepEffect;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.world.TensuraGameRules;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/**
 * The native Ultimate menu offers the entry; only an explicit synthesis can consume its parents.
 */
public final class ForgeSynthesis {
  public static final TagKey<Item> QUALIFYING =
      TagKey.create(Registries.ITEM, HeavenlyRestriction.id("hephaestus_synthesis_equipment"));
  private static final Set<UUID> BUSY = new HashSet<>();
  private static final Map<LivingEntity, Long> AWAKENING_WINDOW = new WeakHashMap<>();

  public static Optional<ManasSkillInstance> active(LivingEntity e) {
    return e == null
        ? Optional.empty()
        : SkillAPI.getSkillsFrom(e)
            .getSkill(HeavenlyRestriction.HEPHAESTUS.get())
            .filter(i -> i.getMastery() >= 0 && i.isToggled() && i.canInteractSkill(e));
  }

  public static Optional<ManasSkillInstance> owned(LivingEntity e) {
    return SkillAPI.getSkillsFrom(e)
        .getSkill(HeavenlyRestriction.HEPHAESTUS.get())
        .filter(i -> i.getMastery() >= 0);
  }

  private static boolean permanent(ManasSkillInstance i) {
    return !i.isTemporarySkill() && !i.isSubInstance();
  }

  public static boolean hasCraftsman(LivingEntity e) {
    return SkillAPI.getSkillsFrom(e)
        .getSkill(UniqueSkills.GODLY_CRAFTSMAN.get())
        .filter(i -> i.getMastery() >= 0 && permanent(i))
        .isPresent();
  }

  public static boolean mastered(LivingEntity e, ManasSkill s) {
    return SkillAPI.getSkillsFrom(e)
        .getSkill(s)
        .filter(i -> permanent(i) && i.isMastered(e))
        .isPresent();
  }

  public static boolean awakened(LivingEntity e) {
    var ep = TensuraStorages.getExistenceFrom(e);
    return ep.isTrueHero() || ep.isTrueDemonLord();
  }

  public static boolean settled(LivingEntity e) {
    return TensuraStorages.getExistenceFrom(e).getHarvestTick() <= 0
        && !SleepEffect.isForcedSleeping(e)
        && (e.getServer() == null
            || e.getServer().getTickCount() >= AWAKENING_WINDOW.getOrDefault(e, Long.MIN_VALUE));
  }

  public static double cost(ServerPlayer p) {
    return p.hasInfiniteMaterials()
        ? 0
        : ForgeConfig.ACQUISITION.get()
            * p.level().getGameRules().getInt(TensuraGameRules.MP_SKILL_COST)
            / 100d;
  }

  public static boolean affordable(ServerPlayer p) {
    var attr = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
    return attr != null && attr.getBaseValue() > cost(p) && attr.getValue() > cost(p);
  }

  public static boolean ready(ServerPlayer p) {
    return owned(p).isEmpty()
        && settled(p)
        && awakened(p)
        && mastered(p, HeavenlyRestriction.PROSPECTOR.get())
        && mastered(p, UniqueSkills.GODLY_CRAFTSMAN.get())
        && RestrictionData.of(p).getBoolean("hephaestus_forged_high_tier")
        && affordable(p);
  }

  public static void awakening(LivingEntity e) {
    if (e.getServer() != null) AWAKENING_WINDOW.put(e, (long) e.getServer().getTickCount() + 2);
  }

  public static void discover(ServerPlayer p) {
    var skills = SkillAPI.getSkillsFrom(p);
    if (skills.getSkill(HeavenlyRestriction.HEPHAESTUS.get()).isPresent()
        || skills.getSkill(HeavenlyRestriction.PROSPECTOR.get()).isEmpty() && !hasCraftsman(p))
      return;
    var i = HeavenlyRestriction.HEPHAESTUS.get().createLearningInstance(p);
    i.setMastery(-1);
    SkillHelper.learnSkill(p, i);
  }

  public static boolean synthesize(ServerPlayer p) {
    if (!ready(p) || !BUSY.add(p.getUUID())) return false;
    try {
      var skills = SkillAPI.getSkillsFrom(p);
      var prospect = skills.getSkill(HeavenlyRestriction.PROSPECTOR.get()).orElseThrow();
      var crafts = skills.getSkill(UniqueSkills.GODLY_CRAFTSMAN.get()).orElseThrow();
      if (p.containerMenu != p.inventoryMenu) p.closeContainer();
      var tag = crafts.getOrCreateTag().copy();
      tag.remove("Brewing");
      tag.remove("RepeatBrewing");
      tag.remove("startRefine");
      var storage = tag.getList("SpatialStorage", Tag.TAG_COMPOUND).copy();
      var prospectStorage = prospect.getOrCreateTag().getList("SpatialStorage", Tag.TAG_COMPOUND);
      // Keep both source containers, without merging/truncating slots or materializing copies.
      tag.put("ProspectorStorage", prospectStorage.copy());
      tag.put("SpatialStorage", storage);
      var result = HeavenlyRestriction.HEPHAESTUS.get().createDefaultInstance();
      result.setMastery(0);
      result.setParentSkill(UniqueSkills.GODLY_CRAFTSMAN.get());
      result.getOrCreateTag().putBoolean("NoMagiculeCost", true);
      try (var provenance =
          ChainedContext.ability(
              p,
              ChainedState.inherited(p, crafts.getSkill())
                  ? crafts.getSkill()
                  : prospect.getSkill())) {
        if (!SkillHelper.learnSkill(p, result)) return false;
      }
      var actual = owned(p).orElseThrow();
      actual.setTag(tag);
      actual.setToggled(prospect.isToggled());
      actual.setParentSkill(null);
      if (ChainedState.inherited(p, prospect.getSkill())
          || ChainedState.inherited(p, crafts.getSkill()))
        ChainedState.remember(p, actual.getSkill());
      double price = cost(p);
      var attr = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
      attr.setBaseValue(attr.getBaseValue() - price);
      var energy = TensuraStorages.getExistenceFrom(p);
      energy.setMagicule(Math.max(0, Math.min(energy.getMagicule() - price, attr.getValue())));
      energy.markDirty();
      var slots = new ArrayList<int[]>();
      var ability = TensuraStorages.getAbilityFrom(p);
      for (int preset = 0; preset < 9; preset++)
        for (int slot = 0; slot < 3; slot++) {
          var s = ability.getAbilitySlot(preset, slot).getSkill();
          if (s == prospect.getSkill() || s == crafts.getSkill())
            slots.add(new int[] {preset, slot});
        }
      skills.forgetSkill(prospect);
      skills.forgetSkill(crafts);
      for (var slot : slots) ability.setAbilitySlot(slot[0], slot[1], actual.getSkill(), 0);
      ability.markDirty();
      skills.checkAndMarkDirty(actual);
      ((io.github.manasmods.manascore.storage.api.StorageHolder) p).manasCore$sync();
      return true;
    } finally {
      BUSY.remove(p.getUUID());
    }
  }

  private ForgeSynthesis() {}
}
