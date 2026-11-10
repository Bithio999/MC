package dev.heavenlyrestriction.metis;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MetisConfig {
  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
  public static final ModConfigSpec.DoubleValue ACQUISITION =
      B.comment(
              "Permanent magicules charged after completed awakening; respects mpSkillCost. No EP"
                  + " requirement.")
          .defineInRange("acquisitionMagicules", 250000d, 0, 1e12);
  public static final ModConfigSpec.IntValue MAGIC_ACHIEVEMENT =
      B.comment(
              "Learn this many distinct aspectual/spiritual magics to satisfy synthesis's magic"
                  + " achievement.")
          .defineInRange("learnedMagicAchievement", 3, 0, 128);
  public static final ModConfigSpec SPEC = B.build();

  private MetisConfig() {}
}
