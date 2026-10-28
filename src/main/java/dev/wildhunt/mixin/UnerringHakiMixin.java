package dev.wildhunt.mixin;

import dev.wildhunt.UnerringBody;
import io.github.manasmods.tensura.entity.magic.field.haki.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Includes Sacred Haki's separate effect implementation, not only Fear application. */
@Mixin({HakiField.class,SacredHakiField.class})
public abstract class UnerringHakiMixin {
    @Inject(method="applyEffect",at=@At("HEAD"),cancellable=true,remap=false)
    private void myths$immune(LivingEntity target,boolean instant,CallbackInfo ci){if(UnerringBody.active(target))ci.cancel();}
}
