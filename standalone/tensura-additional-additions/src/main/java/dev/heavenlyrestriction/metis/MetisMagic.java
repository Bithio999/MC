package dev.heavenlyrestriction.metis;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.magic.*;
import io.github.manasmods.tensura.ability.magic.aspectual.AspectualMagic;
import io.github.manasmods.tensura.ability.magic.spiritual.SpiritualMagic;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.effect.template.TensuraMobEffectInstance;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Analysis is read-only: native resistance checks are inspected, never weakened or removed. */
public final class MetisMagic {
  public enum Role {
    HEALING,
    SUPPORT,
    OFFENSE
  }

  public static Role role(ManasSkillInstance i) {
    return role(i, 0);
  }

  public static Role role(ManasSkillInstance i, int mode) {
    if (!(i.getSkill() instanceof Magic)
        && i.getSkill() instanceof io.github.manasmods.tensura.ability.TensuraSkill ability) {
      String id = ability.getModeId(i, mode).toLowerCase(Locale.ROOT);
      String action = id.substring(id.lastIndexOf('.') + 1);
      if (Set.of("heal", "healing", "self_healing", "self_recovery", "recover", "recovery")
          .contains(action)) return Role.HEALING;
      if (Set.of(
              "strength",
              "agility",
              "aura_sword",
              "physical_barrier",
              "magical_barrier",
              "multilayer_barrier",
              "magic_barrier",
              "armor",
              "enhance",
              "defense",
              "dividend",
              "guaranteed_profit")
          .contains(action)) return Role.SUPPORT;
    }
    if (i.getSkill() instanceof AspectualMagic a)
      return switch (a.getAspectualType()) {
        case RECOVERY -> Role.HEALING;
        case ENHANCEMENT, BARRIER -> Role.SUPPORT;
        default -> Role.OFFENSE;
      };
    return Role.OFFENSE;
  }

  public static boolean needed(ServerPlayer p, ManasSkillInstance i) {
    return needed(p, i, 0);
  }

  public static boolean needed(ServerPlayer p, ManasSkillInstance i, int mode) {
    if (role(i, mode) == Role.OFFENSE) return true;
    if (i.getSkillId().getPath().equals("critical_merchant")
        || i.getSkillId().getPath().equals("tyche"))
      return dev.heavenlyrestriction.critical.CriticalMerchant.remaining(i) == 0;
    String path = i.getSkillId().getPath();
    if (role(i, mode) == Role.HEALING) {
      if (Set.of("antidote").contains(path))
        return p.getActiveEffects().stream().anyMatch(e -> !e.getEffect().value().isBeneficial());
      return p.getHealth() < p.getMaxHealth() * .9
          || i.getSkillId().getPath().equals("healer")
              && i.isMastered(p)
              && io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(p)
                      .getSpiritualHealth()
                  < p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH) * .9;
    }
    if (path.equals("strength") && p.hasEffect(MobEffects.DAMAGE_BOOST)) return false;
    if (path.equals("agility")
        && p.hasEffect(MobEffects.MOVEMENT_SPEED)
        && p.hasEffect(MobEffects.DIG_SPEED)) return false;
    return p.getActiveEffects().stream()
        .noneMatch(
            e -> {
              var source = ((TensuraMobEffectInstance) e).tensura$getSourceAbility();
              return source != null && source.equals(i.getSkillId().toString());
            });
  }

  public static ResourceKey<DamageType> damageType(ManasSkillInstance i) {
    if (i.getSkill() instanceof AspectualMagic a)
      return switch (a.getAspectualType()) {
        case EARTH -> TensuraDamageTypes.EARTH_ELEMENTAL;
        case FIRE -> TensuraDamageTypes.FIRE_ELEMENTAL;
        case ICE -> TensuraDamageTypes.ICE_ELEMENTAL;
        case LIGHTNING -> TensuraDamageTypes.LIGHTNING_ELEMENTAL;
        case GRAVITY -> TensuraDamageTypes.GRAVITY_ELEMENTAL;
        case SPACE -> TensuraDamageTypes.SPACE_ELEMENTAL;
        case WATER -> TensuraDamageTypes.WATER_ELEMENTAL;
        case WIND -> TensuraDamageTypes.WIND_ELEMENTAL;
        default -> TensuraDamageTypes.MAGIC_GENERIC;
      };
    if (i.getSkill() instanceof SpiritualMagic s && s.getElemental().getDefaultDamage() != null)
      return s.getElemental().getDefaultDamage();
    return TensuraDamageTypes.MAGIC_GENERIC;
  }

  private static Holder<Attribute> elemental(ManasSkillInstance i) {
    var type = damageType(i);
    if (type == TensuraDamageTypes.EARTH_ELEMENTAL) return TensuraAttributes.EARTH_RESISTANCE;
    if (type == TensuraDamageTypes.FIRE_ELEMENTAL) return TensuraAttributes.FLAME_RESISTANCE;
    if (type == TensuraDamageTypes.WATER_ELEMENTAL || type == TensuraDamageTypes.ICE_ELEMENTAL)
      return TensuraAttributes.WATER_RESISTANCE;
    if (type == TensuraDamageTypes.WIND_ELEMENTAL || type == TensuraDamageTypes.LIGHTNING_ELEMENTAL)
      return TensuraAttributes.WIND_RESISTANCE;
    if (type == TensuraDamageTypes.SPACE_ELEMENTAL) return TensuraAttributes.SPACE_RESISTANCE;
    if (type == TensuraDamageTypes.LIGHT_ELEMENTAL) return TensuraAttributes.LIGHT_RESISTANCE;
    if (type == TensuraDamageTypes.DARKNESS_ELEMENTAL) return TensuraAttributes.DARKNESS_RESISTANCE;
    return null;
  }

  public static double effectiveness(
      ServerPlayer p, LivingEntity target, ManasSkillInstance spell, int mode) {
    if (!(spell.getSkill() instanceof Magic))
      return 1; // Unknown skill damage types are never guessed.
    var source = ((Magic) spell.getSkill()).createSource(spell, p, damageType(spell), mode);
    double score = 1;
    boolean resistant = false;
    for (var i : SkillAPI.getSkillsFrom(target).getLearnedSkills()) {
      if (i.getMastery() < 0
          || !i.isToggled()
          || !i.canInteractSkill(target)
          || !(i.getSkill() instanceof ResistSkill r)
          || !r.isDamageResisted(target, source, i)) continue;
      if (r.getResistType() == ResistSkill.ResistType.NULLIFICATION
          && !ResistSkill.isNullificationDisabled(target.level())) return 0;
      if (r.getResistType() == ResistSkill.ResistType.RESISTANCE) resistant = true;
    }
    if (resistant) score *= Math.max(0, ResistSkill.CONFIG.resistanceDamageMultiplier);
    if (TensuraDamageHelper.isTensuraMagic(source)) {
      if (target.getAttribute(TensuraAttributes.MAGIC_RESISTANCE) != null)
        score *= Math.max(0, 1 - target.getAttributeValue(TensuraAttributes.MAGIC_RESISTANCE));
    } else {
      var a = elemental(spell);
      if (a != null && target.getAttribute(a) != null)
        score *= Math.max(0, 1 - target.getAttributeValue(a));
    }
    return Double.isFinite(score) ? Math.clamp(score, 0, 1024) : 0;
  }

  public static void record(ServerPlayer p, LivingEntity target, ManasSkillInstance mind) {
    var records = mind.getOrCreateTag().getList("MetisDossiers", Tag.TAG_COMPOUND).copy();
    records.removeIf(
        t ->
            ((CompoundTag) t).hasUUID("target")
                && ((CompoundTag) t).getUUID("target").equals(target.getUUID()));
    while (records.size() >= 32) records.remove(0);
    var entry = new CompoundTag();
    entry.putUUID("target", target.getUUID());
    entry.putString("name", target.getName().getString());
    var scores = new ListTag();
    for (var slot : MetisMatrix.slots(p, mind)) {
      var spell = SkillAPI.getSkillsFrom(p).getSkill(slot.id()).orElseThrow();
      if (!(spell.getSkill() instanceof Magic) || role(spell, slot.mode()) != Role.OFFENSE)
        continue;
      var score = new CompoundTag();
      score.putString("id", slot.id().toString());
      score.putDouble("effectiveness", effectiveness(p, target, spell, slot.mode()));
      scores.add(score);
    }
    entry.put("spells", scores);
    if (mind.getOrCreateTag().getList("MetisDossiers", Tag.TAG_COMPOUND).stream()
        .anyMatch(entry::equals)) return;
    records.add(entry);
    mind.getOrCreateTag().put("MetisDossiers", records);
    mind.markDirty();
  }

  private MetisMagic() {}
}
