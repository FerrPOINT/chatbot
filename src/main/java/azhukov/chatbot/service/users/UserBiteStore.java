package azhukov.chatbot.service.users;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import lombok.RequiredArgsConstructor;
import org.mapdb.Serializer;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserBiteStore {

    private final DbService dbService;

    public int bite(String user) {
        return getCurrentStage().compute(createKey(user), (s, aLong) -> aLong == null ? 1 : aLong + 1);
    }

    private Map<String, Integer> getCurrentStage() {
        return dbService.getDb(DbType.BITE)
                .hashMap("LIFETIME_BITE", Serializer.STRING, Serializer.INTEGER)
                .createOrOpen();
    }


    private String createKey(String user) {
        return "USER_BITE_" + user.toUpperCase();
    }

}
