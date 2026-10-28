package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.*;

public final class ChainedSkill extends Skill {
    public ChainedSkill(){super(SkillType.UNIQUE);}
    @Override public MutableComponent getName(){return Component.literal("Chained");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(net.minecraft.ChatFormatting.GOLD);}
    @Override public ResourceLocation getSkillIcon(){return WildHunt.id("textures/skill/chained.png");}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Heavenly Restriction. True passive: 5x aura-capacity gains from every source, excluding the saved starting aura pool; no future maximum-magicule gains; retain normal 1x magicule regeneration and convert its bonus rate above 1x into aura regeneration. New skills are restricted to resistances/nullifications and your recorded reincarnation skills and their derived grants; Battlewills remain learnable; later lineage evolutions also grant aura recovery. Resistance/Nullification acquisition costs current aura instead of permanent magicules. Toggle: +9 Battlewill learning/mastery points, +19 resistance learning points, 5x Battlewill damage, 3x numeric Battlewill buffs except Aura Sword, and 3x all outgoing Physical damage. True passive, mastered: Broken Limit lowers enemy resistance/nullification by one tier for your hits and ignores dodge, independent of toggle and slots. In slot: 100% projectile dodge, 25% other dodge, subject to explicit source bypass. Press: Severance Rush, an instant teleport with a 16-block aim reach that stops before an aimed enemy, ignores fluids, and supports upward air aim, with 3-block path and 4-block arrival hits. Costs 2% current aura; 1-second cooldown; path damage is 1.5 times the square root of aura spent, arrival damage is twice that. The dash deals only Physical damage, not Battlewill.");}
    @Override public int getMaxMastery(){return 1_000;}
    @Override public double getDefaultAcquiringMagiculeCost(){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return Math.max(0,TensuraStorages.getExistenceFrom(e).getAura()*.02);}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal("Severance Rush");}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "chained.severance_rush";}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return i.getMastery()>=0;}
    @Override public boolean canTick(ManasSkillInstance i,LivingEntity e){return true;}
    @Override public void onLearnSkill(ManasSkillInstance i,LivingEntity e){super.onLearnSkill(i,e);ChainedState.initialize(e);}
    @Override public void onTick(ManasSkillInstance i,LivingEntity e){if(!e.level().isClientSide())ChainedState.refresh(e);}
    @Override public void onToggleOn(ManasSkillInstance i,LivingEntity e){i.setToggled(true);ChainedState.refresh(e);}
    @Override public void onToggleOff(ManasSkillInstance i,LivingEntity e){i.setToggled(false);ChainedState.refresh(e);}
    @Override public void onForgetSkill(ManasSkillInstance i,LivingEntity e){super.onForgetSkill(i,e);ChainedState.remove(e);}
    @Override public boolean onBeingDamaged(ManasSkillInstance i,LivingEntity e,DamageSource source,float amount){
        if(!isInSlot(e,i) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || ((TensuraDamageSource)source).tensura$isDodgeBypass() || ChainedState.mastered(GodlyPassives.attacker(source)))return true;
        return e.getRandom().nextDouble()>=(source.getDirectEntity() instanceof Projectile || source.is(DamageTypeTags.IS_PROJECTILE)?1:.25);
    }
    public static float pathDamage(double spent){return (float)(1.5*Math.sqrt(Math.max(0,spent)));}
    public static float arrivalDamage(double spent){return pathDamage(spent)*2;}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity caster,int slot,int mode){
        if(!(caster.level() instanceof ServerLevel level) || mode!=0 || i.onCoolDown(0) || !canInteractSkill(i,caster) || !canActivateSkill(i,caster,0))return;
        var destination=ChainedDash.destination(caster);if(destination.isEmpty())return;
        Vec3 start=caster.position(),end=destination.get();
        var energy=TensuraStorages.getExistenceFrom(caster);double spent=getAuraCost(caster,i,0);if(!Double.isFinite(spent) || spent<=0 || energy.getAura()<spent)return;
        energy.setAura(energy.getAura()-spent);energy.markDirty();i.setCoolDown(1,0);addMasteryPoint(i,caster);
        Vec3 from=start.add(0,caster.getBbHeight()*.5,0),to=end.add(0,caster.getBbHeight()*.5,0),travel=to.subtract(from);double length=travel.lengthSqr();
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(4),e->HunterData.canHarm(caster,e))){
            Vec3 center=target.getBoundingBox().getCenter(),closest=from.add(travel.scale(Math.clamp(center.subtract(from).dot(travel)/length,0,1)));
            if(center.distanceToSqr(closest)<=9)hit(i,caster,target,spent,pathDamage(spent));
            if(target.isAlive() && center.distanceToSqr(to)<=16)hit(i,caster,target,spent,arrivalDamage(spent));
        }
        caster.unRide();caster.teleportTo(end.x,end.y,end.z);caster.resetFallDistance();caster.hurtMarked=true;caster.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        ArtVisualEntity.spawn(level,ArtVisualEntity.CHAINED_RUSH,from,travel,14);
        level.playSound(null,BlockPos.containing(end),io.github.manasmods.tensura.registry.sound.TensuraSoundEvents.INSTANT_MOVE.get(),ABILITY_SOUND,1,1);
        level.playSound(null,BlockPos.containing(start),SoundEvents.CHAIN_BREAK,SoundSource.PLAYERS,.7f,.8f);
    }
    private void hit(ManasSkillInstance i,LivingEntity caster,LivingEntity target,double spent,float damage){
        var source=createSource(i,caster,net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK,0);((TensuraDamageSource)source).tensura$setAuraCost(spent);((IndependentHitSource)source).myths$independentHit();target.hurt(source,damage);
    }
}
