package dev.wildhunt;

/** Raw damage before Tensura elemental boosts/resistances. Cooldowns are SECONDS. */
public record ApexBalance(float primary, float burst, float pulse, int primaryCooldown,
                          int burstCooldown, int mantleCooldown, double primaryAura,
                          double burstAura, double mantleAura, double radius) {
    public static ApexBalance of(Apex apex) {
        return switch (apex) {
            case REY_DAU -> new ApexBalance(90, 60, 20, 4, 8, 20, 200, 600, 300, 8);
            case UTH_DUNA -> new ApexBalance(65, 45, 18, 3, 7, 18, 160, 500, 250, 9);
            case NU_UDRA -> new ApexBalance(70, 90, 24, 3, 8, 20, 160, 800, 350, 8);
            case JIN_DAHAAD -> new ApexBalance(75, 45, 20, 4, 8, 20, 200, 650, 350, 9);
        };
    }
    public static float power(double ep, double masteryFraction, boolean ultimate) {
        double epBonus = 1 + Math.sqrt(Math.clamp(ep / 2_000_000.0, 0, 1));
        return (float) (epBonus * (1 + 0.5 * Math.clamp(masteryFraction, 0, 1)) * (ultimate ? 3 : 1));
    }
    public int cooldown(int mode, boolean ultimate) {
        int seconds = switch (mode) { case 0 -> primaryCooldown; case 1 -> burstCooldown; case 2 -> mantleCooldown; default -> 60; };
        return ultimate && mode < 3 ? Math.max(1, seconds - 1) : seconds;
    }
    public double aura(int mode, boolean ultimate) {
        if (mode == 3) return 6_000;
        return (switch (mode) { case 0 -> primaryAura; case 1 -> burstAura; default -> mantleAura; }) * (ultimate ? 4 : 1);
    }
}
