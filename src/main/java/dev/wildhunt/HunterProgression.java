package dev.wildhunt;

/** Pure progression rules; server storage and the Tensura UI use the same gates. */
public final class HunterProgression {
    private HunterProgression() {}
    public static final double[] EP = {0, 100_000, 400_000, 2_000_000};
    public static final double[] HP_BONUS = {0, 100, 500, 1000};
    public static final double[] SHP_BONUS = {0, 400, 3200, 6600};
    public static final int[] BOSSES = {0, 1, 3, 5};
    public static boolean canEvolve(int currentStage, double ep, int bosses, int mastered) {
        if (currentStage < 0 || currentStage >= 3) return false;
        int next = currentStage + 1;
        return Double.isFinite(ep) && ep >= EP[next] && bosses >= BOSSES[next] && (next < 3 || mastered >= 1);
    }
    public static boolean canAscend(int stage, boolean mastered, double ep) {
        return stage == 3 && mastered && Double.isFinite(ep) && ep >= EP[3];
    }
}
