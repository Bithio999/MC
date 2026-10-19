package dev.wildhunt.myths;

import dev.wildhunt.*;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class MythsProgression {
    private MythsProgression() {}
    public static String ascend(ServerPlayer player,String sourceId) {
        var skills=SkillAPI.getSkillsFrom(player);
        var source=WildHunt.ULTIMATE.values().stream().map(s->s.get()).filter(s->s.getRegistryName().toString().equals(sourceId)).findFirst();
        if(source.isEmpty())return "Choose one of your mastered Myths Ultimates.";
        var instance=skills.getSkill(source.get());
        if(skills.getSkill(WildHunt.GODLY.get()).isPresent())return "You already possess Aion.";
        if(instance.isEmpty() || !instance.get().isMastered(player) || !DuoSystem.awakened(player) || !Double.isFinite(EnergyHelper.getBaseMaxEP(player)) || EnergyHelper.getBaseMaxEP(player)<MythsConfig.GODLY_EP.get())
            return "Godly ascension requires a mastered Myths Ultimate, True Hero or True Demon Lord, and "+Math.round(MythsConfig.GODLY_EP.get())+" base EP.";
        var abilities=TensuraStorages.getAbilityFrom(player);
        var slots=new java.util.ArrayList<int[]>();
        for(int p=0;p<9;p++)for(int s=0;s<3;s++)if(abilities.getAbilitySlot(p,s).getSkill()==source.get())slots.add(new int[]{p,s,abilities.getAbilitySlot(p,s).getMode()});
        int remaining=instance.get().getCooldownList().stream().mapToInt(Integer::intValue).max().orElse(0);
        if(!HunterData.learnReward(player,WildHunt.GODLY.get()))return "The native skill system rejected the ascension; your source skill is unchanged.";
        var evolved=skills.getSkill(WildHunt.GODLY.get()).orElseThrow();
        for(int mode=0;mode<4;mode++)evolved.setCoolDown(remaining,mode);
        skills.forgetSkill(instance.get());
        for(var slot:slots)abilities.setAbilitySlot(slot[0],slot[1],WildHunt.GODLY.get(),Math.clamp(slot[2],0,3));
        abilities.markDirty(); skills.checkAndMarkDirty(evolved); ((io.github.manasmods.manascore.storage.api.StorageHolder)player).manasCore$sync();
        MythsVisuals.convergence(player.serverLevel(),player.position().add(0,1,0),0xF0CE80,0xB69CFA);
        return "Godly ascension: Aion, God of Causality. Your presets and remaining cooldown are preserved.";
    }
}
