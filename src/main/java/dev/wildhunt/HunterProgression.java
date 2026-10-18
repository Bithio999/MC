package dev.wildhunt;

/** Pure progression rules; server storage and the Tensura UI use the same gates. */
public final class HunterProgression {
    private HunterProgression() {}
    public static final double[] EP = {0, 100_000, 400_000, 2_000_000};
    public static final int[] HUNTS = {0, 20, 100, 250};
    public static boolean canEvolve(int currentStage, double ep, int hunts, int trials, int mastered) {
        if (currentStage < 0 || currentStage >= 3) return false;
        int next = currentStage + 1;
        return Double.isFinite(ep) && ep >= EP[next] && hunts >= HUNTS[next]
                && (next < 2 || trials >= 4) && (next < 3 || mastered >= 1);
    }
    public static boolean canAscend(int stage, boolean mastered, boolean trialCompleted, double ep) {
        return stage == 3 && mastered && trialCompleted && Double.isFinite(ep) && ep >= EP[3];
    }
}
