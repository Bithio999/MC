package dev.wildhunt;

import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import java.util.Locale;
import java.util.function.Supplier;

/** Progression reads the same effective capacity total as the native Status menu. */
public final class ProgressionEP {
    private static final ThreadLocal<LivingEntity> SECONDARY_QUERY=new ThreadLocal<>();
    private ProgressionEP(){}
    public static boolean usesTotalRequirements(LivingEntity owner){return SECONDARY_QUERY.get()==owner;}
    public static <T> T withTotalRequirements(LivingEntity owner,Supplier<T> query){
        var previous=SECONDARY_QUERY.get();SECONDARY_QUERY.set(owner);
        try{return query.get();}finally{if(previous==null)SECONDARY_QUERY.remove();else SECONDARY_QUERY.set(previous);}
    }
    public static Component label(double requirement){return Component.literal(String.format(Locale.ROOT,"Total EP: %,.0f",requirement));}
    /** Preserve the native EP requirement type so lineage threshold discovery still works. */
    public static final class Requirement extends EvolutionRequirement.EPRequirement {
        public Requirement(double requirement){super(requirement);}
        @Override public float getProgress(ManasRaceInstance instance,LivingEntity entity){
            double ep=EnergyHelper.getMaxEP(entity);
            if(!Double.isFinite(ep) || ep<0)return 0;
            return getRequirement()<=0?1:(float)Math.clamp(ep/getRequirement(),0,1);
        }
        @Override public Component getRequirementComponent(ManasRaceInstance instance,LivingEntity entity){return label(getRequirement());}
    }
}
