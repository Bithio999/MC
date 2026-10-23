package dev.wildhunt;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Opt-in, native-client review only. Never included by the normal release build. */
@EventBusSubscriber(modid=WildHunt.ID)
public final class OriginReview {
    private static EschatonEntity boss(ServerLevel level){for(var e:level.getAllEntities())if(e instanceof EschatonEntity b && b.isAlive())return b;throw new IllegalStateException("Run /originreview setup first");}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        var root=Commands.literal("originreview").requires(s->s.hasPermission(2));
        root.then(Commands.literal("setup").executes(c->{var l=c.getSource().getLevel();
            for(var e:l.getAllEntities())if(e instanceof EschatonEntity || e instanceof GenesisSealEntity || e instanceof ArtVisualEntity || e.getTags().contains("origin_review"))e.discard();
            var b=EschatonItems.summon(l,new Vec3(0,70,8));if(b==null)return 0;b.setNoAi(true);b.setYRot(180);b.setYBodyRot(180);b.setYHeadRot(180);
            var t=EntityType.IRON_GOLEM.create(l);t.setPos(0,70,36);t.setNoAi(true);t.setInvulnerable(true);t.addTag("origin_review");t.setSilent(true);l.addFreshEntity(t);b.setTarget(t);return 1;}));
        root.then(Commands.literal("form").then(Commands.argument("form",IntegerArgumentType.integer(1,3)).executes(c->{var b=boss(c.getSource().getLevel());int wanted=IntegerArgumentType.getInteger(c,"form");while(b.form()<wanted){b.invulnerableTime=0;b.hurt(b.damageSources().genericKill(),20_000_000);}return 1;})));
        root.then(Commands.literal("cast").then(Commands.argument("mode",IntegerArgumentType.integer(0,7)).executes(c->{var l=c.getSource().getLevel();var b=boss(l);EschatonRites.clear(l,b);var i=SkillAPI.getSkillsFrom(b).getSkill(EschatonContent.DEMIURGE.get()).orElseThrow();int mode=IntegerArgumentType.getInteger(c,"mode");i.setCoolDown(0,mode);if(mode==5)TensuraStorages.getExistenceFrom(b).setMagicule(100_000_000);i.onPressed(b,0,mode);c.getSource().sendSuccess(()->Component.literal("Actual native cast: "+b.isCasting()),false);return b.isCasting()?1:0;})));
        root.then(Commands.literal("break").then(Commands.argument("slot",IntegerArgumentType.integer(0,2)).executes(c->{var l=c.getSource().getLevel();var b=boss(l);int slot=IntegerArgumentType.getInteger(c,"slot");for(var e:l.getAllEntities())if(e instanceof GenesisSealEntity s && s.ownedBy(b) && s.slot()==slot)s.hurt(s.damageSources().genericKill(),1_000_000);return 1;})));
        root.then(Commands.literal("clear").executes(c->{var l=c.getSource().getLevel();EschatonRites.clear(l,boss(l));for(var e:l.getAllEntities())if(e instanceof ArtVisualEntity)e.discard();return 1;}));
        event.getDispatcher().register(root);
    }
}
