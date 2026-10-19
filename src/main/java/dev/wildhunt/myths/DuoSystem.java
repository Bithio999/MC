package dev.wildhunt.myths;

import dev.wildhunt.*;
import io.github.manasmods.manascore.race.api.*;
import io.github.manasmods.tensura.race.TensuraRace;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

public final class DuoSystem {
    private DuoSystem() {}
    public static CompoundTag data(LivingEntity e) {
        var root=HunterData.of(e);
        if(!root.contains("myths_duo"))root.put("myths_duo",new CompoundTag());
        return root.getCompound("myths_duo");
    }
    public static Optional<ManasRaceInstance> primary(LivingEntity e) { return RaceAPI.getRaceFrom(e).getRace(); }
    public static Optional<ManasRaceInstance> secondary(LivingEntity e) {
        var tag=data(e); if(!tag.contains("secondary"))return Optional.empty();
        try { return Optional.of(ManasRaceInstance.fromNBT(tag.getCompound("secondary"))); }
        catch(RuntimeException ex) { return Optional.empty(); }
    }
    public static void saveSecondary(LivingEntity e,ManasRaceInstance instance) { data(e).put("secondary",instance.toNBT()); }
    public static boolean awakened(LivingEntity e) {
        var d=TensuraStorages.getExistenceFrom(e); return d.isTrueHero() || d.isTrueDemonLord();
    }
    public static boolean terminal(ManasRaceInstance race,LivingEntity e) { return race.getNextEvolutions(e).isEmpty(); }
    public static Optional<RaceFamily> primaryFamily(LivingEntity e) { return primary(e).flatMap(r->RaceFamily.from(r.getRace())); }
    public static Optional<RaceFamily> secondaryFamily(LivingEntity e) { return RaceFamily.byKey(data(e).getString("secondary_family")); }
    public static boolean converged(LivingEntity e) { return primary(e).map(r->r.getRace() instanceof FusionRace).orElse(false); }
    public static String select(ServerPlayer p,RaceFamily family) {
        var first=primary(p);
        if(first.isEmpty() || primaryFamily(p).isEmpty())return "Your primary race has no supported native lineage.";
        if(!DuoRules.eligible(terminal(first.get(),p),awakened(p),EnergyHelper.getBaseMaxEP(p),MythsConfig.SECONDARY_EP.get(),secondary(p).isPresent()))
            return "Selection requires a final primary evolution, True Hero or True Demon Lord awakening, "+Math.round(MythsConfig.SECONDARY_EP.get())+" base EP, and an empty secondary slot.";
        if(primaryFamily(p).get()==family)return "Choose a different ancestral race family.";
        var instance=family.baseRace().createDefaultInstance();
        data(p).putString("primary_race",first.get().getRaceId().toString());
        data(p).putString("primary_family",primaryFamily(p).get().key());
        data(p).putString("secondary_family",family.key()); saveSecondary(p,instance);
        instance.learnIntrinsicSkills(p); saveSecondary(p,instance); applySecondary(p,instance);
        ((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();
        return "Your secondary lineage is now "+family.title+". This choice is permanent for this life. Evolve both lineages before Convergent Awakening.";
    }
    public static List<ManasRace> options(LivingEntity p,ManasRaceInstance secondary) {
        // Respect native branching; the original ancestry remains recorded if a
        // Goblin branch becomes an Oni; all native branches remain available.
        return secondary.getNextEvolutions(p).stream().filter(r-> !(r instanceof FusionRace))
            .filter(r->RaceFamily.from(r).isPresent())
            .toList();
    }
    public static String evolve(ServerPlayer p,ResourceLocation targetId) {
        var current=secondary(p); if(current.isEmpty() || converged(p))return "No independent secondary race to evolve.";
        var target=options(p,current.get()).stream().filter(r->targetId.equals(r.getRegistryName())).findFirst();
        if(target.isEmpty())return "That is not a valid next secondary evolution.";
        if(current.get().getEvolutionProgress(p,target.get())<100)return "The native secondary evolution requirements are not met.";
        var next=target.get().createDefaultInstance(); next.deserialize(current.get().serialize(new CompoundTag()));
        saveSecondary(p,next); removeSecondary(p); applySecondary(p,next); next.learnIntrinsicSkills(p); saveSecondary(p,next);
        ((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync(); return "Secondary race evolved into "+target.get().getName().getString()+".";
    }
    public static String convergence(ServerPlayer p) {
        var a=primary(p); var b=secondary(p);
        if(a.isEmpty() || b.isEmpty() || converged(p) || data(p).getBoolean("awakened"))return "Two independent lineages are required; Convergent Awakening can happen only once.";
        if(!terminal(a.get(),p) || !terminal(b.get(),p) || !awakened(p) || !Double.isFinite(EnergyHelper.getBaseMaxEP(p)) || EnergyHelper.getBaseMaxEP(p)<MythsConfig.CONVERGENCE_EP.get())
            return "Both races must reach their final evolution. True Hero or True Demon Lord and "+Math.round(MythsConfig.CONVERGENCE_EP.get())+" base EP are required.";
        var first=RaceFamily.byKey(data(p).getString("primary_family")); var second=secondaryFamily(p);
        if(first.isEmpty() || second.isEmpty() || first.equals(second))return "The lineage combination is invalid.";
        var profile=FusionCatalog.profile(first.get(),second.get()); var destination=FusionCatalog.RACES.get(profile.key()).get();
        double aura=EnergyHelper.getBaseMaxAura(p), mp=EnergyHelper.getBaseMaxMagicule(p);
        var original=a.get().toNBT();
        // Native setRace validates its event before removing the old race.
        if(!RaceAPI.getRaceFrom(p).setRace(destination.createDefaultInstance(),true,false,Component.literal("Convergent Awakening: "+profile.name())))return "The native race system rejected the awakening.";
        removeSecondary(p); data(p).put("original_primary",original); data(p).putBoolean("awakened",true);
        data(p).putString("primary_race",destination.getRegistryName().toString());
        data(p).putString("fusion",profile.key());
        EnergyHelper.setMaxAura(p,aura*MythsConfig.CONVERGENCE_BOOST.get());
        EnergyHelper.setMaxMagicule(p,mp*MythsConfig.CONVERGENCE_BOOST.get());
        var existence=TensuraStorages.getExistenceFrom(p);
        existence.setAura(EnergyHelper.getMaxAura(p)); existence.setMagicule(EnergyHelper.getMaxMagicule(p));
        existence.setSpiritualHealth(p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)); existence.markDirty();
        p.setHealth(p.getMaxHealth()); ((io.github.manasmods.manascore.storage.api.StorageHolder)p).manasCore$sync();
        MythsVisuals.convergence(p.serverLevel(),p.position().add(0,1,0),first.get().color,second.get().color);
        return "You awakened as "+profile.name()+". This race has its own permanent EP growth.";
    }
    public static DuoRules.Gains gains(LivingEntity p,double nativeAura,double nativeMagicule) {
        var current=primary(p);
        if(current.isPresent() && current.get().getRace() instanceof FusionRace fusion)return fusion.profile.gains();
        var second=secondaryFamily(p);
        if(second.isEmpty() || secondary(p).isEmpty())return new DuoRules.Gains(nativeAura,nativeMagicule);
        // The primary race's alignment, plus half the SECONDARY BASE race's weights.
        var first=current.filter(r->r.getRace() instanceof TensuraRace).map(r->((TensuraRace)r.getRace()).getAlignment());
        var base=first.map(a->new DuoRules.Gains(a.getAuraGainMultiplier(),a.getMagiculeGainMultiplier())).orElse(new DuoRules.Gains(nativeAura,nativeMagicule));
        return base.plusHalf(second.get().baseGains());
    }
    public static boolean sharedAttribute(Holder<Attribute> h) {
        return Set.of(Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.MOVEMENT_SPEED,Attributes.KNOCKBACK_RESISTANCE,TensuraAttributes.MAX_SPIRITUAL_HEALTH).contains(h);
    }
    public static ResourceLocation modifier(Holder<Attribute> holder) { return WildHunt.id("secondary/"+holder.unwrapKey().orElseThrow().location().getPath()); }
    public static void applySecondary(LivingEntity p,ManasRaceInstance secondary) {
        secondary.getRace().getAttributeModifiers().forEach((holder,t)-> {
            var attr=p.getAttribute(holder);
            if(attr!=null && sharedAttribute(holder))attr.addOrUpdateTransientModifier(new AttributeModifier(modifier(holder),t.amount()*.5,t.operation()));
        });
    }
    public static void removeSecondary(LivingEntity p) {
        for(var holder:List.of(Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.MOVEMENT_SPEED,Attributes.KNOCKBACK_RESISTANCE,TensuraAttributes.MAX_SPIRITUAL_HEALTH)) {
            var attr=p.getAttribute(holder); if(attr!=null)attr.removeModifier(modifier(holder));
        }
    }
    public static List<ManasRaceInstance> inherited(LivingEntity e) {
        var result=new ArrayList<ManasRaceInstance>();
        if(converged(e) && data(e).contains("original_primary")) {
            try { result.add(ManasRaceInstance.fromNBT(data(e).getCompound("original_primary"))); }catch(RuntimeException ignored){}
        }
        secondary(e).ifPresent(result::add); return result;
    }
    public static void saveInherited(LivingEntity e,ManasRaceInstance instance) {
        if(secondary(e).map(s->s.getRaceId().equals(instance.getRaceId())).orElse(false))saveSecondary(e,instance);
        else if(converged(e))data(e).put("original_primary",instance.toNBT());
    }
    public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        SecondaryAbility.tick(p);
        if(p.tickCount%20!=0)return;
        var current=primary(p); var d=data(p);
        if(d.contains("primary_race") && current.isPresent() && !current.get().getRaceId().toString().equals(d.getString("primary_race"))) {
            removeSecondary(p); HunterData.of(p).remove("myths_duo"); return;
        }
        if(!converged(p))secondary(p).ifPresent(s->applySecondary(p,s));
        for(var instance:inherited(p)) {
            if(instance.isOnCooldown())instance.setCooldown(Math.max(0,instance.getCooldown()-1));
            if(instance.canTick(p))instance.onTick(p);
            saveInherited(p,instance);
        }
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer p) {SecondaryAbility.clear(p);if(!converged(p))secondary(p).ifPresent(s->applySecondary(p,s));}
    }
}
