package dev.wildhunt.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildhunt.*;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

@Mixin(ReincarnationMenu.class)
public abstract class ChainedReincarnationMixin {
    @Inject(method="getReincarnationSkills",at=@At("RETURN"),cancellable=true,remap=false)
    private static void myths$restrictedRolls(List<ManasSkill> pool,ServerLevel level,boolean ignoreGameRule,Player roller,CallbackInfoReturnable<List<ManasSkill>> out){
        out.setReturnValue(ChainedReincarnation.filter(out.getReturnValue(),roller));
    }
    @Inject(method={"getRacePool","getRandomRacePool"},at=@At("RETURN"),cancellable=true,remap=false)
    private void myths$startingRace(CallbackInfoReturnable<List<ManasRace>> out){var list=new ArrayList<>(out.getReturnValue());var race=WildHunt.RESTRICTED.getFirst().get();if(!list.contains(race))list.add(race);out.setReturnValue(List.copyOf(list));}
    @WrapMethod(method="grantedUniqueSkill",remap=false)
    private static boolean myths$recordReincarnation(Player player,ServerLevel level,ManasSkill skill,boolean coverEP,Operation<Boolean> original){
        try(var scope=ChainedContext.reincarnation(player,skill)){boolean granted=original.call(player,level,skill,coverEP);if(granted)ChainedState.remember(player,skill);return granted;}
    }
    @WrapMethod(method="setRace",remap=false)
    private static void myths$initializeStartingPools(Player player,ManasRace race,boolean resetEP,boolean grantUnique,Operation<Void> original){
        if(!resetEP){original.call(player,race,false,grantUnique);return;}
        try(var birth=ChainedContext.reincarnation(player,null)){
            if(race instanceof RestrictedRace){
                try(var scope=ChainedContext.initialRace(player)){original.call(player,race,true,grantUnique);ChainedState.finishInitialRace(player);}
            }else original.call(player,race,true,grantUnique);
        }
    }
    @WrapMethod(method="reincarnateAsRimuru",remap=false)
    private static void myths$specialReincarnation(net.minecraft.server.level.ServerPlayer player,Operation<Void> original){try(var scope=ChainedContext.reincarnation(player,null)){original.call(player);}}
}
