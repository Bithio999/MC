package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.manasmods.tensura.config.EnergyConfig;
import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.tensura.storage.ep.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Setters are terminal boundaries: positive gains square, costs/damage/restores do not. */
@Mixin(value=ExistenceStorage.class,priority=500)
public abstract class ExponentiationEnergyMixin {
    @Shadow(remap=false) protected abstract LivingEntity getOwner();
    @Shadow(remap=false) public abstract double getAura();
    @Shadow(remap=false) public abstract double getMagicule();
    @Shadow(remap=false) public abstract double getSpiritualHealth();
    @WrapMethod(method="load",remap=false)
    private void exponentiation$load(CompoundTag tag,Operation<Void> original){try(var scope=ExponentiationContext.raw(getOwner(),ExponentiationContext.RESTORE)){original.call(tag);}}
    @WrapMethod(method={"setAura","setMagicule","setSpiritualHealth"},remap=false)
    private void exponentiation$capacity(double requested,Operation<Void> original){try(var scope=ExponentiationContext.raw(getOwner(),ExponentiationContext.FINAL)){original.call(requested);}}
    @ModifyVariable(method="setAura",at=@At("HEAD"),argsOnly=true,remap=false)
    private double exponentiation$aura(double proposed){var owner=getOwner();return owner==null || owner.level().isClientSide() || !Exponentiation.active(owner)?proposed:Exponentiation.gain(owner,getAura(),proposed,Exponentiation.capacity(owner,()->EnergyHelper.getMaxAura(owner)));}
    @ModifyVariable(method="setMagicule",at=@At("HEAD"),argsOnly=true,remap=false)
    private double exponentiation$magicule(double proposed){var owner=getOwner();return owner==null || owner.level().isClientSide() || !Exponentiation.active(owner)?proposed:Exponentiation.gain(owner,getMagicule(),proposed,Exponentiation.capacity(owner,()->EnergyHelper.getMaxMagicule(owner)));}
    @ModifyVariable(method="setSpiritualHealth",at=@At("HEAD"),argsOnly=true,remap=false)
    private double exponentiation$spiritual(double proposed){var owner=getOwner();return owner==null || owner.level().isClientSide() || !Exponentiation.active(owner)?proposed:Exponentiation.gain(owner,getSpiritualHealth(),proposed,Exponentiation.capacity(owner,()->owner.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)));}
    @ModifyConstant(method={"setAura","setMagicule"},constant=@Constant(doubleValue=2.147483647E9),remap=false)
    private double exponentiation$reserveLimit(double limit){return Exponentiation.active(getOwner())?Exponentiation.square(limit):limit;}
    @WrapMethod(method="handleAuraRegen",remap=false)
    private static void exponentiation$auraRegen(IExistence energy,LivingEntity owner,double max,Operation<Void> original){try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.RECOVERY)){original.call(energy,owner,max);}}
    @WrapMethod(method="handleMagiculeRegen",remap=false)
    private static void exponentiation$magiculeRegen(IExistence energy,LivingEntity owner,double area,double max,Operation<Void> original){try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.RECOVERY)){original.call(energy,owner,area,max);}}
    @WrapMethod(method="handleSpiritualHealthRegen",remap=false)
    private static void exponentiation$spiritualRegen(net.minecraft.server.MinecraftServer server,LivingEntity owner,IExistence energy,Operation<Void> original){try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.RECOVERY)){original.call(server,owner,energy);}}
    @WrapOperation(method="handleAuraRegen",at=@At(value="FIELD",target="Lio/github/manasmods/tensura/config/EnergyConfig;sleepModeAura:D"),remap=false)
    private static double exponentiation$sleepAura(EnergyConfig config,Operation<Double> original,@Local(argsOnly=true) LivingEntity owner,@Local(argsOnly=true) double maximum){double rate=original.call(config);if(!Exponentiation.active(owner) || maximum<=0)return rate;try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.DAMAGE)){return rate*Math.min(1,EnergyHelper.getMaxAura(owner)/maximum);}}
    @WrapOperation(method="handleMagiculeRegen",at=@At(value="FIELD",target="Lio/github/manasmods/tensura/config/EnergyConfig;sleepModeMagicule:D"),remap=false)
    private static double exponentiation$sleepMagicule(EnergyConfig config,Operation<Double> original,@Local(argsOnly=true) LivingEntity owner,@Local(argsOnly=true,ordinal=1) double maximum){double rate=original.call(config);if(!Exponentiation.active(owner) || maximum<=0)return rate;try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.DAMAGE)){return rate*Math.min(1,EnergyHelper.getMaxMagicule(owner)/maximum);}}
}
