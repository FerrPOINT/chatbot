package azhukov.chatbot.service.pet;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
@Service
@Slf4j
public class LifecycleService {

    private final DbService dbService;

    //every 6 hours
    @Scheduled(cron = "0 * */6 ? * *")
    void update() {
        offset(-1);
    }

    public LifecycleStage offset(int count) {
        final LifecycleStage offset = current().offset(count);
        save(offset);
        return offset;
    }

    public LifecycleStage current() {
        final DB db = dbService.getDb(DbType.DOG_LIFE);
        final DB.AtomicStringMaker current = db.atomicString("CURRENT_STAGE");
        final Atomic.String string = current.createOrOpen();
        if (string.get() == null) {
            final String name = LifecycleStage.WERY_HUNGRY.name();
            string.getAndSet(name);
            return LifecycleStage.valueOf(name);
        } else {
            return LifecycleStage.valueOf(string.get());
        }
    }

    private void save(LifecycleStage lifecycleStage) {
        final DB db = dbService.getDb(DbType.DOG_LIFE);
        final String name = lifecycleStage.name();
        final DB.AtomicStringMaker current = db.atomicString("CURRENT_STAGE");
        final Atomic.String string = current.createOrOpen();
        string.getAndSet(name);
    }

}
