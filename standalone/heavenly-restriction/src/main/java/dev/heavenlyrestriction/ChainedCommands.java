package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Explicit operator certification for reincarnation history predating this feature. */
public final class ChainedCommands {
    private ChainedCommands(){}
    public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("heavenlyrestriction").then(Commands.literal("chained").requires(s->s.hasPermission(2))
            .then(Commands.literal("inherit").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("skill",ResourceLocationArgument.id())
                .suggests((c,b)->SharedSuggestionProvider.suggestResource(SkillAPI.getSkillRegistry().getIds(),b)).executes(c->{
                    var p=EntityArgument.getPlayer(c,"player");var skill=SkillAPI.getSkillRegistry().get(ResourceLocationArgument.getId(c,"skill"));
                    if(skill==null || SkillAPI.getSkillsFrom(p).getSkill(skill).isEmpty()){c.getSource().sendFailure(Component.literal("That player must already own the skill being certified."));return 0;}
                    ChainedState.remember(p,skill);c.getSource().sendSuccess(()->Component.literal("Recorded "+skill.getName().getString()+" as an inherited reincarnation skill for "+p.getName().getString()+"."),true);return 1;
                }))))));
    }
}
