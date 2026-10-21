package dev.wildhunt;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.boss.BossFightConfigManager;
import io.github.manasmods.tensura.storage.boss.template.BossFightInstance;

public final class MythBossCommands {
    private MythBossCommands(){}
    public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("myths").then(Commands.literal("boss").requires(s->s.hasPermission(2))
            .then(Commands.literal("summon").executes(c->{var source=c.getSource();var at=source.getPosition().add(source.getEntity()==null?new Vec3(5,0,0):source.getEntity().getLookAngle().multiply(1,0,1).normalize().scale(8));
                if(AnankeItems.summon(source.getLevel(),at)==null){source.sendFailure(Component.literal("No room for Ananke at the summon position."));return 0;}source.sendSuccess(()->Component.literal("Summoned Ananke: 30M EP. Survival: use a Writ of Last Dawn on a lodestone."),false);return 1;}))
            .then(Commands.literal("arena").executes(c->{var source=c.getSource();var holder=TensuraStorages.getBossFightHolder(source.getServer().overworld());
                if(holder.getBossFight("ananke")!=null){source.sendFailure(Component.literal("The native 'ananke' arena already exists. Edit it with /bossFight."));return 0;}
                var center=net.minecraft.core.BlockPos.containing(source.getPosition());var fight=new BossFightInstance(center,source.getLevel().dimension(),48);
                fight.setBossType(WildHunt.id("ananke"));fight.setBossPosition(center.above(1));fight.setEntrance(center.offset(0,0,24));fight.setMaxPlayer(4);fight.setStartDelay(100);fight.setTimer(18000);fight.setForceExitOnLeave(false);
                holder.addBossFight("ananke",fight);holder.markDirty();new BossFightConfigManager(source.getServer()).saveBossFight("ananke",fight);
                source.sendSuccess(()->Component.literal("Created native /bossFight profile 'ananke' here (48-block arena, up to four players). Prepare the arena, then use /bossFight join @s ananke."),false);return 1;}))));
    }
}
