package dev.wildhunt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApexBalanceTest {
    @Test void everyLineHasCombatCooldownsInSeconds() {
        for (Apex apex : Apex.values()) {
            var balance = ApexBalance.of(apex);
            assertTrue(balance.cooldown(0, false) <= 4, apex + " primary cooldown");
            assertTrue(balance.cooldown(1, false) <= 8, apex + " area cooldown");
            assertEquals(60, balance.cooldown(2, true));
            assertTrue(balance.upkeep(false) > 0);
        }
    }
    @Test void damageDependsOnlyOnMasteryAndRank() {
        assertEquals(1, ApexBalance.power(0, false));
        assertEquals(1.25f, ApexBalance.power(0.5, false));
        assertEquals(1.5f, ApexBalance.power(1, false));
        assertEquals(1.5f, ApexBalance.power(100, false));
        assertEquals(4.5f, ApexBalance.power(1, true));
    }
    @Test void apexUniquesHaveAffordableCombatOpeners() {
        for (Apex apex : Apex.values()) {
            assertTrue(ApexBalance.of(apex).primary() >= 400);
            assertTrue(ApexBalance.of(apex).aura(0, false) <= 800);
        }
    }
}
