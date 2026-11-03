package dev.heavenlyrestriction.highrealm;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HighRealmConfig {
  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
  public static final ModConfigSpec.DoubleValue ENTRY_EP =
      B.comment(
              "Required actual maximum EP (aura + magicules, including modifiers) to enter The High"
                  + " Realm. Leaving never requires EP.")
          .defineInRange("entryEP", 10000000d, 0, 1e12);
  public static final ModConfigSpec.IntValue CHARGE_TICKS =
      B.comment("Ticks spent inside an awakened gate before entry.")
          .defineInRange("gateChargeTicks", 40, 0, 200);
  public static final ModConfigSpec.DoubleValue ELITE_POWER =
      B.comment(
              "Multiplier for High Realm elite health, SHP, EP and attack floors. Applied only when"
                  + " a new elite spawns.")
          .defineInRange("elitePower", 1d, .25, 10);
  public static final ModConfigSpec.BooleanValue ELITES =
      B.comment("Enable the High Realm's natural elite spawn pool.").define("eliteSpawns", true);
  public static final ModConfigSpec SPEC = B.build();

  private HighRealmConfig() {}
}
