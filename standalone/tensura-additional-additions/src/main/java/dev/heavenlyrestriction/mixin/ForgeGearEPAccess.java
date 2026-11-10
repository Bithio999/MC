package dev.heavenlyrestriction.mixin;

import io.github.manasmods.tensura.handler.DeathHandler;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = DeathHandler.class, remap = false)
public interface ForgeGearEPAccess {
  @Invoker("gearGetEP")
  static void forge$nativeGrowth(LivingEntity owner, EquipmentSlot slot, double ep) {
    throw new AssertionError();
  }
}
