package dev.heavenlyrestriction.laplace;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LaplaceConfig {
  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
  public static final ModConfigSpec.DoubleValue ACQUISITION =
      B.comment("Permanent magicules after completed awakening. No EP gate; respects mpSkillCost.")
          .defineInRange("acquisitionMagicules", 250000d, 0, 1e12);
  public static final ModConfigSpec.IntValue ACHIEVEMENT =
      B.comment("Master this many distinct supported native skills.")
          .defineInRange("masteredActions", 3, 0, 8);
  public static final ModConfigSpec SPEC = B.build();

  private LaplaceConfig() {}
}
