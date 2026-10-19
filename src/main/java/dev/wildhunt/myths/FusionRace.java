package dev.wildhunt.myths;

import com.mojang.datafixers.util.Pair;
import dev.wildhunt.WildHunt;
import io.github.manasmods.manascore.race.api.*;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.race.TensuraRace;
import io.github.manasmods.tensura.storage.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;

/** A real native race per ancestral pair, not a display name over the old race. */
public final class FusionRace extends TensuraRace {
    public final FusionCatalog.Profile profile;
    public FusionRace(FusionCatalog.Profile profile) { super(Difficulty.EXTREME); this.profile=profile; }
    @Override public MutableComponent getName() { return Component.literal(profile.name()); }
    @Override public MutableComponent getRaceDescription() {
        return Component.literal(profile.description()+" Convergent Awakening. EP growth: "+profile.aura()+" aura / "+profile.magicules()+" magicules per native EP unit.");
    }
    @Override public ResourceLocation getRaceIcon() { return WildHunt.id("textures/race/convergence/"+profile.key()+".png"); }
    @Override public Pair<Double,Double> getBaseAuraRange() { return Pair.of(0d,0d); }
    @Override public Pair<Double,Double> getBaseMagiculeRange() { return Pair.of(0d,0d); }
    @Override public Alignment getAlignment() { return Alignment.CHAOS; }
    @Override public List<ManasRace> getNextEvolutions(ManasRaceInstance instance, LivingEntity entity) { return List.of(); }
    @Override public List<ManasRace> getPreviousEvolutions(ManasRaceInstance instance, LivingEntity entity) { return List.of(profile.first().finalRace(),profile.second().finalRace()); }
    @Override public List<ManasSkill> getIntrinsicSkills(ManasRaceInstance instance, LivingEntity entity) {
        var result=new LinkedHashSet<ManasSkill>();
        for(var f:List.of(profile.first(),profile.second())) {
            var r=f.finalRace(); result.addAll(r.getIntrinsicSkills(r.createDefaultInstance(),entity));
        }
        return List.copyOf(result);
    }
    @Override public void onRaceSet(ManasRaceInstance instance, LivingEntity entity) {
        var data=TensuraStorages.getExistenceFrom(entity); data.setAlignment(Alignment.CHAOS); data.setOriginalAlignment(Alignment.CHAOS); data.markDirty();
    }
    private Map<Holder<Attribute>,Double> bonuses() {
        var result=new LinkedHashMap<Holder<Attribute>,Double>();
        for(var family:List.of(profile.first(),profile.second())) {
            for(var entry:family.finalRace().getAttributeModifiers().entrySet()) {
                if(DuoSystem.sharedAttribute(entry.getKey()) && entry.getValue().operation()==AttributeModifier.Operation.ADD_VALUE)
                    result.merge(entry.getKey(),entry.getValue().amount()*.65,Double::sum);
            }
        }
        result.merge(Attributes.MAX_HEALTH,80d,Double::sum);
        result.merge(TensuraAttributes.MAX_SPIRITUAL_HEALTH,1500d,Double::sum);
        result.merge(Attributes.ATTACK_DAMAGE,4d+(profile.first().ordinal()+profile.second().ordinal())*.15,Double::sum);
        return result;
    }
    @Override public Map<Holder<Attribute>,ManasRace.AttributeTemplate> getAttributeModifiers() {
        var result=new LinkedHashMap<Holder<Attribute>,ManasRace.AttributeTemplate>();
        bonuses().forEach((holder,amount)->result.put(holder,new ManasRace.AttributeTemplate(WildHunt.id("convergence/"+holder.unwrapKey().orElseThrow().location().getPath()),holder.equals(Attributes.MAX_HEALTH)?Math.min(amount,1000):amount,AttributeModifier.Operation.ADD_VALUE)));
        return result;
    }
    @Override public void addAttributeModifiers(ManasRaceInstance instance, LivingEntity entity) {
        bonuses().forEach((holder,amount)-> {
            var attr=entity.getAttribute(holder); if(attr==null)return;
            if(holder.equals(Attributes.MAX_HEALTH))amount=Math.min(amount,1000);
            attr.addOrReplacePermanentModifier(new AttributeModifier(WildHunt.id("convergence/"+holder.unwrapKey().orElseThrow().location().getPath()),amount,AttributeModifier.Operation.ADD_VALUE));
        });
    }
    @Override public void removeAttributeModifiers(ManasRaceInstance instance, LivingEntity entity) {
        bonuses().keySet().forEach(holder->{ var a=entity.getAttribute(holder); if(a!=null)a.removeModifier(WildHunt.id("convergence/"+holder.unwrapKey().orElseThrow().location().getPath())); });
    }
    @Override public boolean canActivateAbility(ManasRaceInstance instance, LivingEntity entity) { return !instance.isOnCooldown() && !io.github.manasmods.tensura.storage.ep.ExistenceStorage.isInSleepMode(TensuraStorages.getExistenceFrom(entity))
        && !io.github.manasmods.tensura.effect.debuff.SleepEffect.isSleeping(entity)
        && java.util.stream.Stream.of(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.MOVEMENT_INTERFERENCE,io.github.manasmods.tensura.registry.effect.TensuraMobEffects.INFINITE_IMPRISONMENT,io.github.manasmods.tensura.registry.effect.TensuraMobEffects.REST,io.github.manasmods.tensura.registry.effect.TensuraMobEffects.SHADOW_STEP)
            .noneMatch(effect->entity.hasEffect(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(effect))); }
    @Override public void onActivateAbility(ManasRaceInstance instance, LivingEntity entity) {
        if(!(entity.level() instanceof net.minecraft.server.level.ServerLevel level) || !canActivateAbility(instance,entity))return;
        if(io.github.manasmods.tensura.util.EnergyHelper.isOutOfEnergy(entity,1500,0))return;
        entity.heal(Math.min(80,entity.getMaxHealth()*.1f));
        for(var target:level.getEntitiesOfClass(LivingEntity.class,entity.getBoundingBox().inflate(7),e->dev.wildhunt.HunterData.canHarm(entity,e))) {
            var delta=target.position().subtract(entity.position()).normalize().scale(.9); target.push(delta.x,.25,delta.z); target.hurtMarked=true;
        }
        MythsVisuals.convergence(level,entity.position().add(0,1,0),profile.first().color,profile.second().color);
        instance.setCooldown(30); instance.markDirty();
    }
}
