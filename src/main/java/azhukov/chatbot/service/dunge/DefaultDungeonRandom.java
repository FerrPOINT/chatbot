package azhukov.chatbot.service.dunge;

import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

@Component
public class DefaultDungeonRandom implements DungeonRandom {
    private static final ThreadLocal<RandomGenerator> RANDOM = ThreadLocal.withInitial(
            () -> RandomGeneratorFactory.<RandomGenerator>of("L64X1024MixRandom").create());

    @Override
    public int nextInt(int originInclusive, int boundExclusive) {
        return RANDOM.get().nextInt(originInclusive, boundExclusive);
    }
}
