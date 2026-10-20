package dev.wildhunt;

/** Fixed raw damage before native Tensura rules. Cooldowns are seconds. */
public record ApexBalance(float primary, float burst, float pulse, int primaryCooldown,
                          int burstCooldown, double primaryAura, double burstAura,
                          double passiveAuraPerSecond, double radius) {
    public static ApexBalance of(Apex apex) {
        return switch (apex) {
            case REY_DAU -> new ApexBalance(300, 200, 80, 3, 4, 600, 700, 25, 8);
            case UTH_DUNA -> new ApexBalance(250, 100, 60, 3, 6, 500, 1200, 30, 9);
            case NU_UDRA -> new ApexBalance(260, 350, 75, 3, 7, 550, 1600, 35, 8);
            case JIN_DAHAAD -> new ApexBalance(320, 180, 100, 3, 7, 700, 1100, 40, 4);
        };
    }
    public static float power(double masteryFraction, boolean ultimate) {
        return (float)((1 + 0.5 * Math.clamp(masteryFraction, 0, 1)) * (ultimate ? 3 : 1));
    }
    public int cooldown(int mode, boolean ultimate) {
        if (mode == 2) return 60;
        return Math.max(1, (mode == 0 ? primaryCooldown : burstCooldown) - (ultimate ? 1 : 0));
    }
    public double aura(int mode, boolean ultimate) {
        if (mode == 2) return 6_000;
        return (mode == 0 ? primaryAura : burstAura) * (ultimate ? 4 : 1);
    }
    public double upkeep(boolean ultimate) { return passiveAuraPerSecond * (ultimate ? 4 : 1); }
}
