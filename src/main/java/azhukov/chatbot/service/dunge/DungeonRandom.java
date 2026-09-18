package azhukov.chatbot.service.dunge;

import java.util.List;

public interface DungeonRandom {
    int nextInt(int originInclusive, int boundExclusive);

    default boolean chance(int percent) {
        if (percent <= 0) return false;
        if (percent >= 100) return true;
        return nextInt(0, 100) < percent;
    }

    default <T> T item(List<T> items) {
        return items == null || items.isEmpty() ? null : items.get(nextInt(0, items.size()));
    }
}
