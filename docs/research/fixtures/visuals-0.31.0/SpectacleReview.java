package dev.wildhunt;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Opt-in native casts from the initialized review boss; no release registration. */
@EventBusSubscriber(modid=WildHunt.ID)
public final class SpectacleReview {
    private static final Map<UUID,Long> CHANNELS=new HashMap<>();
    private static int playAt=-1,freezeAt=-1;
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("reviewplay").requires(s->s.hasPermission(2)).then(Commands.argument("ticks",IntegerArgumentType.integer(1,400)).executes(c->{playAt=c.getSource().getServer().getTickCount()+40;freezeAt=playAt+IntegerArgumentType.getInteger(c,"ticks");return 1;})));
        event.getDispatcher().register(Commands.literal("aioncast").requires(s->s.hasPermission(2)).then(Commands.argument("mode",IntegerArgumentType.integer(0,3)).executes(c->{var p=c.getSource().getPlayerOrException();HunterData.learnReward(p,WildHunt.GODLY.get());GodlySkill.clearTransient(p);var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();i.setMastery(i.getMaxMastery());int mode=IntegerArgumentType.getInteger(c,"mode");i.setCoolDown(0,mode);i.onPressed(p,0,mode);if(mode==0)CHANNELS.put(p.getUUID(),p.level().getGameTime());return 1;})));
        event.getDispatcher().register(Commands.literal("aionback").requires(s->s.hasPermission(2)).executes(c->{var p=c.getSource().getPlayerOrException();var i=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.GODLY.get()).orElseThrow();i.setCoolDown(0,2);i.onPressed(p,0,2);return 1;}));
        event.getDispatcher().register(Commands.literal("aionreturn").requires(s->s.hasPermission(2)).executes(c->{for(var e:c.getSource().getLevel().getAllEntities())if(e instanceof EschatonEntity b && b.isAlive()){var i=SkillAPI.getSkillsFrom(b).getSkill(WildHunt.GODLY.get()).orElseThrow();i.setCoolDown(0,2);i.onPressed(b,0,2);return 1;}return 0;}));
        event.getDispatcher().register(Commands.literal("aionreview").requires(s->s.hasPermission(2)).then(Commands.argument("mode",IntegerArgumentType.integer(0,3)).executes(c->{
            var level=c.getSource().getLevel();EschatonEntity boss=null;for(var e:level.getAllEntities())if(e instanceof EschatonEntity b && b.isAlive()){boss=b;break;}if(boss==null || boss.getTarget()==null)return 0;
            var target=boss.getTarget();int mode=IntegerArgumentType.getInteger(c,"mode");boss.lookAt(EntityAnchorArgument.Anchor.EYES,mode==1?new net.minecraft.world.phys.Vec3(0,70,30):target.getEyePosition());
            EschatonRites.clear(level,boss);GodlySkill.clearTransient(boss);var i=SkillAPI.getSkillsFrom(boss).getSkill(WildHunt.GODLY.get()).orElseThrow();i.setCoolDown(0,mode);i.onPressed(boss,0,mode);
            if(mode==0)CHANNELS.put(boss.getUUID(),level.getGameTime());return 1;
        })));
    }
    @SubscribeEvent public static void held(ServerTickEvent.Post event){
        var server=event.getServer();if(playAt>=0 && server.getTickCount()>=playAt){server.tickRateManager().setFrozen(false);playAt=-1;}if(freezeAt>=0 && server.getTickCount()>=freezeAt){server.tickRateManager().setFrozen(true);freezeAt=-1;}
        if(CHANNELS.isEmpty())return;var remove=new ArrayList<UUID>();
        for(var entry:CHANNELS.entrySet()){
            net.minecraft.world.entity.LivingEntity owner=null;for(var level:server.getAllLevels())if(level.getEntity(entry.getKey()) instanceof net.minecraft.world.entity.LivingEntity e){owner=e;break;}
            if(owner==null || !owner.isAlive() || owner.level().getGameTime()-entry.getValue()>=110){remove.add(entry.getKey());continue;}
            var i=SkillAPI.getSkillsFrom(owner).getSkill(WildHunt.GODLY.get()).orElseThrow();WildHunt.GODLY.get().onHeld(i,owner,(int)(owner.level().getGameTime()-entry.getValue()),0);
        }for(var id:remove)CHANNELS.remove(id);
    }
}
