package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.highrealm.boss.KaelVeyrEntity;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;

/** Bound incoming EP before native positive-growth events award extra stats or healing. */
@Mixin(AttributeInstance.class)
public abstract class WayfarerCapacityMixin {
  @WrapMethod(method = "setBaseValue")
  private void wayfarer$capGrowth(double proposed, Operation<Void> original) {
    var attribute = (AttributeInstance) (Object) this;
    var owner = ((TensuraAttributeInstance) this).tensura$getOwner();
    original.call(
        owner instanceof KaelVeyrEntity kael ? kael.capGrowth(attribute, proposed) : proposed);
  }
}
