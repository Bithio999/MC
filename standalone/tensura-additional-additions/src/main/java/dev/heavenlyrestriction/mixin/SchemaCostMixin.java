package dev.heavenlyrestriction.mixin;
import dev.heavenlyrestriction.SchemaArt;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Adjust the actual native bill before affordability checks, once at the paying overload. */
@Mixin(EnergyHelper.class)
public abstract class SchemaCostMixin {
    @ModifyVariable(method="isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;DD)Z",at=@At("HEAD"),argsOnly=true,index=1,remap=false)
    private static double schema$aura(double amount,LivingEntity owner,double aura,double magicules){return SchemaArt.cost(owner,amount);}
    @ModifyVariable(method="isOutOfEnergy(Lnet/minecraft/world/entity/LivingEntity;DD)Z",at=@At("HEAD"),argsOnly=true,index=3,remap=false)
    private static double schema$magicules(double amount,LivingEntity owner,double aura,double magicules){return SchemaArt.cost(owner,amount);}
    @ModifyVariable(method={"isOutOfAuraConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D","isOutOfMagiculeConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D"},at=@At("HEAD"),argsOnly=true,index=1,remap=false)
    private static double schema$consuming(double amount,LivingEntity owner,double cost,double remain){return SchemaArt.cost(owner,amount);}
    // Callers divide this result by their original per-HP/shield price. Keep that unit intact.
    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method={"isOutOfAuraConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D","isOutOfMagiculeConsuming(Lnet/minecraft/world/entity/LivingEntity;DD)D"},at=@At("RETURN"),remap=false)
    private static double schema$shortfall(double amount,LivingEntity owner,double cost,double remain){return amount>0?amount/SchemaArt.costFactor(owner):amount;}
}
