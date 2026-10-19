package dev.wildhunt.myths;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MythsConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue SECONDARY_EP = B.comment("Base EP required to choose a permanent secondary race.")
        .defineInRange("secondaryRaceEP", 5_000_000d, 0, 1e15);
    public static final ModConfigSpec.DoubleValue SECONDARY_EVOLUTION_STEP = B.comment("Fixed base EP increase per secondary stage above secondaryRaceEP. Native requirements also apply.")
        .defineInRange("secondaryEvolutionEPStep", 2_500_000d, 1, 1e15);
    public static final ModConfigSpec.DoubleValue CONVERGENCE_EP = B.comment("Minimum base EP for Convergent Awakening; lineage evolution gates can raise it.")
        .defineInRange("convergenceEP", 10_000_000d, 0, 1e15);
    public static final ModConfigSpec.DoubleValue CONVERGENCE_EP_FACTOR = B.comment("Convergent Awakening EP multiplier over the highest preceding lineage evolution EP gate.")
        .defineInRange("convergenceEvolutionEPFactor", 1.25d, 1, 10);
    public static final ModConfigSpec.DoubleValue GODLY_EP = B.comment("Base EP required to evolve a mastered addon Ultimate into a Godly skill.")
        .defineInRange("godlyEP", 12_000_000d, 0, 1e15);
    public static final ModConfigSpec.DoubleValue CONVERGENCE_BOOST = B.comment("One-time permanent energy multiplier on Convergent Awakening.")
        .defineInRange("convergenceEnergyMultiplier", 1.25d, 1, 10);
    public static final ModConfigSpec SPEC = B.build();
    private MythsConfig() {}
}
