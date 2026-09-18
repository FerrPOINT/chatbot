package azhukov.chatbot.service.dunge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class DungeonAppliedOperationCompactor {
    private final DungeonJournalService journal;
    private final HeroInfoService heroes;
    private final BossService bosses;
    private final DungeonStateService state;

    @Scheduled(cron = "0 0 5 * * SUN")
    public void compact() {
        Set<String> retained = journal.retainedOperationIds();
        heroes.updateAll(hero -> hero.safeAppliedOperationIds().removeIf(id -> !isRetained(id, retained)));
        bosses.compactAppliedOperationIds(retained);
        state.compactAppliedOperationIds(retained);
    }

    static boolean isRetained(String appliedId, Set<String> retained) {
        return retained.stream().anyMatch(operationId -> appliedId.equals(operationId) || appliedId.startsWith(operationId + ":"));
    }
}
