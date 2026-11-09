package dev.heavenlyrestriction;

import io.github.manasmods.manascore.attribute.api.*;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/** Native melee crits plus exactly one crit multiplier for owned projectiles and ability damage. */
public final class UnboundCritical implements AutoCloseable {
    private static final ThreadLocal<UnboundCritical> ATTACK=new ThreadLocal<>();
    private final UnboundCritical previous;private final Player owner;private Entity criticalTarget;
    private UnboundCritical(Player owner){this.owner=owner;previous=ATTACK.get();ATTACK.set(this);}
    public static UnboundCritical melee(Player owner){return new UnboundCritical(owner);}
    public static boolean enabled(LivingEntity owner){return owner instanceof Player && ChainedState.ultimate(owner) && ChainedState.mastered(owner);}
    public static void critical(CriticalHitEvent event){
        if(!enabled(event.getEntity()))return;
        event.setCriticalHit(true);event.setDamageMultiplier(Math.max(event.getDamageMultiplier(),(float)event.getEntity().getAttributeValue(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER)));
        var frame=ATTACK.get();if(frame!=null && frame.owner==event.getEntity())frame.criticalTarget=event.getTarget();
    }
    public static void noteCritical(Player owner,Entity target){var frame=ATTACK.get();if(frame!=null && frame.owner==owner)frame.criticalTarget=target;}
    public static boolean meleeCritical(LivingEntity owner,LivingEntity target,DamageSource source){var frame=ATTACK.get();return frame!=null && frame.owner==owner && frame.criticalTarget==target && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && source.getDirectEntity()==owner && ((TensuraDamageSource)source).tensura$getAbilityInstance()==null;}
    public static void apply(LivingEntity owner,LivingEntity target,DamageSource source,Changeable<Float> amount){
        if(!enabled(owner) || owner==target || amount.get()<=0)return;
        var frame=ATTACK.get();
        if(frame!=null && frame.owner==owner && frame.criticalTarget==target && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && source.getDirectEntity()==owner && ((TensuraDamageSource)source).tensura$getAbilityInstance()==null)return;
        amount.set((float)(amount.get()*owner.getAttributeValue(ManasCoreAttributes.CRITICAL_DAMAGE_MULTIPLIER)));
        ManasCoreAttributeUtils.triggerCriticalAttackEffect(target,owner);
    }
    @Override public void close(){if(previous==null)ATTACK.remove();else ATTACK.set(previous);}
}
