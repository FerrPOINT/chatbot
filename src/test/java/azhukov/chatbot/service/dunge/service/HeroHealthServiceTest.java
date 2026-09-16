package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.DungeonTime;
import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HeroHealthServiceTest {
    private ArtifactCatalog artifacts;
    private DungeonTime time;

    @BeforeEach
    void setUp() {
        artifacts = mock(ArtifactCatalog.class);
        time = mock(DungeonTime.class);
        when(time.now()).thenReturn(LocalDateTime.of(2026, 9, 16, 12, 0));
    }

    @Test
    void partialShieldActuallyReducesDamage() {
        HeroHealthService service = new HeroHealthService(artifacts, fixed(99), time);
        HeroInfo hero = new HeroInfo().setDamageGot(HeroDamage.NONE).setShield(2);
        HeroHealthService.DamageResult result = service.damage(hero, 3);
        assertEquals(2, result.getAbsorbed());
        assertEquals(1, result.getApplied());
        assertEquals(HeroDamage.SLIGHT, hero.getDamageGot());
        assertEquals(0, hero.getShield());
    }

    @Test
    void deathPenaltyAndCoinLossApplyExactlyOnceAndFullLockIsObserved() {
        HeroHealthService service = new HeroHealthService(artifacts, fixed(99), time);
        HeroInfo hero = new HeroInfo().setExperience(10_000).setCoins(50).setDamageGot(HeroDamage.ALMOUST_DEAD);
        service.damage(hero, 1);
        assertEquals(8_000, hero.getExperience());
        assertEquals(0, hero.getCoins());
        assertEquals(LocalDateTime.of(2026, 9, 17, 12, 0), service.reviveAt(hero));
        service.kill(hero);
        assertEquals(8_000, hero.getExperience());

        when(time.now()).thenReturn(LocalDateTime.of(2026, 9, 17, 11, 59));
        assertFalse(service.reviveIfExpired(hero));
        when(time.now()).thenReturn(LocalDateTime.of(2026, 9, 17, 12, 0));
        assertTrue(service.reviveIfExpired(hero));
        assertEquals(HeroDamage.NONE, hero.getDamageGot());
    }

    @Test
    void exactFiftyPercentBoundaryHasBothOutcomes() {
        HeroInfo saved = new HeroInfo().setDamageGot(HeroDamage.ALMOUST_DEAD).setRebornPercentage(50);
        new HeroHealthService(artifacts, fixed(49), time).damage(saved, 1);
        assertEquals(HeroDamage.ALMOUST_DEAD, saved.getDamageGot());

        HeroInfo dead = new HeroInfo().setDamageGot(HeroDamage.ALMOUST_DEAD).setRebornPercentage(50);
        new HeroHealthService(artifacts, fixed(50), time).damage(dead, 1);
        assertEquals(HeroDamage.DEAD, dead.getDamageGot());
    }

    @Test
    void maximumLateArtifactsStillLeaveMeaningfulDeathCost() {
        when(artifacts.deathPenaltyReductionPoints(any())).thenReturn(5);
        when(artifacts.deathCoinRetentionPercent(any())).thenReturn(25);
        when(artifacts.deathHoursDiscount(any())).thenReturn(5);
        HeroHealthService service = new HeroHealthService(artifacts, fixed(99), time);
        HeroInfo hero = new HeroInfo().setExperience(10_000).setCoins(100).setDamageGot(HeroDamage.ALMOUST_DEAD);
        service.damage(hero, 1);
        assertEquals(8_500, hero.getExperience());
        assertEquals(25, hero.getCoins());
        assertEquals(LocalDateTime.of(2026, 9, 17, 7, 0), service.reviveAt(hero));
        service.reviveEarly(hero);
        assertEquals(8_500, hero.getExperience(), "early necromancer revive must not apply or reverse the penalty");
        assertEquals(HeroDamage.MEDIUM, hero.getDamageGot());
    }

    private DungeonRandom fixed(int value) { return (origin, bound) -> value; }
}
