package dev.heavenlyrestriction;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;

/** Resource accounting for the true pact, independent of its combat toggle. */
public final class ChainedResources {
    // Native energy recovery runs every ten ticks: two 0.5-MP pulses retain 1 MP/second.
    public static final double RETAINED_MP_PER_PULSE=.5;
    private static final net.minecraft.tags.TagKey<net.minecraft.world.effect.MobEffect> PAUSED=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.MOB_EFFECT,HeavenlyRestriction.id("pauses_energy_regeneration"));
    private ChainedResources(){}
    public static double regeneration(IExistence storage,LivingEntity owner,double areaMagicule){
        if(owner.getActiveEffects().stream().anyMatch(effect->effect.getEffect().is(PAUSED)))return 0;
        double amount=areaMagicule*EnergyHelper.CONFIG.areaMagiculeRegen*owner.getAttributeValue(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);
        if(storage.getSleepModeTime()>0)amount+=EnergyHelper.getMaxMagicule(owner)*EnergyHelper.CONFIG.sleepModeMagicule;
        return Math.max(0,amount);
    }
    public static double capMagicules(LivingEntity owner,double amount){
        return owner!=null && !owner.level().isClientSide() && ChainedState.owned(owner) && !ChainedContext.initializing(owner)?Math.min(amount,EnergyHelper.getMaxMagicule(owner)):amount;
    }
    public static void reconcile(LivingEntity owner){
        if(owner.level().isClientSide() || !ChainedState.owned(owner) || ChainedContext.initializing(owner))return;
        var storage=TensuraStorages.getExistenceFrom(owner);double value=capMagicules(owner,storage.getMagicule());
        if(value<storage.getMagicule()){storage.setMagicule(value);storage.markDirty();}
    }
}
