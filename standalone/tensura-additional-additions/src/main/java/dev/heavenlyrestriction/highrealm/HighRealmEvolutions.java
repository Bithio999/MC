package dev.heavenlyrestriction.highrealm;

import io.github.manasmods.tensura.entity.monster.*;
import io.github.manasmods.tensura.entity.variant.DirewolfVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;

/** Native non-boss naming evolutions, applied once without acquiring an owner or true name. */
public final class HighRealmEvolutions {
  public static final String KEY = "heavenlyrestriction:high_realm_evolution";

  public static boolean apply(Mob mob) {
    if (mob.getPersistentData().getBoolean(KEY) || !HighRealmElites.unowned(mob)) return false;
    if (mob instanceof net.minecraft.world.entity.AgeableMob ageable) ageable.setAge(0);
    if (mob instanceof DirewolfEntity wolf) {
      wolf.setStar(true);
      if (wolf.getCurrentEvolutionState() < wolf.getMaxEvolutionState())
        wolf.setVariant(DirewolfVariant.STAR_WOLF);
      while (wolf.getCurrentEvolutionState() < wolf.getMaxEvolutionState()) wolf.evolve();
      wolf.setVariant(DirewolfVariant.TEMPEST_STAR_WOLF);
    } else if (mob instanceof LizardmanEntity lizard) {
      while (lizard.getCurrentEvolutionState() < lizard.getMaxEvolutionState()) lizard.evolve();
    } else if (mob instanceof OrcEntity orc
        && mob.getType()
            == io.github.manasmods.tensura.registry.entity.MonsterEntityTypes.ORC.get()) {
      // Orc.evolve supplies native buffs but does not increment its saved state itself.
      if (orc.getCurrentEvolutionState() < orc.getMaxEvolutionState()) {
        orc.evolve();
        orc.setCurrentEvolutionState(orc.getMaxEvolutionState());
      }
    }
    mob.refreshDimensions();
    mob.getPersistentData().putBoolean(KEY, true);
    return true;
  }

  public static Component name(Mob mob) {
    if (mob instanceof DirewolfEntity)
      return Component.translatable("high_realm.heavenlyrestriction.tempest_star_wolf");
    if (mob instanceof LizardmanEntity)
      return Component.translatable("high_realm.heavenlyrestriction.dragonewt");
    if (mob.getType() == io.github.manasmods.tensura.registry.entity.MonsterEntityTypes.ORC.get())
      return Component.translatable("high_realm.heavenlyrestriction.high_orc");
    return mob.getType().getDescription();
  }

  public static void refreshName(Mob mob) {
    var k = HighRealmElites.kind(mob);
    String title =
        k == HighRealmElites.Kind.BULWARK
            ? "High Bulwark "
            : k == HighRealmElites.Kind.SENTINEL ? "High Sentinel " : "High Stalker ";
    mob.setCustomName(
        Component.literal(title).append(name(mob)).withStyle(net.minecraft.ChatFormatting.GOLD));
  }

  private HighRealmEvolutions() {}
}
