package azhukov.chatbot.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DbService {

    private static final String FOLDER = "/opt/db" + File.separator;

    private Map<String, DB> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        new File(FOLDER).mkdirs();
    }

    public DB getDb(DbType dbType) {
        return cache.computeIfAbsent(dbType.name(), s -> DBMaker.fileDB(FOLDER + s + ".db")
                .transactionEnable()
                .closeOnJvmShutdown()
                .fileLockWait()
                .make()
        );
    }

    //every 1 min
    @Scheduled(cron = "0 */1 * ? * *")
    void commit() {
        for (DB value : cache.values()) {
            try {
                value.commit();
            } catch (Exception e) {
                log.error("While commit db service", e);
            }
        }
    }

    @PreDestroy
    void shutdown() {
        for (DB value : cache.values()) {
            try {
                value.commit();
                value.close();
            } catch (Exception e) {
                log.error("While shutdown db service", e);
            }
        }
    }

}
