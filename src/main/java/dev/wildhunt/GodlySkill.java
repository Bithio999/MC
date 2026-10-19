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

/** Native Ultimate, addon Godly. Fixed spatial damage; no EP multiplier. */
public final class GodlySkill extends Skill {
    private static final ResourceLocation PASSIVE=WildHunt.id("aion_continuum");
    public GodlySkill() { super(SkillType.ULTIMATE); }
    @Override public MutableComponent getName() { return Component.translatableWithFallback("wildhunt.skill.aion","Aion, God of Causality"); }
    @Override public MutableComponent getColoredName() { return getName().withColor(0xE8C56F); }
    @Override public ResourceLocation getSkillIcon() { return WildHunt.id("textures/skill/aion.png"); }
    @Override public MutableComponent getSkillDescription() { return Component.literal("[Godly / Ultimate] Sever worldlines with a piercing spatial ray. Place an Event Horizon that draws enemies and slows projectiles. Causal Reversal records a 12-second checkpoint; press again to return and recover lost HP. Genesis Verdict collapses your horizon in three warned pulses. Toggle Unbroken Continuum for a recovering native barrier. Aura only; fixed damage enhanced by mastery, never EP."); }
    @Override public int getMaxMastery() { return 50_000; }
    @Override public double getDefaultAcquiringMagiculeCost() { return 0; }
    @Override public int getModes(ManasSkillInstance i) { return 4; }
    @Override public String getModeId(ManasSkillInstance i,int mode) { return switch(mode) {case 0->"aion.worldline_sever";case 1->"aion.event_horizon";case 2->"aion.causal_reversal";default->"aion.genesis_verdict";}; }
    @Override public Component getModeName(ManasSkillInstance i,int mode) { return Component.literal(switch(mode) {case 0->"Worldline Sever";case 1->"Event Horizon";case 2->"Causal Reversal";default->"Genesis Verdict";}); }
    @Override public int nextMode(LivingEntity e,ManasSkillInstance i,int mode,boolean reverse) { return Math.floorMod(mode+(reverse?-1:1),4); }
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode) { return switch(mode) {case 0->2500;case 1->4000;case 2->1800;default->12000;}; }
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode) { return 0; }
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e) { return true; }
    @Override public boolean canTick(ManasSkillInstance i,LivingEntity e) { return false; }
    @Override public void onPressed(ManasSkillInstance i,LivingEntity caster,int slot,int mode) {
        if(!(caster.level() instanceof ServerLevel level) || mode<0 || mode>3 || i.onCoolDown(mode) || !canInteractSkill(i,caster) || !canActivateSkill(i,caster,mode))return;
        var tag=i.getOrCreateTag(); long now=level.getGameTime();
        if(mode==3 && (tag.getLong("horizon_until")<=now || !sameDimension(i,caster))) { message(caster,"Place an Event Horizon before collapsing it."); return; }
        boolean returning=mode==2 && tag.getLong("anchor_until")>now;
        Vec3 anchor=ApexArts.point(tag,"anchor");
        if(returning && (!sameDimension(i,caster) || !level.noCollision(caster,caster.getBoundingBox().move(anchor.subtract(caster.position()))) || !level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(anchor)))) {
            message(caster,"Your recorded position is no longer safe."); return;
        }
        if(EnergyHelper.isOutOfEnergy(caster,i,mode))return;
        int cooldown;
        switch(mode) {
            case 0 -> {
                Vec3 from=caster.getEyePosition(),end=ApexArts.cursorPlacement(level,caster,64);
                ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_RAY,from,end.subtract(from),12);
                var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(1),e->HunterData.canHarm(caster,e) && e.getBoundingBox().inflate(.5).clip(from,end).isPresent());
                targets.sort(java.util.Comparator.comparingDouble(e->e.distanceToSqr(caster)));
                targets.stream().limit(5).forEach(target->hit(level,i,caster,target,360,0)); cooldown=6;
            }
            case 1 -> {
                Vec3 center=ApexArts.cursorPlacement(level,caster,40); ApexArts.point(tag,"horizon",center);
                tag.putLong("horizon_until",now+120); tag.putString("dimension",level.dimension().location().toString());
                ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_SPHERE,center,new Vec3(7,0,0),120); cooldown=18;
            }
            case 2 -> {
                if(returning) {
                    Vec3 before=caster.position(); caster.teleportTo(anchor.x,anchor.y,anchor.z);
                    caster.setDeltaMovement(Vec3.ZERO); caster.hurtMarked=true;
                    caster.heal(Math.max(0,Math.min(caster.getMaxHealth(),tag.getFloat("anchor_hp"))-caster.getHealth()));
                    tag.remove("anchor_until"); ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_RAY,before.add(0,1,0),anchor.subtract(before),18); cooldown=40;
                } else {
                    ApexArts.point(tag,"anchor",caster.position()); tag.putFloat("anchor_hp",caster.getHealth()); tag.putLong("anchor_until",now+240);
                    tag.putString("dimension",level.dimension().location().toString());
                    ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_SPHERE,caster.position().add(0,.2,0),new Vec3(1.5,0,0),240); cooldown=2;
                }
            }
            default -> {
                ApexArts.point(tag,"verdict",ApexArts.point(tag,"horizon")); tag.remove("horizon_until");
                tag.putLong("verdict_until",now+61); tag.putLong("verdict_next",now+20);
                ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_SPHERE,ApexArts.point(tag,"verdict"),new Vec3(8,1,0),65); cooldown=60;
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
        if(!canInteractSkill(i,e) || !canActivateSkill(i,e,0) || EnergyHelper.isOutOfEnergy(e,120,0)) {onToggleOff(i,e);return;}
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
            var a=e.getAttribute(holder); if(a!=null)a.addOrUpdateTransientModifier(new AttributeModifier(PASSIVE,holder.equals(Attributes.KNOCKBACK_RESISTANCE)?.5:25,AttributeModifier.Operation.ADD_VALUE));
        }
        var barrier=e.getAttribute(TensuraAttributes.MULTILAYER_BARRIER);
        if(barrier!=null && e.level().getGameTime()>=i.getOrCreateTag().getLong("barrier_ready") && barrier.getModifier(PASSIVE)==null)
            barrier.addOrReplacePermanentModifier(new AttributeModifier(PASSIVE,600,AttributeModifier.Operation.ADD_VALUE));
    }
    public boolean tickActive(ManasSkillInstance i,LivingEntity e) {
        if(!(e.level() instanceof ServerLevel level))return false;
        var tag=i.getOrCreateTag(); long now=level.getGameTime();
        if(!e.isAlive() || !canInteractSkill(i,e) || !sameDimension(i,e)) {
            tag.remove("horizon_until"); tag.remove("verdict_until");
            if(tag.contains("anchor_until")) {tag.remove("anchor_until");i.setCoolDown(Math.max(40,i.getCoolDown(2)),2);}
        }
        if(i.isToggled()) {
            if(!e.isAlive() || !canInteractSkill(i,e))onToggleOff(i,e);
            else if(now>=tag.getLong("upkeep")) {
                if(EnergyHelper.isOutOfEnergy(e,120,0))onToggleOff(i,e);
                else {restore(i,e); tag.putLong("upkeep",now+20);}
            }
        }
        if(tag.contains("anchor_until") && now>=tag.getLong("anchor_until")) {tag.remove("anchor_until");i.setCoolDown(Math.max(40,i.getCoolDown(2)),2);}
        if(tag.getLong("horizon_until")>now && sameDimension(i,e)) {
            Vec3 center=ApexArts.point(tag,"horizon");
            for(var target:ApexArts.enemies(level,e,center,7)) {
                double resistance=1-Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),0,1);
                Vec3 pull=center.subtract(target.position()).normalize().scale(.16*resistance); target.push(pull.x,.01,pull.z); target.hurtMarked=true;
                if(now%20==0)hit(level,i,e,target,36,1);
            }
            for(var projectile:level.getEntitiesOfClass(Projectile.class,new AABB(center,center).inflate(7),p->p.getOwner()!=e && p.distanceToSqr(center)<49)) {
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(.85)); projectile.hurtMarked=true;
            }
        }
        if(tag.getLong("verdict_until")>now && now>=tag.getLong("verdict_next") && sameDimension(i,e)) {
            Vec3 center=ApexArts.point(tag,"verdict");
            for(var target:ApexArts.enemies(level,e,center,8))hit(level,i,e,target,140,3);
            ArtVisualEntity.spawn(level,ArtVisualEntity.CAUSAL_SPHERE,center,new Vec3(8,2,0),12); tag.putLong("verdict_next",now+20);
            level.playSound(null,net.minecraft.core.BlockPos.containing(center),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,1.5f,.5f);
        }
        boolean active=i.isToggled() || tag.getLong("horizon_until")>now || tag.getLong("verdict_until")>now || tag.contains("anchor_until");
        if(active)i.markDirty(); return active;
    }
    public static void clearTransient(LivingEntity entity) {
        SkillAPI.getSkillsFrom(entity).getSkill(WildHunt.GODLY.get()).ifPresent(i->{
            var tag=i.getOrCreateTag();
            if(tag.contains("anchor_until"))i.setCoolDown(Math.max(40,i.getCoolDown(2)),2);
            tag.remove("anchor_until");tag.remove("horizon_until");tag.remove("verdict_until");i.markDirty();
            if(i.isToggled()) { HunterData.of(entity).putBoolean("active_skills",true); }
        });
    }
    public static void damaged(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        SkillAPI.getSkillsFrom(event.getEntity()).getSkill(WildHunt.GODLY.get()).ifPresent(i->{if(i.isToggled()) {i.getOrCreateTag().putLong("barrier_ready",event.getEntity().level().getGameTime()+200);i.markDirty();}});
    }
    private static void message(LivingEntity e,String message) { e.sendSystemMessage(Component.literal(message)); }
}
