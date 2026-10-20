package dev.wildhunt;

import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** One ordinary elemental source at a time; innate base values and Godly power remain. */
public final class ElementalBonuses {
    private ElementalBonuses() {}
    public static boolean elemental(ResourceLocation id) {
        return id.getNamespace().equals("tensura") && switch(id.getPath()) {
            case "earth_boost", "darkness_boost", "flame_boost", "light_boost", "space_boost",
                "water_boost", "wind_boost", "gravity_boost", "lightning_boost", "illusion_boost", "sound_boost" -> true;
            default -> false;
        };
    }
    private static boolean godly(AttributeModifier m) {
        return m.id().equals(WildHunt.id("thoth_passive"));
    }
    public static double value(double base,Collection<AttributeModifier> modifiers) {
        double best=calculate(base,modifiers,null);
        for(var candidate:modifiers)if(candidate.amount()>0 && !godly(candidate))
            best=Math.max(best,calculate(base,modifiers,candidate));
        return best;
    }
    private static double calculate(double base,Collection<AttributeModifier> modifiers,AttributeModifier selected) {
        double flat=base,baseMultiplier=0,total=1;
        for(var m:modifiers) {
            if(m.amount()>0 && !godly(m) && m!=selected)continue;
            switch(m.operation()) {
                case ADD_VALUE -> flat+=m.amount();
                case ADD_MULTIPLIED_BASE -> baseMultiplier+=m.amount();
                case ADD_MULTIPLIED_TOTAL -> total*=1+m.amount();
            }
        }
        return flat*(1+baseMultiplier)*total;
    }
}
