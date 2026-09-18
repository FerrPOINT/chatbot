package azhukov.chatbot.service.dunge.service;

import azhukov.chatbot.service.dunge.ArtifactCatalog;
import azhukov.chatbot.service.dunge.DungeonRandom;
import azhukov.chatbot.service.dunge.DungeonTime;
import azhukov.chatbot.service.dunge.data.HeroDamage;
import azhukov.chatbot.service.dunge.data.HeroInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HeroHealthService {
    private final ArtifactCatalog artifacts;
    private final DungeonRandom random;
    private final DungeonTime time;

    public DamageResult damage(HeroInfo hero, HeroDamage incoming) {
        return damage(hero, incoming == null ? 0 : incoming.getValue());
    }

    public DamageResult damage(HeroInfo hero, int incomingLevels) {
        return damage(hero, incomingLevels, time.now());
    }

    public DamageResult damage(HeroInfo hero, int incomingLevels, LocalDateTime occurredAt) {
        int incoming = Math.max(0, incomingLevels);
        int absorbed = Math.min(Math.max(0, hero.getShield()), incoming);
        hero.setShield(Math.max(0, hero.getShield() - absorbed));
        int remaining = incoming - absorbed;
        HeroDamage current = hero.getDamageGot() == null ? HeroDamage.NONE : hero.getDamageGot();
        HeroDamage result = HeroDamage.getByValue(current.getValue() + remaining);
        boolean saved = false;
        if (result == HeroDamage.DEAD && hero.getRebornPercentage() > 0 && random.chance(hero.getRebornPercentage())) {
            result = HeroDamage.ALMOUST_DEAD;
            hero.setRebornPercentage(0);
            saved = true;
        }
        hero.setDamageGot(result);
        boolean died = result == HeroDamage.DEAD;
        if (died) kill(hero, occurredAt);
        return new DamageResult(incoming, absorbed, remaining, result, died, saved);
    }

    public boolean kill(HeroInfo hero) {
        return kill(hero, time.now());
    }

    public boolean kill(HeroInfo hero, LocalDateTime occurredAt) {
        if (hero.getDeadTime() != null) return false;
        int penalty = Math.max(15, 20 - artifacts.deathPenaltyReductionPoints(hero));
        hero.setExperience(DungeonNumbers.multiplyDivide(hero.getExperience(), 100L - penalty, 100L));
        int retention = artifacts.deathCoinRetentionPercent(hero);
        hero.setCoins(DungeonNumbers.multiplyDivide(hero.getCoins(), retention, 100L));
        hero.setDamageGot(HeroDamage.DEAD);
        hero.setDeadTime(occurredAt);
        DungeonMetrics.death(hero.getName(), penalty, hero.getExperience(), hero.getCoins(), reviveAt(hero));
        return true;
    }

    public boolean reviveIfExpired(HeroInfo hero) {
        if (hero.getDeadTime() == null) return false;
        if (time.now().isBefore(reviveAt(hero))) return false;
        hero.setDeadTime(null).setDamageGot(HeroDamage.NONE).setShield(artifacts.dailyShield(hero));
        resetDailyFlags(hero);
        return true;
    }

    public boolean reviveEarly(HeroInfo hero) {
        if (hero.getDeadTime() == null) return false;
        hero.setDeadTime(null).setDamageGot(HeroDamage.MEDIUM).setShield(artifacts.dailyShield(hero));
        resetDailyFlags(hero);
        return true;
    }

    public LocalDateTime reviveAt(HeroInfo hero) {
        int hours = Math.max(19, 24 - artifacts.deathHoursDiscount(hero));
        return hero.getDeadTime() == null ? null : hero.getDeadTime().plusHours(hours);
    }

    public boolean healOne(HeroInfo hero) {
        return heal(hero, 1);
    }

    public boolean heal(HeroInfo hero, int levels) {
        if (hero.isDead() || hero.getDamageGot() == HeroDamage.NONE || levels <= 0) return false;
        hero.heal(levels);
        return true;
    }

    public void dailyReset(HeroInfo hero) {
        if (!hero.isDead()) {
            hero.setDamageGot(HeroDamage.NONE);
            hero.setShield(artifacts.dailyShield(hero));
        }
        resetDailyFlags(hero);
    }

    private void resetDailyFlags(HeroInfo hero) {
        hero.setCrit(0).setEvents(0).setSpecialAbilityUsed(false).setRebornPercentage(0)
                .setCoinsEarnedToday(0).setHealUsedToday(false).setRushUsedToday(false);
    }

    @Data
    public static class DamageResult {
        private final int incoming;
        private final int absorbed;
        private final int applied;
        private final HeroDamage damage;
        private final boolean died;
        private final boolean saved;
    }
}
