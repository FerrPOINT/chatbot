package azhukov.chatbot.service.dunge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DungeonRandomTest {
    @Test
    void percentageBoundariesAreExact() {
        assertTrue(((DungeonRandom) (o, b) -> 14).chance(15));
        assertFalse(((DungeonRandom) (o, b) -> 15).chance(15));
        assertTrue(((DungeonRandom) (o, b) -> 39).chance(40));
        assertFalse(((DungeonRandom) (o, b) -> 40).chance(40));
        assertTrue(((DungeonRandom) (o, b) -> 49).chance(50));
        assertFalse(((DungeonRandom) (o, b) -> 50).chance(50));
    }
}
