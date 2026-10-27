package dev.heavenlyrestriction;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/** Persistent pact/flight state and attribution; no boss or dual-race data. */
public final class RestrictionData {
    private RestrictionData(){}
    public static CompoundTag of(LivingEntity e){
        var root=e.getPersistentData();if(!root.contains(HeavenlyRestriction.ID))root.put(HeavenlyRestriction.ID,new CompoundTag());
        return root.getCompound(HeavenlyRestriction.ID);
    }
    public static LivingEntity attacker(DamageSource source){
        if(source.getEntity() instanceof LivingEntity living)return living;
        return source.getDirectEntity() instanceof Projectile p && p.getOwner() instanceof LivingEntity living?living:null;
    }
    public static boolean learnReward(LivingEntity e,ManasSkill skill){
        var i=skill.createDefaultInstance();i.getOrCreateTag().putBoolean("NoMagiculeCost",true);
        return SkillAPI.getSkillsFrom(e).learnSkill(i);
    }
    public static boolean canHarm(LivingEntity caster,LivingEntity target){
        if(caster==target || !target.isAlive() || caster.isAlliedTo(target))return false;
        if(target instanceof Player p && (p.isCreative() || p.isSpectator()))return false;
        if(caster instanceof Player p && target instanceof Player other && (!p.canHarmPlayer(other) || caster.getServer()!=null && !caster.getServer().isPvpAllowed()))return false;
        return !(target instanceof TamableAnimal pet && pet.isOwnedBy(caster));
    }
    public record Growth(double aura,double magicules){}
    public static Growth growth(LivingEntity e,double aura,double magicules){
        return RaceAPI.getRaceFrom(e).getRace().filter(i->i.getRace() instanceof RestrictedRace).isPresent()
            ?new Growth((aura+magicules)*.9,(aura+magicules)*.1):new Growth(aura,magicules);
    }
}
