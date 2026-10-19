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
    @Override public MutableComponent getSkillDescription() { return Component.literal("[Godly] Cut through eight enemies, then fracture their worldlines twice more. Event Horizon manifests a gravity well that drags enemies and hostile projectiles into its core. Causal Reversal restores recorded HP, SHP and position within 20 seconds. Genesis Verdict tears open a three-pulse spatial catastrophe; collapsing an existing Horizon empowers its final strike. Toggle Unbroken Continuum for a recovering multilayer barrier. Aura only; mastery strengthens damage."); }
    @Override public int getMaxMastery() { return 50_000; }
    @Override public double getDefaultAcquiringMagiculeCost() { return 0; }
    @Override public int getModes(ManasSkillInstance i) { return 4; }
    @Override public String getModeId(ManasSkillInstance i,int mode) { return switch(mode) {case 0->"aion.worldline_sever";case 1->"aion.event_horizon";case 2->"aion.causal_reversal";default->"aion.genesis_verdict";}; }
    @Override public Component getModeName(ManasSkillInstance i,int mode) { return Component.literal(switch(mode) {case 0->"Worldline Sever";case 1->"Event Horizon";case 2->"Causal Reversal";default->"Genesis Verdict";}); }
    @Override public int nextMode(LivingEntity e,ManasSkillInstance i,int mode,boolean reverse) { return Math.floorMod(mode+(reverse?-1:1),4); }
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode) { return switch(mode) {case 0->3200;case 1->6000;case 2->2400;default->15000;}; }
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode) { return 0; }
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e) { return true; }
    @Override public boolean canTick(ManasSkillInstance i,LivingEntity e) { return false; }
    @Override public void onPressed(ManasSkillInstance i,LivingEntity caster,int slot,int mode) {
        if(!(caster.level() instanceof ServerLevel level) || mode<0 || mode>3 || i.onCoolDown(mode) || !canInteractSkill(i,caster) || !canActivateSkill(i,caster,mode))return;
        var tag=i.getOrCreateTag(); long now=level.getGameTime();
        boolean returning=mode==2 && tag.getLong("anchor_until")>now;
        Vec3 anchor=ApexArts.point(tag,"anchor");
        if(returning && (!sameDimension(i,caster) || !level.noCollision(caster,caster.getBoundingBox().move(anchor.subtract(caster.position()))) || !level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(anchor)))) {
            message(caster,"Your recorded position is no longer safe."); return;
        }
        if(EnergyHelper.isOutOfEnergy(caster,i,mode))return;
        int cooldown;
        switch(mode) {
            case 0 -> {
                Vec3 from=caster.getEyePosition(),end=ApexArts.cursorPlacement(level,caster,80);
                ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_RAY,from,end.subtract(from),28);
                var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(2),e->HunterData.canHarm(caster,e) && e.getBoundingBox().inflate(1.25).clip(from,end).isPresent());
                targets.sort(java.util.Comparator.comparingDouble(e->e.distanceToSqr(caster)));
                var echoes=new ListTag();
                targets.stream().limit(8).forEach(target->{hit(level,i,caster,target,900,0);var echo=new CompoundTag();echo.putUUID("target",target.getUUID());echoes.add(echo);GodlyVisuals.impact(level,target.getBoundingBox().getCenter(),1.8);});
                tag.put("sever_echoes",echoes);tag.putLong("echo_next",now+12);tag.putInt("echo_count",2);tag.putString("dimension",level.dimension().location().toString());cooldown=4;
            }
            case 1 -> {
                Vec3 center=ApexArts.cursorPlacement(level,caster,64).add(0,1,0);removeVisual(level,tag,"horizon_visual");removeVisual(level,tag,"horizon_circle"); ApexArts.point(tag,"horizon",center);
                tag.putLong("horizon_until",now+200); tag.putString("dimension",level.dimension().location().toString());
                var visual=ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_DOMAIN,center,new Vec3(10,0,0),200);tag.putUUID("horizon_visual",visual.getUUID());
                tag.putUUID("horizon_circle",GodlyVisuals.circle(level,caster,center.subtract(0,.9,0),10,200,MagicCircleVariant.GRAVITY).getUUID());cooldown=16;
            }
            case 2 -> {
                if(returning) {
                    Vec3 before=caster.position(); caster.teleportTo(anchor.x,anchor.y,anchor.z);
                    caster.setDeltaMovement(Vec3.ZERO); caster.hurtMarked=true;
                    caster.heal(Math.max(0,Math.min(caster.getMaxHealth(),tag.getFloat("anchor_hp"))-caster.getHealth()));
                    var existence=TensuraStorages.getExistenceFrom(caster);
                    existence.setSpiritualHealth(Math.max(existence.getSpiritualHealth(),Math.min(caster.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH),tag.getDouble("anchor_shp"))));existence.markDirty();
                    tag.remove("anchor_until");removeVisual(level,tag,"anchor_visual");GodlyVisuals.impact(level,before.add(0,1,0),3);GodlyVisuals.impact(level,anchor.add(0,1,0),3);
                    GodlyVisuals.circle(level,caster,anchor.add(0,.1,0),3,30,MagicCircleVariant.RECOVERY);cooldown=25;
                } else {
                    ApexArts.point(tag,"anchor",caster.position()); tag.putFloat("anchor_hp",caster.getHealth()); tag.putLong("anchor_until",now+400);tag.putDouble("anchor_shp",TensuraStorages.getExistenceFrom(caster).getSpiritualHealth());
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
    private boolean sameDimension(ManasSkillInstance i,LivingEntity e) { return i.getOrCreateTag().getString("dimension").equals(e.level().dimension().location().toString()); }
    private void hit(ServerLevel level,ManasSkillInstance i,LivingEntity caster,LivingEntity target,float damage,int mode) {
        if(!HunterData.canHarm(caster,target))return;
        float power=(float)(1+.5*Math.clamp(i.getMastery()/getMaxMastery(),0,1));
        if(target.hurt(createSource(i,caster,TensuraDamageTypes.SPACE_ELEMENTAL,mode),damage*power))level.sendParticles(ParticleTypes.END_ROD,target.getX(),target.getY()+1,target.getZ(),8,.3,.7,.3,.02);
    }
    @Override public void onToggleOn(ManasSkillInstance i,LivingEntity e) {
        if(e.level().isClientSide())return;
        if(!canInteractSkill(i,e) || !canActivateSkill(i,e,0) || EnergyHelper.isOutOfEnergy(e,180,0)) {onToggleOff(i,e);return;}
        i.setToggled(true); i.getOrCreateTag().putLong("upkeep",e.level().getGameTime()+20); restore(i,e);
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
            tag.remove("horizon_until"); tag.remove("verdict_until");tag.remove("sever_echoes");clearVisuals(level,tag);
            if(tag.contains("anchor_until")) {tag.remove("anchor_until");i.setCoolDown(Math.max(25,i.getCoolDown(2)),2);}
        }
        if(i.isToggled()) {
            if(!e.isAlive() || !canInteractSkill(i,e))onToggleOff(i,e);
            else if(now>=tag.getLong("upkeep")) {
                if(EnergyHelper.isOutOfEnergy(e,180,0))onToggleOff(i,e);
                else {restore(i,e); tag.putLong("upkeep",now+20);}
            }
        }
        if(tag.contains("anchor_until") && now>=tag.getLong("anchor_until")) {tag.remove("anchor_until");removeVisual(level,tag,"anchor_visual");i.setCoolDown(Math.max(25,i.getCoolDown(2)),2);}
        if(tag.getLong("horizon_until")>now && sameDimension(i,e)) {
            Vec3 center=ApexArts.point(tag,"horizon");
            for(var target:ApexArts.enemies(level,e,center,10)) {
                double resistance=1-Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),0,1);
                Vec3 pull=center.subtract(target.position()).normalize().scale(.32*resistance); target.push(pull.x,.01,pull.z); target.hurtMarked=true;
                if(now%20==0)hit(level,i,e,target,120,1);
            }
            for(var projectile:level.getEntitiesOfClass(Projectile.class,new AABB(center,center).inflate(10),p->p.getOwner()!=e && (!(p.getOwner() instanceof LivingEntity owner) || HunterData.canHarm(e,owner)) && p.distanceToSqr(center)<100)) {
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(.65).add(center.subtract(projectile.position()).normalize().scale(.025))); projectile.hurtMarked=true;
            }
        }
        if(tag.getLong("verdict_until")>now && now>=tag.getLong("verdict_next") && sameDimension(i,e)) {
            Vec3 center=ApexArts.point(tag,"verdict");
            int pulse=tag.getInt("verdict_pulse");boolean empowered=tag.getBoolean("verdict_empowered");double radius=empowered?14:12;
            for(var target:ApexArts.enemies(level,e,center,radius))hit(level,i,e,target,pulse<2?700:empowered?2100:1400,3);
            ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_IMPACT,center,new Vec3(radius,pulse+1,0),22);
            level.sendParticles(ParticleTypes.END_ROD,center.x,center.y,center.z,60,radius*.45,2,radius*.45,.1);
            tag.putInt("verdict_pulse",pulse+1);tag.putLong("verdict_next",now+20);
            level.playSound(null,net.minecraft.core.BlockPos.containing(center),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,1.5f,.5f);
        }
        if(tag.contains("sever_echoes") && now>=tag.getLong("echo_next")) {
            for(var raw:tag.getList("sever_echoes",Tag.TAG_COMPOUND)) {
                var echo=(CompoundTag)raw;
                if(level.getEntity(echo.getUUID("target")) instanceof LivingEntity target && HunterData.canHarm(e,target) && target.distanceToSqr(e)<96*96 && e.hasLineOfSight(target)) {
                    hit(level,i,e,target,300,0);GodlyVisuals.impact(level,target.getBoundingBox().getCenter(),2.2);
                }
            }
            int remaining=tag.getInt("echo_count")-1;tag.putInt("echo_count",remaining);tag.putLong("echo_next",now+12);
            if(remaining<=0)tag.remove("sever_echoes");
        }
        boolean active=tag.contains("sever_echoes") || i.isToggled() || tag.getLong("horizon_until")>now || tag.getLong("verdict_until")>now || tag.contains("anchor_until");
        if(active)i.markDirty(); return active;
    }
    @Override public void onForgetSkill(ManasSkillInstance i,LivingEntity entity) {
        onToggleOff(i,entity);
        i.getOrCreateTag().remove("sever_echoes");i.getOrCreateTag().remove("horizon_until");i.getOrCreateTag().remove("verdict_until");i.getOrCreateTag().remove("anchor_until");
        if(entity.level() instanceof ServerLevel level)clearVisuals(level,i.getOrCreateTag());
        super.onForgetSkill(i,entity);
    }
    public static void clearTransient(LivingEntity entity) {
        SkillAPI.getSkillsFrom(entity).getSkill(WildHunt.GODLY.get()).ifPresent(i->{
            var tag=i.getOrCreateTag();
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
        for(var key:java.util.List.of("horizon_visual","horizon_circle","anchor_visual","verdict_visual","verdict_circle"))removeVisual(level,tag,key);
    }
    private static void message(LivingEntity e,String message) { e.sendSystemMessage(Component.literal(message)); }
}
