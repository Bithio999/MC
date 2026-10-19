package dev.wildhunt;

import dev.wildhunt.myths.DuoRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DuoRulesTest {
    @Test void humanSlimeUsesTheRequestedAdditiveExampleWithoutRenormalizing() {
        var result=new DuoRules.Gains(2,1).plusHalf(new DuoRules.Gains(1,2));
        assertEquals(7.5,result.auraFor(3),1e-9);
        assertEquals(6,result.magiculesFor(3),1e-9);
    }
    @Test void changingOrderChangesDualGainsButNotTheFusionIdentity() {
        var human=new DuoRules.Gains(2,1);var slime=new DuoRules.Gains(1,2);
        assertNotEquals(human.plusHalf(slime),slime.plusHalf(human));
        assertEquals(DuoRules.pairKey("human","slime"),DuoRules.pairKey("slime","human"));
        assertThrows(IllegalArgumentException.class,()->DuoRules.pairKey("human","human"));
    }
    @Test void everyGateIsRequiredAndNonfiniteEnergyCannotUnlock() {
        assertTrue(DuoRules.eligible(true,true,5_000_000,5_000_000,false));
        assertFalse(DuoRules.eligible(false,true,10_000_000,5_000_000,false));
        assertFalse(DuoRules.eligible(true,false,10_000_000,5_000_000,false));
        assertFalse(DuoRules.eligible(true,true,4_999_999,5_000_000,false));
        assertFalse(DuoRules.eligible(true,true,10_000_000,5_000_000,true));
        assertFalse(DuoRules.eligible(true,true,Double.POSITIVE_INFINITY,5_000_000,false));
        assertFalse(DuoRules.eligible(true,true,Double.NaN,5_000_000,false));
    }
}
