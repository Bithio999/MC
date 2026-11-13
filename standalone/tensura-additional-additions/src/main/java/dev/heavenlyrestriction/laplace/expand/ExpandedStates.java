package dev.heavenlyrestriction.laplace.expand;

import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.world.entity.LivingEntity;

/** End unpaid sustained states and refresh slot modifiers when the formula provider stops. */
public final class ExpandedStates {
  public static boolean requiresStop(LivingEntity owner, LaplaceActions.Action action) {
    if (action.skill().getSkill()
        == io.github.manasmods.tensura.registry.skill.CommonSkills.SELF_REGENERATION.get())
      return action.skill().isToggled();
    if (action.mode() < 0 && action.skill().isToggled()) return true;
    if (action.skill().getSkillId().getNamespace().equals("tensura")
        && action.mode() == 0
        && action.skill().isToggled()) return true;
    String id = action.skill().getSkillId().getPath();
    return id.equals("fusion") && dev.heavenlyrestriction.fusion.Fusion.session(owner) != null
        || id.equals("unbound")
            && action.mode() == 1
            && dev.heavenlyrestriction.UnboundCounter.active(owner);
  }

  public static void stopped(LivingEntity owner) {
    var own = Laplace.owned(owner);
    if (own.isEmpty()) return;
    var rows = own.get().getOrCreateTag().getCompound(Laplace.PROFILES);
    for (var i : SkillAPI.getSkillsFrom(owner).getLearnedSkills())
      if (i.getSkillId().getNamespace().equals("tensura")
          && !LaplaceActions.legacy(i)
          && i.isToggled()
          && rows.getAllKeys().stream()
              .anyMatch(
                  k ->
                      k.startsWith(i.getSkillId() + "#")
                          && rows.getCompound(k).getBoolean("enabled"))) {
        i.setToggled(false);
        i.onToggleOff(owner);
        i.markDirty();
      }
    refreshAttributes(owner);
    dev.heavenlyrestriction.SchemaArt.refresh(owner);
    dev.heavenlyrestriction.SplitMindSkill.refresh(owner);
    dev.heavenlyrestriction.metis.MetisIntellect.refresh(owner);
    dev.heavenlyrestriction.tyche.Tyche.refresh(owner);
  }

  public static void refreshAttributes(LivingEntity owner) {
    var own = Laplace.owned(owner);
    if (own.isEmpty()) return;
    var rows = own.get().getOrCreateTag().getCompound(Laplace.PROFILES);
    for (var slot : TensuraStorages.getAbilityFrom(owner).getAbilitySlots()) {
      if (slot.isEmpty()) continue;
      var i = SkillAPI.getSkillsFrom(owner).getSkill(slot.getSkill()).orElse(null);
      if (i != null
          && rows.contains(i.getSkillId() + "#-1")
          && ExpandedActions.attributeEffects(i)) {
        i.removeAttributeModifiers(owner, slot.getMode());
        i.addHeldAttributeModifiers(owner, slot.getMode());
      }
    }
  }

  private ExpandedStates() {}
}
