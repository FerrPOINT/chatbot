package azhukov.chatbot.service.weight;

import azhukov.chatbot.util.RangesContainer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WeightUtilsTest {

    @Test
    void test() {
        List<WeightItem> of = List.of(new Item(Weight.LOW), new Item(Weight.HIGH));
        RangesContainer<WeightItem> container = WeightUtils.createContainer(of);
        WeightItem item = container.getItem(1);
        assertEquals(Weight.LOW, item.getWeight());
        item = container.getItem(2);
        assertEquals(Weight.LOW, item.getWeight());
        item = container.getItem(3);
        assertEquals(Weight.HIGH, item.getWeight());
        item = container.getItem(4);
        assertEquals(Weight.HIGH, item.getWeight());
        item = container.getItem(5);
        assertEquals(Weight.HIGH, item.getWeight());
        item = container.getItem(6);
        assertEquals(Weight.HIGH, item.getWeight());
        item = container.getItem(7);
        assertNull(item);

        for (int i = 0; i < 100; i++) {
            System.out.println("Random weight: " + container.getRandomItem().getWeight());
        }
    }

    @Getter
    @RequiredArgsConstructor
    private class Item implements WeightItem {

        private final Weight weight;

    }
}