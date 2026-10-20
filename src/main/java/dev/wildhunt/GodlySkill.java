package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.entity.variant.MagicCircleVariant;

/** Native Ultimate, addon Godly. Fixed spatial damage; no EP multiplier. */
public final class GodlySkill extends Skill {
    private static final ResourceLocation PASSIVE=WildHunt.id("aion_continuum");
    public GodlySkill() { super(SkillType.ULTIMATE); }
    @Override public MutableComponent getName() { return Component.translatableWithFallback("wildhunt.skill.aion","Aion, God of Causality"); }
    @Override public MutableComponent getColoredName() { return getName().withColor(0xE8C56F); }
    @Override public ResourceLocation getSkillIcon() { return WildHunt.id("textures/skill/aion.png"); }
    @Override public MutableComponent getSkillDescription() { return Component.literal("[Godly] Hold Worldline Sever to sweep an eight-target spatial beam along your aim. Event Horizon pulls from 24 blocks into a 10-block damaging core. Causal Reversal restores recorded HP, SHP, aura, magicules and position; the free return triggers automatically after 20 seconds. Genesis Verdict collapses a Horizon for an empowered finale. Unbroken Continuum grants recovering barriers and a proximity slowing aura. Magicules only; mastery strengthens damage."+GodlyPassives.DESCRIPTION); }
    @Override public int getMaxMastery() { return 50_000; }
    @Override public double getDefaultAcquiringMagiculeCost() { return 0; }
    @Override public int getModes(ManasSkillInstance i) { return 4; }
    @Override public String getModeId(ManasSkillInstance i,int mode) { return switch(mode) {case 0->"aion.worldline_sever";case 1->"aion.event_horizon";case 2->"aion.causal_reversal";default->"aion.genesis_verdict";}; }
    @Override public Component getModeName(ManasSkillInstance i,int mode) { return Component.literal(switch(mode) {case 0->"Worldline Sever";case 1->"Event Horizon";case 2->"Causal Reversal";default->"Genesis Verdict";}); }
    @Override public int nextMode(LivingEntity e,ManasSkillInstance i,int mode,boolean reverse) { return Math.floorMod(mode+(reverse?-1:1),4); }
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode) { return 0; }
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode) { return switch(mode) {case 0->2400;case 1->10000;case 2->i.getOrCreateTag().contains("anchor_until")?0:12000;default->28000;}; }
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e) { return true; }
    @Override public boolean canTick(ManasSkillInstance i,LivingEntity e) { return false; }
    @Override public void onPressed(ManasSkillInstance i,LivingEntity caster,int slot,int mode) {
        if(!(caster.level() instanceof ServerLevel level) || mode<0 || mode>3 || i.onCoolDown(mode) || !canInteractSkill(i,caster) || !canActivateSkill(i,caster,mode))return;
        var tag=i.getOrCreateTag(); long now=level.getGameTime();
        if(mode==0 && tag.contains("channel_until"))return;
        boolean returning=mode==2 && tag.contains("anchor_until");
        Vec3 anchor=ApexArts.point(tag,"anchor");
        if(returning && (!sameDimension(i,caster) || !level.noCollision(caster,caster.getBoundingBox().move(anchor.subtract(caster.position()))) || !level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(anchor)))) {
            message(caster,"Your recorded position is no longer safe."); return;
        }
        if(!returning && EnergyHelper.isOutOfEnergy(caster,i,mode))return;
        int cooldown;
        switch(mode) {
            case 0 -> {
                tag.putLong("channel_until",now+120);tag.putLong("channel_heartbeat",now);
                ApexArts.point(tag,"channel_aim",caster.getLookAngle());tag.putLong("channel_aim_time",now);
                tag.putLong("channel_next",now+10);tag.putString("dimension",level.dimension().location().toString());
                updateRay(level,i,caster,true);cooldown=0;
            }
            case 1 -> {
                Vec3 center=ApexArts.cursorPlacement(level,caster,64).add(0,1,0);removeVisual(level,tag,"horizon_visual");removeVisual(level,tag,"horizon_circle"); ApexArts.point(tag,"horizon",center);
                tag.putLong("horizon_until",now+200); tag.putString("dimension",level.dimension().location().toString());
                var visual=ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_DOMAIN,center,new Vec3(10,24,0),200);tag.putUUID("horizon_visual",visual.getUUID());
                tag.putUUID("horizon_circle",GodlyVisuals.circle(level,caster,center.subtract(0,.9,0),10,200,MagicCircleVariant.GRAVITY).getUUID());cooldown=16;
            }
            case 2 -> {
                if(returning) {
                    reverse(level,i,caster,false);cooldown=25;
                } else {
                    ApexArts.point(tag,"anchor",caster.position()); tag.putFloat("anchor_hp",caster.getHealth()); tag.putLong("anchor_until",now+400);tag.putDouble("anchor_shp",TensuraStorages.getExistenceFrom(caster).getSpiritualHealth());
                    // Snapshot after the initial cast payment: returning never refunds the cast itself.
                    var energy=TensuraStorages.getExistenceFrom(caster);
                    tag.putDouble("anchor_aura",energy.getAura());tag.putDouble("anchor_magicules",energy.getMagicule());
                    tag.putString("dimension",level.dimension().location().toString());
                    tag.putUUID("anchor_visual",ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_ANCHOR,caster.position().add(0,.1,0),new Vec3(2.5,0,0),400).getUUID());cooldown=1;
                }
            }
            default -> {
                boolean empowered=tag.getLong("horizon_until")>now && sameDimension(i,caster);
                ApexArts.point(tag,"verdict",empowered?ApexArts.point(tag,"horizon"):ApexArts.cursorPlacement(level,caster,64).add(0,1,0));
                tag.putBoolean("verdict_empowered",empowered);tag.putInt("verdict_pulse",0);tag.remove("horizon_until");
                removeVisual(level,tag,"horizon_visual");removeVisual(level,tag,"horizon_circle");tag.putString("dimension",level.dimension().location().toString());
                tag.putLong("verdict_until",now+61); tag.putLong("verdict_next",now+20);
                var center=ApexArts.point(tag,"verdict");double radius=empowered?14:12;
                tag.putUUID("verdict_visual",ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_VERDICT,center,new Vec3(radius,0,0),65).getUUID());
                tag.putUUID("verdict_circle",GodlyVisuals.circle(level,caster,center.subtract(0,.9,0),(float)radius,65,MagicCircleVariant.SPACE).getUUID());cooldown=45;
            }
        }
        i.setCoolDown(cooldown,mode); i.markDirty(); HunterData.of(caster).putBoolean("active_skills",true); addMasteryPoint(i,caster);
        level.playSound(null,caster.blockPosition(),SoundEvents.BEACON_POWER_SELECT,SoundSource.PLAYERS,1f,.65f+mode*.15f);
    }
    @Override public int getMaxHeldTime(ManasSkillInstance i,LivingEntity e){return 120;}
    @Override public boolean shouldTriggerReleaseOnHeldInterrupt(ManasSkillInstance i,LivingEntity e,int key,int mode){return mode==0;}
    @Override public boolean onHeld(ManasSkillInstance i,LivingEntity e,int heldTicks,int mode) {
        if(mode!=0 || !(e.level() instanceof ServerLevel level))return false;
        var tag=i.getOrCreateTag();long now=level.getGameTime();
        if(!tag.contains("channel_until") || now>=tag.getLong("channel_until") || !e.isAlive() || !sameDimension(i,e) || !canActivateSkill(i,e,0)) {stopChannel(i,e);return false;}
        tag.putLong("channel_heartbeat",now);
        boolean pulse=now>=tag.getLong("channel_next");
        if(pulse && EnergyHelper.isOutOfEnergy(e,i,0,.5f)){stopChannel(i,e);return false;}
        Vec3 facing=WorldlineAim.approach(ApexArts.point(tag,"channel_aim"),e.getLookAngle(),Math.max(0,now-tag.getLong("channel_aim_time"))/20d);
        ApexArts.point(tag,"channel_aim",facing);tag.putLong("channel_aim_time",now);
        updateRay(level,i,e,pulse);
        if(pulse)tag.putLong("channel_next",now+10);
        i.markDirty();return true;
    }
    @Override public void onRelease(ManasSkillInstance i,LivingEntity e,int ticks,int key,int mode) {if(mode==0)stopChannel(i,e);}
    private static void stopChannel(ManasSkillInstance i,LivingEntity e) {
        var tag=i.getOrCreateTag();
        if(tag.contains("channel_until")){tag.remove("channel_until");i.setCoolDown(Math.max(4,i.getCoolDown(0)),0);i.markDirty();}
        if(e.level() instanceof ServerLevel level)removeVisual(level,tag,"channel_visual");
    }
    private void updateRay(ServerLevel level,ManasSkillInstance i,LivingEntity caster,boolean damage) {
        var tag=i.getOrCreateTag();Vec3 facing=ApexArts.point(tag,"channel_aim").normalize();
        Vec3 from=caster.getEyePosition(),end=WorldlineAim.end(caster,from,facing);
        // Begin beside and ahead of the eye so the caster can see the channel without sitting inside its core.
        Vec3 side=facing.cross(new Vec3(0,1,0)).normalize();
        if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
        Vec3 visualFrom=from.add(facing.scale(Math.min(.9,from.distanceTo(end)*.3))).add(side.scale(.35)).add(0,-.25,0);
        if(tag.hasUUID("channel_visual") && level.getEntity(tag.getUUID("channel_visual")) instanceof ArtVisualEntity visual)visual.updateVector(visualFrom,end.subtract(visualFrom));
        else {
            var visual=ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_RAY,visualFrom,end.subtract(visualFrom),120);
            visual.setCaster(caster);tag.putUUID("channel_visual",visual.getUUID());
        }
        if(!damage)return;
        var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(2),e->HunterData.canHarm(caster,e) && e.getBoundingBox().inflate(1.25).clip(from,end).isPresent());
        targets.sort(java.util.Comparator.comparingDouble(e->e.distanceToSqr(caster)));
        targets.stream().limit(8).forEach(target->{hit(level,i,caster,target,1200,0);GodlyVisuals.impact(level,target.getBoundingBox().getCenter(),1.2);});
    }
    public static boolean passiveActive(net.minecraft.world.entity.Entity entity) {
        if(!(entity instanceof LivingEntity e) || !e.isAlive())return false;
        return SkillAPI.getSkillsFrom(e).getSkill(WildHunt.GODLY.get()).map(i->i.isToggled() && i.getSkill().canInteractSkill(i,e) && (e.hasInfiniteMaterials() || i.getOrCreateTag().getLong("upkeep")>e.level().getGameTime() || TensuraStorages.getExistenceFrom(e).getMagicule()>=180)).orElse(false);
    }
    private static final ResourceLocation SLOW=WildHunt.id("causal_dilation");
    private static void slowNearby(ServerLevel level,LivingEntity caster) {
        for(var target:ApexArts.enemies(level,caster,caster.position(),16)) {
            double strength=.15+.65*(1-Math.clamp(target.distanceTo(caster)/16d,0,1));
            var d=HunterData.of(target);var movement=target.getAttribute(Attributes.MOVEMENT_SPEED);if(movement==null)continue;
            // One non-stacking modifier; the strongest overlapping aura wins until its short lease expires.
            var old=movement.getModifier(SLOW);
            if(old==null || -old.amount()<=strength || level.getGameTime()>=d.getLong("aion_slow_until")) {
                movement.addOrUpdateTransientModifier(new AttributeModifier(SLOW,-strength,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                d.putLong("aion_slow_until",level.getGameTime()+10);
            }
        }
    }
    private static double dilation(double distance) {return .15+.65*(1-Math.clamp(distance/16,0,1));}
    private static void slowProjectiles(ServerLevel level,LivingEntity caster) {
        for(var projectile:level.getEntitiesOfClass(Projectile.class,caster.getBoundingBox().inflate(16),p->p.distanceToSqr(caster)<256 && p.getOwner()!=caster && (!(p.getOwner() instanceof LivingEntity owner) || HunterData.canHarm(caster,owner)))) {
            var data=projectile.getPersistentData();double factor=1-dilation(projectile.distanceTo(caster));
            if(data.getLong("myths_dilation_tick")!=level.getGameTime() || factor<data.getDouble("myths_dilation_factor"))data.putDouble("myths_dilation_factor",factor);
            data.putLong("myths_dilation_tick",level.getGameTime());
            data.putLong("myths_dilation_until",level.getGameTime()+2);
        }
    }
    /** Change the velocity by the ratio of factors, so staying inside an aura never compounds the slow. */
    public static void slowProjectile(Projectile projectile) {
        var data=projectile.getPersistentData();
        double before=data.contains("myths_dilation_applied")?data.getDouble("myths_dilation_applied"):1;
        double factor=data.getLong("myths_dilation_until")>projectile.level().getGameTime()?data.getDouble("myths_dilation_factor"):1;
        factor=Math.clamp(factor,.2,1);before=Math.clamp(before,.2,1);
        if(Math.abs(before-factor)>.00001){projectile.setDeltaMovement(projectile.getDeltaMovement().scale(factor/before));projectile.hurtMarked=true;}
        if(factor<1)data.putDouble("myths_dilation_applied",factor);
        else {data.remove("myths_dilation_applied");data.remove("myths_dilation_until");data.remove("myths_dilation_factor");}
    }
    public static void projectileTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if(!event.getEntity().level().isClientSide() && event.getEntity() instanceof Projectile p)slowProjectile(p);
    }
    private static boolean safeAnchor(ServerLevel level,LivingEntity caster,Vec3 anchor) {
        return level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(anchor)) && level.noCollision(caster,caster.getBoundingBox().move(anchor.subtract(caster.position())));
    }
    private void reverse(ServerLevel level,ManasSkillInstance i,LivingEntity caster,boolean automatic) {
        var tag=i.getOrCreateTag();Vec3 before=caster.position(),anchor=ApexArts.point(tag,"anchor");
        // Manual blocked returns remain pending. On timeout restore in place rather than teleporting into a wall.
        boolean safe=sameDimension(i,caster) && safeAnchor(level,caster,anchor);
        if(!safe && !automatic)return;
        if(safe){caster.teleportTo(anchor.x,anchor.y,anchor.z);caster.setDeltaMovement(Vec3.ZERO);caster.hurtMarked=true;}
        else message(caster,"Your recorded position is blocked. Causal Reversal restored your condition in place.");
        caster.heal(Math.max(0,Math.min(caster.getMaxHealth(),tag.getFloat("anchor_hp"))-caster.getHealth()));
        var energy=TensuraStorages.getExistenceFrom(caster);
        energy.setSpiritualHealth(Math.max(energy.getSpiritualHealth(),Math.min(caster.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),tag.getDouble("anchor_shp"))));
        // Old checkpoints have no resource snapshot; never invent a refund for those saves.
        if(tag.contains("anchor_aura"))energy.setAura(Math.max(energy.getAura(),Math.clamp(tag.getDouble("anchor_aura"),0,EnergyHelper.getMaxAura(caster))));
        if(tag.contains("anchor_magicules"))energy.setMagicule(Math.max(energy.getMagicule(),Math.clamp(tag.getDouble("anchor_magicules"),0,EnergyHelper.getMaxMagicule(caster))));
        energy.markDirty();tag.remove("anchor_until");tag.remove("anchor_aura");tag.remove("anchor_magicules");removeVisual(level,tag,"anchor_visual");
        GodlyVisuals.impact(level,before.add(0,1,0),3);GodlyVisuals.impact(level,caster.position().add(0,1,0),3);
        GodlyVisuals.circle(level,caster,caster.position().add(0,.1,0),3,30,MagicCircleVariant.RECOVERY);
        i.setCoolDown(25,2);i.markDirty();
    }
    public static void tickSlow(LivingEntity target) {
        var d=target.getPersistentData().getCompound("wildhunt");
        d.remove("aion_degraded_until"); // Discard the retired, target-wide suppression lease.
        if(d.contains("aion_slow_until") && (!target.isAlive() || target.level().getGameTime()>=d.getLong("aion_slow_until"))) {
            var movement=target.getAttribute(Attributes.MOVEMENT_SPEED);if(movement!=null)movement.removeModifier(SLOW);d.remove("aion_slow_until");
        }
    }
    private boolean sameDimension(ManasSkillInstance i,LivingEntity e) { return i.getOrCreateTag().getString("dimension").equals(e.level().dimension().location().toString()); }
    private void hit(ServerLevel level,ManasSkillInstance i,LivingEntity caster,LivingEntity target,float damage,int mode) {
        if(!HunterData.canHarm(caster,target))return;
        float power=(float)(1+.5*Math.clamp(i.getMastery()/getMaxMastery(),0,1));
        if(target.hurt(createSource(i,caster,TensuraDamageTypes.SPACE_ELEMENTAL,mode),damage*power))level.sendParticles(ParticleTypes.END_ROD,target.getX(),target.getY()+1,target.getZ(),8,.3,.7,.3,.02);
    }
    @Override public void onToggleOn(ManasSkillInstance i,LivingEntity e) {
        if(e.level().isClientSide())return;
        if(!canInteractSkill(i,e) || !canActivateSkill(i,e,0) || EnergyHelper.isOutOfEnergy(e,0,180)) {onToggleOff(i,e);return;}
        i.setToggled(true);i.getOrCreateTag().putString("dimension",e.level().dimension().location().toString()); i.getOrCreateTag().putLong("upkeep",e.level().getGameTime()+20); restore(i,e);
        HunterData.of(e).putBoolean("active_skills",true); i.markDirty();
    }
    @Override public void onToggleOff(ManasSkillInstance i,LivingEntity e) {
        i.setToggled(false);
        for(var holder:java.util.List.of(TensuraAttributes.MULTILAYER_BARRIER,TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MAGIC_BARRIER,Attributes.KNOCKBACK_RESISTANCE)) {
            var a=e.getAttribute(holder);if(a!=null)a.removeModifier(PASSIVE);
        }
        i.markDirty();
    }
    private void restore(ManasSkillInstance i,LivingEntity e) {
        for(var holder:java.util.List.of(TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MAGIC_BARRIER,Attributes.KNOCKBACK_RESISTANCE)) {
            var a=e.getAttribute(holder); if(a!=null)a.addOrUpdateTransientModifier(new AttributeModifier(PASSIVE,holder.equals(Attributes.KNOCKBACK_RESISTANCE)?.75:45,AttributeModifier.Operation.ADD_VALUE));
        }
        var barrier=e.getAttribute(TensuraAttributes.MULTILAYER_BARRIER);
        if(barrier!=null && e.level().getGameTime()>=i.getOrCreateTag().getLong("barrier_ready") && (barrier.getModifier(PASSIVE)==null || barrier.getModifier(PASSIVE).amount()==600))
            barrier.addOrReplacePermanentModifier(new AttributeModifier(PASSIVE,1200,AttributeModifier.Operation.ADD_VALUE));
    }
    public boolean tickActive(ManasSkillInstance i,LivingEntity e) {
        if(!(e.level() instanceof ServerLevel level))return false;
        var tag=i.getOrCreateTag(); long now=level.getGameTime();
        if(!e.isAlive() || !canInteractSkill(i,e) || !sameDimension(i,e)) {
            stopChannel(i,e);tag.remove("horizon_until");tag.remove("verdict_until");tag.remove("sever_echoes");
            for(var key:java.util.List.of("channel_visual","horizon_visual","horizon_circle","verdict_visual","verdict_circle"))removeVisual(level,tag,key);
            // A checkpoint is already committed, not a fresh activation. Temporary suppression cannot cancel its scheduled recovery.
            if((!e.isAlive() || !sameDimension(i,e)) && tag.contains("anchor_until")) {
                tag.remove("anchor_until");removeVisual(level,tag,"anchor_visual");i.setCoolDown(Math.max(25,i.getCoolDown(2)),2);
            }
        }
        if(i.isToggled()) {
            if(!e.isAlive() || !canInteractSkill(i,e))onToggleOff(i,e);
            else if(now>=tag.getLong("upkeep")) {
                if(EnergyHelper.isOutOfEnergy(e,0,180))onToggleOff(i,e);
                else {restore(i,e); tag.putLong("upkeep",now+20);}
            }
        }
        if(tag.contains("channel_until") && (now>=tag.getLong("channel_until") || now-tag.getLong("channel_heartbeat")>2))stopChannel(i,e);
        if(passiveActive(e)) {
            slowProjectiles(level,e);
            if(now%5==0)slowNearby(level,e);
        }
        if(tag.contains("anchor_until") && now>=tag.getLong("anchor_until"))reverse(level,i,e,true);
        if(tag.getLong("horizon_until")>now && sameDimension(i,e)) {
            Vec3 center=ApexArts.point(tag,"horizon");
            for(var target:ApexArts.enemies(level,e,center,24)) {
                double resistance=1-Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),0,1);
                Vec3 pull=center.subtract(target.position()).normalize().scale(.4*resistance); target.push(pull.x,.01,pull.z); target.hurtMarked=true;
                if(now%20==0 && target.distanceToSqr(center)<=100)hit(level,i,e,target,700,1);
            }
            for(var projectile:level.getEntitiesOfClass(Projectile.class,new AABB(center,center).inflate(24),p->p.getOwner()!=e && (!(p.getOwner() instanceof LivingEntity owner) || HunterData.canHarm(e,owner)) && p.distanceToSqr(center)<24*24)) {
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(.65).add(center.subtract(projectile.position()).normalize().scale(.025))); projectile.hurtMarked=true;
            }
        }
        if(tag.getLong("verdict_until")>now && now>=tag.getLong("verdict_next") && sameDimension(i,e)) {
            Vec3 center=ApexArts.point(tag,"verdict");
            int pulse=tag.getInt("verdict_pulse");boolean empowered=tag.getBoolean("verdict_empowered");double radius=empowered?14:12;
            for(var target:ApexArts.enemies(level,e,center,radius))hit(level,i,e,target,pulse<2?2400:empowered?7200:4800,3);
            ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_IMPACT,center,new Vec3(radius,pulse+1,0),22);
            level.sendParticles(ParticleTypes.END_ROD,center.x,center.y,center.z,60,radius*.45,2,radius*.45,.1);
            tag.putInt("verdict_pulse",pulse+1);tag.putLong("verdict_next",now+20);
            level.playSound(null,net.minecraft.core.BlockPos.containing(center),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,1.5f,.5f);
        }
        tag.remove("sever_echoes"); // Retire old delayed worldline attacks on existing saves.
        boolean active=tag.contains("channel_until") || i.isToggled() || tag.getLong("horizon_until")>now || tag.getLong("verdict_until")>now || tag.contains("anchor_until");
        if(active)i.markDirty(); return active;
    }
    @Override public void onForgetSkill(ManasSkillInstance i,LivingEntity entity) {
        stopChannel(i,entity);onToggleOff(i,entity);
        i.getOrCreateTag().remove("sever_echoes");i.getOrCreateTag().remove("horizon_until");i.getOrCreateTag().remove("verdict_until");i.getOrCreateTag().remove("anchor_until");
        if(entity.level() instanceof ServerLevel level)clearVisuals(level,i.getOrCreateTag());
        super.onForgetSkill(i,entity);
    }
    public static void clearTransient(LivingEntity entity) {
        SkillAPI.getSkillsFrom(entity).getSkill(WildHunt.GODLY.get()).ifPresent(i->{
            var tag=i.getOrCreateTag();stopChannel(i,entity);
            if(tag.contains("anchor_until"))i.setCoolDown(Math.max(25,i.getCoolDown(2)),2);
            tag.remove("anchor_until");tag.remove("horizon_until");tag.remove("verdict_until");tag.remove("sever_echoes");
            if(entity.level() instanceof ServerLevel level)clearVisuals(level,tag);i.markDirty();
            if(i.isToggled()) { HunterData.of(entity).putBoolean("active_skills",true); }
        });
    }
    public static void damaged(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        SkillAPI.getSkillsFrom(event.getEntity()).getSkill(WildHunt.GODLY.get()).ifPresent(i->{if(i.isToggled()) {i.getOrCreateTag().putLong("barrier_ready",event.getEntity().level().getGameTime()+200);i.markDirty();}});
    }
    private static void removeVisual(ServerLevel level,CompoundTag tag,String key) {
        if(tag.hasUUID(key)){var entity=level.getEntity(tag.getUUID(key));if(entity!=null)entity.discard();tag.remove(key);}
    }
    private static void clearVisuals(ServerLevel level,CompoundTag tag) {
        for(var key:java.util.List.of("channel_visual","horizon_visual","horizon_circle","anchor_visual","verdict_visual","verdict_circle"))removeVisual(level,tag,key);
    }
    private static void message(LivingEntity e,String message) { e.sendSystemMessage(Component.literal(message)); }
}
