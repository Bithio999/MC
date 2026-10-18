package dev.wildhunt.mixin;

import dev.wildhunt.WildHunt;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.tensura.race.human.HumanRace;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(HumanRace.class)
public abstract class HumanEvolutionMixin {
    @Inject(method = "getNextEvolutions", at = @At("RETURN"), cancellable = true, remap = false)
    private void wildhunt$appendHunter(ManasRaceInstance instance, LivingEntity entity, CallbackInfoReturnable<List<ManasRace>> cir) {
        if (!instance.getRaceId().toString().equals("tensura:human")) return;
        List<ManasRace> choices = new ArrayList<>(cir.getReturnValue());
        choices.add(WildHunt.HUNTERS.getFirst().get());
        cir.setReturnValue(choices);
    }
}
