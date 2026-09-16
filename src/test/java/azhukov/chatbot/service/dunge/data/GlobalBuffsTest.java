package azhukov.chatbot.service.dunge.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalBuffsTest {
    @Test
    void eachGlobalBuffCapsAtThreeStacks() {
        GlobalBuffs buffs = new GlobalBuffs();
        for (int i = 0; i < 10; i++) { buffs.addAttack(); buffs.addShield(); buffs.addExp(); }
        assertEquals(60, buffs.attackPercent());
        assertEquals(6, buffs.shield());
        assertEquals(60, buffs.expPercent());
    }
}
