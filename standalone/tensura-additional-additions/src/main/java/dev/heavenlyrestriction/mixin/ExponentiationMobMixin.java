package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.exponentiation.ExponentiationContext;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Mob.class)
public abstract class ExponentiationMobMixin {
    @WrapMethod(method="doHurtTarget")
    private boolean exponentiation$attack(Entity target,Operation<Boolean> original){try(var scope=ExponentiationContext.raw((Mob)(Object)this,ExponentiationContext.DAMAGE)){return original.call(target);}}
}
