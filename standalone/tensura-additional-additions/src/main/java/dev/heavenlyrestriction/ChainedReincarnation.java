package dev.heavenlyrestriction;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.tensura.config.ability.skill.UniqueSkillConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.TagKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Read-only cost inspection. Never casts, learns, toggles or removes a real skill. */
public final class ChainedReincarnation {
    public static final TagKey<ManasSkill> SKILL_TAKING=TagKey.create(SkillAPI.getSkillRegistryKey(),HeavenlyRestriction.id("fettered_skill_taking"));
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
        if(ChainedReincarnationConfig.ALLOWLIST.get().contains(id))return false;
        return ChainedReincarnationConfig.BLOCK_SKILL_TAKING.get() && takesTargetSkills(skill)
            || ChainedReincarnationConfig.BLOCK_MAGICULE_COSTS.get() && hasMagiculeCost(skill,player);
    }
    public static boolean takesTargetSkills(ManasSkill skill){
        var id=skill.getRegistryName();
        // Pride's technique bank is an explicit design exception, including tag additions.
        return id!=null && !id.toString().equals("tensura:pride") && SkillAPI.getSkillRegistry().delegate(id).is(SKILL_TAKING);
    }
    public static boolean hasMagiculeCost(ManasSkill skill,Player player){
        if(nativeInlineCost(skill) || addonInlineCost(skill))return true;
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
    /** Optional addon charges performed inside ability bodies, absent from their cost getters. */
    private static boolean addonInlineCost(ManasSkill skill){
        if(skill.getRegistryName()==null)return false;
        String id=skill.getRegistryName().toString();
        String[] fields=switch(id){
            case "mysticism:subjugator" -> new String[]{"takeoverEPMultiplierRequirement","takeoverEPMultiplierRequirementMastered"};
            case "mysticism:gardener" -> new String[]{"cropMasteryCost","blessingCost","blessingCostMastered"};
            case "tensuramoreskills:authority_of_greed" -> new String[]{"lionheartMagiculeLossPercent"};
            default -> null;
        };
        if(fields==null)return false;
        try{
            Object config;
            if(id.equals("tensuramoreskills:authority_of_greed")){
                var getter=skill.getClass().getDeclaredMethod("cfg");getter.setAccessible(true);config=getter.invoke(null);
            }else{
                var field=skill.getClass().getDeclaredField("CONFIG");field.setAccessible(true);config=field.get(null);
            }
            for(String name:fields){
                var field=config.getClass().getField(name);Object value=field.get(config);
                if(value instanceof net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?> configured)value=configured.get();
                if(!(value instanceof Number number))throw new IllegalStateException("Non-numeric addon cost: "+name);
                double amount=number.doubleValue();if(amount>0 || !Double.isFinite(amount))return true;
            }
            return false;
        }catch(ReflectiveOperationException | RuntimeException ex){
            if(WARNED.add(skill))LOG.warn("Unable to inspect inline addon costs for {}; excluded for Fettered Human. An explicit allowlist entry can override this.",id,ex);
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
