package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.laplace.expand.ParameterSnapshots;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public abstract class LaplaceDelayedEntityMixin {
  @WrapMethod(method = "guardEntityTick")
  private <T extends Entity> void laplace$tick(
      Consumer<T> consumer, T entity, Operation<Void> original) {
    if (!ParameterSnapshots.has(entity)
        && !dev.heavenlyrestriction.laplace.expand.ParameterRuntime.active()) {
      original.call(consumer, entity);
      return;
    }
    try (var scope = ParameterSnapshots.entity(entity)) {
      original.call(consumer, entity);
    }
  }
}
