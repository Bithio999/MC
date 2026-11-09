package dev.heavenlyrestriction.forge;

import net.neoforged.neoforge.common.ModConfigSpec;

/** No EP gate: synthesis is mastery, awakening, craftsmanship and acquisition cost. */
public final class ForgeConfig {
  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
  public static final ModConfigSpec.DoubleValue ACQUISITION =
      B.comment(
              "Permanent magicule acquisition cost. Uses Tensura's mpSkillCost gamerule; charged"
                  + " after awakening has completed.")
          .defineInRange("acquisitionMagicules", 250000d, 0, 1e12);
  public static final ModConfigSpec SPEC = B.build();

  private ForgeConfig() {}
}
