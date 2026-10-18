package dev.wildhunt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApexBalanceTest {
    @Test void everyLineHasCombatCooldownsInSeconds() {
        for (Apex apex : Apex.values()) {
            var balance = ApexBalance.of(apex);
            assertTrue(balance.cooldown(0, false) <= 4, apex + " primary cooldown");
            assertTrue(balance.cooldown(1, false) <= 8, apex + " area cooldown");
            assertTrue(balance.cooldown(2, false) <= 20, apex + " mantle cooldown");
            assertEquals(60, balance.cooldown(3, true));
        }
    }
    @Test void progressionIncreasesDamageButHasAFiniteCeiling() {
        float starting = ApexBalance.power(0, 0, false);
        float mid = ApexBalance.power(400_000, 0.5, false);
        float mastered = ApexBalance.power(2_000_000, 1, false);
        assertTrue(starting < mid && mid < mastered);
        assertEquals(mastered, ApexBalance.power(100_000_000, 100, false));
        assertEquals(mastered * 3, ApexBalance.power(2_000_000, 1, true));
    }
    @Test void eachUniqueStartsAboveTheOldEightDamageBeam() {
        for (Apex apex : Apex.values()) {
            assertTrue(ApexBalance.of(apex).primary() >= 65);
            assertTrue(ApexBalance.of(apex).aura(0, false) <= 200);
        }
    }
}
