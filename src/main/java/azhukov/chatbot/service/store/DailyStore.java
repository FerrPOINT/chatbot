package azhukov.chatbot.service.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DailyStore {

    private static Map<String, Store> stores = new ConcurrentHashMap<>();

    private static Map<String, AtomicInteger> keyToCount = new ConcurrentHashMap<>();

    //every day at 0:00
    @Scheduled(cron = "0 0 6 * * ?")
    void cleanScheduled() {
        clean();
    }

    public static void clean(){
        log.info("Clean the store with keys: {}", stores.keySet());
        keyToCount.clear();
        stores.forEach((k, store) -> store.clear());
    }

    public static <K, V> Store<K, V> getStore(String key){
        return stores.computeIfAbsent(key, s -> new Store<K, V>());
    }

    public boolean isTodayAllowed(String key) {
        return keyToCount.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet() < 2;
    }

}
