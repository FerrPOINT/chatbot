package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.DungeonTime;
import azhukov.chatbot.service.dunge.data.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DungeonResetServiceTest {
    @Test
    void dailyResetHealsLivingHeroButDoesNotReviveDeadHero() {
        HeroInfo living = new HeroInfo().setName("living").setDamageGot(HeroDamage.BIG).setShield(0)
                .setEvents(3).setSpecialAbilityUsed(true).setCoinsEarnedToday(15).setHealUsedToday(true).setRushUsedToday(true);
        HeroInfo dead = new HeroInfo().setName("dead").setDamageGot(HeroDamage.DEAD)
                .setDeadTime(LocalDateTime.of(2026, 9, 16, 1, 0)).setEvents(3).setSpecialAbilityUsed(true);
        HeroInfoService heroes = heroes(living, dead);
        ArtifactCatalog artifacts = mock(ArtifactCatalog.class);
        when(artifacts.dailyShield(living)).thenReturn(4);
        DungeonTime time = mock(DungeonTime.class);
        when(time.today()).thenReturn(LocalDate.of(2026, 9, 17));
        HeroHealthService health = new HeroHealthService(artifacts, mock(DungeonRandom.class), time);
        DungeonState stateData = new DungeonState();
        stateData.setLastDungeonResetDate(LocalDate.of(2026, 9, 16));
        stateData.buffs().addAttack();
        DungeonStateService state = state(stateData);
        DungeonJournalService journal = journal();
        DungeonResetService reset = new DungeonResetService(heroes, health, state, time, journal);

        assertTrue(reset.resetForDate(LocalDate.of(2026, 9, 17)));

        assertEquals(HeroDamage.NONE, living.getDamageGot());
        assertEquals(4, living.getShield());
        assertEquals(0, living.getEvents());
        assertFalse(living.isSpecialAbilityUsed());
        assertEquals(0, living.getCoinsEarnedToday());
        assertEquals(HeroDamage.DEAD, dead.getDamageGot());
        assertNotNull(dead.getDeadTime());
        assertEquals(0, stateData.buffs().attackPercent());
    }

    private HeroInfoService heroes(HeroInfo... data) {
        HeroInfoService heroes = mock(HeroInfoService.class);
        when(heroes.all()).thenReturn(List.of(data));
        doAnswer(i -> { for (HeroInfo hero : data) ((Consumer<HeroInfo>) i.getArgument(0)).accept(hero); return null; })
                .when(heroes).updateAll(any());
        for (HeroInfo hero : data) {
            when(heroes.getCurrent(hero.getName())).thenReturn(hero);
            doAnswer(i -> { ((Consumer<HeroInfo>) i.getArgument(1)).accept(hero); return null; })
                    .when(heroes).update(eq(hero.getName()), any());
        }
        return heroes;
    }

    private DungeonStateService state(DungeonState data) {
        DungeonStateService state = mock(DungeonStateService.class);
        when(state.get()).thenReturn(data);
        doAnswer(i -> { ((Consumer<DungeonState>) i.getArgument(0)).accept(data); return null; }).when(state).update(any());
        return state;
    }

    private DungeonJournalService journal() {
        DungeonJournalService journal = mock(DungeonJournalService.class);
        when(journal.prepare(anyString(), any(), nullable(String.class), nullable(String.class))).thenAnswer(i ->
                new DungeonOperation().setId(i.getArgument(0)).setType(i.getArgument(1))
                        .setHero(i.getArgument(2)).setBossInstance(i.getArgument(3)));
        return journal;
    }
}
