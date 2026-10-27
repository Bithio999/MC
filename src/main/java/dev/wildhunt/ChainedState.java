package dev.wildhunt;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.damage.*;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.Optional;

/** The vow and mastered Broken Limit follow ownership; Martial Supremacy uses the toggle. */
public final class ChainedState {
    public static final ResourceLocation VOW=WildHunt.id("chained_vow"),START=WildHunt.id("chained_starting_aura"),COMBAT=WildHunt.id("chained_combat");
    private ChainedState(){}
    public static Optional<ManasSkillInstance> instance(LivingEntity e){return e==null?Optional.empty():SkillAPI.getSkillsFrom(e).getSkill(WildHunt.CHAINED.get()).filter(i->i.getMastery()>=0);}
    public static boolean owned(LivingEntity e){return instance(e).isPresent();}
    public static boolean active(LivingEntity e){return instance(e).filter(i->i.isToggled() && i.getSkill().canInteractSkill(i,e)).isPresent();}
    public static boolean mastered(LivingEntity e){return instance(e).filter(i->i.isMastered(e)).isPresent();}
    public static double mastery(LivingEntity e){return instance(e).map(i->Math.clamp(i.getMastery()/i.getMaxMastery(),0,1)).orElse(0d);}
    public static long masteredBattlewills(LivingEntity e){return SkillAPI.getSkillsFrom(e).getLearnedSkills().stream().filter(i->i.getSkill() instanceof Battlewill && i.isMastered(e)).count();}
    public static boolean inherited(LivingEntity e,ManasSkill skill){return e!=null && skill!=null && HunterData.of(e).getList("chained_inheritance",Tag.TAG_STRING).contains(StringTag.valueOf(skill.getRegistryName().toString()));}
    public static void remember(LivingEntity e,ManasSkill skill){
        var data=HunterData.of(e);var list=data.getList("chained_inheritance",Tag.TAG_STRING);var key=StringTag.valueOf(skill.getRegistryName().toString());
        if(!list.contains(key)){list.add(key);data.put("chained_inheritance",list);}
    }
    private record BlockedNotice(String skill,long time){}
    private static final java.util.Map<LivingEntity,BlockedNotice> BLOCKED_NOTICES=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    public static MutableComponent blockedMessage(ManasSkill skill){
        return net.minecraft.network.chat.Component.translatableWithFallback(WildHunt.ID+".chained.blocked_skill",
            "Chained blocks %s. You can only acquire Battlewills, resistances/nullifications, inherited reincarnation skills, and racial aura recovery.",skill.getName()).withStyle(net.minecraft.ChatFormatting.RED);
    }
    public static void notifyBlocked(LivingEntity owner,ManasSkillInstance skill){
        if(!(owner instanceof net.minecraft.server.level.ServerPlayer player) || owner.level().isClientSide())return;
        String id=skill.getSkillId().toString();long now=owner.level().getGameTime();var previous=BLOCKED_NOTICES.get(owner);
        if(previous!=null && previous.skill().equals(id) && now>=previous.time() && now-previous.time()<20)return;
        BLOCKED_NOTICES.put(owner,new BlockedNotice(id,now));player.sendSystemMessage(blockedMessage(skill.getSkill()));
    }
    public static EventResult unlock(ManasSkillInstance i,LivingEntity e,Changeable<MutableComponent> message){
        if(e.level().isClientSide())return EventResult.pass();
        if(canLearn(e,i))return EventResult.pass();
        message.set(blockedMessage(i.getSkill()));notifyBlocked(e,i);return EventResult.interruptFalse();
    }
    public static boolean canLearn(LivingEntity e,ManasSkillInstance i){return e.level().isClientSide() || !owned(e) || ChainedContext.inherited(e) || ChainedContext.permitsRecovery(e,i.getSkill()) || i.getSkill()==WildHunt.CHAINED.get() || i.getSkill() instanceof Battlewill || i.getSkill() instanceof ResistSkill || inherited(e,i.getSkill()) || i.getParentSkill()!=null && inherited(e,i.getParentSkill());}
    public static double magiculeCeiling(LivingEntity e,double original){
        if(e==null || ChainedContext.initializing(e))return original;
        return instance(e).map(i->i.getOrCreateTag().contains("magicule_ceiling")?Math.min(original,i.getOrCreateTag().getDouble("magicule_ceiling")):original).orElse(original);
    }
    public static void recordStartingAura(LivingEntity e,double amount){
        if(e.level().isClientSide() || !Double.isFinite(amount) || amount<0)return;
        HunterData.of(e).putDouble("chained_starting_aura",amount);
        instance(e).ifPresent(i->{i.getOrCreateTag().putDouble("starting_aura",amount);i.markDirty();});
    }
    private static double startingAura(LivingEntity e){
        var data=HunterData.of(e);
        if(data.contains("chained_starting_aura"))return data.getDouble("chained_starting_aura");
        var i=instance(e).orElseThrow();var tag=i.getOrCreateTag();
        double base=io.github.manasmods.tensura.util.EnergyHelper.getBaseMaxAura(e);
        // Pre-0.32.2 lineage births used a fixed 20,000 raw aura. Exclude that
        // historical starting pool without rerolling it or discarding earned EP.
        boolean legacy=tag.contains("magicule_ceiling");
        double start=tag.contains("starting_aura")?tag.getDouble("starting_aura"):legacy?20_000:base;
        recordStartingAura(e,Math.max(0,start));return Math.max(0,start);
    }
    public static void initialize(LivingEntity e){
        if(e.level().isClientSide())return;
        if(owned(e))startingAura(e);
        instance(e).ifPresent(i->{if(!i.getOrCreateTag().contains("magicule_ceiling")){i.getOrCreateTag().putDouble("magicule_ceiling",io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(e));i.markDirty();}});refresh(e);
    }
    public static void finishInitialRace(LivingEntity e){
        instance(e).ifPresent(i->{i.getOrCreateTag().putDouble("magicule_ceiling",io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(e));i.markDirty();});refresh(e);
    }
    private static void modifier(LivingEntity e,net.minecraft.core.Holder<Attribute> h,ResourceLocation id,double value,AttributeModifier.Operation op,boolean enabled){
        var a=e.getAttribute(h);if(a==null)return;if(!enabled){a.removeModifier(id);return;}
        var old=a.getModifier(id);if(old==null || old.amount()!=value)a.addOrReplacePermanentModifier(new AttributeModifier(id,value,op));
    }
    public static void refresh(LivingEntity e){
        if(e.level().isClientSide())return;
        boolean owner=owned(e),on=owner && active(e),master=mastered(e);
        // (base - 0.8 * starting pool + later additions) * 5:
        // the initial pool stays unmultiplied; all subsequent gains get 5x.
        modifier(e,TensuraAttributes.MAX_AURA,START,owner?-.8*Math.min(startingAura(e),Math.max(0,e.getAttribute(TensuraAttributes.MAX_AURA).getBaseValue())):0,AttributeModifier.Operation.ADD_VALUE,owner);
        modifier(e,TensuraAttributes.MAX_AURA,VOW,4,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,owner);
        // Remove the retired attack-stat multiplier from existing saves.
        modifier(e,Attributes.ATTACK_DAMAGE,COMBAT,0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,false);
        modifier(e,TensuraAttributes.DODGE_NEGATE_CHANCE,COMBAT,100,AttributeModifier.Operation.ADD_VALUE,master);
        modifier(e,TensuraAttributes.RESISTANCE_DEGRADATION,COMBAT,1,AttributeModifier.Operation.ADD_VALUE,master);
        ChainedBuffs.refresh(e);
    }
    public static void remove(LivingEntity e){
        for(var h:java.util.List.of(TensuraAttributes.MAX_AURA,Attributes.ATTACK_DAMAGE,TensuraAttributes.DODGE_NEGATE_CHANCE,TensuraAttributes.RESISTANCE_DEGRADATION)){
            var a=e.getAttribute(h);if(a!=null){a.removeModifier(VOW);a.removeModifier(START);a.removeModifier(COMBAT);}
        }
        ChainedBuffs.refresh(e,false);
    }
    public static EventResult damage(Skills storage,LivingEntity target,DamageSource source,Changeable<Float> amount){
        var attacker=GodlyPassives.attacker(source);
        if(active(attacker)){
            float multiplier=TensuraDamageHelper.isPhysicalAttack(source)?3:1;
            if(TensuraDamageHelper.isBattlewill(source,attacker))multiplier*=5;
            if(multiplier!=1)amount.set(amount.get()*multiplier);
        }
        return EventResult.pass();
    }
    public static boolean isDash(DamageSource source){var i=((TensuraDamageSource)source).tensura$getAbilityInstance();return i!=null && i.getSkill()==WildHunt.CHAINED.get();}
}
