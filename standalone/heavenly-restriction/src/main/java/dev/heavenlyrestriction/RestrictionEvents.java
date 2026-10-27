package dev.heavenlyrestriction;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RestrictionEvents {
    private RestrictionEvents(){}
    public static void tick(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof LivingEntity e) || e.level().isClientSide())return;
        AuraRegenerationSkill.tick(e);
        if(e.tickCount%10==0 && ChainedState.owned(e))ChainedState.refresh(e);
        if(e instanceof ServerPlayer p){AirSovereigntyArt.tick(p);if(e.tickCount%20==0)RestrictedRace.migrateRecovery(p);}
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity().level().isClientSide())return;
        ChainedState.initialize(event.getEntity());RestrictedRace.migrateRecovery(event.getEntity());
    }
    public static void onClone(PlayerEvent.Clone event){
        if(event.getOriginal().getPersistentData().contains(HeavenlyRestriction.ID))
            event.getEntity().getPersistentData().put(HeavenlyRestriction.ID,RestrictionData.of(event.getOriginal()).copy());
    }
}
