package azhukov.chatbot.service.users;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import lombok.RequiredArgsConstructor;
import org.mapdb.DB;
import org.mapdb.Serializer;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserStore {

    private final DbService dbService;

    public boolean isExist(String user) {
        return getCurrent().contains(user);
    }

    public void addUser(String user) {
        getCurrent().add(user);
    }

    private Set<String> getCurrent() {
        DB db = dbService.getDb(DbType.USERS);
        return db.hashSet("ALL_USERS", Serializer.STRING)
                .expireAfterGet(360, TimeUnit.DAYS)
                .createOrOpen();
    }

}
