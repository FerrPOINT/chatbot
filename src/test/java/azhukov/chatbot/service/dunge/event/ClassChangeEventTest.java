package azhukov.chatbot.service.dunge.event;

import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.data.HeroClass;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassChangeEventTest {
    @Test
    void usesInjectedRandomSource() {
        DungeonRandom random = (origin, bound) -> 1;
        HeroInfo hero = new HeroInfo().setType(HeroClass.SAILOR);
        new ClassChangeEvent(random).handle(hero);
        assertEquals(HeroClass.DEFENDER, hero.getType());
    }
}
