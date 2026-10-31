package dev.heavenlyrestriction;

import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.*;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.config.ability.BattlewillConfig;
import io.github.manasmods.tensura.particle.*;
import io.github.manasmods.tensura.registry.battlewill.UtilityArts;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.sound.TensuraSoundEvents;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Native creative-flight movement, earned through Battlewill mastery and paid in aura. */
public final class AirSovereigntyArt extends Battlewill {
    private static final String ENABLED="air_sovereignty_enabled",LEASE="air_sovereignty_flight",TICKS="air_sovereignty_ticks";
    @Override public MutableComponent getName(){return Component.literal("Air Walk");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(ChatFormatting.RED);}
    @Override public ResourceLocation getSkillIcon(){return HeavenlyRestriction.id("textures/battlewill/air_sovereignty.png");}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Evolution of mastered Air Flight (Air Float). Press to start or stop free flight like Gravity Manipulation: hover, move freely, jump to ascend and sneak to descend. Activation and each second spent flying cost the sum of Air Flight's configured aura and magicule costs, paid entirely in aura (30 by default, halved to 15 when mastered). Stopping is free. No magicule cost, gravity-element boost or damage. Learn the evolved art through the normal ability menu. Native interference and movement restrictions still apply.");}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){
        var config=ConfigRegistry.getConfig(BattlewillConfig.class).AirFlight;
        return (Math.max(0,config.auraCost)+Math.max(0,config.magiculeCost))*(i.isMastered(e)?.5:1);
    }
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal("Air Walk");}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "air_sovereignty.flight";}

    /** Native Formhide -> Haze uses the same mastery-to-learning-instance progression. */
    public static void unlockEvolution(LivingEntity e,ManasSkillInstance parent){
        if(e.level().isClientSide() || parent.getSkill()!=UtilityArts.AIR_FLIGHT.get() || parent.isTemporarySkill() || parent.isSubInstance() || !parent.isMastered(e))return;
        var storage=SkillAPI.getSkillsFrom(e);
        if(storage.getSkill(parent.getSkill()).filter(owned->owned==parent).isEmpty() || storage.getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get()).isPresent())return;
        SkillHelper.learnSkill(e,HeavenlyRestriction.AIR_SOVEREIGNTY.get().createLearningInstance(e));
    }
    private static boolean usable(Player p,ManasSkillInstance i){
        return p.isAlive() && i.getMastery()>=0 && i.canInteractSkill(p)
            && HeavenlyRestriction.AIR_SOVEREIGNTY.get().canActivateInArea(i,p,0,true,false)
            && !SkillUtils.shouldCancelInteraction(p) && p.getAttributeValue(Attributes.MOVEMENT_SPEED)>0
            && !p.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.MAGIC_INTERFERENCE));
    }
    public static boolean authorizesFlight(Player p){
        if(!p.level().isClientSide() && !RestrictionData.of(p).getBoolean(LEASE))return false;
        return SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get())
            .filter(i->i.getOrCreateTag().getBoolean(ENABLED) && usable(p,i)).isPresent();
    }
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){
        if(!(e instanceof ServerPlayer p) || mode!=0)return;
        if(i.getOrCreateTag().getBoolean(ENABLED)){stop(p);pulse(p,false);return;}
        if(p.isCreative() || p.isSpectator() || p.getAbilities().mayfly || !usable(p,i) || !canActivateSkill(i,p,mode))return;
        if(EnergyHelper.isOutOfEnergy(p,i,0))return;
        i.getOrCreateTag().putBoolean(ENABLED,true);i.getOrCreateTag().putInt(TICKS,0);i.markDirty();
        RestrictionData.of(p).putBoolean(LEASE,true);
        p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.resetFallDistance();p.onUpdateAbilities();
        addMasteryPoint(i,p);SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);pulse(p,true);
    }
    private static void pulse(ServerPlayer p,boolean enabling){
        for(double height:new double[]{.33,.66})TensuraParticleHelper.spawnServerParticles(p.level(),
            TensuraParticleUtils.getColorlessWave(.9f,p.getBbWidth()*3,enabling?-.5f:.5f,true),
            p.getX(),p.getY()+p.getBbHeight()*height,p.getZ());
        p.playNotifySound(enabling?TensuraSoundEvents.BUFF_ACTIVATE.get():TensuraSoundEvents.BUFF_DEACTIVATE.get(),ABILITY_SOUND,1,1);
    }
    /** Remove our entitlement before asking native code whether another source permits flight. */
    public static void stop(Player p){
        var instance=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get());
        boolean leased=RestrictionData.of(p).getBoolean(LEASE) || instance.filter(i->i.getOrCreateTag().getBoolean(ENABLED)).isPresent();
        instance.ifPresent(i->{i.getOrCreateTag().remove(ENABLED);i.getOrCreateTag().remove(TICKS);i.markDirty();SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);});
        RestrictionData.of(p).remove(LEASE);
        if(leased && !SkillUtils.canFlyLegit(p)){
            p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.resetFallDistance();p.onUpdateAbilities();
        }
    }
    @Override public void onForgetSkill(ManasSkillInstance i,LivingEntity e){
        if(e instanceof Player p && !e.level().isClientSide())stop(p);
        super.onForgetSkill(i,e);
    }
    /** Runs even if native passive ticking is suppressed, so stale flight is revoked. */
    public static void tick(ServerPlayer p){
        var storage=SkillAPI.getSkillsFrom(p);
        if(p.tickCount%20==0)storage.getSkill(UtilityArts.AIR_FLIGHT.get()).ifPresent(i->unlockEvolution(p,i));
        var optional=storage.getSkill(HeavenlyRestriction.AIR_SOVEREIGNTY.get());
        if(!RestrictionData.of(p).getBoolean(LEASE)){if(optional.filter(i->i.getOrCreateTag().getBoolean(ENABLED)).isPresent())stop(p);return;}
        if(optional.isEmpty() || p.isCreative() || p.isSpectator() || !usable(p,optional.get()) || !optional.get().getOrCreateTag().getBoolean(ENABLED) || !p.getAbilities().mayfly){stop(p);return;}
        if(!p.getAbilities().flying)return;
        var i=optional.get();var tag=i.getOrCreateTag();int ticks=tag.getInt(TICKS)+1;
        if(ticks>=20){
            if(EnergyHelper.isOutOfEnergy(p,i,0)){stop(p);return;}
            ticks=0;i.addMasteryPoint(p);storage.checkAndMarkDirty(i);
        }
        tag.putInt(TICKS,ticks);p.resetFallDistance();
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.AIR_FLIGHT.get()).ifPresent(i->unlockEvolution(p,i));
        if(RestrictionData.of(p).getBoolean(LEASE)){
            if(authorizesFlight(p) && !p.isCreative() && !p.isSpectator()){p.getAbilities().mayfly=true;p.onUpdateAbilities();}
            else stop(p);
        }
    }
    public static void clonePlayer(PlayerEvent.Clone event){
        // HunterEvents copies the persistent root first. Respawning ends this flight lease.
        if(!event.getEntity().level().isClientSide())stop(event.getEntity());
    }
}
