package dev.wildhunt.mixin;

import dev.wildhunt.ChainedState;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.ep.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExistenceStorage.class)
public abstract class ChainedRegenerationMixin {
    @Shadow(remap=false) private static boolean isAffectedByEnergyLost(LivingEntity owner){throw new AssertionError();}
    @Inject(method="handleMagiculeRegen",at=@At("HEAD"),cancellable=true,remap=false)
    private static void myths$convert(IExistence storage,LivingEntity owner,double areaMagicule,double maxMagicule,CallbackInfo out){
        if(!ChainedState.owned(owner) || storage.isSpiritualForm() && !SkillUtils.inSpiritualWorld(owner.level().dimension()) && isAffectedByEnergyLost(owner))return;
        double rate=areaMagicule*EnergyHelper.CONFIG.areaMagiculeRegen*owner.getAttributeValue(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);
        if(storage.getSleepModeTime()>0)rate+=maxMagicule*EnergyHelper.CONFIG.sleepModeMagicule;
        double value=Math.min(EnergyHelper.getMaxAura(owner),storage.getAura()+Math.max(0,rate));if(value>storage.getAura()){storage.setAura(value);storage.markDirty();}
        // Keep native excess-magicule decay, but do not replenish the spent magicule pool.
        if(storage.getMagicule()<=maxMagicule)out.cancel();
    }
}
