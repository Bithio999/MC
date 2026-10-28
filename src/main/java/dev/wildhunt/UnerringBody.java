package dev.wildhunt;

import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import java.util.List;

/** Ownership alone does not grant an in-slot defense. */
public final class UnerringBody {
    private UnerringBody(){}
    public static boolean active(LivingEntity owner){return ChainedState.instance(owner).filter(i->i.getSkill() instanceof ChainedSkill s && s.isInSlot(owner,i)).isPresent();}
    public static boolean immune(MobEffectInstance effect){return effect.is(MobEffects.MOVEMENT_SLOWDOWN) || effect.is(TensuraMobEffects.getReference(TensuraMobEffects.FEAR)) || effect.is(TensuraMobEffects.getReference(TensuraMobEffects.BURDEN));}
    public static void applicable(MobEffectEvent.Applicable event){if(active(event.getEntity()) && immune(event.getEffectInstance()))event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);}
    public static void tick(LivingEntity owner){
        if(owner.level().isClientSide() || !active(owner))return;
        for(var effect:List.copyOf(owner.getActiveEffects()))if(immune(effect))owner.removeEffect(effect.getEffect());
    }
}
