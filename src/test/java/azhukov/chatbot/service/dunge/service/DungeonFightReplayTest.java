package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.combination.CombinationService;
import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.ability.HeroAbilityService;
import azhukov.chatbot.service.dunge.data.BossInfo;
import azhukov.chatbot.service.dunge.data.DungeonOperation;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DungeonFightReplayTest {
    @Test
    void replayAfterBossWriteAppliesHeroSideExactlyOnce() {
        HeroInfo hero = new HeroInfo().setName("hero");
        BossInfo boss = new BossInfo();
        boss.setCycle(1);
        boss.setStage(12);
        boss.setMaxHp(100);

        HeroInfoService heroes = mock(HeroInfoService.class);
        when(heroes.getCurrent("hero")).thenReturn(hero);
        doAnswer(invocation -> {
            ((Consumer<HeroInfo>) invocation.getArgument(1)).accept(hero);
            return null;
        }).when(heroes).update(eq("hero"), any());

        BossService bosses = mock(BossService.class);
        when(bosses.getCurrentBoss()).thenReturn(boss);
        doAnswer(invocation -> {
            ((Consumer<BossInfo>) invocation.getArgument(0)).accept(boss);
            return null;
        }).when(bosses).updateCurrentBoss(any());

        DungeonOperation operation = new DungeonOperation()
                .setId("fight-replay-1")
                .setType(DungeonOperation.Type.FIGHT)
                .setHero("hero")
                .setBossInstance("1:12")
                .setAmount(10)
                .setSecondaryAmount(25)
                .setCoinReward(2)
                .setIncomingDamage(0)
                .setCreatedAt(LocalDateTime.of(2026, 9, 16, 12, 0));
        DungeonJournalService journal = mock(DungeonJournalService.class);
        when(journal.pending()).thenReturn(List.of(operation));

        ArtifactCatalog artifacts = mock(ArtifactCatalog.class);
        BossCombatService combat = new BossCombatService(bosses, heroes, artifacts, journal);
        DungeonEconomyService economy = mock(DungeonEconomyService.class);
        doAnswer(invocation -> {
            HeroInfo target = invocation.getArgument(0);
            long coins = invocation.getArgument(1);
            target.setCoins(target.getCoins() + coins);
            target.setCoinsEarnedToday(target.getCoinsEarnedToday() + coins);
            return coins;
        }).when(economy).earnForFight(any(HeroInfo.class), anyLong());
        HeroHealthService health = mock(HeroHealthService.class);
        when(health.damage(any(), anyInt(), any())).thenReturn(null);

        DungeonService dungeon = new DungeonService(heroes, health, mock(DungeonResetService.class), bosses, combat, economy,
                mock(DungeonStateService.class), journal, artifacts, mock(DungeonRandom.class), List.of(),
                mock(CombinationService.class), mock(HeroAbilityService.class));

        // Simulate a crash after the boss-side store committed, before the hero-side fight result.
        combat.applyPrepared(operation);
        dungeon.replayPendingActions();
        dungeon.replayPendingActions();

        assertEquals(10, boss.getDamageReceived());
        assertEquals(10, hero.getBossDamage());
        assertEquals(25, hero.getExperience());
        assertEquals(2, hero.getCoins());
        assertEquals(2, hero.getCoinsEarnedToday());
        verify(journal, times(2)).complete(operation);
    }
}
