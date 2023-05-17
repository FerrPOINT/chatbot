package azhukov.chatbot.service.dunge.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeroDamageTest {

    @Test
    void testJoin() {
        assertEquals(HeroDamage.ALMOUST_DEAD, HeroDamage.HUGE.join(HeroDamage.SLIGHT));
        assertEquals(HeroDamage.SLIGHT, HeroDamage.NONE.join(HeroDamage.SLIGHT));
        assertEquals(HeroDamage.MEDIUM, HeroDamage.SLIGHT.join(HeroDamage.SLIGHT));
        assertEquals(HeroDamage.BIG, HeroDamage.MEDIUM.join(HeroDamage.SLIGHT));
        assertEquals(HeroDamage.ALMOUST_DEAD, HeroDamage.BIG.join(HeroDamage.MEDIUM));
        assertEquals(HeroDamage.DEAD, HeroDamage.ALMOUST_DEAD.join(HeroDamage.MEDIUM));
    }

    @Test
    void testHeal() {
        assertEquals(HeroDamage.BIG, HeroDamage.HUGE.heal(HeroDamage.SLIGHT));
        assertEquals(HeroDamage.NONE, HeroDamage.SLIGHT.heal(HeroDamage.SLIGHT));
        assertEquals(HeroDamage.NONE, HeroDamage.SLIGHT.heal(HeroDamage.HUGE));
        assertEquals(HeroDamage.BIG, HeroDamage.ALMOUST_DEAD.heal(HeroDamage.MEDIUM));
    }

}