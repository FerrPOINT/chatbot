package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonTime;
import azhukov.chatbot.service.dunge.data.BossInfo;
import azhukov.chatbot.service.dunge.data.BossStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BossServiceCycleTest {
    @Test
    void stageFortySixTransitionsToScaledStageThirtyEight() {
        BossStore store = mock(BossStore.class);
        AtomicReference<BossInfo> current = new AtomicReference<>();
        when(store.get("CURRENT_BOSS")).thenAnswer(i -> current.get());
        when(store.put(eq("CURRENT_BOSS"), any(BossInfo.class))).thenAnswer(i -> {
            current.set(i.getArgument(1)); return null;
        });
        DungeonTime time = mock(DungeonTime.class);
        when(time.now()).thenReturn(LocalDateTime.of(2026, 9, 16, 12, 0));
        BossService service = new BossService(new ObjectMapper(), store, mock(ArtifactCatalog.class), time);
        service.init();

        BossInfo defeated = service.createInstance(46, 1);
        defeated.setDamageReceived(defeated.getMaxHp());
        defeated.setRewards(new HashSet<>());
        current.set(defeated);

        BossInfo next = service.transitionAfterVictory();
        assertEquals(38, next.getStage());
        assertEquals(2, next.getCycle());
        assertEquals(92_000, next.getMaxHp());
        assertEquals("2:38", next.getInstanceId());
    }
}
