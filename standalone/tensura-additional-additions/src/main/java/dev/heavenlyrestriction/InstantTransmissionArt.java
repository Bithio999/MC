package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.*;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.ability.subclass.ISpatialMovement;
import io.github.manasmods.tensura.data.TensuraBlockTags;
import io.github.manasmods.tensura.registry.battlewill.UtilityArts;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.*;

/** A native coordinate menu with self-only, server-authoritative aura transmission. */
public final class InstantTransmissionArt extends Battlewill implements ISpatialMovement {
    public static final int CHARGE_TICKS=100;
    private record Charge(ManasSkillInstance skill,ResourceKey<Level> dimension,Vec3 target,long due) {}
    private static final Map<UUID,Charge> CHARGES=new HashMap<>();
    @Override public MutableComponent getName(){return Component.literal("Instant Transmission");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(ChatFormatting.RED);}
    @Override public ResourceLocation getSkillIcon(){return UtilityArts.INSTANT_MOVE.get().getSkillIcon();}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Evolution of mastered Instant Move. Press to enter X, Y and Z in Tensura's Spatial Movement menu and teleport yourself within the current dimension. Charge: 100 ticks (5 seconds), instant when mastered. Costs 10 aura per block of straight-line distance, halved to 5 when mastered, paid once when you confirm. Opening the menu is free. No magicule cost or portal creation. Native teleportation restrictions apply.");}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "instant_transmission.warp";}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return getName();}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return i.isMastered(e)?5:10;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public double getWarpCost(double distance){return 10*distance;}
    @Override public int getWarpChargeTick(ManasSkillInstance i,LivingEntity e){return i.isMastered(e)?0:CHARGE_TICKS;}
    @Override public boolean canPortal(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public boolean canDimensionTravel(ManasSkillInstance i,LivingEntity e){return false;}
    @Override public boolean canWarp(ManasSkillInstance i,LivingEntity e){return i.getMastery()>=0 && (!(e instanceof ServerPlayer p) || !charging(p));}
    public static boolean charging(ServerPlayer p){return CHARGES.containsKey(p.getUUID());}
    public static void unlockEvolution(LivingEntity e,ManasSkillInstance parent){
        if(e.level().isClientSide() || parent.getSkill()!=UtilityArts.INSTANT_MOVE.get() || parent.isTemporarySkill() || parent.isSubInstance() || !parent.isMastered(e))return;
        var storage=SkillAPI.getSkillsFrom(e);
        if(storage.getSkill(parent.getSkill()).filter(i->i==parent).isEmpty() || storage.getSkill(HeavenlyRestriction.INSTANT_TRANSMISSION.get()).isPresent())return;
        SkillHelper.learnSkill(e,HeavenlyRestriction.INSTANT_TRANSMISSION.get().createLearningInstance(e));
    }
    private boolean usable(ServerPlayer p,ManasSkillInstance i){
        return p.isAlive() && i.getMastery()>=0 && !p.isSpectator()
            && SkillAPI.getSkillsFrom(p).getSkill(this).filter(owned->owned==i).isPresent()
            && !getBlacklistDimensions().contains(p.level().dimension().location().toString())
            && i.canInteractSkill(p) && canActivateSkill(i,p,0)
            && !SkillUtils.shouldCancelInteraction(p) && !SkillUtils.shouldCancelTeleportation(p)
            && !p.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.MAGIC_INTERFERENCE));
    }
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){
        if(e instanceof ServerPlayer p && mode==0 && !charging(p) && usable(p,i))openSpatialMovementMenu(p,this);
    }
    /** Called only for this art by the native coordinate request handler. No target entity is accepted. */
    public boolean requestWarp(ServerPlayer p,ManasSkillInstance i,double x,double y,double z,ResourceKey<Level> dimension,boolean warpPad){
        if(charging(p) || !usable(p,i) || !p.level().dimension().equals(dimension))return false;
        if(!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))return false;
        var at=BlockPos.containing(x,y,z);
        if(!Level.isInSpawnableBounds(at) || !p.level().getWorldBorder().isWithinBounds(at)){
            p.displayClientMessage(Component.translatable("tensura.skill.teleport.out_border").withStyle(ChatFormatting.RED),true);return false;
        }
        if(warpPad){
            if(!p.level().getBlockState(at).is(TensuraBlockTags.WARP_PADS)){
                p.displayClientMessage(Component.translatable("tensura.warp_pad.not_found").withStyle(ChatFormatting.RED),true);return false;
            }
            y+=.5;
        }
        var target=new Vec3(x,y,z);double distance=p.position().distanceTo(target);
        if(distance<.001)return false;
        double cost=getAuraCost(p,i,0)*distance;
        if(!Double.isFinite(cost))return false;
        try(var equations=dev.heavenlyrestriction.laplace.expand.ExpandedRuntime.open(p,i,0)){if(EnergyHelper.isOutOfEnergy(p,cost,0))return false;}
        p.closeContainer();
        int ticks=getWarpChargeTick(i,p);
        if(ticks==0)finish(p,i,target);
        else{
            CHARGES.put(p.getUUID(),new Charge(i,dimension,target,p.serverLevel().getGameTime()+ticks));
            pulse(p);p.displayClientMessage(Component.translatable("message.heavenlyrestriction.instant_transmission.charging",5),true);
        }
        return true;
    }
    private static void finish(ServerPlayer p,ManasSkillInstance i,Vec3 target){
        var before=p.position();
        ISpatialMovement.warp(p,p,target.x,target.y,target.z,p.level().dimension());
        if(p.position().distanceToSqr(before)>1e-8){i.addMasteryPoint(p);i.markDirty();SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);}
    }
    private static void pulse(ServerPlayer p){
        p.serverLevel().sendParticles(ParticleTypes.PORTAL,p.getX(),p.getY()+p.getBbHeight()*.5,p.getZ(),12,.2,.4,.2,.15);
    }
    public static void cancel(ServerPlayer p){CHARGES.remove(p.getUUID());}
    public static void tick(ServerPlayer p){
        if(p.tickCount%20==0)SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.INSTANT_MOVE.get()).ifPresent(i->unlockEvolution(p,i));
        var charge=CHARGES.get(p.getUUID());if(charge==null)return;
        var art=HeavenlyRestriction.INSTANT_TRANSMISSION.get();
        if(!p.level().dimension().equals(charge.dimension) || !art.usable(p,charge.skill)){
            cancel(p);p.displayClientMessage(Component.translatable("message.heavenlyrestriction.instant_transmission.cancelled").withStyle(ChatFormatting.RED),true);return;
        }
        long remaining=charge.due-p.serverLevel().getGameTime();
        if(remaining<=0){cancel(p);finish(p,charge.skill,charge.target);}
        else if(remaining%4==0){
            pulse(p);
            if(remaining%20==0)p.displayClientMessage(Component.translatable("message.heavenlyrestriction.instant_transmission.charging",remaining/20),true);
        }
    }
    @Override public void onForgetSkill(ManasSkillInstance i,LivingEntity e){if(e instanceof ServerPlayer p)cancel(p);super.onForgetSkill(i,e);}
    public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p){cancel(p);SkillAPI.getSkillsFrom(p).getSkill(UtilityArts.INSTANT_MOVE.get()).ifPresent(i->unlockEvolution(p,i));}
    }
    public static void register(){
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post event)->{if(event.getEntity() instanceof ServerPlayer p)tick(p);});
        NeoForge.EVENT_BUS.addListener(InstantTransmissionArt::login);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof ServerPlayer p)cancel(p);});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event)->{if(event.getEntity() instanceof ServerPlayer p)cancel(p);});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone event)->{if(event.getOriginal() instanceof ServerPlayer p)cancel(p);if(event.getEntity() instanceof ServerPlayer p)cancel(p);});
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event)->{if(event.getEntity() instanceof ServerPlayer p)cancel(p);});
    }
}
