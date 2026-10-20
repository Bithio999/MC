package dev.wildhunt;

import dev.architectury.event.EventResult;
import dev.wildhunt.myths.MythsTiers;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.effect.template.TensuraMobEffectInstance;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Ownership traits shared by every Godly skill, including datapack-classified skills. */
public final class GodlyPassives {
    public static final String DESCRIPTION=" Godly classification passives (always active): +35% damage against non-Godly owners; your damage bypasses resistance and nullification; immunity to Magic Interference from non-Godly skills. These traits do not stack.";
    private GodlyPassives() {}

    public static boolean ownsGodly(Entity entity) {
        return entity instanceof LivingEntity living && SkillAPI.getSkillsFrom(living).getLearnedSkills().stream()
                .anyMatch(instance->MythsTiers.isGodly(instance.getSkill()));
    }

    /** A projectile's shooter is the user; nearby Godly owners never lend their traits to others. */
    public static LivingEntity attacker(DamageSource source) {
        if(source.getEntity() instanceof LivingEntity living)return living;
        if(source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity living)return living;
        return null;
    }

    public static boolean bypassesDefenses(DamageSource source) {return ownsGodly(attacker(source));}

    public static EventResult modifyDamage(Skills storage,LivingEntity target,DamageSource source,Changeable<Float> amount) {
        var caster=attacker(source);
        if(ownsGodly(caster) && HunterData.canHarm(caster,target) && !ownsGodly(target))amount.set(amount.get()*1.35f);
        return EventResult.pass();
    }

    public static boolean blocksInterference(LivingEntity target,MobEffectInstance effect) {
        if(!effect.is(TensuraMobEffects.getReference(TensuraMobEffects.MAGIC_INTERFERENCE)) || !ownsGodly(target))return false;
        var origin=((TensuraMobEffectInstance)effect).tensura$getSourceAbility();
        // Unattributed effects have no Godly authority. Caster ownership cannot promote an ordinary skill.
        return origin==null || origin.isEmpty() || !MythsTiers.isGodly(origin.getSkill());
    }

    public static void effectApplicable(MobEffectEvent.Applicable event) {
        if(blocksInterference(event.getEntity(),event.getEffectInstance()))event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    /** Also cleanse interference that predates learning a Godly skill or was loaded from an older save. */
    public static void tick(LivingEntity owner) {
        var effect=owner.getEffect(TensuraMobEffects.getReference(TensuraMobEffects.MAGIC_INTERFERENCE));
        if(effect!=null && blocksInterference(owner,effect))owner.removeEffect(effect.getEffect());
    }
}
