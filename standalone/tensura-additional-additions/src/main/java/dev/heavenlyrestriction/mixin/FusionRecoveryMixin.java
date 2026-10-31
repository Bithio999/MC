package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.fusion.Fusion;
import dev.heavenlyrestriction.ChainedState;
import io.github.manasmods.tensura.storage.ep.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Multiply only native positive recovery, after biome, rate and sleep calculations. */
@Mixin(ExistenceStorage.class)
public abstract class FusionRecoveryMixin {
    @WrapOperation(method="handleAuraRegen",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/ep/IExistence;setAura(D)V",ordinal=1),remap=false)
    private static void fusion$aura(IExistence storage,double proposed,Operation<Void> original,@Local(argsOnly=true) LivingEntity owner){original.call(storage,Math.min(EnergyHelper.getMaxAura(owner),storage.getAura()+Math.max(0,proposed-storage.getAura())*Fusion.regenerationFactor(owner)));}
    @WrapOperation(method="handleMagiculeRegen",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/ep/IExistence;setMagicule(D)V",ordinal=2),remap=false)
    private static void fusion$magicule(IExistence storage,double proposed,Operation<Void> original,@Local(argsOnly=true) LivingEntity owner){original.call(storage,Math.min(EnergyHelper.getMaxMagicule(owner),storage.getMagicule()+Math.max(0,proposed-storage.getMagicule())*(ChainedState.owned(owner)?1:Fusion.regenerationFactor(owner))));}
}
