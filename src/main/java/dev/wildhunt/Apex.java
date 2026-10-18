package dev.wildhunt;

/** Immutable balance and identity data shared by skills, trials and commands. */
public enum Apex {
    REY_DAU("storm_crown", "raijin", "Storm Crown", "Raijin, Lord of Thunder", 0xE9C64A),
    UTH_DUNA("scarlet_deluge", "varuna", "Scarlet Deluge", "Varuna, Lord of Tides", 0xDE627A),
    NU_UDRA("black_flame", "agni", "Black Flame", "Agni, Lord of the Black Flame", 0xE76B2E),
    JIN_DAHAAD("frostbound_colossus", "ymir", "Frostbound Colossus", "Ymir, Lord of Winter", 0x72D2DE);

    public final String uniqueId, ultimateId, uniqueName, ultimateName;
    public final int color;
    Apex(String uniqueId, String ultimateId, String uniqueName, String ultimateName, int color) {
        this.uniqueId = uniqueId; this.ultimateId = ultimateId;
        this.uniqueName = uniqueName; this.ultimateName = ultimateName; this.color = color;
    }
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
}
