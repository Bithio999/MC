package dev.wildhunt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HunterProgressionTest {
    @Test void humanNeedsBothEpAndHunts() {
        assertFalse(HunterProgression.canEvolve(0, 99_999, 20, 4, 4));
        assertFalse(HunterProgression.canEvolve(0, 100_000, 19, 4, 4));
        assertTrue(HunterProgression.canEvolve(0, 100_000, 20, 0, 0));
    }
    @Test void apexRequiresEveryTrial() {
        assertFalse(HunterProgression.canEvolve(1, 400_000, 100, 3, 0));
        assertTrue(HunterProgression.canEvolve(1, 400_000, 100, 4, 0));
    }
    @Test void sovereignRequiresMasteryAndCannotEvolveFurther() {
        assertFalse(HunterProgression.canEvolve(2, 2_000_000, 250, 4, 0));
        assertTrue(HunterProgression.canEvolve(2, 2_000_000, 250, 4, 1));
        assertFalse(HunterProgression.canEvolve(3, 9_000_000, 1000, 4, 4));
        assertFalse(HunterProgression.canEvolve(-1, 9_000_000, 1000, 4, 4));
    }
    @Test void ultimateCannotSkipRaceMasteryOrTrial() {
        assertTrue(HunterProgression.canAscend(3, true, true, 2_000_000));
        assertFalse(HunterProgression.canAscend(2, true, true, 2_000_000));
        assertFalse(HunterProgression.canAscend(3, false, true, 2_000_000));
        assertFalse(HunterProgression.canAscend(3, true, false, 2_000_000));
        assertFalse(HunterProgression.canAscend(3, true, true, 1_999_999));
        assertFalse(HunterProgression.canAscend(3, true, true, Double.NaN));
    }
    @Test void nonfiniteEpCannotUnlockProgression() {
        assertFalse(HunterProgression.canEvolve(0, Double.NaN, 1000, 4, 4));
        assertFalse(HunterProgression.canEvolve(0, Double.POSITIVE_INFINITY, 1000, 4, 4));
    }
}
