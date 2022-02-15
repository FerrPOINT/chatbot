package azhukov.chatbot.service.store;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyStore {

    private final DbService dbService;

    private static Map<String, Store> stores = new ConcurrentHashMap<>();

    private static Map<String, AtomicInteger> keyToCount = new ConcurrentHashMap<>();

    //every day at 0:00
    @Scheduled(cron = "0 0 6 * * ?")
    void cleanScheduled() {
        clean();
    }

    public void clean(){
        log.info("Clean the store with keys: {}", stores.keySet());
        keyToCount.clear();
        stores.forEach((k, store) -> store.clear());
    }

    public Store getStore(String key){
        return stores.computeIfAbsent(key, s -> new Store(() -> dbService.getDb(DbType.STORE), key));
    }

    public boolean isTodayAllowed(String key) {
        return keyToCount.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet() < 2;
    }

}
