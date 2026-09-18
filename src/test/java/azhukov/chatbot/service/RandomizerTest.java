package azhukov.chatbot.service;

import azhukov.chatbot.service.util.Randomizer;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RandomizerTest {

    @Test
    void tossCoinCanReturnBothOutcomes() {
        Set<Boolean> outcomes = new HashSet<>();
        for (int i = 0; i < 1000; i++) outcomes.add(Randomizer.tossCoin());
        assertEquals(Set.of(false, true), outcomes);
    }
}
