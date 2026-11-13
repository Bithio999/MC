package dev.heavenlyrestriction.laplace.expand;

import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.world.entity.LivingEntity;

/** Automatic-cast preparation checks the selected pools, without reserving or charging them. */
public final class LaplaceFunding {
  public static boolean funded(
      LivingEntity owner, ManasSkillInstance skill, int mode, double aura, double mp) {
    var energy = TensuraStorages.getExistenceFrom(owner);
    boolean nativeResult = energy.getAura() >= aura && energy.getMagicule() >= mp;
    var own = Laplace.active(owner);
    if (own.isEmpty()) return nativeResult;
    var action = LaplaceActions.find(owner, skill, mode);
    if (action == null || !Laplace.effectiveEnabled(owner, own.get(), action.key()))
      return nativeResult;
    try {
      var model = action.model();
      var result = model.evaluate(Laplace.equations(own.get(), action));
      var type = (TensuraSkill) skill.getSkill();
      double raw =
          type.getMagiculeCost(owner, skill, mode)
              + type.getAuraCost(owner, skill, mode) * model.payment().rates().magiculesPerAura();
      double finalNative = mp + aura * model.payment().rates().magiculesPerAura();
      double budget =
          raw > 0
              ? result.values().get("magicule_cost") * finalNative / raw
              : result.values().get("magicule_cost");
      return PaymentPlan.prepare(
              new PaymentPlan.Pools(
                  energy.getMagicule(),
                  energy.getAura(),
                  owner.getHealth(),
                  energy.getSpiritualHealth()),
              model.payment().bill(budget))
          .affordable();
    } catch (IllegalArgumentException ex) {
      return nativeResult;
    }
  }

  private LaplaceFunding() {}
}
