package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.mixin.ForgeGearEPAccess;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

/** Redirect only native gear-growth reads/writes; never physically equip an escrowed weapon. */
public final class ForgeGearGrowth {
  private static final ThreadLocal<View> CURRENT = new ThreadLocal<>();

  private static final class View {
    final LivingEntity owner;
    ItemStack item;

    View(LivingEntity owner, ItemStack item) {
      this.owner = owner;
      this.item = item;
    }
  }

  public static boolean active(LivingEntity owner) {
    var view = CURRENT.get();
    return view != null && view.owner == owner;
  }

  public static ItemStack get(LivingEntity owner, EquipmentSlot slot) {
    var view = CURRENT.get();
    return view != null && view.owner == owner && slot == EquipmentSlot.MAINHAND ? view.item : null;
  }

  public static boolean replace(LivingEntity owner, EquipmentSlot slot, ItemStack replacement) {
    var view = CURRENT.get();
    if (view == null || view.owner != owner || slot != EquipmentSlot.MAINHAND) return false;
    view.item = replacement;
    return true;
  }

  public static ItemStack award(LivingEntity owner, ItemStack item, double ep) {
    var previous = CURRENT.get();
    var view = new View(owner, item);
    CURRENT.set(view);
    try {
      ForgeGearEPAccess.forge$nativeGrowth(owner, EquipmentSlot.MAINHAND, ep);
      return view.item;
    } finally {
      if (previous == null) CURRENT.remove();
      else CURRENT.set(previous);
    }
  }

  private ForgeGearGrowth() {}
}
