package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.loans.LoanShark;
import io.github.manasmods.tensura.registry.attribute.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
/** Observe real permanent pool awards, including commands/items/skills, after native write guards. */
@Mixin(value=AttributeInstance.class,priority=400)
public abstract class LoanGrowthMixin {
    @WrapMethod(method="setBaseValue")
    private void loan$gain(double proposed,Operation<Void> original){var a=(AttributeInstance)(Object)this;var e=((TensuraAttributeInstance)this).tensura$getOwner();boolean observe=e!=null && e.getAttributes()!=null && !(e instanceof net.minecraft.server.level.ServerPlayer player && player.gameMode==null) && !e.level().isClientSide() && !LoanShark.transferring() && (a.getAttribute().equals(TensuraAttributes.MAX_AURA) || a.getAttribute().equals(TensuraAttributes.MAX_MAGICULE));double before=a.getBaseValue(),effective=observe?a.getValue():0;original.call(proposed);if(observe)LoanShark.gained(e,a,before,effective);}
}
