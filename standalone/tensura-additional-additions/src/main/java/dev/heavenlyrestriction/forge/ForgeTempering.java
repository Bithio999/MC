package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.critical.CriticalMerchant;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import java.util.function.BooleanSupplier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

public final class ForgeTempering {
  private record Hit(
      Hit previous,
      LivingEntity owner,
      LivingEntity target,
      DamageSource source,
      boolean[] critical) {}

  private static final ThreadLocal<Hit> CURRENT = new ThreadLocal<>();

  public static void melee(CriticalHitEvent e) {
    if (e.isCriticalHit()) UnboundCritical.noteCritical(e.getEntity(), e.getTarget());
  }

  public static boolean observe(
      LivingEntity target, DamageSource source, BooleanSupplier operation) {
    var owner = RestrictionData.attacker(source);
    if (owner == null
        || target.level().isClientSide()
        || !ForgeItems.forged(weapon(owner, source))
            && armorPower(owner, ForgeItems.Temper.KEEN) <= 0) return operation.getAsBoolean();
    var h = new Hit(CURRENT.get(), owner, target, source, new boolean[] {false});
    CURRENT.set(h);
    try {
      return operation.getAsBoolean();
    } finally {
      if (h.previous == null) CURRENT.remove();
      else CURRENT.set(h.previous);
    }
  }

  public static void critical(Entity target, Entity owner) {
    for (var h = CURRENT.get(); h != null; h = h.previous)
      if (h.owner == owner && h.target == target) {
        h.critical[0] = true;
        break;
      }
  }

  private static ItemStack weapon(LivingEntity owner, DamageSource source) {
    var item = source.getWeaponItem();
    return item == null || item.isEmpty() ? ForgeArsenal.weapon(owner) : item;
  }

  public static double armorPower(LivingEntity wearer, ForgeItems.Temper temper) {
    if (wearer == null) return 0;
    double power = 0;
    for (var slot :
        new EquipmentSlot[] {
          EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
      var item = wearer.getItemBySlot(slot);
      if (item.getItem() instanceof ArmorItem armor
          && armor.getEquipmentSlot() == slot
          && ForgeItems.forged(item)
          && ForgeItems.temper(item) == temper)
        power += ForgeItems.strength(item) * ForgeItems.armorWeight(item);
    }
    return Math.clamp(power, 0, .35);
  }

  private static double weaponPower(
      LivingEntity owner, DamageSource source, ForgeItems.Temper temper) {
    if (owner == null) return 0;
    var item = weapon(owner, source);
    // Armor carried in the hand cannot activate its worn or weapon effect.
    return ForgeItems.forged(item) && !ForgeItems.armor(item) && ForgeItems.temper(item) == temper
        ? ForgeItems.strength(item)
        : 0;
  }

  private static boolean isCritical(LivingEntity owner, LivingEntity target, DamageSource source) {
    if (owner == null) return false;
    boolean critical =
        UnboundCritical.meleeCritical(owner, target, source)
            || UnboundCritical.enabled(owner)
            || CriticalMerchant.criticalOrGuaranteed(owner, target, source);
    for (var hit = CURRENT.get(); hit != null; hit = hit.previous)
      if (hit.owner == owner && hit.target == target && hit.source == source)
        critical |= hit.critical[0];
    return critical;
  }

  public static boolean elemental(DamageSource source) {
    var element = ((TensuraDamageSource) source).tensura$getElement();
    return element != null && element.isElemental()
        || TensuraDamageHelper.isEarthDamage(source)
        || TensuraDamageHelper.isFireDamage(source)
        || TensuraDamageHelper.isWaterDamage(source)
        || TensuraDamageHelper.isWindDamage(source)
        || TensuraDamageHelper.isLightDamage(source)
        || TensuraDamageHelper.isDarkDamage(source)
        || TensuraDamageHelper.isSpatialDamage(source)
        || TensuraDamageHelper.isLightningDamage(source)
        || TensuraDamageHelper.isCold(source);
  }

  public static void incoming(LivingIncomingDamageEvent event) {
    var owner = RestrictionData.attacker(event.getSource());
    if (owner == null) return;
    double piercing =
        weaponPower(owner, event.getSource(), ForgeItems.Temper.PIERCING)
            + armorPower(owner, ForgeItems.Temper.PIERCING);
    if (piercing > 0) {
      float retained = (float) (1 - piercing);
      event.addReductionModifier(
          DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * retained);
    }
  }

  private static float modified(LivingEntity target, DamageSource source, float amount) {
    if (amount <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
    var owner = RestrictionData.attacker(source);
    double factor = 1;
    if (isCritical(owner, target, source))
      factor +=
          weaponPower(owner, source, ForgeItems.Temper.KEEN)
              + armorPower(owner, ForgeItems.Temper.KEEN);
    var element = ((TensuraDamageSource) source).tensura$getElement();
    // Preserve the existing weapon Conductor definition; its armor counterpart is defensive.
    if (element != null && element.isElemental())
      factor += weaponPower(owner, source, ForgeItems.Temper.CONDUCTOR);
    double protection = armorPower(target, ForgeItems.Temper.CONDUCTOR);
    if (protection > 0 && elemental(source)) factor *= 1 - protection;
    return (float) Math.min(Float.MAX_VALUE, amount * factor);
  }

  public static void damage(LivingDamageEvent.Pre event) {
    event.setNewDamage(modified(event.getEntity(), event.getSource(), event.getNewDamage()));
  }

  /** Native direct SHP hits skip NeoForge's ordinary HP damage events. */
  public static void spiritual(
      LivingEntity target,
      DamageSource source,
      io.github.manasmods.manascore.network.api.util.Changeable<Float> damage) {
    damage.set(modified(target, source, damage.get()));
  }

  private ForgeTempering() {}
}
