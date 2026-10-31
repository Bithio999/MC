package dev.heavenlyrestriction;

/** Per-source opt-in: a Verdict collision is a separate native hit, even inside hurt cooldown. */
public interface IndependentHitSource {
    void restriction$independentHit();
}
