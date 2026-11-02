package dev.heavenlyrestriction.exponentiation;

import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.*;

public final class Exponentiation {
    public static final double POWER=1.2,LIMIT=1e150;public static final float FLOAT_LIMIT=1e30f;
    private static final ThreadLocal<Set<LivingEntity>> CHECKING=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final Map<Attribute,Rule> RULES=new java.util.concurrent.ConcurrentHashMap<>();
    private static final String WAS_ACTIVE="heavenlyrestriction.exponentiation_active";
    public record Rule(boolean included,int role,String exclusion){}
    public static boolean active(LivingEntity owner){
        if(owner==null || owner.getAttributes()==null || owner instanceof net.minecraft.world.entity.player.Player player && player.getAbilities()==null || owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer && serverPlayer.gameMode==null)return false;var owned=SkillAPI.getSkillsFrom(owner).getSkill(HeavenlyRestriction.EXPONENTIATION.get());if(owned.isEmpty() || owned.get().getMastery()<0 || !owner.isAlive() || owner.isSpectator())return false;var checking=CHECKING.get();if(!checking.add(owner))return false;
        try{return owned.filter(i->i.getMastery()>=0 && HeavenlyRestriction.EXPONENTIATION.get().isInSlot(owner,i)).isPresent();}
        finally{checking.remove(owner);}
    }
    /** Positive fractional values are retained: a beneficial-only skill must never turn 0.5 into 0.25. */
    public static double amplify(double value){if(Double.isNaN(value))return 0;if(value<=1)return value;return Math.min(LIMIT,Math.pow(value,POWER));}
    public static float amplifyFloat(double value){return (float)Math.min(FLOAT_LIMIT,amplify(value));}
    public static Rule rule(Attribute attribute){return RULES.computeIfAbsent(attribute,a->a.getMergedStyle(true).getValue()==0xF93131?new Rule(false,0,"Higher values are harmful"):classify(BuiltInRegistries.ATTRIBUTE.getKey(a)));}
    private static Rule classify(ResourceLocation id){String p=id.getPath().replace("generic.","");
        if(p.equals("max_aura") || p.equals("max_magicule"))return new Rule(false,0,"Aura/magicule capacity (not amplified)");
        if(p.equals("aura_gain") || p.equals("magicule_gain"))return new Rule(false,0,"Consolidated EP gain; no separate pool multiplier");
        if(p.equals("gravity") || p.endsWith("_gravity") || p.equals("view_zoom") || p.matches(".*(movement|flying|flight|swim|sneak|glid|lava_speed|water_speed|sprint|walk_speed|run_speed|mount_speed|dash_speed|dash_distance|warp_shot|jump|step_height|safe_fall|fall_damage|knockback|velocity|teleport|travel|dodge_strength|height_multiplier|width_multiplier|scale).*"))return new Rule(false,0,"Movement/body geometry");
        if(p.matches(".*(cost|cooldown|consumption|requirement|penalty|damage_taken|limited_spiritual|exhaustion|spawn|reinforcement).*"))return new Rule(false,0,"Costs, limits or nonnumeric/binary state");
        int role=0;
        if(p.contains("attack_damage") || p.endsWith("_boost") || p.contains("magic_power") || p.contains("damage_multiplier") || p.contains("crit_damage") || p.contains("sweeping_damage"))role=ExponentiationContext.DAMAGE;
        if(p.startsWith("max_") || p.equals("max_health"))role|=ExponentiationContext.DAMAGE|ExponentiationContext.CAPACITY;
        if(p.contains("capacity") || p.equals("oxygen_bonus"))role|=ExponentiationContext.CAPACITY;
        if(p.contains("regeneration") || p.contains("healing") || p.contains("recovery"))role|=ExponentiationContext.RECOVERY;
        if(p.equals("aura_gain") || p.equals("magicule_gain"))role|=ExponentiationContext.GROWTH;
        if(p.contains("mastery_gain") || p.contains("learning_gain"))role|=ExponentiationContext.LEARNING;
        if(p.contains("mining") || p.contains("block_break"))role|=ExponentiationContext.MINING;
        return new Rule(true,role,"");
    }
    public static double attribute(LivingEntity owner,AttributeInstance instance,double value){var rule=rule(instance.getAttribute().value());if(!rule.included || value<=1 || ExponentiationContext.rawAttribute(owner,rule.role) || !active(owner))return value;
        double result=amplify(value);String path=BuiltInRegistries.ATTRIBUTE.getKey(instance.getAttribute().value()).getPath();
        if(path.contains("chance") && instance.getAttribute().value() instanceof RangedAttribute ranged)result=Math.min(ranged.getMaxValue(),result);
        if(instance.getAttribute().equals(Attributes.MAX_HEALTH))result=Math.min(FLOAT_LIMIT,result);
        if(instance.getAttribute().equals(TensuraAttributes.MAX_MAGICULE))result=ChainedState.magiculeCeiling(owner,result);
        return result;
    }
    public static double gain(LivingEntity owner,double before,double proposed,double cap){
        if(proposed<=before || !active(owner) || ExponentiationContext.restoring(owner))return proposed;
        return Math.max(before,Math.min(cap,before+amplify(proposed-before)));
    }
    public static double capacity(LivingEntity owner,java.util.function.DoubleSupplier read){try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.FINAL)){return read.getAsDouble();}}
    /** Removing a temporary capacity lens never leaves usable over-cap HP or reserves. Bases and modifiers are not rewritten. */
    public static void tick(EntityTickEvent.Post event){if(!(event.getEntity() instanceof LivingEntity owner) || owner.level().isClientSide())return;boolean was=owner.getPersistentData().getBoolean(WAS_ACTIVE),now=active(owner);if(!was && !now)return;owner.getPersistentData().putBoolean(WAS_ACTIVE,now);
        if((was && !now) || (now && !owner.getPersistentData().getBoolean("heavenlyrestriction.exponentiation_power_v3")))try(var scope=ExponentiationContext.raw(owner,ExponentiationContext.RESTORE)){owner.setAirSupply(Math.min(owner.getAirSupply(),owner.getMaxAirSupply()));owner.setHealth(Math.min(owner.getHealth(),owner.getMaxHealth()));var e=TensuraStorages.getExistenceFrom(owner);e.setSpiritualHealth(Math.min(e.getSpiritualHealth(),owner.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)));e.setAura(Math.min(e.getAura(),EnergyHelper.getMaxAura(owner)));e.setMagicule(Math.min(e.getMagicule(),EnergyHelper.getMaxMagicule(owner)));e.markDirty();owner.getPersistentData().putBoolean("heavenlyrestriction.exponentiation_power_v3",true);}
    }
    private Exponentiation(){}
}
