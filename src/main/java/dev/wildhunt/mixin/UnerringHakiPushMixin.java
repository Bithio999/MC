package dev.wildhunt.mixin;

import dev.wildhunt.UnerringBody;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.extra.HakiSkill;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HakiSkill.class)
public abstract class UnerringHakiPushMixin {
    @Inject(method="hakiPush",at=@At("HEAD"),cancellable=true,remap=false)
    private static void myths$immune(LivingEntity target,Entity source,ManasSkillInstance skill,int fearLevel,CallbackInfo ci){if(UnerringBody.active(target))ci.cancel();}
}
