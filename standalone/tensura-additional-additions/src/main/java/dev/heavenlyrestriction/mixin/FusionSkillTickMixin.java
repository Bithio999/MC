package dev.heavenlyrestriction.mixin;
import dev.heavenlyrestriction.fusion.Fusion;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The embedded body's passives must not run a second copy beside its controlling body. */
@Mixin(ManasSkillInstance.class)
public abstract class FusionSkillTickMixin {
    @Inject(method="onTick",at=@At("HEAD"),cancellable=true,remap=false)
    private void fusion$oneBody(LivingEntity owner,CallbackInfo ci){if(Fusion.hidden(owner))ci.cancel();}
}
