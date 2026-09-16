package azhukov.chatbot.service.dunge.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DungeonCombatMathTest {
    @Test
    void appliesDamageAndRiskOnceThenPositiveBuffsOnce() {
        // damageXp=200, risk=2*(20+57)=154, base=354, buffs=30%
        assertEquals(460, DungeonCombatMath.normalizedXp(2000, 10, 2, 2, 33, 20, 10));
    }

    @Test
    void neverReturnsNegativeAndSaturatesOverflow() {
        assertEquals(1, DungeonCombatMath.normalizedXp(-10, 1, 0, -5, 200, 0, 0));
        assertEquals(Long.MAX_VALUE, DungeonCombatMath.normalizedXp(Long.MAX_VALUE, 1, Integer.MAX_VALUE,
                Integer.MAX_VALUE, 1, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
