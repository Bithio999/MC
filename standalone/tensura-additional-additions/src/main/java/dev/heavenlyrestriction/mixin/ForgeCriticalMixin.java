package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.forge.ForgeTempering;
import io.github.manasmods.manascore.attribute.api.ManasCoreAttributeUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ManasCoreAttributeUtils.class)
public abstract class ForgeCriticalMixin {
  @Inject(method = "triggerCriticalAttackEffect", at = @At("HEAD"), remap = false)
  private static void forge$critical(Entity target, Entity owner, CallbackInfo ci) {
    ForgeTempering.critical(target, owner);
  }
}
