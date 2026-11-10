package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.chromatic.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.subclass.IResearcherEnchanter;
import io.github.manasmods.tensura.data.TensuraTags;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;

/** Research may reproduce an overcap only through Hephaestus, and never reset its Orb allowance. */
public final class ForgeEngravingPolicy {
  public static boolean godly(ManasSkill skill) {
    return skill == UniqueSkills.GODLY_CRAFTSMAN.get();
  }

  public static boolean governed(ManasSkill skill) {
    return godly(skill) || skill == HeavenlyRestriction.HEPHAESTUS.get();
  }

  public static boolean overcap(Holder<Enchantment> enchantment, int level) {
    return enchantment.is(TensuraTags.Enchantments.ENGRAVING)
        && level > enchantment.value().getMaxLevel();
  }

  public static ItemEnchantments filterResearch(ItemEnchantments source) {
    var accepted = new ItemEnchantments.Mutable(source);
    accepted.removeIf(e -> overcap(e, source.getLevel(e)));
    return accepted.toImmutable();
  }

  /** Clamp old saved Godly Craftsman knowledge, without changing equipment or the other skills. */
  public static ItemEnchantments knowledge(
      LivingEntity owner, ManasSkillInstance instance, ItemEnchantments source) {
    if (!godly(instance.getSkill())) return source;
    var clean = new ItemEnchantments.Mutable(source);
    boolean changed = false;
    for (var entry : source.entrySet())
      if (overcap(entry.getKey(), entry.getIntValue())) {
        clean.set(entry.getKey(), entry.getKey().value().getMaxLevel());
        changed = true;
      }
    if (!changed) return source;
    var result = clean.toImmutable();
    if (!owner.level().isClientSide()) {
      instance
          .getOrCreateTag()
          .put(
              IResearcherEnchanter.STORED_ENCHANTMENTS,
              IResearcherEnchanter.getEnchantmentsTag(result, owner.registryAccess()));
      instance.markDirty();
      SkillAPI.getSkillsFrom(owner).markDirty();
    }
    return result;
  }

  /**
   * Keep an existing item's legal overcap, but never transfer a higher one through Godly Craftsman.
   */
  public static ItemEnchantments application(
      ManasSkill skill, ItemStack input, ItemEnchantments selected) {
    if (!godly(skill)) return selected;
    var original = EnchantmentHelper.getEnchantmentsForCrafting(input);
    var safe = new ItemEnchantments.Mutable(selected);
    for (var entry : selected.entrySet())
      if (overcap(entry.getKey(), entry.getIntValue()))
        safe.set(
            entry.getKey(),
            Math.min(
                entry.getIntValue(),
                Math.max(entry.getKey().value().getMaxLevel(), original.getLevel(entry.getKey()))));
    return safe.toImmutable();
  }

  public static void markAllowance(ManasSkill skill, ItemStack result) {
    if (governed(skill)
        && EnchantmentHelper.getEnchantmentsForCrafting(result).entrySet().stream()
            .anyMatch(
                e -> ChromaticOrb.eligible(e.getKey()) && overcap(e.getKey(), e.getIntValue())))
      result.set(ChromaticContent.INFUSED.get(), true);
  }

  public static void blocked(LivingEntity owner) {
    if (owner instanceof ServerPlayer player)
      player.displayClientMessage(
          Component.translatable("heavenlyrestriction.forge.research_overcap")
              .withStyle(ChatFormatting.RED),
          true);
  }

  private ForgeEngravingPolicy() {}
}
