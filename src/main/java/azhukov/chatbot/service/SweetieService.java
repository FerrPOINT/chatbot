package azhukov.chatbot.service;

import azhukov.chatbot.constants.Constants;
import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import azhukov.chatbot.dto.RespGgMessage;
import azhukov.chatbot.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class SweetieService {

    private final DbService dbService;
    private final AuthService authService;

    private final Map<String, Map<String, AtomicInteger>> tempData = new ConcurrentHashMap<>();

    public String getSweetie(String user) {
        DB db = dbService.getDb(DbType.SWEETIE);
        HTreeMap<String, Integer> map = getMap(db, user);
        ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
        if (entries.isEmpty()) {
            return null;
        }
        Collections.sort(entries, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        return entries.get(0).getKey();
    }

    public void addSweetie(String user, RespGgMessage message) {
        String text = message.getText();
        String sweetieNickname = getSweetieNickname(text);
        if (sweetieNickname != null && !Constants.MASTER_NAME.equals(sweetieNickname) && !authService.getLogin().equals(sweetieNickname)) {
            Map<String, AtomicInteger> counter = tempData.computeIfAbsent(user, s -> new ConcurrentHashMap<>());
            counter.computeIfAbsent(sweetieNickname, k -> new AtomicInteger()).addAndGet(1);
        }
    }

    private String getSweetieNickname(String message) {
        String name = null;

        String[] tokenized = message.split(", ");
        if (tokenized.length == 1) {
            return null;
        }
        for (String nickname : tokenized) {
            if (nickname.contains(" ")) {
                return null;
            }
            if (name != null) {
                return null;
            }
            name = nickname;
            if (tokenized.length == 2) {
                break;
            }
        }
        return name;
    }

    //every 1 min
    @Scheduled(cron = "0 */1 * ? * *")
    void flush() {
        DB db = dbService.getDb(DbType.SWEETIE);

        for (String user : tempData.keySet()) {
            Map<String, AtomicInteger> counter = tempData.remove(user);
            HTreeMap<String, Integer> map = getMap(db, user);
            for (Map.Entry<String, AtomicInteger> entry : counter.entrySet()) {
                map.compute(entry.getKey(), (key, count) -> count == null ? entry.getValue().get() : entry.getValue().get() + count);
            }
        }
    }

    private HTreeMap<String, Integer> getMap(DB db, String user) {
        DB.HashMapMaker<String, Integer> current = db.hashMap(user, Serializer.STRING, Serializer.INTEGER);
        return current.createOrOpen();
    }


}
