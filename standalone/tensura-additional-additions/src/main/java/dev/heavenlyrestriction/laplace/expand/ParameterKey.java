package dev.heavenlyrestriction.laplace.expand;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Stable field identities; never use editable field names as reflection paths from a packet. */
public final class ParameterKey {
  public static String key(String field) {
    try {
      String name =
          field
              .substring(field.lastIndexOf('.') + 1)
              .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
              .toLowerCase(Locale.ROOT)
              .replaceAll("[^a-z0-9_]", "_");
      return "p_"
          + name.substring(0, Math.min(18, name.length()))
          + "_"
          + HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(field.getBytes(StandardCharsets.UTF_8)))
              .substring(0, 6);
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public static boolean allowed(String owner, String name, String desc) {
    if (!Set.of("I", "F", "D", "J").contains(desc)) return false;
    if (!owner.startsWith("io/github/manasmods/tensura/config/ability/")
        && !(owner.startsWith("dev/heavenlyrestriction/") && owner.endsWith("Config")))
      return false;
    if (owner.contains("AbilityConfig$Learning") || owner.contains("AbilityConfig$Mastery"))
      return false;
    String n = name.toLowerCase(Locale.ROOT);
    if (name.contains("EP") || n.startsWith("ep") || n.matches(".*ep(mastered)?$")) return false;
    // These are engine/progression/transaction controls, rather than numeric action effects.
    for (String term :
        List.of(
            "acquir",
            "acquire",
            "learn",
            "mastery",
            "masteryactivate",
            "masteryrequire",
            "masterylow",
            "masterymid",
            "masteryhigh",
            "evolution",
            "require",
            "craft",
            "recipe",
            "engraving",
            "inventory",
            "slot",
            "storage",
            "packet",
            "cooldown",
            "casttime",
            "chargetick",
            "chargetime",
            "interval",
            "frequency",
            "chance",
            "dodge",
            "inputmultiplier",
            "reduction",
            "cost",
            "consume",
            "soul",
            "levelrequire",
            "skillsteal",
            "maxep",
            "baseep",
            "magiculegain",
            "auragain",
            "epgain",
            "epbonus",
            "epmultiplier",
            "maxaura",
            "maxmagicule",
            "awakening",
            "existencepoint",
            "growth")) if (n.contains(term)) return false;
    return !n.equals("mode") && !n.endsWith("id");
  }

  public static String label(String name) {
    return name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ');
  }

  private ParameterKey() {}
}
