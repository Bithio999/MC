package dev.wildhunt.myths;

import dev.wildhunt.*;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class MythsProgression {
    private MythsProgression() {}
    public static String ascend(ServerPlayer player,String sourceId){return ascend(player,sourceId,"aion");}
    public static String ascend(ServerPlayer player,String sourceId,String destination) {
        boolean thoth=destination.equals("thoth");
        if(!thoth && !destination.equals("aion"))return "Unknown Godly destination.";
        var skills=SkillAPI.getSkillsFrom(player);
        var source=MythicEvolution.ultimates().stream().map(k->WildHunt.MYTHIC.get(k).get()).filter(s->s.getRegistryName().toString().equals(sourceId)).findFirst();
        if(source.isEmpty())return "Choose one of your mastered combination Ultimates.";
        io.github.manasmods.manascore.skill.api.ManasSkill target=thoth?WildHunt.MYTHIC.get(MythicKind.THOTH).get():WildHunt.GODLY.get();
        if(skills.getSkill(target).isPresent())return "You already possess "+target.getName().getString()+".";
        var instance=skills.getSkill(source.get());var rune=skills.getSkill(WildHunt.MYTHIC.get(MythicKind.RUNE_SMITH).get());
        if(instance.isEmpty() || !instance.get().isMastered(player) || !DuoSystem.awakened(player) || !Double.isFinite(EnergyHelper.getBaseMaxEP(player)) || EnergyHelper.getBaseMaxEP(player)<MythsConfig.GODLY_EP.get() || thoth && (rune.isEmpty() || !rune.get().isMastered(player)))
            return "Godly ascension requires a mastered combination Ultimate, True Hero or True Demon Lord, and "+Math.round(MythsConfig.GODLY_EP.get())+" base EP."+(thoth?" Thoth also requires a mastered Rune Smith, which is combined into the result.":"");
        var sources=new java.util.ArrayList<io.github.manasmods.manascore.skill.api.ManasSkillInstance>();sources.add(instance.get());if(thoth)sources.add(rune.orElseThrow());
        if(!MythicEvolution.replace(player,sources,target,false))return "The native skill system rejected the ascension; your source skills are unchanged.";
        MythsVisuals.convergence(player.serverLevel(),player.position().add(0,1,0),thoth?0x92E9F7:0xF0CE80,thoth?0xF5D17C:0xB69CFA);
        return "Godly ascension: "+target.getName().getString()+". Your presets and remaining cooldowns are preserved.";
    }
}
