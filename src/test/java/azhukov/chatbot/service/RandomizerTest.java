package azhukov.chatbot.service;

import azhukov.chatbot.service.util.Randomizer;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class RandomizerTest {

    @Test
    void test() {
        Set<Integer> nums = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            final int percent = Randomizer.nextInt(1000);
//            System.out.println("Randomizer.getPercent() = " + percent);
            nums.add(percent);
        }
        System.out.println("nums.size() = " + nums.size());
    }
}