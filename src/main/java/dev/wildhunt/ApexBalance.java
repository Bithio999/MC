package dev.wildhunt;

/** Fixed raw damage before native Tensura rules. Cooldowns are seconds. */
public record ApexBalance(float primary, float burst, float pulse, int primaryCooldown,
                          int burstCooldown, double primaryAura, double burstAura,
                          double passiveAuraPerSecond, double radius) {
    public static ApexBalance of(Apex apex) {
        return switch (apex) {
            case REY_DAU -> new ApexBalance(90, 80, 20, 4, 5, 200, 600, 10, 8);
            case UTH_DUNA -> new ApexBalance(65, 45, 18, 3, 7, 160, 500, 10, 9);
            case NU_UDRA -> new ApexBalance(70, 90, 24, 3, 8, 160, 800, 15, 8);
            case JIN_DAHAAD -> new ApexBalance(100, 55, 30, 4, 8, 200, 650, 20, 4);
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
