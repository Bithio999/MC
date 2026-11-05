package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.highrealm.boss.KaelVeyrEntity;
import io.github.manasmods.tensura.handler.DeathHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve native reward handling while clamping this boss's actual EP immediately. */
@Mixin(DeathHandler.class)
public abstract class WayfarerGrowthMixin {
  @Inject(method = "gainEPEntity", at = @At("TAIL"), remap = false)
  private static void wayfarer$capReward(
      LivingEntity owner, LivingEntity victim, double ep, CallbackInfo ci) {
    if (owner instanceof KaelVeyrEntity kael) kael.capEP();
  }
}
