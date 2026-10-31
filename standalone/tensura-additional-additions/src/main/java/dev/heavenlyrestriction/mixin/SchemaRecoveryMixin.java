package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.SchemaArt;
import io.github.manasmods.tensura.storage.ep.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ExistenceStorage.class)
public abstract class SchemaRecoveryMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method="handleAuraRegen",at=@At("HEAD"),remap=false)
    private static void schema$refresh(IExistence energy,LivingEntity owner,double maximum,org.spongepowered.asm.mixin.injection.callback.CallbackInfo out){SchemaArt.refresh(owner);}

    @WrapOperation(method="handleAuraRegen",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/ep/IExistence;setAura(D)V",ordinal=1),remap=false)
    private static void schema$recovery(IExistence energy,double proposed,Operation<Void> original,@Local(argsOnly=true) LivingEntity owner){
        double before=energy.getAura();
        if(energy.getSleepModeTime()>0)proposed=Math.min(EnergyHelper.getMaxAura(owner),proposed+EnergyHelper.getMaxAura(owner)*EnergyHelper.CONFIG.sleepModeAura*(SchemaArt.regenerationFactor(owner)-1));
        original.call(energy,proposed);SchemaArt.train(owner,energy.getAura()-before);
    }
}
