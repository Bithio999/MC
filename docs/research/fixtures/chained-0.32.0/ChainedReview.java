package dev.wildhunt;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Disposable Myths Validation world only; excluded from every normal release build. */
@EventBusSubscriber(modid=WildHunt.ID)
public final class ChainedReview {
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("chainedreview").requires(s->s.hasPermission(2))
            .then(Commands.literal("setup").executes(c->{var p=c.getSource().getPlayerOrException();var l=p.serverLevel();
                for(var e:com.google.common.collect.Lists.newArrayList(l.getAllEntities()))if(e instanceof ArtVisualEntity || e instanceof EschatonEntity || e instanceof AnankeEntity || e instanceof GenesisSealEntity)e.discard();
                for(var i:java.util.List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(i);
                p.removeAllEffects();p.setGameMode(GameType.CREATIVE);EnergyHelper.setBaseMaxEP(p,1000);RaceAPI.getRaceFrom(p).setRace(WildHunt.RESTRICTED.getFirst().get(),false);
                var energy=TensuraStorages.getExistenceFrom(p);energy.setAura(EnergyHelper.getMaxAura(p));energy.setMagicule(EnergyHelper.getMaxMagicule(p));energy.setSleepModeTime(-1);p.teleportTo(0,70,0);p.setYRot(-90);p.setXRot(0);
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){l.getChunk(x,z);l.setChunkForced(x,z,true);}return 1;
            }))
            .then(Commands.literal("cast").executes(c->{var p=c.getSource().getPlayerOrException();p.teleportTo(0,70,0);p.setYRot(-90);p.setXRot(0);var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.CHAINED.get()).orElseThrow();i.setCoolDown(0,0);double before=TensuraStorages.getExistenceFrom(p).getAura();i.onPressed(p,0,0);
                c.getSource().getServer().sendSystemMessage(net.minecraft.network.chat.Component.literal("Chained native-player review: travelled="+p.getX()+" aura spent="+(before-TensuraStorages.getExistenceFrom(p).getAura())));
                p.teleportTo(21,77,-17);p.lookAt(EntityAnchorArgument.Anchor.EYES,new Vec3(8,71,0));return 1;
            })));
    }
}
