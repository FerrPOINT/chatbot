package azhukov.chatbot.service.util;

import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class Randomizer {

    private static final ThreadLocal<RandomGenerator> THREAD_LOCAL = new ThreadLocal<>();
    private static final RandomGeneratorFactory<RandomGenerator> FACTORY = RandomGeneratorFactory.of("L64X1024MixRandom");

    public static int getPercent() {
        return getRandom().nextInt(100) + 1;
    }

    public static boolean tossCoin() {
        int num = 0;
        while (num == 0) {
            num = getRandom().nextInt(2);
        }
        return num == 1;
    }

    public static int nextInt(int bound) {
        return getRandom().nextInt(bound);
    }

    public static int nextSafeInt(int bound) {
        return bound < 1 ? 0 : nextInt(bound);
    }

    public static int nextInt(int origin, int bound) {
        return getRandom().nextInt(origin, bound);
    }

    public static <T> T getRandomItem(List<T> items) {
        return CollectionUtils.isEmpty(items) ? null : items.get(Randomizer.nextInt(items.size()));
    }

    private static RandomGenerator getRandom() {
        RandomGenerator randomGenerator = THREAD_LOCAL.get();
        if (randomGenerator == null) {
            randomGenerator = FACTORY.create();
            THREAD_LOCAL.set(randomGenerator);
        }
        return randomGenerator;
    }

}
