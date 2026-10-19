package dev.wildhunt.mixin;

import dev.wildhunt.GodlySkill;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.handler.DamagingHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DamagingHandler.class)
public abstract class GodlyResistanceAttributesMixin {
    @Inject(method="applyResistanceAttributes",at=@At("HEAD"),cancellable=true,remap=false)
    private static void myths$degrade(LivingEntity target,DamageSource source,Changeable<Float> amount,CallbackInfo ci) {
        if(GodlySkill.passiveActive(source.getEntity()) || GodlySkill.degraded(target))ci.cancel();
    }
}
