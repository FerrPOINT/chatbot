package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.DungeonTime;
import azhukov.chatbot.service.dunge.data.DungeonState;
import azhukov.chatbot.service.dunge.data.DungeonOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DungeonResetService {
    private final HeroInfoService heroes;
    private final HeroHealthService health;
    private final DungeonStateService state;
    private final DungeonTime time;
    private final DungeonJournalService journal;

    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledReset() { resetForDate(time.today()); }

    @Scheduled(cron = "0 * * * * ?")
    public void reviveExpired() {
        heroes.all().forEach(hero -> reviveIfExpired(hero.getName()));
    }

    public synchronized boolean reviveIfExpired(String heroName) {
        var snapshot = heroes.getCurrent(heroName);
        if (snapshot == null || snapshot.getDeadTime() == null || time.now().isBefore(health.reviveAt(snapshot))) return false;
        String id = "revive:" + snapshot.getName() + ":" + snapshot.getDeadTime();
        DungeonOperation operation = journal.prepare(id, DungeonOperation.Type.REVIVE, snapshot.getName(), null);
        operation.setPayload(snapshot.getDeadTime().toString());
        journal.save(operation);
        applyRevive(operation);
        journal.complete(operation);
        return true;
    }

    public synchronized boolean catchUp() {
        DungeonState current = state.get();
        LocalDate today = time.today();
        if (current.getLastDungeonResetDate() == null) {
            // Migration baseline: preserve the live heroes on first deployment.
            state.update(s -> s.setLastDungeonResetDate(today));
            return false;
        }
        return resetForDate(today);
    }

    public synchronized boolean resetForDate(LocalDate date) {
        DungeonState current = state.get();
        if (date.equals(current.getLastDungeonResetDate())) return false;
        DungeonOperation operation = journal.prepare("reset:" + date, DungeonOperation.Type.RESET, null, null);
        operation.setPayload(date.toString());
        journal.save(operation);
        applyReset(operation, date);
        journal.complete(operation);
        log.info("Dungeon daily reset completed for {}", date);
        return true;
    }

    public synchronized void replayPending() {
        for (DungeonOperation operation : journal.pending()) {
            if (operation.getType() == DungeonOperation.Type.RESET) {
                journal.replay(operation);
                LocalDate date = operation.getPayload() == null
                        ? LocalDate.parse(operation.getId().substring("reset:".length()))
                        : LocalDate.parse(operation.getPayload());
                LocalDate latest = state.get().getLastDungeonResetDate();
                if (latest == null || !latest.isAfter(date)) applyReset(operation, date);
                journal.complete(operation);
            } else if (operation.getType() == DungeonOperation.Type.REVIVE) {
                journal.replay(operation);
                applyRevive(operation);
                journal.complete(operation);
            }
        }
    }

    private void applyReset(DungeonOperation operation, LocalDate date) {
        heroes.updateAll(hero -> {
            if (hero.safeAppliedOperationIds().add(operation.getId())) health.dailyReset(hero);
        });
        state.update(s -> {
            s.setLastDungeonResetDate(date);
            s.buffs().clear();
        });
    }

    private void applyRevive(DungeonOperation operation) {
        heroes.update(operation.getHero(), hero -> {
            if (hero.safeAppliedOperationIds().add(operation.getId())) health.reviveIfExpired(hero);
        });
    }
}
