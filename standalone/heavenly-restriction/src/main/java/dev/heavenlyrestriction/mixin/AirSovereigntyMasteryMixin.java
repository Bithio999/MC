package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.AirSovereigntyArt;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ManasSkillInstance.class)
public abstract class AirSovereigntyMasteryMixin {
    @Inject(method="onSkillMastered",at=@At("RETURN"),remap=false)
    private void restriction$airEvolution(LivingEntity owner,CallbackInfo out){AirSovereigntyArt.unlockEvolution(owner,(ManasSkillInstance)(Object)this);}
}
