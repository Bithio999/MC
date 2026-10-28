package dev.wildhunt.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildhunt.*;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.menu.SkillCreationMenu;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** The native menu runs on a later packet, outside Creator's onPressed provenance scope. */
@Mixin(SkillCreationMenu.class)
public abstract class ChainedCreatorMixin {
    @WrapMethod(method="clickMenuButton")
    private boolean myths$creation(Player player,int index,Operation<Boolean> original){
        var ability=((SkillCreationMenu)(Object)this).getAbility();
        if(player.level().isClientSide() || ability!=UniqueSkills.CREATOR.get() || !ChainedState.owned(player) || !ChainedState.inherited(player,ability))return original.call(player,index);
        var owner=SkillAPI.getSkillsFrom(player).getSkill(ability);
        if(owner.filter(i->i.getMastery()>=0 && !i.isTemporarySkill() && !i.isSubInstance()).isEmpty())return original.call(player,index);
        try(var scope=ChainedContext.ability(player,ability)){return original.call(player,index);}
    }
}
