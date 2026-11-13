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
  public static final ModConfigSpec.DoubleValue PER_AURA =
      B.comment("Magicule-equivalent budget paid by one aura point.")
          .defineInRange("magiculesPerAura", 1d, .000001, 1e9);
  public static final ModConfigSpec.DoubleValue PER_HP =
      B.comment(
              "Magicule-equivalent budget paid by one HP. Healing/regeneration cannot use HP or SHP"
                  + " payments.")
          .defineInRange("magiculesPerHP", 10d, .000001, 1e9);
  public static final ModConfigSpec.DoubleValue PER_SHP =
      B.comment("Magicule-equivalent budget paid by one SHP.")
          .defineInRange("magiculesPerSHP", 10d, .000001, 1e9);

  public static PaymentPlan.Rates rates() {
    return new PaymentPlan.Rates(PER_AURA.get(), PER_HP.get(), PER_SHP.get());
  }

  public static final ModConfigSpec SPEC = B.build();

  private LaplaceConfig() {}
}
