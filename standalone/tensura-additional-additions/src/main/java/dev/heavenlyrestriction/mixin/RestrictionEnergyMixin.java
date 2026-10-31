package dev.heavenlyrestriction.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.heavenlyrestriction.RestrictionData;
import io.github.manasmods.tensura.handler.DeathHandler;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Redistribute the existing native award before its penalties/caps; never award twice. */
@Mixin(DeathHandler.class)
public abstract class RestrictionEnergyMixin {
    @ModifyExpressionValue(method="gainEPEntity",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/Alignment;getAuraGainMultiplier()D"),remap=false)
    private static double restriction$aura(double original,@Local(argsOnly=true,ordinal=0) LivingEntity e){
        return RestrictionData.growth(e,original,TensuraStorages.getExistenceFrom(e).getAlignment().getMagiculeGainMultiplier()).aura();
    }
    @ModifyExpressionValue(method="gainEPEntity",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/Alignment;getMagiculeGainMultiplier()D"),remap=false)
    private static double restriction$magicules(double original,@Local(argsOnly=true,ordinal=0) LivingEntity e){
        return RestrictionData.growth(e,TensuraStorages.getExistenceFrom(e).getAlignment().getAuraGainMultiplier(),original).magicules();
    }
}
