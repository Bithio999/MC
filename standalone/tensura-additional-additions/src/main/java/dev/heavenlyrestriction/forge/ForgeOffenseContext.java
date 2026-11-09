package dev.heavenlyrestriction.forge;

import io.github.manasmods.manascore.attribute.api.ManasCoreAttributes;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;

/**
 * Temporarily equip only native offensive engraving attributes; never touch capacities or movement.
 */
public final class ForgeOffenseContext implements AutoCloseable {
  private record Key(AttributeInstance attribute, ResourceLocation id) {}

  private record Old(AttributeModifier modifier, boolean permanent) {}

  private final Map<Key, Old> previous = new LinkedHashMap<>();
  private static final Set<Holder<Attribute>> OFFENSE =
      Set.of(
          ManasCoreAttributes.CRITICAL_ATTACK_CHANCE,
          ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER,
          TensuraAttributes.EARTH_BOOST,
          TensuraAttributes.DARKNESS_BOOST,
          TensuraAttributes.FLAME_BOOST,
          TensuraAttributes.LIGHT_BOOST,
          TensuraAttributes.SPACE_BOOST,
          TensuraAttributes.WATER_BOOST,
          TensuraAttributes.WIND_BOOST,
          TensuraAttributes.GRAVITY_BOOST,
          TensuraAttributes.LIGHTNING_BOOST,
          TensuraAttributes.ILLUSION_BOOST,
          TensuraAttributes.SOUND_BOOST,
          Attributes.ATTACK_KNOCKBACK);

  public static ForgeOffenseContext open(LivingEntity owner, ItemStack weapon) {
    var context = new ForgeOffenseContext();
    owner
        .getMainHandItem()
        .forEachModifier(EquipmentSlot.MAINHAND, (a, m) -> context.change(owner, a, m, false));
    weapon.forEachModifier(EquipmentSlot.MAINHAND, (a, m) -> context.change(owner, a, m, true));
    return context;
  }

  private void change(
      LivingEntity owner, Holder<Attribute> holder, AttributeModifier modifier, boolean equip) {
    if (!OFFENSE.contains(holder)) return;
    var attribute = owner.getAttribute(holder);
    if (attribute == null) return;
    var key = new Key(attribute, modifier.id());
    if (!previous.containsKey(key)) {
      var old = attribute.getModifier(modifier.id());
      previous.put(
          key,
          new Old(
              old,
              old != null
                  && attribute.save().getList("modifiers", 10).stream()
                      .anyMatch(
                          t ->
                              t instanceof net.minecraft.nbt.CompoundTag n
                                  && n.getString("id").equals(old.id().toString()))));
    }
    attribute.removeModifier(modifier.id());
    if (equip) attribute.addTransientModifier(modifier);
  }

  @Override
  public void close() {
    for (var e : previous.entrySet()) {
      e.getKey().attribute.removeModifier(e.getKey().id);
      var old = e.getValue();
      if (old.modifier != null) {
        if (old.permanent) e.getKey().attribute.addPermanentModifier(old.modifier);
        else e.getKey().attribute.addTransientModifier(old.modifier);
      }
    }
  }

  private ForgeOffenseContext() {}
}
