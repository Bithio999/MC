package dev.heavenlyrestriction;

import com.mojang.datafixers.util.Pair;
import io.github.manasmods.manascore.race.api.*;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.race.TensuraRace;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.Alignment;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;

/** A starting human lineage followed by four fixed, native-menu evolutions. */
public final class RestrictedRace extends TensuraRace {
    public static final String[] IDS={"fettered_human","tempered_vessel","unshackled","heavenly_restricted","heavenbreaker"};
    public static final String[] NAMES={"Fettered Human","Tempered Vessel","Unshackled","Heavenly Restricted","Heavenbreaker"};
    public static final double[] EP={0,100_000,500_000,2_000_000,8_000_000};
    private static final double[] HP={0,100,300,600,1000},SHP={0,400,1600,3600,6600};
    public final int stage;
    public RestrictedRace(int stage){
        super(Difficulty.HARD);this.stage=stage;
        addAttributeModifier(Attributes.MAX_HEALTH,HeavenlyRestriction.id("restricted_vitality"),HP[stage],AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(TensuraAttributes.MAX_SPIRITUAL_HEALTH,HeavenlyRestriction.id("restricted_soul"),SHP[stage],AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,HeavenlyRestriction.id("restricted_strength"),stage*4,AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,HeavenlyRestriction.id("restricted_speed"),stage*.075,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE,HeavenlyRestriction.id("restricted_poise"),stage*.15,AttributeModifier.Operation.ADD_VALUE);
    }
    @Override public MutableComponent getName(){return Component.literal(NAMES[stage]);}
    @Override public MutableComponent getRaceDescription(){return Component.literal(stage==0?"A human bound by Heavenly Restriction. Chained trades magical growth and unrestricted skill learning for immense aura growth and physical power. Four evolutions await.":"A human body forged by a binding heavenly restriction. Evolution "+stage+"/4. Chained exchanges magical growth and unrestricted skill acquisition for overwhelming aura and martial potential.");}
    @Override public ResourceLocation getRaceIcon(){return HeavenlyRestriction.id("textures/race/"+IDS[stage]+".png");}
    @Override public Pair<Double,Double> getBaseAuraRange(){return Pair.of(1_500d,2_000d);}
    @Override public Pair<Double,Double> getBaseMagiculeRange(){return Pair.of(50d,100d);}
    @Override public void resetExistenceData(LivingEntity e){
        super.resetExistenceData(e);
        RestrictionData.of(e).remove("restricted_recovery_stage");
        // Capture only the native race roll, before separate reincarnation rewards.
        ChainedState.recordStartingAura(e,io.github.manasmods.tensura.util.EnergyHelper.getBaseMaxAura(e));
        ChainedState.refresh(e);
    }
    @Override public Alignment getAlignment(){return Alignment.DEFAULT;}
    @Override public List<ManasSkill> getIntrinsicSkills(ManasRaceInstance i,LivingEntity e){return stage==0?List.of(HeavenlyRestriction.CHAINED.get()):java.util.stream.IntStream.range(0,Math.max(0,stage-1)).mapToObj(n->(ManasSkill)HeavenlyRestriction.REGENERATION.get(n).get()).toList();}
    @Override public void learnIntrinsicSkills(ManasRaceInstance i,LivingEntity e){
        // Exempt only these named racial recovery grants, not arbitrary Extra skills.
        try(var scope=ChainedContext.racialRecovery(e,Math.max(0,stage-1))){super.learnIntrinsicSkills(i,e);}
    }
    public static void migrateRecovery(LivingEntity e){
        if(e.level().isClientSide())return;
        var best=RaceAPI.getRaceFrom(e).getRace().filter(i->i.getRace() instanceof RestrictedRace r && r.stage>=2);
        if(best.isEmpty())return;
        var instance=best.get();var race=(RestrictedRace)instance.getRace();var data=RestrictionData.of(e);
        if(data.getInt("restricted_recovery_stage")>=race.stage)return;
        race.learnIntrinsicSkills(instance,e);
        if(race.getIntrinsicSkills(instance,e).stream().allMatch(s->io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(e).getSkill(s).isPresent())){
            data.putInt("restricted_recovery_stage",race.stage);
        }
    }
    @Override public List<ManasRace> getNextEvolutions(ManasRaceInstance i,LivingEntity e){return stage==4?List.of():List.of(HeavenlyRestriction.RESTRICTED.get(stage+1).get());}
    @Override public List<ManasRace> getPreviousEvolutions(ManasRaceInstance i,LivingEntity e){return stage==0?List.of():List.of(HeavenlyRestriction.RESTRICTED.get(stage-1).get());}
    @Override public ManasRace getDefaultEvolution(ManasRaceInstance i,LivingEntity e){return stage==4?null:HeavenlyRestriction.RESTRICTED.get(stage+1).get();}
    @Override public Map<EvolutionRequirement,Float> getEvolutionRequirements(ManasRaceInstance i,LivingEntity e){
        if(stage==0)return Map.of();var gates=new LinkedHashMap<EvolutionRequirement,Float>();
        gates.put(new ProgressionEP.Requirement(EP[stage]),50f);
        gates.put(new Gate("Master "+stage+" Battlewill"+(stage==1?"":"s"),v->(float)ChainedState.masteredBattlewills(v)/stage),25f);
        gates.put(new Gate(stage==4?"Master Chained":"Raise Chained mastery to "+(stage*20)+"%",v->(float)(ChainedState.mastery(v)/(stage==4?1:stage*.2))),25f);
        return gates;
    }
    private static final class Gate extends EvolutionRequirement {
        final String text;final java.util.function.Function<LivingEntity,Float> progress;
        Gate(String text,java.util.function.Function<LivingEntity,Float> progress){this.text=text;this.progress=progress;}
        @Override public float getProgress(ManasRaceInstance i,LivingEntity e){return Math.clamp(progress.apply(e),0,1);}
        @Override public Component getRequirementComponent(ManasRaceInstance i,LivingEntity e){return Component.literal(text);}
    }
}
