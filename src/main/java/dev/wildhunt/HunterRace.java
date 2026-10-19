package dev.wildhunt;

import com.mojang.datafixers.util.Pair;
import io.github.manasmods.manascore.race.api.*;
import io.github.manasmods.tensura.race.TensuraRace;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HunterRace extends TensuraRace {
    public final int stage;
    public HunterRace(int stage) {
        super(Difficulty.HARD);
        this.stage = stage;
        addAttributeModifier(Attributes.MAX_HEALTH, WildHunt.id("hunter_health"), HunterProgression.HP_BONUS[stage], Operation.ADD_VALUE);
        addAttributeModifier(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH,
            WildHunt.id("hunter_spiritual_health"), HunterProgression.SHP_BONUS[stage], Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, WildHunt.id("hunter_attack"), 2 * stage, Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, WildHunt.id("hunter_speed"), 0.05 * stage, Operation.ADD_MULTIPLIED_BASE);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, WildHunt.id("hunter_resolve"), 0.1 * stage, Operation.ADD_VALUE);
    }
    @Override public Pair<Double, Double> getBaseAuraRange() { return Pair.of(baseEP() * 2 / 3, baseEP() * 2 / 3); }
    @Override public Pair<Double, Double> getBaseMagiculeRange() { return Pair.of(baseEP() / 3, baseEP() / 3); }
    private double baseEP() { return switch (stage) { case 1 -> 200_000; case 2 -> 800_000; default -> 2_000_000; }; }
    @Override public List<io.github.manasmods.manascore.skill.api.ManasSkill> getIntrinsicSkills(ManasRaceInstance instance, LivingEntity entity) {
        return java.util.stream.IntStream.range(0, stage).mapToObj(i -> (io.github.manasmods.manascore.skill.api.ManasSkill)WildHunt.REGENERATION.get(i).get()).toList();
    }
    @Override public MutableComponent getName() {
        return Component.literal(switch(stage) { case 1 -> "Awakened Hunter"; case 2 -> "Apex Hunter"; default -> "Sovereign Hunter"; });
    }
    @Override public net.minecraft.resources.ResourceLocation getRaceIcon() {
        return WildHunt.id("textures/race/" + switch(stage) {
            case 1 -> "awakened_hunter"; case 2 -> "apex_hunter"; default -> "sovereign_hunter";
        } + ".png");
    }
    @Override public MutableComponent getRaceDescription() {
        return Component.literal("A human who has honed their soul through the hunt. Stage " + stage + "/3. Your appearance remains human.");
    }
    @Override public List<ManasRace> getNextEvolutions(ManasRaceInstance instance, LivingEntity entity) {
        return stage == 3 ? List.of() : List.of(WildHunt.HUNTERS.get(stage).get());
    }
    @Override public List<ManasRace> getPreviousEvolutions(ManasRaceInstance instance, LivingEntity entity) {
        return List.of(stage == 1 ? TensuraRaces.HUMAN.get() : WildHunt.HUNTERS.get(stage - 2).get());
    }
    @Override public ManasRace getDefaultEvolution(ManasRaceInstance instance, LivingEntity entity) {
        return stage == 3 ? null : WildHunt.HUNTERS.get(stage).get();
    }
    @Override public Map<EvolutionRequirement, Float> getEvolutionRequirements(ManasRaceInstance instance, LivingEntity entity) {
        Map<EvolutionRequirement, Float> gates = new LinkedHashMap<>();
        gates.put(new EvolutionRequirement.EPRequirement(HunterProgression.EP[stage]), 50f);
        gates.put(new Gate("Defeat " + HunterProgression.BOSSES[stage] + " distinct boss types", e -> (float) HunterData.bossKills(e).size() / HunterProgression.BOSSES[stage]), stage < 3 ? 50f : 25f);
        if (stage >= 3) gates.put(new Gate("Master a Myths Unique skill", e -> HunterData.mastered(e) > 0 ? 1f : 0f), 25f);
        return gates;
    }
    private static final class Gate extends EvolutionRequirement {
        private final String description;
        private final java.util.function.Function<LivingEntity, Float> progress;
        Gate(String description, java.util.function.Function<LivingEntity, Float> progress) { this.description = description; this.progress = progress; }
        @Override public float getProgress(ManasRaceInstance instance, LivingEntity entity) { return Math.min(1f, progress.apply(entity)); }
        @Override public Component getRequirementComponent(ManasRaceInstance instance, LivingEntity entity) { return Component.literal(description); }
    }
}
