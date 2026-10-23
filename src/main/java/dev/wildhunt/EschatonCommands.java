package dev.wildhunt;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.boss.BossFightConfigManager;
import io.github.manasmods.tensura.storage.boss.template.BossFightInstance;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class EschatonCommands {
    private EschatonCommands(){}
    public static void register(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("myths").then(Commands.literal("boss").requires(s->s.hasPermission(2)).then(Commands.literal("eschaton")
        .then(Commands.literal("summon").executes(c->{var s=c.getSource();var direction=s.getEntity()==null?new Vec3(0,0,1):s.getEntity().getLookAngle().multiply(1,0,1).normalize();var b=EschatonItems.summon(s.getLevel(),s.getPosition().add(direction.scale(12)));if(b==null){s.sendFailure(Component.literal("Eschaton requires hostile difficulty and an open seven-block-high space."));return 0;}s.sendSuccess(()->Component.literal("Summoned Eschaton, the Last Myth: 1,000,000,000 total EP."),false);return 1;}))
        .then(Commands.literal("arena").executes(c->{var s=c.getSource();var holder=TensuraStorages.getBossFightHolder(s.getServer().overworld());if(holder.getBossFight("eschaton")!=null){s.sendFailure(Component.literal("The 'eschaton' arena already exists. Edit it with /bossFight."));return 0;}
            var center=net.minecraft.core.BlockPos.containing(s.getPosition());var fight=new BossFightInstance(center,s.getLevel().dimension(),96);fight.setBossType(WildHunt.id("eschaton"));fight.setBossPosition(center.above(1));fight.setEntrance(center.offset(0,0,72));fight.setMaxPlayer(8);fight.setStartDelay(200);fight.setTimer(60000);fight.setForceExitOnLeave(false);holder.addBossFight("eschaton",fight);holder.markDirty();new BossFightConfigManager(s.getServer()).saveBossFight("eschaton",fight);s.sendSuccess(()->Component.literal("Created native 'eschaton' arena: 96-block radius, up to eight players. Prepare the arena, then /bossFight join @s eschaton."),false);return 1;})))));}
}
