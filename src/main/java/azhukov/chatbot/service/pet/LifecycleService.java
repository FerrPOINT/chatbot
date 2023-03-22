package azhukov.chatbot.service.pet;

import azhukov.chatbot.db.DbService;
import azhukov.chatbot.db.DbType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class LifecycleService {

    private final DbService dbService;

    //every 12 hours
    @Scheduled(cron = "0 * */12 ? * *")
    void update() {
        offset(-1);
    }

    public void reset() {
        offset(-100500);
    }

    public LifecycleStage offset(int count) {
        final LifecycleStage offset = current().offset(count);
        save(offset);
        return offset;
    }

    public LifecycleStage current() {
        final Atomic.String currentStage = getCurrentStage();
        if (currentStage.get() == null) {
            final String name = LifecycleStage.WERY_HUNGRY.name();
            currentStage.getAndSet(name);
            return LifecycleStage.valueOf(name);
        } else {
            return LifecycleStage.valueOf(currentStage.get());
        }
    }

    private void save(LifecycleStage lifecycleStage) {
        final Atomic.String currentStage = getCurrentStage();
        final String name = lifecycleStage.name();
        currentStage.getAndSet(name);
    }

    private Atomic.String getCurrentStage() {
        final DB db = dbService.getDb(DbType.DOG_LIFE);
        final DB.AtomicStringMaker current = db.atomicString("CURRENT_STAGE");
        return current.createOrOpen();
    }

}
