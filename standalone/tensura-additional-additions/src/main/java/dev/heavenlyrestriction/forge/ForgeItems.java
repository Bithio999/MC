package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.*;
import io.github.manasmods.tensura.registry.item.misc.TensuraDataComponents;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ForgeItems {
  public enum Temper {
    NONE,
    KEEN,
    PIERCING,
    CONDUCTOR,
    ENDURING
  }

  public static CompoundTag data(ItemStack s) {
    return s.getOrDefault(ForgeContent.WORK.get(), new CompoundTag());
  }

  public static boolean equipment(ItemStack s) {
    return !s.isEmpty()
        && s.getCount() == 1
        && (s.isDamageableItem() || s.has(TensuraDataComponents.MAX_EP.get()));
  }

  public static boolean weapon(ItemStack s) {
    return !s.isEmpty()
        && (s.getItem() instanceof SwordItem
            || s.getItem() instanceof AxeItem
            || s.getItem() instanceof TridentItem);
  }

  public static boolean forged(ItemStack s) {
    return data(s).hasUUID("smith") && data(s).hasUUID("work");
  }

  public static boolean own(ServerPlayer p, ItemStack s) {
    return forged(s) && data(s).getUUID("smith").equals(p.getUUID());
  }

  public static boolean stamp(ServerPlayer p, ItemStack s) {
    if (!equipment(s) || forged(s)) return false;
    var t = new CompoundTag();
    t.putUUID("smith", p.getUUID());
    t.putUUID("work", UUID.randomUUID());
    t.putBoolean("benefits", ForgeSynthesis.active(p).isPresent());
    s.set(ForgeContent.WORK.get(), t);
    return true;
  }

  public static boolean masterpiece(ItemStack s) {
    if (!forged(s)) return false;
    var t = data(s);
    UUID owner = t.getUUID("smith"), work = t.getUUID("work");
    if (EffectiveSide.get().isServer()) {
      var server = ServerLifecycleHooks.getCurrentServer();
      return server != null && work.equals(ForgeLedger.get(server).works.get(owner));
    }
    return work.equals(ForgeNetwork.clientWorks.get(owner));
  }

  public static double statBonus(ItemStack s) {
    return !forged(s) ? 0 : masterpiece(s) ? .5 : data(s).getBoolean("benefits") ? .2 : 0;
  }

  public static Temper temper(ItemStack s) {
    return Temper.values()[Math.clamp(data(s).getInt("temper"), 0, Temper.values().length - 1)];
  }

  public static double strength(ItemStack s) {
    return Math.clamp(data(s).getDouble("quality"), 0, .35);
  }

  public static double wearMultiplier(ItemStack s) {
    if (!forged(s)) return 1;
    double base = masterpiece(s) ? .25 : data(s).getBoolean("benefits") ? .5 : 1;
    return base * (temper(s) == Temper.ENDURING ? 1 - strength(s) : 1);
  }

  /** Fractional wear is carried forward instead of rounding every one-point hit down to zero. */
  public static int wear(ItemStack s, int amount) {
    if (amount <= 0 || !forged(s)) return amount;
    var t = data(s).copy();
    double pending = amount * wearMultiplier(s) + t.getDouble("wearFraction");
    int take = (int) Math.min(Integer.MAX_VALUE, Math.floor(pending + 1e-9));
    t.putDouble("wearFraction", Math.max(0, pending - take));
    s.set(ForgeContent.WORK.get(), t);
    return take;
  }

  public static double spiritualWear(ItemStack s, double old, double next) {
    return next < old ? old - (old - next) * wearMultiplier(s) : next;
  }

  public static void attributes(ItemAttributeModifierEvent e) {
    var s = e.getItemStack();
    double bonus = statBonus(s);
    if (bonus <= 0) return;
    record Key(Holder<Attribute> attribute, EquipmentSlotGroup slot) {}
    var additions = new LinkedHashMap<Key, Double>();
    for (var entry : e.getModifiers())
      if (entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE
          && !entry.modifier().id().getPath().startsWith("forge_stat_")
          && (entry.attribute().equals(Attributes.ATTACK_DAMAGE)
              || entry.attribute().equals(Attributes.ARMOR)
              || entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)))
        additions.merge(
            new Key(entry.attribute(), entry.slot()), entry.modifier().amount(), Double::sum);
    additions.forEach(
        (key, value) -> {
          if (value > 0)
            e.addModifier(
                key.attribute(),
                new AttributeModifier(
                    HeavenlyRestriction.id(
                        "forge_stat_"
                            + key.slot().getSerializedName()
                            + "_"
                            + key.attribute().unwrapKey().orElseThrow().location().getPath()),
                    (value
                            + (key.attribute().equals(Attributes.ATTACK_DAMAGE)
                                    && key.slot() == EquipmentSlotGroup.MAINHAND
                                ? 1
                                : 0))
                        * bonus,
                    AttributeModifier.Operation.ADD_VALUE),
                key.slot());
        });
  }

  public static void crafted(PlayerEvent.ItemCraftedEvent event) {
    craftedBy(event.getCrafting(), event.getEntity());
  }

  public static void prepare(ItemStack s, net.minecraft.world.entity.player.Player player) {
    if (player instanceof ServerPlayer p
        && (ForgeSynthesis.hasCraftsman(p) || ForgeSynthesis.active(p).isPresent())) stamp(p, s);
  }

  public static void craftedBy(ItemStack s, net.minecraft.world.entity.player.Player player) {
    if (!(player instanceof ServerPlayer p)
        || !ForgeSynthesis.hasCraftsman(p) && ForgeSynthesis.active(p).isEmpty()
        || !equipment(s)) return;
    stamp(p, s);
    if (s.is(ForgeSynthesis.QUALIFYING)) {
      RestrictionData.of(p).putBoolean("hephaestus_forged_high_tier", true);
    }
    ForgeSynthesis.discover(p);
  }

  public static void reconcile(ItemStack s) {
    if (!forged(s)) return;
    boolean active = masterpiece(s);
    if (data(s).getBoolean("activeMasterpiece") != active) {
      var t = data(s).copy();
      t.putBoolean("activeMasterpiece", active);
      s.set(ForgeContent.WORK.get(), t);
    }
  }

  public static void tooltip(ItemTooltipEvent e) {
    var s = e.getItemStack();
    if (!forged(s)) return;
    boolean opus = masterpiece(s);
    double bonus = statBonus(s);
    e.getToolTip()
        .add(
            Component.translatable(
                    opus ? "heavenlyrestriction.forge.opus" : "heavenlyrestriction.forge.signed")
                .withStyle(opus ? ChatFormatting.GOLD : ChatFormatting.GRAY));
    if (bonus > 0)
      e.getToolTip()
          .add(
              Component.translatable("heavenlyrestriction.forge.stats", Math.round(bonus * 100))
                  .withStyle(ChatFormatting.GRAY));
    if (wearMultiplier(s) < 1)
      e.getToolTip()
          .add(
              Component.translatable(
                      "heavenlyrestriction.forge.wear", Math.round((1 - wearMultiplier(s)) * 100))
                  .withStyle(ChatFormatting.GRAY));
    if (temper(s) != Temper.NONE)
      e.getToolTip()
          .add(
              Component.translatable(
                      "heavenlyrestriction.forge.temper",
                      temper(s).name(),
                      Math.round(strength(s) * 100))
                  .withStyle(ChatFormatting.YELLOW));
  }

  public static double weaponDamage(
      ItemStack s,
      LivingEntity owner,
      LivingEntity target,
      net.minecraft.world.damagesource.DamageSource source) {
    double[] m = {0, 0, 1};
    s.forEachModifier(
        EquipmentSlot.MAINHAND,
        (a, v) -> {
          if (a.equals(Attributes.ATTACK_DAMAGE))
            switch (v.operation()) {
              case ADD_VALUE -> m[0] += v.amount();
              case ADD_MULTIPLIED_BASE -> m[1] += v.amount();
              case ADD_MULTIPLIED_TOTAL -> m[2] *= 1 + v.amount();
            }
        });
    float raw = (float) Math.max(0, (1 + m[0]) * (1 + m[1]) * m[2]);
    return owner.level() instanceof net.minecraft.server.level.ServerLevel l
        ? EnchantmentHelper.modifyDamage(l, s, target, source, raw)
        : raw;
  }

  private ForgeItems() {}
}
