package dev.wildhunt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HunterProgressionTest {
    @Test void firstEvolutionRequiresOneBossAndEp() {
        assertFalse(HunterProgression.canEvolve(0, 99_999, 1, 0));
        assertFalse(HunterProgression.canEvolve(0, 100_000, 0, 0));
        assertTrue(HunterProgression.canEvolve(0, 100_000, 1, 0));
    }
    @Test void apexRequiresThreeBossTypes() {
        assertFalse(HunterProgression.canEvolve(1, 400_000, 2, 0));
        assertTrue(HunterProgression.canEvolve(1, 400_000, 3, 0));
    }
    @Test void sovereignRequiresFiveBossTypesAndMastery() {
        assertFalse(HunterProgression.canEvolve(2, 2_000_000, 4, 1));
        assertFalse(HunterProgression.canEvolve(2, 2_000_000, 5, 0));
        assertTrue(HunterProgression.canEvolve(2, 2_000_000, 5, 1));
        assertFalse(HunterProgression.canEvolve(3, 9_000_000, 10, 4));
        assertFalse(HunterProgression.canEvolve(-1, 9_000_000, 10, 4));
    }
    @Test void ultimateNeedsOnlyRaceEpAndMatchingMastery() {
        assertTrue(HunterProgression.canAscend(3, true, 2_000_000));
        assertFalse(HunterProgression.canAscend(2, true, 2_000_000));
        assertFalse(HunterProgression.canAscend(3, false, 2_000_000));
        assertFalse(HunterProgression.canAscend(3, true, 1_999_999));
        assertFalse(HunterProgression.canAscend(3, true, Double.NaN));
    }
    @Test void nonfiniteEpCannotUnlockProgression() {
        assertFalse(HunterProgression.canEvolve(0, Double.NaN, 10, 4));
        assertFalse(HunterProgression.canEvolve(0, Double.POSITIVE_INFINITY, 10, 4));
    }
}
