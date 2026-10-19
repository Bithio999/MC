package dev.wildhunt.myths;

/** A second lineage adds ten percent growth without changing energy proportions. */
public final class DuoRules {
    private DuoRules() {}
    public record Gains(double aura, double magicules) {
        public Gains {
            if (!Double.isFinite(aura) || !Double.isFinite(magicules) || aura <= 0 || magicules <= 0)
                throw new IllegalArgumentException("Energy gains must be positive and finite");
        }
        public Gains withDuoBonus() { return new Gains(aura * 1.1, magicules * 1.1); }
        public double auraFor(double nativeBase) { return aura * nativeBase; }
        public double magiculesFor(double nativeBase) { return magicules * nativeBase; }
    }
    public static boolean eligible(boolean finalPrimary, boolean awakened, double ep, double threshold, boolean hasSecondary) {
        return finalPrimary && awakened && Double.isFinite(ep) && ep >= threshold && !hasSecondary;
    }
    public static String pairKey(String first, String second) {
        if (first.equals(second)) throw new IllegalArgumentException("Choose two different race families");
        return first.compareTo(second) < 0 ? first + "_" + second : second + "_" + first;
    }
}
