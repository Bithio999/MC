package dev.wildhunt.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.wildhunt.SchemaArt;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.handler.AbilityHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native learning failures assume a 100-point skill; Schema has its own budget. */
@Mixin(AbilityHandler.class)
public abstract class SchemaLearningMixin {
    @WrapOperation(method="lambda$init$4",at=@At(value="INVOKE",target="Ljava/lang/Math;max(DD)D"),remap=false)
    private static double schema$failureFloor(double proposed,double nativeFloor,Operation<Double> original,Changeable<ManasSkillInstance> requested,LivingEntity owner,int key,int mode){
        return original.call(proposed,requested.get().getSkill() instanceof SchemaArt ? -1000d : nativeFloor);
    }
    @WrapOperation(method="lambda$init$4",at=@At(value="INVOKE",target="Lio/github/manasmods/tensura/handler/AbilityHandler;checkFailChance(Lnet/minecraft/world/entity/LivingEntity;D)Z"),remap=false)
    private static boolean schema$failureProgress(LivingEntity owner,double remaining,Operation<Boolean> original,Changeable<ManasSkillInstance> requested,LivingEntity activatingOwner,int key,int mode){
        // Preserve native failure odds at the same percentage of learning completed.
        return original.call(owner,requested.get().getSkill() instanceof SchemaArt ? remaining/10d : remaining);
    }
}
