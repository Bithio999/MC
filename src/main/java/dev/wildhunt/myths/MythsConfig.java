package dev.wildhunt.myths;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MythsConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue SECONDARY_EP = B.comment("Base EP required to choose a permanent secondary race.")
        .defineInRange("secondaryRaceEP", 5_000_000d, 0, 1e15);
    public static final ModConfigSpec.DoubleValue CONVERGENCE_EP = B.comment("Base EP required when both lineages are terminal and the player is awakened.")
        .defineInRange("convergenceEP", 10_000_000d, 0, 1e15);
    public static final ModConfigSpec.DoubleValue GODLY_EP = B.comment("Base EP required to evolve a mastered addon Ultimate into Aion.")
        .defineInRange("godlyEP", 12_000_000d, 0, 1e15);
    public static final ModConfigSpec.DoubleValue CONVERGENCE_BOOST = B.comment("One-time permanent energy multiplier on Convergent Awakening.")
        .defineInRange("convergenceEnergyMultiplier", 1.25d, 1, 10);
    public static final ModConfigSpec SPEC = B.build();
    private MythsConfig() {}
}
