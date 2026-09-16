package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.data.DungeonOperation;
import azhukov.chatbot.service.dunge.data.DungeonOperationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;

@Service
@RequiredArgsConstructor
public class DungeonJournalService {
    private final DungeonOperationStore store;

    public synchronized DungeonOperation prepare(DungeonOperation.Type type, String hero, String bossInstance) {
        return prepare(UUID.randomUUID().toString(), type, hero, bossInstance);
    }

    public synchronized DungeonOperation prepare(String id, DungeonOperation.Type type, String hero, String bossInstance) {
        DungeonOperation existing = store.get(id);
        if (existing != null) return existing;
        return new DungeonOperation()
                .setId(id).setType(type).setHero(hero)
                .setBossInstance(bossInstance).setCreatedAt(LocalDateTime.now());
    }

    public synchronized void save(DungeonOperation operation) {
        store.put(operation.getId(), operation);
        store.commit();
        DungeonMetrics.operation("PREPARED", operation.getId(), operation.getType().name(), operation.getBossInstance(), operation.getHero());
    }

    public synchronized void complete(DungeonOperation operation) {
        operation.setStatus(DungeonOperation.Status.COMPLETED).setCompletedAt(LocalDateTime.now());
        store.put(operation.getId(), operation);
        store.commit();
        DungeonMetrics.operation("COMPLETED", operation.getId(), operation.getType().name(), operation.getBossInstance(), operation.getHero());
    }

    public List<DungeonOperation> pending() {
        List<DungeonOperation> result = new ArrayList<>();
        store.handleAll(op -> { if (op.getStatus() != DungeonOperation.Status.COMPLETED) result.add(op); });
        return result;
    }

    public Set<String> retainedOperationIds() {
        Set<String> result = new HashSet<>();
        store.handleAll(op -> result.add(op.getId()));
        return result;
    }

    public void replay(DungeonOperation operation) {
        DungeonMetrics.operation("REPLAY", operation.getId(), operation.getType().name(), operation.getBossInstance(), operation.getHero());
    }

    @Scheduled(cron = "0 30 4 * * SUN")
    public synchronized void compact() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<String> remove = new ArrayList<>();
        store.handleAll(op -> {
            if (op.getStatus() == DungeonOperation.Status.COMPLETED && op.getCompletedAt() != null
                    && op.getCompletedAt().isBefore(threshold)) remove.add(op.getId());
        });
        remove.forEach(store::delete);
        store.commit();
    }
}
