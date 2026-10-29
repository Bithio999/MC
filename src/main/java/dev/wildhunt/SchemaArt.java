package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.*;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.*;

/** Persisted, ordered Battlewill builds. Positive attenuation never discounts a drawback. */
public final class SchemaArt extends Battlewill {
    public static final int SLOTS=6,MITOCHONDRIAL=1,STRENGTH=2;
    private static final String TREE="SchemaTree",REVISION="SchemaRevision",TRAIN="SchemaLastTraining";
    private static final ResourceLocation REGEN=WildHunt.id("schema_regeneration");
    private static final ResourceLocation ATTACK=WildHunt.id("schema_strength");
    @Override public MutableComponent getName(){return Component.literal("Schema");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(ChatFormatting.RED);}
    @Override public ResourceLocation getSkillIcon(){return WildHunt.id("textures/battlewill/schema.png");}
    @Override public int getMaxMastery(){return 2000;}
    @Override public int getAcquirementMastery(LivingEntity e){return -1000;}
    @Override public double getLearningPointRequirement(ManasSkillInstance i,LivingEntity e,int mode){return 1000;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return i.getMastery()>=0;}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "schema.build";}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal("Build");}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Learn: 1000 points. Master: 2000 points. Toggle to activate your saved Schema Tree; press Build to organize up to six effects, including repeated effects. Positive slots after the full-strength slots have 50%, 25%, 12.5%... power. Every 400 mastery unlocks another full-strength slot, reaching all six at 2000. Mitochondrial Build: +500% aura regeneration and -5% all outgoing damage per stack. Strength Build: +5 base attack damage and +2% damage taken per stack. Drawbacks always apply at full strength. Train by actually recovering aura or landing hits with Strength Build active. No activation or upkeep cost.");}
    public static int fullSlots(double mastery){return 1+Math.min(5,(int)(Math.max(0,mastery)/400));}
    public static double weight(int order,double mastery){int full=fullSlots(mastery);return order<full?1:Math.scalb(1d,-(order-full+1));}
    public static int[] build(ManasSkillInstance i){
        var raw=i.getOrCreateTag().getIntArray(TREE);var result=new int[SLOTS];int at=0;
        for(int value:raw)if(validEffect(value) && at<SLOTS)result[at++]=value;
        return result;
    }
    private static boolean validEffect(int effect){return effect==MITOCHONDRIAL || effect==STRENGTH;}
    public static int count(int[] tree){int n=0;for(int v:tree)if(validEffect(v))n++;return n;}
    public static int count(int[] tree,int effect){int n=0;for(int v:tree)if(v==effect)n++;return n;}
    public static double positive(int[] tree,double mastery){return positive(tree,mastery,MITOCHONDRIAL);}
    public static double positive(int[] tree,double mastery,int effect){double sum=0;int order=0;for(int value:tree)if(validEffect(value)){double w=weight(order++,mastery);if(value==effect)sum+=w;}return sum;}
    public static double bonus(int[] tree,double mastery,double battlewillBoost){return 5*positive(tree,mastery)*battlewillBoost;}
    public static double strengthBonus(int[] tree,double mastery,double battlewillBoost){return 5*positive(tree,mastery,STRENGTH)*battlewillBoost;}
    public static double penalty(int[] tree){return .05*count(tree,MITOCHONDRIAL);}
    public static double vulnerability(int[] tree){return .02*count(tree,STRENGTH);}
    public static Optional<ManasSkillInstance> active(LivingEntity owner){
        if(owner==null || !owner.isAlive() || owner.isSpectator())return Optional.empty();
        return SkillAPI.getSkillsFrom(owner).getSkill(WildHunt.SCHEMA.get()).filter(i->i.getMastery()>=0 && i.isToggled());
    }
    public static double battlewillBoost(LivingEntity owner){return ChainedState.active(owner)?ChainedState.buffMultiplier(owner):1;}
    public static double regenerationFactor(LivingEntity owner){return active(owner).map(i->1+bonus(build(i),i.getMastery(),battlewillBoost(owner))).orElse(1d);}
    public static double damageFactor(LivingEntity owner){return active(owner).map(i->1-penalty(build(i))).orElse(1d);}
    public static double damageTakenFactor(LivingEntity owner){return active(owner).map(i->1+vulnerability(build(i))).orElse(1d);}
    public static void trainStrength(LivingEntity owner){active(owner).filter(i->count(build(i),STRENGTH)>0).ifPresent(i->train(owner,1));}
    public static void discover(ServerPlayer p){
        if(SkillAPI.getSkillsFrom(p).getSkill(WildHunt.SCHEMA.get()).isEmpty())SkillHelper.learnSkill(p,WildHunt.SCHEMA.get().createLearningInstance(p));
    }
    public static boolean save(ServerPlayer p,int[] slots,int revision){
        var owned=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.SCHEMA.get());
        if(owned.isEmpty() || slots==null || slots.length!=SLOTS || Arrays.stream(slots).anyMatch(v->v!=0 && !validEffect(v)))return false;
        var i=owned.get();if(i.getMastery()<0 || !p.isAlive() || !i.canInteractSkill(p) || !WildHunt.SCHEMA.get().canActivateSkill(i,p,0) || i.getOrCreateTag().getInt(REVISION)!=revision)return false;
        i.getOrCreateTag().putIntArray(TREE,slots.clone());i.getOrCreateTag().putInt(REVISION,revision+1);i.markDirty();SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);refresh(p);return true;
    }
    public static CompoundTag snapshot(ServerPlayer p){
        var state=new CompoundTag();var owned=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.SCHEMA.get());
        state.putBoolean("allowed",owned.filter(i->i.getMastery()>=0).isPresent());
        owned.ifPresent(i->{state.putIntArray("tree",build(i));state.putDouble("mastery",i.getMastery());state.putInt("revision",i.getOrCreateTag().getInt(REVISION));state.putBoolean("toggled",i.isToggled());});
        state.putDouble("boost",battlewillBoost(p));return state;
    }
    public static void refresh(LivingEntity owner){
        if(owner.level().isClientSide())return;
        double strength=active(owner).map(i->strengthBonus(build(i),i.getMastery(),battlewillBoost(owner))).orElse(0d);
        // Apply the Battlewill boost exactly once, without feeding this derived modifier back to ChainedBuffs.
        try(var ignored=ChainedContext.ability(owner,null)){
            modifier(owner.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),REGEN,regenerationFactor(owner)-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            modifier(owner.getAttribute(Attributes.ATTACK_DAMAGE),ATTACK,strength,AttributeModifier.Operation.ADD_VALUE);
        }
    }
    private static void modifier(AttributeInstance attribute,ResourceLocation id,double amount,AttributeModifier.Operation operation){
        if(attribute==null)return;var old=attribute.getModifier(id);
        if(amount<=0){if(old!=null)attribute.removeModifier(id);}
        else if(old==null || Math.abs(old.amount()-amount)>1e-8)attribute.addOrUpdateTransientModifier(new AttributeModifier(id,amount,operation));
    }
    public static void train(LivingEntity owner,double recovered){
        if(recovered<=0 || owner.level().isClientSide())return;
        active(owner).filter(i->count(build(i))>0 && !i.isMastered(owner)).ifPresent(i->{
            long now=owner.level().getGameTime();var tag=i.getOrCreateTag();
            if(!tag.contains(TRAIN) || now-tag.getLong(TRAIN)>=20){tag.putLong(TRAIN,now);i.addMasteryPoint(owner);i.markDirty();SkillAPI.getSkillsFrom(owner).checkAndMarkDirty(i);refresh(owner);}
        });
    }
    @Override public void onToggleOn(ManasSkillInstance i,LivingEntity e){i.setToggled(true);refresh(e);}
    @Override public void onToggleOff(ManasSkillInstance i,LivingEntity e){i.setToggled(false);refresh(e);}
    @Override public void onForgetSkill(ManasSkillInstance i,LivingEntity e){modifier(e.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER),REGEN,0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);modifier(e.getAttribute(Attributes.ATTACK_DAMAGE),ATTACK,0,AttributeModifier.Operation.ADD_VALUE);super.onForgetSkill(i,e);}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){if(e instanceof ServerPlayer p && mode==0 && i.getMastery()>=0 && canActivateSkill(i,p,0))SchemaNetwork.send(p,true,"");}
    public static void register(){
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e)->{if(e.getEntity() instanceof LivingEntity owner && !owner.level().isClientSide()){refresh(owner);if(owner instanceof ServerPlayer p && p.tickCount%20==0)discover(p);}});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e)->{if(e.getEntity() instanceof ServerPlayer p){discover(p);refresh(p);}});
        // Run after native effect bonuses and pack damage handlers, including paths that skip skill callbacks.
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,(LivingDamageEvent.Pre e)->{var owner=e.getSource().getEntity() instanceof LivingEntity living?living:null;e.setNewDamage((float)(e.getNewDamage()*damageFactor(owner)*damageTakenFactor(e.getEntity())));});
        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Post e)->{if(e.getNewDamage()>0 && !e.getEntity().level().isClientSide() && e.getSource().getEntity() instanceof LivingEntity owner)trainStrength(owner);});
    }
}
