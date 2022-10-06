package azhukov.chatbot.service.users;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserCollectionStore {

    private final DbService dbService;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Set<String>> VALUE_TYPE_REF = new TypeReference<>() {
    };

    public Set<String> getCurrentSet(String user, String dictionary) {
        HTreeMap<String, String> total = getCurrent(dictionary);
        String value = total.get(user);
        return value == null ? null : deserialize(value);
    }

    public void save(String user, Set<String> values, String dictionary) {
        HTreeMap<String, String> total = getCurrent(dictionary);
        total.put(user, serialize(values));
    }

    private HTreeMap<String, String> getCurrent(String dictionary) {
        return dbService.getDb(DbType.COLLECT)
                .hashMap("TOTAL_" + dictionary, Serializer.STRING, Serializer.STRING)
                .createOrOpen();
    }

    @SneakyThrows
    private Set<String> deserialize(String strValue) {
        return objectMapper.readValue(strValue, VALUE_TYPE_REF);
    }

    @SneakyThrows
    private String serialize(Set<String> values) {
        return objectMapper.writeValueAsString(values);
    }

}
