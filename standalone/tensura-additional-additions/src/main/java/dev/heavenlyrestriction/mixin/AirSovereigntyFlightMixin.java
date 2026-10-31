package dev.heavenlyrestriction.mixin;

import dev.heavenlyrestriction.AirSovereigntyArt;
import io.github.manasmods.tensura.ability.SkillUtils;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkillUtils.class)
public abstract class AirSovereigntyFlightMixin {
    @Inject(method="canFlyWithSkills",at=@At("RETURN"),cancellable=true,remap=false)
    private static void restriction$battlewillFlight(Player player,CallbackInfoReturnable<Boolean> out){
        if(!out.getReturnValue() && AirSovereigntyArt.authorizesFlight(player))out.setReturnValue(true);
    }
}
