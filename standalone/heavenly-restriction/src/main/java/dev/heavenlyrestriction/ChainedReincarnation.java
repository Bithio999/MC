package dev.heavenlyrestriction;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.tensura.config.ability.skill.UniqueSkillConfig;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Read-only cost inspection. Never casts, learns, toggles or removes a real skill. */
public final class ChainedReincarnation {
    private static final Logger LOG=LoggerFactory.getLogger(ChainedReincarnation.class);
    private static final Set<ManasSkill> WARNED=Collections.newSetFromMap(new IdentityHashMap<>());
    private ChainedReincarnation(){}
    public static boolean applies(Player player){
        return RaceAPI.getRaceFrom(player).getRace().filter(i->i.getRace() instanceof RestrictedRace race && race.stage==0).isPresent();
    }
    public static List<ManasSkill> filter(List<ManasSkill> pool,Player player){
        if(!applies(player))return pool;
        return pool.stream().filter(skill->!blocked(skill,player)).toList();
    }
    public static boolean blocked(ManasSkill skill,Player player){
        String id=skill.getRegistryName().toString();
        if(ChainedReincarnationConfig.BLACKLIST.get().contains(id))return true;
        if(!ChainedReincarnationConfig.BLOCK_MAGICULE_COSTS.get() || ChainedReincarnationConfig.ALLOWLIST.get().contains(id))return false;
        return hasMagiculeCost(skill,player);
    }
    public static boolean hasMagiculeCost(ManasSkill skill,Player player){
        if(nativeInlineCost(skill))return true;
        if(!(skill instanceof TensuraSkill tensura))return false;
        try{
            for(boolean mastered:new boolean[]{false,true})for(boolean toggled:new boolean[]{false,true}){
                // Fresh, detached instances cannot alter owned mastery, tags or toggle effects.
                var probe=skill.createDefaultInstance();probe.setMastery(mastered?probe.getMaxMastery():0);probe.setToggled(toggled);
                int modes=Math.max(1,skill.getModes(probe));
                if(modes>256)throw new IllegalStateException("Unbounded skill mode count: "+modes);
                for(int mode=0;mode<modes;mode++){
                    double cost=tensura.getMagiculeCost(player,probe,mode);
                    if(cost>0 || !Double.isFinite(cost))return true;
                }
            }
            return false;
        }catch(RuntimeException ex){
            if(WARNED.add(skill))LOG.warn("Unable to inspect reincarnation costs for {}; excluded for Fettered Human. An explicit allowlist entry can override this.",skill.getRegistryName(),ex);
            return true;
        }
    }
    private static boolean positive(double... values){for(double value:values)if(value>0)return true;return false;}
    /** Pinned native effects that spend MP without exposing that charge through getMagiculeCost. */
    private static boolean nativeInlineCost(ManasSkill skill){
        if(skill.getRegistryName()==null)return false;
        var c=(UniqueSkillConfig)ConfigRegistry.getConfig(UniqueSkillConfig.class);
        return switch(skill.getRegistryName().toString()){
            case "tensura:reaper" -> true; // Infinite Eater pays the targeted entity's current EP in MP.
            case "tensura:reverser" -> positive(c.Reverser.magiculeCostCurse);
            case "tensura:unyielding" -> positive(c.Unyielding.magiculeBackupCost);
            case "tensura:chef" -> positive(c.Chef.magiculeCostHP,c.Chef.magiculeCostHPMastered);
            case "tensura:healer" -> positive(c.Healer.magiculeCostHP,c.Healer.magiculeCostHPMastered,c.Healer.magiculeCostSHP);
            case "tensura:lust" -> positive(c.Lust.magiculeCostHP,c.Lust.magiculeCostHPMastered);
            case "tensura:berserk" -> positive(c.Berserk.magiculeCostFlameAura);
            case "tensura:greed" -> positive(c.Greed.magiculeCostFlareAlly,c.Greed.magiculeCostFlareAttack);
            case "tensura:infinity_prison" -> positive(c.InfinityPrison.magiculeCostGuard,c.InfinityPrison.magiculeCostImprisonTarget);
            case "tensura:traveler" -> positive(c.Traveler.magiculeCostTeleport,c.Traveler.magiculeCostPortal,c.Traveler.magiculeCostMotion);
            case "tensura:suppressor" -> positive(c.Suppressor.magiculeCostMotion,c.Suppressor.magiculeCostPortal);
            case "tensura:sloth" -> positive(c.Sloth.restAllyCost);
            default -> false;
        };
    }
}
