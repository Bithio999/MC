package dev.wildhunt;

import dev.wildhunt.myths.DuoRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DuoRulesTest {
    @Test void secondaryAddsTenPercentWithoutChangingThePrimarySplit() {
        var result=new DuoRules.Gains(2,1).withDuoBonus();
        assertEquals(6.6,result.auraFor(3),1e-9);
        assertEquals(3.3,result.magiculesFor(3),1e-9);
        assertEquals(2,result.aura()/result.magicules(),1e-9);
    }
    @Test void bonusPreservesEachFusionProfileAndPairIdentity() {
        var fusion=new DuoRules.Gains(3.195,2.805);var boosted=fusion.withDuoBonus();
        assertEquals(1.1,boosted.aura()/fusion.aura(),1e-9);
        assertEquals(fusion.aura()/fusion.magicules(),boosted.aura()/boosted.magicules(),1e-9);
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
