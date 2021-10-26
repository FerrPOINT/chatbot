package azhukov.chatbot.service.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Store<K, V> {

    private Map<K, V> map = new ConcurrentHashMap<>();

    public V put(K key, V value) {
        return map.put(key, value);
    }

    public V get(Object key) {
        return map.get(key);
    }

    public void clear() {
        map.clear();
    }

}
