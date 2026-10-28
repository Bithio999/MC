package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.effect.template.TensuraMobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

/** Keep native buff modifiers and their original amounts; never multiply them again on refresh. */
public final class ChainedBuffs {
    private static final ThreadLocal<Boolean> REPLAY=ThreadLocal.withInitial(()->false);
    private ChainedBuffs(){}
    public static ManasSkill source(MobEffectInstance effect){
        var origin=((TensuraMobEffectInstance)effect).tensura$getSourceAbility();
        return origin!=null && !origin.isEmpty() && origin.getSkill() instanceof Battlewill?origin.getSkill():null;
    }
    public static void mark(LivingEntity owner,MobEffectInstance effect){
        var skill=ChainedContext.battlewill(owner);if(skill==null || !effect.getEffect().value().isBeneficial())return;
        ((TensuraMobEffectInstance)effect).tensura$setSourceAbility(skill,0);
        ((TensuraMobEffectInstance)effect).tensura$setSource(owner.getUUID());
    }
    public static AttributeModifier modifier(LivingEntity owner,AttributeInstance attribute,AttributeModifier value,boolean permanent){
        if(owner==null || owner.level().isClientSide() || REPLAY.get() || ChainedContext.battlewill(owner)==null)return value;
        // A capacity change inside a Battlewill callback can refresh Chained.
        // Its own pact/combat modifiers are not buffs supplied by that Battlewill.
        if(value.id().equals(ChainedState.VOW) || value.id().equals(ChainedState.START) || value.id().equals(ChainedState.COMBAT))return value;
        var list=HunterData.of(owner).getList("chained_buffs",Tag.TAG_COMPOUND);String id=BuiltInRegistries.ATTRIBUTE.getKey(attribute.getAttribute().value()).toString(),key=value.id().toString();
        CompoundTag record=null;for(var v:list){var t=(CompoundTag)v;if(t.getString("attribute").equals(id) && t.getString("modifier").equals(key)){record=t;break;}}
        if(record==null){record=new CompoundTag();record.putString("attribute",id);record.putString("modifier",key);list.add(record);}
        record.putDouble("original",value.amount());record.putInt("operation",value.operation().ordinal());record.putBoolean("permanent",permanent);
        double amount=value.amount()*(ChainedState.active(owner)?3:1);record.putDouble("applied",amount);HunterData.of(owner).put("chained_buffs",list);
        return new AttributeModifier(value.id(),amount,value.operation());
    }
    public static void refresh(LivingEntity owner){refresh(owner,ChainedState.active(owner));}
    public static void refresh(LivingEntity owner,boolean active){
        if(owner.level().isClientSide() || REPLAY.get())return;
        var list=HunterData.of(owner).getList("chained_buffs",Tag.TAG_COMPOUND);if(list.isEmpty())return;
        REPLAY.set(true);try{
            for(int n=list.size()-1;n>=0;n--){var t=list.getCompound(n);var holder=BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(t.getString("attribute")));var a=holder.map(owner::getAttribute).orElse(null);var id=ResourceLocation.parse(t.getString("modifier"));var old=a==null?null:a.getModifier(id);
                if(old==null || Math.abs(old.amount()-t.getDouble("applied"))>1e-8){list.remove(n);continue;}
                double wanted=t.getDouble("original")*(active?3:1);if(Math.abs(old.amount()-wanted)<1e-8)continue;
                var value=new AttributeModifier(id,wanted,old.operation());if(t.getBoolean("permanent"))a.addOrReplacePermanentModifier(value);else a.addOrUpdateTransientModifier(value);t.putDouble("applied",wanted);
            }
        }finally{REPLAY.remove();}
    }
    public static boolean damageBuff(LivingEntity owner,Object action){
        // Aura Sword keeps its native added damage; only the outgoing damage bonuses apply to it.
        if(action==io.github.manasmods.tensura.registry.effect.TensuraMobEffects.AURA_SWORD.get())return false;
        return ChainedState.active(owner) && owner.getActiveEffects().stream().anyMatch(e->e.getEffect().value()==action && e.getEffect().value().isBeneficial() && source(e)!=null);
    }
}
