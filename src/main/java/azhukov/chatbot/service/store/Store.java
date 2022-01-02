package azhukov.chatbot.service.store;

import azhukov.chatbot.db.DbType;
import lombok.RequiredArgsConstructor;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    public void clear() {
        getMap().clear();
    }

    private HTreeMap<String, String> getMap(){
       return dbGet.get().hashMap(this.key, new SerializerString(), new SerializerString()).createOrOpen();
    }

}
