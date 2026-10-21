package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.effect.EffectStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;

/** Unwritten Renewal: delayed, interruptible, funded recovery rather than instant regeneration. */
final class AnankeRecovery {
    static final int COOLDOWN=600,WINDUP=40,LAST_PULSE=100;
    private long readyAt,lastDamageAt,start;
    private long lastPulse=-1;
    private boolean active,observed;
    private float previousHP;
    private double previousSHP;
    private ArtVisualEntity visual;
    boolean active(){return active;}
    private boolean permitted(AnankeEntity boss){
        var skill=SkillAPI.getSkillsFrom(boss).getSkill(MythBosses.MOIRA.get()).orElse(null);
        return boss.isAlive() && skill!=null && skill.canInteractSkill(boss) && skill.canActivateSkill(boss,0)
            && !SkillUtils.shouldCancelHealing(boss) && !SkillUtils.shouldCancelInteraction(boss);
    }
    void observeDamage(AnankeEntity boss){
        double shp=TensuraStorages.getExistenceFrom(boss).getSpiritualHealth();
        if(observed && (boss.getHealth()<previousHP || shp<previousSHP))damaged(boss);
        observed=true;previousHP=boss.getHealth();previousSHP=shp;
    }
    void damaged(AnankeEntity boss){lastDamageAt=boss.level().getGameTime();if(active)cancel(boss);}
    boolean tryBegin(AnankeEntity boss){
        long now=boss.level().getGameTime();var energy=TensuraStorages.getExistenceFrom(boss);
        if(boss.isCasting() || now<readyAt || now-lastDamageAt<80 || !permitted(boss) || boss.getTarget()==null || !boss.eligible(boss.getTarget())
            || energy.getMagicule()<5_000 || boss.getHealth()>=boss.getMaxHealth()*.85 && energy.getSpiritualHealth()>=boss.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)*.8)return false;
        energy.setMagicule(energy.getMagicule()-5_000);energy.markDirty();readyAt=now+COOLDOWN;start=now;lastPulse=-1;active=true;boss.setRite(5);
        var effect=ArtVisualEntity.spawn(boss.level(),ArtVisualEntity.ANANKE_RENEWAL,boss.position(),new Vec3(5,0,0),120);effect.setCaster(boss);visual=effect;
        boss.level().playSound(null,boss.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.HOSTILE,1.4f,.7f);return true;
    }
    void tick(AnankeEntity boss){
        if(!active)return;
        if(!permitted(boss)){cancel(boss);return;}
        boss.setDeltaMovement(boss.getDeltaMovement().scale(.3));long age=boss.level().getGameTime()-start;
        if(age>=WINDUP && age<=LAST_PULSE && (age-WINDUP)%20==0 && age!=lastPulse){
            lastPulse=age;
            if(!pulse(boss)){cancel(boss);return;}
        }
        if(age>=LAST_PULSE){active=false;visual=null;boss.setRite(0);}
    }
    private boolean pulse(AnankeEntity boss){
        var energy=TensuraStorages.getExistenceFrom(boss);
        double maxHP=EffectStorage.getSeveranceMaxHealth(boss),maxSHP=boss.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);
        double hp=Math.min(Math.max(0,maxHP-boss.getHealth()),boss.getMaxHealth()*.03);
        double shp=Math.min(Math.max(0,maxSHP-energy.getSpiritualHealth()),maxSHP*.03);
        double requested=hp*50+shp*20;if(requested<=0)return false;
        double budget=Math.max(0,energy.getMagicule()),fraction=Math.min(1,budget/requested);
        if(fraction<=0)return false;
        hp*=fraction;shp*=fraction;double reserved=hp*50+shp*20;
        energy.setMagicule(budget-reserved);
        float before=boss.getHealth();if(hp>0)boss.heal((float)hp);
        // Refund suppressed/reduced HP healing; SHP is governed by the same native healing gate.
        energy.setMagicule(Math.min(energy.getMagicule()+Math.max(0,hp-(boss.getHealth()-before))*50,boss.getAttributeValue(TensuraAttributes.MAX_MAGICULE)));
        energy.setSpiritualHealth(Math.min(maxSHP,energy.getSpiritualHealth()+shp));energy.markDirty();
        // Do not misread a native severance clamp as a hostile hit next tick.
        previousHP=boss.getHealth();previousSHP=energy.getSpiritualHealth();
        return true;
    }
    void cancel(AnankeEntity boss){
        if(visual!=null)visual.discard();
        if(boss.level() instanceof ServerLevel level)for(var tail:level.getEntitiesOfClass(ArtVisualEntity.class,boss.getBoundingBox().inflate(192),v->v.ownedBy(boss)
            && v.kind()>=ArtVisualEntity.ANANKE_RENEWAL && v.kind()<=ArtVisualEntity.ANANKE_SHIFT))tail.discard();
        visual=null;if(active)boss.setRite(0);active=false;
    }
    void reset(AnankeEntity boss){cancel(boss);readyAt=boss.level().getGameTime()+COOLDOWN;lastDamageAt=boss.level().getGameTime();observed=false;}
    void save(AnankeEntity boss,CompoundTag tag){tag.putLong("myth_renewal_remaining",Math.clamp(readyAt-boss.level().getGameTime(),0,COOLDOWN));}
    void load(AnankeEntity boss,CompoundTag tag){
        readyAt=boss.level().getGameTime()+Math.clamp(tag.getLong("myth_renewal_remaining"),0,COOLDOWN);
        lastDamageAt=boss.level().getGameTime();active=false;visual=null;observed=false;
    }
}
