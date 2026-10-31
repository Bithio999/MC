package dev.heavenlyrestriction.fusion;
import io.github.manasmods.manascore.skill.api.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
/** Restore loaned banks and cooldowns even if the source participant loads later. */
public final class FusionReturns extends SavedData {
    private CompoundTag returns=new CompoundTag();
    public static FusionReturns get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(new Factory<>(FusionReturns::new,(t,r)->{var d=new FusionReturns();d.returns=t.copy();return d;}),"heavenlyrestriction_fusion_returns");}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){tag.merge(returns);return tag;}
    public void put(java.util.UUID owner,ManasSkillInstance skill){var key=owner.toString();var list=returns.getCompound(key);list.put(skill.getSkillId().toString(),skill.toNBT());returns.put(key,list);setDirty();}
    public void apply(LivingEntity e){String key=e.getUUID().toString();if(!returns.contains(key))return;var list=returns.getCompound(key);
        for(var id:list.getAllKeys()){var i=ManasSkillInstance.fromNBT(list.getCompound(id));if(i!=null){var existing=SkillAPI.getSkillsFrom(e).getSkill(i.getSkill());if(existing.isPresent()){if(i.getRemoveTime()>0 && existing.get().getRemoveTime()>=0)i.setRemoveTime(Math.min(i.getRemoveTime(),existing.get().getRemoveTime()));if(i.getRemoveTime()!=0)SkillAPI.getSkillsFrom(e).updateSkill(i,true);}}}
        returns.remove(key);setDirty();
    }
}
