package dev.wildhunt.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wildhunt.ProgressionEP;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keep native secondary branching and weighted requirements, changing only its EP input. */
@Mixin(EvolutionRequirement.EPRequirement.class)
public abstract class SecondaryTotalEPMixin {
    @ModifyExpressionValue(method="getProgress",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/util/EnergyHelper;getBaseMaxEP(Lnet/minecraft/world/entity/LivingEntity;)D"),remap=false)
    private double myths$totalSecondaryEP(double original,@Local(argsOnly=true) LivingEntity entity){
        return ProgressionEP.usesTotalRequirements(entity)?EnergyHelper.getMaxEP(entity):original;
    }
}
