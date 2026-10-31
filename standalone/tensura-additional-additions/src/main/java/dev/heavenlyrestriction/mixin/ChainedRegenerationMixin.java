package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.*;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.storage.ep.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retain a flat 1 MP/second and convert all remaining native area/rate/sleep recovery. */
@Mixin(ExistenceStorage.class)
public abstract class ChainedRegenerationMixin {
    @Shadow(remap=false) private static boolean isAffectedByEnergyLost(LivingEntity owner){throw new AssertionError();}
    @Inject(method="handleMagiculeRegen",at=@At("HEAD"),remap=false)
    private static void restriction$convert(IExistence storage,LivingEntity owner,double areaMagicule,double maxMagicule,CallbackInfo out){
        if(!ChainedState.owned(owner) || storage.isSpiritualForm() && !SkillUtils.inSpiritualWorld(owner.level().dimension()) && isAffectedByEnergyLost(owner))return;
        double amount=Math.max(0,ChainedResources.regeneration(storage,owner,areaMagicule)-ChainedResources.RETAINED_MP_PER_PULSE);
        amount*=SchemaArt.regenerationFactor(owner);
        double recovered=Math.min(EnergyHelper.getMaxAura(owner),storage.getAura()+amount)-storage.getAura();
        double value=storage.getAura()+recovered;
        if(value>storage.getAura()){storage.setAura(value);storage.markDirty();SchemaArt.train(owner,recovered);}
    }
    @WrapOperation(method="handleMagiculeRegen",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/ep/IExistence;setMagicule(D)V",ordinal=2),remap=false)
    private static void restriction$retainFlatRate(IExistence storage,double proposed,Operation<Void> original,@Local(argsOnly=true) LivingEntity owner,@Local(argsOnly=true,ordinal=0) double areaMagicule){
        if(ChainedState.owned(owner))proposed=Math.min(EnergyHelper.getMaxMagicule(owner),storage.getMagicule()+Math.min(ChainedResources.regeneration(storage,owner,areaMagicule),ChainedResources.RETAINED_MP_PER_PULSE));
        original.call(storage,proposed);
    }
}
