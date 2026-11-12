package dev.heavenlyrestriction.mentor;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MentorConfig {
  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
  public static final ModConfigSpec.DoubleValue COST =
      B.comment(
              "Permanent EP reserve spent for the one-time lesson, plus one Chromatic Orb. Does not"
                  + " charge the native magicule acquisition cost again.")
          .defineInRange("permanentEpCost", 1_000_000d, 1, 1e12);
  public static final ModConfigSpec SPEC = B.build();

  private MentorConfig() {}
}
