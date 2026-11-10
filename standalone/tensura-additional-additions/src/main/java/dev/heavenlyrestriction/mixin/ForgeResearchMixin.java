package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.forge.ForgeEngravingPolicy;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.subclass.IResearcherEnchanter;
import java.util.ArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = IResearcherEnchanter.class, remap = false)
public interface ForgeResearchMixin {
  @WrapMethod(
      method =
          "addEnchantments(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/ItemEnchantments$Mutable;Lio/github/manasmods/manascore/skill/api/ManasSkill;)Z")
  private static boolean forge$research(
      LivingEntity owner,
      ItemEnchantments.Mutable source,
      ManasSkill skill,
      Operation<Boolean> original) {
    if (!ForgeEngravingPolicy.godly(skill)) return original.call(owner, source, skill);
    var before = source.toImmutable();
    var filtered = ForgeEngravingPolicy.filterResearch(before);
    if (filtered.equals(before)) return original.call(owner, source, skill);
    ForgeEngravingPolicy.blocked(owner);
    var accepted = new ItemEnchantments.Mutable(filtered);
    boolean learned = original.call(owner, accepted, skill);
    // Reflect native consumption only for accepted entries; rejected overcaps stay on the donor.
    for (var key : new ArrayList<>(filtered.keySet()))
      if (accepted.getLevel(key) == 0) source.removeIf(e -> e.equals(key));
    return learned;
  }

  @WrapMethod(
      method =
          "getAllEnchantments(Lnet/minecraft/world/entity/LivingEntity;Lio/github/manasmods/manascore/skill/api/ManasSkillInstance;)Lnet/minecraft/world/item/enchantment/ItemEnchantments;")
  private static ItemEnchantments forge$legacy(
      LivingEntity owner, ManasSkillInstance skill, Operation<ItemEnchantments> original) {
    return ForgeEngravingPolicy.knowledge(owner, skill, original.call(owner, skill));
  }
}
