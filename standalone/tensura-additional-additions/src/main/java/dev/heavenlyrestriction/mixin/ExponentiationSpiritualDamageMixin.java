package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.heavenlyrestriction.exponentiation.*;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=TensuraDamageHelper.class,priority=500)
public abstract class ExponentiationSpiritualDamageMixin {
    @WrapMethod(method="directSpiritualHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;FF)Z",remap=false)
    private static boolean exponentiation$raw(LivingEntity victim,Entity attacker,DamageSource source,float amount,float resistance,Operation<Boolean> original){try(var scope=ExponentiationContext.raw(attacker instanceof LivingEntity l?l:source==null?null:dev.heavenlyrestriction.RestrictionData.attacker(source),ExponentiationContext.DAMAGE)){return original.call(victim,attacker,source,amount,resistance);}}
    @WrapOperation(method="directSpiritualHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;FF)Z",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/storage/ep/IExistence;setSpiritualHealth(D)V"),remap=false)
    private static void exponentiation$damage(IExistence energy,double proposed,Operation<Void> original,LivingEntity victim,Entity attacker,DamageSource source,float amount,float resistance){var owner=attacker instanceof LivingEntity l?l:source==null?null:dev.heavenlyrestriction.RestrictionData.attacker(source);if(owner!=victim && Exponentiation.active(owner))proposed=Math.max(0,energy.getSpiritualHealth()-Exponentiation.amplify(Math.max(0,energy.getSpiritualHealth()-proposed)));original.call(energy,proposed);}
}
