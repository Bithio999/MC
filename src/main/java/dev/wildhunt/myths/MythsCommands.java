package dev.wildhunt.myths;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class MythsCommands {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("myths").executes(c->{MythsNetwork.send(c.getSource().getPlayerOrException(),"");return 1;})
            .then(Commands.literal("status").executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(DuoSystem.primary(p).map(r->r.getDisplayName().getString()).orElse("No race")+" / "+DuoSystem.secondary(p).map(r->r.getDisplayName().getString()).orElse("No secondary")));MythsNetwork.send(p,"");return 1;}))
            .then(Commands.literal("secondary").then(Commands.argument("family",StringArgumentType.word()).suggests((c,b)->{for(var f:RaceFamily.values())b.suggest(f.key());return b.buildFuture();}).executes(c->{var p=c.getSource().getPlayerOrException();String result=RaceFamily.byKey(StringArgumentType.getString(c,"family")).map(f->DuoSystem.select(p,f)).orElse("Unknown family.");p.sendSystemMessage(Component.literal(result));return 1;})))
            .then(Commands.literal("evolve_secondary").then(Commands.argument("race",ResourceLocationArgument.id()).executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(DuoSystem.evolve(p,ResourceLocationArgument.getId(c,"race"))));return 1;})))
            .then(Commands.literal("awaken").executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(DuoSystem.convergence(p)));return 1;}))
            .then(Commands.literal("synthesize").then(Commands.argument("skill",StringArgumentType.word()).suggests((c,b)->{for(var k:dev.wildhunt.MythicEvolution.ultimates())b.suggest(k.id());return b.buildFuture();}).executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(dev.wildhunt.MythicEvolution.synthesize(p,StringArgumentType.getString(c,"skill"))));return 1;})))
            .then(Commands.literal("godly").then(Commands.argument("ultimate",ResourceLocationArgument.id()).executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(MythsProgression.ascend(p,ResourceLocationArgument.getId(c,"ultimate").toString())));return 1;})
                .then(Commands.argument("destination",StringArgumentType.word()).suggests((c,b)->{b.suggest("aion");b.suggest("thoth");return b.buildFuture();}).executes(c->{var p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(MythsProgression.ascend(p,ResourceLocationArgument.getId(c,"ultimate").toString(),StringArgumentType.getString(c,"destination"))));return 1;})))));
    }
    private MythsCommands() {}
}
