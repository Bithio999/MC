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
  public static final ModConfigSpec.IntValue APPRAISAL =
      B.comment("Metis Analytical Appraisal level before mastery.")
          .defineInRange("appraisalLevel", 20, 1, 1024);
  public static final ModConfigSpec.IntValue APPRAISAL_MASTERED =
      B.comment("Metis Analytical Appraisal level when mastered.")
          .defineInRange("appraisalLevelMastered", 40, 1, 1024);
  public static final ModConfigSpec.IntValue ANALYSIS_COOLDOWN =
      B.comment("Successful manual Analysis cooldown in seconds.")
          .defineInRange("analysisCooldown", 5, 0, 3600);
  public static final ModConfigSpec.IntValue ANALYSIS_COOLDOWN_MASTERED =
      B.comment("Successful manual Analysis cooldown in seconds when mastered.")
          .defineInRange("analysisCooldownMastered", 2, 0, 3600);
  public static final ModConfigSpec SPEC = B.build();

  private MetisConfig() {}
}
