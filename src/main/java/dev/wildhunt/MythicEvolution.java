package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Atomic reward-first replacement shared by menu, commands and old-save migration. */
public final class MythicEvolution {
    private MythicEvolution(){}
    public static final double SYNTHESIS_EP=2_000_000;
    public static List<MythicKind> ultimates(){return List.of(MythicKind.VRITRA,MythicKind.DEMETER,MythicKind.JANUS);}
    public static List<ManasSkill> ingredients(MythicKind result){return switch(result){
        case VRITRA->List.of(WildHunt.UNIQUE.get(Apex.REY_DAU).get(),WildHunt.UNIQUE.get(Apex.NU_UDRA).get());
        case DEMETER->List.of(WildHunt.MYTHIC.get(MythicKind.VERDANT_KEEPER).get(),WildHunt.UNIQUE.get(Apex.JIN_DAHAAD).get());
        case JANUS->List.of(WildHunt.MYTHIC.get(MythicKind.THREADWEAVER).get(),WildHunt.MYTHIC.get(MythicKind.MIRRORBOUND).get());
        default->List.of();
    };}
    public static boolean ready(LivingEntity e,MythicKind result){
        double ep=EnergyHelper.getBaseMaxEP(e);var storage=SkillAPI.getSkillsFrom(e);var recipe=ingredients(result);
        return ultimates().contains(result) && Double.isFinite(ep) && ep>=SYNTHESIS_EP && storage.getSkill(WildHunt.MYTHIC.get(result).get()).isEmpty() && recipe.stream().allMatch(s->storage.getSkill(s).map(i->i.isMastered(e)).orElse(false));
    }
    public static String synthesize(ServerPlayer p,String id){
        var kind=ultimates().stream().filter(k->id.equals(k.id()) || id.equals(WildHunt.id(k.id()).toString())).findFirst();
        if(kind.isEmpty())return "Choose Vritra, Demeter or Janus.";
        var result=kind.get();
        if(!ready(p,result))return result.title+" requires 2M base EP and mastery of both "+ingredients(result).stream().map(s->s.getName().getString()).reduce((a,b)->a+" + "+b).orElse("")+". An owned result cannot be synthesized twice.";
        var skills=SkillAPI.getSkillsFrom(p);var sources=ingredients(result).stream().map(s->skills.getSkill(s).orElseThrow()).toList();
        if(!replace(p,sources,WildHunt.MYTHIC.get(result).get(),false))return "The native skill system rejected synthesis. Your ingredients are unchanged.";
        dev.wildhunt.myths.MythsVisuals.convergence(p.serverLevel(),p.position().add(0,1,0),result.color,0xF4E7C0);
        return "Skill synthesis: "+result.title+". Both mastered ingredients were combined; presets and remaining cooldowns were preserved.";
    }
    public static boolean replace(LivingEntity e,List<ManasSkillInstance> sources,ManasSkill destination,boolean preserveMastery){
        var storage=SkillAPI.getSkillsFrom(e);var existing=storage.getSkill(destination);boolean newly=existing.isEmpty();
        if(newly && !HunterData.learnReward(e,destination))return false;
        var target=storage.getSkill(destination).orElseThrow();int remaining=sources.stream().flatMap(i->i.getCooldownList().stream()).mapToInt(Integer::intValue).max().orElse(0);
        if(preserveMastery){double fraction=sources.stream().mapToDouble(i->i.getMastery()/Math.max(1,i.getMaxMastery())).max().orElse(0);target.setMastery(Math.max(target.getMastery(),fraction*target.getMaxMastery()));}
        for(int mode=0;mode<target.getModes();mode++)target.setCoolDown(Math.max(remaining,target.getCoolDown(mode)),mode);
        if(e instanceof ServerPlayer p){
            var abilities=TensuraStorages.getAbilityFrom(p);
            // Forgetting can clear native slots, so first record all nine presets.
            var slots=new ArrayList<int[]>();
            for(int preset=0;preset<9;preset++)for(int slot=0;slot<3;slot++){
                var value=abilities.getAbilitySlot(preset,slot);if(sources.stream().anyMatch(i->i.getSkill()==value.getSkill()))slots.add(new int[]{preset,slot,value.getMode()});
            }
            for(var source:sources)storage.forgetSkill(source);
            for(var slot:slots)abilities.setAbilitySlot(slot[0],slot[1],destination,Math.clamp(slot[2],0,target.getModes()-1));abilities.markDirty();
        }else for(var source:sources)storage.forgetSkill(source);
        storage.checkAndMarkDirty(target);if(e instanceof io.github.manasmods.manascore.storage.api.StorageHolder holder)holder.manasCore$sync();return true;
    }
    /** Retired registry IDs are retained only to decode old saves. They are hidden and never offered. */
    public static boolean retired(ManasSkill skill){return WildHunt.ULTIMATE.values().stream().anyMatch(s->s.get()==skill);}
    public static void migrate(LivingEntity e){
        var storage=SkillAPI.getSkillsFrom(e);var old=new ArrayList<>(storage.getLearnedSkills()).stream().filter(i->retired(i.getSkill())).toList();
        for(var source:old){
            var skill=(ApexSkill)source.getSkill();
            // Grant an earned replacement immediately; no new recipe gate is imposed on an old Ultimate.
            var kind=switch(skill.apex){case REY_DAU,NU_UDRA->MythicKind.VRITRA;case UTH_DUNA,JIN_DAHAAD->MythicKind.DEMETER;};
            replace(e,List.of(source),WildHunt.MYTHIC.get(kind).get(),true);
        }
    }
}
