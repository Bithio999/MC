package dev.wildhunt.mixin;

import dev.wildhunt.Apex;
import dev.wildhunt.WildHunt;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

/** Extend the configured primary pool; keep Tensura's roll and uniqueness rules. */
@Mixin(ReincarnationMenu.class)
public abstract class ReincarnationSkillPoolMixin {
    @Inject(method = "getSkillPool", at = @At("RETURN"), cancellable = true, remap = false)
    private static void wildhunt$addApexUniques(CallbackInfoReturnable<List<ManasSkill>> cir) {
        var pool = new ArrayList<>(cir.getReturnValue());
        for (Apex apex : Apex.values()) {
            var unique = WildHunt.UNIQUE.get(apex).get();
            if (!pool.contains(unique)) pool.add(unique);
        }
        cir.setReturnValue(List.copyOf(pool));
    }
}
