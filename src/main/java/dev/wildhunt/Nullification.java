package dev.wildhunt;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.data.TensuraTags;
import io.github.manasmods.tensura.effect.template.TensuraMobEffectInstance;
import io.github.manasmods.tensura.entity.magic.barrier.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.phys.*;
import java.util.List;

public final class Nullification {
    public static final ResourceKey<Enchantment> ENGRAVING=ResourceKey.create(Registries.ENCHANTMENT,WildHunt.id("nullification"));
    private Nullification(){}
    public static boolean sealed(LivingEntity owner){return owner.hasEffect(HeavenlyArsenal.SKILL_SEAL);}
    public static EventResult activate(Changeable<ManasSkillInstance> instance,LivingEntity owner,int key,int mode){return sealed(owner)?EventResult.interruptFalse():EventResult.pass();}
    public static boolean engraved(LivingEntity owner){
        ItemStack weapon=owner.getMainHandItem();
        if(weapon.getItem() instanceof InvertedSpearItem){if(!owner.level().isClientSide())InvertedSpearItem.engrave(weapon,owner.level().registryAccess());return true;}
        return owner.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(ENGRAVING).map(h->EnchantmentHelper.getItemEnchantmentLevel(h,weapon)>0).orElse(false);
    }
    /** A held spear does not lend its engraving to unrelated spells or projectiles. */
    public static LivingEntity wielder(DamageSource source){
        if(!(source.getDirectEntity() instanceof LivingEntity owner) || source.getEntity()!=owner)return null;
        var ability=((TensuraDamageSource)source).tensura$getAbilityInstance();
        if(ability!=null && !(ability.getSkill() instanceof ChainedSkill))return null;
        if(!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && !source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))return null;
        return engraved(owner)?owner:null;
    }
    public static EventResult beforeDamage(Skills skills,LivingEntity target,DamageSource source,Changeable<Float> damage){
        var owner=wielder(source);if(owner==null || damage.get()<=0 || !HunterData.canHarm(owner,target))return EventResult.pass();
        shatter(target);
        for(var effect:List.copyOf(target.getActiveEffects())){
            var origin=((TensuraMobEffectInstance)effect).tensura$getSourceAbility();
            if(effect.getEffect().is(TensuraTags.MobEffects.SKILL_EFFECT) || origin!=null && !origin.isEmpty())target.removeEffect(effect.getEffect());
        }
        target.addEffect(new MobEffectInstance(HeavenlyArsenal.SKILL_SEAL,40,0,false,true,true),owner);
        // Entity barriers around their owner also shatter when the blade reaches them.
        if(target.level() instanceof ServerLevel level)for(var barrier:level.getEntitiesOfClass(BarrierEntity.class,target.getBoundingBox().inflate(64),b->b.getOwner()==target))breakBarrier(barrier);
        return EventResult.pass();
    }
    public static void shatter(LivingEntity target){
        for(var key:List.of(TensuraAttributes.MULTILAYER_BARRIER,TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MAGIC_BARRIER)){
            var attribute=target.getAttribute(key);if(attribute!=null && attribute.getValue()>0){attribute.removeModifiers();attribute.setBaseValue(0);}
        }
        burst(target);
    }
    public static boolean breakBarrier(BarrierEntity barrier,DamageSource source){
        var owner=wielder(source);if(owner==null || barrier.getOwner() instanceof LivingEntity protectedOwner && !HunterData.canHarm(owner,protectedOwner))return false;
        breakBarrier(barrier);return true;
    }
    private static void breakBarrier(BarrierEntity barrier){if(barrier.level().isClientSide())return;barrier.setHealth(0);burst(barrier);barrier.discard();}
    public static void sweep(LivingEntity owner,Vec3 start,Vec3 end,double radius){
        if(!(owner.level() instanceof ServerLevel level) || !engraved(owner))return;
        for(var barrier:level.getEntitiesOfClass(BarrierEntity.class,new AABB(start,end).inflate(radius))){
            if(barrier.getOwner() instanceof LivingEntity protectedOwner && !HunterData.canHarm(owner,protectedOwner))continue;
            var box=barrier.getBoundingBox().inflate(radius);
            if(box.contains(start) || box.clip(start,end).isPresent())breakBarrier(barrier);
        }
    }
    private static void burst(Entity target){
        if(!(target.level() instanceof ServerLevel level))return;
        var center=target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.END_ROD,center.x,center.y,center.z,16,.6,.6,.6,.08);
        level.sendParticles(ParticleTypes.CRIT,center.x,center.y,center.z,24,.7,.7,.7,.25);
        level.playSound(null,target.blockPosition(),SoundEvents.GLASS_BREAK,SoundSource.PLAYERS,.8f,.65f);
    }
}
