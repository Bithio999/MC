package dev.wildhunt.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildhunt.UnboundCritical;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Scope native melee crit accounting, including nested attacks, without leaking to later sources. */
@Mixin(Player.class)
public abstract class UnboundCriticalMixin {
    @WrapMethod(method="attack")
    private void myths$attack(Entity target,Operation<Void> original){try(var scope=UnboundCritical.melee((Player)(Object)this)){original.call(target);}}
}
