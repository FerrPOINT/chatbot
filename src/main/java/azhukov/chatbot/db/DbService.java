package azhukov.chatbot.db;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DbService {
    private final String folder;
    private final Map<String, DB> cache = new ConcurrentHashMap<>();

    public DbService(@Value("${dogen.db.folder:/opt/db}") String folder) {
        this.folder = folder.endsWith(File.separator) ? folder : folder + File.separator;
    }

    @PostConstruct
    public void init() {
        new File(folder).mkdirs();
    }

    public DB getDb(DbType dbType) {
        return cache.computeIfAbsent(dbType.name(), s -> DBMaker.fileDB(folder + s + ".db")
                .transactionEnable()
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
    public void shutdown() {
        log.info("Closing DBs");
        for (DB value : cache.values()) {
            try {
                if (!value.isClosed()) {
                    value.commit();
                    value.close();
                }
            } catch (Exception e) {
                log.error("While shutdown db service", e);
            }
        }
    }

}
