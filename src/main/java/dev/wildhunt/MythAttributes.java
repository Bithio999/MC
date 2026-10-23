package dev.wildhunt;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

/** Entity-local evaluation for authored boss HP; does not raise vanilla/global bounds. */
final class MythAttributes {
    private MythAttributes(){}
    static double maximumHealth(LivingEntity e){return value(e,Attributes.MAX_HEALTH);}
    static double value(LivingEntity e,net.minecraft.core.Holder<Attribute> attribute){
        var a=e.getAttribute(attribute);if(a==null)return 1;double base=a.getBaseValue();
        for(var m:a.getModifiers())if(m.operation()==AttributeModifier.Operation.ADD_VALUE)base+=m.amount();
        double value=base;for(var m:a.getModifiers())if(m.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)value+=base*m.amount();
        for(var m:a.getModifiers())if(m.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)value*=1+m.amount();
        return Double.isFinite(value)?Math.clamp(value,1,10_000_000):1;
    }
}
