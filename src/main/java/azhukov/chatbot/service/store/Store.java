package azhukov.chatbot.service.store;

import lombok.RequiredArgsConstructor;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.serializer.SerializerString;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class Store {

    private final Supplier<DB> dbGet;
    private final String key;

    public String put(String key, String value) {
        return getMap().put(key, value);
    }

    public String get(Object key) {
        return getMap().get(key);
    }

    public void foreach(Consumer<Map.Entry<String, String>> acceptor) {
        getMap().entrySet().forEach(acceptor);
    }

    public void clear() {
        getMap().clear();
    }

    private HTreeMap<String, String> getMap() {
        return dbGet.get().hashMap(this.key, new SerializerString(), new SerializerString()).createOrOpen();
    }

}
