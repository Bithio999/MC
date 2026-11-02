package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(EnergyHelper.class)
public abstract class ExponentiationGainMixin {
    @WrapMethod(method={"gainAura","gainMagicule"},remap=false)
    private static void exponentiation$gain(LivingEntity owner,double amount,EnergyHelper.GainType type,Operation<Void> original){try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.GROWTH)){original.call(owner,amount,type);}}
}
