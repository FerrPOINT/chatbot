package azhukov.chatbot.service.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DistinctEvictingCollection<T extends Comparable> {

    private final int capacity;
    private final Set<T> set;
    private final List<T> list;

    public DistinctEvictingCollection(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }
        this.capacity = capacity;
        this.set = new HashSet<>(capacity);
        this.list = new ArrayList<>(capacity);
    }

    public synchronized T getPrev() {
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    public synchronized void add(T newElement) {
        if (set.add(newElement)) {
            list.add(newElement);
            // Если превышаем лимит ёмкости — удаляем самый старый
            if (list.size() > capacity) {
                T removed = list.remove(0);
                set.remove(removed);
            }
        }
    }

}
