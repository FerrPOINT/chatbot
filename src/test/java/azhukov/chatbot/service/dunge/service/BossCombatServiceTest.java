package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.data.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BossCombatServiceTest {
    @Test
    void overkillVictoryRewardAndTransitionApplyExactlyOnce() {
        BossInfo defeated = new BossInfo(); defeated.setCycle(1); defeated.setStage(43); defeated.setMaxHp(100);
        defeated.setDamageReceived(90); defeated.setRewards(Set.of("utan"));
        BossInfo next = new BossInfo(); next.setCycle(1); next.setStage(44); next.setMaxHp(200);
        AtomicReference<BossInfo> current = new AtomicReference<>(defeated);
        BossService bosses = mock(BossService.class);
        when(bosses.getCurrentBoss()).thenAnswer(i -> current.get());
        doAnswer(i -> { ((Consumer<BossInfo>) i.getArgument(0)).accept(current.get()); return null; })
                .when(bosses).updateCurrentBoss(any());
        when(bosses.transitionAfterVictory()).thenAnswer(i -> { current.set(next); return next; });

        HeroInfo hero = new HeroInfo().setName("hero");
        Map<String, HeroInfo> heroMap = new HashMap<>(); heroMap.put("hero", hero);
        HeroInfoService heroes = mock(HeroInfoService.class);
        when(heroes.getCurrent("hero")).thenReturn(hero);
        doAnswer(i -> { ((Consumer<HeroInfo>) i.getArgument(1)).accept(heroMap.get(i.getArgument(0))); return null; })
                .when(heroes).update(anyString(), any());
        ArtifactCatalog artifacts = mock(ArtifactCatalog.class);
        AtomicInteger rewards = new AtomicInteger();
        when(artifacts.grantBossReward(any(), eq("utan"))).thenAnswer(i -> {
            rewards.incrementAndGet(); return ArtifactCatalog.RewardResult.GRANTED;
        });
        DungeonJournalService journal = mock(DungeonJournalService.class);
        when(journal.prepare(startsWith("reward:"), eq(DungeonOperation.Type.REWARD), anyString(), anyString()))
                .thenReturn(new DungeonOperation().setId("reward:1:43").setType(DungeonOperation.Type.REWARD));
        BossCombatService service = new BossCombatService(bosses, heroes, artifacts, journal);
        DungeonOperation op = new DungeonOperation().setId("damage-1").setType(DungeonOperation.Type.FIGHT)
                .setHero("hero").setBossInstance("1:43").setAmount(10);

        BossCombatService.DamageResult first = service.applyPrepared(op);
        BossCombatService.DamageResult replay = service.applyPrepared(op);

        assertEquals(10, first.getRealDamage());
        assertEquals(0, replay.getRealDamage());
        assertEquals(100, defeated.getDamageReceived());
        assertEquals(1, rewards.get());
        assertEquals(10, hero.getCoins());
        verify(bosses, times(1)).transitionAfterVictory();
    }
}
