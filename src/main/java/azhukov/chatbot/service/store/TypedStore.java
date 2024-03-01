package azhukov.chatbot.service.store;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.serializer.SerializerString;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class TypedStore<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules();

    private final Supplier<DB> dbGet;
    private final String key;
    private final Class<T> clazz;

    @SneakyThrows
    public String put(String key, T t) {
        return getMap().put(key, MAPPER.writeValueAsString(t));
    }

    @SneakyThrows
    public String delete(String key) {
        return getMap().remove(key);
    }

    @SneakyThrows
    public T get(String key) {
        String rawValue = getMap().get(key);
        return rawValue == null ? null : MAPPER.readValue(rawValue, clazz);
    }

    public void clear() {
        getMap().clear();
    }

    public void updateAll(Consumer<T> acceptor, StoreUpdater<T> storeUpdater) {
        Set<String> keys = getMap().getKeys();
        for (String key : keys) {
            T value = get(key);
            if (storeUpdater != null) {
                Function<String, String> keyUpdater = storeUpdater.getKeyUpdater();
                Comparator<T> comparator = storeUpdater.getComparator();
                String updatedKey = keyUpdater.apply(key);
                if (!updatedKey.equals(key)) {
                    T anotherValue = get(updatedKey);
                    if (anotherValue != null) {
                        int compare = value == null ? -1 : comparator.compare(value, anotherValue);
                        if (compare < 0) {
                            value = anotherValue;
                        }
                    }
                    key = updatedKey;
                }
            }
            if (value != null) {
                acceptor.accept(value);
                put(key, value);
            }
        }
    }

    public void updateAll(Consumer<T> acceptor) {
        updateAll(acceptor, null);
    }

    public void handleAll(Consumer<T> acceptor) {
        Set<String> keys = getMap().getKeys();
        for (String s : keys) {
            T value = get(s);
            if (value != null) {
                acceptor.accept(value);
            }
        }
    }


    private HTreeMap<String, String> getMap() {
        return dbGet.get().hashMap(this.key, new SerializerString(), new SerializerString()).createOrOpen();
    }

}
