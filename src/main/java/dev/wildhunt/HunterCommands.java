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
        for (var kind : MythicEvolution.ultimates()) ascend.then(Commands.literal(kind.id()).executes(ctx -> {var p=ctx.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(MythicEvolution.synthesize(p,kind.id())));return 1;}));
        root.then(ascend);
        event.getDispatcher().register(root);
    }
    private static int guide(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("Wild Hunt — Human → Awakened Hunter → Apex Hunter → Sovereign Hunter\n"
            + "Awakened: 100K EP + 1 distinct boss. Apex: 400K EP + 3 distinct bosses. Sovereign: 2M EP + 5 distinct bosses + mastery of a Unique skill.\n"
            + "Repeated kills of one boss type count once. Apex Unique skills are rollable through normal Tensura reincarnation.\n"
            + "Equip skills and toggle passives in Tensura's ability menu. Use /wildhunt status, /wildhunt evolve, and /wildhunt ascend <vritra|demeter|janus>. Use Skills → Ultimate → Synthesis to combine two mastered Uniques."), false);
        return Command.SINGLE_SUCCESS;
    }
    private static int status(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        ctx.getSource().sendSuccess(() -> Component.literal("Wild Hunt | Stage " + HunterData.stage(player)
            + "/3 | EP " + (long) EnergyHelper.getMaxEP(player) + " | Distinct bosses " + HunterData.bossKills(player).size()
            + " | Mastered " + HunterData.mastered(player) + "/8"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Defeated: " + String.join(", ", HunterData.bossKills(player))), false);
        for (Apex apex : Apex.values()) {
            for (var instance : SkillAPI.getSkillsFrom(player).getLearnedSkills()) {
                if (!(instance.getSkill() instanceof ApexSkill skill) || skill.apex != apex) continue;
                var tag = instance.getOrCreateTag();
                String resource = skill.passiveName() + ": " + (instance.isToggled() ? "ON" : "OFF")
                    + " | Upkeep " + (long)skill.balance.upkeep(skill.ultimate) + " aura/s";
                ctx.getSource().sendSuccess(() -> skill.getName().append(": " + resource), false);
            }
        }
        return Command.SINGLE_SUCCESS;
    }
    private static int evolve(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        int stage = HunterData.stage(player);
        if ((stage == 0 && !HunterData.human(player)) || !HunterProgression.canEvolve(stage,
                EnergyHelper.getMaxEP(player), HunterData.bossKills(player).size(), HunterData.mastered(player))) {
            ctx.getSource().sendFailure(Component.literal("Evolution requirements unmet. Use /wildhunt status and /wildhunt guide."));
            return 0;
        }
        return RaceAPI.getRaceFrom(player).evolveRace(WildHunt.HUNTERS.get(stage).get()) ? 1 : 0;
    }
}
