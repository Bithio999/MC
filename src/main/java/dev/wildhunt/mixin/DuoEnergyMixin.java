package dev.wildhunt.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wildhunt.myths.DuoSystem;
import io.github.manasmods.tensura.handler.DeathHandler;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Modify weights before native penalties and caps; do not award a second kill reward. */
@Mixin(DeathHandler.class)
public abstract class DuoEnergyMixin {
    @ModifyExpressionValue(method="gainEPEntity",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/Alignment;getAuraGainMultiplier()D"),remap=false)
    private static double myths$aura(double original,@Local(argsOnly=true,ordinal=0) LivingEntity player) {
        var alignment=TensuraStorages.getExistenceFrom(player).getAlignment();
        return DuoSystem.gains(player,original,alignment.getMagiculeGainMultiplier()).aura();
    }
    @ModifyExpressionValue(method="gainEPEntity",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/Alignment;getMagiculeGainMultiplier()D"),remap=false)
    private static double myths$magicules(double original,@Local(argsOnly=true,ordinal=0) LivingEntity player) {
        var alignment=TensuraStorages.getExistenceFrom(player).getAlignment();
        return DuoSystem.gains(player,alignment.getAuraGainMultiplier(),original).magicules();
    }
}
