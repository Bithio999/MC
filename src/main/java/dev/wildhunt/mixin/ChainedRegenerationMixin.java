package dev.wildhunt.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wildhunt.ChainedState;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.ep.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep native baseline MP regeneration, sleep recovery and spiritual/overflow rules intact. */
@Mixin(ExistenceStorage.class)
public abstract class ChainedRegenerationMixin {
    @Shadow(remap=false) private static boolean isAffectedByEnergyLost(LivingEntity owner){throw new AssertionError();}
    @Inject(method="handleMagiculeRegen",at=@At("HEAD"),remap=false)
    private static void myths$convertBonus(IExistence storage,LivingEntity owner,double areaMagicule,double maxMagicule,CallbackInfo out){
        if(!ChainedState.owned(owner) || storage.isSpiritualForm() && !SkillUtils.inSpiritualWorld(owner.level().dimension()) && isAffectedByEnergyLost(owner))return;
        double bonus=Math.max(0,owner.getAttributeValue(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER)-1);
        double amount=Math.max(0,areaMagicule*EnergyHelper.CONFIG.areaMagiculeRegen*bonus);
        double value=Math.min(EnergyHelper.getMaxAura(owner),storage.getAura()+amount);
        if(value>storage.getAura()){storage.setAura(value);storage.markDirty();}
    }
    @ModifyExpressionValue(method="handleMagiculeRegen",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"),remap=false)
    private static double myths$retainBaseline(double original,@Local(argsOnly=true) LivingEntity owner){
        // Preserve rates below 1 too: the pact must not cancel native regeneration penalties.
        return ChainedState.owned(owner)?Math.min(1,original):original;
    }
}
