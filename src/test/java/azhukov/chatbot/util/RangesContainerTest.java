package azhukov.chatbot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RangesContainerTest {

    @Test
    void test() {

        List<Range<String>> ranges = List.of(
                new Range<>(0, 10, "a"),
                new Range<>(11, 20, "b"),
                new Range<>(21, 30, "c"),
                new Range<>(31, 40, "d"),
                new Range<>(41, 50, "e"),
                new Range<>(51, 60, "f"),
                new Range<>(61, 70, "g"),
                new Range<>(71, 80, "h"),
                new Range<>(81, 90, "i"),
                new Range<>(91, 100, "j")
        );
        RangesContainer<String> container = new RangesContainer<>(ranges);

        assertEquals("d", container.getItem(33));
        assertEquals("a", container.getItem(0));
        assertEquals("j", container.getItem(100));
        assertEquals("f", container.getItem(55));
        assertEquals("h", container.getItem(71));
        assertEquals("h", container.getItem(80));

    }

}