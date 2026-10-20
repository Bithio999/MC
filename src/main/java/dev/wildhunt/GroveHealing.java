package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import java.util.*;

/** Reserve a pulse's aura, share funding fairly, then refund unperformed healing. */
final class GroveHealing {
    private GroveHealing() {}
    private record Plan(LivingEntity target,double hp,double shp) {}
    private record Quota(LivingEntity target,float hp) {}
    private static final ThreadLocal<Quota> QUOTA=new ThreadLocal<>();

    static double hpRate(ManasSkillInstance i){return 40-10*mastery(i);}
    static double shpRate(ManasSkillInstance i){return 60-20*mastery(i);}
    private static double mastery(ManasSkillInstance i){return Math.clamp(i.getMastery()/Math.max(1,i.getMaxMastery()),0,1);}
    private static double maxHP(LivingEntity e){return Math.max(0,Math.min(e.getMaxHealth(),io.github.manasmods.tensura.storage.effect.EffectStorage.getSeveranceMaxHealth(e)));}
    private static double maxSHP(LivingEntity e){return Math.max(0,e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));}

    /** Only a synchronous Grove heal is capped; unrelated healing is untouched. */
    static void limitHeal(LivingHealEvent event){
        var quota=QUOTA.get();
        if(quota!=null && event.getEntity()==quota.target())event.setAmount(Math.min(event.getAmount(),quota.hp()));
    }

    static void pulse(ManasSkillInstance i,LivingEntity caster,List<LivingEntity> recipients){
        double hpRate=hpRate(i),shpRate=shpRate(i),price=0;
        var plans=new ArrayList<Plan>();
        for(var target:recipients){
            if(!target.isAlive() || SkillUtils.shouldCancelHealing(target))continue;
            var energy=TensuraStorages.getExistenceFrom(target);
            double hp=Math.min(target.getMaxHealth()*.07,Math.max(0,maxHP(target)-target.getHealth()));
            double shp=Math.min(maxSHP(target)*.06,Math.max(0,maxSHP(target)-energy.getSpiritualHealth()));
            if(hp<=0 && shp<=0)continue;
            plans.add(new Plan(target,hp,shp));price+=hp*hpRate+shp*shpRate;
        }
        if(price<=0 || !Double.isFinite(price))return;
        var energy=TensuraStorages.getExistenceFrom(caster);
        double before=Math.max(0,energy.getAura());
        double reserved=caster.hasInfiniteMaterials()?price:Math.min(before,price);
        if(reserved<=0)return;
        double fraction=reserved/price;
        if(EnergyHelper.isOutOfEnergy(caster,reserved,0))return;
        double paid=Math.max(0,before-energy.getAura()),used=0;
        for(var plan:plans){
            var target=plan.target();if(!target.isAlive() || SkillUtils.shouldCancelHealing(target))continue;
            double hp=Math.min(plan.hp()*fraction,Math.max(0,maxHP(target)-target.getHealth()));
            float allowed=(float)hp;if(allowed>hp)allowed=Math.nextDown(allowed);
            if(allowed>0){
                float initial=target.getHealth();var previous=QUOTA.get();QUOTA.set(new Quota(target,allowed));
                try{target.heal(allowed);}finally{if(previous==null)QUOTA.remove();else QUOTA.set(previous);}
                used+=Math.min(hp,Math.max(0,target.getHealth()-initial))*hpRate;
            }
            if(!target.isAlive() || SkillUtils.shouldCancelHealing(target))continue;
            var targetEnergy=TensuraStorages.getExistenceFrom(target);
            double initial=targetEnergy.getSpiritualHealth();
            double shp=Math.min(plan.shp()*fraction,Math.max(0,maxSHP(target)-initial));
            if(shp>0){targetEnergy.setSpiritualHealth(initial+shp);targetEnergy.markDirty();used+=Math.max(0,targetEnergy.getSpiritualHealth()-initial)*shpRate;}
        }
        // Refund only this reservation, preserving aura received/spent by other handlers.
        double refund=Math.min(paid,Math.max(0,reserved-used));
        if(refund>0){energy.setAura(Math.min(EnergyHelper.getMaxAura(caster),energy.getAura()+refund));energy.markDirty();}
    }
}
