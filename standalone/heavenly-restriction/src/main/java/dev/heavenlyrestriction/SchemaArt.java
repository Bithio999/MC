package dev.heavenlyrestriction;

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
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.*;

/** Persisted, ordered Battlewill builds. Positive attenuation never discounts a drawback. */
public final class SchemaArt extends Battlewill {
    public static final int SLOTS=6,MITOCHONDRIAL=1;
    private static final String TREE="SchemaTree",REVISION="SchemaRevision",TRAIN="SchemaLastTraining";
    private static final ResourceLocation REGEN=HeavenlyRestriction.id("schema_regeneration");
    @Override public MutableComponent getName(){return Component.literal("Schema");}
    @Override public MutableComponent getColoredName(){return getName().withStyle(ChatFormatting.RED);}
    @Override public ResourceLocation getSkillIcon(){return HeavenlyRestriction.id("textures/battlewill/schema.png");}
    @Override public int getMaxMastery(){return 2000;}
    @Override public int getAcquirementMastery(LivingEntity e){return -1000;}
    @Override public double getLearningPointRequirement(ManasSkillInstance i,LivingEntity e,int mode){return 1000;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return 0;}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return i.getMastery()>=0;}
    @Override public String getModeId(ManasSkillInstance i,int mode){return "schema.build";}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal("Build");}
    @Override public MutableComponent getSkillDescription(){return Component.literal("Learn: 1000 points. Master: 2000 points. Toggle to activate your saved Schema Tree; press Build to organize up to six effects, including repeated effects. Positive slots after the full-strength slots have 50%, 25%, 12.5%... power. Every 400 mastery unlocks another full-strength slot, reaching all six at 2000. Mitochondrial Build: +500% aura regeneration and -10% all outgoing damage per stack. Drawbacks always apply at full strength. Training occurs while the active build actually restores aura. No activation or upkeep cost.");}
    public static int fullSlots(double mastery){return 1+Math.min(5,(int)(Math.max(0,mastery)/400));}
    public static double weight(int order,double mastery){int full=fullSlots(mastery);return order<full?1:Math.scalb(1d,-(order-full+1));}
    public static int[] build(ManasSkillInstance i){
        var raw=i.getOrCreateTag().getIntArray(TREE);var result=new int[SLOTS];int at=0;
        for(int value:raw)if(value==MITOCHONDRIAL && at<SLOTS)result[at++]=value;
        return result;
    }
    public static int count(int[] tree){int n=0;for(int v:tree)if(v==MITOCHONDRIAL)n++;return n;}
    public static double positive(int[] tree,double mastery){double sum=0;int order=0;for(int value:tree)if(value==MITOCHONDRIAL)sum+=weight(order++,mastery);return sum;}
    public static double bonus(int[] tree,double mastery,double battlewillBoost){return 5*positive(tree,mastery)*battlewillBoost;}
    public static double penalty(int[] tree){return .1*count(tree);}
    public static Optional<ManasSkillInstance> active(LivingEntity owner){
        if(owner==null || !owner.isAlive() || owner.isSpectator())return Optional.empty();
        return SkillAPI.getSkillsFrom(owner).getSkill(HeavenlyRestriction.SCHEMA.get()).filter(i->i.getMastery()>=0 && i.isToggled());
    }
    public static double battlewillBoost(LivingEntity owner){return ChainedState.active(owner)?ChainedState.buffMultiplier(owner):1;}
    public static double regenerationFactor(LivingEntity owner){return active(owner).map(i->1+bonus(build(i),i.getMastery(),battlewillBoost(owner))).orElse(1d);}
    public static double damageFactor(LivingEntity owner){return active(owner).map(i->1-penalty(build(i))).orElse(1d);}
    public static void discover(ServerPlayer p){
        if(SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).isEmpty())SkillHelper.learnSkill(p,HeavenlyRestriction.SCHEMA.get().createLearningInstance(p));
    }
    public static boolean save(ServerPlayer p,int[] slots,int revision){
        var owned=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get());
        if(owned.isEmpty() || slots==null || slots.length!=SLOTS || Arrays.stream(slots).anyMatch(v->v<0 || v>MITOCHONDRIAL))return false;
        var i=owned.get();if(i.getMastery()<0 || !p.isAlive() || !i.canInteractSkill(p) || !HeavenlyRestriction.SCHEMA.get().canActivateSkill(i,p,0) || i.getOrCreateTag().getInt(REVISION)!=revision)return false;
        i.getOrCreateTag().putIntArray(TREE,slots.clone());i.getOrCreateTag().putInt(REVISION,revision+1);i.markDirty();SkillAPI.getSkillsFrom(p).checkAndMarkDirty(i);refresh(p);return true;
    }
    public static CompoundTag snapshot(ServerPlayer p){
        var state=new CompoundTag();var owned=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get());
        state.putBoolean("allowed",owned.filter(i->i.getMastery()>=0).isPresent());
        owned.ifPresent(i->{state.putIntArray("tree",build(i));state.putDouble("mastery",i.getMastery());state.putInt("revision",i.getOrCreateTag().getInt(REVISION));state.putBoolean("toggled",i.isToggled());});
        state.putDouble("boost",battlewillBoost(p));return state;
    }
    public static void refresh(LivingEntity owner){
        if(owner.level().isClientSide())return;var attribute=owner.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER);if(attribute==null)return;
        double amount=regenerationFactor(owner)-1;var old=attribute.getModifier(REGEN);
        // Apply the Battlewill boost exactly once, without feeding this derived modifier back to ChainedBuffs.
        try(var ignored=ChainedContext.ability(owner,null)){
            if(amount<=0){if(old!=null)attribute.removeModifier(REGEN);}
            else if(old==null || Math.abs(old.amount()-amount)>1e-8)attribute.addOrUpdateTransientModifier(new AttributeModifier(REGEN,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
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
    @Override public void onForgetSkill(ManasSkillInstance i,LivingEntity e){var attr=e.getAttribute(TensuraAttributes.AURA_REGENERATION_MULTIPLIER);if(attr!=null)attr.removeModifier(REGEN);super.onForgetSkill(i,e);}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){if(e instanceof ServerPlayer p && mode==0 && i.getMastery()>=0 && canActivateSkill(i,p,0))SchemaNetwork.send(p,true,"");}
    public static void register(){
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e)->{if(e.getEntity() instanceof LivingEntity owner && !owner.level().isClientSide()){refresh(owner);if(owner instanceof ServerPlayer p && p.tickCount%20==0)discover(p);}});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e)->{if(e.getEntity() instanceof ServerPlayer p){discover(p);refresh(p);}});
        SkillEvents.SKILL_DAMAGE_CALCULATION.register((storage,target,source,amount)->{if(source.getEntity() instanceof LivingEntity owner)amount.set((float)(amount.get()*damageFactor(owner)));return dev.architectury.event.EventResult.pass();});
    }
}
