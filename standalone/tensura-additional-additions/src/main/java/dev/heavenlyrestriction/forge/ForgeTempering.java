package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.critical.CriticalMerchant;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import java.util.function.BooleanSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
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
    if (owner == null || target.level().isClientSide() || !ForgeItems.forged(weapon(owner, source)))
      return operation.getAsBoolean();
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

  public static void incoming(LivingIncomingDamageEvent e) {
    var owner = RestrictionData.attacker(e.getSource());
    if (owner == null) return;
    var item = weapon(owner, e.getSource());
    if (ForgeItems.temper(item) == ForgeItems.Temper.PIERCING) {
      float retained = (float) (1 - ForgeItems.strength(item));
      e.addReductionModifier(
          DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * retained);
    }
  }

  public static void damage(LivingDamageEvent.Pre e) {
    if (e.getNewDamage() <= 0) return;
    var owner = RestrictionData.attacker(e.getSource());
    if (owner == null) return;
    var item = weapon(owner, e.getSource());
    var temper = ForgeItems.temper(item);
    double quality = ForgeItems.strength(item);
    if (quality <= 0) return;
    switch (temper) {
      case KEEN -> {
        boolean critical =
            UnboundCritical.meleeCritical(owner, e.getEntity(), e.getSource())
                || UnboundCritical.enabled(owner)
                || CriticalMerchant.criticalOrGuaranteed(owner, e.getEntity(), e.getSource());
        for (var h = CURRENT.get(); h != null; h = h.previous)
          if (h.owner == owner && h.target == e.getEntity() && h.source == e.getSource())
            critical |= h.critical[0];
        if (critical)
          e.setNewDamage((float) Math.min(Float.MAX_VALUE, e.getNewDamage() * (1 + quality)));
      }
      case CONDUCTOR -> {
        var element = ((TensuraDamageSource) e.getSource()).tensura$getElement();
        if (element != null && element.isElemental())
          e.setNewDamage((float) Math.min(Float.MAX_VALUE, e.getNewDamage() * (1 + quality)));
      }
      default -> {}
    }
  }

  private ForgeTempering() {}
}
