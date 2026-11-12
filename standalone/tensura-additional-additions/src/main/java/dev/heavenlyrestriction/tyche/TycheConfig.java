package dev.heavenlyrestriction.tyche;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TycheConfig {
  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
  public static final ModConfigSpec.DoubleValue ACQUISITION =
      B.defineInRange("acquisitionMagicules", 250000d, 0, 1e12);
  public static final ModConfigSpec.IntValue CRITICALS =
      B.defineInRange("criticalAchievement", 50, 0, 1000000);
  public static final ModConfigSpec.IntValue SURVIVALS =
      B.defineInRange("distinctTunerSurvivals", 3, 0, 128);
  public static final ModConfigSpec.DoubleValue DIVIDEND_COST =
      B.defineInRange("dividendMagicules", 5000d, 0, 1e12);
  public static final ModConfigSpec.IntValue ODDS_DURATION =
      B.defineInRange("againstAllOddsDurationSeconds", 20, 1, 3600);
  public static final ModConfigSpec.IntValue ODDS_COOLDOWN =
      B.defineInRange("againstAllOddsCooldownSeconds", 60, 1, 86400);
  public static final ModConfigSpec SPEC = B.build();

  private TycheConfig() {}
}
