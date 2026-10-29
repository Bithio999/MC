package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import java.util.*;

/** One enemy hit, one terrain-piercing slash. Stances never survive respawn, logout or a world change. */
public final class UnboundCounter {
    private static final net.minecraft.resources.ResourceLocation ROOT=HeavenlyRestriction.id("unbound_counter_root");
    private record Stance(ServerLevel level,long until,double spent,ArtVisualEntity visual){}
    private static final Map<LivingEntity,Stance> STANCES=Collections.synchronizedMap(new WeakHashMap<>());
    private UnboundCounter(){}
    public static boolean active(LivingEntity e){
        if(e.level().isClientSide())return SkillAPI.getSkillsFrom(e).getSkill(HeavenlyRestriction.UNBOUND.get()).filter(i->i.getTag()!=null && i.getTag().getLong("counter_active_until")>e.level().getGameTime() && i.getTag().getString("counter_dimension").equals(e.level().dimension().location().toString())).isPresent();
        var s=STANCES.get(e);return s!=null && s.level==e.level() && s.until>e.level().getGameTime();
    }
    public static void press(ManasSkillInstance i,LivingEntity e){
        if(!(e.level() instanceof ServerLevel level))return;
        if(active(e)){clear(e);return;}
        clear(e);if(i.getMastery()<0 || i.onCoolDown(1) || !i.canInteractSkill(e) || !i.canActivateSkill(e,1))return;
        var energy=TensuraStorages.getExistenceFrom(e);double spent=energy.getAura()*.1,cost=SchemaArt.cost(e,spent);
        if(!Double.isFinite(cost) || spent<=0 || energy.getAura()<cost)return;
        energy.setAura(energy.getAura()-cost);energy.markDirty();i.setCoolDown(10,1);i.addMasteryPoint(e);SkillAPI.getSkillsFrom(e).checkAndMarkDirty(i);
        var visual=ArtVisualEntity.spawn(level,ArtVisualEntity.UNBOUND_STANCE,e.position(),e.getLookAngle(),60);visual.setCaster(e);
        STANCES.put(e,new Stance(level,level.getGameTime()+60,spent,visual));
        i.getOrCreateTag().putLong("counter_active_until",level.getGameTime()+60);i.getOrCreateTag().putString("counter_dimension",level.dimension().location().toString());i.markDirty();SkillAPI.getSkillsFrom(e).checkAndMarkDirty(i);
        var speed=e.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.addOrUpdateTransientModifier(new AttributeModifier(ROOT,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        e.setDeltaMovement(0,e.getDeltaMovement().y,0);e.hurtMarked=true;
        level.playSound(null,e.blockPosition(),SoundEvents.ANVIL_LAND,SoundSource.PLAYERS,.45f,1.6f);
    }
    public static void tick(LivingEntity e){
        if(e.level().isClientSide())return;
        var s=STANCES.get(e);if(s==null){clear(e);return;}
        if(!active(e) || !e.isAlive() || !ChainedState.ultimate(e) || ChainedState.instance(e).filter(i->i.canInteractSkill(e) && i.canActivateSkill(e,1)).isEmpty()){clear(e);return;}
        e.setDeltaMovement(0,e.getDeltaMovement().y,0);
    }
    public static void clear(LivingEntity e){
        var stance=STANCES.remove(e);if(stance!=null)stance.visual.discard();
        SkillAPI.getSkillsFrom(e).getSkill(HeavenlyRestriction.UNBOUND.get()).ifPresent(i->{if(i.getTag()!=null && i.getTag().contains("counter_active_until")){i.getTag().remove("counter_active_until");i.getTag().remove("counter_dimension");i.markDirty();SkillAPI.getSkillsFrom(e).checkAndMarkDirty(i);}});
        var speed=e.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(ROOT);
    }
    public static float damage(float negated,double spent){return (float)(10*Math.max(0,negated)+6*Math.sqrt(Math.max(0,spent)));}
    public static void incoming(LivingIncomingDamageEvent event){if(intercept(event.getEntity(),event.getSource(),event.getAmount()))event.setCanceled(true);}
    public static boolean intercept(LivingEntity e,net.minecraft.world.damagesource.DamageSource damageSource,float amount){
        if(!active(e) || amount<=0 || !Float.isFinite(amount) || (damageSource.is(DamageTypes.GENERIC_KILL) || damageSource.is(DamageTypes.FELL_OUT_OF_WORLD)))return false;
        var attacker=RestrictionData.attacker(damageSource);if(attacker==null || !RestrictionData.canHarm(e,attacker) || !ChainedState.ultimate(e) || ChainedState.instance(e).filter(i->i.canInteractSkill(e) && i.canActivateSkill(e,1)).isEmpty())return false;
        var stance=STANCES.get(e);var i=ChainedState.instance(e).orElseThrow();float incoming=amount;
        clear(e);var level=(ServerLevel)e.level();
        Vec3 start=e.getBoundingBox().getCenter(),aim=attacker.getBoundingBox().getCenter().subtract(start);
        if(aim.lengthSqr()<1e-6)aim=e.getLookAngle();Vec3 travel=aim.normalize().scale(Math.min(64,Math.max(16,aim.length()+4)));double length=travel.lengthSqr();
        Nullification.sweep(e,start,start.add(travel),3);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(start,start.add(travel)).inflate(3),t->RestrictionData.canHarm(e,t))){
            Vec3 center=target.getBoundingBox().getCenter(),closest=start.add(travel.scale(Math.clamp(center.subtract(start).dot(travel)/length,0,1)));
            if(center.distanceToSqr(closest)>9)continue;
            ChainedWeaponArts.hit(i,e,target,1,damage(incoming,stance.spent),stance.spent);
        }
        ArtVisualEntity.spawn(level,ArtVisualEntity.UNBOUND_SLASH,start,travel,18);
        level.playSound(null,e.blockPosition(),SoundEvents.TRIDENT_THUNDER.value(),SoundSource.PLAYERS,1.5f,1.35f);
        e.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);return true;
    }
}
