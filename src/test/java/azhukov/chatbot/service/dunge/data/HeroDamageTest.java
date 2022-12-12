package azhukov.chatbot.service.dunge.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeroDamageTest {

    @Test
    void test() {
        assertEquals(HeroDamage.DEAD, HeroDamage.HUGE.join(HeroDamage.SLIGHT));
    }

}