package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonTime;
import azhukov.chatbot.service.dunge.data.BossInfo;
import azhukov.chatbot.service.dunge.data.DungeonOperation;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DungeonEconomyServiceTest {
    @Test
    void siegeBoundariesUseSpecifiedPpm() {
        assertEquals(400, DungeonEconomyService.siegePpm(1));
        assertEquals(400, DungeonEconomyService.siegePpm(9));
        assertEquals(450, DungeonEconomyService.siegePpm(10));
        assertEquals(450, DungeonEconomyService.siegePpm(24));
        assertEquals(500, DungeonEconomyService.siegePpm(25));
        assertEquals(600, DungeonEconomyService.siegePpm(50));
        assertEquals(700, DungeonEconomyService.siegePpm(100));
    }

    @Test
    void preparedSiegeReplayDebitsAndCreditsExactlyOnce() {
        HeroInfo hero = new HeroInfo().setName("hero").setCoins(100);
        BossInfo boss = new BossInfo(); boss.setCycle(1); boss.setStage(43); boss.setMaxHp(1_000);
        HeroInfoService heroes = mock(HeroInfoService.class);
        doAnswer(i -> { ((Consumer<HeroInfo>) i.getArgument(1)).accept(hero); return null; })
                .when(heroes).update(eq("hero"), any());
        BossService bosses = mock(BossService.class);
        doAnswer(i -> { ((Consumer<BossInfo>) i.getArgument(0)).accept(boss); return null; })
                .when(bosses).updateCurrentBoss(any());
        DungeonOperation op = new DungeonOperation().setId("op-1").setType(DungeonOperation.Type.SIEGE)
                .setHero("hero").setBossInstance("1:43").setAmount(10).setSecondaryAmount(10);
        DungeonJournalService journal = mock(DungeonJournalService.class);
        when(journal.pending()).thenReturn(List.of(op));
        DungeonEconomyService service = new DungeonEconomyService(heroes, mock(HeroHealthService.class), bosses,
                mock(BossCombatService.class), mock(ArtifactCatalog.class), journal, mock(DungeonTime.class));

        service.replayPending();
        service.replayPending();

        assertEquals(90, hero.getCoins());
        assertEquals(10, hero.getBossDonations());
        assertEquals(10, boss.getTreasury());
        assertEquals(10, boss.getDonationsByHero().get("hero"));
    }

    @Test
    void ordinaryCoinIncomeStopsAtDailyCap() {
        HeroInfo hero = new HeroInfo().setName("hero");
        HeroInfoService heroes = mock(HeroInfoService.class);
        doAnswer(i -> { ((Consumer<HeroInfo>) i.getArgument(1)).accept(hero); return null; })
                .when(heroes).update(eq("hero"), any());
        ArtifactCatalog artifacts = mock(ArtifactCatalog.class);
        DungeonEconomyService service = new DungeonEconomyService(heroes, mock(HeroHealthService.class), mock(BossService.class),
                mock(BossCombatService.class), artifacts, mock(DungeonJournalService.class), mock(DungeonTime.class));
        for (int i = 0; i < 30; i++) service.earnForEvent(hero);
        assertEquals(15, hero.getCoins());
        assertEquals(15, hero.getCoinsEarnedToday());
    }

    @Test
    void fightRewardDoesNotUnderflowAtLongBoundaries() {
        DungeonEconomyService service = new DungeonEconomyService(mock(HeroInfoService.class), mock(HeroHealthService.class),
                mock(BossService.class), mock(BossCombatService.class), mock(ArtifactCatalog.class),
                mock(DungeonJournalService.class), mock(DungeonTime.class));
        assertEquals(5, service.fightCoinReward(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(1, service.fightCoinReward(0, Long.MAX_VALUE));
    }

    @Test
    void concurrentDonationsCannotSpendTheSameCoinsTwice() throws Exception {
        HeroInfo hero = new HeroInfo().setName("hero").setCoins(50);
        BossInfo boss = new BossInfo(); boss.setCycle(1); boss.setStage(43); boss.setMaxHp(10_000);
        HeroInfoService heroes = mock(HeroInfoService.class);
        when(heroes.getCurrent("hero")).thenReturn(hero);
        doAnswer(i -> { ((Consumer<HeroInfo>) i.getArgument(1)).accept(hero); return null; })
                .when(heroes).update(eq("hero"), any());
        BossService bosses = mock(BossService.class);
        when(bosses.getCurrentBoss()).thenReturn(boss);
        DungeonJournalService journal = mock(DungeonJournalService.class);
        when(journal.prepare(eq(DungeonOperation.Type.SIEGE), eq("hero"), eq("1:43"))).thenAnswer(i ->
                new DungeonOperation().setId(UUID.randomUUID().toString()).setType(DungeonOperation.Type.SIEGE)
                        .setHero("hero").setBossInstance("1:43"));
        BossCombatService combat = mock(BossCombatService.class);
        DungeonEconomyService service = new DungeonEconomyService(heroes, mock(HeroHealthService.class), bosses,
                combat, mock(ArtifactCatalog.class), journal, mock(DungeonTime.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<DungeonEconomyService.ActionResult> first = executor.submit(() -> service.siege("hero", 40));
            Future<DungeonEconomyService.ActionResult> second = executor.submit(() -> service.siege("hero", 40));
            long successes = List.of(first.get(), second.get()).stream().filter(DungeonEconomyService.ActionResult::isSuccess).count();
            assertEquals(1, successes);
            assertEquals(10, hero.getCoins());
            verify(combat, times(1)).applyPrepared(any());
        } finally {
            executor.shutdownNow();
        }
    }
}
