package dev.heavenlyrestriction.highrealm;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HighRealmConfig {
  private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
  public static final ModConfigSpec.DoubleValue ENTRY_EP =
      B.comment(
              "Required actual maximum EP (aura + magicules, including modifiers) to enter The High"
                  + " Realm. Leaving never requires EP.")
          .defineInRange("entryEP", 5000000d, 0, 1e12);
  public static final ModConfigSpec.IntValue CHARGE_TICKS =
      B.comment("Ticks spent inside an awakened gate before entry.")
          .defineInRange("gateChargeTicks", 40, 0, 200);
  public static final ModConfigSpec.DoubleValue ELITE_POWER =
      B.comment(
              "Multiplier for High Realm elite health, SHP, EP and attack floors. Applied only when"
                  + " a new elite spawns.")
          .defineInRange("elitePower", 1d, .25, 10);
  public static final ModConfigSpec.DoubleValue SPAWN_CHANCE =
      B.comment("Fraction of otherwise valid High Realm natural/chunk-generation spawns accepted.")
          .defineInRange("eliteSpawnChance", .5d, 0, 1);
  public static final ModConfigSpec.IntValue LOCAL_CAP =
      B.comment("Maximum living unowned High Realm elites within 64 blocks of a new natural spawn.")
          .defineInRange("eliteLocalCap", 16, 1, 64);
  public static final ModConfigSpec.BooleanValue ELITES =
      B.comment("Enable the High Realm's natural elite spawn pool.").define("eliteSpawns", true);
  public static final ModConfigSpec.BooleanValue WAYFARER_SPAWNS =
      B.comment("Enable Kael Veyr, the rare High Realm otherworlder boss.")
          .define("wayfarerSpawns", true);
  public static final ModConfigSpec.DoubleValue WAYFARER_CHANCE =
      B.comment(
              "Additional acceptance chance for the weight-1 boss spawn candidate, independent of"
                  + " elite density.")
          .defineInRange("wayfarerSpawnChance", .025d, 0, 1);
  public static final ModConfigSpec.IntValue WAYFARER_COOLDOWN =
      B.comment(
              "Minimum ticks between accepted natural Wayfarer spawns across this realm. 24000 = 20"
                  + " minutes.")
          .defineInRange("wayfarerSpawnCooldown", 24000, 200, 1728000);
  public static final ModConfigSpec SPEC = B.build();

  public static void upgradeLegacyDefault(RealmData data) {
    if (data.balanceVersion >= 2) return;
    if (ENTRY_EP.get() == 10000000d) {
      ENTRY_EP.set(5000000d);
      ENTRY_EP.save();
    }
    data.balanceVersion = 2;
    data.setDirty();
  }

  private HighRealmConfig() {}
}
