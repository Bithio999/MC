package dev.wildhunt;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class HunterCommands {
    private HunterCommands() {}
    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("wildhunt")
            .executes(HunterCommands::guide)
            .then(Commands.literal("guide").executes(HunterCommands::guide))
            .then(Commands.literal("status").executes(HunterCommands::status))
            .then(Commands.literal("evolve").executes(HunterCommands::evolve));
        var ascend = Commands.literal("ascend");
        for (Apex apex : Apex.values()) ascend.then(Commands.literal(apex.id()).executes(ctx -> ascend(ctx, apex)));
        root.then(ascend);
        event.getDispatcher().register(root);
    }
    private static int guide(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("Wild Hunt — Human → Awakened Hunter → Apex Hunter → Sovereign Hunter\n"
            + "Awakened: 100K EP + 1 distinct boss. Apex: 400K EP + 3 distinct bosses. Sovereign: 2M EP + 5 distinct bosses + mastery of an apex skill.\n"
            + "Repeated kills of one boss type count once. Apex Unique skills are rollable through normal Tensura reincarnation.\n"
            + "Equip skills in Tensura's ability menu. Use /wildhunt status, /wildhunt evolve, and /wildhunt ascend <apex>. Each Ultimate requires mastery of its own Unique skill."), false);
        return Command.SINGLE_SUCCESS;
    }
    private static int status(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        ctx.getSource().sendSuccess(() -> Component.literal("Wild Hunt | Stage " + HunterData.stage(player)
            + "/3 | EP " + (long) EnergyHelper.getBaseMaxEP(player) + " | Distinct bosses " + HunterData.bossKills(player).size()
            + " | Mastered " + HunterData.mastered(player) + "/4"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Defeated: " + String.join(", ", HunterData.bossKills(player))), false);
        return Command.SINGLE_SUCCESS;
    }
    private static int evolve(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        int stage = HunterData.stage(player);
        if ((stage == 0 && !HunterData.human(player)) || !HunterProgression.canEvolve(stage,
                EnergyHelper.getBaseMaxEP(player), HunterData.bossKills(player).size(), HunterData.mastered(player))) {
            ctx.getSource().sendFailure(Component.literal("Evolution requirements unmet. Use /wildhunt status and /wildhunt guide."));
            return 0;
        }
        return RaceAPI.getRaceFrom(player).evolveRace(WildHunt.HUNTERS.get(stage).get()) ? 1 : 0;
    }
    private static int ascend(CommandContext<CommandSourceStack> ctx, Apex apex) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        var skills = SkillAPI.getSkillsFrom(player);
        var unique = skills.getSkill(WildHunt.UNIQUE.get(apex).get());
        var ultimate = WildHunt.ULTIMATE.get(apex).get();
        if (skills.getSkill(ultimate).isPresent() || !HunterProgression.canAscend(HunterData.stage(player),
                unique.map(s -> s.isMastered(player)).orElse(false),
                EnergyHelper.getBaseMaxEP(player))) {
            ctx.getSource().sendFailure(Component.literal("Ascension requires Sovereign Hunter, 2M EP, a mastered matching Unique skill."));
            return 0;
        }
        // Only remove the source skill after the native skill storage accepts its replacement.
        if (!HunterData.learnReward(player, ultimate)) return 0;
        skills.forgetSkill(unique.orElseThrow());
        player.sendSystemMessage(Component.literal("Your soul has awakened " + apex.ultimateName + "!"));
        return Command.SINGLE_SUCCESS;
    }
}
