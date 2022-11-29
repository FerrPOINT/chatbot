package azhukov.chatbot.service.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.serializer.SerializerString;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class TypedStore<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Supplier<DB> dbGet;
    private final String key;
    private final Class<T> clazz;

    @SneakyThrows
    public String put(String key, T t) {
        return getMap().put(key, MAPPER.writeValueAsString(t));
    }

    @SneakyThrows
    public T get(String key) {
        String rawValue = getMap().get(key);
        return rawValue == null ? null : MAPPER.readValue(rawValue, clazz);
    }

    public void clear() {
        getMap().clear();
    }

    private HTreeMap<String, String> getMap() {
        return dbGet.get().hashMap(this.key, new SerializerString(), new SerializerString()).createOrOpen();
    }

}
