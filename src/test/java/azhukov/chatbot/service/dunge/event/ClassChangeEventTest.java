package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.data.HeroClass;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ClassChangeEventTest {

    @Test
    void test() {
        ClassChangeEvent classChangeEvent = new ClassChangeEvent();
        HeroInfo hero = new HeroInfo();
        hero.setType(HeroClass.LALKA);
        String handle = classChangeEvent.handle(hero);
        assertNotEquals(HeroClass.LALKA, hero.getType());
    }

}