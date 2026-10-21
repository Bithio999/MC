package dev.wildhunt;

/** Per-source opt-in: a Verdict collision is a separate native hit, even inside hurt cooldown. */
public interface IndependentHitSource {
    void myths$independentHit();
}
