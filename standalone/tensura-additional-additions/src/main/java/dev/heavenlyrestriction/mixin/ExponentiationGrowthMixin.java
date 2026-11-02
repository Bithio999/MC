package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.tensura.handler.DeathHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(DeathHandler.class)
public abstract class ExponentiationGrowthMixin {
    @WrapMethod(method="gainEPEntity",remap=false)
    private static void exponentiation$growth(LivingEntity owner,LivingEntity victim,double ep,Operation<Void> original){try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.GROWTH)){original.call(owner,victim,Exponentiation.active(owner)?Exponentiation.square(ep):ep);}}
}
