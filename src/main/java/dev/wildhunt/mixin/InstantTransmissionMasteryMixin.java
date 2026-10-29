package dev.wildhunt.mixin;
import dev.wildhunt.InstantTransmissionArt;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ManasSkillInstance.class)
public abstract class InstantTransmissionMasteryMixin {
    @Inject(method="onSkillMastered",at=@At("RETURN"),remap=false)
    private void transmission$evolution(LivingEntity owner,CallbackInfo out){InstantTransmissionArt.unlockEvolution(owner,(ManasSkillInstance)(Object)this);}
}
