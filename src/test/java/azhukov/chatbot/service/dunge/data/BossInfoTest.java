package azhukov.chatbot.service.dunge.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossInfoTest {
    @Test
    void hpOnlyDecreasesAndDamageIsClampedToRemainingHp() {
        BossInfo boss = new BossInfo();
        boss.setMaxHp(100);
        assertEquals(0, boss.dealDamage(-50));
        assertEquals(100, boss.getCurrentHp());
        assertEquals(40, boss.dealDamage(40));
        assertEquals(60, boss.getCurrentHp());
        assertEquals(60, boss.dealDamage(1_000));
        assertEquals(0, boss.getCurrentHp());
    }

    @Test
    void instanceIdentityIncludesCycle() {
        BossInfo boss = new BossInfo();
        boss.setCycle(3); boss.setStage(42);
        assertEquals("3:42", boss.getInstanceId());
    }
}
