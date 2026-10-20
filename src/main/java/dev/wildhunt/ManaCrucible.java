package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Native hold channel: resource-conserving conversion, never a timed regeneration buff. */
final class ManaCrucible {
    static final double AURA_PER_MAGICULE=2;
    static final double MAGICULE_FRACTION_PER_SECOND=.05;
    private ManaCrucible(){}
    static boolean canConvert(LivingEntity e){
        var energy=TensuraStorages.getExistenceFrom(e);
        double max=EnergyHelper.getMaxMagicule(e),mp=energy.getMagicule(),aura=energy.getAura();
        return Double.isFinite(max) && Double.isFinite(mp) && Double.isFinite(aura)
            && max>0 && mp>=0 && mp<max && aura>0;
    }
    static void begin(ManasSkillInstance i,LivingEntity e){
        if(!(e.level() instanceof ServerLevel level) || !canConvert(e))return;
        var tag=i.getOrCreateTag();
        if(tag.getBoolean("crucible_active"))return;
        stop(i,e);
        tag.putBoolean("crucible_active",true);tag.putLong("crucible_heartbeat",level.getGameTime());
        tag.putLong("crucible_last_tick",level.getGameTime());tag.putInt("crucible_paid_ticks",0);
        tag.putString("dimension",level.dimension().location().toString());
        var visual=ArtVisualEntity.spawn(level,ArtVisualEntity.MANA_CRUCIBLE,e.position(),new Vec3(3,0,0),Integer.MAX_VALUE);
        visual.setCaster(e);tag.putUUID("crucible_visual",visual.getUUID());
        i.markDirty();HunterData.of(e).putBoolean("active_skills",true);
    }
    static boolean held(MythicSkill s,ManasSkillInstance i,LivingEntity e){
        if(!(e.level() instanceof ServerLevel level))return false;
        var tag=i.getOrCreateTag();long now=level.getGameTime();
        if(!tag.getBoolean("crucible_active") || !valid(s,i,e) || !canConvert(e)){
            stop(i,e);return false;
        }
        tag.putLong("crucible_heartbeat",now);
        // Duplicate native callbacks cannot spend or create resources twice in one game tick.
        if(now<=tag.getLong("crucible_last_tick"))return true;
        tag.putLong("crucible_last_tick",now);
        var energy=TensuraStorages.getExistenceFrom(e);double max=EnergyHelper.getMaxMagicule(e);
        double transfer=Math.min(max*MAGICULE_FRACTION_PER_SECOND/20,
            Math.min(energy.getAura()/AURA_PER_MAGICULE,max-energy.getMagicule()));
        if(!(transfer>0)){stop(i,e);return false;}
        energy.setAura(Math.max(0,energy.getAura()-transfer*AURA_PER_MAGICULE));
        energy.setMagicule(Math.min(max,energy.getMagicule()+transfer));energy.markDirty();
        int paid=tag.getInt("crucible_paid_ticks")+1;
        if(paid>=20){s.addMasteryPoint(i,e);paid=0;}
        tag.putInt("crucible_paid_ticks",paid);i.markDirty();
        if(!canConvert(e)){stop(i,e);return false;}
        return true;
    }
    private static boolean valid(MythicSkill s,ManasSkillInstance i,LivingEntity e){
        return e.isAlive() && s.canInteractSkill(i,e) && s.canActivateSkill(i,e,2)
            && i.getOrCreateTag().getString("dimension").equals(e.level().dimension().location().toString());
    }
    static boolean tick(MythicSkill s,ManasSkillInstance i,LivingEntity e){
        var tag=i.getOrCreateTag();
        if(!tag.getBoolean("crucible_active"))return false;
        // Conversion runs only from native held callbacks. A missed release cannot leave an aura drain.
        if(!valid(s,i,e) || !canConvert(e) || e.level().getGameTime()-tag.getLong("crucible_heartbeat")>2){
            stop(i,e);return false;
        }
        if(e.level() instanceof ServerLevel level && tag.hasUUID("crucible_visual")
            && level.getEntity(tag.getUUID("crucible_visual")) instanceof ArtVisualEntity visual)
            visual.updateVector(e.position(),new Vec3(3,0,0));
        return true;
    }
    static void stop(ManasSkillInstance i,LivingEntity e){
        var tag=i.getOrCreateTag();boolean changed=tag.contains("crucible_active") || tag.hasUUID("crucible_visual") || i.getCoolDown(2)!=0;
        if(e.level() instanceof ServerLevel level && tag.hasUUID("crucible_visual")){
            var visual=level.getEntity(tag.getUUID("crucible_visual"));if(visual!=null)visual.discard();
        }
        for(String key:java.util.List.of("crucible_active","crucible_visual","crucible_last_tick","crucible_heartbeat","crucible_paid_ticks"))tag.remove(key);
        if(changed){i.setCoolDown(0,2);i.markDirty();}
    }
}
